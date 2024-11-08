package org.xsocket.connection;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.Server;
import org.xsocket.connection.ConnectionUtils;

public class ReadByteBufferTest {

    private static final String DELIMITER = "\r";

    @Test
    public void testReadByLengthNegativeArgument() throws Exception {
        IServer server = new Server(new EchoHandler());
        ConnectionUtils.start(server);
        INonBlockingConnection connection = new NonBlockingConnection("localhost", server.getLocalPort());
        connection.write("test" + EchoHandler.DELIMITER);
        QAUtil.sleep(1000);
        byte[] bytes = connection.readBytesByLength(-1);
        Assert.assertArrayEquals(new byte[0], bytes);
        connection.close();
        server.close();
    }

    @Test
    public void testBlockingConnection() throws Exception {
        IServer server = new Server(new Handler());
        ConnectionUtils.start(server);
        IBlockingConnection connection = new BlockingConnection("localhost", server.getLocalPort());
        connection.setAutoflush(false);
        String s = "test123";
        ByteBuffer request = ByteBuffer.wrap(s.getBytes());
        connection.write(request);
        connection.write(DELIMITER);
        connection.flush();
        ByteBuffer response = ByteBuffer.allocate(request.capacity() + 30);
        connection.read(response);
        Assert.assertTrue(response.position() > 0);
        Assert.assertTrue(response.get(0) == request.get(0));
        connection.close();
        server.close();
    }

    @Test
    public void testBlockingNoBytesConnection() throws Exception {
        IServer server = new Server(new DelayHandler(500));
        ConnectionUtils.start(server);
        IBlockingConnection connection = new BlockingConnection("localhost", server.getLocalPort());
        connection.setAutoflush(false);
        connection.setReadTimeoutMillis(200);
        String s = "test123";
        ByteBuffer request = ByteBuffer.wrap(s.getBytes());
        connection.write(request);
        connection.write(DELIMITER);
        connection.flush();
        ByteBuffer response = ByteBuffer.allocate(request.capacity());
        try {
            connection.read(response);
            Assert.fail("timeout exception should haven been thrown");
        } catch (SocketTimeoutException expected) {
        }
        ;
        QAUtil.sleep(1000);
        connection.read(response);
        Assert.assertTrue(QAUtil.isEquals(request, response));
        connection.close();
        server.close();
    }

    @Test
    public void testNonBlockingConnection() throws Exception {
        IServer server = new Server(new Handler());
        server.start();
        INonBlockingConnection connection = new NonBlockingConnection("localhost", server.getLocalPort());
        connection.setAutoflush(false);
        String s = "test123";
        ByteBuffer request = ByteBuffer.wrap(s.getBytes());
        connection.write(request);
        connection.write(DELIMITER);
        connection.flush();
        QAUtil.sleep(2000);
        ByteBuffer response = ByteBuffer.allocate(request.capacity());
        connection.read(response);
        Assert.assertTrue(response.position() == request.position());
        Assert.assertTrue(QAUtil.isEquals(response, request));
        connection.close();
        server.close();
    }

    private static final class Handler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.setAutoflush(false);
            connection.write(connection.readByteBufferByDelimiter(DELIMITER));
            connection.write(DELIMITER);
            connection.flush();
            return true;
        }
    }

    private static final class DelayHandler implements IDataHandler {

        private int delay = 0;

        DelayHandler(int delay) {
            this.delay = delay;
        }

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.setAutoflush(false);
            connection.write(connection.readByteBufferByDelimiter(DELIMITER));
            connection.write(DELIMITER);
            QAUtil.sleep(delay);
            connection.flush();
            return true;
        }
    }
}
