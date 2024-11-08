package org.xsocket.connection;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;

/**
*
* @author grro@xsocket.org
*/
public final class ReuseAddressExample {

    @Test
    public void testSimple() throws Exception {
        Map<String, Object> options = new HashMap<String, Object>();
        options.put(IConnection.SO_REUSEADDR, true);
        Server server = new Server(0, options, new Handler());
        server.start();
        QAUtil.sleep(500);
        server.close();
        QAUtil.sleep(200);
        server = new Server(0, options, new Handler());
        server.start();
        server.close();
    }

    private static final class Handler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.write(connection.readStringByDelimiter("\r\n") + "\r\n");
            return true;
        }
    }
}
