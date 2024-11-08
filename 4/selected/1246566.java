package org.ibex.js;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.ibex.net.HTTPFactory;
import org.ibex.net.HTTP.HTTPEntityInfo;
import org.ibex.net.HTTP.HTTPResponse;
import org.ibex.util.Cache;
import org.ibex.util.Callable;
import org.ibex.util.IOUtil;
import org.ibex.util.Logger;

/**
 *   Essentially an InputStream "factory".  You can repeatedly ask a
 *   Fountain for an InputStream, and each InputStream you get back will
 *   be totally independent of the others (ie separate stream position
 *   and state) although they draw from the same data source.
 *   
 *   NOTES
 *   coerceToString for at least File and HTTP returns a canonical url, 
 *   used to construct filename when cached ...
 *   MAYBE WRONG. Should the coerce be unique key, so we should use some
 *   other method for the toString
 */
public abstract class Fountain extends JS.Obj implements JS.Cloneable, Constants {

    public static class NotCacheableException extends Exception {
    }

    private Cache getCache = new Cache(100);

    private Scheduler scheduler;

    public final JS get(JS key) throws JSExn {
        JS ret = (JS) getCache.get(key);
        if (ret == null) getCache.put(key, ret = _get(key));
        return ret;
    }

    /** For delegating to fountain type to get child */
    public JS _get(JS key) throws JSExn {
        return null;
    }

    Trap wtrap(JS key) throws JSExn {
        Trap r = getTrap(key);
        return r == null ? null : r.findWrite();
    }

    public final InputStream getInputStream() throws IOException {
        return getInputStream(this);
    }

    public final InputStream getInputStream(final Fountain principal) throws IOException {
        try {
            InputStream is = _getInputStream();
            if (is == null) return is;
            Trap startTrap = principal.wtrap(SC_start);
            final Trap progressTrap = principal.wtrap(SC_progress);
            final Trap finishTrap = principal.wtrap(SC_finish);
            if (startTrap == null && progressTrap == null && finishTrap == null) return is;
            final Scheduler sched = principal.scheduler;
            JS info = getInfo();
            int length = JSU.toInt(info.get(SC_length));
            final int callbackRate = Math.max(8192, length / 100);
            if (startTrap != null) {
                sched.scheduleJustTriggerTraps(principal, SC_start, info);
            }
            return new FilterInputStream(is) {

                private int bytesDownloaded = 0;

                private int bytesSinceCallback = 0;

                public int read() throws IOException {
                    int ret = super.read();
                    if (ret != -1) triggerProgress(1); else triggerFinish();
                    return ret;
                }

                public int read(byte[] b, int off, int len) throws IOException {
                    int ret = super.read(b, off, len);
                    if (ret != -1) triggerProgress(ret); else triggerFinish();
                    return ret;
                }

                private void triggerProgress(int n) {
                    bytesDownloaded += n;
                    bytesSinceCallback += n;
                    if (bytesSinceCallback > callbackRate) {
                        bytesSinceCallback -= callbackRate;
                        if (progressTrap != null) sched.scheduleJustTriggerTraps(principal, SC_progress, JSU.N(bytesDownloaded));
                    }
                }

                private void triggerFinish() {
                    if (finishTrap != null) sched.scheduleJustTriggerTraps(principal, SC_finish, JSU.T);
                }
            };
        } catch (JSExn e) {
            throw new IOException(e.getMessage());
        }
    }

    protected abstract InputStream _getInputStream() throws IOException;

    public OutputStream getOutputStream() throws IOException {
        throw new IOException(toString() + " is read only");
    }

    public void addTrap(JS key, JS f) throws JSExn {
        super.addTrap(key, f);
        if (scheduler == null) scheduler = Scheduler.findCurrent();
    }

    public void cache(Fountain principal) throws IOException {
    }

    ;

    public JS getInfo() throws IOException {
        return new JS.Obj();
    }

