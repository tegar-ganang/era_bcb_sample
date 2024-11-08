package com.android.internal.util;

import java.io.File;
import java.io.IOException;

public class JournaledFile {

    File mReal;

    File mTemp;

    boolean mWriting;

    public JournaledFile(File real, File temp) {
        mReal = real;
        mTemp = temp;
    }

    /** Returns the file for you to read.
     * @more
     * Prefers the real file.  If it doesn't exist, uses the temp one, and then copies
     * it to the real one.  If there is both a real file and a temp one, assumes that the
     * temp one isn't fully written and deletes it.
     */
    public File chooseForRead() {
        File result;
        if (mReal.exists()) {
            result = mReal;
            if (mTemp.exists()) {
                mTemp.delete();
            }
        } else if (mTemp.exists()) {
            result = mTemp;
            mTemp.renameTo(mReal);
        } else {
            return mReal;
        }
        return result;
    }

    /**
     * Returns a file for you to write.
     * @more
     * If a write is already happening, throws.  In other words, you must provide your
     * own locking.
     * <p>
     * Call {@link #commit} to commit the changes, or {@link #rollback} to forget the changes.
     */
    public File chooseForWrite() {
        if (mWriting) {
            throw new IllegalStateException("uncommitted write already in progress");
        }
        if (!mReal.exists()) {
            try {
                mReal.createNewFile();
            } catch (IOException e) {
            }
        }
        if (mTemp.exists()) {
            mTemp.delete();
        }
        mWriting = true;
        return mTemp;
    }

    /**
     * Commit changes.
     */
    public void commit() {
        if (!mWriting) {
            throw new IllegalStateException("no file to commit");
        }
        mWriting = false;
        mTemp.renameTo(mReal);
    }

    /**
     * Roll back changes.
     */
    public void rollback() {
        if (!mWriting) {
            throw new IllegalStateException("no file to roll back");
        }
        mWriting = false;
        mTemp.delete();
    }
}
