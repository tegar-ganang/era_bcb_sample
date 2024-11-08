package org.xsocket.connection;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileChannel;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IConnection.FlushMode;

/**
*
* @author grro@xsocket.org
*/
public final class SimpleFileServerClient implements Closeable {

    private final String host;

    private final int port;

    private final BlockingConnectionPool pool = new BlockingConnectionPool();

    public SimpleFileServerClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void close() throws IOException {
        pool.close();
    }

    public void send(String name, File file) throws IOException {
        IBlockingConnection con = null;
        try {
            con = pool.getBlockingConnection(host, port);
            con.write("put\r\n");
            con.write(name + "\r\n");
            con.write((int) file.length());
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel fc = raf.getChannel();
            long size = con.transferFrom(fc);
            fc.close();
            raf.close();
            con.close();
            System.out.println("client uploaded (size=" + size + ")");
        } catch (IOException ioe) {
            if (con != null) {
                pool.destroy(con);
            }
        }
    }

    public void read(String name, File file) throws IOException {
        IBlockingConnection con = null;
        try {
            con = pool.getBlockingConnection(host, port);
            con.write("get\r\n");
            con.write(name + "\r\n");
            int size = con.readInt();
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel fc = raf.getChannel();
            long s = con.transferTo(fc, size);
            if (s != size) {
                System.out.println("WARNING written size " + s + " is not equals expected " + size);
            }
            fc.close();
            raf.close();
            System.out.println("client downloaded (size=" + s + ")");
            con.close();
        } catch (IOException ioe) {
            if (con != null) {
                pool.destroy(con);
            }
        }
    }
}
