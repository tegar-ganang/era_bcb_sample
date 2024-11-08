package org.pepsoft.syncranator;

import java.io.File;

/**
 * @author Pepijn Schmitz
 */
class CopyFileAction implements SyncAction {

    CopyFileAction(File file, File destDir, Direction direction, Reason reason) {
        this.file = file;
        this.destDir = destDir;
        this.direction = direction;
        this.reason = reason;
    }

    File getFile() {
        return file;
    }

    File getDestDir() {
        return destDir;
    }

    Direction getDirection() {
        return direction;
    }

    Reason getReason() {
        return reason;
    }

    public String toString() {
        return "Copying " + file + " from " + ((direction == Direction.LEFT_TO_RIGHT) ? "left to right" : "right to left") + " because it is " + ((reason == Reason.NEW) ? "new" : "newer");
    }

    public void perform(ResultList resultList, boolean continueOnError) {
        FileUtils.copyFile(file, destDir);
        resultList.addResult(new SyncResult(Result.Type.OK, "Copied " + file.getName() + " from " + file.getParent() + " to " + destDir));
    }

    private File file, destDir;

    private Direction direction;

    private Reason reason;
}
