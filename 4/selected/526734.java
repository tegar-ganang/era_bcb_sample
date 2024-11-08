package info.olteanu.utils.io;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class NIOTools {

    public static ByteBuffer readBytes(SocketChannel socket, int size) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(size);
        int k = 0;
        while (k < size) {
            int len = socket.read(b);
            if (len == -1) throw new EOFException();
            k += len;
        }
        b.flip();
        return b;
    }

    public static ByteBuffer readDataFile(String fileName) throws IOException {
        FileChannel fcData = new FileInputStream(fileName).getChannel();
        int len = (int) new File(fileName).length();
        ByteBuffer buf = ByteBuffer.allocateDirect(len);
        buf.rewind();
        int numRead = fcData.read(buf);
        buf.rewind();
        if (len != numRead) throw new IOException("Less bytes were read than expected: " + len + " - " + numRead);
        return buf;
    }
}
