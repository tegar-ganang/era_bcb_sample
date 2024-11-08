package org.nilisoft.jftp4i.conn;

import java.io.Reader;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.PrintWriter;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import org.nilisoft.jftp4i.GenericServer;

/**
 * <p> This class is responsable to mantain the open stream pipe between the server
 * and the client. </p>
 * <p> All stream communication should be done by this class, that has the 
 * necessary methods to deal with the stream. </p>
 *
 * <p> Created on October 7, 2004, 10:20 AM </p>
 * @author  Jlopes
 */
public class JFTPStream extends GenericServer {

    /**
   * <p> Flag defining that the server should listen the client requests. </p>
   */
    public static final int PASV_ROTINE = 999;

    /**
   * <p> Flag defining that the server should connect to the client to transfer
   * data. </p>
   */
    public static final int PORT_ROTINE = 9999;

    /**
   * <p> Defines the binary FLAG to be used on the client connection. </p>
   */
    public static final int BINARY_TYPE = 1;

    /**
   * <p> Defines the ASCII FLAG to be used on the client connection. </p>
   */
    public static final int ASCII_TYPE = 2;

    /** 
   * <p> Creates a new instance of JFTPStream </p>
   */
    protected JFTPStream(BufferedReader reader, BufferedInputStream in, PrintWriter writer, BufferedOutputStream out, GenericCommunication comm) {
        super("org.nilisoft.jftp4i.conn.JFTPStream");
        this._in = in;
        this._reader = reader;
        this._writer = writer;
        this._out = out;
        this.comm = comm;
    }

