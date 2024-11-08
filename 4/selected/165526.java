package org.xsocket.connection;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.Execution;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.Resource;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.Server;
import org.xsocket.connection.IConnection.FlushMode;

/**
*
* @author grro@xsocket.org
*/
public final class ReplaceHandlerTest {

    private final List<String> errors = new ArrayList<String>();

    private AtomicInteger running = new AtomicInteger(0);

    @Test
    public void testServerSide() throws Exception {
        errors.clear();
        final Server server = new Server(new ServerHandlerA());
        server.setFlushmode(FlushMode.ASYNC);
        server.start();
        for (int j = 0; j < 10; j++) {
            Thread t = new Thread() {

                @Override
                public void run() {
                    running.incrementAndGet();
                    try {
                        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
                        for (int i = 0; i < 100; i++) {
                            con.write("test\r\n");
                            Assert.assertEquals("Atest", con.readStringByDelimiter("\r\n"));
                            con.write("OneMoreTest\r\n");
                            Assert.assertEquals("AOneMoreTest", con.readStringByDelimiter("\r\n"));
                            con.write("And");
                            QAUtil.sleep(100);
                            con.write("AnotherOne\r\n");
                            Assert.assertEquals("AAndAnotherOne", con.readStringByDelimiter("\r\n"));
                            con.write("switch\r\ntest\r\n");
                            Assert.assertEquals("Btest", con.readStringByDelimiter("\r\n"));
                            con.write("AnothrTest\r\n");
                            Assert.assertEquals("BAnothrTest", con.readStringByDelimiter("\r\n"));
                            con.write("switch\r\ntest\r\n");
                            Assert.assertEquals("Atest", con.readStringByDelimiter("\r\n"));
                        }
                        con.close();
                    } catch (Exception e) {
                        errors.add(e.toString());
                    }
                    running.decrementAndGet();
                }
            };
            t.start();
        }
        do {
            QAUtil.sleep(100);
        } while (running.get() > 0);
        Assert.assertTrue(errors.isEmpty());
        server.close();
    }

    @Test
    public void testClientSideBlocking() throws Exception {
        Server server = new Server(new ServerHandler());
        server.start();
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.write("test\r\n");
        Assert.assertEquals("test", con.readStringByDelimiter("\r\n"));
        con.write("OneMoreTest\r\n");
        Assert.assertEquals("OneMoreTest", con.readStringByDelimiter("\r\n"));
        con.close();
        server.close();
    }

    @Test
    public void testClientSideNonBlockingMultithreaded() throws Exception {
        Server server = new Server(new ServerHandler());
        server.start();
        ClientHandler hdl = new ClientHandler();
        INonBlockingConnection con = new NonBlockingConnection("localhost", server.getLocalPort(), hdl);
        con.write("test\r\n");
        QAUtil.sleep(2000);
        Assert.assertEquals("test", con.readStringByDelimiter("\r\n"));
        Assert.assertTrue(hdl.getThreadname().startsWith("xNbcPool"));
        con.write("OneMoreTest\r\n");
        QAUtil.sleep(1000);
        Assert.assertEquals("OneMoreTest", con.readStringByDelimiter("\r\n"));
        Assert.assertTrue(hdl.getThreadname().startsWith("xNbcPool"));
        con.close();
        server.close();
    }

    @Test
    public void testClientSideNonBlockingNonthreaded() throws Exception {
        Server server = new Server(new ServerHandler());
        server.start();
        NonThreadedClientHandler hdl = new NonThreadedClientHandler();
        INonBlockingConnection con = new NonBlockingConnection("localhost", server.getLocalPort(), hdl);
        con.write("test\r\n");
        QAUtil.sleep(1000);
        Assert.assertEquals("test", con.readStringByDelimiter("\r\n"));
        Assert.assertTrue(hdl.getThreadname().startsWith("xDispatcher"));
        con.write("OneMoreTest\r\n");
        QAUtil.sleep(1000);
        Assert.assertEquals("OneMoreTest", con.readStringByDelimiter("\r\n"));
        Assert.assertTrue(hdl.getThreadname().startsWith("xDispatcher"));
        ClientHandler hdl2 = new ClientHandler();
        con.setHandler(hdl2);
        con.write("test2\r\n");
        QAUtil.sleep(2000);
        Assert.assertEquals("test2", con.readStringByDelimiter("\r\n"));
        Assert.assertTrue(hdl2.getThreadname().startsWith("xNbcPool"));
        con.write("OneMoreTest2\r\n");
        QAUtil.sleep(1000);
        Assert.assertEquals("OneMoreTest2", con.readStringByDelimiter("\r\n"));
        Assert.assertTrue(hdl2.getThreadname().startsWith("xNbcPool"));
        con.close();
        server.close();
    }

