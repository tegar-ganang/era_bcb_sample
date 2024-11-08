package org.xsocket.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;

/**
*
* @author grro@xsocket.org
*/
public final class NotifyAllClientsTest {

    @Test
    public void testSimple() throws Exception {
        Server server = new Server(new ServerHandler());
        server.start();
        List<IBlockingConnection> connections = new ArrayList<IBlockingConnection>();
        for (int i = 0; i < 15; i++) {
            IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
            for (int j = 0; j < 10; j++) {
                con.write("test\r\n");
                Assert.assertEquals("test", con.readStringByDelimiter("\r\n"));
            }
        }
        for (INonBlockingConnection serverCon : server.getOpenConnections()) {
            synchronized (serverCon) {
                serverCon.write("notify\r\n");
            }
        }
        for (IBlockingConnection connection : connections) {
            Assert.assertEquals("notify", connection.readStringByDelimiter("\r\n"));
            connection.close();
        }
        server.close();
    }

    private static final class ServerHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException {
            synchronized (connection) {
                connection.write(connection.readStringByDelimiter("\r\n") + "\r\n");
            }
            return true;
        }
    }
}
