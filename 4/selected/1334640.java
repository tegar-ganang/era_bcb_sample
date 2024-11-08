package combasics.streamWriter;

import java.io.IOException;
import java.io.InputStream;

/**
 * Diese Klasse ist �quivalent zu <code>java.io.PipedInputStream</code>. Die
 * Schnittstelle wurde lediglich um einen Konstruktor erweitert, der es erlaubt,
 * eine beliebige Puffergr��e anzugeben.
 * 
 * Mehrere Leser k�nnen von einem Strom nebenl�ufig lesen.
 * 
 * Kommentare wurden teilweise �bersetzt.
 * 
 * @author Yark Schroeder, Manuel Scholz
 * @version $Id: PipedInputStream.java,v 1.1 2006/03/25 21:26:23 yark Exp $
 * @since 1.3
 */
public class PipedInputStream extends InputStream {

    byte[] buffer;

    boolean closed = false;

    int readLaps = 0;

    int readPosition = 0;

    PipedOutputStream source;

    int writeLaps = 0;

    int writePosition = 0;

    /**
	 * Erzeugt einen unverbundenen <code>PipedInputStream</code> mit
	 * Standardpuffergr��e (0x10000 = 65536 Bytes)
	 * 
	 * @exception IOException
	 * @since 1.3
	 */
    public PipedInputStream() throws IOException {
        this(null);
    }

    /**
	 * Erzeugt einen <code>PipedInputStream</code> mit Standardpuffergr��e
	 * (0x10000 = 65536 Bytes) und verbindet ihn mit der �bergebenen Quelle.
	 * 
	 * @exception IOException
	 *                Eine Verbindung bestand bereits
	 * @since 1.3
	 */
    public PipedInputStream(PipedOutputStream source) throws IOException {
        this(source, 0x10000);
    }

    /**
	 * Erzeugt einen <code>PipedInputStream</code> mit �bergebener Puffergr��e
	 * und verbindet ihn mit der �bergebenen Quelle.
	 * 
	 * @exception IOException
	 *                Eine Verbindung bestand bereits
	 * @since 1.3
	 */
    public PipedInputStream(PipedOutputStream source, int bufferSize) throws IOException {
        if (source != null) {
            connect(source);
        }
        buffer = new byte[bufferSize];
    }

    public int available() throws IOException {
        return writePosition > readPosition ? writePosition - readPosition : (writePosition < readPosition ? buffer.length - readPosition + 1 + writePosition : (writeLaps > readLaps ? buffer.length : 0));
    }

    public void close() throws IOException {
        if (source == null) {
            throw new IOException("Unconnected pipe");
        }
        synchronized (buffer) {
            closed = true;
            buffer.notifyAll();
        }
    }

    /**
	 * Verbindet den <code>PipedInputStream</code> mit dem �bergebenen
	 * <code>PipedOutputStream</code>.
	 * 
	 * @exception IOException
	 *                Eine Verbindung bestand bereits
	 * @since 1.3
	 */
    public void connect(PipedOutputStream source) throws IOException {
        if (this.source != null) {
            throw new IOException("Pipe already connected");
        }
        this.source = source;
        source.sink = this;
    }

    /**
	 * Schlie�t den <code>PipedInputStream</code>, falls er noch ge�ffnet
	 * ist.
	 * 
	 * @since 1.3
	 */
    protected void finalize() throws Throwable {
        close();
    }

    /**
	 * Wird nicht unterst�tzt.
	 * 
	 * @since 1.3
	 */
    public void mark(int readLimit) {
        return;
    }

    public boolean markSupported() {
        return false;
    }

    public int read() throws IOException {
        byte[] b = new byte[1];
        int result = read(b);
        return result == -1 ? -1 : b[0] & 0xFF;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        boolean bReady = false;
        int iRead = 0;
        synchronized (buffer) {
            while (!bReady) {
                if (source == null) {
                    throw new IOException("Unconnected pipe");
                }
                if (writePosition == readPosition && writeLaps == readLaps) {
                    if (closed) {
                        return -1;
                    }
                    try {
                        buffer.notifyAll();
                        buffer.wait();
                    } catch (InterruptedException e) {
                        throw new IOException(e.getMessage());
                    }
                    continue;
                }
                int amount = Math.min(len, (writePosition > readPosition ? writePosition : buffer.length) - readPosition);
                System.arraycopy(buffer, readPosition, b, off, amount);
                readPosition += amount;
                iRead += amount;
                if (readPosition == buffer.length) {
                    readPosition = 0;
                    ++readLaps;
                }
                if (amount < len) {
                    off = off + amount;
                    len = len - amount;
                } else {
                    bReady = true;
                    buffer.notifyAll();
                }
            }
        }
        return iRead;
    }
}