    @Test
    public void testBoth() throws Exception {
        IServer server = new Server(new ProtocolHandler());
        server.start();
        IBlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());
        bc.setAutoflush(false);
        int length = 200;
        byte[] data = QAUtil.generateByteArray(length);
        bc.write((byte) 'A');
        bc.write("1.0.1\r\n");
        bc.write("MBwGA1UEChM...\r\n");
        bc.write(length);
        bc.flush();
        bc.write(data);
        bc.flush();
        String status = bc.readStringByDelimiter("\r\n");
        Assert.assertEquals("ACCEPTED", status);
        bc.close();
        server.close();
    }

    @Test
    public void testServerSideReplace() throws Exception {
        IServer server = new Server(new SwitchingHandler());
        server.start();
        IBlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());
        bc.write("switch\r\n");
        Assert.assertEquals("switched", bc.readStringByDelimiter("\r\n"));
        bc.write("test\r\n");
        Assert.assertEquals("Atest", bc.readStringByDelimiter("\r\n"));
        IBlockingConnection bc2 = new BlockingConnection("localhost", server.getLocalPort());
        bc2.write("test\r\n");
        Assert.assertEquals("Btest", bc2.readStringByDelimiter("\r\n"));
        bc.close();
        bc2.close();
        server.close();
    }

    private static final class SwitchingHandler implements IDataHandler {

        @Resource
        private Server server;

        public boolean onData(INonBlockingConnection connection) throws IOException {
            String cmd = connection.readStringByDelimiter("\r\n");
            if (cmd.equals("switch")) {
                server.setHandler(new ServerHandlerB());
                connection.write("switched\r\n");
            } else {
                connection.write("A" + cmd + "\r\n");
            }
            return true;
        }
    }

    private static final class ProtocolHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection nbc) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            nbc.resetToReadMark();
            nbc.markReadPosition();
            byte recordType = nbc.readByte();
            String version = nbc.readStringByDelimiter("\r\n");
            String signature = nbc.readStringByDelimiter("\r\n");
            int dataLength = nbc.readInt();
            nbc.removeReadMark();
            nbc.setHandler(new ContentHandler(dataLength, signature));
            return true;
        }
    }

    private static final class ContentHandler implements IDataHandler {

        private int remaining = 0;

        public ContentHandler(int dataLength, String signature) {
            remaining = dataLength;
        }

        public boolean onData(INonBlockingConnection nbc) throws IOException {
            int available = nbc.available();
            int lengthToRead = remaining;
            if (available < remaining) {
                lengthToRead = available;
            }
            ByteBuffer[] buffers = nbc.readByteBufferByLength(lengthToRead);
            remaining -= lengthToRead;
            if (remaining == 0) {
                nbc.setAttachment(null);
                nbc.write("ACCEPTED\r\n");
            }
            return true;
        }
    }

    private static final class ClientHandler implements IDataHandler {

        private String threadname = null;

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            this.threadname = Thread.currentThread().getName();
            return true;
        }

        String getThreadname() {
            return threadname;
        }
    }

    @Execution(Execution.NONTHREADED)
    private static final class NonThreadedClientHandler implements IDataHandler {

        private String threadname = null;

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            this.threadname = Thread.currentThread().getName();
            return true;
        }

        String getThreadname() {
            return threadname;
        }
    }

    private static final class ServerHandlerA implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            String cmd = connection.readStringByDelimiter("\r\n");
            if (cmd.equals("switch")) {
                connection.setHandler(new ServerHandlerB());
            } else {
                connection.write("A" + cmd + "\r\n");
            }
            return true;
        }
    }

    private static final class ServerHandlerB implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            String cmd = connection.readStringByDelimiter("\r\n");
            if (cmd.equals("switch")) {
                connection.setHandler(new ServerHandlerA());
            } else {
                connection.write("B" + cmd + "\r\n");
            }
            return true;
        }
    }

    private static final class ServerHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.write(connection.readByteBufferByLength(connection.available()));
            return true;
        }
    }
}