    public JS callMethod(JS this_, JS method, JS[] args) throws JSExn {
        if ("info".equals(JSU.toString(method))) {
            try {
                final Scheduler sched = Scheduler.findCurrent();
                final Callable callback = sched.pauseJSThread();
                new java.lang.Thread() {

                    public void run() {
                        try {
                            sched.schedule(callback, getInfo());
                        } catch (Exception e) {
                            JSExn jsexn = (e instanceof JSExn) ? (JSExn) e : new JSExn(e.getMessage());
                            sched.schedule(callback, jsexn);
                        }
                    }
                }.start();
                return null;
            } catch (JSExn e) {
                throw new JSExn("cannot access a potentially remote resource in the foreground thread");
            }
        }
        return super.callMethod(this_, method, args);
    }

    /** HTTP or HTTPS resource */
    public static class HTTP extends Fountain {

        final Logger logger;

        public final String url;

        public String mimetype = "";

        private InputStream responseStream;

        private HTTPEntityInfo responseInfo;

        public HTTP(Logger logger, String url) {
            this.logger = logger;
            while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
            this.url = url;
        }

        private org.ibex.net.HTTP http() throws IOException {
            return HTTPFactory.create(logger, url);
        }

        public String canonical() {
            return url;
        }

        public JS _get(JS key) throws JSExn {
            return new HTTP(logger, url + "/" + JSU.toString(key));
        }

        private JS readInfo(HTTPEntityInfo info) {
            JS.Obj r = new JS.Obj();
            r.putSafe(SC_lastModified, JSU.S(info.lastModified));
            r.putSafe(SC_length, JSU.N(info.contentLength));
            r.putSafe(SC_mimetype, JSU.S(info.contentType));
            r.putSafe(SC_name, JSU.S(url));
            return r;
        }

        public HTTPEntityInfo getRawInfo() {
            return responseInfo;
        }

        public JS getInfo() throws IOException {
            if (responseInfo == null) {
                HTTPResponse response = http().GET();
                responseStream = response.body;
                responseInfo = response.info;
                ;
            }
            return readInfo(responseInfo);
        }

        public InputStream _getInputStream() throws IOException {
            if (responseStream == null) {
                HTTPResponse response = http().GET();
                responseStream = response.body;
                responseInfo = response.info;
            }
            InputStream r = responseStream;
            responseStream = null;
            return r;
        }

        public OutputStream getOutputStream() throws IOException {
            ByteArrayOutputStream r = new ByteArrayOutputStream() {

                public void close() throws IOException {
                    HTTPResponse response = http().POST(mimetype, toByteArray());
                    responseStream = response.body;
                    responseInfo = response.info;
                }
            };
            return r;
        }

        public JS getResponseInfo() {
            return readInfo(responseInfo);
        }

        public void setRequestInfo(JS info) throws JSExn {
            Enumeration en = info.keys().iterator();
            while (en.hasNext()) {
                String key = JSU.toString(en.next());
                if (!"mimetype".equals(key)) {
                    throw new JSExn("'" + key + "' not supported, only mimetype currently supported in request info");
                }
                mimetype = JSU.toString(info.get(SC_mimetype));
            }
        }
    }

    public static class Resource extends Fountain {

        private final URL res;

        public Resource(URL res) {
            this.res = res;
        }

        public InputStream _getInputStream() throws IOException {
            return res.openStream();
        }

        public String coerceToString() {
            return res.toString();
        }
    }

    /** byte arrays */
    public static class ByteArray extends Fountain {

        private byte[] bytes;

        private int suggested;

        public ByteArray(byte[] bytes) {
            this.bytes = bytes;
            suggested = bytes.length;
        }

        public ByteArray(JS suggested) throws JSExn {
            this.suggested = suggested == null ? 8192 : JSU.toInt(suggested);
        }

        public InputStream _getInputStream() throws IOException {
            return new ByteArrayInputStream(bytes);
        }

        public OutputStream getOutputStream() throws IOException {
            return new ByteArrayOutputStream(suggested) {

                public void close() throws IOException {
                    super.close();
                    bytes = toByteArray();
                }
            };
        }
    }

    /** a file */
    public static class File extends Fountain {

