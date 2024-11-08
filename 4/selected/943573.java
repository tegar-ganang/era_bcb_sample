package org.xsocket.stream.io.mina;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.stream.BlockingConnection;
import org.xsocket.stream.IBlockingConnection;
import org.xsocket.stream.IConnectHandler;
import org.xsocket.stream.IDataHandler;
import org.xsocket.stream.IMultithreadedServer;
import org.xsocket.stream.INonBlockingConnection;
import org.xsocket.stream.MultithreadedServer;
import org.xsocket.stream.StreamUtils;
import org.xsocket.stream.IConnection.FlushMode;

/**
*
* @author grro@xsocket.org
*/
public final class SingleThreadedTest {

    private static final String DELIMITER = "\r";

    @Test
    public void testSingleThreaded() throws Exception {
        System.setProperty("org.xsocket.stream.io.spi.ServerIoProviderClass", org.xsocket.stream.io.mina.MinaIoProvider.class.getName());
        Handler serverHandler = new Handler();
        IMultithreadedServer server = new MultithreadedServer(serverHandler, null);
        StreamUtils.start(server);
        IBlockingConnection connection = new BlockingConnection(server.getLocalAddress(), server.getLocalPort());
        connection.setAutoflush(true);
        connection.write("test" + DELIMITER);
        String response = connection.readStringByDelimiter(DELIMITER);
        Assert.assertEquals("test", response);
        Assert.assertTrue(serverHandler.threadName.startsWith("SocketAcceptorIoProcessor"));
        connection.close();
        server.close();
    }

    @Test
    public void testMultiThreaded() throws Exception {
        System.setProperty("org.xsocket.stream.io.spi.AcceptorClassname", org.xsocket.stream.io.mina.MinaIoProvider.class.getName());
        Handler serverHandler = new Handler();
        IMultithreadedServer server = new MultithreadedServer(serverHandler);
        StreamUtils.start(server);
        IBlockingConnection connection = new BlockingConnection(server.getLocalAddress(), server.getLocalPort());
        connection.setAutoflush(true);
        connection.write("test" + DELIMITER);
        String response = connection.readStringByDelimiter(DELIMITER);
        Assert.assertEquals("test", response);
        Assert.assertFalse(serverHandler.threadName.startsWith("SocketAcceptorIoProcessor"));
        connection.close();
        server.close();
    }

    private static final class Handler implements IConnectHandler, IDataHandler {

        private String threadName = null;

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            connection.setFlushmode(FlushMode.ASYNC);
            return false;
        }

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            threadName = Thread.currentThread().getName();
            connection.write(connection.readStringByDelimiter(DELIMITER) + DELIMITER);
            return true;
        }
    }
}
