package xml;

import java.awt.Component;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import org.apache.xml.resolver.Catalog;
import org.gjt.sp.jedit.*;
import org.gjt.sp.jedit.io.VFS;
import org.gjt.sp.jedit.io.VFSManager;
import org.gjt.sp.jedit.msg.VFSUpdate;
import org.gjt.sp.util.Log;
import org.xml.sax.*;

public class CatalogManager {

    public static InputSource resolve(String current, String publicId, String systemId) throws Exception {
        load();
        if (publicId != null && publicId.length() == 0) publicId = null;
        if (systemId != null && systemId.length() == 0) systemId = null;
        String newSystemId = null;
        String parent;
        if (current != null) {
            Entry entry = (Entry) reverseResourceCache.get(current);
            if (entry != null) parent = entry.uri; else parent = MiscUtilities.getParentOfPath(current);
        } else parent = null;
        if (publicId == null && systemId != null && parent != null) {
            if (systemId.startsWith(parent)) {
                newSystemId = systemId.substring(parent.length());
                if (newSystemId.startsWith("/")) newSystemId = newSystemId.substring(1);
                newSystemId = resolveSystem(newSystemId);
            }
        }
        if (newSystemId == null) {
            if (publicId == null) newSystemId = resolveSystem(systemId); else newSystemId = resolvePublic(systemId, publicId);
        }
        if (newSystemId == null) {
            if (systemId == null) return null; else if (MiscUtilities.isURL(systemId)) newSystemId = systemId;
        }
        if (newSystemId == null) return null;
        Buffer buf = jEdit.getBuffer(XmlPlugin.uriToFile(newSystemId));
        if (buf != null) {
            if (buf.isPerformingIO()) VFSManager.waitForRequests();
            Log.log(Log.DEBUG, CatalogManager.class, "Found open buffer for " + newSystemId);
            InputSource source = new InputSource(systemId);
            try {
                buf.readLock();
                source.setCharacterStream(new StringReader(buf.getText(0, buf.getLength())));
            } finally {
                buf.readUnlock();
            }
            return source;
        } else if (newSystemId.startsWith("file:") || newSystemId.startsWith("jeditresource:")) {
            InputSource source = new InputSource(systemId);
            source.setByteStream(new URL(newSystemId).openStream());
            return source;
        } else if (!network) return null; else {
            final String _newSystemId = newSystemId;
            final VFS vfs = VFSManager.getVFSForPath(_newSystemId);
            final Object[] session = new Object[1];
            Runnable run = new Runnable() {

                public void run() {
                    View view = jEdit.getActiveView();
                    if (!cache || showDownloadResourceDialog(view, _newSystemId)) {
                        session[0] = vfs.createVFSSession(_newSystemId, view);
                    }
                }
            };
            if (SwingUtilities.isEventDispatchThread()) run.run(); else {
                try {
                    SwingUtilities.invokeAndWait(run);
                } catch (Exception e) {
                    Log.log(Log.ERROR, CatalogManager.class, e);
                }
            }
            if (session[0] != null) {
                InputSource source = new InputSource(systemId);
                if (cache) {
                    File file;
                    try {
                        file = copyToLocalFile(session[0], vfs, newSystemId);
                    } finally {
                        vfs._endVFSSession(session, null);
                    }
                    addUserResource(publicId, systemId, file.toURL().toString());
                    source.setByteStream(new FileInputStream(file));
                } else source.setByteStream(vfs._createInputStream(session, newSystemId, false, null));
                return source;
            } else throw new IOException(jEdit.getProperty("xml.network-error"));
        }
    }

    public static boolean isLocal(Entry e) {
        if (e == null || jEdit.getSettingsDirectory() == null) return false;
        try {
            URL url = new File(jEdit.getSettingsDirectory()).toURL();
            String fileUrl = (String) resourceCache.get(e);
            return fileUrl.startsWith(url.toString());
        } catch (MalformedURLException ex) {
            return false;
        }
    }

    public static void propertiesChanged() {
        if (jEdit.getSettingsDirectory() == null) {
            cache = false;
        } else {
            resourceDir = MiscUtilities.constructPath(jEdit.getSettingsDirectory(), "dtds");
            cache = jEdit.getBooleanProperty("xml.cache");
        }
        network = jEdit.getBooleanProperty("xml.network");
        if (!cache) clearCache();
        loadedCatalogs = false;
    }

