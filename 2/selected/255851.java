package org.eclipse.core.runtime.internal.adaptor;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.osgi.baseadaptor.*;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.baseadaptor.hooks.StorageHook;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

public final class EclipseStorageHook implements StorageHook, HookConfigurator {

    private static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration";

    private static final int STORAGE_VERION = 2;

    public static final String KEY = EclipseStorageHook.class.getName();

    public static final int HASHCODE = KEY.hashCode();

    private static final byte FLAG_AUTO_START = 0x01;

    private static final byte FLAG_HAS_PACKAGE_INFO = 0x02;

    private static final byte FLAG_ACTIVATE_ON_CLASSLOAD = 0x04;

    /** data to detect modification made in the manifest */
    private long manifestTimeStamp = 0;

    private byte manifestType = PluginConverterImpl.MANIFEST_TYPE_UNKNOWN;

    private BaseData bundledata;

    /** the Plugin-Class header */
    private String pluginClass = null;

    /**  Eclipse-LazyStart header */
    private String[] autoStartExceptions;

    /** shortcut to know if a bundle has a buddy */
    private String buddyList;

    /** shortcut to know if a bundle is a registrant to a registered policy */
    private String registeredBuddyList;

    private byte flags = 0;

    public int getStorageVersion() {
        return STORAGE_VERION;
    }

    public StorageHook create(BaseData bundledata) throws BundleException {
        EclipseStorageHook storageHook = new EclipseStorageHook();
        storageHook.bundledata = bundledata;
        return storageHook;
    }

    public void initialize(Dictionary manifest) throws BundleException {
        String lazyStart = (String) manifest.get(Constants.ECLIPSE_LAZYSTART);
        if (lazyStart == null) lazyStart = (String) manifest.get(Constants.ECLIPSE_AUTOSTART);
        parseLazyStart(this, lazyStart);
        pluginClass = (String) manifest.get(Constants.PLUGIN_CLASS);
        buddyList = (String) manifest.get(Constants.BUDDY_LOADER);
        registeredBuddyList = (String) manifest.get(Constants.REGISTERED_POLICY);
        if (hasPackageInfo(bundledata.getEntry(Constants.OSGI_BUNDLE_MANIFEST))) flags |= FLAG_HAS_PACKAGE_INFO;
        String genFrom = (String) manifest.get(PluginConverterImpl.GENERATED_FROM);
        if (genFrom != null) {
            ManifestElement generatedFrom = ManifestElement.parseHeader(PluginConverterImpl.GENERATED_FROM, genFrom)[0];
            if (generatedFrom != null) {
                manifestTimeStamp = Long.parseLong(generatedFrom.getValue());
                manifestType = Byte.parseByte(generatedFrom.getAttribute(PluginConverterImpl.MANIFEST_TYPE_ATTRIBUTE));
            }
        }
    }

    public StorageHook load(BaseData target, DataInputStream in) throws IOException {
        EclipseStorageHook storageHook = new EclipseStorageHook();
        storageHook.bundledata = target;
        storageHook.flags = in.readByte();
        int exceptionsCount = in.readInt();
        storageHook.autoStartExceptions = exceptionsCount > 0 ? new String[exceptionsCount] : null;
        for (int i = 0; i < exceptionsCount; i++) storageHook.autoStartExceptions[i] = in.readUTF();
        storageHook.buddyList = AdaptorUtil.readString(in, false);
        storageHook.registeredBuddyList = AdaptorUtil.readString(in, false);
        storageHook.pluginClass = AdaptorUtil.readString(in, false);
        storageHook.manifestTimeStamp = in.readLong();
        storageHook.manifestType = in.readByte();
        return storageHook;
    }

