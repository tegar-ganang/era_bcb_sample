package org.bluecove.socket;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class NativeSocketSimpleTest extends NativeSocketTestCase {

    private final boolean socketAbstractNamespace = true;

    private final String socketName = "target/test-sock_name";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected TestRunnable createTestServer() {
        return new TestRunnable() {

            public void run() throws Exception {
                LocalServerSocket serverSocket = new LocalServerSocket(new LocalSocketAddress(socketName, socketAbstractNamespace));
                try {
                    System.out.println("server starts");
                    serverAcceptsNotifyAll();
                    Socket socket = serverSocket.accept();
                    InputStream in = socket.getInputStream();
                    OutputStream out = socket.getOutputStream();
                    int len = in.read();
                    out.write(len);
                    for (int i = 0; i < len; i++) {
                        out.write(in.read());
                    }
                    Thread.sleep(500);
                    in.close();
                    out.close();
                    socket.close();
                } finally {
                    serverSocket.close();
                    System.out.println("server ends");
                }
            }
        };
    }

    public void testOneByte() throws Exception {
        serverAcceptsWait();
        Socket socket = new LocalSocket(new LocalSocketAddress(socketName, socketAbstractNamespace));
        System.out.println("client connected");
        OutputStream out = socket.getOutputStream();
        out.write(1);
        out.write(2);
        InputStream in = socket.getInputStream();
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(-1, in.read());
        in.close();
        out.close();
        socket.close();
        assertServerErrors();
    }
}
