package com.ibm.aglets.tahiti;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Hashtable;
import net.sourceforge.aglets.log.AgletsLogger;
import com.ibm.aglet.message.MessageManager;
import com.ibm.aglets.ResourceManager;
import com.ibm.aglets.thread.AgletThread;
import com.ibm.awb.misc.Archive;
import com.ibm.maf.AgentProfile;
import com.ibm.maf.ClassName;

/**
 * Class <tt>AgletClassLoader<tt> is responsible for loading classes for the
 *  aglets. This class has a class loader cache as a static member and store all
 *  class loader objects with keys which is the URL of the origin of classes
 *  managed by the loader. In aglets bytecodes of classes are transfered with
 *  objects. Therefore, there may be many version of classes whose names are
 *  same in an aglet server. In aglets classes are managed based on their
 *  origin. Classes which sources of their bytecodes are same are managed by a
 *  same aglet loader in an aglet server. Therefore, an object can access
 *  objects if sources of their class bytecodes are same, otherwise
 *  ClassCastException will occur.<p>
 * 
 *  An aglet loader caches classes and their bytecodes.
 * 
 * @author Danny B. Lange
 * @author Gaku Yamamoto
 * @author Mitsuru Oshima
 * @version 1.20 $Date: 2009/07/28 07:04:53 $
 */
class AgletClassLoader extends ClassLoader implements ResourceManager {

    /**
     * Cache to store bytecodes of classes.
     * 
     * @since
     */
    protected static CacheManager _cache = null;

    static AgentProfile _agent_profile = null;

    private static AgletsLogger logger = AgletsLogger.getLogger(AgletClassLoader.class.getName());

    /**
     * Digest table for classes managed by this classloader.
     */
    protected DigestTable _digest_table = new DigestTable();

    private URL _codeBase = null;

    private Certificate _ownerCert = null;

    /**
     * Cache to store resolved classes.
     */
    private Hashtable _resolvedClassCache = new Hashtable();

    private java.util.Vector _resources = new java.util.Vector();

    static {
        _cache = CacheManager.getCacheManager();
        _agent_profile = new AgentProfile((short) 1, (short) 1, "Aglets", (short) 0, (short) 2, (short) 1, null);
    }

    /**
     * Constructs a new AgletClassLoader with codebase.
     * 
     * @param codebase
     *            the codebase in which the all classes are originated.
     * @param owner
     *            Certificate of the owner
     */
    protected AgletClassLoader(URL codebase, Certificate owner) {
        logger.debug("Ctor: [" + codebase + "]");
        this._codeBase = codebase;
        this._ownerCert = owner;
    }

    /**
     * Sets the resourceManagerContext attribute of the AgletClassLoader object
     * 
     * @since
     */
    @Override
    public void setResourceManagerContext() {
    }

    /**
     * Gets the archive attribute of the AgletClassLoader object
     * 
     * @param t
     *            Description of Parameter
     * @return The archive value
     * @since
     */
    @Override
    public Archive getArchive(ClassName[] t) {
        int size = t.length;
        if (this.match(t)) {
            Archive a = new Archive();
            for (int i = 0; i < size; i++) {
                String name = t[i].name;
                byte b[] = this.findByteCodeInCache(name);
                if (b != null) {
                    long d = this._digest_table.getDigest(name);
                    logger.debug("putResource(" + name + "," + d + "," + b.length + ") into archive");
                    a.putResource(name, d, b);
                }
            }
            return a;
        } else {
            System.err.println("getArchive: doesn't match");
            return null;
        }
    }

    /**
     * Computes Digest
     * 
     * @param classes
     *            Description of Parameter
     * @return The classNames value
     * @since
     */
    @Override
    public synchronized ClassName[] getClassNames(Class[] classes) {
        return this._digest_table.getClassNames(classes);
    }

    /**
     * Tells where the class was loaded from.
     * 
     * @return The codeBase value
     * @since
     */
    public URL getCodeBase() {
        return this._codeBase;
    }

    /**
     * Gets certificate of the owner. (replacement of getIdentity)
     * 
     * @return Certificate of the owner
     * @since
     */
    public Certificate getOwnerCertificate() {
        return this._ownerCert;
    }

