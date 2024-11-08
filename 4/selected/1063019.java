package expectj;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * This class is responsible for piping the output of one stream to the
 * other. Optionally it also copies the content to standard out or
 * standard err
 *
 * @author	Sachin Shekar Shetty  
 * @version 1.0
 */
class StreamPiper extends Thread implements Runnable {

    private InputStream pi = null;

    private OutputStream po = null;

    private PrintStream copyStream = null;

    volatile boolean stopPiping = false;

    volatile boolean continueProcessing = true;

    private Debugger debug = new Debugger("StreamPiper", true);

    /**
     * Constructor
     *
     * @param copyStream Stream to copy the contents to before piping
     * the data to another stream. When this parameter is null, it does
     * not copy the contents
     * @param pi Input stream to read the data
     * @param po Output stream to write the data
     * 
     */
    StreamPiper(PrintStream copyStream, InputStream pi, OutputStream po) {
        this.pi = pi;
        this.po = po;
        this.copyStream = copyStream;
        this.setDaemon(true);
    }

    /**
     * This method is used to stop copying on to Standard out and err.
     * This is used after interact.
     */
    public synchronized void stopPipingToStandardOut() {
        stopPiping = true;
    }

    synchronized void startPipingToStandardOut() {
        stopPiping = false;
    }

    /** 
     * This is used to stop the thread, after the process is killed
     */
    public synchronized void stopProcessing() {
        continueProcessing = false;
    }

    /**
     * Thread method that reads from the stream and writes to the other.
     */
    public void run() {
        int ch;
        byte[] buffer = new byte[512];
        int bytes_read;
        try {
            while (continueProcessing) {
                bytes_read = pi.read(buffer);
                if (bytes_read == -1) {
                    debug.print("Closing Streams");
                    pi.close();
                    po.close();
                    return;
                }
                po.write(buffer, 0, bytes_read);
                if (copyStream != null && !stopPiping) {
                    copyStream.write(buffer, 0, bytes_read);
                    copyStream.flush();
                }
                po.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }
}
