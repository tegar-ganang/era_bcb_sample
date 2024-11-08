package org.xsocket.connection;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.BufferUnderflowException;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.Server;
import org.xsocket.connection.ConnectionUtils;

/**
*
* @author grro@xsocket.org
*/
public final class FragmentedDataWriteTest {

    @Test
    public void testSmall() throws Exception {
        IServer server = new Server(new EchoHandler());
        ConnectionUtils.start(server);
        IBlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());
        for (int i = 0; i < 10; i++) {
            String request = new String(QAUtil.generateByteArray(100));
            bc.write(request + "\r\n");
            String response = bc.readStringByDelimiter("\r\n");
            Assert.assertEquals(request, response);
        }
        server.close();
    }

    @Test
    public void testLarge() throws Exception {
        IServer server = new Server(new EchoHandler());
        ConnectionUtils.start(server);
        IBlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());
        for (int i = 0; i < 10; i++) {
            String request = new String(QAUtil.generateByteArray(54321));
            bc.write(request + "\r\n");
            String response = bc.readStringByDelimiter("\r\n");
            Assert.assertEquals(request, response);
        }
        server.close();
    }

    @Test
    public void testLargeFragmented() throws Exception {
        IServer server = new Server(new EchoHandler());
        ConnectionUtils.start(server);
        BlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());
        bc.setAutoflush(false);
        for (int i = 0; i < 30; i++) {
            System.out.println(i);
            String request0 = new String(QAUtil.generateByteArray(21));
            String request1 = new String(QAUtil.generateByteArray(321));
            String request2 = new String(QAUtil.generateByteArray(4321));
            String request3 = new String(QAUtil.generateByteArray(54321));
            bc.write(request0);
            bc.write(request1);
            bc.write(request2);
            bc.write(request3 + "\r\n");
            bc.flush();
            String response = null;
            try {
                response = bc.readStringByDelimiter("\r\n");
            } catch (SocketTimeoutException ste) {
                System.out.println("read timeout! available=" + bc.getDelegate().available());
                throw ste;
            }
            Assert.assertEquals(request0 + request1 + request2 + request3, response);
        }
        server.close();
    }

    private static final class EchoHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.write(connection.readByteBufferByLength(connection.available()));
            return true;
        }
    }
}
