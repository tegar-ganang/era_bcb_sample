package org.xsocket.server;

import java.io.IOException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xsocket.IBlockingConnection;
import org.xsocket.BlockingConnection;
import org.xsocket.INonBlockingConnection;

/**
*
* @author grro@xsocket.org
*/
public final class OnConnectTest extends AbstractServerTest {

    private static final String DELIMITER = "x";

    private static final String GREETING = "helo";

    private IMultithreadedServer server = null;

    @Before
    public void setUp() {
        server = createServer(8378, new ServerHandler());
    }

    @After
    public void tearDown() {
        server.shutdown();
    }

    @Test
    public void testSimple() throws Exception {
        setUp();
        server.setReceiveBufferPreallocationSize(3);
        IBlockingConnection connection = new BlockingConnection("127.0.0.1", server.getPort());
        String greeting = connection.receiveStringByDelimiter(DELIMITER);
        Assert.assertEquals(greeting, GREETING);
        String request = "reert";
        connection.write(request + DELIMITER);
        String response = connection.receiveStringByDelimiter(DELIMITER);
        Assert.assertEquals(request, response);
        connection.close();
        tearDown();
    }

    private static class ServerHandler implements IDataHandler, IConnectHandler, IConnectionScoped {

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            connection.write(GREETING + DELIMITER);
            return true;
        }

        public boolean onData(INonBlockingConnection connection) throws IOException {
            connection.write(connection.readAvailable());
            connection.write(DELIMITER);
            return true;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }
}
