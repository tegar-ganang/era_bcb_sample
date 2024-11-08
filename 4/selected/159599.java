package org.kalypso.nofdpidss.export.worker;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.kalypso.contribs.eclipse.core.runtime.ExceptionHelper;
import org.kalypso.contribs.eclipse.core.runtime.StatusUtilities;
import org.kalypso.contribs.eclipse.jface.operation.ICoreRunnableWithProgress;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.export.data.ExportManagerData;
import org.kalypso.nofdpidss.report.utils.OpenOfficeExporter;
import org.kalypso.nofdpidss.report.utils.ReportingUtils;
import org.kalypso.nofdpidss.report.utils.OpenOfficeExporter.REPORT_FORMAT;
import org.kalypsodeegree.model.feature.Feature;

public class ReportExportWorker implements ICoreRunnableWithProgress {

    private final String DIR_REPORT = "report";

    private final IProject m_project;

    private final ExportManagerData m_data;

    private final IFolder m_tmpDir;

    public ReportExportWorker(final IProject project, final ExportManagerData data, final IFolder tmpDir) {
        m_project = project;
        m_data = data;
        m_tmpDir = tmpDir;
    }

    public IStatus execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
        IFolder dirReport;
        try {
            dirReport = m_tmpDir.getFolder(DIR_REPORT);
            final REPORT_FORMAT[] formats = m_data.getReportFormats();
            final OpenOfficeExporter exporter = new OpenOfficeExporter(m_project, formats);
            final Feature[] reports = m_data.getReports();
            for (final Feature report : reports) {
                final IFile iReportFile = ReportingUtils.getReportFile(m_project, report);
                final IFile[] files = exporter.getDocuments(iReportFile);
                for (final IFile file : files) {
                    final File iFile = file.getLocation().toFile();
                    final File dest = new File(dirReport.getLocation().toFile(), iFile.getName());
                    FileUtils.copyFile(iFile, dest);
                }
            }
        } catch (final Exception e) {
            NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            throw ExceptionHelper.getCoreException(IStatus.ERROR, getClass(), e.getMessage());
        }
        WorkspaceSync.sync(dirReport, IResource.DEPTH_INFINITE);
        return Status.OK_STATUS;
    }
}
