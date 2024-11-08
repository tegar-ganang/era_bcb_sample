package rabbit.io;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rabbit.http.HTTPHeader;
import rabbit.util.Counter;
import rabbit.util.IllegalConfigurationException;
import rabbit.util.Logger;
import rabbit.util.SProperties;

/** A class to handle the connections to the net. Should reuse connections if possible.
 */
public class ConnectionHandler implements Runnable {

    private Logger logger;

    private Counter counter;

    private Resolver resolver;

    private NLSOHandler nlsoHandler;

    private boolean running = true;

    private Map<Address, List<WebConnection>> activeConnections;

    private Map<Address, List<WebConnection>> pipelineConnections;

    private Thread cleaner;

    private long keepalivetime = 1000;

    private boolean usePipelining = true;

    private Selector selector = null;

    /** Create a new ConnectionHandler for use. 
     */
    public ConnectionHandler(Logger logger, Counter counter, Resolver resolver, NLSOHandler nlsoHandler) throws IOException {
        this.logger = logger;
        this.counter = counter;
        this.resolver = resolver;
        this.nlsoHandler = nlsoHandler;
        activeConnections = Collections.synchronizedMap(new HashMap<Address, List<WebConnection>>());
        pipelineConnections = Collections.synchronizedMap(new HashMap<Address, List<WebConnection>>());
        selector = Selector.open();
        cleaner = new Thread(this, getClass().getName() + ".cleaner");
        cleaner.setDaemon(true);
        cleaner.start();
    }

    /** Check if the cleaner of this ConnectionHandler is running.
     */
    public boolean isCleanerRunning() {
        return running;
    }

    /** Get the addresses we have connections to.
     */
    public Iterator<Address> getAddresses() {
        return activeConnections.keySet().iterator();
    }

    /** Get the pipeline address we have. 
     */
    public Iterator<Address> getPipelineAddresses() {
        return pipelineConnections.keySet().iterator();
    }

    /** Get the pool for an Address.
     *  NOTE! synchronize on the pool if you are taking connections from it.
     */
    public List<WebConnection> getPool(Address a) {
        return activeConnections.get(a);
    }

    /** Get the pool for an Address.
     *  NOTE! synchronize on the pool if you are taking connections from it.
     */
    public List<WebConnection> getPipelinePool(Address a) {
        return pipelineConnections.get(a);
    }

    /** A class to handle the addresses of the connections.
     *  Basically just a pair of InetAddress and port number.
     */
    public static class Address {

        /** The internet address of this Address. */
        public InetAddress ia;

        /** The port number were connected to. */
        public int port;

        /** The hash code.*/
        private int hash;

        /** Create a new Address with given InetAddress and port 
	 * @param ia the InetAddress this Address is connected to.
	 * @param port the port number this Address is connected to.
	 */
        public Address(InetAddress ia, int port) {
            this.ia = ia;
            this.port = port;
            String s = ia.getHostAddress() + ":" + port;
            hash = s.hashCode();
        }

        /** Get the hash code for this object.
	 * @return the hash code.
	 */
        public int hashCode() {
            return hash;
        }

        /** Compare this objcet agains another object.
	 * @param o the Object to compare against.
	 * @return true if the other Object is an Address connected to 
	 *  the same InetAddress and port, false otherwise.
	 */
        public boolean equals(Object o) {
            if (o instanceof Address) {
                Address a = (Address) o;
                return (port == a.port && ia.equals(a.ia));
            }
            return false;
        }

        /** Get a String representation of this Address */
        public String toString() {
            return ia + ":" + port;
        }
    }

