package com.memoire.fu;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.StringTokenizer;

/**
 * Very unstable and buggy. Don't use.
 */
public class FuClassLoaderDedicated extends ClassLoader {

    private static final FuHashtableFast global_ = new FuHashtableFast(501);

    private FuHashtableFast local_ = new FuHashtableFast(11);

    private Class only_;

    public FuClassLoaderDedicated() {
        only_ = null;
    }

    public final synchronized Class get(String _name) {
        String s = _name.intern();
        Class r = (Class) global_.get(s);
        if (r == null) r = (Class) local_.get(s);
        return r;
    }

    public final synchronized void putLocal(Class _class) {
        local_.put(_class.getName().intern(), _class);
    }

    public static final synchronized void putGlobal(Class _class) {
        global_.put(_class.getName().intern(), _class);
    }

    public final synchronized Class[] list() {
        Class[] r = new Class[local_.size()];
        Enumeration e = local_.elements();
        int i = 0;
        while (e.hasMoreElements()) {
            r[i] = (Class) e.nextElement();
            i++;
        }
        return r;
    }

    public Class loadClass(String _name) throws ClassNotFoundException {
        return loadClass(_name, true);
    }

    public Class loadClass(String _name, boolean _resolve) throws ClassNotFoundException {
        Class r = get(_name);
        if (r == null) {
            if (!_name.startsWith("java")) {
                try {
                    String n = _name.replace('.', '/') + ".class";
                    InputStream in = null;
                    String classpath = System.getProperty("java.class.path");
                    StringTokenizer st = new StringTokenizer(classpath, ":");
                    while (st.hasMoreTokens()) {
                        String t = st.nextToken();
                        if (in == null) {
                            try {
                                File d = new File(t);
                                if (d.isDirectory()) {
                                    File f = new File(d, n);
                                    if (f.exists()) {
                                        in = new FileInputStream(f);
                                    }
                                }
                            } catch (Exception ex) {
                            }
                        }
                    }
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    if (in != null) while (in.available() > 0) out.write(in.read());
                    byte[] data = out.toByteArray();
                    r = defineClass(_name, data, 0, data.length);
                    if (only_ == null) only_ = r;
                    putLocal(r);
                    if (_resolve) resolveClass(r);
                } catch (Throwable th) {
                    r = null;
                }
            }
        }
        if (r == null) {
            r = Class.forName(_name);
            putGlobal(r);
        }
        return r;
    }

    public String toString() {
        return "FuClassLoaderDedicated(" + (only_ != null ? only_.getName() : "null") + ")";
    }

    public static final Class load(String _name, Class[] _exclude) throws ClassNotFoundException {
        FuClassLoaderDedicated cl = new FuClassLoaderDedicated();
        for (int i = 0; i < _exclude.length; i++) cl.putLocal(_exclude[i]);
        return cl.loadClass(_name);
    }

    public static final Class load(String _name, Class _exclude) throws ClassNotFoundException {
        return load(_name, new Class[] { _exclude });
    }

    public static final Class load(String _name) throws ClassNotFoundException {
        return load(_name, new Class[] {});
    }
}
