package org.xsocket.connection;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.junit.Test;
import org.xsocket.QAUtil;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;
import org.xsocket.connection.IConnection.FlushMode;

/**
*
* @author grro@xsocket.org
*/
public final class BlockingWriteTest {

    @Test
    public void testSimple() throws Exception {
        IServer server = new Server(new EchoHandler());
        server.start();
        File file = QAUtil.createTestfile_400k();
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.setFlushmode(FlushMode.SYNC);
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer transferBuffer = ByteBuffer.allocate(4096);
        int read = 0;
        do {
            transferBuffer.clear();
            read = channel.read(transferBuffer);
            transferBuffer.flip();
            if (read > 0) {
                con.write(transferBuffer);
            }
        } while (read > 0);
        channel.close();
        raf.close();
        File tempFile = QAUtil.createTempfile();
        RandomAccessFile raf2 = new RandomAccessFile(tempFile, "rw");
        FileChannel fc2 = raf2.getChannel();
        con.transferTo(fc2, (int) file.length());
        fc2.close();
        raf2.close();
        QAUtil.isEquals(file, tempFile);
        file.delete();
        tempFile.delete();
        con.close();
        server.close();
    }

    private static final class EchoHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
            connection.write(connection.readByteBufferByLength(connection.available()));
            return true;
        }
    }
}
