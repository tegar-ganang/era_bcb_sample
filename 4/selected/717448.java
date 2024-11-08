package fr.x9c.cadmium.kernel;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import fr.x9c.cadmium.primitives.unix.Chmod;
import fr.x9c.cadmium.util.MemoryInputStream;

/**
 * This class implements a channel that is an abstraction over input and output
 * streams and is identified by a file descriptor (an integer) that is set to
 * <tt>-1</tt> at channel creation. <br/>
 * The channel can be based on:
 * <ul>
 *   <li>a {@link java.io.RandomAccessFile};</li>
 *   <li>an {@link java.io.InputStream};</li>
 *   <li>an {@link java.io.OutputStream};</li>
 *   <li>a {@link java.net.ServerSocket};</li>
 *   <li>a {@link java.net.Socket};</li>
 *   <li>a {@link java.net.DatagramSocket}.</li>
 * </ul>
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
public final class Channel {

    /** File descriptor for standard input. */
    public static final int STDIN = 0;

    /** File descriptor for standard output. */
    public static final int STDOUT = 1;

    /** File descriptor for standard error output. */
    public static final int STDERR = 2;

    /**
     * Open flag: read only. <br/>
     * (from "<tt>/usr/include/sys/fcntl.h</tt>")
     */
    public static final int O_RDONLY = 0x0000;

    /**
     * Open flag: write only. <br/>
     * (from "<tt>/usr/include/sys/fcntl.h</tt>")
     */
    public static final int O_WRONLY = 0x0001;

    /**
     * Open flag: append. <br/>
     * (from "<tt>/usr/include/sys/fcntl.h</tt>")
     */
    public static final int O_APPEND = 0x0008;

    /**
     * Open flag: file creation. <br/>
     * (from "<tt>/usr/include/sys/fcntl.h</tt>")
     */
    public static final int O_CREAT = 0x0200;

    /**
     * Open flag: truncate file. <br/>
     * (from "<tt>/usr/include/sys/fcntl.h</tt>")
     */
    public static final int O_TRUNC = 0x0400;

    /**
     * Open flag: exclusive. <br/>
     * (from "<tt>/usr/include/sys/fcntl.h</tt>")
     */
    public static final int O_EXCL = 0x0800;

    /**
     * Open flag: non-blocking i/o. <br/>
     * (from "<tt>/usr/include/sys/fcntl.h</tt>")
     */
    public static final int O_NONBLOCK = 0x0004;

    /**
     * Open flag: binary. <br/>
     * <b>IGNORED</b><br/>
     * (from "<tt>/usr/include/sys/fcntl.h</tt>")
     */
    public static final int O_BINARY = 0x0000;

    /**
     * Open flag: text. <br/>
     * <b>IGNORED</b><br/>
     * (from "<tt>/usr/include/sys/fcntl.h</tt>")
     */
    public static final int O_TEXT = 0x0000;

    /** Size of input buffers. */
    public static final int BUFFER_SIZE = 4096;

    /** Seek from file begin. */
    public static final int SEEK_SET = 0;

    /** Seek from file current position. */
    public static final int SEEK_CUR = 1;

    /** Seek from file end. */
    public static final int SEEK_END = 2;

    /** File descriptor. */
    private int fd;

    /** Underlying stream, if file based. */
    private final RandomAccessFile stream;

    /** Underlying stream, if memory based. */
    private final MemoryInputStream memStream;

    /** Underlying input stream. Pushable for scan-line input. */
    private PushbackInputStream inStream;

    /** Underlying ouput stream. */
    private OutputStream outStream;

    /** Underlying client socket. */
    private Socket socket;

    /** Underlying datagram socket. */
    private final DatagramSocket datagramSocket;

    /** Underlying server socket. */
    private ServerSocket serverSocket;

    /** Input facility, non-<tt>null</tt> for any channel allowing input. */
    private DataInput in;

    /** Output facility, non-<tt>null</tt> for any channel allowing output. */
    private DataOutput out;

    /**
     * Address the socket is bound to. <br/>
     * Kept outside [server] socket instance because the program will only know
     * later if the socket is a client or a server one.
     */
    private InetSocketAddress bindAddress;

    /**
     * Constructs a channel backed up by a file.
     * @param ctxt context
     * @param f file to read/write data from/to - should not be <tt>null</tt>
     * @param flg flags defining read/write
     * @param perms file permissions
     * @throws IOException in an error occurs
     */
    public Channel(final CodeRunner ctxt, final File f, final int flg, final int perms) throws IOException {
        assert ctxt != null : "null ctxt";
        assert f != null : "null f";
        this.fd = -1;
        RandomAccessFile st = null;
        if (((flg & O_EXCL) != 0) && f.exists()) {
            throw new IOException("file already exists: " + f.toString());
        }
        if (!f.exists()) {
            if ((flg & O_CREAT) != 0) {
                if (!f.createNewFile()) {
                    throw new IOException("unable to create file: " + f.toString());
                }
                try {
                    Chmod.unix_chmod(ctxt, Value.createFromBlock(Block.createString(f.getCanonicalPath())), Value.createFromLong(perms));
                } catch (final FalseExit fe) {
                } catch (final Fail.Exception fe) {
                }
            } else {
                throw new IOException("file does not exist: " + f.toString());
            }
        }
        if ((flg & O_WRONLY) != 0) {
            st = new RandomAccessFile(f, "rw");
        } else {
            st = new RandomAccessFile(f, "r");
        }
        if ((flg & O_TRUNC) != 0) {
            st.setLength(0);
        }
        if ((flg & O_APPEND) != 0) {
            st.seek(st.length());
        }
        this.stream = st;
        this.memStream = null;
        this.inStream = null;
        this.outStream = null;
        this.socket = null;
        this.datagramSocket = null;
        this.serverSocket = null;
        this.in = this.stream;
        this.out = this.stream;
        this.bindAddress = null;
    }

    /**
     * Constructs a channel backed up by an input stream.
     * @param ins input stream - should not be <tt>null</tt>
     */
    public Channel(final InputStream ins) {
        assert ins != null : "null ins";
        this.fd = -1;
        this.stream = null;
        if (ins instanceof MemoryInputStream) {
            this.memStream = (MemoryInputStream) ins;
        } else {
            this.memStream = null;
        }
        this.inStream = new PushbackInputStream(ins, BUFFER_SIZE);
        this.outStream = null;
        this.socket = null;
        this.datagramSocket = null;
        this.serverSocket = null;
        this.in = new DataInputStream(this.inStream);
        this.out = null;
        this.bindAddress = null;
    }

    /**
     * Constructs a channel backed up by an output stream.
     * @param outs output stream - should not be <tt>null</tt>
     */
    public Channel(final OutputStream outs) {
        assert outs != null : "null outs";
        this.fd = -1;
        this.stream = null;
        this.memStream = null;
        this.inStream = null;
        this.outStream = outs;
        this.socket = null;
        this.datagramSocket = null;
        this.serverSocket = null;
        this.in = null;
        this.out = new DataOutputStream(outs);
        this.bindAddress = null;
    }

    /**
     * Constructs a channel backed up by a bare socket that can become
     * either client or server.
     */
    public Channel() throws IOException {
        this.fd = -1;
        this.stream = null;
        this.memStream = null;
        this.inStream = null;
        this.outStream = null;
        this.bindAddress = null;
        this.socket = new Socket();
        this.datagramSocket = null;
        this.serverSocket = new ServerSocket();
        this.in = null;
        this.out = null;
        this.bindAddress = null;
    }

    /**
     * Constructs a channel backed up by a client socket.
     * @param s client socket - should not be <tt>null</tt> and should be both
     *          bound and connected
     */
    public Channel(final Socket s) throws IOException {
        assert s != null : "null s";
        assert s.isBound() : "s should be bound";
        assert s.isConnected() : "s should be connected";
        this.fd = -1;
        this.stream = null;
        this.memStream = null;
        this.inStream = new PushbackInputStream(s.getInputStream(), BUFFER_SIZE);
        this.outStream = s.getOutputStream();
        this.socket = s;
        this.datagramSocket = null;
        this.serverSocket = null;
        this.in = new DataInputStream(this.inStream);
        this.out = new DataOutputStream(this.outStream);
        this.bindAddress = null;
    }

    /**
     * Constructs a channel backed up by a datagram socket.
     * @param ds datagram socket - should not be <tt>null</tt>
     */
    public Channel(final DatagramSocket ds) {
        assert ds != null : "null ds";
        this.fd = -1;
        this.stream = null;
        this.memStream = null;
        this.in = null;
        this.inStream = null;
        this.out = null;
        this.outStream = null;
        this.bindAddress = null;
        this.socket = null;
        this.datagramSocket = ds;
        this.serverSocket = null;
    }

    /**
     * Returns the file descriptor.
     * @return the file descriptor
     */
    public int getFD() {
        return this.fd;
    }

    /**
     * Sets the file descriptor.
     * @param d new file descriptor
     */
    public void setFD(final int d) {
        this.fd = d;
    }

    /**
     * Returns the underlying input/output stream.
     * @return the underlying input/output stream if any,
     *         <tt>null</tt> otherwise
     */
    public RandomAccessFile asStream() {
        return this.stream;
    }

    /**
     * Returns the underlying memory input stream.
     * @return the underlying memory input stream if any,
     *         <tt>null</tt> otherwise
     */
    public MemoryInputStream asMemStream() {
        return this.memStream;
    }

    /**
     * Returns the underlying input stream.
     * @return the underlying input stream if the channel allows input,
     *         <tt>null</tt> otherwise
     */
    public PushbackInputStream asInputStream() {
        return this.inStream;
    }

    /**
     * Returns the underlying output stream.
     * @return the underlying output stream if the channel allows output,
     *         <tt>null</tt> otherwise
     */
    public OutputStream asOutputStream() {
        return this.outStream;
    }

    /**
     * Returns the underlying socket.
     * @return the underlying socket if any, <tt>null</tt> otherwise
     */
    public Socket asSocket() {
        return this.socket;
    }

    /**
     * Returns the underlying datagram socket.
     * @return the underlying datagram socket if any, <tt>null</tt> otherwise
     */
    public DatagramSocket asDatagramSocket() {
        return this.datagramSocket;
    }

    /**
     * Returns the underlying server socket.
     * @return the underlying server socket if any, <tt>null</tt> otherwise
     */
    public ServerSocket asServerSocket() {
        return this.serverSocket;
    }

    /**
     * Returns the underlying input reader.
     * @return the underlying input reader if the channel allows input,
     *         <tt>null</tt> otherwise
     */
    public DataInput asDataInput() {
        return this.in;
    }

    /**
     * Returns the underlying output writer.
     * @return the underlying output writer if the channel allows output,
     *         <tt>null</tt> otherwise
     */
    public DataOutput asDataOutput() {
        return this.out;
    }

    /**
     * Returns the address the socket is bound to.
     * @return the address the socket is bound to if any,
     *         <tt>null</tt> otherwise
     */
    public InetSocketAddress getSocketAddress() {
        return this.bindAddress;
    }

    /**
     * Closes underlying streams and set the file descriptor to <tt>-1</tt>.
     * @throws IOException if an error occurs while closing channel
     */
    public void close() throws IOException {
        this.fd = -1;
        if (this.stream != null) {
            this.stream.close();
        } else if (this.inStream != null) {
            this.inStream.close();
        } else if (this.outStream != null) {
            this.outStream.close();
        } else if (this.socket != null) {
            this.socket.close();
        } else if (this.serverSocket != null) {
            this.serverSocket.close();
        } else if (this.datagramSocket != null) {
            this.datagramSocket.close();
        }
    }

    /**
     * Returns the position of the file pointer, relatively to file begin.
     * @return the position of the file pointer, relatively to file begin
     * @throws IOException if it is not possible to get position
     */
    public long pos() throws IOException {
        if (this.stream != null) {
            return this.stream.getFilePointer();
        } else if (this.memStream != null) {
            return this.memStream.getPosition();
        } else {
            throw new IOException("unable to get channel position");
        }
    }

    /**
     * Seeks the file pointer to a given position.
     * @param pos new file pointer position
     * @param cmd seek mode:
     *            <ul>
     *              <li>{@link #SEEK_SET}: <tt>pos</tt> is relative to file begin</li>
     *              <li>{@link #SEEK_CUR}: <tt>pos</tt> is relative to current file pointer position</li>
     *              <li>{@link #SEEK_END}: <tt>pos</tt> is relative to file end</li>
     *            </ul>
     * @throws IOException if seek is not possible on channel
     */
    public long seek(final long pos, final int cmd) throws IOException {
        if (this.stream != null) {
            final long p;
            switch(cmd) {
                case Channel.SEEK_SET:
                    p = pos;
                    break;
                case Channel.SEEK_CUR:
                    p = this.stream.getFilePointer() + pos;
                    break;
                case Channel.SEEK_END:
                    p = this.stream.length() + pos;
                    break;
                default:
                    assert false : "invalid seek command";
                    p = -1;
                    break;
            }
            this.stream.seek(p);
            return this.stream.getFilePointer();
        } else if (this.memStream != null) {
            final long p;
            switch(cmd) {
                case Channel.SEEK_SET:
                    p = pos;
                    break;
                case Channel.SEEK_CUR:
                    p = this.memStream.getPosition() + pos;
                    break;
                case Channel.SEEK_END:
                    p = this.memStream.length() + pos;
                    break;
                default:
                    assert false : "invalid seek command";
                    p = -1;
                    break;
            }
            this.memStream.setPosition((int) p);
            return this.memStream.getPosition();
        } else {
            throw new IOException("unable to set channel position");
        }
    }

    /**
     * Returns the channel size, if available.
     * @return the channel size, if available
     * @throws IOException if channel size cannot be determined
     */
    public long size() throws IOException {
        if (this.stream != null) {
            return this.stream.length();
        } else if (this.memStream != null) {
            return this.memStream.length();
        } else {
            throw new IOException("unable to get channel size");
        }
    }

    /**
     * Reads up to <tt>len</tt> bytes from channel into <tt>b</tt>.
     * @param b array to put read data into - should not be <tt>null</tt>
     * @param start offset at which data is put
     * @param len maximum number of bytes to read
     * @return number of read bytes
     * @throws IOException if the channel is not an input one
     * @throws IOException if an i/o occurs
     */
    public int read(final byte[] b, final int start, final int len) throws IOException {
        assert b != null : "null b";
        if (this.stream != null) {
            return this.stream.read(b, start, len);
        } else if (asInputStream() != null) {
            return asInputStream().read(b, start, len);
        } else {
            throw new IOException("not an input channel");
        }
    }

    /**
     * Flushes output streams, if any.
     */
    public void flush() throws IOException {
        if (this.outStream != null) {
            this.outStream.flush();
        } else if (this.stream != null) {
            this.stream.getChannel().force(false);
        }
    }

    /**
     * Binds a socket. <br/>
     * If the channel is a datagram socket, it is actually bound. <br/>
     * If the channel is another type of socket, the address is saved
     * and the socket will  be bound to it when its type (client or server)
     * will be set by connection/listening.
     * @param a address to connect to - should not be <tt>null</tt>
     * @throws IOException if the channel is not a socket
     */
    public void socketBind(final InetSocketAddress a) throws IOException {
        assert a != null : "null a";
        if (this.datagramSocket != null) {
            this.datagramSocket.bind(a);
            this.bindAddress = a;
        } else if ((this.socket != null) || (this.serverSocket != null)) {
            this.bindAddress = a;
        } else {
            throw new IOException("not a socket");
        }
    }

    /**
     * Binds and then connects given socket. <br/>
     * The socket is now a client one.
     * @param a address to connect to - should not be <tt>null</tt>
     * @throws IOException if the channel is not a socket
     * @throws IOException if bind or connection fails
     */
    public void socketConnect(final InetSocketAddress a) throws IOException {
        assert a != null : "null a";
        if (this.socket != null) {
            this.socket.bind(this.bindAddress);
            this.socket.connect(a);
            this.inStream = new PushbackInputStream(this.socket.getInputStream(), BUFFER_SIZE);
            this.outStream = this.socket.getOutputStream();
            this.in = new DataInputStream(this.inStream);
            this.out = new DataOutputStream(this.outStream);
            this.serverSocket = null;
        } else {
            throw new IOException("not a socket");
        }
    }

    /**
     * Binds the socket and then listens on it. <br/>
     * The socket is now a server one.
     * @param backlog maximum number of pending connections
     * @throws IOException if the channel is not a socket
     * @throws IOException if bind or listening fails
     */
    public void socketListen(final int backlog) throws IOException {
        if (this.serverSocket != null) {
            this.serverSocket.bind(this.bindAddress, backlog);
            this.socket = null;
        } else {
            throw new IOException("not a socket");
        }
    }
}
