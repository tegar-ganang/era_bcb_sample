package org.xsocket.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;

/**
*
* @author grro@xsocket.org
*/
public final class ManyConcurrentServerTest {

    @Test
    public void testSimple() throws Exception {
        int num = 100;
        List<IServer> serverlist = new ArrayList<IServer>();
        for (int i = 0; i < num; i++) {
            IServer server = new Server(new ServerHandler());
            server.start();
            serverlist.add(server);
        }
        for (IServer server : serverlist) {
            IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
            for (int j = 0; j < 10; j++) {
                con.write("test\r\n");
                Assert.assertEquals("test", con.readStringByDelimiter("\r\n"));
            }
            con.close();
        }
        for (IServer server : serverlist) {
            server.close();
        }
        System.out.println(num + " concurrent server tested");
    }

    private static final class ServerHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException {
            connection.write(connection.readStringByDelimiter("\r\n") + "\r\n");
            return true;
        }
    }
}
