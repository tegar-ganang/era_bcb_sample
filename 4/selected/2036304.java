package com.astrel.io.atomic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

public abstract class Action implements Serializable {

    private transient Transaction txn;

    void setTransaction(Transaction t) {
        txn = t;
    }

    /** Prepare for the action by storing all information required
			for undo in non-transient instance variables.  
			<i>This method should not change the state of the filesystem.</i>
      After this call, the Action object will be serialized into the journal.
			Exceptions are considered transaction errors, and will result in a 
			rollback.
	*/
    protected abstract void prepare() throws IOException;

    /** Create a backup file, if necessary. 
			Exceptions cause rollback.
	*/
    protected void createBackup() throws IOException {
    }

    /** Do the action.  Exceptions are reported to the user as is.  */
    protected abstract Object execute() throws IOException;

    /** Called at commit or rollback, only if action was successful. */
    protected void close() throws IOException {
    }

    /** Undo the effects of the action, if those effects are present.  
			This must be <i>locally idempotent</i>; it must be able to be
			invoked multiple times with the same results, assuming no other
			changes to the relevant files are made between invocations. 
			<p>
			Exceptions thrown from this method are considered
			to leave the system in an inconsistent state. 
	*/
    protected abstract void undo() throws IOException;

    /** Delete backup and perform other needed cleanup.
			Called after commit and rollback.
			Exceptions thrown during cleanup do not prevent commit or rollback
			from succeeding.  They are not thrown out of Transaction methods;
			instead, you can get them by calling Transaction.getCleanupExceptions.
			@see Transaction#getCleanupExceptions
	*/
    protected void cleanup() throws IOException {
    }

    private static int uniqueFileNum = 0;

    protected File generateBackupFilename(File f) {
        File backupDir = txn.getTransactionManager().getBackupDir();
        int n;
        File result;
        do {
            synchronized (Action.class) {
                n = uniqueFileNum++;
            }
            result = new File(backupDir, f.getName() + n + txn.getTransactionManager().getFileExtension());
        } while (result.exists());
        return result;
    }

    protected void restoreBackup(File backup, File original) throws RenameException, DeleteException {
        if (backup.exists()) renameDeleting(backup, original);
    }

    /** Delete the file.  Uses Java's semantics for delete.
			@throws DeleteException where File.delete would return false
	*/
    protected static void delete(File f) throws DeleteException {
        if (!f.delete()) throw new DeleteException(f);
    }

    /** Delete the file only if it exists. */
    protected static synchronized void deleteIfExists(File f) throws DeleteException {
        if (f.exists()) delete(f);
    }

    /** Copy source to dest, overwriting dest. */
    protected static void copyDeleting(File source, File dest) throws IOException {
        byte[] buf = new byte[8 * 1024];
        FileInputStream in = new FileInputStream(source);
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
    }

    protected static void copyNotDeleting(File source, File dest) throws IOException, FileExistsException {
        if (dest.createNewFile()) copyDeleting(source, dest); else throw new FileExistsException(dest);
    }

    /** Rename the source file to the destination file, deleting the destination 
			file if it exists.  For filesystems where a file cannot be renamed
			to an existing file, this method is not atomic.
			@throws DeleteException if the existing file could not be deleted
			@throws RenameException if the renaming failed
	*/
    protected synchronized void renameDeleting(File source, File dest) throws RenameException, DeleteException {
        if (!txn.getTransactionManager().renameCanDelete()) deleteIfExists(dest);
        rename(source, dest);
    }

    /** Rename the source file to the destination file, throwing an exception 
			if the destination file exists.
			@throws RenameException if there is an error in File.renameTo
      @throws FileExistsException if dest exists
	*/
    protected synchronized void renameNotDeleting(File source, File dest) throws IOException {
        if (!txn.getTransactionManager().renameCanDelete()) rename(source, dest); else if (dest.createNewFile()) rename(source, dest); else throw new FileExistsException(dest.toString());
    }

    private static void rename(File source, File dest) throws RenameException {
        if (!source.renameTo(dest)) throw new RenameException(source, dest);
    }
}
