package org.xsocket.connection;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;

/**
*
* @author grro@xsocket.org
*/
public final class DispatcherMaxHandlesTest {

    @Ignore
    @Test
    public void testSimple() throws Exception {
        System.setProperty("org.xsocket.connection.dispatcher.maxHandles", "10");
        Handler serverHandler = new Handler();
        Server server = new Server(serverHandler);
        server.start();
        List<IBlockingConnection> cons = new ArrayList<IBlockingConnection>();
        for (int i = 0; i < 100; i++) {
            IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
            cons.add(con);
            con.write("test\r\n");
            Assert.assertEquals("test", con.readStringByDelimiter("\r\n"));
        }
        Assert.assertTrue(server.getAcceptor().getDispatcherPool().getDispatcherSize() > 5);
        for (IBlockingConnection con : cons) {
            con.close();
        }
        server.close();
    }

    private static final class Handler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.write(connection.readStringByDelimiter("\r\n") + "\r\n");
            return true;
        }
    }
}
