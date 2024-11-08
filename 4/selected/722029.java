package net.borderwars.userserver;

import net.borderwars.util.HibernateUtil;
import net.borderwars.util.LogFormat;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Eric
 *         Date: Jul 26, 2005
 *         Time: 5:56:28 PM
 */
public class Server {

    static Logger log = Logger.getLogger("Server");

    private static PriorityBlockingQueue<Runnable> objectQueue = new PriorityBlockingQueue<Runnable>();

    private static int cpuCount = Runtime.getRuntime().availableProcessors();

    private static Executor threadPool = new ThreadPoolExecutor(cpuCount, cpuCount * 2, 1, TimeUnit.SECONDS, objectQueue);

    private static Selector selector;

    static HashSet<Class> remoteInterfaces = new HashSet<Class>();

    public static String getCompiler() {
        return knownCompiler;
    }

    private static String knownCompiler;

    static {
        remoteInterfaces.add(ServerInterface.class);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        LogFormat.setupLogging();
        log.info("Starting JAR watcher (now looks at manifest)");
        startJarWatcher();
        Thread.sleep(1000);
        log.info("Creating database");
        try {
            HibernateUtil.setupDumbDatabase();
        } catch (Exception e) {
            System.err.println("cl " + Server.class.getClassLoader());
            e.printStackTrace();
            System.exit(1);
        }
        log.info("Looking for compiler");
        knownCompiler = findCompiler();
        if (knownCompiler == null) {
            log.warning("Could not find a java compiler!");
            System.exit(1);
        }
        log.info("Found compiler at " + knownCompiler);
        log.info("Waiting for Clients to connect and console input");
        Console c = new Console();
        start();
    }

    private static void start() throws IOException, ClassNotFoundException {
        selector = SelectorProvider.provider().openSelector();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        InetAddress lh = InetAddress.getLocalHost();
        InetSocketAddress isa = new InetSocketAddress(lh, 8989);
        ssc.socket().bind(isa);
        ssc.register(selector, OP_ACCEPT);
        int keysAdded;
        while ((keysAdded = selector.select()) > 0) {
            Set readyKeys = selector.selectedKeys();
            Iterator i = readyKeys.iterator();
            while (i.hasNext()) {
                SelectionKey sk = (SelectionKey) i.next();
                i.remove();
                if (sk.isAcceptable()) {
                    ServerSocketChannel nextReady = (ServerSocketChannel) sk.channel();
                    Socket s = nextReady.accept().socket();
                    s.getChannel().configureBlocking(false);
                    s.getChannel().register(selector, SelectionKey.OP_READ);
                } else if (sk.isReadable()) {
                    SocketChannel sc = (SocketChannel) sk.channel();
                    if (!sc.isOpen()) {
                        sc.register(selector, 0);
                    }
                    Attachment at = (Attachment) sk.attachment();
                    if (at == null) {
                        log.info("New Client Connected");
                        URLClassLoader cl = (URLClassLoader) Thread.currentThread().getContextClassLoader();
                        System.out.println("We use this loader " + cl);
                        at = new Attachment(sc);
                        sk.attach(at);
                    }
                    List<Object> objs = at.read(sk, sc);
                    putObjectsOnProcessQueue(objs, at);
                }
            }
        }
        throw new UnsupportedOperationException("Somehow our acceptor escaped!");
    }

    private static void putObjectsOnProcessQueue(List<Object> objs, Attachment at) {
        for (Object o : objs) {
            if (o instanceof Invocation) {
                Invocation i = (Invocation) o;
                InvocationProcessor ip = new InvocationProcessor(i, at);
                threadPool.execute(ip);
            } else if (o instanceof InvocationResult) {
                InvocationResult ir = (InvocationResult) o;
                InvocationResultProcessor irp = new InvocationResultProcessor(ir);
                threadPool.execute(irp);
            } else {
                log.severe("Unexpected object type from client " + o.getClass().getName());
            }
        }
    }