        public final String path;

        private boolean writeable;

        public File(String path) throws JSExn {
            this(path, true);
        }

        public File(String path, boolean writeable) throws JSExn {
            this(new java.io.File(path), writeable);
        }

        public File(java.io.File file, boolean writeable) throws JSExn {
            try {
                this.path = file.getCanonicalPath();
            } catch (IOException e) {
                throw new JSExn(e.getMessage());
            }
            this.writeable = writeable;
        }

        public String canonical() {
            return "file://" + path;
        }

        public JS getInfo() throws IOException {
            java.io.File f = new java.io.File(path);
            JS.Obj r = new JS.Obj();
            r.putSafe(SC_length, JSU.N(f.length()));
            r.putSafe(SC_name, JSU.S(canonical()));
            return r;
        }

        public InputStream _getInputStream() throws IOException {
            try {
                return new FileInputStream(path);
            } catch (FileNotFoundException e) {
                return null;
            }
        }

        public OutputStream getOutputStream() throws IOException {
            if (!writeable) throw new IOException("File readonly: " + coerceToString());
            try {
                return new FileOutputStream(path);
            } catch (FileNotFoundException e) {
                return null;
            }
        }

        public JS _get(JS key) throws JSExn {
            return new File(path + java.io.File.separatorChar + JSU.toString(key));
        }

        public void remove() throws JSExn {
            if (writeable) {
                new java.io.File(path).delete();
            } else throw new JSExn("not writeable " + writeable);
        }

        Set _getKeySet() throws IOException {
            Set s = new HashSet();
            java.io.File f = new java.io.File(path);
            if (f.isDirectory()) {
                String[] list = f.list();
                for (int i = 0; i < list.length; i++) {
                    s.add(list[i]);
                }
            }
            return s;
        }

        public long getTimestamp() {
            return new java.io.File(path).lastModified();
        }
    }

    static final int[] ARGTYPES_parseUTF8 = new int[] { JSU.FOUNTAIN };

    public static JS parseUTF8(JS[] args) throws JSExn {
        JSU.checkArgs(args, ARGTYPES_parseUTF8);
        try {
            InputStream is = JSU.getInputStream(args[0]);
            if (is == null) return SC_;
            try {
                return JSU.S(new String(IOUtil.toByteArray(is)));
            } finally {
                is.close();
            }
        } catch (IOException e) {
            throw JSU.handleFountainExn(e);
        }
    }

    static final int[] ARGTYPES_writeUTF8 = new int[] { JSU.FOUNTAIN, JSU.ANY };

    public static void writeUTF8(JS[] args) throws JSExn {
        JSU.checkArgs(args, ARGTYPES_writeUTF8);
        try {
            OutputStream out = JSU.getOutputStream(args[0]);
            try {
                out.write(JSU.toString(args[1]).getBytes());
            } finally {
                out.close();
            }
        } catch (IOException e) {
            throw JSU.handleFountainExn(e);
        }
    }

    public static final int[] ARGTYPES_afountain = new int[] { JSU.FOUNTAIN };

    public static String nameOf(JS[] args) throws JSExn {
        JSU.checkArgs(args, ARGTYPES_afountain);
        return ((Fountain) args[0]).canonical();
    }

    static final int[] ARGTYPES_2fountains = new int[] { JSU.FOUNTAIN, JSU.FOUNTAIN };

    public static void pipe(final JS[] args) throws JSExn {
        JSU.checkArgs(args, ARGTYPES_2fountains);
        try {
            final Scheduler sched = Scheduler.findCurrent();
            final Callable callback = sched.pauseJSThread();
            new java.lang.Thread() {

                public void run() {
                    try {
                        IOUtil.pipe(((Fountain) args[0]).getInputStream(), ((Fountain) args[1]).getOutputStream());
                        sched.schedule(callback, null);
                    } catch (IOException e) {
                        sched.schedule(callback, JSU.handleFountainExn(e));
                    }
                }
            }.start();
        } catch (JSExn e) {
            throw new JSExn("cannot pipe streams in the foreground thread");
        }
    }

