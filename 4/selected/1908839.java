package tests.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Vector;
import junit.framework.Assert;
import junit.framework.TestCase;

public class Support_HttpServer {

    private static final int timeout = 10000;

    public static final String AUTHTEST = "/authTest";

    public static final String CHUNKEDTEST = "/chunkedTest.html";

    public static final String CONTENTTEST = "/contentTest.html";

    public static final String REDIRECTTEST = "/redirectTest.html";

    public static final String PORTREDIRTEST = "/portredirTest.html";

    public static final String OTHERTEST = "/otherTest.html";

    public static final String POSTTEST = "/postTest.html";

    public static final String HEADERSTEST = "/headersTest.html";

    public static final int OK = 200;

    public static final int MULT_CHOICE = 300;

    public static final int MOVED_PERM = 301;

    public static final int FOUND = 302;

    public static final int SEE_OTHER = 303;

    public static final int NOT_MODIFIED = 304;

    public static final int UNUSED = 306;

    public static final int TEMP_REDIRECT = 307;

    public static final int UNAUTHORIZED = 401;

    public static final int NOT_FOUND = 404;

    private int port;

    private boolean proxy = false;

    private boolean started = false;

    private boolean portRedirectTestEnable = false;

    private volatile Support_ServerSocket serversocket;

    private boolean shuttingDown = false;

    private final Object lock = new Object();

    TestCase testcase = null;

    public Support_HttpServer(Support_ServerSocket serversocket, TestCase test) {
        this.serversocket = serversocket;
        this.testcase = test;
    }

    public int getPort() {
        return port;
    }