    /** Get a WebConnection for the given header.
     * @param header the HTTPHeader containing the URL to connect to.
     * @return a WebConnection.
     */
    public WebConnection getConnection(HTTPHeader header) throws IOException {
        String error = null;
        try {
            WebConnection wc = null;
            counter.inc("WebConnections used");
            String requri = header.getRequestURI();
            URL url = new URL(requri);
            InetAddress ia = resolver.getInetAddress(url);
            int port = url.getPort() > 0 ? url.getPort() : 80;
            port = resolver.getConnectPort(port);
            Address a = new Address(ia, port);
            String method = header.getMethod().trim();
            if (!(method != null && (method.equals("GET") || method.equals("HEAD")))) {
                return new WebConnection(ia, port, resolver.isProxyConnected(), resolver.getProxyAuthString(), counter, logger, nlsoHandler);
            }
            List<WebConnection> pool = null;
            if (usePipelining) {
                pool = pipelineConnections.get(a);
                if (pool != null) {
                    synchronized (pool) {
                        if (pool.size() > 0) {
                            wc = pool.get(pool.size() - 1);
                            pool.remove(pool.size() - 1);
                            return wc;
                        }
                    }
                }
            }
            pool = activeConnections.get(a);
            if (pool == null) {
                return new WebConnection(ia, port, resolver.isProxyConnected(), resolver.getProxyAuthString(), counter, logger, nlsoHandler);
            }
            return getPooledConnection(ia, port, pool);
        } catch (UnknownHostException e) {
            logger.logError(Logger.WARN, "Could not find the host: '" + e + "':\n" + header);
            error = e.toString();
        } catch (MalformedURLException e) {
            logger.logError(Logger.WARN, "Bad URI in request: '" + e + "':\n" + header);
            error = e.toString();
        } catch (IllegalArgumentException e) {
            logger.logError(Logger.WARN, "Bad port number in request: '" + e + "':\n" + header);
            error = e.toString();
        }
        throw new IOException("Could not connect to host: " + error);
    }

    private WebConnection getPooledConnection(InetAddress ia, int port, List<WebConnection> pool) throws IOException {
        WebConnection wc = null;
        synchronized (pool) {
            if (pool.size() > 0) {
                wc = pool.remove(pool.size() - 1);
                synchronized (selector) {
                    selector.selectNow();
                    Set<SelectionKey> set = selector.selectedKeys();
                    SelectionKey sk = wc.getSelectionKey();
                    wc.setSelectionKey(null);
                    if (!set.contains(sk)) {
                        logger.logError(Logger.WARN, "Keepalive connection not in selected set, ignoring");
                        wc.close();
                        return getPooledConnection(ia, port, pool);
                    }
                    sk.cancel();
                    wc.getChannel().configureBlocking(true);
                    return wc;
                }
            } else {
                wc = new WebConnection(ia, port, resolver.isProxyConnected(), resolver.getProxyAuthString(), counter, logger, nlsoHandler);
            }
            return wc;
        }
    }

    private void removeFromPool(WebConnection wc, Map<Address, List<WebConnection>> conns, Address a) {
        synchronized (conns) {
            List<WebConnection> pool = conns.get(a);
            if (pool != null) {
                int i = pool.indexOf(wc);
                if (i >= 0) pool.remove(i);
            }
        }
    }

    /** Return a WebConnection to the pool so that it may be reused.
     * @param wc the WebConnection to return.
     */
    public void releaseConnection(WebConnection wc) {
        counter.inc("WebConnections released");
        if (wc.getInetAddress() == null) {
            wc.close();
            return;
        }
        Address a = new Address(wc.getInetAddress(), wc.getPort());
        if (!wc.getKeepAlive()) {
            removeFromPool(wc, pipelineConnections, a);
            wc.close();
            return;
        }
        boolean waiting = false;
        synchronized (wc) {
            waiting = wc.getWaiting();
            wc.setReleased();
        }
        if (!waiting) {
            removeFromPool(wc, pipelineConnections, a);
            synchronized (activeConnections) {
                List<WebConnection> pool = activeConnections.get(a);
                if (pool == null) {
                    pool = new ArrayList<WebConnection>();
                    activeConnections.put(a, pool);
                }
                try {
                    wc.getChannel().configureBlocking(false);
                    SelectionKey sk = wc.getChannel().register(selector, SelectionKey.OP_WRITE);
                    wc.setSelectionKey(sk);
                    pool.add(wc);
                    wc = null;
                } catch (CancelledKeyException cke) {
                    logger.logError(Logger.ALL, "Cancelled key, ignoring keepalive: " + cke);
                } catch (ClosedChannelException cce) {
                    logger.logError(Logger.ALL, "Closed channel, ignoring keepalive: " + cce);
                } catch (IOException e) {
                    logger.logError(Logger.ALL, "failed to configure blocking: " + e);
                } finally {
                    if (wc != null) {
                        wc.close();
                    }
                }
            }
        }
    }