    public static Fountain newZip(Fountain f) throws IOException {
        if (f == null) throw new IOException("Null fountain");
        if (f instanceof File) {
            return new ZipFile(new JarFile(new java.io.File(((File) f).path)));
        }
        return new ZipStream(f);
    }

    public static JS multiple(final JS[] streams) throws JSExn {
        try {
            final Fountain.Multiple multiStream = new Fountain.Multiple(streams.length);
            for (int i = 0; i < streams.length; i++) {
                Fountain cacheable = JSU.getFountain(streams[i]);
                multiStream.addOverrideStream(cacheable);
            }
            return multiStream;
        } catch (Exception e) {
            throw new JSExn(e.getMessage());
        }
    }

    /** Random access and so faster than the ZipStream for large archives */
    public static class ZipFile extends Fountain {

        private final JarFile parent;

        private final String path;

        ZipFile(JarFile parent) {
            this.parent = parent;
            this.path = null;
        }

        private ZipFile(JarFile parent, String path) {
            while (path != null && path.startsWith("/")) path = path.substring(1);
            this.parent = parent;
            this.path = path;
        }

        public JS _get(JS key) throws JSExn {
            return new ZipFile(parent, path == null ? JSU.toString(key) : path + '/' + JSU.toString(key));
        }

        public InputStream _getInputStream() throws IOException {
            ZipEntry entry = parent.getEntry(path);
            if (entry == null) throw new IOException("requested file (" + path + ") not found in archive");
            return parent.getInputStream(entry);
        }

        Set _getKeySet() throws IOException {
            java.util.Enumeration en = parent.entries();
            String path = this.path == null ? "" : this.path;
            Set s = new HashSet();
            while (en.hasMoreElements()) {
                ZipEntry ze = (ZipEntry) en.nextElement();
                String name = ze.getName();
                name = name.replace('\\', '/');
                if (name.startsWith(path)) {
                    if (!name.endsWith("/")) {
                        name = path.length() == 0 ? name : name.substring(path.length() + 1);
                        if (name.indexOf('/') != -1) {
                            name = name.substring(0, name.indexOf('/'));
                        }
                        s.add(name);
                    }
                }
            }
            return s;
        }

        public long getTimestamp() {
            return new java.io.File(parent.getName()).lastModified();
        }
    }

    /** "unwrap" a Zip archive */
    public static class ZipStream extends Fountain {

        private final Fountain parent;

        private final String path;

        ZipStream(Fountain parent) {
            this.parent = parent;
            this.path = null;
        }

        private ZipStream(Fountain parent, String path) {
            while (path != null && path.startsWith("/")) path = path.substring(1);
            this.parent = parent;
            this.path = path;
        }

        public JS _get(JS key) throws JSExn {
            return new ZipStream(parent, path == null ? JSU.toString(key) : path + '/' + JSU.toString(key));
        }

        public InputStream _getInputStream() throws IOException {
            InputStream pis = parent.getInputStream();
            ZipInputStream zis = new ZipInputStream(pis);
            ZipEntry ze = zis.getNextEntry();
            while (ze != null && !ze.getName().equals(path)) ze = zis.getNextEntry();
            if (ze == null) throw new IOException("requested file (" + path + ") not found in archive");
            return zis;
        }

        Set _getKeySet() throws IOException {
            InputStream pis = parent.getInputStream();
            ZipInputStream zis = new ZipInputStream(pis);
            ZipEntry ze = zis.getNextEntry();
            String path = this.path == null ? "" : this.path;
            Set s = new HashSet();
            while (ze != null) {
                String name = ze.getName();
                name = name.replace('\\', '/');
                if (name.startsWith(path)) {
                    if (!name.endsWith("/")) {
                        name = path.length() == 0 ? name : name.substring(path.length() + 1);
                        if (name.indexOf('/') != -1) {
                            name = name.substring(0, name.indexOf('/'));
                        }
                        s.add(name);
                    }
                }
                ze = zis.getNextEntry();
            }
            return s;
        }
    }

