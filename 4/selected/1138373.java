package org.kalypso.nofdpidss.report.worker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.progress.UIJob;
import org.kalypso.commons.java.util.zip.ZipUtilities;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolReporting;
import org.kalypso.nofdpidss.core.base.gml.pool.MyBasePool.POOL_TYPE;
import org.kalypso.nofdpidss.core.common.NofdpIDSSConstants;
import org.kalypso.nofdpidss.core.common.utils.gml.ReportingGmlTools;
import org.kalypso.nofdpidss.core.common.utils.various.BaseGeoUtils;
import org.kalypso.nofdpidss.report.i18n.Messages;
import org.kalypso.nofdpidss.report.interfaces.IMapContributor;
import org.kalypso.nofdpidss.report.interfaces.IReportParts;
import org.kalypso.nofdpidss.report.ui.tree.DocumentTreeCategory;
import org.kalypso.nofdpidss.report.utils.StreamGobbler;
import org.kalypso.nofdpidss.report.wizard.report.WizardCreateReport;
import org.kalypso.nofdpidss.report.worker.builders.ModuleReportBuilder;
import org.kalypso.openofficereportservice.util.schema.DocumentType;

/**
 * @author Dirk Kuch
 */
public class ReportBuilder implements ICoreRunnableWithProgress {

    private final IReportParts m_parts;

    private final WizardCreateReport m_wizard;

    private final IMapContributor m_maps;

    public ReportBuilder(final WizardCreateReport wizard, final IReportParts parts, final IMapContributor maps) {
        m_wizard = wizard;
        m_parts = parts;
        m_maps = maps;
    }

    /**
   * @see org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress#execute(org.eclipse.core.runtime.IProgressMonitor)
   */
    public IStatus execute(final IProgressMonitor monitor) {
        final UIJob job = new UIJob("processing") {

            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                try {
                    monitor.beginTask(Messages.ReportBuilder_0, 10);
                    final ReportFolders folders = new ReportFolders(m_wizard.getActiveProject());
                    final MyReportJob myReportJob = new MyReportJob(folders);
                    final DocumentTreeCategory[] categories = m_parts.getSelectedCategories();
                    final DocumentTreeCategory[] maps = m_maps.getSelectedMaps();
                    final List<DocumentTreeCategory> merge = new ArrayList<DocumentTreeCategory>();
                    for (final DocumentTreeCategory dtc : categories) merge.add(dtc);
                    for (final DocumentTreeCategory dtc : maps) merge.add(dtc);
                    final ModuleReportBuilder[] builder = ModuleReportBuilder.getModuleReportBuilders(m_wizard, m_maps, merge.toArray(new DocumentTreeCategory[] {}), m_parts.getSelectedVariants(), folders);
                    for (final ModuleReportBuilder b : builder) {
                        final DocumentType[] types = b.getDocumentTypes();
                        myReportJob.addDocumentTemplates(types);
                    }
                    final IFile iJobFile = myReportJob.writeJobFile();
                    processJobFile(iJobFile);
                    final IFolder iTmpFolder = (IFolder) iJobFile.getParent();
                    WorkspaceSync.sync(iTmpFolder, IResource.DEPTH_INFINITE);
                    final IFile iResultZip = iTmpFolder.getFile(MyReportJob.RESULT_ZIP_NAME);
                    if (!iResultZip.exists()) throw new IllegalStateException(Messages.ReportBuilder_1 + iResultZip.getLocation().toOSString());
                    ZipUtilities.unzip(iResultZip.getLocation().toFile(), iTmpFolder.getLocation().toFile());
                    WorkspaceSync.sync(iTmpFolder, IResource.DEPTH_INFINITE);
                    final IFile iTempResultDocument = iTmpFolder.getFile(MyReportJob.RESULT_DOCUMENT_ODT);
                    if (!iTempResultDocument.exists()) throw new IllegalStateException(Messages.ReportBuilder_2 + iTempResultDocument.getLocation().toOSString());
                    final IFolder iFolderDocuments = m_wizard.getActiveProject().getFolder(NofdpIDSSConstants.NOFDP_PROJECT_REPORTING_DOCUMENTS_FOLDER_PATH);
                    WorkspaceSync.sync(iFolderDocuments, IResource.DEPTH_INFINITE);
                    final String fileName = BaseGeoUtils.getFileName(iFolderDocuments, m_parts.getReportName()) + "." + iTempResultDocument.getFileExtension();
                    final IFile iResultDocument = iFolderDocuments.getFile(fileName);
                    FileUtils.copyFile(iTempResultDocument.getLocation().toFile(), iResultDocument.getLocation().toFile());
                    WorkspaceSync.sync(iFolderDocuments, IResource.DEPTH_INFINITE);
                    final PoolReporting pool = (PoolReporting) NofdpCorePlugin.getProjectManager().getPool(POOL_TYPE.eReporting);
                    ReportingGmlTools.createResultDocumentFeature(pool, m_parts.getReportName(), m_parts.getReportDescription(), iResultDocument.getName());
                } catch (final Exception e) {
                    NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
                    return new Status(IStatus.ERROR, Messages.ReportBuilder_4, e.getMessage());
                }
                return Status.OK_STATUS;
            }
        };
        return job.run(monitor);
    }

    private void processJobFile(final IFile iJobFile) throws IOException, CoreException, InterruptedException {
        final String java = System.getProperty("java.home") + "/bin/java";
        final IProject globalProject = NofdpCorePlugin.getProjectManager().getBaseProject();
        final IFolder ooLibFolder = globalProject.getFolder(NofdpIDSSConstants.NOFDP_PROJECT_REPORTING_OO_LIB_FOLDER_PATH);
        if (!ooLibFolder.exists()) throw new IllegalStateException(Messages.ReportBuilder_7);
        final File ooJarFile = ooLibFolder.getFile("ooreporting.jar").getLocation().toFile();
        final File log4jFile = ooLibFolder.getFile("log4j.conf").getLocation().toFile();
        if (!log4jFile.exists()) throw new IllegalStateException(Messages.ReportBuilder_10);
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
                throw new CoreException(StatusUtilities.createErrorStatus(Messages.ReportBuilder_22));
            }
            Thread.sleep(100);
            timeRunning = timeRunning + 100;
        }
    }
}
