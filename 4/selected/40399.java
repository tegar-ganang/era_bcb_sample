package x360mediaserver.upnpmediaserver.upnp.formats.streamers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;

public class StreamThreader implements Runnable {

    private Thread thread = null;

    private final File file;

    private final OutputStream os;

    private BufferedInputStream is = null;

    public StreamThreader(File file, OutputStream os) {
        this.file = file;
        this.os = os;
    }

    /**
     * Copies a given file to the OutputStream (which in my case, is a PipedOutputStream)
     * 
     * @param file
     * @param os
     */
    public void writeToStream() {
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            byte input[] = new byte[4096];
            int bytesread;
            while ((bytesread = is.read(input)) != -1) os.write(input, 0, bytesread);
        } catch (Exception e) {
            if (!e.getMessage().equals("Pipe closed")) System.err.println("StreamThreader error: " + e.toString());
        } finally {
            stop();
        }
    }

    /**
     * Start the file reader.
     */
    public synchronized void start() {
        if (thread == null) thread = new Thread(this, "File Reader");
        thread.start();
    }

    /**
     * Stop the file reader.
     */
    public void stop() {
        if (is != null) try {
            is.close();
            is = null;
        } catch (Exception e) {
        }
        if (thread != null) thread = null;
    }

    /**
     * This is the run method and runs when we start playing a file.
     */
    public void run() {
        try {
            while (thread != null) {
                writeToStream();
            }
        } catch (OutOfMemoryError e) {
            System.out.println("ERROR: RAN OUT OF MEMORY");
            stop();
        }
    }
}
