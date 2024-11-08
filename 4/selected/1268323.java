package org.xsocket.connection;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.connection.IConnectHandler;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.IServer;
import org.xsocket.connection.NonBlockingConnection;
import org.xsocket.connection.Server;

/**
*
* @author grro@xsocket.org
*/
public final class ReadSuspendAndResumeTest {

    private static final String DELIMITER = "\r";

    @Test
    public void testSimple() throws Exception {
        IServer server = new Server(new EchoHandler());
        server.start();
        INonBlockingConnection connection = new NonBlockingConnection("localhost", server.getLocalPort());
        for (int i = 0; i < 3; i++) {
            connection.write("helo" + DELIMITER);
            do {
                QAUtil.sleep(200);
            } while (connection.indexOf(DELIMITER) == -1);
            Assert.assertEquals(connection.readStringByDelimiter(DELIMITER), "helo");
            connection.suspendReceiving();
            connection.write("helo again" + DELIMITER);
            QAUtil.sleep(1000);
            Assert.assertEquals(connection.available(), 0);
            connection.resumeReceiving();
            do {
                QAUtil.sleep(200);
            } while (connection.indexOf(DELIMITER) == -1);
            Assert.assertEquals(connection.readStringByDelimiter(DELIMITER), "helo again");
            System.out.print(".");
        }
        connection.close();
        server.close();
    }

    @Test
    public void testServerSupsend() throws Exception {
        SuspendedHandler hdl = new SuspendedHandler();
        IServer server = new Server(hdl);
        server.start();
        INonBlockingConnection connection = new NonBlockingConnection("localhost", server.getLocalPort());
        for (int i = 0; i < 3; i++) {
            connection.write("helo" + DELIMITER);
            QAUtil.sleep(500);
        }
        Assert.assertTrue(hdl.con.available() < 10);
        hdl.con.resumeReceiving();
        QAUtil.sleep(1000);
        Assert.assertTrue(hdl.con.available() > 10);
        connection.close();
        server.close();
    }

    private static final class SuspendedHandler implements IConnectHandler, IDataHandler {

        private INonBlockingConnection con = null;

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            connection.suspendReceiving();
            this.con = connection;
            return true;
        }

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            return true;
        }
    }

    private static final class EchoHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.write(connection.readStringByDelimiter(DELIMITER) + DELIMITER);
            return true;
        }
    }
}