    public void save(DataOutputStream out) throws IOException {
        if (bundledata == null) throw new IllegalStateException();
        out.writeByte(flags & ~FLAG_ACTIVATE_ON_CLASSLOAD);
        String[] autoStartExceptions = getAutoStartExceptions();
        if (autoStartExceptions == null) out.writeInt(0); else {
            out.writeInt(autoStartExceptions.length);
            for (int i = 0; i < autoStartExceptions.length; i++) out.writeUTF(autoStartExceptions[i]);
        }
        AdaptorUtil.writeStringOrNull(out, getBuddyList());
        AdaptorUtil.writeStringOrNull(out, getRegisteredBuddyList());
        AdaptorUtil.writeStringOrNull(out, getPluginClass());
        out.writeLong(getManifestTimeStamp());
        out.writeByte(getManifestType());
    }

    public int getKeyHashCode() {
        return HASHCODE;
    }

    public boolean compare(KeyedElement other) {
        return other.getKey() == KEY;
    }

    public Object getKey() {
        return KEY;
    }

    public boolean isAutoStart() {
        return (flags & FLAG_AUTO_START) == FLAG_AUTO_START;
    }

    public String[] getAutoStartExceptions() {
        return autoStartExceptions;
    }

    public String getBuddyList() {
        return buddyList;
    }

    public boolean hasPackageInfo() {
        return (flags & FLAG_HAS_PACKAGE_INFO) == FLAG_HAS_PACKAGE_INFO;
    }

    public String getPluginClass() {
        return pluginClass;
    }

    public String getRegisteredBuddyList() {
        return registeredBuddyList;
    }

    public long getManifestTimeStamp() {
        return manifestTimeStamp;
    }

    public byte getManifestType() {
        return manifestType;
    }

    /**
	 * Checks whether this bundle is auto started for all resource/class loads or only for a
	 * subset of resource/classloads 
	 * @return true if the bundle is auto started; false otherwise
	 */
    public boolean isAutoStartable() {
        return isAutoStart() || (autoStartExceptions != null && autoStartExceptions.length > 0);
    }