    public void startServer(final int port) {
        if (started) {
            return;
        }
        started = true;
        this.port = port;
        Thread serverThread = new Thread(new Runnable() {

            public void run() {
                try {
                    synchronized (lock) {
                        serversocket.setPort(port);
                        serversocket.setTimeout(timeout);
                        serversocket.open();
                        lock.notifyAll();
                    }
                    while (true) {
                        Support_Socket socket = serversocket.accept();
                        Thread thread = new Thread(new ServerThread(socket));
                        thread.start();
                    }
                } catch (InterruptedIOException e) {
                    System.out.println("Wait timed out.  Test HTTP Server shut down.");
                    started = false;
                    try {
                        if (serversocket != null) {
                            serversocket.close();
                        }
                    } catch (IOException e2) {
                    }
                } catch (IOException e) {
                    if (!shuttingDown) {
                        e.printStackTrace();
                        Assert.fail("Test server error on HTTP Server on port " + port + ": " + e);
                    }
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                } finally {
                    try {
                        if (serversocket != null) {
                            serversocket.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        synchronized (lock) {
            serverThread.start();
            try {
                lock.wait();
            } catch (InterruptedException e) {
                System.err.println("Unexpected interrupt 2");
                e.printStackTrace();
            }
        }
    }

    public void stopServer() {
        try {
            shuttingDown = true;
            serversocket.close();
        } catch (IOException e) {
        }
    }

    class ServerThread implements Runnable {

        Support_Socket socket;

        ServerThread(Support_Socket s) {
            socket = s;
        }

        String readln(InputStream is) throws IOException {
            boolean lastCr = false;
            StringBuffer result = new StringBuffer();
            int c = is.read();
            if (c < 0) {
                return null;
            }
            while (c != '\n') {
                if (lastCr) {
                    result.append('\r');
                    lastCr = false;
                }
                if (c == '\r') {
                    lastCr = true;
                } else {
                    result.append((char) c);
                }
                c = is.read();
                if (c < 0) {
                    break;
                }
            }
            return result.toString();
        }

        void print(OutputStream os, String text) throws IOException {
            os.write(text.getBytes("ISO8859_1"));
        }

        public void run() {
            try {
                InputStream in = socket.getInputStream();
                String line;
                boolean authenticated = false, contentLength = false, chunked = false;
                int length = -1;
                String resourceName = "";
                Vector<String> headers = new Vector<String>();
                while (((line = readln(in)) != null) && (line.length() > 1)) {
                    headers.addElement(line);
                    String lline = line.toLowerCase();
                    if (lline.startsWith("get") || lline.startsWith("post")) {
                        int start = line.indexOf(' ') + 1;
                        int end = line.indexOf(' ', start);
                        if (start > 0 && end > -1) {
                            resourceName = line.substring(start, end);
                        }
                    }
                    if (lline.startsWith("authorization:")) {
                        authenticated = true;
                    }
                    if (lline.startsWith("content-length")) {
                        if (contentLength) {
                            Assert.fail("Duplicate Content-Length: " + line);
                        }
                        contentLength = true;
                        length = Integer.parseInt(line.substring(line.indexOf(' ') + 1));
                    }
                    if (line.startsWith("transfer-encoding")) {
                        if (chunked) {
                            Assert.fail("Duplicate Transfer-Encoding: " + line);
                        }
                        chunked = true;
                        String encoding = line.substring(line.indexOf(' ') + 1);
                        if ("chunked".equals(encoding)) {
                            Assert.fail("Unknown Transfer-Encoding: " + encoding);
                        }
                    }
                }
                if (contentLength && chunked) {
                    Assert.fail("Found both Content-Length and Transfer-Encoding");
                }
                if (resourceName.equals(CHUNKEDTEST)) {
                    chunkedTest();
                } else if (resourceName.equals(CONTENTTEST)) {
                    contentTest();
                } else if (resourceName.equals(AUTHTEST)) {
                    authenticateTest(authenticated);
                } else if (resourceName.startsWith(REDIRECTTEST)) {
                    redirectTest(resourceName);
                } else if (portRedirectTestEnable && resourceName.equals(PORTREDIRTEST)) {
                    contentTest();
                } else if (resourceName.equals(OTHERTEST)) {
                    otherTest();
                } else if (resourceName.equals(HEADERSTEST)) {
                    headersTest(headers);
                } else if (resourceName.startsWith("http://") && resourceName.indexOf(OTHERTEST) > -1) {
                    otherTest();
                } else if (resourceName.equals(POSTTEST)) {
                    postTest(length, in);
                } else {
                    notFound();
                }
                in.close();
                socket.close();
            } catch (IOException e) {
                System.err.println("Error performing http server test.");
                e.printStackTrace();
            }
        }

        private void contentTest() {
            try {
                OutputStream os = socket.getOutputStream();
                print(os, "HTTP/1.1 " + OK + " OK\r\n");
                print(os, "Content-Length: 5\r\n");
                print(os, "\r\nABCDE");
                os.flush();
                os.close();
            } catch (IOException e) {
                System.err.println("Error performing content coding test.");
                e.printStackTrace();
            }
        }

        private void chunkedTest() {
            try {
                OutputStream os = socket.getOutputStream();
                print(os, "HTTP/1.1 " + OK + " OK\r\n");
                print(os, "Transfer-Encoding: chunked\r\n");
                print(os, "\r\n");
                print(os, "5\r\nFGHIJ");
                print(os, "\r\n0\r\n\r\n");
                os.flush();
                os.close();
            } catch (IOException e) {
                System.err.println("Error performing chunked transfer coding test.");
                e.printStackTrace();
            }
        }

        private void authenticateTest(boolean authenticated) {
            try {
                OutputStream os = socket.getOutputStream();
                if (!authenticated) {
                    print(os, "HTTP/1.1 " + UNAUTHORIZED + " Unauthorized\r\n");
                    print(os, "WWW-Authenticate: Basic realm=\"test\"\r\n");
                } else {
                    print(os, "HTTP/1.1 " + OK + " OK\r\n");
                }
                print(os, "Content-Length: 5\r\n");
                print(os, "\r\nKLMNO");
                os.flush();
                os.close();
            } catch (IOException e) {
                System.err.println("Error performing authentication test.");
                e.printStackTrace();
            }
        }

        private void redirectTest(String test) {
            int responseNum = Integer.parseInt(test.substring(test.indexOf('3'), test.indexOf('3') + 3));
            String location = test.substring(test.lastIndexOf('-') + 1);
            try {
                OutputStream os = socket.getOutputStream();
                print(os, "HTTP/1.1 " + responseNum + " Irrelevant\r\n");
                print(os, "Location: " + location + "\r\n");
                print(os, "Content-Length: 5\r\n");
                print(os, "\r\nPQRST");
                os.flush();
                os.close();
            } catch (IOException e) {
                System.err.println("Error performing redirection test.");
                e.printStackTrace();
            }
        }

        private void otherTest() {
            try {
                OutputStream os = socket.getOutputStream();
                if (!proxy) {
                    print(os, "HTTP/1.1 " + 305 + " Use Proxy\r\n");
                    print(os, "Location: http://localhost:" + (port + 1) + "/otherTest.html\r\n");
                    print(os, "Content-Length: 9\r\n");
                    print(os, "\r\nNOT PROXY");
                } else {
                    print(os, "HTTP/1.1 " + OK + " OK\r\n");
                    print(os, "Content-Length: 5\r\n");
                    print(os, "\r\nPROXY");
                }
                os.flush();
                os.close();
            } catch (IOException e) {
                System.err.println("Error performing content coding test.");
                e.printStackTrace();
            }
        }

        private void headersTest(Vector<String> headers) {
            int found = 0;
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.elementAt(i);
                if (header.startsWith("header1:")) {
                    found++;
                    Assert.assertTrue("unexpected header: " + header, found == 1);
                    Assert.assertTrue("invalid header: " + header, "header1: value2".equals(header));
                }
            }
            try {
                OutputStream os = socket.getOutputStream();
                print(os, "HTTP/1.1 " + OK + " OK\r\n");
                print(os, "Cache-Control: no-cache=\"set-cookie\"\r\n");
                print(os, "Cache-Control: private\r\n");
                print(os, "Cache-Control: no-transform\r\n\r\n");
                os.flush();
                os.close();
            } catch (IOException e) {
                System.err.println("Error performing headers test.");
                e.printStackTrace();
            }
        }

        /**
		 * Method postTest.
		 */
        private void postTest(int length, InputStream in) {
            try {
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                for (int i = 0; i < length; i++) {
                    data.write(in.read());
                }
                if (length == -1) {
                    int len = in.read() - 48;
                    in.read();
                    in.read();
                    while (len > 0) {
                        for (int i = 0; i < len; i++) {
                            data.write(in.read());
                        }
                        in.read();
                        in.read();
                        len = in.read() - 48;
                        in.read();
                        in.read();
                    }
                    in.read();
                    in.read();
                }
                OutputStream os = socket.getOutputStream();
                print(os, "HTTP/1.1 " + OK + " OK\r\n");
                print(os, "Content-Length: " + data.size() + "\r\n\r\n");
                os.write(data.toByteArray());
                os.flush();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void notFound() {
            try {
                OutputStream os = socket.getOutputStream();
                print(os, "HTTP/1.1 " + NOT_FOUND + " Not Found\r\n");
                print(os, "Content-Length: 1\r\n");
                print(os, "\r\nZ");
                os.flush();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Sets portRedirectTestEnable.
	 * 
	 * @param portRedirectTestEnable
	 *            The portRedirectTestEnable to set
	 */
    public void setPortRedirectTestEnable(boolean portRedirectTestEnable) {
        this.portRedirectTestEnable = portRedirectTestEnable;
    }

    /**
	 * Sets the proxy.
	 * 
	 * @param proxy
	 *            The proxy to set
	 */
    public void setProxy(boolean proxy) {
        this.proxy = proxy;
    }
}
