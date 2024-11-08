package de.tfh.pdvl.hp.connections;

import java.io.IOException;
import junit.framework.TestCase;

/**
 * @author s717689
 *
 */
public class NetworkConnectionTest extends TestCase {

    private NetworkConnectionTestHelper serverThread;

    private ClientConnection clientConn;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(NetworkConnectionTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        serverThread = new NetworkConnectionTestHelper();
        serverThread.start();
        Thread.sleep(100);
        clientConn = new ClientConnection("localhost", 2222);
    }

    public void testClientWrite() throws InterruptedException, IOException {
        String sendString = "set, 1000, sine, 0.5, 2";
        clientConn.write(sendString);
        Thread.sleep(100);
        assertEquals(sendString, serverThread.readString());
    }

    public void testClientRead() throws InterruptedException, IOException {
        String sendString = "info, 1000, sine, 5e-1, 2";
        serverThread.writeString(sendString);
        Thread.sleep(100);
        assertEquals(sendString, clientConn.read());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        clientConn.close();
        serverThread.getServerConnection().closeConnection();
        serverThread.interrupt();
        clientConn = null;
        serverThread = null;
    }

    /**
     * Constructor for NetworkConnectionTest.
     * @param name
     */
    public NetworkConnectionTest(String name) {
        super(name);
    }
}
