package org.xsocket.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferUnderflowException;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.QAUtil;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;

/**
*
* @author grro@xsocket.org
*/
public final class ClientLocalAddressTest {

    @Test
    public void testBlocking() throws Exception {
        int localPort = 11223;
        IServer server = new Server(new ServerHandler());
        server.start();
        BlockingConnection con = new BlockingConnection(new InetSocketAddress("localhost", server.getLocalPort()), new InetSocketAddress("localhost", localPort), true, Integer.MAX_VALUE, new HashMap<String, Object>(), null, false);
        Assert.assertEquals(localPort, con.getLocalPort());
        con.write("test\r\n");
        Assert.assertEquals("test", con.readStringByDelimiter("\r\n"));
        con.close();
        server.close();
    }

    @Test
    public void testNonBlocking() throws Exception {
        int localPort = 11228;
        IServer server = new Server(new ServerHandler());
        server.start();
        INonBlockingConnection con = new NonBlockingConnection(new InetSocketAddress("localhost", server.getLocalPort()), new InetSocketAddress("localhost", localPort), null, true, Integer.MAX_VALUE, new HashMap<String, Object>(), null, false);
        Assert.assertEquals(localPort, con.getLocalPort());
        con.write("test\r\n");
        do {
            QAUtil.sleep(200);
        } while (con.available() < 6);
        Assert.assertEquals("test", con.readStringByDelimiter("\r\n"));
        con.close();
        server.close();
    }

    private static final class ServerHandler implements IConnectHandler, IDataHandler {

        private INonBlockingConnection con;

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            con = connection;
            return true;
        }

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
            connection.write(connection.readByteBufferByLength(connection.available()));
            return true;
        }

        INonBlockingConnection getCon() {
            return con;
        }
    }
}
