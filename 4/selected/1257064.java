package simtools.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.ErrorManager;
import simtools.logging.LoggingEntryByteBuffer.Handler;

public class FileBufferHandler extends Handler {

    protected File file;

    protected FileChannel channel;

    protected FileOutputStream stream;

    protected Object streamLock;

    /**
     * The constructor of the class.
	 * @param loggingOut       The file to write the logs.
     *                         If an absolute path is given, it will use it, else it will create the relative
     *                         file using the user.home properties.
	 * @throws IOException
	 */
    public FileBufferHandler(String loggingOut) throws IOException {
        File logFile = new File(loggingOut);
        if (logFile.isAbsolute()) {
            file = logFile;
        } else {
            file = new File(System.getProperty("user.home"), loggingOut);
        }
        streamLock = new Object();
        try {
            open();
        } catch (IOException e) {
            getErrorManager().error("Can not open file " + file.getAbsolutePath(), e, ErrorManager.OPEN_FAILURE);
            throw e;
        }
    }

    protected void open() throws IOException {
        synchronized (streamLock) {
            stream = new FileOutputStream(file);
            channel = stream.getChannel();
        }
    }

    protected void write(ByteBuffer bb) throws IOException {
        synchronized (streamLock) {
            if (channel != null) {
                channel.write(bb);
            }
        }
    }

    public void close() throws SecurityException {
        synchronized (streamLock) {
            channel = null;
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ix) {
                    getErrorManager().error("Can not close", ix, ErrorManager.CLOSE_FAILURE);
                }
                stream = null;
            }
        }
    }

    public void flush() {
        synchronized (streamLock) {
            try {
                stream.flush();
            } catch (IOException e) {
                getErrorManager().error("Can not flush", e, ErrorManager.FLUSH_FAILURE);
            }
        }
    }
}
