package org.apache.harmony.luni.tests.java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.Permission;
import java.util.Arrays;
import java.util.Locale;
import org.apache.harmony.luni.net.PlainSocketImpl;
import tests.support.Support_Configuration;

public class SocketTest extends SocketTestCase {

    private class ClientThread implements Runnable {

        public void run() {
            try {
                Socket socket = new Socket();
                InetSocketAddress addr = new InetSocketAddress(host, port);
                socket.connect(addr);
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class ServerThread implements Runnable {

        private static final int FIRST_TIME = 1;

        private static final int SECOND_TIME = 2;

        private int backlog = 10;

        public boolean ready = false;

        private int serverSocketConstructor = 0;

        public void run() {
            try {
                ServerSocket socket = null;
                switch(serverSocketConstructor) {
                    case FIRST_TIME:
                        socket = new ServerSocket(port, backlog, new InetSocketAddress(host, port).getAddress());
                        port = socket.getLocalPort();
                        break;
                    case SECOND_TIME:
                        socket = new ServerSocket(port, backlog);
                        host = socket.getInetAddress().getHostName();
                        port = socket.getLocalPort();
                        break;
                    default:
                        socket = new ServerSocket();
                        break;
                }
                synchronized (this) {
                    ready = true;
                    this.notifyAll();
                }
                socket.setSoTimeout(5000);
                Socket client = socket.accept();
                client.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        public synchronized void waitCreated() throws Exception {
            while (!ready) {
                this.wait();
            }
        }
    }

    boolean interrupted;

    String host = "localhost";

    int port;

    Thread t;

    private void connectTestImpl(int ssConsType) throws Exception {
        ServerThread server = new ServerThread();
        server.serverSocketConstructor = ssConsType;
        Thread serverThread = new Thread(server);
        serverThread.start();
        server.waitCreated();
        ClientThread client = new ClientThread();
        Thread clientThread = new Thread(client);
        clientThread.start();
        try {
            serverThread.join();
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void tearDown() {
        try {
            if (t != null) {
                t.interrupt();
            }
        } catch (Exception e) {
        }
        this.t = null;
        this.interrupted = false;
    }

    /**
     * @tests java.net.Socket#bind(java.net.SocketAddress)
     */
    public void test_bindLjava_net_SocketAddress() throws IOException {
        @SuppressWarnings("serial")
        class UnsupportedSocketAddress extends SocketAddress {

            public UnsupportedSocketAddress() {
            }
        }
        Socket theSocket = new Socket();
        InetSocketAddress bogusAddress = new InetSocketAddress(InetAddress.getByAddress(Support_Configuration.nonLocalAddressBytes), 42);
        try {
            theSocket.bind(bogusAddress);
            fail("No exception when binding to bad address");
        } catch (IOException ex) {
        }
        theSocket.close();
        theSocket = new Socket();
        theSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        int portNumber = theSocket.getLocalPort();
        assertEquals("Local address not correct after bind", new InetSocketAddress(InetAddress.getLocalHost(), portNumber), theSocket.getLocalSocketAddress());
        InetSocketAddress theAddress = new InetSocketAddress(InetAddress.getLocalHost(), 0);
        ServerSocket server = new ServerSocket();
        server.bind(theAddress);
        int sport = server.getLocalPort();
        InetSocketAddress boundAddress = new InetSocketAddress(InetAddress.getLocalHost(), sport);
        theSocket.connect(boundAddress);
        Socket worker = server.accept();
        assertEquals("Returned Remote address from server connected to does not match expected local address", new InetSocketAddress(InetAddress.getLocalHost(), portNumber), worker.getRemoteSocketAddress());
        theSocket.close();
        worker.close();
        server.close();
        theSocket = new Socket();
        theSocket.bind(null);
        assertNotNull("Bind with null did not work", theSocket.getLocalSocketAddress());
        theSocket.close();
        theSocket = new Socket();
        theAddress = new InetSocketAddress(InetAddress.getLocalHost(), 0);
        theSocket.bind(theAddress);
        Socket theSocket2 = new Socket();
        try {
            theSocket2.bind(theSocket.getLocalSocketAddress());
            fail("No exception binding to address that is not available");
        } catch (IOException ex) {
        }
        theSocket.close();
        theSocket2.close();
        theSocket = new Socket();
        try {
            theSocket.bind(new UnsupportedSocketAddress());
            fail("No exception when binding using unsupported SocketAddress subclass");
        } catch (IllegalArgumentException ex) {
        }
        theSocket.close();
    }

    /**
     * @tests java.net.Socket#bind(java.net.SocketAddress)
     */
    public void test_bindLjava_net_SocketAddress_Proxy() throws IOException {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 0));
        Socket socket = new Socket(proxy);
        InetAddress address = InetAddress.getByName("localhost");
        socket.bind(new InetSocketAddress(address, 0));
        assertEquals(address, socket.getLocalAddress());
        assertTrue(0 != socket.getLocalPort());
        socket.close();
    }

    /**
     * @tests java.net.Socket#close()
     */
    public void test_close() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            client.setSoLinger(false, 100);
        } catch (IOException e) {
            handleException(e, SO_LINGER);
        }
        client.close();
        try {
            client.getOutputStream();
            fail("Failed to close socket");
        } catch (IOException e) {
        }
        server.close();
    }

    /**
     * @tests Socket#connect(SocketAddress) try an unknownhost
     */
    public void test_connect_unknownhost() throws Exception {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress("unknownhost.invalid", 12345));
            fail("Should throw UnknownHostException");
        } catch (UnknownHostException e) {
        }
    }

