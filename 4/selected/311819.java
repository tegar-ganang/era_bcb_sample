package uk.ac.reload.straker.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import uk.ac.reload.diva.util.FileUtils;

/**
 * File Utils
 * 
 * @author Phillip Beauvoir
 * @version $Id: StrakerFileUtils.java,v 1.3 2006/07/10 11:50:55 phillipus Exp $
 */
public class StrakerFileUtils {

    /**
     * Copy files and folders with a progress monitor
     * 
     * @param targetParentFolder The target parent folder
     * @param srcfiles Fully qualified source file names. If a folder, the folder name will be maintained.
     * @param shell the Shell to work in
     * @throws IOException If error or user cancelled
     */
    public static void copyFilesWithProgressMonitor(File targetParentFolder, String[] srcfiles, Shell shell) throws IOException {
        if (!targetParentFolder.isDirectory()) {
            throw new IOException("Parent folder should be directory");
        }
        ProgressMonitorDialog progress = new ProgressMonitorDialog(shell);
        CopyFilesRunnableWithProgress runnable = new CopyFilesRunnableWithProgress(targetParentFolder, srcfiles);
        try {
            progress.run(true, true, runnable);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof IOException) {
                throw (IOException) ex.getTargetException();
            }
            ex.printStackTrace();
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Copy Files Runnable task
     */
    public static class CopyFilesRunnableWithProgress implements IRunnableWithProgress {

        private File targetParentFolder;

        private String[] srcfiles;

        /**
         * Constructor
         * 
         * @param targetParentFolder The target parent folder
         * @param srcfiles Fully qualified source file names. If a folder, the folder name will be maintained.
         */
        public CopyFilesRunnableWithProgress(File targetParentFolder, String[] srcfiles) {
            this.targetParentFolder = targetParentFolder;
            this.srcfiles = srcfiles;
        }

        public void run(final IProgressMonitor monitor) throws InvocationTargetException {
            uk.ac.reload.diva.util.IProgressMonitor divaMonitor = new uk.ac.reload.diva.util.IProgressMonitor() {

                public void setNote(String name) {
                    monitor.subTask("Copying " + name);
                }

                public boolean isCanceled() {
                    return monitor.isCanceled();
                }

                public void close() {
                    monitor.done();
                }
            };
            monitor.setTaskName("Copying files...");
            for (int i = 0; i < srcfiles.length; i++) {
                File src = new File(srcfiles[i]);
                File tgt = new File(targetParentFolder, src.getName());
                if (!src.equals(tgt)) {
                    try {
                        if (src.isDirectory()) {
                            FileUtils.copyFolder(src, tgt, divaMonitor);
                        } else {
                            monitor.subTask("Copying " + src.getName());
                            FileUtils.copyFile(src, tgt);
                        }
                    } catch (IOException ex) {
                        throw new InvocationTargetException(ex);
                    }
                }
            }
        }
    }
}
