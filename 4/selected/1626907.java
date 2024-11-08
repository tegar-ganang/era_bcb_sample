package vi.log;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 Output stream that can serve as a client to a <code>LogServer</code>.
 It communicates according to the protocol described for class
 <code>vi.log.LogServer</code>.

 @see vi.log.LogServer

 @author <a href="mailto:ivaradi@freemail.c3.hu">Istv�n V�radi</a>
 */
public class LogServerClientStream extends OutputStream {

    /**
         The default buffer size.
         */
    private static int BUFSIZE = 2048;

    /**
         The output stream to write the commands into.
         */
    private OutputStream out;

    /**
         The input stream to read the status from.
         */
    private InputStream in;

    /**
         The URL (encoded as bytes with the trailing newline).
         */
    private byte[] url;

    /**
         The buffer we use. In order to minimize the locking time we
         lock only when the buffer gets full or when the <code>flush</code>
         method is called and we need to write out.
         */
    private byte[] buf;

    /**
         The buffer index.
         */
    private int bufidx;

    /**
         Indicate if we have already locked the buffer.
         */
    private boolean locked;

    /**
         The command to use for locking.
         */
    private static byte[] lockcmd;

    /**
         The command to use for writing data.
         */
    private static byte[] datacmd;

    /**
         The command to use for unlocking.
         */
    private static byte[] unlockcmd;

    /**
         Data encoding name for server.
         */
    private static final String encname = "ASCII";

    static {
        try {
            lockcmd = LogServerThread.CMD_LOCK.getBytes(encname);
            datacmd = LogServerThread.CMD_DATA.getBytes(encname);
            unlockcmd = LogServerThread.CMD_UNLOCK.getBytes(encname);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
         Construct a log-server client stream.

         @param url     the logfile URL (currently a simple filename!)
         @param out     the output stream to write into
         @param in      the input stream to read from
         */
    public LogServerClientStream(String url, OutputStream out, InputStream in) {
        this.out = out;
        this.in = in;
        buf = new byte[BUFSIZE];
        bufidx = 0;
        locked = false;
        try {
            this.url = url.getBytes(encname);
        } catch (Exception e) {
        }
    }

    /**
         Read back and process the status.
         */
    private void processStatus() throws LogException {
        StringBuffer stbuf = new StringBuffer();
        char c;
        try {
            do {
                int chr = in.read();
                if (chr < 0) {
                    throw new LogException(LogException.ERROR_CONN);
                }
                c = (char) chr;
                if (c == LogServerThread.CHAR_CR) continue;
                if (c != LogServerThread.CHAR_EOL) stbuf.append(c);
            } while (c != LogServerThread.CHAR_EOL);
        } catch (LogException le) {
            throw le;
        } catch (IOException ie) {
            throw new LogException(LogException.ERROR_CONN, ie);
        }
        String status = stbuf.toString();
        if (status.equals(LogServerThread.STATUS_OK)) return; else if (status.equals(LogServerThread.STATUS_INVCMD)) {
            throw new LogException(LogException.ERROR_INVCMD);
        } else if (status.startsWith(LogServerThread.STATUS_OTHEREXC)) {
            throw new LogException(LogException.ERROR_OTHEREXC, status.substring(LogServerThread.STATUS_OTHEREXC.length()));
        } else {
            throw new LogException(LogException.ERROR_CONN);
        }
    }

    /**
         Lock the file.
         */
    private void lock() throws LogException {
        try {
            out.write(lockcmd);
            out.write(url);
            out.write(LogServerThread.CHAR_EOL);
            out.flush();
        } catch (IOException ie) {
            throw new LogException(LogException.ERROR_CONN, ie);
        }
        processStatus();
        locked = true;
    }

    /**
         Write the current data in the buffer.
         */
    private void writeBuffer() throws LogException {
        if (bufidx <= 0) return;
        if (!locked) lock();
        try {
            out.write(datacmd);
            out.write(String.valueOf(bufidx).getBytes(encname));
            out.write(LogServerThread.CHAR_EOL);
            out.flush();
            out.write(buf, 0, bufidx);
            out.flush();
        } catch (IOException ie) {
            throw new LogException(LogException.ERROR_CONN, ie);
        }
        processStatus();
        bufidx = 0;
    }

    /**
         Unlock the file.
         */
    private void unlock() throws LogException {
        try {
            out.write(unlockcmd);
            out.write(LogServerThread.CHAR_EOL);
            out.flush();
        } catch (IOException ie) {
            throw new LogException(LogException.ERROR_CONN, ie);
        }
        processStatus();
        locked = false;
    }

    /**
         Write into the stream.

         @param b       the byte to write
         */
    public void write(int b) throws LogException {
        if (bufidx >= buf.length) {
            writeBuffer();
        }
        buf[bufidx++] = (byte) b;
    }

    /**
         Flush the stream.
         */
    public void flush() throws LogException {
        writeBuffer();
        unlock();
    }

    /**
         Close the stream.
         */
    public void close() throws LogException {
        flush();
        try {
            out.close();
        } catch (IOException ie) {
            throw new LogException(LogException.ERROR_CONN, ie);
        }
    }
}
