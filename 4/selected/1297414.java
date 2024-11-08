package org.xsocket.connection;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicBoolean;
import junit.framework.Assert;
import org.junit.Test;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.connection.IServer;

/**
*
* @author grro@xlightweb.org
*/
public final class HanderDisconnectTest {

    @Test
    public void disconnectTest() throws Exception {
        IServer server = new Server(new EchoHandler());
        server.start();
        Handler hdl = new Handler();
        INonBlockingConnection con = new NonBlockingConnection("localhost", server.getLocalPort(), hdl);
        QAUtil.sleep(500);
        server.close();
        QAUtil.sleep(500);
        Assert.assertFalse(hdl.isOpen());
        con.close();
    }

    private final class Handler implements IDataHandler {

        private final AtomicBoolean isOpen = new AtomicBoolean(true);

        @Execution(Execution.NONTHREADED)
        public boolean onData(INonBlockingConnection connection) throws IOException {
            isOpen.set(connection.isOpen());
            return true;
        }

        boolean isOpen() {
            return isOpen.get();
        }
    }

    private static final class EchoHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            connection.write(connection.readByteBufferByLength(connection.available()));
            return true;
        }
    }
}
