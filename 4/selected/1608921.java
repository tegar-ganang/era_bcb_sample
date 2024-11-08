package org.xsocket.stream;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.stream.BlockingConnection;
import org.xsocket.stream.IBlockingConnection;
import org.xsocket.stream.IConnectHandler;
import org.xsocket.stream.IConnectionScoped;
import org.xsocket.stream.IDataHandler;
import org.xsocket.stream.IServer;
import org.xsocket.stream.INonBlockingConnection;
import org.xsocket.stream.Server;
import org.xsocket.stream.IConnection.FlushMode;

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
        StreamUtils.start(server);
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

    private static class ServerHandler implements IDataHandler, IConnectHandler, IConnectionScoped {

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

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }
}
