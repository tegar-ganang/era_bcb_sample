package org.xsocket.connection;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import junit.framework.Assert;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;

/**
*
* @author grro@xsocket.org
*/
public final class MaxConnectionSizeTest {

    @Test
    public void testSimple() throws Exception {
        Server server = new Server(new Handler());
        server.setMaxConcurrentConnections(2);
        server.start();
        ConnectionUtils.registerMBean(server);
        IBlockingConnection connection1 = new BlockingConnection("localhost", server.getLocalPort());
        IBlockingConnection connection2 = new BlockingConnection("localhost", server.getLocalPort());
        IBlockingConnection connection3 = new BlockingConnection("localhost", server.getLocalPort());
        IBlockingConnection connection4 = new BlockingConnection("localhost", server.getLocalPort());
        connection1.write("Test\r\n");
        String s = connection1.readStringByDelimiter("\r\n");
        Assert.assertEquals("Test", s);
        connection2.write("Test\r\n");
        s = connection2.readStringByDelimiter("\r\n");
        Assert.assertEquals("Test", s);
        connection3.setReadTimeoutMillis(500);
        connection3.write("Test\r\n");
        try {
            s = connection3.readStringByDelimiter("\r\n");
            Assert.fail("SocketTimeoutException expected");
        } catch (SocketTimeoutException expected) {
        }
        connection1.close();
        connection2.close();
        connection3.close();
        connection4.write("Test\r\n");
        s = connection4.readStringByDelimiter("\r\n");
        Assert.assertEquals("Test", s);
        connection4.close();
        server.close();
    }

    private static final class Handler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            connection.write(connection.readStringByDelimiter("\r\n") + "\r\n");
            return true;
        }
    }
}