    /**
     * Gets the resourceAsStream attribute of the AgletClassLoader object
     * 
     * @param filename
     *            Description of Parameter
     * @return The resourceAsStream value
     * @since
     */
    @Override
    public InputStream getResourceAsStream(String filename) {
        byte b[] = this.getResourceAsByteArray(filename);
        if (b != null) {
            return new java.io.ByteArrayInputStream(b);
        }
        return null;
    }

    /**
     * Adds a feature to the Resource attribute of the AgletClassLoader object
     * 
     * @param o
     *            The feature to be added to the Resource attribute
     * @since
     */
    @Override
    public void addResource(Object o) {
        logger.debug("Adding resource.");
        synchronized (this._resources) {
            if (this._resources.contains(o) == false) {
                this._resources.addElement(o);
            }
        }
    }

    /**
     * Description of the Method
     * 
     * @param cls
     *            Description of Parameter
     * @since
     */
    public void cacheResolvedClass(Class cls) {
        this._resolvedClassCache.put(cls.getName(), cls);
    }

    /**
     * Checks if the given class is managed by this manager.
     * 
     * @param cls
     *            Description of Parameter
     * @return Description of the Returned Value
     * @since
     */
    @Override
    public boolean contains(Class cls) {
        return this._resolvedClassCache.contains(cls);
    }

    /**
     * Description of the Method
     * 
     * @since
     */
    @Override
    public void disposeAllResources() {
        synchronized (this._resources) {
            java.util.Enumeration e = this._resources.elements();
            while (e.hasMoreElements()) {
                Object o = e.nextElement();
                if (o instanceof java.awt.Window) {
                    ((java.awt.Window) o).dispose();
                } else {
                }
            }
            this._resources = null;
        }
    }

    /**
     * Shout when an AgletClassLoader object is caught by GC. This method is for
     * verifing whether a class loader becomes a target of GC or not.
     * 
     * @since
     */
    @Override
    public void finalize() {
        logger.debug("Class Loader: Garbage Collected");
        this.disposeAllResources();
        this.releaseCacheEntries();
    }

    /**
     * Description of method.
     * 
     * @param a
     *            Description of Parameter
     * @since
     */
    @Override
    public void importArchive(Archive a) {
        Archive.Entry ae[] = a.entries();
        logger.debug("importArchive()");
        for (int i = 0; i < ae.length; i++) {
            logger.debug("archive[" + i + "].name()=" + ae[i].name());
            logger.debug("archive[" + i + "].digest()=" + ae[i].digest());
            logger.debug("archive[" + i + "].data().length=" + ae[i].data().length);
            this.putResource(ae[i].name(), a.getResourceAsByteArray(ae[i].name()));
        }
    }

    /**
     * Description of the Method
     * 
     * @param table
     *            Description of Parameter
     * @return Description of the Returned Value
     * @since
     */
    public synchronized boolean match(ClassName[] table) {
        return this._digest_table.match(table, false);
    }

    /**
     * Description of the Method
     * 
     * @param table
     *            Description of Parameter
     * @return Description of the Returned Value
     * @since
     */
    public synchronized boolean matchAndImport(ClassName[] table) {
        return this._digest_table.match(table, true);
    }

    /**
     * Description of the Method
     * 
     * @param mm
     *            Description of Parameter
     * @return Description of the Returned Value
     * @since
     */
    @Override
    public AgletThread newAgletThread(MessageManager mm) {
        return null;
    }

    /**
     * Description of the Method
     * 
     * @since
     */
    @Override
    public void resumeAllThreads() {
    }

    /**
     * Description of the Method
     * 
     * @since
     */
    @Override
    public void stopAllThreads() {
    }

    /**
     * Description of the Method
     * 
     * @since
     */
    @Override
    public void stopThreadGroup() {
    }

    /**
     * Description of the Method
     * 
     * @since
     */
    @Override
    public void suspendAllThreads() {
    }

    /**
     * Description of the Method
     * 
     * @return Description of the Returned Value
     * @since
     */
    @Override
    public String toString() {
        String cb = null;
        if (this._codeBase == null) {
            cb = "NOWHERE";
        } else {
            cb = this._codeBase.toString();
        }
        String owner = null;
        if (this._ownerCert == null) {
            owner = "NOBODY";
        } else {
            owner = this._ownerCert.toString();
        }
        return "[AgletClassLoader codebase = " + cb + " owner = " + owner + "]";
    }

