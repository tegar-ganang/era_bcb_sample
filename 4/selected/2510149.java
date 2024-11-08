package org.sqlsplatter.tinyhorror.objects;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.SQLException;
import org.sqlsplatter.tinyhorror.other.C;

/**
	 * Business delegate pattern :-)<br>
	 * 'static' because is a private nested class and not an inner one.
	 */
public class PhysTableEntity {

    private File fileHnd;

    private RandomAccessFile file;

    private FileChannel channel;

    private FileLock lock;

    private long position;

    /**
		 * Interface to physical file.<br>
		 * At instantiation time, file is created if it doesn't exist, and locked.
		 * 
		 * @param fileHnd
		 *            file handle.
		 * @throws IOException
		 *             I/O error.
		 */
    public PhysTableEntity(File fileHnd) throws IOException {
        if (!fileHnd.exists()) fileHnd.createNewFile();
        this.fileHnd = fileHnd;
        file = new RandomAccessFile(fileHnd, "rw");
        channel = file.getChannel();
        lock = channel.lock();
        position = 0;
    }

    public void unlockAndClose() throws IOException {
        lock.release();
        channel.close();
        file.close();
    }

    public void delete() throws IOException {
        unlockAndClose();
        if (!fileHnd.delete()) throw new IOException("Cannot delete table file.");
    }

    public String readRecord() throws SQLException {
        try {
            String recordStr = file.readLine();
            this.position = file.getFilePointer();
            return recordStr;
        } catch (IOException e) {
            String message = "Error during record read.";
            throw (SQLException) new SQLException(message).initCause(e);
        }
    }

    public void seek(int position) throws IOException {
        file.seek(position);
        this.position = position;
    }

    public void seekStart() throws IOException {
        seek(0);
    }

    /**
		 * Go backs one record.
		 */
    public void seekBack(StringBuilder sb) throws IOException {
        seek((int) position - sb.length() - 1);
    }

    public void seekEnd() throws IOException {
        seek((int) channel.size());
    }

    /**
		 * <b>Adds LF.</b>
		 * @param str
		 * @throws IOException
		 */
    public void write(StringBuilder sb) throws IOException {
        sb.append(C.LINE_SEP);
        file.writeBytes(sb.toString());
        this.position = file.getFilePointer();
    }

    public void writeByte(int b) throws IOException {
        file.write(b);
        this.position = file.getFilePointer();
    }

    public void truncate() throws IOException {
        file.setLength(position);
    }

    public int getSize() throws IOException {
        return (int) file.length();
    }
}
