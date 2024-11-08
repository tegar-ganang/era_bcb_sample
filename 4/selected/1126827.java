package org.xsocket.connection;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.IConnection.FlushMode;

/**
*
* @author grro@xsocket.org
*/
public final class OnConnectTest {

    private static final String DELIMITER = "x";

    private static final String GREETING = "helo";

    @Test
    public void testSimple() throws Exception {
        IServer server = new Server(new ServerHandler());
        ConnectionUtils.start(server);
        IBlockingConnection connection = new BlockingConnection("localhost", server.getLocalPort());
        connection.setAutoflush(true);
        String greeting = connection.readStringByDelimiter(DELIMITER, Integer.MAX_VALUE);
        Assert.assertEquals(greeting, GREETING);
        String request = "reert";
        connection.write(request + DELIMITER);
        String response = connection.readStringByDelimiter(DELIMITER, Integer.MAX_VALUE);
        Assert.assertEquals(request, response);
        connection.close();
        server.close();
    }

    private static class ServerHandler implements IDataHandler, IConnectHandler {

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            connection.setAutoflush(false);
            connection.setFlushmode(FlushMode.ASYNC);
            connection.write(GREETING + DELIMITER);
            connection.flush();
            return true;
        }

        public boolean onData(INonBlockingConnection connection) throws IOException {
            connection.write(connection.readByteBufferByDelimiter(DELIMITER, Integer.MAX_VALUE));
            connection.write(DELIMITER);
            connection.flush();
            return true;
        }
    }
}
