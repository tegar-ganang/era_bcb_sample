package rabbit.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import rabbit.cache.NCache;
import rabbit.filter.HTTPFilter;
import rabbit.filter.IPAccessFilter;
import rabbit.handler.HandlerFactory;
import rabbit.http.GeneralHeader;
import rabbit.http.HTTPHeader;
import rabbit.io.ConnectionHandler;
import rabbit.io.NLSOHandler;
import rabbit.io.Resolver;
import rabbit.io.WebConnection;
import rabbit.util.Config;
import rabbit.util.Counter;
import rabbit.util.IllegalConfigurationException;
import rabbit.util.Logger;
import rabbit.util.RestartableThreadFactory;
import rabbit.util.SProperties;
import rabbit.util.ThreadPool;

/** This is the central dispatcher for RabbIT.
 *
 *  Basically it sits in a loop accepting connection and for each
 *  connection creating a handler for it.
 *  
 *  Also handles logging so it goes out nicely formatted (and synchronized).
 *
 */
public class Proxy implements Runnable, Logger, Resolver {

    /** Current version */
    public static final String VERSION = "RabbIT proxy version 2.0.41";

    /** The standard configuration file */
    protected static final String CONFIG = "conf/rabbit.conf";

    /** Maximum number of concurrent connections */
    private int maxconnections = 50;

    /** The current connections */
    private List<Connection> convec = Collections.synchronizedList(new ArrayList<Connection>());

    /** The log of things */
    private Counter log = new Counter();

    /** The config filename for this session */
    private String configfile = CONFIG;

    /** The current configuration */
    private Config config = new Config();

    /** Output for errors */
    private LogWriter errorlog = new LogWriter(System.err, true);

    /** monitor for error log. */
    private Object errorMonitor = new Object();

    /** Output for accesses */
    private LogWriter accesslog = new LogWriter(System.out, true);

    /** monitor for access log. */
    private Object accessMonitor = new Object();

    /** The current error log level. */
    private int loglevel = MSG;

    /** The time the proxy started */
    private Date started;

    /** The local adress of the proxy. */
    private InetAddress localhost;

    /** The local time zone the proxy is running in. */
    private TimeZone tz;

    /** The distance to GMT in milis. */
    private long offset;

    /** The port the proxy is using. */
    private int port = -1;

    /** The serversocket the proxy is using. */
    private ServerSocketChannel ssc = null;

    /** The selector the proxy is using. */
    private Selector selector = null;

    /** The queue of returned sockets. */
    private List<SocketChannel> returnedSockets = Collections.synchronizedList(new ArrayList<SocketChannel>());

    /** Adress of connected proxy. */
    private InetAddress proxy = null;

    /** Port of the connected proxy. */
    private int proxyport = -1;

    /** The cache-handler */
    private NCache cache = new NCache();

    /** The connection handler */
    private ConnectionHandler conhandler;

    /** The identity of this server. */
    private String serverIdentity = VERSION;

    /** The format we write dates on. */
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss 'GMT'");

    /** The monitor for sdf. */
    private Object sdfMonitor = new Object();

    /** Is the proxy listening. */
    private volatile boolean accepting = false;

    /** the hash tables with type=>Class mapping */
    private Map<String, HandlerFactory> handlers = new HashMap<String, HandlerFactory>();

    /** the hash tables with type=>Class mapping for cache handling. */
    private Map<String, HandlerFactory> cachehandlers = new HashMap<String, HandlerFactory>();

    /** the filters, a List of classes (in given order) */
    private List<IPAccessFilter> accessfilters = new ArrayList<IPAccessFilter>();

    /** the filters, a List of classes (in given order) */
    private List<HTTPFilter> httpinfilters = new ArrayList<HTTPFilter>();

    /** the filters, a List of classes (in given order) */
    private List<HTTPFilter> httpoutfilters = new ArrayList<HTTPFilter>();

    /** Are we allowed to proxy ssl? */
    protected boolean proxySSL = false;