    /**
     * @tests Socket#connect(SocketAddress)
     */
    public void test_connect_unresolved() throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(InetSocketAddress.createUnresolved("www.apache.org", 80));
            fail("Should throw UnknownHostException");
        } catch (UnknownHostException e) {
        }
        try {
            socket.connect(InetSocketAddress.createUnresolved("unknownhost.invalid", 12345));
            fail("Should throw UnknownHostException");
        } catch (UnknownHostException e) {
        }
    }

    /**
     * @tests java.net.Socket#connect(java.net.SocketAddress)
     */
    public void test_connectLjava_net_SocketAddress() throws Exception {
        @SuppressWarnings("serial")
        class UnsupportedSocketAddress extends SocketAddress {

            public UnsupportedSocketAddress() {
            }
        }
        Socket theSocket = new Socket();
        try {
            theSocket.connect(null);
            fail("No exception for null arg");
        } catch (IllegalArgumentException e) {
        }
        try {
            theSocket.connect(new UnsupportedSocketAddress());
            fail("No exception for invalid socket address");
        } catch (IllegalArgumentException e) {
        }
        try {
            theSocket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 }), 42));
            fail("No exception with non-connectable address");
        } catch (ConnectException e) {
        }
        theSocket = new Socket();
        try {
            theSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(), 0));
            fail("No exception when connecting to address nobody listening on");
        } catch (ConnectException e) {
        }
        ServerSocket server = new ServerSocket(0);
        InetSocketAddress boundAddress = new InetSocketAddress(InetAddress.getLocalHost(), server.getLocalPort());
        Socket client = new Socket();
        client.connect(boundAddress);
        assertTrue("Wrong connected status", client.isConnected());
        assertFalse("Wrong closed status", client.isClosed());
        assertTrue("Wrong bound status", client.isBound());
        assertFalse("Wrong input shutdown status", client.isInputShutdown());
        assertFalse("Wrong output shutdown status", client.isOutputShutdown());
        assertTrue("Local port was 0", client.getLocalPort() != 0);
        client.close();
        server.close();
        server = new ServerSocket(0);
        boundAddress = new InetSocketAddress(InetAddress.getLocalHost(), server.getLocalPort());
        client = new Socket();
        client.connect(boundAddress);
        try {
            client.connect(boundAddress);
            fail("No exception when we try to connect on a connected socket: ");
        } catch (SocketException e) {
        }
        client.close();
        server.close();
    }

    /**
     * Regression for Harmony-2503
     */
    public void test_connectLjava_net_SocketAddress_AnyAddress() throws Exception {
        connectTestImpl(ServerThread.FIRST_TIME);
        connectTestImpl(ServerThread.SECOND_TIME);
    }

    /**
     * @tests java.net.Socket#connect(java.net.SocketAddress, int)
     */
    public void test_connectLjava_net_SocketAddressI() throws Exception {
        @SuppressWarnings("serial")
        class UnsupportedSocketAddress extends SocketAddress {

            public UnsupportedSocketAddress() {
            }
        }
        Socket theSocket = new Socket();
        try {
            theSocket.connect(new InetSocketAddress(0), -100);
            fail("No exception for negative timeout");
        } catch (IllegalArgumentException e) {
        }
        try {
            theSocket.connect(null, 0);
            fail("No exception for null address");
        } catch (IllegalArgumentException e) {
        }
        try {
            theSocket.connect(new UnsupportedSocketAddress(), 1000);
            fail("No exception for invalid socket address type");
        } catch (IllegalArgumentException e) {
        }
        SocketAddress nonConnectableAddress = new InetSocketAddress(InetAddress.getByAddress(new byte[] { 0, 0, 0, 0 }), 0);
        try {
            theSocket.connect(nonConnectableAddress, 1000);
            fail("No exception when non Connectable Address passed in: ");
        } catch (SocketException e) {
        }
        theSocket = new Socket();
        try {
            theSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(), 0), 0);
            fail("No exception when connecting to address nobody listening on");
        } catch (ConnectException e) {
        }
        theSocket.close();
        ServerSocket server = new ServerSocket(0);
        InetSocketAddress boundAddress = new InetSocketAddress(InetAddress.getLocalHost(), server.getLocalPort());
        Socket client = new Socket();
        client.connect(boundAddress, 0);
        assertTrue("Wrong connected status", client.isConnected());
        assertFalse("Wrong closed status", client.isClosed());
        assertTrue("Wrong bound status", client.isBound());
        assertFalse("Wrong input shutdown status", client.isInputShutdown());
        assertFalse("Wrong output shutdown status", client.isOutputShutdown());
        assertTrue("Local port was 0", client.getLocalPort() != 0);
        client.close();
        server.close();
        theSocket = new Socket();
        SocketAddress nonListeningAddress = new InetSocketAddress(InetAddress.getLocalHost(), 42);
        try {
            theSocket.connect(nonListeningAddress, 1000);
            fail("No exception when connecting to address nobody listening on");
        } catch (ConnectException e) {
        } catch (SocketTimeoutException e) {
        }
        theSocket.close();
        server = new ServerSocket(0);
        boundAddress = new InetSocketAddress(InetAddress.getLocalHost(), server.getLocalPort());
        client = new Socket();
        client.connect(boundAddress, 10000);
        try {
            client.connect(boundAddress, 10000);
            fail("No exception when we try to connect on a connected socket: ");
        } catch (SocketException e) {
        }
        client.close();
        server.close();
    }

    /**
     * @tests java.net.Socket#Socket()
     */
    public void test_Constructor() {
        Socket s = new Socket();
        assertFalse("new socket should not be connected", s.isConnected());
        assertFalse("new socket should not be bound", s.isBound());
        assertFalse("new socket should not be closed", s.isClosed());
        assertFalse("new socket should not be in InputShutdown", s.isInputShutdown());
        assertFalse("new socket should not be in OutputShutdown", s.isOutputShutdown());
    }

    /**
     * @tests java.net.Socket#Socket(java.lang.String, int)
     */
    public void test_ConstructorLjava_lang_StringI() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        assertEquals("Failed to create socket", server.getLocalPort(), client.getPort());
        ServerSocket ss = new ServerSocket(0);
        Socket s = new Socket("0.0.0.0", ss.getLocalPort());
        ss.close();
        s.close();
    }

    /**
     * @tests java.net.Socket#Socket(java.lang.String, int,
     *        java.net.InetAddress, int)
     */
    public void test_ConstructorLjava_lang_StringILjava_net_InetAddressI() throws IOException {
        ServerSocket server = new ServerSocket(0);
        int serverPort = server.getLocalPort();
        Socket client = new Socket(InetAddress.getLocalHost().getHostName(), serverPort, InetAddress.getLocalHost(), 0);
        assertTrue("Failed to create socket", client.getPort() == serverPort);
        client.close();
        Socket theSocket = null;
        try {
            theSocket = new Socket("127.0.0.1", serverPort, InetAddress.getLocalHost(), 0);
        } catch (IOException e) {
            assertFalse("Misconfiguration - local host is the loopback address", InetAddress.getLocalHost().isLoopbackAddress());
            throw e;
        }
        assertTrue(theSocket.isConnected());
        try {
            new Socket("127.0.0.1", serverPort, theSocket.getLocalAddress(), theSocket.getLocalPort());
            fail("Was able to create two sockets on same port");
        } catch (IOException e) {
        }
        theSocket.close();
        server.close();
    }

    /**
     * @tests java.net.Socket#Socket(java.lang.String, int,
     *        java.net.InetAddress, int)
     */
    public void test_ConstructorLjava_lang_StringILjava_net_InetAddressI_ipv6() throws IOException {
        boolean preferIPv6 = "true".equals(System.getProperty("java.net.preferIPv6Addresses"));
        boolean preferIPv4 = "true".equals(System.getProperty("java.net.preferIPv4Stack"));
        boolean runIPv6 = "true".equals(System.getProperty("run.ipv6tests"));
        if (!runIPv6 || !preferIPv6 || preferIPv4) {
            return;
        }
        ServerSocket server = new ServerSocket(0);
        int serverPort = server.getLocalPort();
        Socket client = new Socket(InetAddress.getLocalHost().getHostName(), serverPort, InetAddress.getLocalHost(), 0);
        assertTrue("Failed to create socket", client.getPort() == serverPort);
        client.close();
        Socket theSocket = null;
        try {
            theSocket = new Socket(Support_Configuration.IPv6GlobalAddressJcl4, serverPort, InetAddress.getLocalHost(), 0);
        } catch (IOException e) {
            assertFalse("Misconfiguration - local host is the loopback address", InetAddress.getLocalHost().isLoopbackAddress());
            throw e;
        }
        assertTrue(theSocket.isConnected());
        try {
            new Socket(Support_Configuration.IPv6GlobalAddressJcl4, serverPort, theSocket.getLocalAddress(), theSocket.getLocalPort());
            fail("Was able to create two sockets on same port");
        } catch (IOException e) {
        }
        theSocket.close();
        server.close();
    }

    /**
     * @tests java.net.Socket#Socket(java.lang.String, int, boolean)
     */
    @SuppressWarnings("deprecation")
    public void test_ConstructorLjava_lang_StringIZ() throws IOException {
        ServerSocket server = new ServerSocket(0);
        int serverPort = server.getLocalPort();
        Socket client = new Socket(InetAddress.getLocalHost().getHostAddress(), serverPort, true);
        assertEquals("Failed to create socket", serverPort, client.getPort());
        client.close();
        client = new Socket(InetAddress.getLocalHost().getHostName(), serverPort, false);
        client.close();
        server.close();
    }

    /**
     * @tests java.net.Socket#Socket(java.net.InetAddress, int)
     */
    public void test_ConstructorLjava_net_InetAddressI() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        assertEquals("Failed to create socket", server.getLocalPort(), client.getPort());
        client.close();
        server.close();
    }

    /**
     * @tests java.net.Socket#Socket(java.net.InetAddress, int,
     *        java.net.InetAddress, int)
     */
    public void test_ConstructorLjava_net_InetAddressILjava_net_InetAddressI() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort(), InetAddress.getLocalHost(), 0);
        assertNotSame("Failed to create socket", 0, client.getLocalPort());
    }

    /**
     * @tests java.net.Socket#Socket(java.net.InetAddress, int, boolean)
     */
    @SuppressWarnings("deprecation")
    public void test_ConstructorLjava_net_InetAddressIZ() throws IOException {
        ServerSocket server = new ServerSocket(0);
        int serverPort = server.getLocalPort();
        Socket client = new Socket(InetAddress.getLocalHost(), serverPort, true);
        assertEquals("Failed to create socket", serverPort, client.getPort());
        client = new Socket(InetAddress.getLocalHost(), serverPort, false);
        client.close();
    }

    /**
     * @tests java.net.Socket#Socket(Proxy)
     */
    public void test_ConstructorLjava_net_Proxy_Exception() {
        class MockSecurityManager extends SecurityManager {

            public void checkConnect(String host, int port) {
                if ("127.0.0.1".equals(host)) {
                    throw new SecurityException("permission is not allowed");
                }
            }

            public void checkPermission(Permission permission) {
                return;
            }
        }
        SocketAddress addr1 = InetSocketAddress.createUnresolved("127.0.0.1", 80);
        SocketAddress addr2 = new InetSocketAddress("localhost", 80);
        Proxy proxy1 = new Proxy(Proxy.Type.HTTP, addr1);
        try {
            new Socket(proxy1);
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        Proxy proxy2 = new Proxy(Proxy.Type.SOCKS, addr1);
        new Socket(proxy2);
        new Socket(Proxy.NO_PROXY);
        SecurityManager originalSecurityManager = System.getSecurityManager();
        try {
            System.setSecurityManager(new MockSecurityManager());
        } catch (SecurityException e) {
            System.err.println("No permission to setSecurityManager, security related test in test_ConstructorLjava_net_Proxy_Security is ignored");
            return;
        }
        Proxy proxy3 = new Proxy(Proxy.Type.SOCKS, addr1);
        Proxy proxy4 = new Proxy(Proxy.Type.SOCKS, addr2);
        try {
            try {
                new Socket(proxy3);
                fail("should throw SecurityException");
            } catch (SecurityException e) {
            }
            try {
                new Socket(proxy4);
                fail("should throw SecurityException");
            } catch (SecurityException e) {
            }
        } finally {
            System.setSecurityManager(originalSecurityManager);
        }
    }

    /**
     * @tests java.net.Socket#getChannel()
     */
    public void test_getChannel() {
        assertNull(new Socket().getChannel());
    }

    /**
     * @tests java.net.Socket#getInetAddress()
     */
    public void test_getInetAddress() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        assertTrue("Returned incorrect InetAdrees", client.getInetAddress().equals(InetAddress.getLocalHost()));
        client.close();
        server.close();
    }

    /**
     * @tests java.net.Socket#getInputStream()
     */
    public void test_getInputStream() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        InputStream is = client.getInputStream();
        assertNotNull("Failed to get stream", is);
        is.close();
        client.close();
        server.close();
    }

    private boolean isUnix() {
        String osName = System.getProperty("os.name");
        osName = (osName == null ? null : osName.toLowerCase(Locale.ENGLISH));
        if (osName != null && osName.startsWith("windows")) {
            return false;
        }
        return true;
    }

    /**
     * @tests java.net.Socket#getKeepAlive()
     */
    public void test_getKeepAlive() {
        try {
            ServerSocket server = new ServerSocket(0);
            Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort(), null, 0);
            client.setKeepAlive(true);
            assertTrue("getKeepAlive false when it should be true", client.getKeepAlive());
            client.setKeepAlive(false);
            assertFalse("getKeepAlive true when it should be False", client.getKeepAlive());
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_KEEPALIVE);
        } catch (Exception e) {
            handleException(e, SO_KEEPALIVE);
        }
    }

    /**
     * @tests java.net.Socket#getLocalAddress()
     */
    public void test_getLocalAddress() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        assertTrue("Returned incorrect InetAddress", client.getLocalAddress().equals(InetAddress.getLocalHost()));
        String preferIPv4StackValue = System.getProperty("java.net.preferIPv4Stack");
        String preferIPv6AddressesValue = System.getProperty("java.net.preferIPv6Addresses");
        client = new Socket();
        client.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0));
        if (((preferIPv4StackValue == null) || preferIPv4StackValue.equalsIgnoreCase("false")) && (preferIPv6AddressesValue != null) && (preferIPv6AddressesValue.equals("true"))) {
            assertTrue("ANY address not returned correctly (getLocalAddress) with preferIPv6Addresses=true, preferIPv4Stack=false " + client.getLocalSocketAddress(), client.getLocalAddress() instanceof Inet6Address);
        } else {
            assertTrue("ANY address not returned correctly (getLocalAddress) with preferIPv6Addresses=true, preferIPv4Stack=true " + client.getLocalSocketAddress(), client.getLocalAddress() instanceof Inet4Address);
        }
        client.close();
        server.close();
    }

    /**
     * @tests java.net.Socket#getLocalPort()
     */
    public void test_getLocalPort() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        assertNotSame("Returned incorrect port", 0, client.getLocalPort());
        client.close();
        server.close();
    }

    /**
     * @tests java.net.Socket#getLocalSocketAddress()
     */
    public void test_getLocalSocketAddress() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        int clientPort = client.getLocalPort();
        assertEquals("Returned incorrect InetSocketAddress(1):", new InetSocketAddress(InetAddress.getLocalHost(), clientPort), client.getLocalSocketAddress());
        client.close();
        server.close();
        client = new Socket();
        assertNull("Returned incorrect InetSocketAddress -unbound socket- Expected null", client.getLocalSocketAddress());
        client.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        clientPort = client.getLocalPort();
        assertEquals("Returned incorrect InetSocketAddress(2):", new InetSocketAddress(InetAddress.getLocalHost(), clientPort), client.getLocalSocketAddress());
        client.close();
        client = new Socket();
        client.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0));
        String preferIPv4StackValue = System.getProperty("java.net.preferIPv4Stack");
        String preferIPv6AddressesValue = System.getProperty("java.net.preferIPv6Addresses");
        if (((preferIPv4StackValue == null) || preferIPv4StackValue.equalsIgnoreCase("false")) && (preferIPv6AddressesValue != null) && (preferIPv6AddressesValue.equals("true"))) {
            assertTrue("ANY address not returned correctly with preferIPv6Addresses=true, preferIPv4Stack=false " + client.getLocalSocketAddress(), ((InetSocketAddress) client.getLocalSocketAddress()).getAddress() instanceof Inet6Address);
        } else {
            assertTrue("ANY address not returned correctly with preferIPv6Addresses=true, preferIPv4Stack=true " + client.getLocalSocketAddress(), ((InetSocketAddress) client.getLocalSocketAddress()).getAddress() instanceof Inet4Address);
        }
        client.close();
        client = new Socket();
        client.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), 0));
        if (((preferIPv4StackValue == null) || preferIPv4StackValue.equalsIgnoreCase("false")) && (preferIPv6AddressesValue != null) && (preferIPv6AddressesValue.equals("true"))) {
            assertTrue("ANY address not returned correctly with preferIPv6Addresses=true, preferIPv4Stack=false " + client.getLocalSocketAddress(), ((InetSocketAddress) client.getLocalSocketAddress()).getAddress() instanceof Inet6Address);
        } else {
            assertTrue("ANY address not returned correctly with preferIPv6Addresses=true, preferIPv4Stack=true " + client.getLocalSocketAddress(), ((InetSocketAddress) client.getLocalSocketAddress()).getAddress() instanceof Inet4Address);
        }
        client.close();
    }

    /**
     * @tests java.net.Socket#getOOBInline()
     */
    public void test_getOOBInline() {
        try {
            Socket theSocket = new Socket();
            theSocket.setOOBInline(true);
            assertTrue("expected OOBIline to be true", theSocket.getOOBInline());
            theSocket.setOOBInline(false);
            assertFalse("expected OOBIline to be false", theSocket.getOOBInline());
            theSocket.setOOBInline(false);
            assertFalse("expected OOBIline to be false", theSocket.getOOBInline());
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_OOBINLINE);
        } catch (Exception e) {
            handleException(e, SO_OOBINLINE);
        }
    }

    /**
     * @tests java.net.Socket#getOutputStream()
     */
    @SuppressWarnings("deprecation")
    public void test_getOutputStream() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        OutputStream os = client.getOutputStream();
        assertNotNull("Failed to get stream", os);
        os.close();
        client.close();
        server.close();
        final ServerSocket sinkServer = new ServerSocket(0);
        Runnable runnable = new Runnable() {

            public void run() {
                try {
                    Socket worker = sinkServer.accept();
                    sinkServer.close();
                    InputStream in = worker.getInputStream();
                    in.read();
                    in.close();
                    worker.close();
                } catch (IOException e) {
                    fail();
                }
            }
        };
        Thread thread = new Thread(runnable, "Socket.getOutputStream");
        thread.start();
        Socket pingClient = new Socket(InetAddress.getLocalHost(), sinkServer.getLocalPort());
        int c = 0;
        while (!pingClient.isConnected()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            if (++c > 4) {
                fail("thread is not alive");
            }
        }
        OutputStream out = pingClient.getOutputStream();
        out.write(new byte[256]);
        Thread.yield();
        c = 0;
        while (thread.isAlive()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            if (++c > 4) {
                fail("read call did not exit");
            }
        }
        try {
            for (int i = 0; i < 400; i++) {
                out.write(new byte[256]);
            }
            fail("write to closed socket did not cause exception");
        } catch (IOException e) {
        }
        out.close();
        pingClient.close();
        sinkServer.close();
        ServerSocket ss2 = new ServerSocket(0);
        Socket s = new Socket("127.0.0.1", ss2.getLocalPort());
        ss2.accept();
        s.shutdownOutput();
        try {
            s.getOutputStream();
            fail("should throw SocketException");
        } catch (SocketException e) {
        }
    }

    /**
     * @tests java.net.Socket#getPort()
     */
    public void test_getPort() throws IOException {
        ServerSocket server = new ServerSocket(0);
        int serverPort = server.getLocalPort();
        Socket client = new Socket(InetAddress.getLocalHost(), serverPort);
        assertEquals("Returned incorrect port", serverPort, client.getPort());
        client.close();
        server.close();
    }

    /**
     * @tests java.net.Socket#getReceiveBufferSize()
     */
    public void test_getReceiveBufferSize() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            client.setReceiveBufferSize(130);
            assertTrue("Incorrect buffer size", client.getReceiveBufferSize() >= 130);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_RCVBUF);
        } catch (Exception e) {
            handleException(e, SO_RCVBUF);
        } finally {
            client.close();
            server.close();
        }
    }

    /**
     * @tests java.net.Socket#getRemoteSocketAddress()
     */
    public void test_getRemoteSocketAddress() throws IOException {
        ServerSocket server = new ServerSocket(0);
        int serverPort = server.getLocalPort();
        Socket client = new Socket(InetAddress.getLocalHost(), serverPort);
        assertEquals("Returned incorrect InetSocketAddress(1):", new InetSocketAddress(InetAddress.getLocalHost(), serverPort), client.getRemoteSocketAddress());
        client.close();
        Socket theSocket = new Socket();
        theSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        assertNull("Returned incorrect InetSocketAddress -unconnected socket:", theSocket.getRemoteSocketAddress());
        theSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(), serverPort));
        assertEquals("Returned incorrect InetSocketAddress(2):", new InetSocketAddress(InetAddress.getLocalHost(), serverPort), theSocket.getRemoteSocketAddress());
        theSocket.close();
        server.close();
    }

    /**
     * @tests java.net.Socket#getReuseAddress()
     */
    public void test_getReuseAddress() {
        try {
            Socket theSocket = new Socket();
            theSocket.setReuseAddress(true);
            assertTrue("getReuseAddress false when it should be true", theSocket.getReuseAddress());
            theSocket.setReuseAddress(false);
            assertFalse("getReuseAddress true when it should be False", theSocket.getReuseAddress());
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_REUSEADDR);
        } catch (Exception e) {
            handleException(e, SO_REUSEADDR);
        }
    }

    /**
     * @tests java.net.Socket#getSendBufferSize()
     */
    public void test_getSendBufferSize() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            client.setSendBufferSize(134);
            assertTrue("Incorrect buffer size", client.getSendBufferSize() >= 134);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_SNDBUF);
        } catch (Exception e) {
            handleException(e, SO_SNDBUF);
        } finally {
            client.close();
            server.close();
        }
    }

    /**
     * @tests java.net.Socket#getSoLinger()
     */
    public void test_getSoLinger() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            client.setSoLinger(true, 200);
            assertEquals("Returned incorrect linger", 200, client.getSoLinger());
            client.setSoLinger(false, 0);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_LINGER);
        } catch (Exception e) {
            handleException(e, SO_LINGER);
        } finally {
            client.close();
            server.close();
        }
    }

    /**
     * @tests java.net.Socket#getSoTimeout()
     */
    public void test_getSoTimeout() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            client.setSoTimeout(100);
            assertEquals("Returned incorrect sotimeout", 100, client.getSoTimeout());
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_TIMEOUT);
        } catch (Exception e) {
            handleException(e, SO_TIMEOUT);
        } finally {
            client.close();
            server.close();
        }
    }

    /**
     * @tests java.net.Socket#getTcpNoDelay()
     */
    public void test_getTcpNoDelay() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            boolean bool = !client.getTcpNoDelay();
            client.setTcpNoDelay(bool);
            assertTrue("Failed to get no delay setting: " + client.getTcpNoDelay(), client.getTcpNoDelay() == bool);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(TCP_NODELAY);
        } catch (Exception e) {
            handleException(e, TCP_NODELAY);
        } finally {
            client.close();
            server.close();
        }
    }

    /**
     * @tests java.net.Socket#getTrafficClass()
     */
    public void test_getTrafficClass() {
        try {
            int trafficClass = new Socket().getTrafficClass();
            assertTrue(0 <= trafficClass);
            assertTrue(trafficClass <= 255);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(IP_TOS);
        } catch (Exception e) {
            handleException(e, IP_TOS);
        }
    }

    /**
     * @tests java.net.Socket#isBound()
     */
    public void test_isBound() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        Socket worker = server.accept();
        assertTrue("Socket indicated  not bound when it should be (1)", client.isBound());
        worker.close();
        client.close();
        server.close();
        client = new Socket();
        assertFalse("Socket indicated bound when it was not (2)", client.isBound());
        server = new ServerSocket();
        server.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
        InetSocketAddress boundAddress = new InetSocketAddress(server.getInetAddress(), server.getLocalPort());
        client.connect(boundAddress);
        worker = server.accept();
        assertTrue("Socket indicated not bound when it should be (2)", client.isBound());
        worker.close();
        client.close();
        server.close();
        InetSocketAddress theLocalAddress = new InetSocketAddress(InetAddress.getLocalHost(), 0);
        client = new Socket();
        assertFalse("Socket indicated bound when it was not (3)", client.isBound());
        client.bind(theLocalAddress);
        assertTrue("Socket indicated not bound when it should be (3a)", client.isBound());
        client.close();
        assertTrue("Socket indicated not bound when it should be (3b)", client.isBound());
    }

    /**
     * @tests java.net.Socket#isClosed()
     */
    public void test_isClosed() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        Socket worker = server.accept();
        assertFalse("Socket should indicate it is not closed(1):", client.isClosed());
        client.close();
        assertTrue("Socket should indicate it is closed(1):", client.isClosed());
        assertFalse("Accepted Socket should indicate it is not closed:", worker.isClosed());
        worker.close();
        assertTrue("Accepted Socket should indicate it is closed:", worker.isClosed());
        assertFalse("Server Socket should indicate it is not closed:", server.isClosed());
        server.close();
        assertTrue("Server Socket should indicate it is closed:", server.isClosed());
    }

    /**
     * @tests java.net.Socket#isConnected()
     */
    public void test_isConnected() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        Socket worker = server.accept();
        assertTrue("Socket indicated  not connected when it should be", client.isConnected());
        client.close();
        worker.close();
        server.close();
        InetSocketAddress theAddress = new InetSocketAddress(InetAddress.getLocalHost(), 0);
        client = new Socket();
        assertFalse("Socket indicated connected when it was not", client.isConnected());
        server = new ServerSocket();
        server.bind(theAddress);
        InetSocketAddress boundAddress = new InetSocketAddress(server.getInetAddress(), server.getLocalPort());
        client.connect(boundAddress);
        worker = server.accept();
        assertTrue("Socket indicated  not connected when it should be", client.isConnected());
        client.close();
        worker.close();
        server.close();
    }

    /**
     * @tests java.net.Socket#isInputShutdown()
     */
    public void test_isInputShutdown() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        Socket worker = server.accept();
        InputStream theInput = client.getInputStream();
        OutputStream theOutput = worker.getOutputStream();
        assertFalse("Socket indicated input shutdown when it should not have", client.isInputShutdown());
        client.shutdownInput();
        assertTrue("Socket indicated input was NOT shutdown when it should have been", client.isInputShutdown());
        client.close();
        worker.close();
        server.close();
        assertFalse("Socket indicated input was shutdown when socket was closed", worker.isInputShutdown());
        theInput.close();
        theOutput.close();
    }

    /**
     * @tests java.net.Socket#isOutputShutdown()
     */
    public void test_isOutputShutdown() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        Socket worker = server.accept();
        InputStream theInput = client.getInputStream();
        OutputStream theOutput = worker.getOutputStream();
        assertFalse("Socket indicated output shutdown when it should not have", worker.isOutputShutdown());
        worker.shutdownOutput();
        assertTrue("Socket indicated output was NOT shutdown when it should have been", worker.isOutputShutdown());
        client.close();
        worker.close();
        server.close();
        assertFalse("Socket indicated output was output shutdown when the socket was closed", client.isOutputShutdown());
        theInput.close();
        theOutput.close();
    }

    /**
     * @tests java.net.Socket#sendUrgentData(int)
     */
    public void test_sendUrgentDataI() throws Exception {
        String platform = System.getProperty("os.name");
        if (platform.equals("Dummy")) {
            return;
        }
        InetAddress localHost = InetAddress.getLocalHost();
        ServerSocket server = new ServerSocket(0, 5, localHost);
        SocketAddress serverAddress = new InetSocketAddress(localHost, server.getLocalPort());
        Socket client = new Socket();
        client.setOOBInline(false);
        client.connect(serverAddress);
        Socket worker = server.accept();
        worker.setTcpNoDelay(true);
        OutputStream theOutput = worker.getOutputStream();
        byte[] sendBytes = new String("Test").getBytes();
        theOutput.write(sendBytes);
        theOutput.flush();
        worker.sendUrgentData("UrgentData".getBytes()[0]);
        theOutput.write(sendBytes);
        worker.shutdownOutput();
        worker.close();
        int totalBytesRead = 0;
        byte[] myBytes = new byte[100];
        InputStream theInput = client.getInputStream();
        while (true) {
            int bytesRead = theInput.read(myBytes, totalBytesRead, myBytes.length - totalBytesRead);
            if (bytesRead == -1) {
                break;
            }
            totalBytesRead = totalBytesRead + bytesRead;
        }
        client.close();
        server.close();
        byte[] expectBytes = new byte[2 * sendBytes.length];
        System.arraycopy(sendBytes, 0, expectBytes, 0, sendBytes.length);
        System.arraycopy(sendBytes, 0, expectBytes, sendBytes.length, sendBytes.length);
        byte[] resultBytes = new byte[totalBytesRead];
        System.arraycopy(myBytes, 0, resultBytes, 0, totalBytesRead);
        assertTrue("Urgent data was received", Arrays.equals(expectBytes, resultBytes));
        server = new ServerSocket(0, 5, localHost);
        serverAddress = new InetSocketAddress(localHost, server.getLocalPort());
        client = new Socket();
        client.setOOBInline(true);
        client.connect(serverAddress);
        worker = server.accept();
        worker.setTcpNoDelay(true);
        theOutput = worker.getOutputStream();
        sendBytes = new String("Test - Urgent Data").getBytes();
        theOutput.write(sendBytes);
        client.setOOBInline(true);
        byte urgentByte = "UrgentData".getBytes()[0];
        worker.sendUrgentData(urgentByte);
        theOutput.write(sendBytes);
        worker.shutdownOutput();
        worker.close();
        totalBytesRead = 0;
        myBytes = new byte[100];
        theInput = client.getInputStream();
        while (true) {
            int bytesRead = theInput.read(myBytes, totalBytesRead, myBytes.length - totalBytesRead);
            if (bytesRead == -1) {
                break;
            }
            totalBytesRead = totalBytesRead + bytesRead;
        }
        client.close();
        server.close();
        expectBytes = new byte[2 * sendBytes.length + 1];
        System.arraycopy(sendBytes, 0, expectBytes, 0, sendBytes.length);
        expectBytes[sendBytes.length] = urgentByte;
        System.arraycopy(sendBytes, 0, expectBytes, sendBytes.length + 1, sendBytes.length);
        resultBytes = new byte[totalBytesRead];
        System.arraycopy(myBytes, 0, resultBytes, 0, totalBytesRead);
        assertTrue("Urgent data was not received with one urgent byte", Arrays.equals(expectBytes, resultBytes));
        server = new ServerSocket(0, 5, localHost);
        serverAddress = new InetSocketAddress(localHost, server.getLocalPort());
        client = new Socket();
        client.setOOBInline(true);
        client.connect(serverAddress);
        worker = server.accept();
        worker.setTcpNoDelay(true);
        theOutput = worker.getOutputStream();
        sendBytes = new String("Test - Urgent Data").getBytes();
        theOutput.write(sendBytes);
        client.setOOBInline(true);
        byte urgentByte1 = "UrgentData".getBytes()[0];
        byte urgentByte2 = "UrgentData".getBytes()[1];
        worker.sendUrgentData(urgentByte1);
        worker.sendUrgentData(urgentByte2);
        theOutput.write(sendBytes);
        worker.shutdownOutput();
        worker.close();
        totalBytesRead = 0;
        myBytes = new byte[100];
        theInput = client.getInputStream();
        while (true) {
            int bytesRead = theInput.read(myBytes, totalBytesRead, myBytes.length - totalBytesRead);
            if (bytesRead == -1) {
                break;
            }
            totalBytesRead = totalBytesRead + bytesRead;
        }
        client.close();
        server.close();
        expectBytes = new byte[2 * sendBytes.length + 2];
        System.arraycopy(sendBytes, 0, expectBytes, 0, sendBytes.length);
        expectBytes[sendBytes.length] = urgentByte1;
        expectBytes[sendBytes.length + 1] = urgentByte2;
        System.arraycopy(sendBytes, 0, expectBytes, sendBytes.length + 2, sendBytes.length);
        resultBytes = new byte[totalBytesRead];
        System.arraycopy(myBytes, 0, resultBytes, 0, totalBytesRead);
        assertTrue("Urgent data was not received with two urgent bytes", Arrays.equals(expectBytes, resultBytes));
        server = new ServerSocket(0, 5, localHost);
        serverAddress = new InetSocketAddress(localHost, server.getLocalPort());
        client = new Socket();
        client.setOOBInline(true);
        client.connect(serverAddress);
        worker = server.accept();
        worker.setTcpNoDelay(true);
        client.setOOBInline(true);
        urgentByte = "UrgentData".getBytes()[0];
        worker.sendUrgentData(urgentByte);
        worker.close();
        theInput = client.getInputStream();
        int byteRead = theInput.read();
        client.close();
        server.close();
        assertEquals("Sole urgent data was not received", (int) (urgentByte & 0xff), byteRead);
    }

    /**
     * @tests java.net.Socket#setKeepAlive(boolean)
     */
    public void test_setKeepAliveZ() throws IOException {
        class TestSocket extends Socket {

            public TestSocket(SocketImpl impl) throws SocketException {
                super(impl);
            }
        }
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            client.setKeepAlive(true);
            client.setKeepAlive(false);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_KEEPALIVE);
        } catch (Exception e) {
            handleException(e, SO_KEEPALIVE);
        } finally {
            client.close();
            server.close();
        }
        new TestSocket((SocketImpl) null).setKeepAlive(true);
    }

    /**
     * @tests java.net.Socket#setOOBInline(boolean)
     */
    public void test_setOOBInlineZ() {
        try {
            Socket theSocket = new Socket();
            theSocket.setOOBInline(true);
            assertTrue("expected OOBIline to be true", theSocket.getOOBInline());
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_OOBINLINE);
        } catch (Exception e) {
            handleException(e, SO_OOBINLINE);
        }
    }

    /**
     * @tests java.net.Socket#setPerformancePreference()
     */
    public void test_setPerformancePreference_Int_Int_Int() throws IOException {
        Socket theSocket = new Socket();
        theSocket.setPerformancePreferences(1, 1, 1);
    }

    /**
     * @tests java.net.Socket#setReceiveBufferSize(int)
     */
    public void test_setReceiveBufferSizeI() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            client.setReceiveBufferSize(130);
            assertTrue("Incorrect buffer size", client.getReceiveBufferSize() >= 130);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_RCVBUF);
        } catch (Exception e) {
            handleException(e, SO_RCVBUF);
        } finally {
            client.close();
            server.close();
        }
    }

    /**
     * @tests java.net.Socket#setReuseAddress(boolean)
     */
    public void test_setReuseAddressZ() throws UnknownHostException {
        try {
            Socket theSocket = new Socket();
            theSocket.setReuseAddress(false);
            theSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), 0));
            InetSocketAddress localAddress1 = new InetSocketAddress(theSocket.getLocalAddress(), theSocket.getLocalPort());
            Socket theSocket2 = new Socket();
            theSocket2.setReuseAddress(false);
            theSocket.close();
            theSocket2.bind(localAddress1);
            theSocket2.close();
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_REUSEADDR);
        } catch (Exception e) {
            handleException(e, SO_REUSEADDR);
        }
    }

    /**
     * @tests java.net.Socket#setSendBufferSize(int)
     */
    public void test_setSendBufferSizeI() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            client.setSendBufferSize(134);
            assertTrue("Incorrect buffer size", client.getSendBufferSize() >= 134);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_SNDBUF);
        } catch (Exception e) {
            handleException(e, SO_SNDBUF);
        } finally {
            client.close();
            server.close();
        }
    }

    /**
     * @tests java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     */
    public void test_setSocketImplFactoryLjava_net_SocketImplFactory() {
    }

    /**
     * @tests java.net.Socket#setSoLinger(boolean, int)
     */
    public void test_setSoLingerZI() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            client.setSoLinger(true, 500);
            assertEquals("Set incorrect linger", 500, client.getSoLinger());
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_LINGER);
            client.setSoLinger(false, 0);
        } catch (Exception e) {
            handleException(e, SO_LINGER);
        } finally {
            client.close();
            server.close();
        }
    }

    /**
     * @tests java.net.Socket#setSoTimeout(int)
     */
    public void test_setSoTimeoutI() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            client.setSoTimeout(100);
            assertEquals("Set incorrect sotimeout", 100, client.getSoTimeout());
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(SO_TIMEOUT);
        } catch (Exception e) {
            handleException(e, SO_TIMEOUT);
        } finally {
            client.close();
            server.close();
        }
    }

    /**
     * @tests java.net.Socket#setTcpNoDelay(boolean)
     */
    public void test_setTcpNoDelayZ() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        try {
            boolean bool;
            client.setTcpNoDelay(bool = !client.getTcpNoDelay());
            assertTrue("Failed to set no delay setting: " + client.getTcpNoDelay(), client.getTcpNoDelay() == bool);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(TCP_NODELAY);
        } catch (Exception e) {
            handleException(e, TCP_NODELAY);
        } finally {
            client.close();
            server.close();
        }
    }

    /**
     * @tests java.net.Socket#setTrafficClass(int)
     */
    public void test_setTrafficClassI() {
        try {
            int IPTOS_LOWCOST = 0x2;
            int IPTOS_RELIABILTY = 0x4;
            int IPTOS_THROUGHPUT = 0x8;
            int IPTOS_LOWDELAY = 0x10;
            Socket theSocket = new Socket();
            try {
                theSocket.setTrafficClass(256);
                fail("No exception was thrown when traffic class set to 256");
            } catch (IllegalArgumentException e) {
            }
            try {
                theSocket.setTrafficClass(-1);
                fail("No exception was thrown when traffic class set to -1");
            } catch (IllegalArgumentException e) {
            }
            theSocket.setTrafficClass(IPTOS_LOWCOST);
            theSocket.setTrafficClass(IPTOS_RELIABILTY);
            theSocket.setTrafficClass(IPTOS_THROUGHPUT);
            theSocket.setTrafficClass(IPTOS_LOWDELAY);
            ensureExceptionThrownIfOptionIsUnsupportedOnOS(IP_TOS);
        } catch (Exception e) {
            handleException(e, IP_TOS);
        }
    }

    /**
     * @tests java.net.Socket#shutdownInput()
     */
    @SuppressWarnings("deprecation")
    public void test_shutdownInput() throws IOException {
        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();
        Socket client = new Socket(InetAddress.getLocalHost(), port);
        Socket worker = server.accept();
        worker.setTcpNoDelay(true);
        InputStream theInput = client.getInputStream();
        OutputStream theOutput = worker.getOutputStream();
        client.shutdownInput();
        String sendString = new String("Test");
        theOutput.write(sendString.getBytes());
        theOutput.flush();
        assertEquals(0, theInput.available());
        client.close();
        server.close();
        Socket s = new Socket("0.0.0.0", port, false);
        s.shutdownInput();
        try {
            s.shutdownInput();
            fail("should throw SocketException");
        } catch (SocketException se) {
        }
        s.close();
    }

    /**
     * @tests java.net.Socket#shutdownOutput()
     */
    @SuppressWarnings("deprecation")
    public void test_shutdownOutput() throws IOException {
        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();
        Socket client = new Socket(InetAddress.getLocalHost(), port);
        Socket worker = server.accept();
        OutputStream theOutput = worker.getOutputStream();
        worker.shutdownOutput();
        String sendString = new String("Test");
        try {
            theOutput.write(sendString.getBytes());
            theOutput.flush();
            fail("No exception when writing on socket with output shutdown");
        } catch (IOException e) {
        }
        client.close();
        server.close();
        Socket s = new Socket("0.0.0.0", port, false);
        s.shutdownOutput();
        try {
            s.shutdownOutput();
            fail("should throw SocketException");
        } catch (SocketException se) {
        }
        s.close();
    }

    /**
     * @tests java.net.Socket#toString()
     */
    public void test_toString() throws IOException {
        ServerSocket server = new ServerSocket(0);
        Socket client = new Socket(InetAddress.getLocalHost(), server.getLocalPort());
        String expected = "Socket[addr=" + InetAddress.getLocalHost() + ",port=" + client.getPort() + ",localport=" + client.getLocalPort() + "]";
        assertEquals("Returned incorrect string", expected, client.toString());
        client.close();
        server.close();
    }

    /**
     * @tests {@link java.net.Socket#setSocketImplFactory(SocketImplFactory)}
     */
    public void test_setSocketFactoryLjava_net_SocketImplFactory() throws IOException {
        SocketImplFactory factory = new MockSocketImplFactory();
        Socket.setSocketImplFactory(factory);
        try {
            Socket.setSocketImplFactory(null);
            fail("Should throw SocketException");
        } catch (SocketException e) {
        }
        try {
            Socket.setSocketImplFactory(factory);
            fail("Should throw SocketException");
        } catch (SocketException e) {
        }
    }

    private class MockSocketImplFactory implements SocketImplFactory {

        public SocketImpl createSocketImpl() {
            return new PlainSocketImpl();
        }
    }
}
