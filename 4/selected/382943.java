package xml;

import java.awt.Component;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.xml.resolver.Catalog;
import org.gjt.sp.jedit.EditBus;
import org.gjt.sp.jedit.EBComponent;
import org.gjt.sp.jedit.EBMessage;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.GUIUtilities;
import org.gjt.sp.jedit.MiscUtilities;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.VFSUpdate;
import org.gjt.sp.util.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;
import org.w3c.dom.ls.LSResourceResolver;
import org.w3c.dom.ls.LSInput;
import static xml.Debug.*;
import xml.PathUtilities;

/**
 * Resolver grabs and caches DTDs and xml schemas.
 * It also serves as a resource resolver for jeditresource: links
 *
 * @author ezust
 * @author kerik-sf
 * @version $Id: Resolver.java 20811 2012-01-15 15:40:14Z kerik-sf $
 *
 */
public class Resolver implements EntityResolver2, LSResourceResolver {

    /** Ask before downloading */
    public static final String ASK = "ask";

    /** Local files & catalogs only */
    public static final String LOCAL = "local";

    /** Download without asking */
    public static final String ALWAYS = "always";

    public static final String MODES[] = new String[] { ASK, LOCAL, ALWAYS };

    private boolean loadedCache = false;

    private boolean loadedCatalogs = false;

    public static final String NETWORK_PROPS = "xml.general.network";

    void init() {
        EditBus.addToBus(vfsUpdateHandler = new VFSUpdateHandler());
    }

    void uninit() {
        EditBus.removeFromBus(vfsUpdateHandler);
        singleton = null;
    }

    public void save() {
        if (loadedCache) {
            int systemCount = 0;
            int publicCount = 0;
            Iterator keys = resourceCache.keySet().iterator();
            while (keys.hasNext()) {
                Entry entry = (Entry) keys.next();
                Object uri = resourceCache.get(entry);
                if (uri == IGNORE || uri == null) continue;
                if (entry.type == Entry.PUBLIC) {
                    jEdit.setProperty("xml.cache.public-id." + publicCount, entry.id);
                    jEdit.setProperty("xml.cache.public-id." + publicCount + ".uri", uri.toString());
                    publicCount++;
                } else {
                    jEdit.setProperty("xml.cache.system-id." + systemCount, entry.id);
                    jEdit.setProperty("xml.cache.system-id." + systemCount + ".uri", uri.toString());
                    systemCount++;
                }
            }
            jEdit.unsetProperty("xml.cache.public-id." + publicCount);
            jEdit.unsetProperty("xml.cache.public-id." + publicCount + ".uri");
            jEdit.unsetProperty("xml.cache.system-id." + systemCount);
            jEdit.unsetProperty("xml.cache.system-id." + systemCount + ".uri");
        }
    }

    /**
	 * Ask before downloading remote files
	 */
    public static final String MODE = NETWORK_PROPS + ".mode";

    /**
	 * Cache downloaded remote files
	 */
    public static final String CACHE = NETWORK_PROPS + ".cache";

    /** Internal catalog for DTDs which are packaged in
	 * XML.jar and jEdit.jar */
    public static final String INTERNALCATALOG = "jeditresource:/XML.jar!/xml/dtds/catalog";

    private static String IGNORE = new String("IGNORE");

    private static Resolver singleton = null;

    private static String resourceDir;

    private EBComponent vfsUpdateHandler;

    /** Internal catalog for DTDs which are packaged in
	 * XML.jar and jEdit.jar
	   Parses and manages the catalog files
	   Moved away from Xerces' XMLCatalogResolver,
	   as it's really an overlay on top of commons-resolver
	   and it supports less catalog formats than commons-resolver
	   */
    private Catalog catalog = null;

    /** Mapping from public ID to URLs */
    private HashMap<Entry, String> resourceCache;

    /** Set of catalog files to load.
	 *  A set is used to remove duplicates (either via symlinks or double entry by the user)
	 */
    private Set<String> catalogFiles;

    /**
	 *
	 * @return a global catalog resolver object you can use as an
	 * LSResourceResolver or EntityResolver.
	 */
    public static synchronized Resolver instance() {
        if (singleton == null) {
            jEdit.getPlugin(xml.XmlPlugin.class.getName()).getPluginJAR().activatePlugin();
            singleton = new Resolver();
            singleton.init();
            singleton.load();
        }
        return singleton;
    }

    /**
	 * You can't create an object directly.
	 * use @ref instance() to get a singleton instance.
	 *
	 */
    private Resolver() {
    }

