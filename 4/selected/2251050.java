package org.kalypso.nofdpidss.report.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipFile;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.kalypso.contribs.eclipse.core.resources.CollectFilesVisitor;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.common.NofdpIDSSConstants;
import org.kalypso.nofdpidss.core.common.utils.various.ZipUtils;
import org.kalypso.nofdpidss.report.i18n.Messages;
import org.kalypso.nofdpidss.report.worker.ReportFolders;
import org.kalypso.openofficereportservice.util.schema.JobDefinitionType;
import org.kalypso.openofficereportservice.util.schema.ObjectFactory;
import org.kalypso.openofficereportservice.util.schema.ProcessingActionsType;
import org.kalypso.openofficereportservice.util.schema.ReportingJobType;
import org.kalypso.openofficereportservice.util.schema.ResultDocumentType;
import org.kalypso.openofficereportservice.util.schema.TargetDocumentType;

/**
 * @author Dirk Kuch
 */
public class OpenOfficeExporter {

    public enum REPORT_FORMAT {

        eOpenOffice, ePdf, eWord;

        public String getFileExtension() {
            final REPORT_FORMAT type = REPORT_FORMAT.valueOf(name());
            switch(type) {
                case eOpenOffice:
                    return ".odt";
                case ePdf:
                    return ".pdf";
                case eWord:
                    return ".doc";
                default:
                    throw new IllegalStateException();
            }
        }

