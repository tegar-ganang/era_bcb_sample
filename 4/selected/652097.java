package org.kalypso.nofdpidss.export.worker;

import java.io.File;
import java.io.IOException;
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
import org.kalypso.nofdpidss.core.common.utils.various.GEUtils;
import org.kalypso.nofdpidss.export.data.ExportManagerData;
import org.kalypsodeegree.model.feature.Feature;

public class GoogleEarthExportWorker implements ICoreRunnableWithProgress {

    private final String DIR_GOOGLE_EARTH = "googleEarth";

    private final IProject m_project;

    private final ExportManagerData m_data;

    private final IFolder m_tmpDir;

    public GoogleEarthExportWorker(final IProject project, final ExportManagerData data, final IFolder tmpDir) {
        m_project = project;
        m_data = data;
        m_tmpDir = tmpDir;
    }

    public IStatus execute(final IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
        final IFolder dirGoogle = m_tmpDir.getFolder(DIR_GOOGLE_EARTH);
        try {
            final Feature[] googles = m_data.getGoogleEarth();
            for (final Feature google : googles) {
                final IFile iGoogle = GEUtils.getKMZFile(m_project, google);
                final File fGoogle = iGoogle.getLocation().toFile();
                final File dest = new File(dirGoogle.getLocation().toFile(), fGoogle.getName());
                FileUtils.copyFile(fGoogle, dest);
            }
        } catch (final IOException e) {
            NofdpCorePlugin.getDefault().getLog().log(StatusUtilities.statusFromThrowable(e));
            throw ExceptionHelper.getCoreException(IStatus.ERROR, getClass(), e.getMessage());
        }
        WorkspaceSync.sync(dirGoogle, IResource.DEPTH_INFINITE);
        return Status.OK_STATUS;
    }
}
