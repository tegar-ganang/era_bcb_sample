package org.columba.ristretto.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Timer;
import java.util.TimerTask;

/**
 * FileSourceModel of a FileSource. Works together with
 * the FileSource to efficently wrap a File
 * in a FileSource.
 * 
 * 
 * @author tstich
 *
 */
public class FileSourceModel {

    private File file;

    private boolean isTemp;

    private FileChannel channel;

    private ByteBuffer buffer;

    private int bufferStart;

    private int references;

    private CloseChannelTimerTask closeTask;

    private static Timer timer = new Timer();

    private static final int BUFFERSIZE = 61440;

    private static final long CLOSE_DELAY = 2000;

    /**
	 * Constructs the FileSourceModel.
	 * 
	 * @param file
	 * @throws IOException
	 */
    public FileSourceModel(File file, boolean isTemp) throws IOException {
        this.file = file;
        this.isTemp = isTemp;
        buffer = ByteBuffer.allocate(BUFFERSIZE);
        bufferStart = 0;
        references = 0;
        updateBufferFromFile();
    }

    /**
	 * Get the character at the given position.
	 * 
	 * @param pos
	 * @return the character at this position
	 * @throws IOException
	 */
    public char get(int pos) throws IOException {
        if ((pos < bufferStart)) {
            while (pos < bufferStart) {
                bufferStart -= (BUFFERSIZE / 2);
            }
            updateBufferFromFile();
        } else if (pos >= bufferStart + BUFFERSIZE) {
            while (pos >= bufferStart + BUFFERSIZE) {
                bufferStart += (BUFFERSIZE / 2);
            }
            updateBufferFromFile();
        }
        byte value = buffer.get(pos - bufferStart);
        int trueValue = (value & 0x080) + (value & 0x07F);
        return (char) trueValue;
    }

    /**
	 * @throws IOException
	 */
    private void updateBufferFromFile() throws IOException {
        if (channel == null) {
            channel = new RandomAccessFile(file, "r").getChannel();
        }
        buffer.clear();
        channel.read(buffer, bufferStart);
        restartCloseTimer();
    }

    private void restartCloseTimer() {
        if (closeTask != null) closeTask.cancel();
        closeTask = new CloseChannelTimerTask(this);
        timer.schedule(closeTask, CLOSE_DELAY);
    }

    /**
	 * Get the lenght of the File.
	 * 
	 * @return the length
	 * @throws IOException
	 */
    public int length() throws IOException {
        return (int) channel.size();
    }

    /**
	 * Another FileSource is dependent on this
	 * model.
	 * 
	 */
    public void incReferences() {
        references++;
    }

    /**
	 * A dependant FileSource is closed.
	 * If none are left the FileSourceModel
	 * can be closed and the File resources
	 * can be released.
	 */
    public void decReferences() {
        references--;
        if (references <= 0) {
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Closes the FileSourceModel.
	 * 
	 * @throws IOException
	 */
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (file.exists() && isTemp) {
            if (!file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    /**
	 * @return Returns the file.
	 */
    public File getFile() {
        return file;
    }
}

class CloseChannelTimerTask extends TimerTask {

    FileSourceModel model;

    /**
	 * Constructs the CloseChannelTimerTask
	 * 
	 * @param model
	 */
    public CloseChannelTimerTask(FileSourceModel model) {
        super();
        this.model = model;
    }

    /**
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        try {
            model.close();
        } catch (IOException e) {
        }
    }
}