    /** Mark a WebConnection ready for pipelining.
     * @param wc the WebConnection to mark ready for pipelining.
     */
    public void markForPipelining(WebConnection wc) {
        if (!usePipelining) return;
        synchronized (wc) {
            if (!wc.getKeepAlive()) return;
            if (wc.getWaiting()) {
                System.err.println(Thread.currentThread() + ": " + wc + " pipeline waiting?");
                return;
            }
            wc.setPipeline(true);
        }
        Address a = new Address(wc.getInetAddress(), wc.getPort());
        synchronized (this) {
            List<WebConnection> pool = pipelineConnections.get(a);
            if (pool == null) {
                pool = new ArrayList<WebConnection>();
                pipelineConnections.put(a, pool);
            }
            if (pool.indexOf(wc) >= 0) {
                System.err.println(Thread.currentThread() + ": " + wc + " already in pool...");
            }
            pool.add(wc);
        }
    }

    /** Set the keep alive time for this handler.
     * @param milis the keep alive time in miliseconds.
     */
    public void setKeepaliveTime(long milis) {
        keepalivetime = milis;
    }

    /** Get the current keep alive time.
     * @return the keep alive time in miliseconds.
     */
    public long getKeepaliveTime() {
        return keepalivetime;
    }

    /** The cleaner thread.
     */
    public void run() {
        while (running) {
            Date d = new Date();
            d.setTime(d.getTime() - keepalivetime);
            synchronized (activeConnections) {
                Iterator<Map.Entry<Address, List<WebConnection>>> e = activeConnections.entrySet().iterator();
                while (e.hasNext()) {
                    Map.Entry<Address, List<WebConnection>> me = e.next();
                    List<WebConnection> v = me.getValue();
                    synchronized (v) {
                        int vsize = v.size();
                        for (int i = vsize - 1; i >= 0; i--) {
                            WebConnection wc = v.get(i);
                            Date rd = wc.getReleasedAt();
                            if (rd == null || rd.before(d)) {
                                v.remove(i);
                                wc.close();
                                SelectionKey sk = wc.getSelectionKey();
                                if (sk != null) sk.cancel();
                            }
                        }
                        if (v.size() == 0) {
                            e.remove();
                        }
                    }
                }
            }
            synchronized (selector) {
                try {
                    selector.selectNow();
                } catch (IOException e) {
                    logger.logError(Logger.WARN, "failed to select during cleanup: " + e);
                }
            }
            try {
                Thread.sleep(keepalivetime);
            } catch (InterruptedException ie) {
            }
        }
    }

    /** Configure the connection handler system from the given config.
     * @param config the properties describing the cache settings.
     * @throws IllegalConfigurationException if some setting is strange.
     */
    public void setup(SProperties config) throws IllegalConfigurationException {
        if (config == null) return;
        String kat = config.getProperty("keepalivetime", "1000");
        try {
            setKeepaliveTime(Long.parseLong(kat));
        } catch (NumberFormatException e) {
            throw (new IllegalConfigurationException("Bad number for ConnectionHandler keepalivetime: '" + kat + "'"));
        }
        String up = config.get("usepipelining");
        if (up == null) up = "true";
        usePipelining = up.equalsIgnoreCase("true");
    }
}