        public String getJobFormatString() {
            final REPORT_FORMAT type = REPORT_FORMAT.valueOf(name());
            switch(type) {
                case eOpenOffice:
                    return "odt";
                case ePdf:
                    return "pdf";
                case eWord:
                    return "doc";
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public static final String RESULT_ZIP_NAME = "result.zip";

    private static ReportingJobType m_root = null;

    private final IProject m_project;

    private final REPORT_FORMAT[] m_formats;

    public OpenOfficeExporter(final IProject project, final REPORT_FORMAT[] formats) {
        m_project = project;
        m_formats = formats;
    }

    private void configureJob(final IFile iTempReportFile) {
        m_root = new ReportingJobType();
        final JobDefinitionType jdt = new JobDefinitionType();
        final ResultDocumentType rs = new ResultDocumentType();
        jdt.setResultSettings(rs);
        rs.setArchiveFileName(OpenOfficeExporter.RESULT_ZIP_NAME);
        rs.setMasterTemplate(iTempReportFile.getName());
        final ProcessingActionsType actions = new ProcessingActionsType();
        actions.setProcessMacros(false);
        actions.setProcessReplacementsOnMasterTemplate(false);
        actions.setReplaceImages(false);
        actions.setReplaceTables(false);
        actions.setReplaceText(false);
        actions.setStartHidden(true);
        jdt.setProcessActions(actions);
        m_root.setJobDefinitionMember(jdt);
        final List<TargetDocumentType> targets = rs.getDocumentTarget();
        for (final REPORT_FORMAT format : m_formats) {
            final TargetDocumentType target = new TargetDocumentType();
            target.setFormat(format.getJobFormatString());
            final String name = iTempReportFile.getName();
            final String ext = iTempReportFile.getFileExtension();
            target.setName(name.substring(0, name.length() - ext.length() - 1) + format.getFileExtension());
            targets.add(target);
        }
    }

    public IFile[] getDocuments(final IFile iReportFile) throws Exception {
        final ReportFolders folder = new ReportFolders(NofdpCorePlugin.getProjectManager().getActiveProject());
        final IFolder iTempFolder = folder.getTempFolder(true);
        final IFile iTempReportFile = iTempFolder.getFile(iReportFile.getName());
        FileUtils.copyFile(iReportFile.getLocation().toFile(), iTempReportFile.getLocation().toFile());
        WorkspaceSync.sync(iTempFolder, IResource.DEPTH_INFINITE);
        if (!iTempReportFile.exists()) throw new IllegalStateException(Messages.OpenOfficeExporter_1);
        configureJob(iTempReportFile);
        final IFile jobFile = writeJobFile(iTempFolder);
        processJobFile(jobFile);
        WorkspaceSync.sync(iTempFolder, IResource.DEPTH_INFINITE);
        final IFile resultZip = iTempFolder.getFile(OpenOfficeExporter.RESULT_ZIP_NAME);
        if (!resultZip.exists()) throw new IllegalStateException(Messages.OpenOfficeExporter_2);
        final IFolder iDocumentResultFolder = iTempFolder.getFolder(Messages.OpenOfficeExporter_3);
        final ZipFile zf = new ZipFile(resultZip.getLocation().toFile());
        ZipUtils.unpack(zf, iDocumentResultFolder.getLocation().toFile());
        zf.close();
        WorkspaceSync.sync(iTempFolder, IResource.DEPTH_INFINITE);
        final CollectFilesVisitor visitor = new CollectFilesVisitor();
        iDocumentResultFolder.accept(visitor);
        return visitor.getFiles();
    }

    private void processJobFile(final IFile iJobFile) throws IOException, CoreException, InterruptedException {
        final String java = System.getProperty("java.home") + "/bin/java";
        final IProject globalProject = NofdpCorePlugin.getProjectManager().getBaseProject();
        final IFolder ooLibFolder = globalProject.getFolder(NofdpIDSSConstants.NOFDP_PROJECT_REPORTING_OO_LIB_FOLDER_PATH);
        if (!ooLibFolder.exists()) throw new IllegalStateException(Messages.OpenOfficeExporter_6);
        final File ooJarFile = ooLibFolder.getFile("ooreporting.jar").getLocation().toFile();
        final File log4jFile = ooLibFolder.getFile("log4j.conf").getLocation().toFile();
        if (!log4jFile.exists()) throw new IllegalStateException("log configuration file has to exist!");
        final String ooJarString = ooJarFile.toString();
        final String log4jString = log4jFile.toString();
        final String reportingDataString = iJobFile.getLocation().toFile().toString();
        final String[] cmd = new String[] { java, "-jar", "\"" + ooJarString + "\"", "-c", "\"" + log4jString + "\"", "-f", "\"" + reportingDataString + "\"" };
        final Process exec = Runtime.getRuntime().exec(cmd, null, iJobFile.getLocation().toFile().getParentFile());
        final InputStream errorStream = exec.getErrorStream();
        final InputStream inputStream = exec.getInputStream();
        final StreamGobbler error = new StreamGobbler(errorStream, "Report: ERROR_STREAM");
        final StreamGobbler input = new StreamGobbler(inputStream, "Report: INPUT_STREAM");
        error.start();
        input.start();
        int exitValue = 0;
        int timeRunning = 0;
        while (true) {
            try {
                exitValue = exec.exitValue();
                break;
            } catch (final RuntimeException e) {
            }
            if (timeRunning >= 300000) {
                exec.destroy();
                throw new CoreException(StatusUtilities.createErrorStatus(Messages.OpenOfficeExporter_21));
            }
            Thread.sleep(100);
            timeRunning = timeRunning + 100;
        }
    }

    public IFile writeJobFile(final IFolder iTempFolder) throws JAXBException, IOException {
        final ObjectFactory factory = new ObjectFactory();
        final JAXBElement<ReportingJobType> jaxRoot = factory.createReportingJobRoot(m_root);
        final JAXBContext jc = JAXBContext.newInstance(org.kalypso.openofficereportservice.util.schema.ObjectFactory.class);
        final Marshaller m = jc.createMarshaller();
        final IFile iJobFile = iTempFolder.getFile("job.xml");
        final FileOutputStream os = new FileOutputStream(iJobFile.getLocation().toFile());
        m.marshal(jaxRoot, os);
        os.flush();
        os.close();
        return iJobFile;
    }
}
