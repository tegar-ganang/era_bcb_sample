package org.jactr.eclipse.remote.jobs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.jactr.eclipse.remote.Activator;

public abstract class ArchiveJob extends WorkspaceJob {

    /**
   * Logger definition
   */
    private static final transient Log LOGGER = LogFactory.getLog(ArchiveJob.class);

    public static IPath createFile(String prefix, String suffix, IPath dir) throws IOException {
        File directory = dir.toFile();
        directory.mkdirs();
        File fp = File.createTempFile(prefix, suffix, directory);
        return new Path(fp.getAbsolutePath());
    }

    private IPath _archiveFile;

    private IPath _strip = new Path("/");

    public ArchiveJob(String name) {
        super(name);
    }

    protected void setArchiveFile(IPath path) {
        _archiveFile = path;
    }

    public IPath getArchiveFile() {
        return _archiveFile;
    }

    protected void setPathToStrip(IPath strip) {
        _strip = strip;
    }

    /**
   * return the resources to be archived
   * 
   * @return
   */
    protected abstract Collection<IResource> getResources();

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        Collection<IResource> resources = getResources();
        monitor = new SubProgressMonitor(monitor, resources.size());
        File fp = _archiveFile.toFile();
        try {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Using " + fp.getAbsolutePath());
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(fp)));
            byte[] block = new byte[4096];
            for (IResource resource : resources) {
                IPath path = resource.getFullPath();
                if (_strip.isPrefixOf(path)) path = path.removeFirstSegments(_strip.segmentCount());
                String name = path.toString();
                if (resource instanceof IContainer) name += "/";
                if (name.startsWith("/")) name = name.substring(1, name.length());
                if (LOGGER.isDebugEnabled()) LOGGER.debug("Compressing " + name);
                monitor.subTask("Compression " + name);
                ZipEntry entry = new ZipEntry(name);
                zos.putNextEntry(entry);
                if (resource instanceof IFile) {
                    IFile file = (IFile) resource;
                    entry.setComment(Boolean.toString(file.getResourceAttributes().isExecutable()));
                    InputStream is = file.getContents(true);
                    int read = 0;
                    int written = 0;
                    while ((read = is.read(block)) > 0) {
                        zos.write(block, 0, read);
                        written += read;
                    }
                    is.close();
                    if (LOGGER.isDebugEnabled()) LOGGER.debug(written + " bytes ");
                }
                zos.closeEntry();
                monitor.worked(1);
            }
            zos.flush();
            zos.close();
        } catch (IOException ioe) {
            LOGGER.error("IOException, deleting file " + fp, ioe);
            if (fp != null) fp.delete();
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IOException while compressing contents", ioe));
        } finally {
            monitor.done();
        }
        return Status.OK_STATUS;
    }
}