    /** The List of acceptable ssl-ports. */
    protected List<Integer> sslports = null;

    /** The dns handler */
    protected DNSHandler dnsHandler;

    /** The tunnel handler. */
    private TunnelHandler tunnelHandler;

    /** The NLSOHandler */
    private NLSOHandler nlsoHandler;

    private static class ConnectionFactory implements RestartableThreadFactory<Connection> {

        private Proxy proxy;

        public ConnectionFactory(Proxy proxy) {
            this.proxy = proxy;
        }

        public Connection createThread() {
            return new Connection(proxy);
        }
    }

    private RestartableThreadFactory<Connection> tf = new ConnectionFactory(this);

    private ThreadPool<Connection> tp = new ThreadPool<Connection>(tf, maxconnections);

    /** Start a proxy. 
     *  Parse flags and read the config, then starts the proxy.
     * @param args the command-line flags given.
     */
    public static void main(String[] args) {
        int i = 0;
        Proxy instance = new Proxy();
        while (args.length > i) {
            if (args[i].equals("-f") || args[i].equals("--file")) {
                i++;
                if (args.length > i) {
                    instance.configfile = args[i];
                } else {
                    instance.logError(FATAL, "No config file specified");
                    System.exit(-1);
                }
            } else if (args[i].equals("-v") || args[i].equals("--version")) {
                System.out.println(VERSION);
                System.exit(0);
            } else if (args[i].equals("-?") || args[i].equals("-h") || args[i].equals("--help")) {
                printHelp(instance);
                System.exit(0);
            } else {
                System.out.println("Unsupported argument " + args[i]);
            }
            i++;
        }
        instance.loadConfig();
        Thread t = new Thread(instance, VERSION);
        t.start();
    }

