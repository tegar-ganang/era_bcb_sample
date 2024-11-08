package org.xsocket.stream;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.QAUtil;
import org.xsocket.stream.BlockingConnection;
import org.xsocket.stream.IBlockingConnection;
import org.xsocket.stream.IDataHandler;
import org.xsocket.stream.IServer;
import org.xsocket.stream.INonBlockingConnection;
import org.xsocket.stream.Server;

/**
*
* @author grro@xsocket.org
*/
public final class MarkAndResetTest {

    private int running = 0;

    private int handled = 0;

    @Test
    public void testSimple() throws Exception {
        System.out.println("test simple");
        IServer server = new Server(new Handler2());
        StreamUtils.start(server);
        INonBlockingConnection connection = new NonBlockingConnection("localhost", server.getLocalPort());
        QAUtil.sleep(500);
        String requestLine = connection.readStringByDelimiter("\r\n");
        connection.markReadPosition();
        connection.close();
        server.close();
    }

    @Test
    public void testReadLengthField() throws Exception {
        System.out.println("test read length field");
        IServer server = new Server(new ReadServerHandler());
        StreamUtils.start(server);
        IBlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());
        byte[] request = QAUtil.generateByteArray(20);
        bc.write((int) (request.length));
        QAUtil.sleep(500);
        bc.write(request);
        byte[] response = bc.readBytesByLength(request.length);
        Assert.assertTrue(QAUtil.isEquals(request, response));
        bc.close();
        server.close();
    }

    @Test
    public void testReadSplittedLengthField() throws Exception {
        System.out.println("test read splitted length field");
        final IServer server = new Server(new ReadServerHandler());
        StreamUtils.start(server);
        int countThread = 5;
        final int countLoops = 100;
        for (int j = 0; j < countThread; j++) {
            Thread t = new Thread() {

                @Override
                public void run() {
                    try {
                        IBlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());
                        byte[] request = QAUtil.generateByteArray(20);
                        for (int i = 0; i < countLoops; i++) {
                            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
                            lengthBuffer.putInt(request.length);
                            lengthBuffer.clear();
                            while (lengthBuffer.hasRemaining()) {
                                bc.write(lengthBuffer.get());
                            }
                            bc.write(request);
                            byte[] response = bc.readBytesByLength(request.length);
                            Assert.assertTrue(QAUtil.isEquals(request, response));
                            handled++;
                        }
                        bc.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        running--;
                    }
                }
            };
            t.start();
            running++;
        }
        while (running > 0) {
            QAUtil.sleep(200);
        }
        Assert.assertTrue(handled == (countLoops * countThread));
        server.close();
    }

    @Test
    public void testReadMulti() throws Exception {
        System.out.println("test read multi");
        MultiServerHandler handler = new MultiServerHandler();
        final IServer server = new Server(handler);
        StreamUtils.start(server);
        IBlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());
        bc.write((int) 45);
        QAUtil.sleep(500);
        bc.write((int) 77);
        QAUtil.sleep(500);
        bc.write((int) 99);
        QAUtil.sleep(500);
        bc.write((int) 43);
        Assert.assertTrue(handler.errorOccured == false);
        bc.close();
        server.close();
    }

    @Test
    public void testMarkAndAutoflush() throws Exception {
        System.out.println("test mark and autoflush");
        MultiServerHandler handler = new MultiServerHandler();
        final IServer server = new Server(handler);
        StreamUtils.start(server);
        IBlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());
        Assert.assertTrue(bc.getAutoflush());
        bc.write((int) 45);
        try {
            bc.markWritePosition();
            Assert.fail("an exception should haven been thrown, because write mark is only supported for autoflush off");
        } catch (RuntimeException expected) {
        }
        bc.close();
        server.close();
    }

    @Test
    public void testWrite() throws Exception {
        System.out.println("test write");
        WriteServerHandler handler = new WriteServerHandler();
        final IServer server = new Server(handler);
        StreamUtils.start(server);
        IBlockingConnection bc = new BlockingConnection("localhost", server.getLocalPort());
        bc.setAutoflush(false);
        bc.write((int) 88);
        bc.markWritePosition();
        bc.write(45.56);
        bc.write((byte) 3);
        bc.resetToWriteMark();
        int written = 0;
        bc.write((int) 0);
        written += bc.write((byte) 5);
        written += bc.write(4354353L);
        written += bc.write(34);
        bc.resetToWriteMark();
        bc.write((int) written);
        bc.flush();
        Assert.assertTrue(bc.readByte() == (byte) 5);
        Assert.assertTrue(bc.readLong() == 4354353L);
        Assert.assertTrue(bc.readInt() == 34);
        bc.close();
        server.close();
    }

    private static final class WriteServerHandler implements IConnectHandler, IDataHandler {

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            connection.setAutoflush(false);
            return true;
        }

        public boolean onData(INonBlockingConnection connection) throws IOException {
            connection.resetToReadMark();
            connection.markReadPosition();
            int i = connection.readInt();
            int length = connection.readInt();
            if (connection.getNumberOfAvailableBytes() >= length) {
                connection.removeReadMark();
                connection.write(connection.readByteBufferByLength(length));
            }
            connection.flush();
            return true;
        }
    }

    private static final class MultiServerHandler implements IDataHandler {

        boolean errorOccured = false;

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException {
            connection.resetToReadMark();
            connection.markReadPosition();
            int i = connection.readInt();
            if (i != 45) {
                errorOccured = true;
            }
            int i2 = connection.readInt();
            if (i2 != 77) {
                errorOccured = true;
            }
            int i3 = connection.readInt();
            if (i3 != 99) {
                errorOccured = true;
            }
            int i4 = connection.readInt();
            if (i4 != 43) {
                errorOccured = true;
            }
            return false;
        }
    }

    private static final class ReadServerHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException {
            boolean isReset = connection.resetToReadMark();
            connection.markReadPosition();
            int length = connection.readInt();
            if (connection.getNumberOfAvailableBytes() >= length) {
                connection.removeReadMark();
                ByteBuffer[] data = connection.readByteBufferByLength(length);
                connection.write(data);
            }
            return true;
        }
    }

    private static final class Handler2 implements IConnectHandler {

        public boolean onConnect(INonBlockingConnection connection) throws IOException {
            connection.setAutoflush(false);
            connection.write("GET / HTTP/1.1\r\n");
            connection.write("Host: 192.168.1.24:15088\r\n");
            connection.write("User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; de; rv:1.8.1.4) Gecko/20070515 Firefox/2.0.0.4\r\n");
            connection.write("Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5\r\n");
            connection.write("Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3\r\n");
            connection.write("Accept-Encoding: gzip,deflate\r\n");
            connection.write("Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7\r\n");
            connection.write("Keep-Alive: 300\r\n");
            connection.write("Connection: keep-alive\r\n");
            connection.write("\r\n\r\n");
            connection.flush();
            return true;
        }
    }
}
