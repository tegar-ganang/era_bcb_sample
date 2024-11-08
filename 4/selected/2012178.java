package rabbit.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rabbit.http.HTTPHeader;
import rabbit.io.CopyThread;
import rabbit.io.HTTPInputStream;
import rabbit.io.MultiOutputStream;
import rabbit.io.WebConnection;
import rabbit.util.Logger;

/** Tunnel data, useful for things like ssl and ntlm connections. 
 *  Used when we know that the connection has to be kept alive 
 *  without rabbit doing stuff. 
 */
class TunnelHandler {

    private Proxy proxy;

    private Selector selector;

    private Thread selectorThread;

    private List<SocketChannel> readers = new ArrayList<SocketChannel>();

    private List<SocketChannel> writers = new ArrayList<SocketChannel>();

    private Map<SocketChannel, Tunnel> tunnels = new HashMap<SocketChannel, Tunnel>();

    private int counter = 5;

    public enum HandleMode {

        THREADED, CHANNELED
    }

    ;

    public TunnelHandler(Proxy proxy) {
        this.proxy = proxy;
    }

    /** Check if we must tunnel a request. 
     *  Currently will only check if the Authorization starts with NTLM or Negotiate. 
     * @param header the client request 
     * @param rh the request handler. 
     */
    protected boolean mustTunnel(HTTPHeader header, Connection.RequestHandler rh) {
        String auth = header.getHeader("Authorization");
        if (auth != null) {
            if (auth.startsWith("NTLM") || auth.startsWith("Negotiate")) return true;
        }
        return false;
    }

    /** Tunnel data. 
     * @param in the client input stream.
     * @param out the client output stream.
     * @param rh the request handler with a valid web connection. 
     */
    protected void handleTunnel(HTTPInputStream in, MultiOutputStream out, Connection.RequestHandler rh) {
        try {
            proxy.getCounter().inc("Tunneled connections");
            out.writeHTTPHeader(rh.webheader);
            tunnel(in, out, rh.wc);
        } catch (SocketException e) {
        } catch (SocketTimeoutException e) {
        } catch (IOException e) {
            proxy.logError(Logger.WARN, "Odd tunnel connection " + e);
        } finally {
            if (rh.wc != null) {
                try {
                    rh.wc.getOutputStream().close();
                } catch (IOException e) {
                }
                try {
                    rh.wc.getInputStream().close();
                } catch (IOException e) {
                }
            }
        }
    }

    /** Tunnel all data between the requester and the current connection.
     *  Used to handle SSL and other connections. 
     * @param in the client input stream
     * @param out the client output stream.
     * @param wc the WebConnection to tunnel data to/from.
     */
    protected HandleMode tunnel(HTTPInputStream in, MultiOutputStream out, WebConnection wc) throws IOException {
        SocketChannel rcc = in.getSocketChannel();
        SocketChannel wcc = (SocketChannel) out.getChannel();
        if (rcc != null && wcc != null) {
            SocketChannel rwc = (SocketChannel) wc.getOutputStream().getChannel();
            SocketChannel wwc = wc.getInputStream().getSocketChannel();
            if (rwc != null && wwc != null) {
                proxy.getCounter().inc("Tunneles handled by channels");
                channelTunnel(rcc, wcc, rwc, wwc);
                return HandleMode.CHANNELED;
            }
        }
        proxy.getCounter().inc("Tunneles handled by threads");
        OutputStream server = wc.getOutputStream();
        InputStream resp = wc.getInputStream();
        byte[] buf = new byte[1024];
        int read;
        new CopyThread(resp, out);
        while ((read = in.read(buf)) > 0) {
            server.write(buf, 0, read);
            server.flush();
        }
        return HandleMode.THREADED;
    }

    private void setupSelector() throws IOException {
        selector = Selector.open();
        selectorThread = new Thread(new Runnable() {

            public void run() {
                while (selector != null && selector.isOpen()) {
                    try {
                        int num = selector.select(10 * 1000);
                        handleSelects();
                        addTunnels();
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (--counter == 0) System.exit(-1);
                    }
                }
            }
        }, "TunnelHandler");
        selectorThread.start();
    }

