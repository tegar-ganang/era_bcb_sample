package supersync.fileManager;

import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import org.jdesktop.application.ResourceMap;
import supersync.file.AbstractFile;
import supersync.sync.Logger;
import supersync.ui.ConfirmOverwriteForm;

/** This class can be used to schedule a copy operation with the file manager.  It can also prompt the user if files will be overwritten.
 *
 * @author Brandon Drake
 */
public class ScheduleCopyOperationThread extends Thread {

    public static final int OVERWRITE_ACTION_OVERWRITE = 1;

    public static final int OVERWRITE_ACTION_DONT_OVERWRITE = 2;

    protected static final ResourceMap resMap = org.jdesktop.application.Application.getInstance(supersync.SynchronizerApp.class).getContext().getResourceMap(ScheduleCopyOperationThread.class);

    protected final boolean includeHiddenFiles;

    protected boolean promptOnOverwrite = true;

    protected final AbstractFile fromFolder;

    protected final AbstractFile toFolder;

    protected final FileManager fileManager;

    protected int defaultOverwriteAction = OVERWRITE_ACTION_OVERWRITE;

    /** This exception is thrown when the user cancels an operation.
     */
    public class CancelOperationException extends Exception {
    }

    /** Gets a copy operation for the two folders and all sub items.  This will overwrite files by default.
     */
    public static SimpleFileOp getCopyOperation(AbstractFile l_copyFrom, AbstractFile l_copyTo, boolean l_includeHidden) throws IOException, CancelOperationException {
        ScheduleCopyOperationThread thread = new ScheduleCopyOperationThread(l_copyFrom, l_copyTo, l_includeHidden, null, false);
        return thread.getCopyOperation(l_copyFrom, l_copyTo);
    }

    /** Gets a copy operation for the folder and all sub items. Returns null if no operation should be performed (like if the operation would overwrite a file).
     *
     * @param l_copyFrom The folder to copy from.
     * @param l_copyTo The folder to copy to.
     * @param l_incudeHidden Set this to true to include hidden files in the copy operation.
     */
    public SimpleFileOp getCopyOperation(AbstractFile l_copyFrom, AbstractFile l_copyTo) throws IOException, CancelOperationException {
        boolean isDirectory = l_copyFrom.isDirectory();
        boolean copyToExists = l_copyTo.exists();
        if (false == isDirectory && copyToExists) {
            int overwriteAction = getOverwriteAction(l_copyTo.getFullPathName());
            if (OVERWRITE_ACTION_OVERWRITE != overwriteAction) {
                return null;
            }
        }
        ArrayList<SimpleFileOp> subOperations = null;
        if (isDirectory) {
            AbstractFile[] files = l_copyFrom.listFiles();
            subOperations = new ArrayList<SimpleFileOp>(files.length);
            for (AbstractFile file : files) {
                if (includeHiddenFiles || false == file.isHidden()) {
                    AbstractFile copyToFile = l_copyTo.child(file.getName());
                    if (false == file.isDirectory()) {
                        if (copyToFile.exists()) {
                            int overwriteAction = getOverwriteAction(file.getFullPathName());
                            if (OVERWRITE_ACTION_OVERWRITE != overwriteAction) {
                                continue;
                            }
                        }
                        subOperations.add(new SimpleFileOp_Copy(file, copyToFile));
                    } else {
                        SimpleFileOp newOp = getCopyOperation(file, copyToFile);
                        if (null != newOp) {
                            subOperations.add(newOp);
                        }
                    }
                }
            }
        }
        SimpleFileOp result;
        if (isDirectory && copyToExists) {
            result = new SimpleFileOp_None();
        } else {
            result = new SimpleFileOp_Copy(l_copyFrom, l_copyTo);
        }
        if (null != subOperations && 0 != subOperations.size()) {
            result.childOperations = subOperations.toArray(new SimpleFileOp[subOperations.size()]);
        }
        return result;
    }

    /** Gets the default action to take when we would overwrite a file by copying it.
     */
    public int getDefaultOverwriteAction() {
        return defaultOverwriteAction;
    }

    /** Gets the operation to take when we come to a file that will be overwritten.
     */
    protected int getOverwriteAction(String l_fileName) throws CancelOperationException {
        if (false == this.promptOnOverwrite) {
            return this.defaultOverwriteAction;
        }
        ConfirmOverwriteForm confirmForm = new ConfirmOverwriteForm(null, true);
        confirmForm.setFileName(l_fileName);
        confirmForm.askUserToOverwrite();
        int overwriteAction = 0;
        switch(confirmForm.getReturnState()) {
            case JOptionPane.CANCEL_OPTION:
                throw new CancelOperationException();
            case JOptionPane.YES_OPTION:
                overwriteAction = OVERWRITE_ACTION_OVERWRITE;
                break;
            case JOptionPane.NO_OPTION:
                overwriteAction = OVERWRITE_ACTION_DONT_OVERWRITE;
                break;
        }
        if (confirmForm.doSameForRest()) {
            this.defaultOverwriteAction = overwriteAction;
            this.promptOnOverwrite = false;
        }
        return overwriteAction;
    }

    @Override
    public void run() {
        scheduleOperation();
    }

    /** Schedules the operation and returns when complete.  Use the start() method if you just want to start the scheduling thread and return to what you were doing.
     */
    public void scheduleOperation() {
        SimpleFileOp operation;
        try {
            operation = getCopyOperation(fromFolder, toFolder);
        } catch (IOException ex) {
            fileManager.logger.logDebugError(ex);
            fileManager.logger.Log(resMap.getString("message.unableToSetupCopyOperation.text", ex.getLocalizedMessage()), Logger.LogLevel.ERROR);
            return;
        } catch (CancelOperationException ex) {
            return;
        }
        if (null != operation) {
            fileManager.addOperation(operation);
        }
    }

    /** Constructor.
     */
    public ScheduleCopyOperationThread(AbstractFile l_fromFolder, AbstractFile l_toFolder, boolean l_includeHiddenFiles, FileManager l_fileManager, boolean l_promptOnOverwrite) {
        this.fileManager = l_fileManager;
        this.fromFolder = l_fromFolder;
        this.includeHiddenFiles = l_includeHiddenFiles;
        this.promptOnOverwrite = l_promptOnOverwrite;
        this.toFolder = l_toFolder;
    }

    /** Sets the default action to take when we would overwrite a file by copying it.
     */
    public void setDefaultOverwriteAction(int defaultOverwriteAction) {
        this.defaultOverwriteAction = defaultOverwriteAction;
    }
}