    /**
   * <p> Reads one byte from the Stream. </p>
   */
    public byte readFromStream() throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            return (byte) _in.read();
        } catch (IOException ioex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ioex.toString());
        }
    }

    /**
   * <p> Reads a number n of bytes from the Stream. </p>
   * 
   * @param len The number of bytes that should read.
   */
    public byte[] readFromStream(int len) throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            byte[] bytes = new byte[len];
            _in.read(bytes, 0, len);
            return bytes;
        } catch (IOException ioex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ioex.toString());
        }
    }

    /**
   * <p> Reads one character from the Stream. </p>
   */
    public char readAscii() throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            return (char) _reader.read();
        } catch (IOException ioex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ioex.toString());
        }
    }

    /**
   * <p> Reads a number of characters from the Stream, and returns the
   * result <i>String</i>. </p>
   *
   * @param len The number of characters that should be read.
   */
    public String readAscii(int len) throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            char[] c = new char[len];
            _reader.read(c, 0, len);
            return (new String(c));
        } catch (IOException ioex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ioex.toString());
        }
    }

    /**
   * <p> Reads the characters from the Stream until the condition was supplyed
   * or the maximum number of bytes was reached <i>(1024)</i>. </p>
   *
   * @param separator The condition to stop the reading
   */
    public String readAsciiUntil(char separator) throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            char cs[] = new char[1024];
            boolean end = false;
            for (int n = 0; !end && n < cs.length; n++) {
                char c = (char) _reader.read();
                if (!(end = (c == separator))) cs[n] = c;
            }
            return new String(cs);
        } catch (IOException ioex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ioex.toString());
        }
    }

    /**
   * <p> Reads the stream until the condition is resolved. In this case, the 
   * condition is that the data read from the server has the same value
   * passed as parameter. </p>
   *
   * @param b Reads the stream until find this byte
   */
    public byte[] readStreamUntil(byte b) throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            byte bs[] = new byte[1024];
            boolean end = false;
            for (int n = 0; !end && n < bs.length; n++) {
                byte cb = (byte) _reader.read();
                if (!(end = (cb == b))) bs[n] = cb;
            }
            return bs;
        } catch (IOException ioex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ioex.toString());
        }
    }

    /**
   * <p> Reads on line from the client Stream buffer connection. </p>
   */
    public String readAsciiLine() throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            return _reader.readLine();
        } catch (IOException ioex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ioex.toString());
        }
    }

    /**
   * <P> Writes one byte into the stream. </p>
   *
   * @param out The byte to be writed
   * @param autoflush Verify if the new message will be flushed or not
   */
    public void writeToStream(byte bit, boolean autoflush) throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            _out.write(bit);
            if (autoflush) _writer.flush();
        } catch (IOException ioex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ioex.toString());
        }
    }

    /**
   * <p> Writes the byte array into the stream buffer. </p>
   *
   * @param bytes The bytes to be written
   * @param autoflush Verify if the new message will be flushed or not
   */
    public void writeToStream(byte[] bytes, boolean autoflush) throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            _out.write(bytes, 0, bytes.length);
            if (autoflush) _writer.flush();
        } catch (IOException ioex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ioex.toString());
        }
    }

    /**
   * <p> Writes a character in the Stream buffer. </p>
   *
   * @param msg The character that should be writted
   * @param autoflush Verify if the new message will be flushed or not
   */
    public void writeAscii(char c, boolean autoflush) throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            _writer.write(c);
            if (autoflush) _writer.flush();
        } catch (Exception ex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ex.toString());
        }
    }

    /**
   * <p> Writes a new <i>String</i> in the Stream buffer. </p>
   *
   * @param msg The message that should be writted
   * @param autoflush Verify if the new message will be flushed or not
   */
    public void writeAscii(String msg, boolean autoflush) throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            _writer.write(msg);
            if (autoflush) _writer.flush();
        } catch (Exception ex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ex.toString());
        }
    }

    /**
   * <p> Writes a new <i>String</i> line in the Stream buffer. </p>
   *
   * @param msg The message that should be writted
   * @param autoflush Verify if the new message will be flushed or not
   */
    public void writeAsciiLine(String msg, boolean autoflush) throws JFTPConnectionException {
        comm.updateTimemillis();
        try {
            _writer.println(msg);
            if (autoflush) _writer.flush();
        } catch (Exception ex) {
            throw new JFTPConnectionException("An IO error ocurred. Main error: " + ex.toString());
        }
    }

    /**
   * <p> Returns the client address of this connection session. </p>
   */
    public InetAddress getClientAddress() {
        return comm.getConnection().getInetAddress();
    }

    /**
   * <p> Finishs the client connection to the server. </p>
   */
    public synchronized void closeClientConnection() throws JFTPConnectionException {
        try {
            if (comm.getConnection() != null && !comm.getConnection().isClosed()) {
                this.comm.getConnection().shutdownInput();
                this.comm.getConnection().shutdownOutput();
                this.comm.getConnection().close();
            }
        } catch (IOException ioex) {
            throw new JFTPConnectionException(ioex.toString());
        }
    }

    /**
   * <p> Defines the reader out stream pipe. </p>
   */
    protected void setAsciiIn(BufferedReader reader) {
        this._reader = reader;
    }

    /**
   * <p> Returns the reader connection. </p>
   */
    protected BufferedReader getAsciiIn() {
        return this._reader;
    }

    /**
   * <p> Defines the writer out Stream pipe. </p>
   */
    protected void setAsciiOut(PrintWriter writer) {
        this._writer = writer;
    }

    /**
   * <p> Returns the writer connection. </p>
   */
    protected PrintWriter getAsciiOut() {
        return this._writer;
    }

    /**
   * <p> Defines the in out Stream pipe. </p>
   */
    protected void setStreamIn(BufferedInputStream in) {
        this._in = in;
    }

    /**
   * <p> Returns the input stream connection. </p>
   */
    protected BufferedInputStream getStreamIn() {
        return this._in;
    }

    /**
   * <p> Defines the out Stream pipe. </p>
   */
    protected void setStreamOut(BufferedOutputStream out) {
        this._out = out;
    }

    /**
   * <p> Returns the out stream connection. </p>
   */
    protected BufferedOutputStream getStreamOut() {
        return this._out;
    }

    private BufferedReader _reader;

    private BufferedInputStream _in;

    private PrintWriter _writer;

    private BufferedOutputStream _out;

    private GenericCommunication comm;
}
