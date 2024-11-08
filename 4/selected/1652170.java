package org.xsocket.connection;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.QAUtil;
import org.xsocket.connection.IDataHandler;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.NonBlockingConnectionPool;

/**
*
* @author grro@xsocket.org
*/
public final class NonBlockingConnectionPoolDestroyTest {

    @Test
    public void testClientSideClose() throws Exception {
        Server server = new Server(new EchoHandler());
        server.start();
        NonBlockingConnectionPool pool = new NonBlockingConnectionPool();
        ClientHandler hdl = new ClientHandler();
        INonBlockingConnection con = pool.getNonBlockingConnection("localhost", server.getLocalPort(), hdl);
        con.close();
        while (hdl.getCountOnDisconnectCalled() == 0) {
            QAUtil.sleep(100);
        }
        con.close();
        server.close();
    }

    @Test
    public void testClientSideDestroy() throws Exception {
        Server server = new Server(new EchoHandler());
        server.start();
        NonBlockingConnectionPool pool = new NonBlockingConnectionPool();
        ClientHandler hdl = new ClientHandler();
        INonBlockingConnection con = pool.getNonBlockingConnection("localhost", server.getLocalPort(), hdl);
        NonBlockingConnectionPool.destroy(con);
        while (hdl.getCountOnDisconnectCalled() == 0) {
            QAUtil.sleep(100);
        }
        QAUtil.sleep(500);
        Assert.assertTrue("onData should have been called", hdl.getCountOnDataCalled() > 0);
        con.close();
        server.close();
    }

    @Test
    public void testServerSideClose() throws Exception {
        ServerHandler srvHdl = new ServerHandler();
        Server server = new Server(srvHdl);
        server.start();
        NonBlockingConnectionPool pool = new NonBlockingConnectionPool();
        ClientHandler hdl = new ClientHandler();
        INonBlockingConnection con = pool.getNonBlockingConnection("localhost", server.getLocalPort(), hdl);
        while (srvHdl.getConnection() == null) {
            QAUtil.sleep(100);
        }
        srvHdl.getConnection().close();
        while (hdl.getCountOnDisconnectCalled() == 0) {
            QAUtil.sleep(100);
        }
        QAUtil.sleep(500);
        Assert.assertTrue("onData should have been called", hdl.getCountOnDataCalled() > 0);
        con.close();
        server.close();
    }

    private static final class ClientHandler implements IDataHandler, IDisconnectHandler {

        private AtomicInteger countOnDataCalled = new AtomicInteger(0);

        private AtomicInteger countOnDisconnectCalled = new AtomicInteger(0);

        public boolean onData(INonBlockingConnection connection) throws IOException {
            countOnDataCalled.incrementAndGet();
            return true;
        }

        public boolean onDisconnect(INonBlockingConnection connection) throws IOException {
            countOnDisconnectCalled.incrementAndGet();
            return true;
        }

        int getCountOnDataCalled() {
            return countOnDataCalled.get();
        }

        int getCountOnDisconnectCalled() {
            return countOnDisconnectCalled.get();
        }
    }

    private static final class EchoHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException {
            connection.write(connection.readByteBufferByLength(connection.available()));
            return true;
        }
    }

    private static final class ServerHandler implements IConnectHandler {

        private AtomicReference<INonBlockingConnection> connectionRef = new AtomicReference<INonBlockingConnection>(null);

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            connectionRef.set(connection);
            return true;
        }

        public INonBlockingConnection getConnection() {
            return connectionRef.get();
        }
    }
}