    private void handleSelects() throws IOException {
        Set<SelectionKey> selected = selector.selectedKeys();
        ByteBuffer b = ByteBuffer.allocate(2048);
        for (Iterator<SelectionKey> i = selected.iterator(); i.hasNext(); ) {
            SelectionKey sk = i.next();
            i.remove();
            try {
                if (!sk.isValid()) continue;
                if (sk.isReadable()) {
                    Tunnel t = (Tunnel) sk.attachment();
                    b.clear();
                    try {
                        int read = t.in.read(b);
                        if (read == -1) {
                            closeTunnel(t);
                        } else {
                            b.flip();
                            int written = t.out.write(b);
                            if (written < read) {
                                t.toWrite = b;
                                t.out.register(selector, SelectionKey.OP_WRITE, t);
                                if (i.hasNext()) b = ByteBuffer.allocate(2048);
                            } else {
                                t.in.register(selector, SelectionKey.OP_READ, t);
                            }
                        }
                    } catch (IOException e) {
                        proxy.logError(Logger.WARN, "error reading tunnel data " + e);
                        closeTunnel(t);
                    }
                } else if (sk.isWritable()) {
                    Tunnel t = (Tunnel) sk.attachment();
                    t.out.write(t.toWrite);
                    if (t.toWrite.hasRemaining()) t.out.register(selector, SelectionKey.OP_WRITE, t);
                }
            } catch (CancelledKeyException cke) {
                proxy.logError(Logger.WARN, "Cancelled key " + cke);
            }
        }
    }

    private void closeTunnel(Tunnel t1) throws IOException {
        Tunnel t2 = t1.otherDirection;
        tunnels.remove(t1.in);
        tunnels.remove(t2.in);
        shutdown(t1);
        for (SelectionKey sk : selector.keys()) {
            if (sk.attachment() == t2) {
                sk.cancel();
            }
        }
        shutdown(t2);
        t1.in.close();
        t2.in.close();
    }

    private void shutdown(Tunnel t1) {
        if (t1.in.isConnected() && t1.in.isOpen()) try {
            t1.in.socket().shutdownInput();
        } catch (IOException e) {
            proxy.logError(Logger.WARN, "error shutting down tunnel input: " + e);
        }
        if (t1.out.isConnected() && t1.out.isOpen()) try {
            t1.out.socket().shutdownOutput();
        } catch (IOException e) {
            proxy.logError(Logger.WARN, "error shutting down tunnel output: " + e);
        }
    }

    private void addTunnels() throws IOException {
        long now = System.currentTimeMillis();
        synchronized (readers) {
            for (SocketChannel sc : readers) {
                Tunnel t = tunnels.get(sc);
                t.registered = now;
                sc.register(selector, SelectionKey.OP_READ, t);
            }
            readers.clear();
        }
        synchronized (writers) {
            for (SocketChannel sc : writers) {
                Tunnel t = tunnels.get(sc);
                t.registered = now;
                sc.register(selector, SelectionKey.OP_WRITE, t);
            }
            writers.clear();
        }
    }

    private static class Tunnel {

        public SocketChannel in;

        public SocketChannel out;

        public Tunnel otherDirection;

        public long registered;

        public ByteBuffer toWrite;

        public Tunnel(SocketChannel in, SocketChannel out) {
            this.in = in;
            this.out = out;
        }
    }

    private synchronized void channelTunnel(SocketChannel clientIn, SocketChannel clientOut, SocketChannel webIn, SocketChannel webOut) throws IOException {
        if (selector == null) setupSelector();
        clientIn.configureBlocking(false);
        webIn.configureBlocking(false);
        Tunnel t1 = new Tunnel(clientIn, webOut);
        Tunnel t2 = new Tunnel(webIn, clientOut);
        t1.otherDirection = t2;
        t2.otherDirection = t1;
        tunnels.put(clientIn, t1);
        tunnels.put(webIn, t2);
        synchronized (readers) {
            readers.add(clientIn);
            readers.add(webIn);
        }
        selector.wakeup();
    }
}
