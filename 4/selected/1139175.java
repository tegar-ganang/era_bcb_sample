package jaxlib.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.EventListener;
import java.util.EventObject;
import java.util.concurrent.Callable;
import jaxlib.io.channel.FileChannels;
import jaxlib.io.channel.IOChannels;
import jaxlib.thread.AsyncTask;
import jaxlib.util.CheckArg;

/**
 * Transfers one file or stream to another.
 * <p>
 * An {@code URLTransferTask} can be interrupted by {@link Thread#interrupt() interrupting} the executing
 * thread.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: URLTransferTask.java 3036 2012-01-10 02:24:08Z joerg_wassmer $
 */
public class URLTransferTask<V> extends AsyncTask<V> {

    private final int blockSize;

    private OutputStream out;

    private URL dstUrl;

    private URLConnection dstConnection;

    private URLConnectionFactory dstConnectionFactory;

    private InputStream in;

    private URL srcUrl;

    private URLConnection srcConnection;

    private URLConnectionFactory srcConnectionFactory;

    private boolean started;

    public URLTransferTask(int blockSize, URL src, URL dst) {
        this(blockSize, src, null, dst, null);
    }

    public URLTransferTask(int blockSize, File src, File dst) throws MalformedURLException {
        this(blockSize, src.toURI().toURL(), dst.toURI().toURL());
    }

    public URLTransferTask(int blockSize, URLConnection src, URLConnection dst) {
        super();
        CheckArg.notNull(src, "src");
        CheckArg.notNull(dst, "dst");
        CheckArg.gt(blockSize, 0, "blockSize");
        this.blockSize = blockSize;
        this.dstConnection = dst;
        this.dstUrl = dst.getURL();
        this.srcConnection = src;
        this.srcUrl = src.getURL();
    }

    public URLTransferTask(int blockSize, InputStream src, OutputStream dst) {
        super();
        CheckArg.notNull(src, "src");
        CheckArg.notNull(dst, "dst");
        CheckArg.gt(blockSize, 0, "blockSize");
        this.blockSize = blockSize;
        this.in = src;
        this.out = dst;
    }

    public URLTransferTask(int blockSize, URL src, URLConnectionFactory srcConnectionFactory, URL dst, URLConnectionFactory dstConnectionFactory) {
        super();
        CheckArg.notNull(src, "src");
        CheckArg.notNull(dst, "dst");
        CheckArg.gt(blockSize, 0, "blockSize");
        this.blockSize = blockSize;
        this.dstConnectionFactory = dstConnectionFactory;
        this.dstUrl = dst;
        this.srcConnectionFactory = srcConnectionFactory;
        this.srcUrl = src;
    }

    public URLTransferTask(int blockSize, URL src, URLConnectionFactory srcConnectionFactory, OutputStream dst) {
        super();
        CheckArg.notNull(src, "src");
        CheckArg.notNull(dst, "dst");
        CheckArg.gt(blockSize, 0, "blockSize");
        this.blockSize = blockSize;
        this.out = dst;
        this.srcConnectionFactory = srcConnectionFactory;
        this.srcUrl = src;
    }

    public URLTransferTask(int blockSize, InputStream src, URL dst, URLConnectionFactory dstConnectionFactory) {
        super();
        CheckArg.notNull(src, "src");
        CheckArg.notNull(dst, "dst");
        CheckArg.gt(blockSize, 0, "blockSize");
        this.blockSize = blockSize;
        this.dstConnectionFactory = dstConnectionFactory;
        this.dstUrl = dst;
        this.in = src;
    }

    /**
   * The URL of the destination stream; {@code null} if a stream instance rather than an URL was specified
   * as constructor argument.
   *
   * @since JaXLib 1.0
   */
    public URL getDestinationURL() {
        return this.dstUrl;
    }

    /**
   * The URL of the source stream; {@code null} if a stream instance rather than an URL was specified
   * as constructor argument.
   *
   * @since JaXLib 1.0
   */
    public URL getSourceURL() {
        return this.srcUrl;
    }

    /**
   * Indicates the transfer of a block of bytes.
   * <i>This method should be called by {@link #transfer} exclusively.</i>
   * <p>
   * The default implementation tests whether the current thread is {@link Thread#interrupted() interrupted}
   * and throws an {@link InterruptedIOException} if {@code true}.
   * </p>
   *
   * @since JaXLib 1.0
   */
    protected void bytesTransferred(int count) throws IOException {
        if (Thread.interrupted()) throw new InterruptedIOException();
    }

    /**
   * Returns a stream for writing to the destination of the transfer.
   * <i>This method should be called by {@link #transfer} exclusively.</i>
   * <p>
   * Either a non-null stream must be returned or an exception be thrown.
   * </p>
   *
   * @since JaXLib 1.0
   */
    protected OutputStream connectToDestination() throws IOException {
        if (Thread.interrupted()) throw new InterruptedIOException();
        OutputStream out = this.out;
        if (out == null) {
            if (this.dstConnection == null) {
                if (this.dstConnectionFactory == null) this.dstConnection = this.dstUrl.openConnection(); else this.dstConnection = this.dstConnectionFactory.createURLConnection(this.dstUrl);
            }
            out = this.dstConnection.getOutputStream();
            this.dstConnection = null;
            this.dstConnectionFactory = null;
        }
        return out;
    }