    public static void save() {
        if (loadedCache) {
            int systemCount = 0;
            int publicCount = 0;
            Iterator keys = resourceCache.keySet().iterator();
            while (keys.hasNext()) {
                Entry entry = (Entry) keys.next();
                Object uri = resourceCache.get(entry);
                if (uri == IGNORE) continue;
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

    public static void clearCache() {
        load();
        Iterator files = resourceCache.values().iterator();
        while (files.hasNext()) {
            Object obj = files.next();
            if (obj instanceof String) {
                String file = (String) XmlPlugin.uriToFile((String) obj);
                Log.log(Log.NOTICE, CatalogManager.class, "Deleting " + file);
                new File(file).delete();
            }
        }
        resourceCache.clear();
    }

    public static void reloadCatalogs() {
        loadedCatalogs = false;
    }

    static void init() {
        EditBus.addToBus(vfsUpdateHandler = new VFSUpdateHandler());
    }

    static void uninit() {
        EditBus.removeFromBus(vfsUpdateHandler);
    }

    private static boolean loadedCache;

    private static boolean loadedCatalogs;

    private static boolean cache;

    private static boolean network;

    private static Catalog catalog;

    private static Set catalogFiles;

    private static HashMap resourceCache;

    private static HashMap reverseResourceCache;

    private static String resourceDir;

    private static Object IGNORE = new Object();

    private static EBComponent vfsUpdateHandler;

    /**
	 * Don't want this public because then invoking {@link clearCache()}
	 * will remove this file, not what you would expect!
	 */
    private static void addUserResource(String publicId, String systemId, String url) {
        if (publicId != null) {
            Entry pe = new Entry(Entry.PUBLIC, publicId, url);
            resourceCache.put(pe, url);
        }
        Entry se = new Entry(Entry.SYSTEM, systemId, url);
        resourceCache.put(se, url);
        reverseResourceCache.put(url, se);
    }

    private static File copyToLocalFile(Object session, VFS vfs, String path) throws IOException {
        if (jEdit.getSettingsDirectory() == null) return null;
        String userDir = jEdit.getSettingsDirectory();
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

    private static String resolvePublic(String systemId, String publicId) throws Exception {
        Entry e = new Entry(Entry.PUBLIC, publicId, null);
        String uri = (String) resourceCache.get(e);
        if (uri == null) return catalog.resolvePublic(publicId, null); else if (uri == IGNORE) return null; else return uri;
    }

    private static String resolveSystem(String id) throws Exception {
        Entry e = new Entry(Entry.SYSTEM, id, null);
        String uri = (String) resourceCache.get(e);
        if (uri == null) return catalog.resolveSystem(id); else if (uri == IGNORE) return null; else return uri;
    }

    private static boolean showDownloadResourceDialog(Component comp, String systemId) {
        Entry e = new Entry(Entry.SYSTEM, systemId, null);
        if (resourceCache.get(e) == IGNORE) return false;
        int result = GUIUtilities.confirm(comp, "xml.download-resource", new String[] { systemId }, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) return true; else {
            resourceCache.put(e, IGNORE);
            return false;
        }
    }

    private static synchronized void load() {
        if (!loadedCache) {
            loadedCache = true;
            resourceCache = new HashMap();
            reverseResourceCache = new HashMap();
            int i;
            String id, prop, uri;
            i = 0;
            while ((id = jEdit.getProperty(prop = "xml.cache" + ".public-id." + i++)) != null) {
                try {
                    uri = jEdit.getProperty(prop + ".uri");
                    resourceCache.put(new Entry(Entry.PUBLIC, id, uri), uri);
                } catch (Exception ex2) {
                    Log.log(Log.ERROR, CatalogManager.class, ex2);
                }
            }
            i = 0;
            while ((id = jEdit.getProperty(prop = "xml.cache" + ".system-id." + i++)) != null) {
                try {
                    uri = jEdit.getProperty(prop + ".uri");
                    Entry se = new Entry(Entry.SYSTEM, id, uri);
                    resourceCache.put(se, uri);
                    reverseResourceCache.put(uri, se);
                } catch (Exception ex2) {
                    Log.log(Log.ERROR, CatalogManager.class, ex2);
                }
            }
        }
        if (!loadedCatalogs) {
            loadedCatalogs = true;
            catalog = new Catalog();
            catalogFiles = new HashSet();
            catalog.setupReaders();
            try {
                catalog.loadSystemCatalogs();
                catalog.parseCatalog("jeditresource:XML.jar!/xml/dtds/catalog");
                int i = 0;
                String prop, uri;
                while ((uri = jEdit.getProperty(prop = "xml.catalog." + i++)) != null) {
                    Log.log(Log.MESSAGE, CatalogManager.class, "Loading catalog: " + uri);
                    try {
                        if (MiscUtilities.isURL(uri)) catalogFiles.add(uri); else {
                            catalogFiles.add(MiscUtilities.resolveSymlinks(uri));
                        }
                        catalog.parseCatalog(uri);
                    } catch (Exception ex2) {
                        Log.log(Log.ERROR, CatalogManager.class, ex2);
                    }
                }
            } catch (Exception ex1) {
                Log.log(Log.ERROR, CatalogManager.class, ex1);
            }
        }
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
    }

    /**
	 * Reloads a catalog file when the user changes it on disk.
	 */
    public static class VFSUpdateHandler implements EBComponent {

        public void handleMessage(EBMessage msg) {
            if (!loadedCatalogs) return;
            if (msg instanceof VFSUpdate) {
                String path = ((VFSUpdate) msg).getPath();
                if (catalogFiles.contains(path)) loadedCatalogs = false;
            }
        }
    }
}
