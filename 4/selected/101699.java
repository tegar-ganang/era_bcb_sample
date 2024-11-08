package com.go.trove.io;

import java.io.OutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;

/******************************************************************************
 * A ByteData implementation that reads the contents of a file.
 *
 * @author Brian S O'Neill
 * @version
 * <!--$$Revision: 3 $-->, <!--$$JustDate:--> 00/12/06 <!-- $-->
 */
public class FileByteData implements ByteData {

    private static final Object NULL = new Object();

    private File mFile;

    private ThreadLocal mRAF = new ThreadLocal();

    public FileByteData(File file) {
        mFile = file;
        open();
    }

    public long getByteCount() throws IOException {
        RandomAccessFile raf = open();
        if (raf == null) {
            return 0;
        } else {
            return raf.length();
        }
    }

    public void writeTo(OutputStream out) throws IOException {
        RandomAccessFile raf = open();
        if (raf == null) {
            return;
        }
        try {
            long length = raf.length();
            int bufSize;
            if (length > 4000) {
                bufSize = 4000;
            } else {
                bufSize = (int) length;
            }
            byte[] inputBuffer = new byte[bufSize];
            raf.seek(0);
            int readAmount;
            while ((readAmount = raf.read(inputBuffer, 0, bufSize)) > 0) {
                out.write(inputBuffer, 0, readAmount);
            }
        } finally {
            try {
                finalize();
            } catch (IOException e) {
            }
        }
    }

    public void reset() throws IOException {
        Object obj = mRAF.get();
        try {
            if (obj instanceof RandomAccessFile) {
                ((RandomAccessFile) obj).close();
            }
        } finally {
            mRAF.set(null);
        }
    }

    protected final void finalize() throws IOException {
        reset();
    }

    private RandomAccessFile open() {
        Object obj = mRAF.get();
        if (obj instanceof RandomAccessFile) {
            return (RandomAccessFile) obj;
        } else if (obj == NULL) {
            return null;
        }
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(mFile, "r");
            mRAF.set(raf);
        } catch (IOException e) {
            mRAF.set(NULL);
            Thread t = Thread.currentThread();
            t.getThreadGroup().uncaughtException(t, e);
        }
        return raf;
    }
}