    /**
     * Description of the Method
     * 
     * @since
     */
    @Override
    public void unsetResourceManagerContext() {
    }

    /**
     * Gets the resourceAsByteArray attribute of the AgletClassLoader object
     * 
     * @param filename
     *            Description of Parameter
     * @return The resourceAsByteArray value
     * @since
     */
    protected synchronized byte[] getResourceAsByteArray(String filename) {
        long digest = this._digest_table.getDigest(filename);
        byte data[] = null;
        if (digest != 0) {
            data = _cache.getData(filename, digest);
            logger.debug("get '" + filename + "' from cache by getData(" + filename + "," + digest + ")");
        }
        if (data == null) {
            try {
                final String fn = filename;
                data = (byte[]) AccessController.doPrivileged(new PrivilegedAction() {

                    @Override
                    public Object run() {
                        logger.debug("get '" + fn + "' from codebase");
                        byte[] res = AgletClassLoader.this.loadResourceFromCodeBase(fn);
                        return res;
                    }
                });
            } catch (Throwable t) {
                logger.error("Error getting resource: " + t.getMessage());
            }
        }
        if (data != null) {
            this.putResource(filename, data);
        }
        return data;
    }

    /**
     * Description of the Method
     * 
     * @param filename
     *            Description of Parameter
     * @return Description of the Returned Value
     * @since
     */
    protected byte[] findByteCodeInCache(String filename) {
        long d = this._digest_table.getDigest(filename);
        return _cache.getData(filename, d);
    }

