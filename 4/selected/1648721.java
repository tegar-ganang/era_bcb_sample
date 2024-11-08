package org.xsocket.stream;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.stream.IDataHandler;
import org.xsocket.stream.IServer;
import org.xsocket.stream.INonBlockingConnection;
import org.xsocket.stream.Server;
import org.xsocket.stream.IConnection.FlushMode;

/**
*
* @author grro@xsocket.org
*/
public final class SingleThreadedTest {

    private static final String DELIMITER = "\r";

    @Test
    public void testSingleThreaded() throws Exception {
        Handler serverHandler = new Handler();
        IServer server = new Server(serverHandler, null);
        StreamUtils.start(server);
        IBlockingConnection connection = new BlockingConnection("localhost", server.getLocalPort());
        connection.setAutoflush(true);
        connection.write("test" + DELIMITER);
        String response = connection.readStringByDelimiter(DELIMITER);
        Assert.assertEquals("test", response);
        Assert.assertTrue(serverHandler.threadName.startsWith("xDispatcher"));
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