    private synchronized void load() {
        if (!loadedCache) {
            resourceCache = new HashMap<Entry, String>();
            if (isUsingCache()) {
                resourceDir = MiscUtilities.constructPath(jEdit.getSettingsDirectory(), "dtds");
            }
            int i;
            String id, prop, uri;
            i = 0;
            while ((id = jEdit.getProperty(prop = "xml.cache" + ".public-id." + i++)) != null) {
                uri = jEdit.getProperty(prop + ".uri");
                resourceCache.put(new Entry(Entry.PUBLIC, id, uri), uri);
            }
            i = 0;
            while ((id = jEdit.getProperty(prop = "xml.cache" + ".system-id." + i++)) != null) {
                uri = jEdit.getProperty(prop + ".uri");
                Entry se = new Entry(Entry.SYSTEM, id, uri);
                resourceCache.put(se, uri);
                if (DEBUG_RESOLVER) Log.log(Log.DEBUG, Resolver.class, "loading cache " + id + " -> " + uri);
            }
            loadedCache = true;
        }
        if (!loadedCatalogs) {
            loadedCatalogs = true;
            catalog = new Catalog();
            catalog.getCatalogManager().setPreferPublic(true);
            catalog.getCatalogManager().ignoreMissingProperties(!DEBUG_RESOLVER);
            catalog.getCatalogManager().setVerbosity(DEBUG_RESOLVER ? Integer.MAX_VALUE : 2);
            catalog.setupReaders();
            catalogFiles = new HashSet<String>();
            catalogFiles.add(INTERNALCATALOG);
            try {
                if (DEBUG_RESOLVER) Log.log(Log.MESSAGE, Resolver.class, "Loading system catalogs");
                catalog.loadSystemCatalogs();
                if (DEBUG_RESOLVER) Log.log(Log.MESSAGE, Resolver.class, "Loading internal catalog: " + INTERNALCATALOG);
                catalog.parseCatalog(INTERNALCATALOG);
            } catch (Exception ex1) {
                Log.log(Log.ERROR, Resolver.class, ex1);
            }
            int i = 0;
            String uri = null;
            do {
                String prop = "xml.catalog." + i++;
                uri = jEdit.getProperty(prop);
                if (uri == null) break;
                if (DEBUG_RESOLVER) Log.log(Log.DEBUG, Resolver.class, "Loading catalog: " + uri);
                if (MiscUtilities.isURL(uri)) catalogFiles.add(uri); else catalogFiles.add(MiscUtilities.resolveSymlinks(uri));
                try {
                    catalog.parseCatalog(uri);
                } catch (Exception ex2) {
                    ex2.printStackTrace();
                    Log.log(Log.ERROR, Resolver.class, ex2);
                }
            } while (uri != null);
        }
    }

    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        if (DEBUG_RESOLVER) Log.log(Log.DEBUG, Resolver.class, "resolveResource(" + type + "," + namespaceURI + "," + publicId + "," + systemId + "," + baseURI);
        try {
            InputSource is = resolveEntity(type, publicId, baseURI, systemId);
            if (is == null) return null; else return new InputSourceAsLSInput(is);
        } catch (SAXException e) {
            throw new RuntimeException("Error loading resource " + systemId, e);
        } catch (IOException e) {
            throw new RuntimeException("Error loading resource " + systemId, e);
        }
    }

    /**
	 * wrapper arround an InputSource for DOM2 Load and Save,
	 * needed to implement LSResourceResolver for javax.xml.validation.SchemaFactory.
	 * No setter method is active.
	 * Maybe this should be the other way round : implement natively LSResourceResolver
	 * and wrap an LSInput as InputSource...
	 */
    private static class InputSourceAsLSInput implements LSInput {

        private InputSource is;

        InputSourceAsLSInput(InputSource is) {
            this.is = is;
        }

        public String getBaseURI() {
            return null;
        }

        public InputStream getByteStream() {
            return is.getByteStream();
        }

        public boolean getCertifiedText() {
            return false;
        }

        public Reader getCharacterStream() {
            return is.getCharacterStream();
        }

        public String getEncoding() {
            return is.getEncoding();
        }

        public String getPublicId() {
            return is.getPublicId();
        }

        public String getStringData() {
            return null;
        }

        public String getSystemId() {
            return is.getSystemId();
        }

        /**
         * @throws UnsupportedOperationException no setter !
         */
        public void setBaseURI(String baseURI) {
            throw new UnsupportedOperationException("setBaseURI()");
        }

        /**
         * @throws UnsupportedOperationException no setter !
         */
        public void setByteStream(InputStream byteStream) {
            throw new UnsupportedOperationException("setByteStream()");
        }

        /**
         * @throws UnsupportedOperationException no setter !
         */
        public void setCertifiedText(boolean certifiedText) {
            throw new UnsupportedOperationException("setCertifiedText()");
        }

        /**
         * @throws UnsupportedOperationException no setter !
         */
        public void setCharacterStream(Reader characterStream) {
            throw new UnsupportedOperationException("setCharacterStream()");
        }

        /**
         * @throws UnsupportedOperationException no setter !
         */
        public void setEncoding(String encoding) {
            throw new UnsupportedOperationException("setEncoding()");
        }

        /**
         * @throws UnsupportedOperationException no setter !
         */
        public void setPublicId(String publicId) {
            throw new UnsupportedOperationException("setPublicId()");
        }

        /**
         * @throws UnsupportedOperationException no setter !
         */
        public void setStringData(String stringData) {
            throw new UnsupportedOperationException("setStringData()");
        }

        /**
         * @throws UnsupportedOperationException no setter !
         */
        public void setSystemId(String systemId) {
            throw new UnsupportedOperationException("setSystemId()");
        }
    }

    /** implements SAX1 EntityResolver
	 * @see org.xml.sax.ext.DefaultHandler2#resolveEntity(java.lang.String, java.lang.String)
	 */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (DEBUG_RESOLVER) Log.log(Log.DEBUG, Resolver.class, "simple resolveEntity(" + publicId + "," + systemId + ")");
        return resolveEntity(null, publicId, null, systemId);
    }

    /**
	 * @param name
	 * @param publicId
	 * @param current
	 * @param systemId
	 */
    public String resolveEntityToPath(String name, String publicId, String current, String systemId) throws java.io.IOException {
        String[] res = resolveEntityToPathInternal(name, publicId, current, systemId);
        if (res == null) return null; else return res[1];
    }

    /**
	 * systemId may be modified, for instance if resolving docbookx.dtd, 
	 * the systemId  will be the full jeditresource:XML.jar!.../docbookx.dtd
	 * @return array [systemId to report, real systemId]
	 */
    public String[] resolveEntityToPathInternal(String name, String publicId, String current, String systemId) throws java.io.IOException {
        if (publicId != null && publicId.length() == 0) publicId = null;
        if (systemId != null && systemId.length() == 0) systemId = null;
        String newSystemId = null;
        if (DEBUG_RESOLVER) Log.log(Log.DEBUG, Resolver.class, "Resolver.resolveEntity(" + name + "," + publicId + "," + current + "," + systemId + ")");
        if (publicId == null && systemId == null) return null;
        String parent;
        if (current != null) {
            parent = MiscUtilities.getParentOfPath(current);
        } else parent = null;
        if (publicId == null) newSystemId = resolvePublicOrSystem(systemId, false); else {
            newSystemId = resolvePublicOrSystem(publicId, true);
            if (newSystemId == null && systemId != null) {
                newSystemId = resolvePublicOrSystem(systemId, false);
            }
        }
        if (newSystemId == null) {
            if (systemId == null) return null; else if (MiscUtilities.isURL(systemId)) newSystemId = systemId; else {
                if (new File(systemId).isAbsolute() || parent == null) {
                    newSystemId = systemId;
                } else {
                    if (DEBUG_RESOLVER) Log.log(Log.DEBUG, Resolver.class, "using parent !");
                    newSystemId = parent + systemId;
                    systemId = newSystemId;
                }
                if (!MiscUtilities.isURL(newSystemId)) {
                    try {
                        newSystemId = new File(newSystemId).toURI().toURL().toString();
                        ;
                        systemId = newSystemId;
                    } catch (java.net.MalformedURLException mue) {
                    }
                }
            }
        }
        if (newSystemId == null) return null;
        String lastChance = resolvePublicOrSystemFromCache(newSystemId, false);
        if (lastChance != null && lastChance != IGNORE) {
            if (DEBUG_RESOLVER) Log.log(Log.DEBUG, Resolver.class, "was going to fetch it again !");
            newSystemId = lastChance;
        }
        return new String[] { systemId, newSystemId };
    }

    public InputSource resolveEntity(String name, String publicId, String current, String systemId) throws SAXException, java.io.IOException {
        String[] sids = resolveEntityToPathInternal(name, publicId, current, systemId);
        if (sids == null) return null; else return openEntity(name, publicId, current, sids[0], sids[1]);
    }

    /** open an already resolved Entity.
	  * Not public because the systemId may have to be changed (see result of resolveEntityToPathInternal)
	  */
    private InputSource openEntity(String name, String publicId, String current, String systemId, String newSystemId) throws SAXException, java.io.IOException {
        if (newSystemId == null) return null;
        Buffer buf = jEdit.getBuffer(PathUtilities.urlToPath(newSystemId));
        if (buf != null) {
            if (buf.isPerformingIO()) VFSManager.waitForRequests();
            if (DEBUG_RESOLVER) Log.log(Log.DEBUG, getClass(), "Found open buffer for " + newSystemId);
            InputSource source = new InputSource(publicId);
            source.setSystemId(systemId);
            try {
                buf.readLock();
                source.setCharacterStream(new StringReader(buf.getText(0, buf.getLength())));
            } finally {
                buf.readUnlock();
            }
            return source;
        } else if (newSystemId.startsWith("file:") || newSystemId.startsWith("jar:file:") || newSystemId.startsWith("jeditresource:")) {
            if (newSystemId.startsWith("jeditresource:")) {
                systemId = newSystemId;
            }
            if (DEBUG_RESOLVER) {
                Log.log(Log.DEBUG, Resolver.class, "resolving to local file: " + newSystemId);
                Log.log(Log.DEBUG, Resolver.class, "systemId=: " + systemId);
            }
            InputSource source = new InputSource(systemId);
            source.setPublicId(publicId);
            InputStream is = new URL(newSystemId).openStream();
            source.setByteStream(is);
            return source;
        } else if (LOCAL.equals(getNetworkMode())) {
            if (DEBUG_RESOLVER) Log.log(Log.DEBUG, Resolver.class, "refusing to fetch remote entity (configured for Local-only)");
            throw new IOException(jEdit.getProperty("xml.network.error"));
        } else {
            final String _newSystemId = newSystemId;
            final VFS vfs = VFSManager.getVFSForPath(_newSystemId);
            final Object[] sessionArray = new Object[1];
            Runnable run = new Runnable() {

                public void run() {
                    View view = jEdit.getActiveView();
                    if (ALWAYS.equals(getNetworkMode()) || (ASK.equals(getNetworkMode()) && showDownloadResourceDialog(view, _newSystemId))) {
                        sessionArray[0] = vfs.createVFSSession(_newSystemId, view);
                    }
                }
            };
            if (SwingUtilities.isEventDispatchThread()) run.run(); else {
                try {
                    SwingUtilities.invokeAndWait(run);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            Object session = sessionArray[0];
            if (session != null) {
                InputSource source = new InputSource(systemId);
                source.setPublicId(publicId);
                if (isUsingCache()) {
                    File file;
                    try {
                        file = copyToLocalFile(session, vfs, newSystemId);
                    } finally {
                        vfs._endVFSSession(session, null);
                    }
                    addUserResource(publicId, systemId, file.toURI().toURL().toString());
                    source.setByteStream(new FileInputStream(file));
                } else source.setByteStream(vfs._createInputStream(session, newSystemId, false, null));
                if (DEBUG_RESOLVER) {
                    Log.log(Log.DEBUG, Resolver.class, "resolving to remote file: " + newSystemId);
                    Log.log(Log.DEBUG, Resolver.class, "systemId=: " + systemId);
                }
                return source;
            } else {
                throw new IOException(jEdit.getProperty("xml.network.error"));
            }
        }
    }

    public void clearCache() {
        Iterator files = resourceCache.values().iterator();
        while (files.hasNext()) {
            Object obj = files.next();
            if (obj instanceof String) {
                String file = (String) PathUtilities.urlToPath((String) obj);
                Log.log(Log.NOTICE, getClass(), "Deleting " + file);
                new File(file).delete();
            }
        }
        int i = 0;
        String prop;
        while (jEdit.getProperty(prop = "xml.cache" + ".public-id." + i++) != null) {
            jEdit.unsetProperty(prop);
            jEdit.unsetProperty(prop + ".uri");
        }
        i = 0;
        while (jEdit.getProperty(prop = "xml.cache" + ".system-id." + i++) != null) {
            jEdit.unsetProperty(prop);
            jEdit.unsetProperty(prop + ".uri");
        }
        resourceCache.clear();
    }

    /**
	 * called from actions.xml
	 */
    public synchronized void reloadCatalogs() {
        loadedCatalogs = false;
        load();
    }

    private String resolvePublicOrSystemFromCache(String id, boolean isPublic) {
        Entry e = new Entry(isPublic ? Entry.PUBLIC : Entry.SYSTEM, id, null);
        if (DEBUG_RESOLVER) Log.log(Log.DEBUG, Resolver.class, "resolvePublicOrSystemFromCache(" + id + ")");
        String uri = resourceCache.get(e);
        if (DEBUG_RESOLVER) {
            if (uri == IGNORE) {
                Log.log(Log.DEBUG, Resolver.class, "ignored!");
            } else if (uri == null) {
                Log.log(Log.DEBUG, Resolver.class, "not found " + id + " in cache");
            } else {
                Log.log(Log.DEBUG, Resolver.class, "found " + id + " in cache: " + uri);
            }
        }
        return uri;
    }

    String resolvePublicOrSystem(String id, boolean isPublic) throws IOException {
        String uri = resolvePublicOrSystemFromCache(id, isPublic);
        if (uri == null) if (isPublic) {
            return catalog.resolvePublic(id, null);
        } else {
            return catalog.resolveSystem(id);
        } else if (uri == IGNORE) return null; else {
            return uri;
        }
    }

    private static File copyToLocalFile(Object session, VFS vfs, String path) throws IOException {
        if (jEdit.getSettingsDirectory() == null) return null;
        File _resourceDir = new File(resourceDir);
        if (!_resourceDir.exists()) _resourceDir.mkdir();
        BufferedInputStream in = new BufferedInputStream(vfs._createInputStream(session, path, false, null));
        File localFile = File.createTempFile("cache", ".xml", _resourceDir);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(localFile));
        byte[] buf = new byte[4096];
        int count = 0;
        while ((count = in.read(buf)) != -1) out.write(buf, 0, count);
        out.close();
        return localFile;
    }

    private boolean showDownloadResourceDialog(Component comp, String systemId) {
        Entry e = new Entry(Entry.SYSTEM, systemId, null);
        if (resourceCache.get(e) == IGNORE) return false;
        int result = GUIUtilities.confirm(comp, "xml.download-resource", new String[] { systemId }, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) return true; else {
            resourceCache.put(e, IGNORE);
            return false;
        }
    }

    /**
	 * Don't want this public because then invoking {@link clearCache()}
	 * will remove this file, not what you would expect!
	 */
    private void addUserResource(String publicId, String systemId, String url) {
        if (DEBUG_RESOLVER) Log.log(Log.DEBUG, Resolver.class, "addUserResource(" + publicId + "," + systemId + "," + url + ")");
        if (publicId != null) {
            Entry pe = new Entry(Entry.PUBLIC, publicId, url);
            resourceCache.put(pe, url);
        }
        Entry se = new Entry(Entry.SYSTEM, systemId, url);
        resourceCache.put(se, url);
    }

    public static class Entry {

        public static final int SYSTEM = 0;

        public static final int PUBLIC = 1;

        public int type;

        public String id;

        public String uri;

        public Entry(int type, String id, String uri) {
            this.type = type;
            this.id = id;
            this.uri = uri;
        }

        public boolean equals(Object o) {
            if (o instanceof Entry) {
                Entry e = (Entry) o;
                return e.type == type && e.id.equals(id);
            } else return false;
        }

        public int hashCode() {
            return id.hashCode();
        }

        public String toString() {
            return "Resolver.Entry{" + (type == SYSTEM ? "SYSTEM" : "PUBLIC") + ",id=" + id + ",uri=" + uri + "}";
        }
    }

    /**
	 * Reloads all catalog files when the user changes one of it on disk.
	 * copied from CatalogManager
	 */
    public class VFSUpdateHandler implements EBComponent {

        public void handleMessage(EBMessage msg) {
            if (!loadedCatalogs) return;
            if (msg instanceof VFSUpdate) {
                String path = ((VFSUpdate) msg).getPath();
                if (catalogFiles.contains(path)) loadedCatalogs = false;
            }
        }
    }

    public void propertiesChanged() {
        loadedCatalogs = false;
    }

    public static boolean isUsingCache() {
        if (jEdit.getSettingsDirectory() == null) return false;
        return jEdit.getBooleanProperty(CACHE);
    }

    public static void setUsingCache(boolean newCache) {
        jEdit.setBooleanProperty(CACHE, newCache);
    }

    /**
	 *
	 * @return the network mode: LOCAL, ASK, or ALWAYS
	 */
    public static String getNetworkMode() {
        return jEdit.getProperty(MODE);
    }

    public static void setNetworkMode(String newMode) {
        if (!LOCAL.equals(newMode) && !ASK.equals(newMode) && !ALWAYS.equals(newMode)) newMode = ASK;
        jEdit.setProperty(MODE, newMode);
    }

    public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
        return null;
    }
}
