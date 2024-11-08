package jimo.osgi.modules.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.ResourceBundle;
import jimo.osgi.api.Application;
import jimo.osgi.api.BundleService;
import jimo.osgi.api.CommandContext;
import jimo.osgi.api.CommandHandler;
import jimo.osgi.api.Daemon;
import jimo.osgi.api.FrameworkException;
import jimo.osgi.api.IdleEvent;
import jimo.osgi.api.IdleEventListener;
import jimo.osgi.api.JIMOConstants;
import jimo.osgi.api.Persistence;
import jimo.osgi.api.util.Config;
import jimo.osgi.api.util.LogUtil;
import jimo.osgi.impl.BundleRegistryException;
import jimo.osgi.impl.framework.BundleClassLoaderImpl;
import jimo.osgi.impl.framework.BundleImpl;
import jimo.osgi.impl.framework.BundleRegistryImpl;
import jimo.osgi.impl.framework.FrameworkImpl;
import jimo.osgi.impl.framework.FrameworkImpl.Event;
import jimo.osgi.impl.framework.FrameworkImpl.Listener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Core implements BundleActivator {

    public static final String COMMANDPROCESSOR = "jimo/command/systemIn";

    public static final String SHELLPROCESSOR = "jimo/command/shell/";

    private static LogUtil log;

    private static Config config;

    private static ServiceTracker logTracker;

    private static ServiceTracker logReaderTracker;

    public static Core INSTANCE;

    private BundleListener bundleListener = new SynchronousBundleListener() {

        public void bundleChanged(BundleEvent event) {
            Bundle bundle = event.getBundle();
            switch(event.getType()) {
                case BundleEvent.INSTALLED:
                    installedBundle(bundle);
                    break;
                case BundleEvent.RESOLVED:
                    resolvedBundle(bundle);
                    break;
                case BundleEvent.STARTED:
                    activeBundle(bundle);
                    break;
                case BundleEvent.STOPPING:
                    stoppingBundle(bundle);
                    break;
                case BundleEvent.STOPPED:
                    stoppedBundle(bundle);
                    break;
                case BundleEvent.UNINSTALLED:
                    uninstalledBundle(bundle);
                    break;
                case BundleEvent.UNRESOLVED:
                    unresolvedBundle(bundle);
                    break;
            }
        }
    };

    private FrameworkListener frameworkListener = new FrameworkListener() {

        public void frameworkEvent(FrameworkEvent event) {
            switch(event.getType()) {
                case FrameworkEvent.STARTED:
                    frameworkStarted();
                    break;
                case FrameworkEvent.INFO:
                    log.info(event);
                    break;
                case FrameworkEvent.ERROR:
                    log.error(event);
                    break;
                case FrameworkEvent.WARNING:
                    log.warn(event);
                    break;
                case FrameworkEvent.PACKAGES_REFRESHED:
                    log.info(event);
                    break;
            }
        }
    };

    boolean debug = true;

    private BundleContext bundleContext;

    Map activeBundles = Collections.synchronizedMap(new HashMap());

    Map unresolvedBundles = Collections.synchronizedMap(new HashMap());

    Map installedBundles = Collections.synchronizedMap(new HashMap());

    Map mapEventRegistry = Collections.synchronizedMap(new HashMap());

    ;

    Map mapDaemons = Collections.synchronizedMap(new HashMap());

    ;

    ServiceTracker daemonTracker;

    FileLock lock;

    private Thread appMain;

    EventAdminImpl eventAdmin;

    PackageAdmin packageAdmin;

    PreferencesServiceImpl preferencesService;

    ConfigurationAdminImpl configurationAdmin;

    private PeerAdminImpl peerAdmin;

    private ServiceTracker applicationTracker;

    private Persistence bundlePersistence;

    public Core() {
        INSTANCE = this;
    }

    protected void frameworkStarted() {
        FrameworkImpl.INSTANCE.getBundleRegistry().startPendingBundles();
        persistBundleState();
        IdleEventListener listener = new IdleEventListener() {

            public void onEvent(IdleEvent ev) {
                FrameworkImpl.INSTANCE.removeIdleEventListener(this);
                loadBundlesFromStore();
                FrameworkImpl.INSTANCE.getBundleRegistry().startPendingBundles();
                String jimorc = bundleContext.getProperty(JIMOConstants.KEY_JIMORC);
                if (jimorc != null && !"".equals(jimorc)) {
                    File f = new File(jimorc);
                    if (f.exists()) {
                        log.info(MessageFormat.format(getResourceString(CoreConstants.KEY_LOADINGRC), new Object[] { jimorc }));
                        try {
                            CommandProcessor processor = new CommandProcessor(new FileInputStream(f), System.err, bundleContext, "jimo/command/jimorc", false);
                            processor.run();
                        } catch (Exception e) {
                            log.error(e);
                        }
                    }
                }
            }
        };
        FrameworkImpl.INSTANCE.addIdleEventListener(listener, getClass().getClassLoader());
    }

    private void launchApp(final ServiceReference appServiceReference, final Application app) {
        appMain = new Thread() {

            public void run() throws IllegalStateException {
                if (appServiceReference.getBundle().getState() != Bundle.ACTIVE) throw new IllegalStateException();
                log.info(config.getResourceString(CoreConstants.KEY_APPTHREADSTARTED));
                try {
                    int exitCode = app.run();
                    FrameworkImpl.INSTANCE.getConfig().setProperty(JIMOConstants.KEY_EXITCODE, "" + exitCode);
                } catch (Throwable e) {
                    log.error(e);
                }
                log.info(config.getResourceString(CoreConstants.KEY_APPTHREADSTOPPING));
                try {
                    FrameworkImpl.INSTANCE.stop();
                } catch (FrameworkException e) {
                    log.error(e);
                }
            }

            ;
        };
        appMain.setContextClassLoader(((BundleImpl) appServiceReference.getBundle()).getBundleClassLoader());
        appMain.setName("Application-" + appServiceReference.getProperty(Constants.SERVICE_ID));
        try {
            Event event = FrameworkImpl.newIdleEvent(new Runnable() {

                public void run() {
                    appMain.start();
                }
            }, true);
            FrameworkImpl.fireEvent(event);
        } catch (FrameworkException e) {
            log.error(e);
        }
    }

    protected Daemon startDaemon(final ServiceReference reference) {
        final Daemon daemon = (Daemon) bundleContext.getService(reference);
        Thread oldThread = (Thread) mapDaemons.get(daemon);
        if (oldThread != null && oldThread.isAlive()) {
            oldThread.interrupt();
            try {
                oldThread.join();
            } catch (InterruptedException e) {
                log.error(e);
            }
        }
        Thread thread = new Thread(new Runnable() {

            public void run() {
                try {
                    int state = reference.getBundle().getState();
                    if (state != Bundle.ACTIVE && state != Bundle.STARTING) {
                        log.info("Closing daemon " + reference.getProperty(Constants.SERVICE_ID));
                        return;
                    }
                    log.info("Starting daemon " + reference.getProperty(Constants.SERVICE_ID));
                    daemon.run();
                } catch (Exception e) {
                    log.error(e);
                } finally {
                    mapDaemons.remove(daemon);
                }
            }
        });
        thread.setDaemon(true);
        thread.setContextClassLoader(((BundleImpl) reference.getBundle()).getBundleClassLoader());
        thread.setName("Daemon-" + reference.getProperty(Constants.SERVICE_ID));
        thread.start();
        mapDaemons.put(daemon, thread);
        return daemon;
    }

    public void start(final BundleContext context) throws Exception {
        config = new Config(FrameworkImpl.INSTANCE.getConfig(), ResourceBundle.getBundle("Bundle"));
        config.load(context.getBundle().getResource("module.properties").openStream());
        log = new LogUtil(context, Core.class.getName(), config);
        this.bundleContext = context;
        File file = bundleContext.getDataFile(CoreConstants.INSTANCE_LOCK);
        if (!file.exists()) file.createNewFile();
        RandomAccessFile lockFile = new RandomAccessFile(file, "rw");
        FileChannel channel = lockFile.getChannel();
        lock = channel.tryLock();
        if (lock == null) {
            log.error(config.getResourceString(CoreConstants.KEY_ERRINSTANCE));
            FrameworkImpl.INSTANCE.stop();
            return;
        }
        context.addBundleListener(bundleListener);
        context.addFrameworkListener(frameworkListener);
        packageAdmin = new PackageAdminImpl(context);
        eventAdmin = new EventAdminImpl(context);
        preferencesService = new PreferencesServiceImpl(context);
        configurationAdmin = new ConfigurationAdminImpl(context);
        peerAdmin = new PeerAdminImpl(context);
        ServiceFactory factory = new ServiceFactory() {

            public Object getService(final Bundle bundle, ServiceRegistration registration) {
                return new BundleServiceImpl() {

                    public File openConfigFile(String name) {
                        return FrameworkImpl.INSTANCE.getConfigFile(bundle, name);
                    }

                    public void addClasspathURL(URL url) {
                        BundleImpl bundleImpl = (BundleImpl) bundle;
                        BundleRegistryImpl reg = (BundleRegistryImpl) FrameworkImpl.INSTANCE.getBundleRegistry();
                        reg.addUrl(bundleImpl.getBundleClassLoader(), url);
                    }

                    public Config createConfig(ResourceBundle resBundle) {
                        return new Config(config, resBundle);
                    }

                    public void addIdleEventListener(IdleEventListener listener) throws FrameworkException {
                        BundleImpl bundleImpl = (BundleImpl) bundle;
                        FrameworkImpl.INSTANCE.addIdleEventListener(listener, bundleImpl.getBundleClassLoader());
                    }
                };
            }

            public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
            }
        };
        context.registerService(BundleService.class.getName(), factory, null);
        Dictionary properties;
        properties = new Hashtable();
        properties.put(JIMOConstants.SERVICE_COMMANDNAME, CoreConstants.HELP_COMMANDNAME);
        context.registerService(CommandHandler.class.getName(), new CommandHandler() {

            public void onCommand(String command, CommandContext context) {
                String helpText = getResourceString(CoreConstants.KEY_COMMANDHELP);
                context.print(helpText);
                context.newLine();
            }
        }, properties);
        properties = new Hashtable();
        properties.put(JIMOConstants.SERVICE_COMMANDNAME, CoreConstants.CORE_COMMANDNAME);
        context.registerService(CommandHandler.class.getName(), new CoreCommandHandler(), properties);
        properties = new Hashtable();
        properties.put(JIMOConstants.SERVICE_COMMANDNAME, CoreConstants.DISCONNECT_COMMANDNAME);
        context.registerService(CommandHandler.class.getName(), new CommandHandler() {

            public void onCommand(String command, CommandContext context) {
                context.close();
            }
        }, properties);
        properties = new Hashtable();
        properties.put(JIMOConstants.SERVICE_COMMANDNAME, CoreConstants.LIST_COMMANDNAME);
        context.registerService(CommandHandler.class.getName(), new CommandHandler() {

            public void onCommand(String command, CommandContext context) {
                ServiceReference[] commands = CommandProcessor.getCommands(getBundleContext());
                for (int i = 0; i < commands.length; i++) {
                    String name = (String) commands[i].getProperty(JIMOConstants.SERVICE_COMMANDNAME);
                    context.println(name);
                }
            }
        }, properties);
        Event event = FrameworkImpl.newEvent(true);
        Listener[] listeners = new Listener[] { new Listener() {

            public void onEvent(Event ev) throws FrameworkException {
                logTracker = new ServiceTracker(context, LogService.class.getName(), null);
                logTracker.open();
            }
        }, new Listener() {

            public void onEvent(Event ev) throws FrameworkException {
                daemonTracker = new ServiceTracker(bundleContext, Daemon.class.getName(), new ServiceTrackerCustomizer() {

                    public Object addingService(ServiceReference reference) {
                        return startDaemon(reference);
                    }

                    public void modifiedService(ServiceReference reference, Object service) {
                    }

                    public void removedService(ServiceReference reference, Object service) {
                        Thread thread = (Thread) mapDaemons.get(service);
                        if (thread != null && thread.isAlive()) thread.interrupt();
                        mapDaemons.remove(service);
                    }
                });
                daemonTracker.open();
                ServiceReference[] serviceReferences = daemonTracker.getServiceReferences();
                if (serviceReferences != null) {
                    for (int i = 0; i < serviceReferences.length; i++) {
                        ServiceReference reference = serviceReferences[i];
                        startDaemon(reference);
                    }
                }
            }
        }, new Listener() {

            public void onEvent(Event ev) throws FrameworkException {
                final String appId = bundleContext.getProperty(JIMOConstants.KEY_APPLICATION);
                if (appId != null && !"".equals(appId)) {
                    Filter filter = null;
                    try {
                        filter = context.createFilter("(& (" + JIMOConstants.SERVICE_APPID + "=" + appId + ") (" + Constants.OBJECTCLASS + "=" + Application.class.getName() + ") )");
                    } catch (InvalidSyntaxException e1) {
                        log.error(e1);
                    }
                    applicationTracker = new ServiceTracker(context, filter, new ServiceTrackerCustomizer() {

                        public Object addingService(ServiceReference reference) {
                            log.info(config.format(config.getResourceString(CoreConstants.KEY_STARTINGAPPLICATION), new Object[] { appId }));
                            Application app = (Application) context.getService(reference);
                            launchApp(reference, app);
                            return app;
                        }

                        public void modifiedService(ServiceReference reference, Object service) {
                        }

                        public void removedService(ServiceReference reference, Object service) {
                            String serviceAppId = (String) reference.getProperty(JIMOConstants.SERVICE_APPID);
                            if (!appId.equals(serviceAppId)) return;
                            if (appMain != null && appMain.isAlive()) {
                                appMain.interrupt();
                                try {
                                    appMain.join();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                appMain = null;
                            }
                        }
                    });
                    applicationTracker.open();
                }
            }
        }, new Listener() {

            public void onEvent(Event ev) throws FrameworkException {
                boolean consoleMode = new Boolean(context.getProperty(JIMOConstants.KEY_CONSOLE)).booleanValue();
                if (consoleMode) {
                    context.registerService(new String[] { Daemon.class.getName(), CommandProcessor.class.getName() }, new CommandProcessor(System.in, System.err, bundleContext, COMMANDPROCESSOR, true), null);
                }
                String host = context.getProperty(JIMOConstants.KEY_SHELLHOST);
                if (host != null && !"".equals(host)) {
                    try {
                        int port = Integer.parseInt(context.getProperty(JIMOConstants.KEY_SHELLPORT));
                        ServerSocket socket = new ServerSocket();
                        SocketAddress endpoint = new InetSocketAddress(host, port);
                        socket.bind(endpoint);
                        ServerSocketShell shell = new ServerSocketShell(socket, context);
                        context.registerService(Daemon.class.getName(), shell, null);
                    } catch (Throwable e) {
                        log.error(e);
                    }
                }
            }
        }, new Listener() {

            public void onEvent(Event ev) throws FrameworkException {
                File file = bundleContext.getDataFile(CoreConstants.BUNDLE_INSTALLFILE);
                if (file.exists()) installBundles(file); else persistBundleState();
                if (!BundleClassLoaderImpl.resolve()) {
                    log.warn(CoreConstants.KEY_UNRESOLVEDIMPORTS);
                }
                FrameworkImpl.INSTANCE.getBundleRegistry().startPendingBundles();
                try {
                    FrameworkImpl.INSTANCE.sendFrameworkEvent(FrameworkEvent.STARTED, bundleContext.getBundle(), null);
                } catch (FrameworkException e) {
                    log.error(e);
                }
            }
        } };
        event.addListeners(listeners);
        FrameworkImpl.fireEvent(event);
        EventHandler handler = new EventHandler() {

            CommandContext commandContext = new CommandContext(null, System.err, false);

            CommandProcessor commandProcessor = new CommandProcessor(commandContext, bundleContext, "EVENT");

            public void handleEvent(org.osgi.service.event.Event event) {
                String cmd = (String) event.getProperty(CoreConstants.EVENT_COMMAND);
                String args = (String) event.getProperty(CoreConstants.EVENT_COMMAND);
                commandProcessor.process(cmd, args);
            }
        };
        properties = new Hashtable();
        properties.put(EventConstants.EVENT_TOPIC, new String[] { CoreConstants.TOPIC_COMMANDEXEC });
        context.registerService(EventHandler.class.getName(), handler, properties);
    }

    public static ServiceTracker getLogTracker() {
        return logTracker;
    }

    public static LogService getLogService() {
        return (LogService) logTracker.getService();
    }

    protected void unresolvedBundle(Bundle bundle) {
        unresolvedBundles.put(bundle.getSymbolicName(), bundle);
    }

    protected void uninstalledBundle(Bundle bundle) {
        installedBundles.remove(bundle.getSymbolicName());
        unresolvedBundles.remove(bundle.getSymbolicName());
        unstoreBundle(bundle);
    }

    protected void stoppedBundle(Bundle bundle) {
        activeBundles.remove(bundle.getSymbolicName());
        storeBundle(bundle);
        if (bundle.getBundleId() == JIMOConstants.SYSTEMBUNDLEID) {
            if (lock.isValid()) {
                try {
                    lock.release();
                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
                File file = bundleContext.getDataFile(CoreConstants.INSTANCE_LOCK);
                file.delete();
            }
        }
    }

    protected void stoppingBundle(Bundle bundle) {
        if (bundle.getBundleId() == JIMOConstants.SYSTEMBUNDLEID) {
            if (FrameworkImpl.INSTANCE.getClosingStatus() != FrameworkImpl.CLOSE_FAILURE) persistBundleState();
            if (appMain != null && appMain.isAlive()) {
                appMain.interrupt();
                try {
                    appMain.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                appMain = null;
            }
            closeAllBundles();
        }
    }

    protected void activeBundle(Bundle bundle) {
        if (activeBundles.containsKey(bundle.getSymbolicName())) {
            log.error(CoreConstants.KEY_BUNDLEALREADYREGISTERED, new Object[] { bundle.getSymbolicName(), bundle.getLocation() });
            Bundle reg = (Bundle) activeBundles.get(bundle.getSymbolicName());
            log.warn("Registered at \"" + reg.getLocation() + "\"");
            return;
        }
        activeBundles.put(bundle.getSymbolicName(), bundle);
        storeBundle(bundle);
    }

    protected void resolvedBundle(Bundle bundle) {
        unresolvedBundles.remove(bundle.getSymbolicName());
    }

    protected void installedBundle(Bundle bundle) {
        if (installedBundles.containsKey(bundle.getSymbolicName())) {
            log.warn("Bundle already registered \"" + bundle.getSymbolicName() + "\" for \"" + bundle.getLocation() + "\"");
            Bundle reg = (Bundle) installedBundles.get(bundle.getSymbolicName());
            log.warn("Registered at \"" + reg.getLocation() + "\"");
            return;
        }
        installedBundles.put(bundle.getSymbolicName(), bundle);
        unresolvedBundles.put(bundle.getSymbolicName(), bundle);
        storeBundle(bundle);
    }

    private void storeBundle(Bundle bundle) {
        Dictionary properties = new Hashtable();
        properties.put(BundleRegistryPersistenceImpl.BUNDLE_NAME, bundle.getSymbolicName());
        properties.put(BundleRegistryPersistenceImpl.BUNDLE_ID, new Long(bundle.getBundleId()));
        properties.put(BundleRegistryPersistenceImpl.BUNDLE_LOCATION, bundle.getLocation());
        boolean active = FrameworkImpl.INSTANCE.getBundleRegistry().isStarted(bundle);
        properties.put(BundleRegistryPersistenceImpl.BUNDLE_ACTIVE, Boolean.valueOf(active));
        try {
            getBundlePersistence().storeItem(properties);
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void unstoreBundle(Bundle bundle) {
        try {
            getBundlePersistence().removeItem("" + bundle.getBundleId());
        } catch (IOException e) {
            log.error(e);
        }
    }

    private void loadBundlesFromStore() {
        Dictionary[] dictionaries;
        try {
            dictionaries = getBundlePersistence().findItems(null);
        } catch (IOException e) {
            log.error(e);
            return;
        }
        Arrays.sort(dictionaries, new Comparator() {

            public int compare(Object o1, Object o2) {
                Long id1 = (Long) ((Dictionary) o1).get(BundleRegistryPersistenceImpl.BUNDLE_ID);
                Long id2 = (Long) ((Dictionary) o2).get(BundleRegistryPersistenceImpl.BUNDLE_ID);
                return id1.compareTo(id2);
            }
        });
        for (int i = 0; i < dictionaries.length; i++) {
            Dictionary dictionary = dictionaries[i];
            String location = (String) dictionary.get(BundleRegistryPersistenceImpl.BUNDLE_LOCATION);
            Long id = (Long) dictionary.get(BundleRegistryPersistenceImpl.BUNDLE_ID);
            Bundle bundle = null;
            try {
                bundle = FrameworkImpl.INSTANCE.getBundleRegistry().installBundle(location, id.longValue());
            } catch (BundleRegistryException e) {
                log.error(e);
                try {
                    getBundlePersistence().removeItem("" + id);
                } catch (IOException e1) {
                    log.warn(e1);
                }
                continue;
            }
            if (bundle.getBundleId() != id.longValue()) {
                try {
                    getBundlePersistence().removeItem("" + id);
                } catch (IOException e) {
                    log.warn(e);
                }
            }
            Boolean active = (Boolean) dictionary.get(BundleRegistryPersistenceImpl.BUNDLE_ACTIVE);
            if (active.booleanValue()) {
                FrameworkImpl.INSTANCE.getBundleRegistry().setStarted(bundle);
            }
        }
    }

    private Persistence getBundlePersistence() {
        if (bundlePersistence == null) {
            String storageImplClassname = getConfig().getProperty(JIMOConstants.KEY_BUNDLEPERSISTENCEIMPL);
            try {
                Class cls = Class.forName(storageImplClassname);
                if (cls != null) bundlePersistence = (Persistence) cls.newInstance();
            } catch (ClassNotFoundException e) {
                Core.getLogService().log(LogService.LOG_ERROR, "Exception creating BundleStorage <" + storageImplClassname + ">", e);
            } catch (InstantiationException e) {
                Core.getLogService().log(LogService.LOG_ERROR, "Exception creating BundleStorage <" + storageImplClassname + ">", e);
            } catch (IllegalAccessException e) {
                Core.getLogService().log(LogService.LOG_ERROR, "Exception creating BundleStorage <" + storageImplClassname + ">", e);
            }
        }
        return bundlePersistence;
    }

    /**
	 * The file contains a list of bundles to install that the
	 * core bundle depends on.
	 * @param file
	 */
    void installBundles(File file) {
        log.info(CoreConstants.KEY_READINSTALL, new Object[] { file.getPath() });
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while (reader.ready()) {
                String line = reader.readLine();
                log.info(line);
                String[] strings = line.split("#");
                if (strings.length != 3) throw new IllegalArgumentException();
                final int state = Integer.parseInt(strings[0]);
                final String location = strings[2];
                try {
                    if (state >= Bundle.INSTALLED) {
                        Runnable r = new Runnable() {

                            public void run() {
                                BundleImpl bundle;
                                try {
                                    bundle = (BundleImpl) bundleContext.installBundle(location);
                                    if (state == Bundle.ACTIVE) {
                                        FrameworkImpl.INSTANCE.getBundleRegistry().setStarted(bundle);
                                    }
                                } catch (BundleException e) {
                                    log.warn(e.getMessage());
                                }
                            }
                        };
                        Event event = FrameworkImpl.newEvent(r, true);
                        FrameworkImpl.fireEvent(event);
                    }
                } catch (FrameworkException e) {
                    log.warn(e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn(e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.warn(e.getMessage());
                }
            }
        }
    }

    protected synchronized void persistBundleState() {
        FileOutputStream out = null;
        PrintWriter print = null;
        File file = bundleContext.getDataFile(CoreConstants.BUNDLE_INSTALLFILE);
        log.info(config.getResourceString(CoreConstants.KEY_CREATEINSTALL), new Object[] { file.getPath() });
        try {
            file.createNewFile();
            out = new FileOutputStream(file);
            print = new PrintWriter(out);
            Bundle[] bundles = bundleContext.getBundles();
            for (int i = 0; i < bundles.length; i++) {
                Bundle bundle = bundles[i];
                if (!Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(bundle.getSymbolicName()) && bundle.getSymbolicName().matches(JIMOConstants.COREBUNDLES_REGEX)) print.println("" + bundle.getState() + "#" + bundle.getBundleId() + "#" + bundle.getLocation());
            }
        } catch (IOException e) {
            log.warn(e.getMessage());
        } finally {
            if (print != null) {
                print.flush();
                print.close();
            }
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    log.warn(e);
                }
            }
        }
    }

    public void stop(BundleContext context) throws Exception {
        log.info(config.getResourceString(CoreConstants.KEY_SHUTDOWN));
    }

    protected void closeAllBundles() {
        Bundle[] bundles = bundleContext.getBundles();
        for (int i = bundles.length - 1; i > 0; --i) {
            Bundle bundle = bundles[i];
            if (bundle.equals(this)) continue;
            int state = bundle.getState();
            if (state == Bundle.ACTIVE || state == bundle.STARTING) {
                try {
                    bundle.stop();
                } catch (BundleException e) {
                    log.warn(CoreConstants.KEY_SHUTDOWNBUNDLEERROR, new Object[] { bundle.getLocation() });
                    log.warn(e);
                }
            }
        }
    }

    public static String getResourceString(String key) {
        return config.getResourceString(key);
    }

    public static Config getConfig() {
        return config;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }
}