    /**
     * Loads a class specified by the param name. If a bytecode of the class has
     * not been loaded, an AgletClassLoader object will load the bytecode form
     * the codebase and define the class. The loaded bytecode and class will be
     * stored into the class data cache and the class cache respectively.
     * 
     * @param name
     *            the name of the desired class.
     * @param resolve
     *            true if the class must be resolved.
     * @return a loaded class
     * @exception ClassNotFoundException
     *                if the class is not found.
     * @since
     */
    @Override
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        logger.debug("loadClass()++ [" + resolve + "]");
        try {
            Class cl = this.findResolvedClass(name);
            if (cl != null) {
                logger.debug("Using class " + name + " in resolved cache");
                return cl;
            }
            cl = this.findClassInternal(name);
            if (cl == null) {
                throw new ClassNotFoundException(name);
            }
            String realName = cl.getName();
            if (!realName.equals(name)) {
                throw new ClassNotFoundException(name);
            }
            if (resolve) {
                if (this._resolvedClassCache.contains(cl)) {
                    logger.debug(name + " was resolved before.");
                    return cl;
                } else {
                    logger.debug("resolving.. " + name);
                }
                boolean success = false;
                try {
                    this.resolveClass(cl);
                    success = true;
                } catch (Exception ex) {
                    throw new ClassNotFoundException("Resolve class: " + ex.toString());
                } finally {
                    if (success) {
                        this.cacheResolvedClass(cl);
                    } else {
                    }
                }
            }
            return cl;
        } catch (SecurityException e) {
            e.printStackTrace();
            Thread.dumpStack();
            throw e;
        }
    }

    /**
     * Description of the Method
     * 
     * @param classname
     *            Description of Parameter
     * @return Description of the Returned Value
     * @since
     */
    private byte[] findByteCode(String classname) {
        return this.getResourceAsByteArray(classname.replace('.', '/') + ".class");
    }

    /**
     * Loads a class
     * 
     * @param name
     *            Description of Parameter
     * @return Description of the Returned Value
     * @exception ClassNotFoundException
     *                Description of Exception
     * @since
     * @see AgletClassLoader#loadClass
     * @see AgletClassLoader#instantiageAglet
     */
    private Class findClassInternal(String name) throws ClassNotFoundException {
        Class clazz = null;
        try {
            clazz = this.findSystemClass(name);
            if (clazz != null) {
                logger.debug("Loading " + name + " from System");
                return clazz;
            }
        } catch (ClassNotFoundException ex) {
        }
        clazz = this.findLoadedClass(name);
        if (clazz != null) {
            logger.debug("Using class " + name + " in cache");
            return clazz;
        }
        clazz = this.loadClassFromCodeBase(name);
        if (clazz != null) {
            logger.debug("Loading class " + name + " from CodeBase");
        }
        return clazz;
    }

    /**
     * Gets the class specified by the name from resolved cache.
     * 
     * @param classname
     *            Description of Parameter
     * @return the class of the name, null if the class is not in the cache.
     * @since
     */
    private Class findResolvedClass(String classname) {
        return (Class) this._resolvedClassCache.get(classname);
    }

    /**
     * Description of the Method
     * 
     * @param classname
     *            Description of Parameter
     * @return Description of the Returned Value
     * @exception ClassNotFoundException
     *                Description of Exception
     * @since
     */
    private Class loadClassFromCodeBase(String classname) throws ClassNotFoundException {
        logger.debug("loadClassFromCodeBase(" + classname + ")");
        byte[] bytecode = this.findByteCode(classname);
        if (bytecode == null) {
            throw new ClassNotFoundException(classname);
        }
        logger.debug("findByteCode(" + classname + ") returns bytecode (" + bytecode.length + "bytes)");
        try {
            logger.debug("define class " + classname);
            Certificate[] certs = null;
            if (this._ownerCert != null) {
                certs = new Certificate[1];
                certs[0] = this._ownerCert;
            }
            final CodeSource cs = new CodeSource(this._codeBase, certs);
            ProtectionDomain pd = (ProtectionDomain) AccessController.doPrivileged(new PrivilegedAction() {

                @Override
                public Object run() {
                    Policy policy = Policy.getPolicy();
                    PermissionCollection perms = policy.getPermissions(cs);
                    return new ProtectionDomain(cs, perms);
                }
            });
            Class clazz = this.defineClass(classname, bytecode, 0, bytecode.length, pd);
            if (clazz.getName().equals(classname) == false) {
                throw new ClassNotFoundException(classname);
            }
            this.cacheResolvedClass(clazz);
            return clazz;
        } catch (ClassFormatError e) {
            e.printStackTrace();
            System.err.println("When loading " + classname + " from " + this._codeBase + " : " + e.getClass().getName() + e.getMessage());
            throw new ClassNotFoundException("When loading " + classname + " from " + this._codeBase + " : " + e.getClass().getName() + e.getMessage());
        }
    }

    /**
     * The method for loading class data. This loads the bytecode from codebase
     * of this loader.
     * 
     * @param name
     *            the class name.
     * @return the bytecode for the class.
     * @since
     */
    private byte[] loadResourceFromCodeBase(String name) {
        byte[] bytecode;
        InputStream is = null;
        logger.debug("LoadResourceFromCodeBase()++");
        try {
            URL url = new URL(this._codeBase, name);
            int content_length = -1;
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("user-agent", "Aglets/1.1");
            connection.setRequestProperty("agent-system", "aglets");
            connection.setRequestProperty("agent-language", "java");
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.connect();
            is = connection.getInputStream();
            content_length = connection.getContentLength();
            if (content_length < 0) {
                content_length = is.available();
            }
            if (content_length == 0) {
                return null;
            }
            bytecode = new byte[content_length];
            int offset = 0;
            while (content_length > 0) {
                int read = is.read(bytecode, offset, content_length);
                offset += read;
                content_length -= read;
            }
            is.close();
        } catch (IOException ex) {
            logger.error("Error loading [" + name + "] resource from [" + this._codeBase + "]", ex);
            bytecode = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ex) {
                    logger.error("Error closing.", ex);
                }
            }
        }
        logger.debug("LoadResourceFromCodeBase()--");
        return bytecode;
    }

    /**
     * Description of the Method
     * 
     * @param name
     *            Description of Parameter
     * @param data
     *            Description of Parameter
     * @since
     */
    private void putResource(String name, byte[] data) {
        long digest = this._digest_table.getDigest(name);
        if (digest == 0) {
            digest = this._digest_table.setData(name, data);
            logger.debug("digest of " + name + " = " + digest);
            _cache.putData(name, digest, data, true);
        } else {
            _cache.putData(name, digest, data, false);
        }
    }

    /**
     * Description of the Method
     * 
     * @since
     */
    private void releaseCacheEntries() {
        synchronized (this._digest_table) {
            for (int i = 0; i < this._digest_table.size(); i++) {
                String name = this._digest_table.getName(i);
                long digest = this._digest_table.getDigest(i);
                _cache.releaseData(name, digest);
            }
        }
    }
}
