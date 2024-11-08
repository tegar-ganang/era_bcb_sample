package org.xsocket.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.INonBlockingConnection;

/**
*
* @author grro@xsocket.org
*/
@Execution(Execution.NONTHREADED)
public final class AttachmentAvailableAfterDisconnectTest {

    @Test
    public void testSync() throws Exception {
        IServer server = new Server(new ServerHandler());
        server.start();
        Handler hdl = new Handler();
        INonBlockingConnection con = new NonBlockingConnection("localhost", server.getLocalPort(), hdl);
        QAUtil.sleep(500);
        con.close();
        QAUtil.sleep(1000);
        Assert.assertTrue(hdl.isAttachmentAvailable());
        con.close();
        server.close();
    }

    @Test
    public void testAsync() throws Exception {
        IServer server = new Server(new ServerHandler());
        server.start();
        Handler hdl = new Handler();
        INonBlockingConnection con = new NonBlockingConnection(InetAddress.getByName("localhost"), server.getLocalPort(), hdl, false, 100);
        QAUtil.sleep(500);
        con.close();
        QAUtil.sleep(500);
        Assert.assertTrue(hdl.isAttachmentAvailable());
        con.close();
        server.close();
    }

    private static final class Handler implements IConnectHandler, IDisconnectHandler {

        private boolean isAttachmentAvailable = false;

        public boolean onConnect(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.setAttachment("attachment");
            return true;
        }

        public boolean onDisconnect(INonBlockingConnection connection) throws IOException {
            isAttachmentAvailable = (connection.getAttachment() != null);
            return true;
        }

        boolean isAttachmentAvailable() {
            return isAttachmentAvailable;
        }
    }

    private static final class ServerHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            connection.write(connection.readByteBufferByDelimiter("\r\n"));
            return true;
        }
    }
}
