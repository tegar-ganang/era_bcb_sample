package org.cojen.tupl;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

/**
 * 
 *
 * @author Brian S O'Neill
 */
class LockedFile implements Closeable {

    final RandomAccessFile mRaf;

    LockedFile(File file, boolean readOnly) throws IOException {
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
        }
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, readOnly ? "r" : "rw");
            FileLock lock = raf.getChannel().tryLock(0, Long.MAX_VALUE, readOnly);
            if (lock == null) {
                throw new DatabaseException("Database is open and locked by another process");
            }
        } catch (FileNotFoundException e) {
            if (readOnly) {
                raf = null;
            } else {
                throw e;
            }
        } catch (OverlappingFileLockException e) {
            throw new DatabaseException("Database is already open by current process");
        }
        mRaf = raf;
    }

    void write(DatabaseConfig config) throws IOException {
        RandomAccessFile raf = mRaf;
        if (raf == null) {
            return;
        }
        raf.setLength(0);
        Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(raf.getFD()), "UTF-8"));
        config.writeInfo(w);
        w.flush();
        raf.getFD().sync();
    }

    public void close() throws IOException {
        RandomAccessFile raf = mRaf;
        if (raf != null) {
            raf.close();
        }
    }
}
