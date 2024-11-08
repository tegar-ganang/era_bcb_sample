package org.aladdinframework.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.aladdinframework.application.api.Constants.PluginInstallStatus;
import org.aladdinframework.contextplugin.api.ContextPlugin;
import org.aladdinframework.contextplugin.api.IContextPluginRuntimeFactory;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.main.AutoProcessor;
import org.apache.felix.main.Main;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Manages the underlying OSGi framework on behalf of the Aladdin Framework. This class is adapted from the FelixService
 * class developed within the Music Middleware project, which is released under an LGPL license.
 * 
 * @author Darren Carlson
 */
public class OSGIManager implements FrameworkListener, ServiceListener {

    public static final String TAG = OSGIManager.class.getSimpleName();

    private Map<ContextPlugin, BundleInstaller> installers = new ConcurrentHashMap<ContextPlugin, BundleInstaller>();

    private Map<Bundle, IContextPluginRuntimeFactory> factoryCache = new ConcurrentHashMap<Bundle, IContextPluginRuntimeFactory>();

    /**
     * Our local OSGi Framework
     */
    protected static Framework osgiFramework;

    public static final String DEPLOYMENT_DIR_PROPERTY = "deployment.dir";

    public static final String OSGI_DIR = "felix";

    public static final String OSGI_CACHE_DIR = "felix-cache";

    public static final String VERSIONCODE_FILE = "versionCode";

    private Context androidContext;

    private AladdinService aladdinService;

    /**
     * Creates an OSGIManager based on the incoming androidContext
     * 
     * @param androidContext
     *            The Android Context of the AladdinService.
     */
    public OSGIManager(Context androidContext) {
        this.androidContext = androidContext;
    }

    /**
     * Sets the reference to the AladdinService. This is necessary for performing AladdinService callbacks.
     * 
     * @param aladdinService
     *            The current AladdinService object.
     */
    public void setAladdinService(AladdinService aladdinService) {
        this.aladdinService = aladdinService;
    }

    /**
     * Initialize the OSGIManager using a background Thread. Once OSGi is initialized, a FrameworkListener event will be
     * passed from the OSGi Framework to our local 'frameworkEvent' method, which handles AladdinService callbacks. This
     * method is asynchronous.
     */
    public void init() {
        new Thread() {

            @Override
            public void run() {
                Log.i(TAG, "Starting the Android service for the OSGi framework");
                String applicationDataDir = getApplicationDataDir();
                String osgiDeploymentDir = applicationDataDir.concat(File.separator).concat(OSGI_DIR);
                Log.i(TAG, "The deployment directory for the Felix framework is: ".concat(osgiDeploymentDir));
                boolean isSameVersionCode = isSameVersionCode();
                if (!isSameVersionCode || !new File(osgiDeploymentDir).exists()) {
                    if (new File(osgiDeploymentDir).exists()) new File(osgiDeploymentDir).delete();
                    long initial_time = System.currentTimeMillis();
                    unzipFile(androidContext.getResources().openRawResource(R.raw.felix), applicationDataDir);
                    Log.i(TAG, "Unzipped in: " + (System.currentTimeMillis() - initial_time) / 1000 + " seconds.");
                    parseFelixConfigurationFile(osgiDeploymentDir);
                    System.setProperty(Main.CONFIG_PROPERTIES_PROP, "file://".concat(osgiDeploymentDir).concat("/conf/parsed_config.properties"));
                    if (!isSameVersionCode) saveVersionCode();
                } else {
                    System.setProperty(Main.CONFIG_PROPERTIES_PROP, "file://".concat(osgiDeploymentDir).concat("/conf/restart.properties"));
                }
                System.setProperty(Main.SYSTEM_PROPERTIES_PROP, "file://".concat(osgiDeploymentDir).concat("/conf/system.properties"));
                Main.loadSystemProperties();
                Properties configProps = Main.loadConfigProperties();
                configProps.setProperty(Constants.FRAMEWORK_STORAGE, OSGI_CACHE_DIR);
                configProps.setProperty("felix.auto.deploy.dir", osgiDeploymentDir + "/bundle");
                configProps.setProperty(BundleCache.CACHE_ROOTDIR_PROP, osgiDeploymentDir);
                try {
                    Log.i(TAG, "Starting the OSGi framework...");
                    long initial_time = System.currentTimeMillis();
                    FrameworkFactory factory = new org.apache.felix.framework.FrameworkFactory();
                    osgiFramework = factory.newFramework(configProps);
                    osgiFramework.init();
                    osgiFramework.getBundleContext().addFrameworkListener(OSGIManager.this);
                    String filter = "(" + Constants.OBJECTCLASS + "=" + IContextPluginRuntimeFactory.class.getName() + ")";
                    osgiFramework.getBundleContext().addServiceListener(OSGIManager.this, filter);
                    AutoProcessor.process(configProps, osgiFramework.getBundleContext());
                    osgiFramework.start();
                    Log.i(TAG, "OSGi framework started in: " + (System.currentTimeMillis() - initial_time) / 1000 + " seconds");
                } catch (Throwable t) {
                    Log.w(TAG, "The OSGi framework could not be started", t);
                }
            }
        }.start();
    }

