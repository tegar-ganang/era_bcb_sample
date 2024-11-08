package org.xsocket.connection;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.logging.Level;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.Server;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IConnection.FlushMode;

/**
*
* @author grro@xsocket.org
*/
public final class ReducedTransferRateTest {

    @Test
    public void testClientSideData() throws Exception {
        IServer server = new Server(new Handler());
        ConnectionUtils.start(server);
        int size = 1000;
        byte[] data = QAUtil.generateByteArray(size);
        INonBlockingConnection connection = new NonBlockingConnection("localhost", server.getLocalPort());
        connection.setAutoflush(true);
        connection.setFlushmode(FlushMode.ASYNC);
        connection.setWriteTransferRate(550);
        connection.write(data);
        QAUtil.sleep(3000);
        Assert.assertTrue(QAUtil.isEquals(data, connection.readBytesByLength(size)));
        connection.close();
        server.close();
    }

    @Test
    public void testServerSideRate() throws Exception {
        IServer server = new Server(new TestHandler());
        ConnectionUtils.start(server);
        IBlockingConnection connection = new BlockingConnection("localhost", server.getLocalPort());
        connection.setAutoflush(true);
        send(connection, 3, 2000, 3700);
        send(connection, 10, 200, 1800);
        send(connection, 5, 1500, 3000);
        connection.close();
        server.close();
    }

    private void send(IBlockingConnection connection, int i, long min, long max) throws IOException {
        long start = System.currentTimeMillis();
        connection.write((long) i);
        connection.readLong();
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("elapsed time " + elapsed + " (min=" + min + ", max=" + max + ")");
        Assert.assertTrue("elapsed time " + elapsed + " out of range (min=" + min + ", max=" + max + ")", (elapsed > min) && (elapsed < max));
    }

    private static final class Handler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.write(connection.readAvailableByteBuffer());
            return true;
        }
    }

    private static final class TestHandler implements IConnectHandler, IDataHandler {

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            connection.setFlushmode(FlushMode.ASYNC);
            return true;
        }

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
            long delay = connection.readLong();
            connection.setWriteTransferRate((int) delay);
            connection.write((long) delay);
            return true;
        }
    }
}
