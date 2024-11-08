package org.bluecove.socket;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Tests that not applicable java.net.ServerSocket functions throw exception
 */
public class NativeSocketOverridesTest extends NativeSocketTestCase {

    private final boolean socketAbstractNamespace = true;

    private final String socketName = "target/test-sock_name";

    @Override
    protected TestRunnable createTestServer() {
        return new TestRunnable() {

            public void run() throws Exception {
                LocalServerSocket serverSocket = new LocalServerSocket(new LocalSocketAddress(socketName, socketAbstractNamespace));
                try {
                    serverAcceptsNotifyAll();
                    Socket socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream();
                    int len = in.read();
                    out.write(len);
                    Thread.sleep(500);
                    in.close();
                    out.close();
                    socket.close();
                } finally {
                    serverSocket.close();
                }
            }
        };
    }

    public void testOverride() throws Exception {
        serverAcceptsWait();
        Socket socket = new LocalSocket(new LocalSocketAddress(socketName, socketAbstractNamespace));
        OutputStream out = socket.getOutputStream();
        out.write(1);
        assertNull(socket.getChannel());
        try {
            socket.getInetAddress();
            fail("getInetAddress");
        } catch (IllegalArgumentException e) {
        }
        try {
            socket.getLocalAddress();
            fail("getLocalAddress");
        } catch (IllegalArgumentException e) {
        }
        try {
            socket.getPort();
            fail("getPort");
        } catch (IllegalArgumentException e) {
        }
        try {
            socket.getLocalPort();
            fail("getLocalPort");
        } catch (IllegalArgumentException e) {
        }
        InputStream in = socket.getInputStream();
        assertEquals(1, in.read());
        assertEquals(-1, in.read());
        in.close();
        out.close();
        socket.close();
        assertServerErrors();
    }
}
