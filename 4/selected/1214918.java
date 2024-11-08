package org.xsocket.connection.multiplexed;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.Server;
import org.xsocket.connection.ConnectionUtils;
import org.xsocket.connection.multiplexed.IMultiplexedConnection;
import org.xsocket.connection.multiplexed.INonBlockingPipeline;
import org.xsocket.connection.multiplexed.MultiplexedConnection;
import org.xsocket.connection.multiplexed.MultiplexedProtocolAdapter;

/**
*
* @author grro@xsocket.org
*/
public final class MarkTest {

    @Test
    public void testSimple() throws Exception {
        IServer server = new Server(new MultiplexedProtocolAdapter(new MyEchoHandler()));
        ConnectionUtils.start(server);
        IMultiplexedConnection connection = new MultiplexedConnection(new NonBlockingConnection(InetAddress.getByName("localhost"), server.getLocalPort()));
        String pipelineId = connection.createPipeline();
        INonBlockingPipeline pipeline = connection.getNonBlockingPipeline(pipelineId);
        pipeline.setAutoflush(false);
        pipeline.markWritePosition();
        pipeline.write((int) 0);
        int written = pipeline.write("test");
        pipeline.resetToWriteMark();
        pipeline.write(written);
        pipeline.flush();
        QAUtil.sleep(400);
        int length = pipeline.readInt();
        String data = pipeline.readStringByLength(length);
        Assert.assertEquals("test", data);
        pipeline.close();
        connection.close();
        server.close();
    }

    private static final class MyEchoHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.write(connection.readByteBufferByLength(connection.available()));
            return true;
        }
    }
}