    /** Constructor,
     */
    public Proxy() {
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            logError(FATAL, "I couldnt find the hostname of this machine, exiting.");
            System.exit(-1);
        }
        tz = sdf.getTimeZone();
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(new Date());
        offset = tz.getOffset(gc.get(Calendar.ERA), gc.get(Calendar.YEAR), gc.get(Calendar.MONTH), gc.get(Calendar.DAY_OF_MONTH), gc.get(Calendar.DAY_OF_WEEK), gc.get(Calendar.MILLISECOND));
        started = new Date();
    }

    /** Get the time zone the proxy is running in.
     *  This is neccessary to give dates in GMT.
     */
    public TimeZone getTimeZone() {
        return tz;
    }

    /** Get the offset in milis from GMT 
     *  NOTE! the offset is only calculated at startup.
     *  This means that during DST changes this may be invalid for a 
     *  day or two. Restart RabbIT dayly during that time to get it right
     *  _IF_ you feel it is a problem.
     */
    public long getOffset() {
        return offset;
    }

    /** Get a Connection that is ready to service.
     */
    private Connection getConnection() {
        return tp.getThread();
    }

    public ThreadPool.Usage getThreadPoolUsage() {
        return tp.getUsage();
    }

    /** Get the connection handler.
     */
    public ConnectionHandler getConnectionHandler() {
        return conhandler;
    }

    /** Get a WebConnection.
     */
    public WebConnection getWebConnection(HTTPHeader header) throws IOException {
        return conhandler.getConnection(header);
    }

    /** Release a WebConnection so that it may be reused if possible.
     * @param wc the WebConnection to release.
     */
    public void releaseWebConnection(WebConnection wc) {
        conhandler.releaseConnection(wc);
    }

    /** Mark a WebConnection for pipelining.
     * @param wc the WebConnection to mark.
     */
    public void markForPipelining(WebConnection wc) {
        conhandler.markForPipelining(wc);
    }

    /** while we can, accept new sockets, creating a handler for each
     *  and also save connections for statistics.
     */
    public void run() {
        logError(MSG, "Started");
        logError(MSG, "Running on: " + getHost());
        while (true) {
            if (!accepting || !selector.isOpen()) {
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e) {
                }
                continue;
            }
            try {
                int num = selector.select(10 * 1000);
                if (selector.isOpen()) {
                    handleSelects();
                    handleReturned();
                    cancelTimeouts();
                }
            } catch (IOException e) {
                logError(ERROR, "Failed to accept, trying to restart serversocket.");
                closeSocket();
                openSocket();
            } catch (Exception e) {
                logError(ERROR, "Unknown error: " + e);
                e.printStackTrace();
            }
        }
    }

    private void cancelTimeouts() throws IOException {
        long now = System.currentTimeMillis();
        for (SelectionKey sk : selector.keys()) {
            Object o = sk.attachment();
            if (o != null) {
                long when = (Long) o;
                if (now - when > 25 * 1000) {
                    sk.cancel();
                    try {
                        SocketChannel sc = (SocketChannel) sk.channel();
                        sc.socket().shutdownInput();
                        sc.socket().shutdownOutput();
                        sc.close();
                    } catch (IOException e) {
                        logError(ERROR, "failed to shutdown and close socket: " + e);
                    }
                }
            }
        }
    }

    private void handleReturned() throws IOException {
        synchronized (returnedSockets) {
            Long now = System.currentTimeMillis();
            for (SocketChannel sc : returnedSockets) {
                sc.configureBlocking(false);
                try {
                    SelectionKey scKey = sc.register(selector, SelectionKey.OP_READ, now);
                } catch (CancelledKeyException cke) {
                    logError(ALL, "Returned channel cancelled, ignoring: " + cke);
                    try {
                        sc.socket().close();
                    } catch (IOException e) {
                        logError(ALL, "Failed to close client socket: " + e);
                    }
                    try {
                        sc.close();
                    } catch (IOException e) {
                        logError(ALL, "Failed to close client socketchannel: " + e);
                    }
                }
            }
            returnedSockets.clear();
        }
    }

    private void handleSelects() throws IOException {
        Long now = System.currentTimeMillis();
        Set<SelectionKey> selected = selector.selectedKeys();
        for (Iterator<SelectionKey> i = selected.iterator(); i.hasNext(); ) {
            SelectionKey sk = i.next();
            i.remove();
            if (sk.isAcceptable()) {
                SocketChannel sc = ssc.accept();
                try {
                    sc.socket().setSoTimeout(25 * 1000);
                    log.inc("Socket accepts");
                    sc.configureBlocking(false);
                } catch (SocketException e) {
                    logError(WARN, "Socketoptions failed?: " + e);
                    try {
                        sc.close();
                    } catch (IOException ee) {
                    }
                }
                SelectionKey scKey = sc.register(selector, SelectionKey.OP_READ, now);
            } else if (sk.isReadable()) {
                SocketChannel sc = (SocketChannel) sk.channel();
                sk.cancel();
                sc.configureBlocking(true);
                Connection con = getConnection();
                convec.add(con);
                con.setSocket(sc);
            }
        }
    }

    protected void returnSocket(SocketChannel sc) throws IOException {
        returnedSockets.add(sc);
        selector.wakeup();
    }

    /** Open a socket on the specified port 
     *  also make the proxy continue accepting connections.
     */
    protected void openSocket() {
        int tport = Integer.parseInt(config.getProperty(getClass().getName(), "port", "9666").trim());
        if (tport != port) {
            try {
                port = tport;
                accepting = false;
                closeSocket();
                ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);
                ssc.socket().bind(new InetSocketAddress(port));
                accepting = true;
                selector = Selector.open();
                ssc.register(selector, SelectionKey.OP_ACCEPT);
            } catch (IOException e) {
                logError(FATAL, "Failed to open serversocket on port " + port);
                System.exit(-1);
            }
        }
    }

    /** Closes the serversocket and makes the proxy stop listening for
     *	connections. 
     */
    protected void closeSocket() {
        try {
            accepting = false;
            if (ssc != null) {
                selector.close();
                ssc.close();
                ssc = null;
            }
        } catch (IOException e) {
            logError(FATAL, "Failed to close serversocket on port " + port);
            System.exit(-1);
        }
    }

    /** print out the helptext to the user.
     */
    private static void printHelp(Logger logger) {
        try {
            byte[] b = new byte[4096];
            int i;
            InputStream f = new FileInputStream("Help.txt");
            while ((i = f.read(b)) > 0) System.out.write(b, 0, i);
            f.close();
        } catch (IOException e) {
            logger.logError(WARN, "Couldnt read helptext" + e);
        }
    }

    /** Read the configuration in and set up a lot of things...
     * @param filename the file to read the Config from 
     */
    private void loadConfig() {
        try {
            config = new Config(configfile);
        } catch (IOException e) {
            logError(FATAL, "Could not load the configuration file: '" + configfile + "' exiting");
            System.exit(-1);
        }
        setup(config);
    }

    /** Flush and close the logfile given.
     * @param w the logfile to close.
     */
    private void closeLog(LogWriter w) {
        if (w != null && !w.isSystemWriter()) {
            w.flush();
            w.close();
        }
    }

    /** Reconfigure the proxy during runtime.
     * @param config the new config.
     */
    public void reConfigure(Config config) {
        setup(config);
        saveConfig();
    }

    private void setupLogLevel() {
        String loglvl = config.getProperty(getClass().getName(), "loglevel", "MSG");
        loglevel = getErrorLevel(loglvl);
    }

    /** Configure the error log.
     */
    private void setupErrorLog() {
        String elog = config.getProperty(getClass().getName(), "errorlog", "logs/error_log");
        synchronized (errorMonitor) {
            try {
                closeLog(errorlog);
                if (!elog.equals("")) {
                    File f = new File(elog);
                    File p = new File(f.getParent());
                    if (!p.exists()) p.mkdirs();
                    errorlog = new LogWriter(new FileWriter(elog, true), true);
                } else errorlog = new LogWriter(System.err, true);
            } catch (IOException e) {
                logError(FATAL, "Could not create error log on '" + elog + "' exiting");
                System.exit(-1);
            }
        }
        config.setProperty(getClass().getName(), "errorlog", elog);
    }

    /** Configure the access log.
     */
    private void setupAccessLog() {
        String alog = config.getProperty(getClass().getName(), "accesslog", "logs/access_log");
        synchronized (accessMonitor) {
            try {
                closeLog(accesslog);
                if (!alog.equals("")) {
                    File f = new File(alog);
                    File p = new File(f.getParent());
                    if (!p.exists()) p.mkdirs();
                    accesslog = new LogWriter(new FileWriter(alog, true), true);
                } else accesslog = new LogWriter(System.out, true);
            } catch (IOException e) {
                logError(FATAL, "Could not create access log on '" + alog + "' exiting");
                System.exit(-1);
            }
        }
        config.setProperty(getClass().getName(), "accesslog", alog);
    }

    /** Rotate the logs.
     */
    public void rotateLogs() {
        logError(MSG, "log rotation requested.");
        Date d = new Date();
        SimpleDateFormat lf = new SimpleDateFormat("yyyy-MM-dd");
        String date = lf.format(d);
        String s = getClass().getName();
        String elog = config.getProperty(s, "errorlog", "logs/error_log");
        File f = new File(elog);
        File fn = new File(elog + "-" + date);
        synchronized (errorMonitor) {
            closeLog(errorlog);
            if (!f.renameTo(fn)) logError(ERROR, "failed to rotate error log!"); else setupErrorLog();
        }
        String alog = config.getProperty(s, "accesslog", "logs/access_log");
        f = new File(alog);
        fn = new File(alog + "-" + date);
        synchronized (accessMonitor) {
            closeLog(accesslog);
            if (!f.renameTo(fn)) logError(ERROR, "failed to rotate access log!"); else setupAccessLog();
        }
    }

    /** Configure the proxy RabbIT is using (if any). 
     */
    private void setupProxyConnection() {
        String pname = config.getProperty(getClass().getName(), "proxyhost", "");
        String pport = config.getProperty(getClass().getName(), "proxyport", "");
        if (!pname.equals("") && !pport.equals("")) {
            try {
                setProxy(pname);
            } catch (UnknownHostException e) {
                logError(FATAL, "Unknown proxyhost: '" + pname + "' exiting");
                System.exit(-1);
            }
            try {
                setProxyPort(Integer.parseInt(pport.trim()));
            } catch (NumberFormatException e) {
                logError(FATAL, "Strange proxyport: '" + pport + "' exiting");
                System.exit(-1);
            }
        }
        config.setProperty(getClass().getName(), "proxyhost", pname);
        config.setProperty(getClass().getName(), "proxyport", pport);
    }

    /** Set the proxy to use. 
     */
    public void setProxy(String proxyHost) throws UnknownHostException {
        proxy = dnsHandler.getInetAddress(proxyHost);
    }

    /** Set the proxy port to use. 
     */
    public void setProxyPort(int newProxyPort) {
        proxyport = newProxyPort;
    }

    /** Configure the SSL support RabbIT should have.
     */
    private void setupSSLSupport() {
        String ssl = config.getProperty(getClass().getName(), "allowSSL", "no").trim();
        if (ssl.equals("no")) {
            proxySSL = false;
        } else if (ssl.equals("yes")) {
            proxySSL = true;
            sslports = null;
        } else {
            proxySSL = true;
            sslports = new ArrayList<Integer>();
            StringTokenizer st = new StringTokenizer(ssl, ",");
            while (st.hasMoreTokens()) {
                String s = null;
                try {
                    sslports.add(new Integer(s = st.nextToken()));
                } catch (NumberFormatException e) {
                    logError(WARN, "bad number: '" + s + "' for ssl port, ignoring.");
                }
            }
        }
    }

    /** Configure the maximum number of simultanious connections we handle 
     */
    private void setupMaxConnections() {
        String mc = config.getProperty(getClass().getName(), "maxconnections", "500").trim();
        try {
            maxconnections = Integer.parseInt(mc);
            tp.setLimit(maxconnections);
        } catch (NumberFormatException e) {
            logError(WARN, "bad number for maxconnections: '" + mc + "', using old value: " + maxconnections);
        }
    }

    private void setupDNSHandler() {
        String dnsHandlerClass = config.getProperty(getClass().getName(), "dnsHandler", "rabbit.proxy.DNSJavaHandler");
        try {
            Class clz = Class.forName(dnsHandlerClass);
            dnsHandler = (DNSHandler) clz.newInstance();
            dnsHandler.setup(this);
        } catch (Exception e) {
            logError(ERROR, "Unable to create and setup dns handler: " + e + ", will try to use default instead.");
            e.printStackTrace();
            dnsHandler = new DNSJavaHandler();
            dnsHandler.setup(this);
        }
    }

    private void setupTunnelHandler() {
        if (tunnelHandler == null) tunnelHandler = new TunnelHandler(this);
    }

    private void setupNLSOHandler() {
        try {
            if (nlsoHandler == null) nlsoHandler = new NLSOHandler(this);
        } catch (IOException e) {
            logError(ERROR, "Unable to create and setup nlsoHandler");
        }
    }

    private void setupConnectionHandler() {
        try {
            conhandler = new ConnectionHandler(this, log, this, nlsoHandler);
        } catch (IOException e) {
            logError(FATAL, "Failed to open connection handler: " + e);
        }
    }

    /** set things up according to the config
     * @param config the new config.
     */
    private void setup(Config config) {
        this.config = config;
        setupLogLevel();
        setupErrorLog();
        setupAccessLog();
        openSocket();
        setupDNSHandler();
        setupTunnelHandler();
        setupNLSOHandler();
        setupProxyConnection();
        setupConnectionHandler();
        try {
            cache.setup(config.getProperties(cache.getClass().getName()));
        } catch (IllegalConfigurationException e) {
            logError(ERROR, e.getMessage());
        }
        try {
            conhandler.setup(config.getProperties(conhandler.getClass().getName()));
        } catch (IllegalConfigurationException e) {
            logError(ERROR, e.getMessage());
        }
        loadClasses();
        setupMaxConnections();
        setupSSLSupport();
        serverIdentity = config.getProperty(getClass().getName(), "serverIdentity", VERSION);
        String strictHTTP = config.getProperty(getClass().getName(), "StrictHTTP", "true");
        GeneralHeader.setStrictHTTP(strictHTTP.equals("true"));
        logError(MSG, "Configuration loaded, ready for action.");
    }

    /** save the config back to file
     */
    public synchronized void saveConfig() {
        try {
            FileOutputStream fos = new FileOutputStream(configfile);
            synchronized (sdfMonitor) {
                config.save(fos, "This file was automatically generated at " + sdf.format(new Date()));
            }
        } catch (IOException e) {
            logError(ERROR, "Coulnd not write the configuration file: '" + configfile + "' to disk");
        }
    }

    /** Get the actual error level from the given String.
     * @param errorlevel the String to translate.
     * @return the errorlevel suitable for the given String.
     */
    public int getErrorLevel(String errorlevel) {
        if (errorlevel.equals("DEBUG")) return DEBUG;
        if (errorlevel.equals("ALL")) return ALL;
        if (errorlevel.equals("INFO")) return INFO;
        if (errorlevel.equals("WARN")) return WARN;
        if (errorlevel.equals("MSG")) return MSG;
        if (errorlevel.equals("ERROR")) return ERROR;
        if (errorlevel.equals("FATAL")) return FATAL;
        return FATAL;
    }

    /** Get the String description of the given error level
     * @param errorlevel the int to translate to a String.
     * @return the String describing the errorlevel.
     */
    public String getErrorLevelString(int errorlevel) {
        if (errorlevel <= DEBUG) return "DEBUG";
        if (errorlevel <= ALL) return "ALL";
        if (errorlevel <= INFO) return "INFO";
        if (errorlevel <= WARN) return "WARN";
        if (errorlevel <= MSG) return "MSG";
        if (errorlevel <= ERROR) return "ERROR";
        if (errorlevel <= FATAL) return "FATAL";
        return "UNKNOWN";
    }

    /** log errors and other important stuff. 
     * @param error the thing that happend.
     */
    public void logError(String error) {
        logError(ERROR, error);
    }

    /** log errors and other important stuff. 
     * @param type the type of the error. 
     * @param error the thing that happend.
     */
    public void logError(int type, String error) {
        if (type < loglevel) return;
        String stype = getErrorLevelString(type);
        Date d = new Date();
        d.setTime(d.getTime() - offset);
        StringBuilder sb = new StringBuilder("[");
        synchronized (sdfMonitor) {
            sb.append(sdf.format(d));
        }
        sb.append("][");
        sb.append(stype);
        sb.append("][");
        sb.append(error);
        sb.append("]");
        synchronized (errorMonitor) {
            errorlog.println(sb.toString());
        }
    }

    public Config getConfig() {
        return config;
    }

    /** Get the cache the proxy are using.
     * @return the NCache the proxy currently are using.
     */
    public NCache getCache() {
        return cache;
    }

    /** Get the Log-handle
     * @return the Counter we use.
     */
    public Counter getCounter() {
        return log;
    }

    /** Get the port this proxy is using.
     * @return the port number the proxy is listening on.
     */
    public int getPort() {
        return port;
    }

    /** Get the local host.
     * @return the InetAddress of the host the proxy is running on.
     */
    public InetAddress getHost() {
        return localhost;
    }

    /** Try hard to check if the given address matches the proxy. 
     *  Will use the localhost name and all ip addresses.
     */
    public boolean isSelf(String uhost, int urlport) {
        if (urlport == getPort()) {
            String proxyhost = getHost().getHostName();
            if (uhost.equalsIgnoreCase(proxyhost)) return true;
            try {
                Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
                while (e.hasMoreElements()) {
                    NetworkInterface ni = e.nextElement();
                    Enumeration<InetAddress> ei = ni.getInetAddresses();
                    while (ei.hasMoreElements()) {
                        InetAddress ia = ei.nextElement();
                        if (ia.getHostAddress().equals(uhost)) return true;
                    }
                }
            } catch (SocketException e) {
                logError(WARN, "failed to get network interfaces: " + e);
            }
        }
        return false;
    }

    /** are we connected to another proxy? 
     * @return true if the proxy is connected to another proxy.
     */
    public boolean isProxyConnected() {
        return proxy != null;
    }

    /** Get the InetAddress to connect to.
     * @return the InetAddress to give the request
     */
    public InetAddress getInetAddress(URL url) throws UnknownHostException {
        if (isProxyConnected()) return proxy;
        return dnsHandler.getInetAddress(url);
    }

    /** Get the port to connect to.
     * @param port the port we want to connect to.
     * @return the port to connect to.
     */
    public int getConnectPort(int port) {
        if (isProxyConnected()) return proxyport;
        return port;
    }

    /** Get the authenticationstring to use for proxy.
     * @return an authentication string.
     */
    public String getProxyAuthString() {
        return config.getProperty(getClass().getName(), "proxyauth");
    }

    /** returns the current connections.
     * @return a List with the current connections. 
     */
    public List getCurrentConections() {
        return convec;
    }

    /** Remove a connection (it has received full treatment)
     *  @param con the connection that is closing down.
     */
    public void removeConnection(Connection con) {
        convec.remove(con);
        tp.returnThread(con);
    }

    /** return the time when the proxy was started.
     * @return a Date with the time the proxy was started.
     */
    public Date getStartDate() {
        return started;
    }

    public String getServerIdentity() {
        return serverIdentity;
    }

    public Map<String, HandlerFactory> getHandlers() {
        return Collections.unmodifiableMap(handlers);
    }

    public Map<String, HandlerFactory> getCacheHandlers() {
        return Collections.unmodifiableMap(cachehandlers);
    }

    public List<IPAccessFilter> getAccessFilters() {
        return Collections.unmodifiableList(accessfilters);
    }

    public List<HTTPFilter> getHTTPInFilters() {
        return Collections.unmodifiableList(httpinfilters);
    }

    public List<HTTPFilter> getHTTPOutFilters() {
        return Collections.unmodifiableList(httpoutfilters);
    }

    TunnelHandler getTunnelHandler() {
        return tunnelHandler;
    }

    public NLSOHandler getNLSOHandler() {
        return nlsoHandler;
    }

    /** load a set of handlers.
     * @param section the section in the config file.
     * @return a Map with mimetypes as keys and Handlers as values.
     */
    protected Map<String, HandlerFactory> loadHandlers(String section) {
        SProperties handlers = config.getProperties(section);
        String classname = "";
        Map<String, HandlerFactory> hhandlers = new HashMap<String, HandlerFactory>();
        for (String handler : handlers.keySet()) {
            try {
                classname = handlers.getProperty(handler).trim();
                Class<? extends HandlerFactory> cls = Class.forName(classname).asSubclass(HandlerFactory.class);
                HandlerFactory hf = cls.newInstance();
                hf.setup(this, config.getProperties(classname));
                hhandlers.put(handler, hf);
            } catch (ClassNotFoundException ex) {
                logError(ERROR, "Could not load class: '" + classname + "' for handlerfactory '" + handler + "'");
            } catch (InstantiationException ie) {
                logError(ERROR, "Could not instanciate handlerfactory class: '" + classname + "' for handler '" + handler + "' :" + ie);
            } catch (IllegalAccessException iae) {
                logError(ERROR, "Could not instanciate handlerfactory class: '" + classname + "' for handler '" + handler + "' :" + iae);
            }
        }
        return hhandlers;
    }

    /** Make sure all filters and handlers are available
     */
    protected void loadClasses() {
        handlers = loadHandlers("Handlers");
        cachehandlers = loadHandlers("CacheHandlers");
        String filters = config.getProperty("Filters", "accessfilters", "");
        accessfilters = new ArrayList<IPAccessFilter>();
        loadAccessFilters(filters, accessfilters);
        filters = config.getProperty("Filters", "httpinfilters", "");
        httpinfilters = new ArrayList<HTTPFilter>();
        loadHTTPFilters(filters, httpinfilters);
        filters = config.getProperty("Filters", "httpoutfilters", "");
        httpoutfilters = new ArrayList<HTTPFilter>();
        loadHTTPFilters(filters, httpoutfilters);
    }

    private void loadAccessFilters(String filters, List<IPAccessFilter> accessfilters) {
        StringTokenizer st = new StringTokenizer(filters, ",");
        String classname = "";
        while (st.hasMoreElements()) {
            try {
                classname = st.nextToken().trim();
                Class<? extends IPAccessFilter> cls = Class.forName(classname).asSubclass(IPAccessFilter.class);
                IPAccessFilter ipf = cls.newInstance();
                ipf.setup(this, config.getProperties(classname));
                accessfilters.add(ipf);
            } catch (ClassNotFoundException ex) {
                logError(ERROR, "Could not load class: '" + classname + "' " + ex);
            } catch (InstantiationException ex) {
                logError(ERROR, "Could not instansiate: '" + classname + "' " + ex);
            } catch (IllegalAccessException ex) {
                logError(ERROR, "Could not instansiate: '" + classname + "' " + ex);
            }
        }
    }

    private void loadHTTPFilters(String filters, List<HTTPFilter> httpFilters) {
        StringTokenizer st = new StringTokenizer(filters, ",");
        String classname = "";
        while (st.hasMoreElements()) {
            try {
                classname = st.nextToken().trim();
                Class<? extends HTTPFilter> cls = Class.forName(classname).asSubclass(HTTPFilter.class);
                HTTPFilter hf = cls.newInstance();
                hf.setup(this, config.getProperties(classname));
                httpFilters.add(hf);
            } catch (ClassNotFoundException ex) {
                logError(ERROR, "Could not load class: '" + classname + "' " + ex);
            } catch (InstantiationException ex) {
                logError(ERROR, "Could not instansiate: '" + classname + "' " + ex);
            } catch (IllegalAccessException ex) {
                logError(ERROR, "Could not instansiate: '" + classname + "' " + ex);
            }
        }
    }

    /** log a connection handled.
     * @param con the Connection that handled a request
     */
    public void logConnection(Connection con) {
        log.inc("Total pages served");
        Date d = new Date();
        d.setTime(d.getTime() - offset);
        StringBuilder sb = new StringBuilder();
        Socket s = con.getSocket();
        if (s != null) {
            InetAddress ia = s.getInetAddress();
            if (ia != null) sb.append(ia.getHostAddress()); else sb.append("????");
        }
        sb.append(" - ");
        sb.append((con.getUserName() != null ? con.getUserName() : "-"));
        sb.append(" ");
        synchronized (sdfMonitor) {
            sb.append(sdf.format(d));
        }
        sb.append(" \"");
        sb.append(con.getRequestLine());
        sb.append("\" ");
        sb.append(con.getStatusCode());
        sb.append(" ");
        sb.append(con.getContentLength());
        sb.append(" ");
        sb.append((con.getExtraInfo() != null ? con.getExtraInfo() : ""));
        synchronized (accessMonitor) {
            accesslog.println(sb.toString());
        }
    }

    /** shutdown
     */
    public void kill() {
        closeSocket();
        synchronized (accessMonitor) {
            accesslog.flush();
            accesslog.close();
            accesslog = new LogWriter(System.out, true);
        }
        logError(MSG, "Kill issued, shuting down");
        synchronized (errorMonitor) {
            errorlog.flush();
            errorlog.close();
            errorlog = new LogWriter(System.err, true);
        }
        cache.flush();
        System.exit(0);
    }
}
