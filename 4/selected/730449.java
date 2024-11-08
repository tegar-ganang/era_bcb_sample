package org.xsocket.stream;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import org.junit.Test;
import org.xsocket.QAUtil;
import org.xsocket.stream.IMultithreadedServer;
import org.xsocket.stream.MultithreadedServer;

/**
*
* @author grro@xsocket.org
*/
public final class LocalAddressTest {

    @Test
    public void testSimple() throws Exception {
        for (int i = 0; i < 30; i++) {
            IMultithreadedServer server = new MultithreadedServer(QAUtil.getRandomLocalAddress(), 0, new Handler());
            StreamUtils.start(server);
            IBlockingConnection connection = new BlockingConnection(server.getLocalAddress(), server.getLocalPort());
            connection.write("trtr");
            server.close();
        }
    }

    private static final class Handler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
            connection.write(connection.readAvailable());
            return true;
        }
    }
}
