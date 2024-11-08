package com.elibera.gateway.threading;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import com.elibera.gateway.elements.RequestBuffer;
import com.elibera.util.Log;

/**
 * @author meisi
 *
 */
public class ServerIO {

    /**
	 * diesen buffer benötige ich für readDataNonBlockingRequestCollecting()<br>
	 * gibt an wie groß der byte buffer zum lesen vom server maximal sein darf
	 */
    public static int MAX_MEMORY_BINARY_READ_BUFFER = 10 * 1024;

    public static void write(SocketChannel socket, String theOutput) throws IOException {
        if (theOutput == null) theOutput = "";
        ByteBuffer bb = ByteBuffer.wrap(theOutput.getBytes("ISO-8859-1"));
        ByteBuffer bb2 = ByteBuffer.wrap("\n".getBytes("ISO-8859-1"));
        socket.write(bb);
        socket.write(bb2);
    }

    public static void write(SocketChannel socket, byte[] data) throws IOException {
        if (data == null) data = new byte[0];
        ByteBuffer bb = ByteBuffer.wrap(data);
        socket.write(bb);
    }

    /**
	 * liest daten vom stream
	 * @param length
	 * @param in
	 * @return
	 * @throws IOException
	 */
    public static byte[] readData(RequestBuffer buffer, int length, SocketChannel s) throws IOException {
        byte[] b = new byte[length];
        int read = 0;
        ByteBuffer bb = ByteBuffer.wrap(b);
        if (buffer.isAvaliable()) {
            byte[] b2 = new byte[length];
            int rb = buffer.read(b2);
            bb.put(b2, 0, rb);
            read += rb;
        }
        while (read < length) {
            read += s.read(bb);
        }
        bb.rewind();
        return bb.array();
    }

    /**
	 * reads a normal ISO-8859-1 String Line
	 * @param is
	 * @return
	 * @throws IOException
	 */
    public static String readLine(RequestBuffer buffer, SocketChannel s, ByteBuffer bb, ByteArrayOutputStream out) throws IOException {
        out.reset();
        int read = 0;
        bb.rewind();
        while ((read = readByteFromSocketOrBuffer(buffer, s, bb)) != -1) {
            int ch = bb.get(0);
            if (read == 0) continue;
            bb.rewind();
            if ((char) ch == '\n' || (char) ch == '\r') {
                byte[] b = out.toByteArray();
                out.reset();
                return new String(b, "ISO-8859-1");
            }
            out.write(ch);
        }
        out.reset();
        return null;
    }

    /**
	 * liest ein byte entweder vom buffer oder vom socket
	 * @param buffer
	 * @param s
	 * @param bb
	 * @param out
	 * @return
	 * @throws IOException
	 */
    private static int readByteFromSocketOrBuffer(RequestBuffer buffer, SocketChannel s, ByteBuffer bb) throws IOException {
        int bread = buffer.read();
        if (bread < 0) return s.read(bb);
        bb.put((byte) bread);
        return 1;
    }

    public static String readLineNonBlocking(SocketChannel soc, ByteBuffer bb, ByteArrayOutputStream out) throws IOException, ConnectException {
        soc.configureBlocking(false);
        int read = 0;
        String ret = null;
        bb.rewind();
        while ((read = soc.read(bb)) > 0) {
            int ch = bb.get(0);
            bb.rewind();
            if ((char) ch == '\n' || (char) ch == '\r') {
                ret = new String(out.toByteArray(), "ISO-8859-1");
                break;
            }
            out.write(ch);
        }
        soc.configureBlocking(true);
        if (ret != null && ret.length() <= 0) {
            ret = null;
        }
        if (ret != null) {
            out.reset();
        }
        if (read < 0) throw new ConnectException("quite");
        return ret;
    }

    public static byte[] readDataNonBlocking(int len, SocketChannel soc, ByteBuffer bb, ByteArrayOutputStream out) throws IOException, ConnectException {
        soc.configureBlocking(false);
        int read = 0;
        byte[] ret = null;
        bb.rewind();
        while ((read = soc.read(bb)) > 0) {
            int ch = bb.get(0);
            bb.rewind();
            out.write(ch);
            if (out.size() >= len) {
                ret = out.toByteArray();
                break;
            }
        }
        soc.configureBlocking(true);
        if (ret != null) {
            out.reset();
        }
        if (read < 0) throw new ConnectException("quite");
        return ret;
    }

    /**
	 * liest die daten direkt in den request buffer, anstatt in ein byte array, das im speicher gehalten wird
	 * @param len
	 * @param soc
	 * @param bb
	 * @param out
	 * @return
	 * @throws IOException
	 * @throws ConnectException
	 */
    public static boolean readDataNonBlockingRequestCollecting(int len, SocketChannel soc, ByteBuffer bb2, RequestBuffer out) throws IOException, ConnectException {
        soc.configureBlocking(false);
        int read = 0;
        int bufferSize = len;
        if (out.requestReadByteDataRead + bufferSize > len) bufferSize = len - out.requestReadByteDataRead;
        if (bufferSize > MAX_MEMORY_BINARY_READ_BUFFER) bufferSize = MAX_MEMORY_BINARY_READ_BUFFER;
        ByteBuffer bb = ByteBuffer.wrap(new byte[bufferSize]);
        bb.rewind();
        while ((read = soc.read(bb)) > 0) {
            byte[] b = bb.array();
            bb.rewind();
            out.write(b, 0, read);
            out.requestReadByteDataRead += read;
            if (out.requestReadByteDataRead >= len) {
                out.requestReadByteDataRead = 0;
                return true;
            }
            if (out.requestReadByteDataRead + bufferSize > len) {
                bufferSize = len - out.requestReadByteDataRead;
                if (bufferSize > MAX_MEMORY_BINARY_READ_BUFFER) bufferSize = MAX_MEMORY_BINARY_READ_BUFFER;
                bb = ByteBuffer.wrap(new byte[bufferSize]);
            }
        }
        soc.configureBlocking(true);
        if (read < 0) throw new ConnectException("quite");
        return false;
    }

    /**
	 * schreibt eine gelesene Zeile von readLine() in den request buffer
	 * @param line
	 * @param out
	 */
    public static void writeLineToBuffer(String line, RequestBuffer buffer) {
        try {
            buffer.write(line.getBytes("ISO-8859-1"));
            buffer.write(10);
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    /**
	 * liest eine encodierte Zeile
	 * @param s
	 * @return
	 * @throws IOException
	 */
    public static String readEncodedLine(RequestBuffer buffer, SocketChannel s, ByteBuffer bb, ByteArrayOutputStream out, String encoding) throws IOException {
        String count = readLine(buffer, s, bb, out);
        try {
            int len = Integer.parseInt(count);
            byte[] data = null;
            try {
                data = readData(buffer, len, s);
                return new String(data, encoding);
            } catch (Exception e2) {
                return new String(data);
            }
        } catch (Exception e) {
            return count;
        }
    }
}