    /**
   * Returns a stream for reading the source of the transfer.
   * <i>This method should be called by {@link #transfer} exclusively.</i>
   * <p>
   * Either a non-null stream must be returned or an exception be thrown.
   * </p>
   *
   * @since JaXLib 1.0
   */
    protected InputStream connectToSource() throws IOException {
        if (Thread.interrupted()) throw new InterruptedIOException();
        InputStream in = this.in;
        if (in == null) {
            if (this.srcConnection == null) {
                if (this.srcConnectionFactory == null) this.srcConnection = this.srcUrl.openConnection(); else this.srcConnection = this.srcConnectionFactory.createURLConnection(this.srcUrl);
            }
            in = this.srcConnection.getInputStream();
            this.srcConnection = null;
            this.srcConnectionFactory = null;
        }
        return in;
    }

    /**
   * Closes the stream and releases all references to the destination of the transfer.
   * <i>This method should be called by {@link #transfer} exclusively.</i>
   *
   * @since JaXLib 1.0
   */
    protected void disconnectFromDestination(OutputStream out) throws IOException {
        this.out = null;
        out.close();
    }

    /**
   * Closes the stream and releases all references to the source of the transfer.
   * <i>This method should be called by {@link #transfer} exclusively.</i>
   *
   * @since JaXLib 1.0
   */
    protected void disconnectFromSource(InputStream in) throws IOException {
        this.in = null;
        in.close();
    }

    /**
   * Called when an I/O error occurs.
   * <i>This method should be called by {@link #transfer} exclusively.</i>
   * <p>
   * After this call the {@link #transfer} method throws the specified exception. Implementations may
   * throw an exception for themselves.
   * </p>
   *
   * @since JaXLib 1.0
   */
    protected void ioException(InputStream in, OutputStream out, IOException ex) throws IOException {
        this.in = null;
        this.out = null;
        if (in != null) {
            try {
                in.close();
            } catch (final IOException ex2) {
            }
            in = null;
        }
        if (out != null) {
            try {
                out.close();
            } catch (final IOException ex2) {
            }
        }
    }

    /**
   * Performs the task.
   * <p>
   * The default implementation calls following methods in specified order:<br>
   * <ol>
   * <li>{@link #connectToSource()}</li>
   * <li>{@link #connectToDestination()}</li>
   * <li>{@link #transfer(InputStream,OutputStream)}</li>
   * <li>{@link #disconnectFromSource(InputStream)}</li>
   * <li>{@link #disconnectFromDestination(OutputStream)}</li>
   * </ol><br>
   * If an {@code IOException} occurs then {@link #ioException} method is called prior to throwing the
   * exception.
   * </p>
   *
   * @return
   *  this implementation returns {@code null} always.
   */
    @Override
    protected V runTask() throws IOException, Throwable {
        try {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = connectToSource();
                out = connectToDestination();
                transfer(in, out);
                disconnectFromSource(in);
                in = null;
                disconnectFromDestination(out);
                out = null;
                return null;
            } catch (final IOException ex) {
                ioException(in, out, ex);
                throw ex;
            }
        } finally {
            this.dstConnection = null;
            this.dstConnectionFactory = null;
            this.in = null;
            this.out = null;
            this.srcConnection = null;
            this.srcConnectionFactory = null;
        }
    }

    /**
   * Performs the transfer of the file's bytes.
   * <i>This method should be called by {@link #runTask()} exclusively.</i>
   * <p>
   * This method returns when all bytes of the source stream have been transferred to the destination.
   * </p>
   *
   * @since JaXLib 1.0
   */
    protected void transfer(InputStream in, OutputStream out) throws IOException {
        if (this.blockSize == 1) transferByteByByte(in, out); else if (in instanceof FileInputStream) transferFromFile(((FileInputStream) in).getChannel(), IOChannels.asWritableByteChannel(out)); else if (out instanceof FileOutputStream) transferToFile(IOChannels.asReadableByteChannel(in), ((FileOutputStream) out).getChannel()); else transferDefault(in, out);
        out.flush();
    }

    private void transferByteByByte(InputStream in, OutputStream out) throws IOException {
        while (true) {
            int b = in.read();
            if (b >= 0) {
                out.write(b);
                bytesTransferred(1);
            } else {
                break;
            }
        }
    }

    private void transferDefault(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[this.blockSize];
        while (true) {
            int step = in.read(buf, 0, buf.length);
            if (step > 0) {
                out.write(buf, 0, step);
                bytesTransferred(step);
            } else if (step < 0) {
                break;
            }
        }
    }

    private void transferFromFile(FileChannel in, WritableByteChannel out) throws IOException {
        while (true) {
            long step = FileChannels.transferToByteChannel(in, out, this.blockSize);
            if (step > 0) bytesTransferred((int) step); else break;
        }
    }

    private void transferToFile(ReadableByteChannel in, FileChannel out) throws IOException {
        while (true) {
            long step = FileChannels.transferFromByteChannel(in, out, this.blockSize);
            if (step > 0) bytesTransferred((int) step); else break;
        }
    }
}
