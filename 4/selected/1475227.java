package org.pubcurator.core.managers;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.pubcurator.core.Logger;

/**
 * @author Kai Schlamp (schlamp@gmx.de)
 *
 */
public class LockFileManager {

    public static LockFileManager INSTANCE = new LockFileManager();

    private static final String LOCK_FILENAME = "pubcurator.lock";

    private String instancePath;

    private FileLock fileLock;

    private LockFileManager() {
        instancePath = Platform.getInstanceLocation().getURL().getPath();
    }

    public boolean createLock() {
        try {
            File lockFile = new File(instancePath, LOCK_FILENAME);
            if (lockFile.exists() && lockFile.isFile()) {
                RandomAccessFile rw = new RandomAccessFile(lockFile, "rw");
                fileLock = rw.getChannel().tryLock();
                if (fileLock == null) {
                    Logger.log(IStatus.INFO, "Another PubCurator instance is already running.");
                    showMessage("Another PubCurator instance is already running.\nNo second instance is allowed for database consistency reasons.");
                    return false;
                } else {
                    Logger.log(IStatus.INFO, "A prior running instance of PubCurator crashed.");
                }
            } else {
                if (lockFile.getParentFile() != null && !lockFile.getParentFile().isDirectory()) {
                    lockFile.getParentFile().mkdirs();
                }
                lockFile.createNewFile();
                RandomAccessFile rw = new RandomAccessFile(lockFile, "rw");
                fileLock = rw.getChannel().lock();
            }
        } catch (Exception e) {
            Logger.log(IStatus.ERROR, "A PubCurator lock file could not be created.", e);
            showMessage("Error while trying creating a lock file.\nPlease make sure that your PubCurator directory is writable.");
            return false;
        }
        return true;
    }

    public boolean releaseLock() {
        try {
            fileLock.release();
        } catch (IOException e) {
            Logger.log(IStatus.ERROR, "The PubCurator lock file could not be released.", e);
            return false;
        }
        return true;
    }

    private void showMessage(String message) {
        Shell shell = new Shell();
        MessageDialog.openError(shell, "PubCurator Startup Error", message);
    }
}