    static ByteBuffer makeByteBuffer(final Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        byte data[] = baos.toByteArray();
        baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(data.length);
        dos.write(data);
        dos.flush();
        data = baos.toByteArray();
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length);
        bb.put(data);
        bb.position(0);
        return bb;
    }

    static String checkObviousLocations() {
        String path = System.getenv("path");
        if (path == null) {
            path = System.getenv("PATH");
        }
        if (path == null) {
            return (path);
        }
        String dirs[] = path.split(File.pathSeparator);
        for (String dir : dirs) {
            String checkThis = dir + File.separator + "javac";
            if (checkCompiler(checkThis)) {
                return (checkThis);
            }
        }
        return (null);
    }

    static String findCompiler() throws IOException, InterruptedException {
        String compiler = null;
        Properties props = new Properties();
        File propFile = new File("server.properties");
        if (propFile.exists()) {
            props.load(new FileInputStream(propFile));
            compiler = props.getProperty("compiler");
            if (compiler != null) {
                if (checkCompiler(compiler)) {
                    return (compiler);
                }
            }
        }
        compiler = checkObviousLocations();
        if (compiler == null) {
            log.info("Checked the path env variable -- could not find a java 1.5 compiler now searching whole drive");
            compiler = null;
            Queue<File> q = new PriorityQueue<File>();
            File roots[] = File.listRoots();
            for (File file : roots) {
                q.add(file);
            }
            while (compiler == null && !q.isEmpty()) {
                File f = q.poll();
                if (f.isDirectory()) {
                    File inDir[] = f.listFiles();
                    if (inDir != null) {
                        for (File file : inDir) {
                            q.add(file);
                        }
                    }
                } else {
                    if (f.getName().equals("javac") || f.getName().equals("javac.exe")) {
                        log.info("Found: " + f.getCanonicalPath());
                        String candidate = f.getCanonicalPath();
                        if (checkCompiler(candidate)) {
                            compiler = f.getCanonicalPath();
                        }
                    }
                }
            }
        }
        props.put("compiler", compiler);
        props.store(new FileOutputStream(propFile), "This file contains the location of a java 1.5 compiler");
        return (compiler);
    }

    private static boolean checkCompiler(String cmd) {
        try {
            cmd += " -version";
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line = br.readLine();
            if (line != null && line.indexOf("javac 1.5.0") != -1) {
                return (true);
            }
            p.destroy();
        } catch (IOException e) {
            return (false);
        }
        return (false);
    }

    private static void startJarWatcher() {
        log.entering("net.borderwars.userserver.Server", "startJarWatcher");
        JarWatcher jw = new JarWatcher();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(jw, 1000, 1000);
        log.exiting("net.borderwars.userserver.Server", "startJarWatcher");
    }

    private static class JarWatcher extends TimerTask {

        long initialTimeStamp = 0;

        long inititialCRC = 0;

        Map<String, String> initialMap = null;

        public void run() {
            Thread.currentThread().setContextClassLoader(Server.class.getClassLoader());
            File f = new File("server.jar");
            try {
                Map<String, String> currentMap = parseManifest(f);
                if (initialMap == null) {
                    initialMap = currentMap;
                } else if (!same(currentMap, initialMap)) {
                    log.info("Jar file changed -- restarting");
                    shutdown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private boolean same(final Map<String, String> currentMap, final Map<String, String> initialMap) {
            if (initialMap.size() != currentMap.size()) {
                return (false);
            }
            for (Map.Entry<String, String> entry : currentMap.entrySet()) {
                if (!initialMap.containsKey(entry.getKey())) {
                    return (false);
                } else {
                    if (!initialMap.get(entry.getKey()).equals(entry.getValue())) {
                        return (false);
                    }
                }
            }
            return (true);
        }

        private Map<String, String> parseManifest(File f) throws IOException {
            Map<String, String> rtn = new HashMap<String, String>();
            ZipFile zf = new ZipFile(f);
            ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");
            InputStream is = zf.getInputStream(ze);
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = br.readLine();
            String name = null;
            String changed = null;
            while (line != null) {
                if (line.startsWith("Name:")) {
                    name = line.substring(line.indexOf(":") + 1).trim();
                } else if (line.startsWith("Last-Modified:")) {
                    changed = line.substring(line.indexOf(":") + 1).trim();
                    rtn.put(name, changed);
                }
                line = br.readLine();
            }
            return (rtn);
        }

        private long getCRC(final File f) throws IOException {
            CRC32 c = new CRC32();
            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);
            byte data[] = new byte[1024];
            while (bis.read(data) != -1) c.update(data);
            bis.close();
            return (c.getValue());
        }
    }

    static void shutdown() throws SQLException {
        shutdown(0);
    }

    static void shutdown(int code) throws SQLException {
        HibernateUtil.shutdown();
        System.exit(code);
    }
}
