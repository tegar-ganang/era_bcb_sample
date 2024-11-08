package net.sf.babble.plugins.dcc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 *
 * @author  speakmon
 */
public final class DccFileInfo {

    /** Holds value of property fileStartingPosition. */
    private long fileStartingPosition;

    /** Holds value of property bytesTransferred. */
    private long bytesTransferred;

    /** Holds value of property completeFileSize. */
    private long completeFileSize;

    private File file;

    private RandomAccessFile randomAccessFile;

    private long lastAckValue;

    /** Holds value of property channel. */
    private FileChannel channel;

    /** Holds value of property fileByteBuffer. */
    private ByteBuffer fileByteBuffer;

    /** Creates a new instance of DccFileInfo */
    public DccFileInfo(File file, long completeFileSize) {
        this.file = file;
        this.completeFileSize = completeFileSize;
        fileStartingPosition = 0;
        bytesTransferred = 0;
    }

    public DccFileInfo(File file) {
        this.file = file;
    }

    public DccFileInfo(String filename) {
        this.file = new File(filename);
    }

    /** Getter for property fileStartingPosition.
     * @return Value of property fileStartingPosition.
     *
     */
    public long getFileStartingPosition() {
        return this.fileStartingPosition;
    }

    /** Getter for property bytesTransferred.
     * @return Value of property bytesTransferred.
     *
     */
    public long getBytesTransferred() {
        return this.bytesTransferred;
    }

    /** Getter for property completeFileSize.
     * @return Value of property completeFileSize.
     *
     */
    public long getCompleteFileSize() {
        return this.completeFileSize;
    }

    /** Getter for property dccFileName.
     * @return Value of property dccFileName.
     *
     */
    public String getName() {
        return file.getName();
    }

    protected void addBytesTransferred(int additionalBytes) {
        synchronized (this) {
            bytesTransferred += additionalBytes;
        }
    }

    protected boolean acceptPositionMatches(long position) {
        return position == fileStartingPosition;
    }

    protected void gotoWritePosition() throws IOException {
        randomAccessFile.seek(fileStartingPosition + 1);
    }

    protected void gotoReadPosition() throws IOException {
        randomAccessFile.seek(fileStartingPosition);
    }

    protected boolean isResumePositionValid(long position) {
        try {
            return position > 1 && position < channel.size();
        } catch (IOException ioe) {
            return false;
        }
    }

    protected void setResumeToFileSize() throws IOException {
        fileStartingPosition = channel.size();
    }

    protected void setResumePosition(long resumePosition) {
        fileStartingPosition = resumePosition;
        bytesTransferred = fileStartingPosition;
    }

    protected long currentFilePosition() throws IOException {
        return channel.position();
    }

    protected boolean allBytesTransferred() {
        if (completeFileSize == 0) {
            return false;
        } else {
            return (fileStartingPosition + bytesTransferred) == completeFileSize;
        }
    }

    protected void close() throws IOException {
        if (file != null) {
            channel.close();
        }
    }

    protected void openForRead() throws FileNotFoundException {
        randomAccessFile = new RandomAccessFile(file, "r");
        channel = randomAccessFile.getChannel();
        fileByteBuffer = ByteBuffer.allocate(4 * 1024);
    }

    protected void openForWrite() throws FileNotFoundException {
        randomAccessFile = new RandomAccessFile(file, "rws");
        channel = randomAccessFile.getChannel();
        fileByteBuffer = ByteBuffer.allocate(64 * 1024);
    }

    protected boolean shouldResume() throws IOException {
        return channel.size() > 0;
    }

    protected boolean acksFinished(long ack) {
        boolean done = (ack == bytesTransferred || ack == lastAckValue);
        lastAckValue = ack;
        return done;
    }

    /** Getter for property channel.
     * @return Value of property channel.
     *
     */
    protected FileChannel getChannel() {
        return this.channel;
    }

    /** Getter for property fileByteBuffer.
     * @return Value of property fileByteBuffer.
     *
     */
    protected ByteBuffer getFileByteBuffer() {
        return this.fileByteBuffer;
    }
}
