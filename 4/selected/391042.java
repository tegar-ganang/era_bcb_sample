package net.sourceforge.processdash.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Checksum;

public class RobustFileOutputStream extends OutputStream {

    public static final String OUT_PREFIX = "tttt";

    public static final String BACKUP_PREFIX = OUT_PREFIX + "_bak_";

    boolean origFileExists;

    File outFile, backupFile, destFile;

    OutputStream out;

    Checksum checksum;

    boolean closed = false;

    boolean aborted = false;

    public RobustFileOutputStream(String destFile) throws IOException {
        this(new File(destFile));
    }

    public RobustFileOutputStream(File destFile) throws IOException {
        this(destFile, true);
    }

    public RobustFileOutputStream(File destFile, boolean createMissingParentDirectory) throws IOException {
        File parentDir = destFile.getParentFile();
        if (parentDir == null || !parentDir.isDirectory()) if ((createMissingParentDirectory && parentDir != null && parentDir.mkdirs()) == false) throw new IOException("Cannot write to file '" + destFile + "' - parent directory does not exist, " + "and could not be created.");
        String filename = destFile.getName();
        this.destFile = destFile;
        this.outFile = new File(parentDir, OUT_PREFIX + filename);
        this.backupFile = new File(parentDir, BACKUP_PREFIX + filename);
        if (destFile.isDirectory()) throw new IOException("Cannot write to file '" + destFile + "' - directory is in the way."); else if (destFile.exists() && !destFile.canWrite()) throw new IOException("Cannot write to file '" + destFile + "' - file is read-only.");
        origFileExists = destFile.isFile();
        checksum = makeChecksum();
        OutputStream outStream = new FileOutputStream(outFile);
        out = new CheckedOutputStream(outStream, checksum);
    }

    protected Checksum makeChecksum() {
        return new Adler32();
    }

    public void write(int b) throws IOException {
        out.write(b);
    }

    public void write(byte[] b) throws IOException {
        out.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void abort() throws IOException, IllegalStateException {
        if (closed) throw new IllegalStateException("File already closed"); else if (aborted) return; else aborted = true;
        out.close();
        outFile.delete();
    }

    public void close() throws IOException {
        if (aborted) throw new IllegalStateException("Output already aborted"); else if (closed) return; else closed = true;
        out.close();
        long expectedChecksum = checksum.getValue();
        long actualChecksum = FileUtils.computeChecksum(outFile, makeChecksum());
        if (expectedChecksum != actualChecksum) {
            outFile.delete();
            throw new IOException("Error writing file '" + destFile + "' - verification of written data failed.");
        }
        if (origFileExists) {
            if (backupFile.exists()) backupFile.delete();
            if (renameTo(destFile, backupFile) == false) {
                outFile.delete();
                throw new IOException("Error writing file '" + destFile + "' - could not backup original file.");
            }
        }
        if (renameTo(outFile, destFile) == false) {
            if (origFileExists) {
                if (renameTo(backupFile, destFile) == false) {
                    System.err.println("Warning - couldn't restore '" + destFile + "' from backup '" + backupFile + "'.");
                }
            }
            outFile.delete();
            throw new IOException("Error writing file '" + destFile + "'.");
        }
        if (origFileExists) backupFile.delete();
    }

    public long getChecksum() {
        return checksum.getValue();
    }

    /** Rename a file.
     * 
     * This method is a workaround based on Java bug 6213298.  Sometimes,
     * transient filesystem errors may prevent a file from being renamed.
     * This method retries the operation in the case of initial error.
     */
    protected boolean renameTo(File src, File dest) {
        if (src.renameTo(dest)) return true;
        if (src.exists() && !src.canWrite()) return false;
        if (dest.exists() && !dest.canWrite()) return false;
        for (int numTries = 0; numTries < 10; numTries++) {
            try {
                Thread.sleep(10 * (1 << (numTries / 2)));
            } catch (InterruptedException ie) {
            }
            if (src.renameTo(dest)) return true;
        }
        return false;
    }
}
