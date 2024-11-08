package chrriis.dj.nativeswing.swtimpl;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.swing.event.EventListenerList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Display;
import chrriis.common.NetworkURLClassLoader;
import chrriis.common.Utils;
import chrriis.common.WebServer;
import chrriis.dj.nativeswing.NativeSwing;
import chrriis.dj.nativeswing.swtimpl.InProcessMessagingInterface.SWTInProcessMessagingInterface;
import chrriis.dj.nativeswing.swtimpl.InProcessMessagingInterface.SwingInProcessMessagingInterface;
import chrriis.dj.nativeswing.swtimpl.OutProcessMessagingInterface.SWTOutProcessMessagingInterface;
import chrriis.dj.nativeswing.swtimpl.OutProcessMessagingInterface.SwingOutProcessMessagingInterface;

/**
 * The native interface, which establishes the link between the peer VM (native side) and the local side.
 * @author Christopher Deckers
 */
public class NativeInterface {

    private static final boolean IS_SYNCING_MESSAGES = Boolean.parseBoolean(System.getProperty("nativeswing.interface.syncmessages"));

    static boolean isAlive() {
        return isOpen() && messagingInterface.isAlive();
    }

    private static boolean isOpen;

    private static boolean isOpen() {
        return isOpen;
    }

    private static void checkOpen() {
        if (!isOpen()) {
            throw new IllegalStateException("The native interface is not open! Please refer to the instructions to set it up properly.");
        }
    }

    /**
   * Close the native interface, which destroys the native side (peer VM). Note that the native interface can be re-opened later.
   */
    public static void close() {
        if (!isOpen) {
            return;
        }
        isOpen = false;
        messagingInterface.destroy();
        messagingInterface = null;
        for (NativeInterfaceListener listener : getNativeInterfaceListeners()) {
            listener.nativeInterfaceClosed();
        }
    }

    private static NativeInterfaceConfiguration nativeInterfaceConfiguration;

    /**
   * Get the configuration, which allows to modify some parameters.
   */
    public static NativeInterfaceConfiguration getConfiguration() {
        if (nativeInterfaceConfiguration == null) {
            nativeInterfaceConfiguration = new NativeInterfaceConfiguration();
        }
        return nativeInterfaceConfiguration;
    }

