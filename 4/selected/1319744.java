package ru.adv.io.atomic;

import ru.adv.util.ErrorCodeException;
import ru.adv.util.Files;
import java.io.*;

public abstract class Action implements Serializable {

    private transient FileTransaction txn;

    void setTransaction(FileTransaction t) {
        txn = t;
    }

    /** Prepare for the action by storing all information required
	 for undo in non-transient instance variables.
	 <i>This method should not change the state of the filesystem.</i>
	 After this call, the Action object will be serialized into the journal.
	 Exceptions are considered transaction errors, and will result in a
	 rollback.
	 */
    protected abstract void prepare() throws ErrorCodeException;

    /** Create a backup file, if necessary.
	 Exceptions cause rollback.
	 */
    protected void createBackup() throws ErrorCodeException {
    }

    /** Do the action.  Exceptions are reported to the user as is.  */
    protected abstract Object execute() throws ErrorCodeException;

    /** Called at commit or rollback, only if action was successful. */
    protected void close() throws ErrorCodeException {
    }

    /** Undo the effects of the action, if those effects are present.
	 This must be <i>locally idempotent</i>; it must be able to be
	 invoked multiple times with the same results, assuming no other
	 changes to the relevant files are made between invocations.
	 <p>
	 Exceptions thrown from this method are considered
	 to leave the system in an inconsistent state.
	 */
    protected abstract void undo() throws ErrorCodeException;

    /** Delete backup and perform other needed cleanup.
	 Called after commit and rollback.
	 Exceptions thrown during cleanup do not prevent commit or rollback
	 from succeeding.  They are not thrown out of Transaction methods;
	 instead, you can get them by calling Transaction.getCleanupExceptions.
	 */
    protected void cleanup() throws ErrorCodeException {
    }

    private static int uniqueFileNum = 0;

    protected File generateBackupFilename(File f) {
        int n;
        File result;
        do {
            synchronized (Action.class) {
                n = uniqueFileNum++;
            }
            result = new File(f.getAbsolutePath() + n + txn.getTransactionManager().getFileExtension());
        } while (result.exists());
        txn.debug("generateBackupFilename(): result=" + result);
        return result;
    }

    protected void restoreBackup(File backup, File original) throws RenameException, DeleteException {
        if (backup.exists()) renameDeleting(backup, original);
    }

    /** Delete the file.  Uses Java's semantics for delete.
	 @throws ru.adv.io.atomic.DeleteException where File.delete would return false
	 */
    protected void delete(File f) throws DeleteException {
        txn.debug("delete(): f=" + f);
        if (!Files.remove(f, true)) {
            throw new DeleteException(f);
        }
    }

    /** Delete the file only if it exists. */
    protected synchronized void deleteIfExists(File f) throws DeleteException {
        if (txn != null && f != null) {
            txn.debug("deleteIfExists(): f=" + f);
            txn.debug("deleteIfExists(): f.exists()=" + f.exists());
            if (f.exists()) {
                delete(f);
            }
        }
    }

    /** Copy source to dest, overwriting dest. */
    protected static void copyDeleting(File source, File dest) throws ErrorCodeException {
        byte[] buf = new byte[8 * 1024];
        FileInputStream in = null;
        try {
            in = new FileInputStream(source);
            try {
                FileOutputStream out = new FileOutputStream(dest);
                try {
                    int count;
                    while ((count = in.read(buf)) >= 0) out.write(buf, 0, count);
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            throw new ErrorCodeException(e);
        }
    }

    protected static void copyNotDeleting(File source, File dest) throws ErrorCodeException {
        try {
            if (dest.createNewFile()) copyDeleting(source, dest); else throw new FileExistsException(dest);
        } catch (IOException e) {
            throw new ErrorCodeException(e);
        }
    }

    /** Rename the source file to the destination file, deleting the destination
	 file if it exists.  For filesystems where a file cannot be renamed
	 to an existing file, this method is not atomic.
	 @throws ru.adv.io.atomic.DeleteException if the existing file could not be deleted
	 @throws ru.adv.io.atomic.RenameException if the renaming failed
	 */
    protected synchronized void renameDeleting(File source, File dest) throws RenameException, DeleteException {
        if (!txn.getTransactionManager().renameCanDelete()) deleteIfExists(dest);
        rename(source, dest);
    }

    /** Rename the source file to the destination file, throwing an exception
	 if the destination file exists.
	 @throws ru.adv.io.atomic.RenameException if there is an error in File.renameTo
	 @throws ru.adv.io.atomic.FileExistsException if dest exists
	 */
    protected synchronized void renameNotDeleting(File source, File dest) throws ErrorCodeException {
        try {
            txn.debug("renameNotDeleting(): source=" + source);
            txn.debug("renameNotDeleting(): dest=" + dest);
            txn.debug("renameNotDeleting(): renameCanDelete()=" + txn.getTransactionManager().renameCanDelete());
            if (!txn.getTransactionManager().renameCanDelete()) {
                rename(source, dest);
            } else if (dest.createNewFile()) {
                rename(source, dest);
            } else {
                throw new FileExistsException(dest.toString());
            }
        } catch (IOException e) {
            throw new ErrorCodeException(e);
        }
    }

    private void rename(File source, File dest) throws RenameException {
        txn.debug("rename(): source=" + source);
        txn.debug("rename(): dest=" + dest);
        if (!source.renameTo(dest)) {
            throw new RenameException(source, dest);
        }
    }

    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }

    protected abstract void destroy();
}