    public static class Multiple extends Fountain {

        private boolean cached;

        private Vector fountains = null;

        private String path = "";

        public Multiple(int size) {
            fountains = new Vector(size);
            cached = false;
        }

        public Multiple(Vector parentStreams, JS key, String parentPath) {
            this.cached = true;
            int len = parentStreams.size();
            fountains = new Vector(len);
            try {
                path = parentPath + java.io.File.separatorChar + key.coerceToString();
                for (int i = 0; i < len; i++) {
                    Fountain stream = (Fountain) parentStreams.elementAt(i);
                    if (stream != null) fountains.addElement(stream.get(key));
                }
            } catch (JSExn e) {
                e.printStackTrace();
            }
        }

        public void cache(Fountain principal) throws IOException {
            cached = true;
            for (int i = 0; i < fountains.size(); i++) ((Fountain) fountains.elementAt(i)).cache(principal == null ? this : principal);
        }

        public String canonical() {
            return "multi:" + path;
        }

        public void addOverrideStream(Fountain fountain) {
            if (fountain != null) fountains.insertElementAt(fountain, 0);
        }

        public JS _get(JS key) throws JSExn {
            try {
                if (!cached) cache(this);
            } catch (IOException e) {
                throw new JSExn(e.getMessage());
            }
            return new Multiple(fountains, key, path);
        }

        public InputStream _getInputStream() throws IOException {
            InputStream is;
            for (int i = 0; i < fountains.size(); i++) {
                Fountain stream = (Fountain) fountains.elementAt(i);
                if (stream != null) {
                    try {
                        is = stream.getInputStream(this);
                    } catch (FileNotFoundException e) {
                        is = null;
                    } catch (IOException e) {
                        is = null;
                    }
                    if (is != null) return is;
                }
            }
            return null;
        }

        Set _getKeySet() throws IOException {
            Set s = new HashSet();
            for (int i = fountains.size() - 1; i >= 0; i--) {
                Fountain stream = (Fountain) fountains.elementAt(i);
                try {
                    Set s2 = stream.getKeySet();
                    s.addAll(s2);
                } catch (IOException e) {
                    throw new IOException("Couldn't access substream " + e.getMessage());
                }
            }
            return s;
        }

        public long getTimestamp() {
            long r = Long.MIN_VALUE;
            for (int i = fountains.size() - 1; i >= 0; i--) {
                Fountain stream = (Fountain) fountains.elementAt(i);
                r = Math.max(r, stream.getTimestamp());
            }
            return r;
        }
    }

    public Keys keys() throws JSExn {
        return keys(this);
    }

    public Keys keys(Fountain principal) throws JSExn {
        try {
            final Set s = getKeySet();
            return new Keys(this) {

                public boolean contains(JS key) {
                    return s.contains(JSU.toString(key));
                }

                public Enumeration iterator() throws JSExn {
                    return new Enumeration(null) {

                        private Iterator I = s.iterator();

                        public boolean _hasNext() {
                            return I.hasNext();
                        }

                        public JS _next() throws JSExn {
                            return JSU.S((String) I.next());
                        }
                    };
                }

                public int size() {
                    return s.size();
                }
            };
        } catch (IOException e) {
            throw new JSExn(e.getMessage());
        }
    }

    WeakReference keyCache = null;

    Set getKeySet() throws IOException {
        Set s = (Set) ((keyCache == null) ? null : keyCache.get());
        if (s == null) {
            s = _getKeySet();
        }
        return s;
    }

    Set _getKeySet() throws IOException {
        throw new IOException("Cannot list " + getClass().getName());
    }

    public String toString() {
        return coerceToString();
    }

    public String coerceToString() {
        return canonical() + "$" + Integer.toHexString(hashCode());
    }

    /** for cacheable fountains it should return a value with a 1-1 mapping with the name of the underlying resource*/
    public String canonical() {
        return super.toString();
    }

    public JS type() {
        return SC_stream;
    }

    public long getTimestamp() {
        return -1;
    }
}
