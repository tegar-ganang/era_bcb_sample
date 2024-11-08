package net;

import util.FileUtils;
import util.ITask;

/**
 * @author Michael Kurz
 * @created 10. Juni 2002
 * @version $Revision:
 */
public class TFinishedDownloadHandler implements ITask, Runnable, IClientConnectionListener {

    /**
     * Constructor for the TFinishedDownloadHandler object
     */
    public TFinishedDownloadHandler() {
    }

    /**
     * Description of the Method
     */
    public void runTask() {
        throw new java.lang.UnsupportedOperationException("Methode runTask() noch nicht implementiert.");
    }

    /**
     * Main processing method for the TFinishedDownloadHandler object
     */
    public void run() {
        throw new java.lang.UnsupportedOperationException("Methode run() noch nicht implementiert.");
    }

    /**
     * From IClientConnectionListener. No use here.
     *
     * @param connection Description of the Parameter
     */
    public void disconnected(TClientConnection connection) {
        throw new java.lang.UnsupportedOperationException("Methode disconnected() noch nicht implementiert.");
    }

    /**
     * From IClientConnectionListener. No use here.
     *
     * @param connection Description of the Parameter
     */
    public void stateChanged(TClientConnection connection) {
        throw new java.lang.UnsupportedOperationException("Methode stateChanged() noch nicht implementiert.");
    }

    /**
     * From IClientConnectionListener. This metod is used to move or copy the
     * files to another directory. If copying is enabled the file ist first
     * copied to the destination, afterwards it is moved tho the other
     * destination.
     *
     * @param connection Description of the Parameter
     */
    public void downloadComplete(TClientConnection connection) {
        String moveDir = Settings.getInstance().getMoveDir();
        String copyDir = Settings.getInstance().getCopyDir();
        String completeName = connection.getDownload().getLocalFilename();
        String fileName = FileUtils.getName(completeName);
        if (Settings.getInstance().isCopyEnabled()) {
            FileUtils.copyFile(completeName, copyDir + fileName);
        }
        if (Settings.getInstance().isMoveEnabled()) {
            FileUtils.moveFile(completeName, moveDir + fileName);
        }
        throw new java.lang.UnsupportedOperationException("Methode downloadComplete() noch nicht implementiert.");
    }

    /**
     * From IClientConnectionListener. No use here.
     *
     * @param connection Description of the Parameter
     */
    public void downloadFailed(TClientConnection connection) {
        throw new java.lang.UnsupportedOperationException("Methode downloadFailed() noch nicht implementiert.");
    }
}
