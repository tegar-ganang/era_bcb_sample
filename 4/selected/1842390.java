package combasics.streamWriter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Diese Klasse ist �quivalent zu <code>java.io.PipedInputStream</code>. Die
 * Schnittstelle wurde lediglich um einen Konstruktor erweitert, der es erlaubt,
 * eine beliebige Puffergr��e anzugeben.
 * 
 * Mehrere Schreiber k�nnen in einen Strom nebenl�ufig schreiben.
 * 
 * Kommentare wurden teilweise �bersetzt.
 * 
 * @author Yark Schroeder, Manuel Scholz
 * @version $Id: PipedOutputStream.java,v 1.1 2006/03/25 21:26:23 yark Exp $
 * @since 1.3
 */
public class PipedOutputStream extends OutputStream {

    PipedInputStream sink;

    /**
	 * Erzeugt einen unverbundenen <code>PipedOutputStream</code> mit
	 * Standardpuffergr��e (0x10000 = 65536 Bytes)
	 * 
	 * @exception IOException
	 * @since 1.3
	 */
    public PipedOutputStream() throws IOException {
        this(null);
    }

    /**
	 * Erzeugt einen <code>PipedOutputStream</code> mit Standardpuffergr��e
	 * (0x10000 = 65536 Bytes) und verbindet ihn mit der �bergebenen Senke.
	 * 
	 * @exception IOException
	 *                Eine Verbindung bestand bereits
	 * @since 1.3
	 */
    public PipedOutputStream(PipedInputStream sink) throws IOException {
        this(sink, 0x10000);
    }

    /**
	 * Erzeugt einen <code>PipedOutputStream</code> mit �bergebener
	 * Puffergr��e und verbindet ihn mit der �bergebenen Senke.
	 * 
	 * @exception IOException
	 *                Eine Verbindung bestand bereits
	 * @since 1.3
	 */
    public PipedOutputStream(PipedInputStream sink, int bufferSize) throws IOException {
        if (sink != null) {
            connect(sink);
            sink.buffer = new byte[bufferSize];
        }
    }

    public void close() throws IOException {
        if (sink == null) {
            throw new IOException("Unconnected pipe");
        }
        synchronized (sink.buffer) {
            sink.closed = true;
            flush();
        }
    }

    /**
	 * Verbindet den <code>PipedOutputStream</code> mit dem �bergebenen
	 * <code>PipedInputStream</code>.
	 * 
	 * @exception IOException
	 *                Eine Verbindung bestand bereits
	 * @since 1.3
	 */
    public void connect(PipedInputStream sink) throws IOException {
        if (this.sink != null) {
            throw new IOException("Pipe already connected");
        }
        this.sink = sink;
        sink.source = this;
    }

    /**
	 * Schlie�t den <code>PipedOutputStream</code>, falls er noch ge�ffnet
	 * ist.
	 * 
	 * @since 1.3
	 */
    protected void finalize() throws Throwable {
        close();
    }

    public void flush() throws IOException {
        synchronized (sink.buffer) {
            sink.buffer.notifyAll();
        }
    }

    public void write(int b) throws IOException {
        write(new byte[] { (byte) b });
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        boolean bReady = false;
        synchronized (sink.buffer) {
            while (!bReady) {
                if (sink == null) {
                    throw new IOException("Unconnected pipe");
                }
                if (sink.closed) {
                    throw new IOException("Broken pipe");
                }
                if (sink.writePosition == sink.readPosition && sink.writeLaps > sink.readLaps) {
                    try {
                        sink.buffer.notifyAll();
                        sink.buffer.wait();
                    } catch (InterruptedException e) {
                        throw new IOException(e.getMessage());
                    }
                    continue;
                }
                int amount = Math.min(len, (sink.writePosition < sink.readPosition ? sink.readPosition : sink.buffer.length) - sink.writePosition);
                System.arraycopy(b, off, sink.buffer, sink.writePosition, amount);
                sink.writePosition += amount;
                if (sink.writePosition == sink.buffer.length) {
                    sink.writePosition = 0;
                    ++sink.writeLaps;
                }
                if (amount < len) {
                    off = off + amount;
                    len = len - amount;
                } else {
                    bReady = true;
                    sink.buffer.notifyAll();
                }
            }
        }
    }
}