    /**
     * Starts the OSGi Bundle associated with the specified ContextPlugin.
     * 
     * @param plug
     *            The ContextPlugin's bundle to start.
     * Returns true if the bundle was started; false, otherwise.
     */
    public boolean startPluginBundle(ContextPlugin plug) {
        if (plug.isInstalled()) {
            Log.i(TAG, "Trying to start OSGi bundle: " + plug.getBundleId());
            Bundle b = osgiFramework.getBundleContext().getBundle(plug.getBundleId());
            if (b != null) {
                if (b.getState() != Bundle.STARTING || b.getState() != Bundle.ACTIVE) {
                    try {
                        Log.v(TAG, "Starting Bundle: " + b + ", which has state: " + b.getState());
                        b.start();
                        Log.v(TAG, "Started Bundle: " + b + ", which has state: " + b.getState());
                        Properties props = new Properties();
                        Log.i(TAG, "Creating IContextPluginRuntimeFactory for: " + plug);
                        Class<?> factoryClass = b.loadClass(plug.getRuntimeFactoryClass());
                        Object factory = factoryClass.newInstance();
                        Log.i(TAG, "IContextPluginRuntimeFactory ServiceRegistration for: " + factory);
                        ServiceRegistration reg = b.getBundleContext().registerService(IContextPluginRuntimeFactory.class.getName(), factory, props);
                        if (reg != null) {
                            Log.i(TAG, "Started OSGi Bundle for ContextPlugin: " + plug);
                            return true;
                        } else {
                            Log.w(TAG, "Service registration failed. This is probably pretty bad.");
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Exception: " + e.getMessage());
                    }
                } else {
                    Log.i(TAG, "Bundle is already starting or started. Bundle state was: " + b.getState());
                    return true;
                }
            } else Log.w(TAG, "Bundle not found for ContextPlugin: " + plug);
        } else Log.w(TAG, "Cannot start " + plug + " because it's not installed!");
        return false;
    }

    /**
     * Stops the OSGi Bundle associated with the specified ContextPlugin.
     * 
     * @param plug
     *            The ContextPlugin's bundle to stop.
     * Returns true if the bundle was stopped; false, otherwise.
     */
    public boolean stopPluginBundle(ContextPlugin plug) {
        if (plug.isInstalled()) {
            Log.i(TAG, "Trying to stop OSGi bundle: " + plug.getBundleId());
            Bundle b = osgiFramework.getBundleContext().getBundle(plug.getBundleId());
            if (b != null) {
                try {
                    b.stop();
                    Log.i(TAG, "Stopped OSGi Bundle for ContextPlugin: " + plug);
                    return true;
                } catch (BundleException e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                }
            } else {
                Log.w(TAG, "Bundle not found for ContextPlugin: " + plug);
            }
        } else Log.w(TAG, "Cannot stop " + plug + " because it's not installed!");
        return false;
    }

    /**
     * Checks if the bundle for a specific ContextPlugin is installed
     * 
     * @param plug
     *            The ContextPlugin to check for
     * Returns true if the ContextPlugin's bundle is installed; false otherwise
     */
    public boolean isBundleInstalled(ContextPlugin plug) {
        for (Bundle b : factoryCache.keySet()) {
            if (b.getBundleContext().getBundle().getSymbolicName().equalsIgnoreCase(plug.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Installs the OSGi Bundle for the specified ContextPlugin. If the Bundle is already installed for the
     * ContextPlugin, this method operates synchronously, returning true immediately. However, if the OSGi Bundle is not
     * installed, this method operates asynchronously, launching a threaded BundleInstaller and returning false. If a
     * BundleInstaller is launched, the OSGi Framework will call the 'serviceChanged' method when the process is
     * complete.
     * 
     * @param plug
     *            The ContextPlugin to install
     * Returns true if the Bundle was installed; false otherwise.
     */
    public boolean installBundle(ContextPlugin plug) {
        ArrayList<Bundle> remove = new ArrayList<Bundle>();
        if (plug != null) {
            for (Bundle b : factoryCache.keySet()) {
                if (b.getState() == Bundle.ACTIVE) {
                    if (b.getBundleContext().getBundle().getSymbolicName().equalsIgnoreCase(plug.getId())) {
                        return true;
                    }
                } else remove.add(b);
            }
            if (!installers.keySet().contains(plug)) {
                Log.i(TAG, "Installing Bundle for ContextPlugin: " + plug);
                BundleInstaller installer = new BundleInstaller(plug);
                installers.put(plug, installer);
                installer.startInstall();
            } else Log.w(TAG, "Plugin already installing: " + plug + " with status: " + plug.getInstallStatus());
        } else Log.e(TAG, "ContextPlugin was null in installBundle");
        for (Bundle b : remove) {
            Log.i(TAG, "Removing inactive Bundle from the factoryCache: " + b);
            factoryCache.remove(b);
        }
        return false;
    }

    /**
     * Uninstalls a previously installed OSGi Bundle for the specified ContextPlugin.
     * 
     * @param plug
     *            The ContextPlugin's Bundle to uninstall.
     * Returns true if the Bundle was uninstalled; false, otherwise.
     */
    public boolean uninstallBundle(ContextPlugin plug) {
        Bundle b = osgiFramework.getBundleContext().getBundle(plug.getBundleId());
        if (b != null) {
            stopPluginBundle(plug);
            try {
                b.uninstall();
                return true;
            } catch (BundleException e) {
                e.printStackTrace();
                Log.w(TAG, "Bundle Uninstall Error: " + e.getMessage());
            }
        } else Log.w(TAG, "uninstallPlugin could not find Bundle for: " + plug);
        return false;
    }

    /**
     * Returns the previously cached IContextPluginRuntimeFactory for the specified ContextPlugin.
     * 
     * @param plug
     *            The ContextPlugin to obtain the IContextPluginRuntimeFactory for.
     * Returns the associated IContextPluginRuntimeFactory, or null if not found.
     */
    public IContextPluginRuntimeFactory getContextPluginRuntimeFactory(ContextPlugin plug) {
        Log.i(TAG, "Accessing IContextPluginRuntimeFactory service for ContextPlugin: " + plug);
        Bundle b = osgiFramework.getBundleContext().getBundle(plug.getBundleId());
        if (b != null) return factoryCache.get(b); else return null;
    }

    /**
     * Stops the OSGi Framework and clears the factory and installer caches.
     */
    public void stopFramework() {
        Log.i(TAG, "Stopping OSGi Framework");
        if (osgiFramework != null) {
            try {
                for (BundleInstaller i : installers.values()) {
                    i.stopInstall();
                }
                installers.clear();
                factoryCache.clear();
                osgiFramework.stop();
                osgiFramework = null;
            } catch (BundleException e) {
                e.printStackTrace();
                Log.e(TAG, "stopFramework error: " + e.getMessage());
            }
        }
    }

    /**
     * Event handler for OSGi FrameworkEvents.
     * 
     * @see FrameworkEvent
     */
    public void frameworkEvent(FrameworkEvent event) {
        Log.d(TAG, "FrameworkEvent: " + event.toString());
        switch(event.getType()) {
            case FrameworkEvent.STARTED:
                AladdinService.onOSGiFrameworkStarted();
                break;
            case FrameworkEvent.STOPPED:
                AladdinService.onOSGiFrameworkStopped();
                break;
            case FrameworkEvent.ERROR:
                AladdinService.onOSGiFrameworkError();
                break;
        }
    }

    /**
     * Event handler for OSGi ServiceEvents.
     * 
     * @see ServiceEvent
     */
    public void serviceChanged(ServiceEvent event) {
        Log.i(TAG, "serviceChanged for: " + event.getServiceReference().getBundle().getSymbolicName() + " type: " + event.getType());
        if (event.getType() == ServiceEvent.REGISTERED) {
            Bundle b = event.getServiceReference().getBundle();
            if (b != null) {
                try {
                    IContextPluginRuntimeFactory factory = (IContextPluginRuntimeFactory) event.getServiceReference().getBundle().getBundleContext().getService(event.getServiceReference());
                    if (!factoryCache.containsKey(b)) {
                        factoryCache.put(b, factory);
                        Log.d(TAG, "Cached IContextPluginRuntimeFactory: " + factory);
                    } else Log.d(TAG, "IContextPluginRuntimeFactory factory cache already contained: " + factory);
                } catch (Exception e) {
                    Log.w(TAG, "Error creating IContextPluginRuntimeFactory: " + e.getMessage());
                }
            } else Log.w(TAG, "event.getServiceReference().getBundle() was NULL");
        }
        if (aladdinService != null) aladdinService.handleServiceEvent(event);
    }

    /**
     * Utility method that outputs all installed OSGi services to the Log. See:
     * http://www.osgi.org/javadoc/r4v42/org/osgi
     * /framework/BundleContext.html#getServiceReferences%28java.lang.String,%20java.lang.String%29
     */
    private void listAllOSGiServices() {
        Log.i(TAG, "Listing installed OSGi services");
        ServiceReference[] srs;
        try {
            srs = osgiFramework.getBundleContext().getAllServiceReferences(null, null);
            for (ServiceReference sr : srs) {
                Log.i(TAG, "Service: " + sr);
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Utility method that outputs all installed OSGi Bundles to the Log.
     */
    private void listBundles() {
        Log.i(TAG, "Listing installed OSGi bundles");
        Bundle[] bdls;
        bdls = osgiFramework.getBundleContext().getBundles();
        for (Bundle b : bdls) {
            Log.i(TAG, "Bundle: " + b + " / State: " + b.getState());
            try {
                if (b.getState() != Bundle.ACTIVE) {
                    Log.i(TAG, "Starting " + b);
                    b.start();
                }
            } catch (BundleException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Unzip an input stream into a directory
     * 
     * @param inputStream
     *            Input stream corresponding to the zip file
     * @param directory
     *            Directory to store the unzipped content
     */
    private void unzipFile(InputStream inputStream, String directory) {
        try {
            Log.i(TAG, "Unzip the OSGi framework to: " + directory);
            JarInputStream jarInputStream = new JarInputStream(inputStream);
            JarEntry jarEntry;
            byte[] buffer = new byte[2048];
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                File jarEntryFile = new File(directory + File.separator + jarEntry.getName());
                if (jarEntry.isDirectory()) {
                    jarEntryFile.mkdirs();
                    continue;
                }
                FileOutputStream fos = new FileOutputStream(jarEntryFile);
                while (true) {
                    int read = jarInputStream.read(buffer);
                    if (read == -1) break;
                    fos.write(buffer, 0, read);
                }
                fos.close();
            }
            jarInputStream.close();
        } catch (Throwable t) {
            Log.w(TAG, "Error unzipping the zip file", t);
        }
    }

    /**
     * Get the directory for the application data
     * 
     * @return
     */
    private String getApplicationDataDir() {
        String applicationDataDir = null;
        try {
            applicationDataDir = System.getProperty(DEPLOYMENT_DIR_PROPERTY);
            if (applicationDataDir == null) applicationDataDir = androidContext.getFilesDir().getAbsolutePath();
        } catch (Throwable t) {
            Log.w(TAG, "Error getting the directory for the application data", t);
        }
        return applicationDataDir;
    }

    /**
     * Get the versionCode from the AndroidManifest file of the Android application
     * 
     * @return
     */
    private int getVersionCodeFromManifest() {
        int versionCode = -1;
        try {
            PackageInfo packageInfo = androidContext.getPackageManager().getPackageInfo(androidContext.getPackageName(), PackageManager.GET_META_DATA);
            versionCode = packageInfo.versionCode;
        } catch (Throwable t) {
            Log.w(TAG, "Error getting the version code from the AndroidManifest XML file", t);
        }
        return versionCode;
    }

    /**
     * Check if it is not installed a new version of the application by comparing the versionCode in the
     * AndroidManifest.xml file and the value in the versionCode file in the data directory of the application
     * 
     * @return
     */
    private boolean isSameVersionCode() {
        boolean isSaveVersionCode = false;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = androidContext.openFileInput(VERSIONCODE_FILE);
            byte[] buffer = new byte[128];
            int count;
            String savedVersionCodeString = "";
            while ((count = fileInputStream.read(buffer)) != -1) savedVersionCodeString += new String(buffer, 0, count);
            int savedVersionCode = Integer.parseInt(savedVersionCodeString);
            int manifestVersionCode = getVersionCodeFromManifest();
            if (manifestVersionCode != -1 && manifestVersionCode == savedVersionCode) isSaveVersionCode = true;
        } catch (Throwable t) {
            Log.w(TAG, "The version code is not saved yet. Is it the first installation?");
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (Throwable th) {
                    Log.w(TAG, "The versionCode file reader could not be closed", th);
                }
            }
        }
        return isSaveVersionCode;
    }

    /**
     * Save the AndroidManifest.xml version code in the versionCode file
     */
    private void saveVersionCode() {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = androidContext.openFileOutput(VERSIONCODE_FILE, Context.MODE_PRIVATE);
            int versionCode = getVersionCodeFromManifest();
            String versionCodeString = String.valueOf(versionCode);
            fileOutputStream.write(versionCodeString.getBytes());
            Log.i(TAG, "The current version code  is ".concat(versionCodeString));
        } catch (Throwable t) {
            Log.w(TAG, "The version code could not be saved", t);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Throwable th) {
                    Log.w(TAG, "The versionCode file writer could not be closed", th);
                }
            }
        }
    }

    /**
     * Generate a new configuration file where the relative paths to the OSGi bundles are replaced by absolute patch
     * considering the felixDeploymentDir. This is just a hack for Felix which does not permit to specify the directory
     * where the OSGi bundles are stores as other OSGi frameworks actually do.
     * 
     * @param felixDeploymentDir
     * @return
     */
    private boolean parseFelixConfigurationFile(String felixDeploymentDir) {
        boolean isParsed = false;
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        try {
            String originalConfigFilePath = felixDeploymentDir.concat("/conf/config.properties");
            String parsedConfigFilePath = felixDeploymentDir.concat("/conf/parsed_config.properties");
            bufferedReader = new BufferedReader(new FileReader(originalConfigFilePath));
            bufferedWriter = new BufferedWriter(new FileWriter(parsedConfigFilePath));
            Pattern pattern = Pattern.compile("file:bundle/");
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                line = matcher.replaceAll("file:".concat(felixDeploymentDir).concat("/bundle/"));
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (bufferedReader != null) try {
                bufferedReader.close();
            } catch (Throwable th) {
                Log.w(TAG, "The buffered reader for the original configuration file could not be closed", th);
            }
            if (bufferedWriter != null) try {
                bufferedWriter.close();
            } catch (Throwable th) {
                Log.w(TAG, "The buffered writer for the parsed configuration file could not be closed", th);
            }
        }
        return isParsed;
    }

    /**
     * Local class used for installing OSGi Bundles on a dedicated Thread.
     * 
     * @author Darren Carlson
     */
    private class BundleInstaller implements Runnable {

        private ContextPlugin plug;

        private Thread t;

        /**
	 * Created the BundleInstaller for the specified ContextPlugin
	 * 
	 * @param plug
	 *            The ContextPlugin whose Bundle we should install
	 */
        public BundleInstaller(ContextPlugin plug) {
            this.plug = plug;
        }

        /**
	 * Starts the install process using a Thread
	 */
        public synchronized void startInstall() {
            switch(plug.getInstallStatus()) {
                case INSTALLING:
                    Log.w(TAG, "Already installing: " + plug);
                    break;
                case INSTALLED:
                    Log.w(TAG, "Plugin is already installed: " + plug);
                    break;
                default:
                    plug.setInstallStatus(PluginInstallStatus.INSTALLING);
                    t = new Thread(this);
                    t.setDaemon(true);
                    t.start();
            }
        }

        /**
	 * Stops a running install process
	 */
        public synchronized void stopInstall() {
            if (t != null) t.interrupt();
        }

        /**
	 * The BundleInstaller install process runs in this method. Currently, we simply use the install facilities of
	 * the OSGi Framework; however, future versions may handle downloading and installing Bundles more robustly.
	 */
        public void run() {
            Log.i(TAG, "BundleInstaller installing from: " + plug.getInstallUrl());
            Bundle b = null;
            try {
                b = osgiFramework.getBundleContext().installBundle(plug.getInstallUrl());
                plug.setBundleId(b.getBundleId());
                plug.setInstallStatus(PluginInstallStatus.INSTALLED);
                Log.i(TAG, "BundleInstaller finished download and is trying to start: " + b);
                if (startPluginBundle(plug)) {
                    AladdinService.handleBundleInstalled(plug);
                    Log.i(TAG, "Bundle successfully installed from: " + plug.getInstallUrl() + " / has OSGi bundle id: " + b.getBundleId());
                } else {
                    throw new BundleException("Bundle failed to start!");
                }
            } catch (BundleException e) {
                plug.setInstallStatus(PluginInstallStatus.ERROR);
                if (b != null) {
                    try {
                        b.uninstall();
                    } catch (BundleException e1) {
                    }
                }
                Log.w(TAG, "Bundle Install Error: " + e.getMessage());
                AladdinService.handleBundleInstallError(plug);
            } finally {
                installers.remove(plug);
                t = null;
                plug = null;
            }
        }
    }
}