    private void parseLazyStart(EclipseStorageHook storageHook, String headerValue) {
        storageHook.autoStartExceptions = null;
        ManifestElement[] allElements = null;
        try {
            allElements = ManifestElement.parseHeader(Constants.ECLIPSE_LAZYSTART, headerValue);
        } catch (BundleException e) {
            String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CLASSLOADER_CANNOT_GET_HEADERS, storageHook.bundledata.getLocation());
            bundledata.getAdaptor().getFrameworkLog().log(new FrameworkLogEntry(FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, FrameworkLogEntry.ERROR, 0, message, 0, e, null));
        }
        if (allElements == null) return;
        if ("true".equalsIgnoreCase(allElements[0].getValue())) flags |= FLAG_AUTO_START;
        String exceptionsValue = allElements[0].getAttribute(Constants.ECLIPSE_LAZYSTART_EXCEPTIONS);
        if (exceptionsValue == null) return;
        StringTokenizer tokenizer = new StringTokenizer(exceptionsValue, ",");
        int numberOfTokens = tokenizer.countTokens();
        storageHook.autoStartExceptions = new String[numberOfTokens];
        for (int i = 0; i < numberOfTokens; i++) storageHook.autoStartExceptions[i] = tokenizer.nextToken().trim();
    }

    private static boolean hasPackageInfo(URL url) {
        if (url == null) return false;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Specification-Title: ") || line.startsWith("Specification-Version: ") || line.startsWith("Specification-Vendor: ") || line.startsWith("Implementation-Title: ") || line.startsWith("Implementation-Version: ") || line.startsWith("Implementation-Vendor: ")) return true;
            }
        } catch (IOException ioe) {
        } finally {
            if (br != null) try {
                br.close();
            } catch (IOException e) {
            }
        }
        return false;
    }

    public void addHooks(HookRegistry hookRegistry) {
        hookRegistry.addStorageHook(this);
    }

    private void checkTimeStamp() throws IllegalArgumentException {
        if (!checkManifestTimeStamp()) throw new IllegalArgumentException();
    }

    private boolean checkManifestTimeStamp() {
        if (!"true".equalsIgnoreCase(FrameworkProperties.getProperty(EclipseStorageHook.PROP_CHECK_CONFIG))) return true;
        if (PluginConverterImpl.getTimeStamp(bundledata.getBundleFile().getBaseFile(), getManifestType()) == getManifestTimeStamp()) {
            if ((getManifestType() & (PluginConverterImpl.MANIFEST_TYPE_JAR | PluginConverterImpl.MANIFEST_TYPE_BUNDLE)) != 0) return true;
            String cacheLocation = FrameworkProperties.getProperty(LocationManager.PROP_MANIFEST_CACHE);
            Location parentConfiguration = LocationManager.getConfigurationLocation().getParentLocation();
            if (parentConfiguration != null) {
                try {
                    return checkManifestAndParent(cacheLocation, bundledata.getSymbolicName(), bundledata.getVersion().toString(), getManifestType()) != null;
                } catch (BundleException e) {
                    return false;
                }
            }
            File cacheFile = new File(cacheLocation, bundledata.getSymbolicName() + '_' + bundledata.getVersion() + ".MF");
            if (cacheFile.isFile()) return true;
        }
        return false;
    }

    private Headers checkManifestAndParent(String cacheLocation, String symbolicName, String version, byte inputType) throws BundleException {
        Headers result = basicCheckManifest(cacheLocation, symbolicName, version, inputType);
        if (result != null) return result;
        Location parentConfiguration = null;
        if ((parentConfiguration = LocationManager.getConfigurationLocation().getParentLocation()) != null) result = basicCheckManifest(new File(parentConfiguration.getURL().getFile(), FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME + '/' + LocationManager.MANIFESTS_DIR).toString(), symbolicName, version, inputType);
        return result;
    }

    private Headers basicCheckManifest(String cacheLocation, String symbolicName, String version, byte inputType) throws BundleException {
        File currentFile = new File(cacheLocation, symbolicName + '_' + version + ".MF");
        if (PluginConverterImpl.upToDate(currentFile, bundledata.getBundleFile().getBaseFile(), inputType)) {
            try {
                return Headers.parseManifest(new FileInputStream(currentFile));
            } catch (FileNotFoundException e) {
            }
        }
        return null;
    }

    Dictionary createCachedManifest(boolean firstTime) throws BundleException {
        return firstTime ? getGeneratedManifest() : new CachedManifest(this);
    }

    public Dictionary getGeneratedManifest() throws BundleException {
        Dictionary builtIn = AdaptorUtil.loadManifestFrom(bundledata);
        if (builtIn != null) {
            if (!isComplete(builtIn)) {
                Dictionary generatedManifest = generateManifest(builtIn);
                if (generatedManifest != null) return generatedManifest;
            }
            manifestType = PluginConverterImpl.MANIFEST_TYPE_BUNDLE;
            if (bundledata.getBundleFile().getBaseFile().isFile()) {
                manifestTimeStamp = bundledata.getBundleFile().getBaseFile().lastModified();
                manifestType |= PluginConverterImpl.MANIFEST_TYPE_JAR;
            } else manifestTimeStamp = bundledata.getBundleFile().getEntry(Constants.OSGI_BUNDLE_MANIFEST).getTime();
            return builtIn;
        }
        Dictionary result = generateManifest(null);
        if (result == null) throw new BundleException(NLS.bind(EclipseAdaptorMsg.ECLIPSE_DATA_MANIFEST_NOT_FOUND, bundledata.getLocation()));
        return result;
    }

    private Dictionary generateManifest(Dictionary builtIn) throws BundleException {
        String cacheLocation = FrameworkProperties.getProperty(LocationManager.PROP_MANIFEST_CACHE);
        if (bundledata.getSymbolicName() != null) {
            Headers existingHeaders = checkManifestAndParent(cacheLocation, bundledata.getSymbolicName(), bundledata.getVersion().toString(), manifestType);
            if (existingHeaders != null) return existingHeaders;
        }
        PluginConverterImpl converter = PluginConverterImpl.getDefault();
        if (converter == null) converter = new PluginConverterImpl(bundledata.getAdaptor(), bundledata.getAdaptor().getContext());
        Dictionary generatedManifest;
        try {
            generatedManifest = converter.convertManifest(bundledata.getBundleFile().getBaseFile(), true, null, true, null);
        } catch (PluginConversionException pce) {
            String message = NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONVERTER_ERROR_CONVERTING, bundledata.getBundleFile().getBaseFile());
            throw new BundleException(message, pce);
        }
        Version version = Version.parseVersion((String) generatedManifest.get(Constants.BUNDLE_VERSION));
        String symbolicName = ManifestElement.parseHeader(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME, (String) generatedManifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME))[0].getValue();
        ManifestElement generatedFrom = ManifestElement.parseHeader(PluginConverterImpl.GENERATED_FROM, (String) generatedManifest.get(PluginConverterImpl.GENERATED_FROM))[0];
        Headers existingHeaders = checkManifestAndParent(cacheLocation, symbolicName, version.toString(), Byte.parseByte(generatedFrom.getAttribute(PluginConverterImpl.MANIFEST_TYPE_ATTRIBUTE)));
        manifestTimeStamp = Long.parseLong(generatedFrom.getValue());
        manifestType = Byte.parseByte(generatedFrom.getAttribute(PluginConverterImpl.MANIFEST_TYPE_ATTRIBUTE));
        if (bundledata.getAdaptor().isReadOnly() || existingHeaders != null) return existingHeaders;
        if (builtIn != null) {
            Enumeration keysEnum = builtIn.keys();
            while (keysEnum.hasMoreElements()) {
                Object key = keysEnum.nextElement();
                generatedManifest.put(key, builtIn.get(key));
            }
        }
        File bundleManifestLocation = new File(cacheLocation, symbolicName + '_' + version.toString() + ".MF");
        try {
            converter.writeManifest(bundleManifestLocation, generatedManifest, true);
        } catch (Exception e) {
        }
        return generatedManifest;
    }

    private boolean isComplete(Dictionary manifest) {
        if (manifest.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME) != null) return true;
        return bundledata.getEntry(PluginConverterImpl.PLUGIN_MANIFEST) == null && bundledata.getEntry(PluginConverterImpl.FRAGMENT_MANIFEST) == null;
    }

    public BaseData getBaseData() {
        return bundledata;
    }

    public void copy(StorageHook storageHook) {
    }

    public void validate() throws IllegalArgumentException {
        checkTimeStamp();
    }

    public FrameworkAdaptor getAdaptor() {
        if (bundledata != null) return bundledata.getAdaptor();
        return null;
    }

    public Dictionary getManifest(boolean firstLoad) throws BundleException {
        return createCachedManifest(firstLoad);
    }

    public boolean forgetStatusChange(int status) {
        if (!isAutoStartable()) return false;
        if (!isActivatedOnClassLoad()) {
            BundleStopper stopper = getBundleStopper(bundledata);
            return stopper == null ? false : stopper.isStopping();
        }
        return true;
    }

    public boolean forgetStartLevelChange(int startlevel) {
        return false;
    }

    public boolean matchDNChain(String pattern) {
        return false;
    }

    void setActivatedOnClassLoad(boolean classLoadActivate) {
        if (classLoadActivate) flags |= FLAG_ACTIVATE_ON_CLASSLOAD; else flags &= ~FLAG_ACTIVATE_ON_CLASSLOAD;
    }

    boolean isActivatedOnClassLoad() {
        return (flags & FLAG_ACTIVATE_ON_CLASSLOAD) == FLAG_ACTIVATE_ON_CLASSLOAD;
    }

    static BundleStopper getBundleStopper(BaseData bundledata) {
        AdaptorHook[] adaptorhooks = bundledata.getAdaptor().getHookRegistry().getAdaptorHooks();
        for (int i = 0; i < adaptorhooks.length; i++) if (adaptorhooks[i] instanceof EclipseAdaptorHook) return ((EclipseAdaptorHook) adaptorhooks[i]).getBundleStopper();
        return null;
    }
}