    private static void loadClipboardDebuggingProperties() {
        try {
            Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (!systemClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return;
            }
            BufferedReader reader = new BufferedReader(new StringReader((String) systemClipboard.getData(DataFlavor.stringFlavor)));
            if ("[nativeswing debug]".equals(reader.readLine().trim().toLowerCase(Locale.ENGLISH))) {
                for (String line; ((line = reader.readLine()) != null); ) {
                    if (line.length() != 0) {
                        int index = line.indexOf('=');
                        if (index <= 0) {
                            break;
                        }
                        String propertyName = line.substring(0, index).trim();
                        String propertyValue = line.substring(index + 1).trim();
                        if (propertyName.startsWith("nativeswing.")) {
                            System.setProperty(propertyName, propertyValue);
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
        }
    }

    private static boolean isInitialized;

    private static boolean isInitialized() {
        return isInitialized;
    }

    private static boolean isInProcess;

    static boolean isInProcess() {
        return isInProcess;
    }

    /**
   * Initialize the native interface, but do not open it. This method sets some properties and registers a few listeners to keep track of certain states necessary for the good functioning of the framework.<br/>
   * This method is automatically called if open() is used. It should be called early in the program, the best place being as the first call in the main method.
   */
    public static void initialize() {
        if (isInitialized()) {
            return;
        }
        if (SWT.getVersion() < 3536) {
            throw new IllegalStateException("The version of SWT that is required is 3.5M6 or later!");
        }
        if (nativeInterfaceConfiguration == null) {
            nativeInterfaceConfiguration = new NativeInterfaceConfiguration();
        }
        NativeSwing.initialize();
        String inProcessProperty = System.getProperty("nativeswing.interface.inprocess");
        if (inProcessProperty != null) {
            isInProcess = Boolean.parseBoolean(inProcessProperty);
        } else {
            isInProcess = Utils.IS_MAC;
        }
        try {
            for (NativeInterfaceListener listener : getNativeInterfaceListeners()) {
                listener.nativeInterfaceInitialized();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (isInProcess) {
            InProcess.initialize();
        }
        isInitialized = true;
    }

    /**
   * Open the native interface, which creates the peer VM that handles the native side of the native integration.<br/>
   * Initialization takes place if the interface was not already initialized. If initialization was not explicitely performed, this method should be called early in the program, the best place being as the first call in the main method.
   */
    public static void open() {
        if (isOpen()) {
            return;
        }
        initialize();
        loadClipboardDebuggingProperties();
        if (isInProcess) {
            InProcess.createInProcessCommunicationChannel();
        } else {
            OutProcess.createOutProcessCommunicationChannel();
        }
        try {
            for (NativeInterfaceListener listener : getNativeInterfaceListeners()) {
                listener.nativeInterfaceOpened();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean notifyKilled() {
        isOpen = false;
        messagingInterface = null;
        try {
            for (NativeInterfaceListener listener : getNativeInterfaceListeners()) {
                listener.nativeInterfaceClosed();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!NativeInterface.OutProcess.isNativeSide() && nativeInterfaceConfiguration.isNativeSideRespawnedOnError()) {
            OutProcess.createOutProcessCommunicationChannel();
            return true;
        }
        return false;
    }

    static void notifyRespawned() {
        try {
            for (NativeInterfaceListener listener : getNativeInterfaceListeners()) {
                listener.nativeInterfaceOpened();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private NativeInterface() {
    }

    static Object syncSend(boolean isTargetNativeSide, final Message message) {
        checkOpen();
        if (message instanceof LocalMessage) {
            LocalMessage localMessage = (LocalMessage) message;
            return localMessage.runCommand();
        }
        return getMessagingInterface(!isTargetNativeSide).syncSend(message);
    }

    static void asyncSend(boolean isTargetNativeSide, final Message message) {
        if (IS_SYNCING_MESSAGES) {
            syncSend(isTargetNativeSide, message);
        } else {
            checkOpen();
            if (message instanceof LocalMessage) {
                LocalMessage localMessage = (LocalMessage) message;
                localMessage.runCommand();
                return;
            }
            getMessagingInterface(!isTargetNativeSide).asyncSend(message);
        }
    }

    private static volatile MessagingInterface messagingInterface;

    static MessagingInterface getMessagingInterface(boolean isNativeSide) {
        if (isInProcess()) {
            if (isNativeSide) {
                SWTInProcessMessagingInterface swtInProcessMessagingInterface = (SWTInProcessMessagingInterface) ((SwingInProcessMessagingInterface) messagingInterface).getMirrorMessagingInterface();
                return swtInProcessMessagingInterface;
            }
            SwingInProcessMessagingInterface swingInProcessMessagingInterface = (SwingInProcessMessagingInterface) messagingInterface;
            return swingInProcessMessagingInterface;
        }
        if (isNativeSide) {
            SWTOutProcessMessagingInterface swtOutProcessMessagingInterface = (SWTOutProcessMessagingInterface) messagingInterface;
            return swtOutProcessMessagingInterface;
        }
        SwingOutProcessMessagingInterface swingOutProcessMessagingInterface = (SwingOutProcessMessagingInterface) messagingInterface;
        return swingOutProcessMessagingInterface;
    }

    private static Display display;

    /**
   * Get the SWT display. This is only possible when in the native context.
   * @return the display, or null.
   */
    public static Display getDisplay() {
        return display;
    }

    /**
   * Indicate if the current thread is the user interface thread.
   * @return true if the current thread is the user interface thread.
   * @throws IllegalStateException when the native interface is not alive.
   */
    public static boolean isUIThread(boolean isNativeSide) {
        if (!isAlive()) {
            throw new IllegalStateException("The native interface is not alive!");
        }
        return getMessagingInterface(isNativeSide).isUIThread();
    }

    static void checkUIThread(boolean isNativeSide) {
        if (!isAlive()) {
            throw new IllegalStateException("The native interface is not alive!");
        }
        getMessagingInterface(isNativeSide).checkUIThread();
    }

    /**
   * Run the native event pump. Certain platforms require this method call at the end of the main method to function properly, so it is suggested to always add it.
   */
    public static void runEventPump() {
        if (isInProcess) {
            InProcess.runEventPump();
        }
    }

    private static EventListenerList listenerList = new EventListenerList();

    /**
   * Add a native interface listener.
   * @param listener the native listener to add.
   */
    public static void addNativeInterfaceListener(NativeInterfaceListener listener) {
        listenerList.add(NativeInterfaceListener.class, listener);
    }

    /**
   * Remove a native interface listener.
   * @param listener the native listener to remove.
   */
    public static void removeNativeInterfaceListener(NativeInterfaceListener listener) {
        listenerList.remove(NativeInterfaceListener.class, listener);
    }

    /**
   * Get all the native interface listeners.
   * @return the native interface listeners.
   */
    public static NativeInterfaceListener[] getNativeInterfaceListeners() {
        return listenerList.getListeners(NativeInterfaceListener.class);
    }

    static class InProcess {

        private static volatile boolean isEventPumpRunning;

        static void runEventPump() {
            if (!isInProcess) {
                return;
            }
            while (isEventPumpRunning) {
                try {
                    if (!display.readAndDispatch()) {
                        if (isEventPumpRunning) {
                            display.sleep();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        static void createInProcessCommunicationChannel() {
            messagingInterface = createInProcessMessagingInterface();
            isOpen = true;
            isEventPumpRunning = true;
        }

        private static void initialize() {
            Device.DEBUG = Boolean.parseBoolean(System.getProperty("nativeswing.swt.debug.device"));
            DeviceData data = new DeviceData();
            data.debug = Boolean.parseBoolean(System.getProperty("nativeswing.swt.devicedata.debug"));
            data.tracking = Boolean.parseBoolean(System.getProperty("nativeswing.swt.devicedata.tracking"));
            display = new Display(data);
            startAutoShutdownThread();
        }

        private static MessagingInterface createInProcessMessagingInterface() {
            return new SWTInProcessMessagingInterface(display).getMirrorMessagingInterface();
        }

        static void startAutoShutdownThread() {
            Thread autoShutdownThread = new Thread("NativeSwing Auto-Shutdown") {

                protected Thread[] activeThreads = new Thread[1024];

                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {
                        }
                        ThreadGroup group = Thread.currentThread().getThreadGroup();
                        for (ThreadGroup parentGroup = group; (parentGroup = parentGroup.getParent()) != null; group = parentGroup) {
                        }
                        boolean isAlive = display == null;
                        if (!isAlive) {
                            for (int i = group.enumerate(activeThreads, true) - 1; i >= 0; i--) {
                                Thread t = activeThreads[i];
                                if (t != display.getThread() && !t.isDaemon() && t.isAlive()) {
                                    isAlive = true;
                                    break;
                                }
                            }
                        }
                        if (!isAlive) {
                            isEventPumpRunning = false;
                            display.wake();
                        }
                    }
                }
            };
            autoShutdownThread.setDaemon(true);
            autoShutdownThread.start();
        }
    }

    static class OutProcess {

        private static class CMN_setProperties extends CommandMessage {

            @Override
            public Object run(Object[] args) {
                Properties systemProperties = System.getProperties();
                Properties properties = (Properties) args[0];
                for (Object o : properties.keySet()) {
                    if (!systemProperties.containsKey(o)) {
                        try {
                            System.setProperty((String) o, properties.getProperty((String) o));
                        } catch (Exception e) {
                        }
                    }
                }
                return null;
            }
        }

        static boolean isNativeSide() {
            return display != null;
        }

        static void createOutProcessCommunicationChannel() {
            messagingInterface = createOutProcessMessagingInterface();
            isOpen = true;
            Properties nativeProperties = new Properties();
            Properties properties = System.getProperties();
            for (Object key : properties.keySet()) {
                if (key instanceof String) {
                    Object value = properties.get(key);
                    if (value instanceof String) {
                        nativeProperties.setProperty((String) key, (String) value);
                    }
                }
            }
            new CMN_setProperties().syncExec(true, nativeProperties);
        }

        private static Process createProcess(String localHostAddress, int port) {
            List<String> classPathList = new ArrayList<String>();
            String pathSeparator = System.getProperty("path.separator");
            List<Object> referenceList = new ArrayList<Object>();
            List<String> optionalReferenceList = new ArrayList<String>();
            referenceList.add(NativeSwing.class);
            referenceList.add(NativeInterface.class);
            referenceList.add("org/eclipse/swt/widgets/Display.class");
            optionalReferenceList.add("org/mozilla/xpcom/Mozilla.class");
            optionalReferenceList.add("org/mozilla/interfaces/nsIWebBrowser.class");
            for (String optionalReference : optionalReferenceList) {
                if (NativeInterface.class.getClassLoader().getResource(optionalReference) != null) {
                    referenceList.add(optionalReference);
                }
            }
            Class<?>[] nativeClassPathReferenceClasses = nativeInterfaceConfiguration.getNativeClassPathReferenceClasses();
            if (nativeClassPathReferenceClasses != null) {
                referenceList.addAll(Arrays.asList(nativeClassPathReferenceClasses));
            }
            String[] nativeClassPathReferenceResources = nativeInterfaceConfiguration.getNativeClassPathReferenceResources();
            if (nativeClassPathReferenceResources != null) {
                referenceList.addAll(Arrays.asList(nativeClassPathReferenceResources));
            }
            boolean isProxyClassLoaderUsed = Boolean.parseBoolean(System.getProperty("nativeswing.peervm.forceproxyclassloader"));
            if (!isProxyClassLoaderUsed) {
                for (Object o : referenceList) {
                    File clazzClassPath;
                    if (o instanceof Class) {
                        clazzClassPath = Utils.getClassPathFile((Class<?>) o);
                    } else {
                        clazzClassPath = Utils.getClassPathFile((String) o);
                        if (NativeInterface.class.getClassLoader().getResource((String) o) == null) {
                            throw new IllegalStateException("A resource that is needed in the classpath is missing: " + o);
                        }
                    }
                    clazzClassPath = o instanceof Class ? Utils.getClassPathFile((Class<?>) o) : Utils.getClassPathFile((String) o);
                    if (clazzClassPath != null) {
                        String path = clazzClassPath.getAbsolutePath();
                        if (!classPathList.contains(path)) {
                            classPathList.add(path);
                        }
                    } else {
                        isProxyClassLoaderUsed = true;
                    }
                }
            }
            if (isProxyClassLoaderUsed) {
                classPathList.clear();
                File classPathFile = new File(System.getProperty("java.io.tmpdir"), ".djnativeswing/classpath");
                Utils.deleteAll(classPathFile);
                String classPath = NetworkURLClassLoader.class.getName().replace('.', '/') + ".class";
                File mainClassFile = new File(classPathFile, classPath);
                mainClassFile.getParentFile().mkdirs();
                if (!mainClassFile.exists()) {
                    try {
                        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(mainClassFile));
                        BufferedInputStream in = new BufferedInputStream(NativeInterface.class.getResourceAsStream("/" + classPath));
                        byte[] bytes = new byte[1024];
                        for (int n; (n = in.read(bytes)) != -1; out.write(bytes, 0, n)) {
                        }
                        in.close();
                        out.close();
                    } catch (Exception e) {
                    }
                    mainClassFile.deleteOnExit();
                }
                classPathList.add(classPathFile.getAbsolutePath());
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < classPathList.size(); i++) {
                if (i > 0) {
                    sb.append(pathSeparator);
                }
                sb.append(classPathList.get(i));
            }
            String javaHome = System.getProperty("java.home");
            String[] candidateBinaries = new String[] { new File(javaHome, "bin/java").getAbsolutePath(), new File("/usr/lib/java").getAbsolutePath(), "java" };
            Process p = null;
            List<String> argList = new ArrayList<String>();
            argList.add(null);
            String[] peerVMParams = nativeInterfaceConfiguration.getPeerVMParams();
            if (peerVMParams != null) {
                for (String param : peerVMParams) {
                    argList.add(param);
                }
            }
            String[] flags = new String[] { "nativeswing.interface.syncmessages", "nativeswing.interface.debug.printmessages", "nativeswing.peervm.debug.printstartmessage", "nativeswing.swt.debug.device", "nativeswing.swt.devicedata.debug", "nativeswing.swt.devicedata.tracking" };
            for (String flag : flags) {
                if (Boolean.parseBoolean(System.getProperty(flag))) {
                    argList.add("-D" + flag + "=true");
                }
            }
            argList.add("-Dnativeswing.localhostaddress=" + localHostAddress);
            argList.add("-classpath");
            argList.add(sb.toString());
            if (isProxyClassLoaderUsed) {
                argList.add(NetworkURLClassLoader.class.getName());
                argList.add(WebServer.getDefaultWebServer().getClassPathResourceURL("", ""));
            }
            argList.add(NativeInterface.class.getName());
            argList.add(String.valueOf(port));
            String javaVersion = System.getProperty("java.version");
            if (javaVersion != null && javaVersion.compareTo("1.6.0_10") >= 0 && "Sun Microsystems Inc.".equals(System.getProperty("java.vendor"))) {
                boolean isTryingAppletCompatibility = true;
                if (peerVMParams != null) {
                    for (String peerVMParam : peerVMParams) {
                        if (peerVMParam.startsWith("-Xbootclasspath/a:")) {
                            isTryingAppletCompatibility = false;
                            break;
                        }
                    }
                }
                if (isTryingAppletCompatibility) {
                    File[] deploymentFiles = new File[] { new File(javaHome, "lib/deploy.jar"), new File(javaHome, "lib/plugin.jar"), new File(javaHome, "lib/javaws.jar") };
                    List<String> argListX = new ArrayList<String>();
                    argListX.add(candidateBinaries[0]);
                    StringBuilder sbX = new StringBuilder();
                    for (int i = 0; i < deploymentFiles.length; i++) {
                        if (i != 0) {
                            sbX.append(pathSeparator);
                        }
                        File deploymentFile = deploymentFiles[i];
                        if (deploymentFile.exists()) {
                            sbX.append(deploymentFile.getAbsolutePath());
                        }
                    }
                    if (sbX.indexOf(" ") != -1) {
                        argListX.add("\"-Xbootclasspath/a:" + sbX + "\"");
                    } else {
                        argListX.add("-Xbootclasspath/a:" + sbX);
                    }
                    argListX.addAll(argList.subList(1, argList.size()));
                    if (Boolean.parseBoolean(System.getProperty("nativeswing.peervm.debug.printcommandline"))) {
                        System.err.println("Native Command: " + Arrays.toString(argListX.toArray()));
                    }
                    try {
                        p = new ProcessBuilder(argListX).start();
                    } catch (IOException e) {
                    }
                }
            }
            if (p == null) {
                for (String candidateBinary : candidateBinaries) {
                    argList.set(0, candidateBinary);
                    if (Boolean.parseBoolean(System.getProperty("nativeswing.peervm.debug.printcommandline"))) {
                        System.err.println("Native Command: " + Arrays.toString(argList.toArray()));
                    }
                    try {
                        p = new ProcessBuilder(argList).start();
                        break;
                    } catch (IOException e) {
                    }
                }
            }
            if (p == null) {
                throw new IllegalStateException("Failed to spawn the VM!");
            }
            return p;
        }

        private static MessagingInterface createOutProcessMessagingInterface() {
            String localHostAddress = Utils.getLocalHostAddress();
            if (localHostAddress == null) {
                throw new IllegalStateException("Failed to find a suitable local host address to communicate with a spawned VM!");
            }
            int port = Integer.parseInt(System.getProperty("nativeswing.interface.port", "-1"));
            if (port <= 0) {
                ServerSocket serverSocket;
                try {
                    serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(false);
                    serverSocket.bind(new InetSocketAddress(InetAddress.getByName(localHostAddress), 0));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                port = serverSocket.getLocalPort();
                try {
                    serverSocket.close();
                } catch (IOException e) {
                }
            }
            Process p;
            if (Boolean.parseBoolean(System.getProperty("nativeswing.peervm.create", "true"))) {
                p = createProcess(localHostAddress, port);
                connectStream(System.err, p.getErrorStream());
                connectStream(System.out, p.getInputStream());
            } else {
                p = null;
            }
            Socket socket = null;
            for (int i = 99; i >= 0; i--) {
                try {
                    socket = new Socket(localHostAddress, port);
                    break;
                } catch (IOException e) {
                    if (i == 0) {
                        throw new RuntimeException(e);
                    }
                }
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                }
            }
            if (socket == null) {
                if (p != null) {
                    p.destroy();
                }
                throw new IllegalStateException("Failed to connect to spawned VM!");
            }
            return new SwingOutProcessMessagingInterface(socket, false);
        }

        private static void connectStream(final PrintStream out, InputStream in) {
            final BufferedInputStream bin = new BufferedInputStream(in);
            Thread streamThread = new Thread("NativeSwing Stream Connector") {

                @Override
                public void run() {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        String lineSeparator = Utils.LINE_SEPARATOR;
                        byte lastByte = (byte) lineSeparator.charAt(lineSeparator.length() - 1);
                        boolean addMessage = true;
                        byte[] bytes = new byte[1024];
                        for (int i; (i = bin.read(bytes)) != -1; ) {
                            baos.reset();
                            for (int j = 0; j < i; j++) {
                                byte b = bytes[j];
                                if (addMessage) {
                                    baos.write("NativeSwing: ".getBytes());
                                }
                                addMessage = b == lastByte;
                                baos.write(b);
                            }
                            try {
                                out.write(baos.toByteArray());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            };
            streamThread.setDaemon(true);
            streamThread.start();
        }

        static void runNativeSide(String[] args) throws IOException {
            if (Boolean.parseBoolean(System.getProperty("nativeswing.peervm.debug.printstartmessage"))) {
                System.err.println("Starting spawned VM");
            }
            isOpen = true;
            int port = Integer.parseInt(args[0]);
            ServerSocket serverSocket = null;
            for (int i = 19; i >= 0; i--) {
                try {
                    serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(Utils.getLocalHostAddress(), port));
                    break;
                } catch (IOException e) {
                    if (i == 0) {
                        throw e;
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
            final ServerSocket serverSocket_ = serverSocket;
            if (!Boolean.parseBoolean(System.getProperty("nativeswing.peervm.keepalive"))) {
                Thread shutdownThread = new Thread("NativeSwing Shutdown") {

                    @Override
                    public void run() {
                        try {
                            sleep(10000);
                        } catch (Exception e) {
                        }
                        if (messagingInterface == null) {
                            try {
                                serverSocket_.close();
                            } catch (Exception e) {
                            }
                        }
                    }
                };
                shutdownThread.setDaemon(true);
                shutdownThread.start();
            }
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (Exception e) {
                throw new IllegalStateException("The native side did not receive an incoming connection!");
            }
            Device.DEBUG = Boolean.parseBoolean(System.getProperty("nativeswing.swt.debug.device"));
            DeviceData data = new DeviceData();
            data.debug = Boolean.parseBoolean(System.getProperty("nativeswing.swt.devicedata.debug"));
            data.tracking = Boolean.parseBoolean(System.getProperty("nativeswing.swt.devicedata.tracking"));
            display = new Display(data);
            Display.setAppName("DJ Native Swing");
            messagingInterface = new SWTOutProcessMessagingInterface(socket, true, display);
            while (display != null && !display.isDisposed()) {
                try {
                    if (!display.readAndDispatch()) {
                        display.sleep();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
   * The main method that is called by the native side (peer VM).
   * @param args the arguments that are passed to the peer VM.
   */
    public static void main(String[] args) throws Exception {
        OutProcess.runNativeSide(args);
    }
}
