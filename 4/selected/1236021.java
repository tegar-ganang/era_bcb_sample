package org.vexi.shoehorn3;

import java.applet.*;
import java.util.*;
import java.lang.reflect.*;
import java.io.*;
import java.net.*;
import java.util.zip.*;
import java.awt.*;
import java.awt.font.*;
import java.math.*;
import org.ibex.crypto.*;

/** This class is Vexi's presence on the user's computer; it must be run as trusted code */
public class ShoeHorn extends Applet {

    private static final String defaultUrl = "http://localhost/Emanate/";

    private String build = null;

    private String path = null;

    private String coreurl = null;

    private String core = null;

    private boolean applog = false;

    public ShoeHorn() {
        log("*** constructor invoked for " + this.getClass().getName());
    }

    public final String getParameter(String arg) {
        return super.getParameter(arg);
    }

    public final void main(String[] s) {
        new ShoeHorn().start();
    }

    private static void log(String s) {
        System.out.println(s);
    }

    /** this just ensures that we are running with full privileges */
    public final void start() {
        build = getParameter("build");
        coreurl = getParameter("coreurl");
        core = getParameter("core");
        path = getParameter("path");
        if (getParameter("log") != null) applog = true;
        new Thread() {

            public void run() {
                log("ShoeHorn thread spawned");
                try {
                    if (System.getProperty("java.vendor", "").startsWith("Netscape")) {
                        log("Detected Navigator 4.x");
                        Method m = Class.forName("netscape.security.PrivilegeManager").getMethod("enablePrivilege", new Class[] { String.class });
                        m.invoke(null, new Object[] { "MarimbaInternalTarget" });
                        m.invoke(null, new Object[] { "UniversalExecAccess" });
                        m.invoke(null, new Object[] { "UniversalPropertyRead" });
                        go();
                    } else if (System.getProperty("java.vendor", "").startsWith("Microsoft")) {
                        Class permissionIdClass = Class.forName("com.ms.security.PermissionID");
                        Object o = permissionIdClass.getField("SYSTEM").get(null);
                        Method m = Class.forName("com.ms.security.PolicyEngine").getMethod("assertPermission", new Class[] { permissionIdClass });
                        m.invoke(null, new Object[] { o });
                        go();
                    } else {
                        log("Detected non-Navigator JVM");
                        Method m = Class.forName("org.vexi.shoehorn3.ShoeHorn$Java12").getMethod("run", new Class[] { Object.class });
                        m.invoke(null, new Object[] { ShoeHorn.this });
                    }
                } catch (Throwable e) {
                    if (e instanceof InvocationTargetException) e = ((InvocationTargetException) e).getTargetException();
                    e.printStackTrace();
                    update(-1.0, "Error; please check the Java console");
                }
            }
        }.start();
    }

    /** ask Java Plugin for privs */
    private static class Java12 {

        public static void run(final Object a) {
            java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {

                public Object run() {
                    ((ShoeHorn) a).go();
                    return null;
                }
            });
        }
    }

    /** inserts the required entries into the user's ~/.java.policy */
    private void modifyPolicyFile() throws IOException {
        log("Adjusting ~/.java.policy");
        File policy = new File(System.getProperty("user.home") + File.separatorChar + ".java.policy");
        if (policy.exists()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(policy)));
            String s = null;
            while ((s = br.readLine()) != null) if (s.startsWith("// VEXI_MARKER:")) {
                log("Java policy file has already been adjusted");
                return;
            }
        }
        FileOutputStream fos = new FileOutputStream(policy.getAbsolutePath(), true);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos));
        pw.println("");
        pw.println("// VEXI_MARKER: this line and the following two grant blocks are required for VEXI; DO NOT REMOVE THEM.");
        pw.println("grant {");
        pw.println("    permission java.io.FilePermission \"${user.home}${/}.vexi${/}shoehorn.jar\", \"read\";");
        pw.println("};");
        pw.println("grant codebase \"file:${user.home}${/}.vexi${/}shoehorn.jar\" {");
        pw.println("    permission java.security.AllPermission;");
        pw.println("};");
        pw.println("// END_VEXI_MARKER");
        pw.println("");
        pw.flush();
        pw.close();
    }

    /** read ourselves out of the resources and write a jarfile to some trusted place */
    private void implantSelf() throws IOException {
        InputStream manifest = getClass().getClassLoader().getResourceAsStream("META-INF/manifest.mf");
        log("my classloader is " + getClass().getClassLoader().getClass().getName());
        ClassLoader loader = getClass().getClassLoader();
        if (manifest == null || loader == null || (loader.getClass().getName().indexOf("Applet") == -1 && loader.getClass().getName().indexOf("Plugin") == -1)) return;
        BufferedReader br = new BufferedReader(new InputStreamReader(manifest));
        Vector entries = new Vector();
        String s = null;
        while ((s = br.readLine()) != null) if (s.startsWith("Name: ")) entries.addElement(s.substring(6));
        String ext_dirs = System.getProperty("java.ext.dirs");
        log("java.ext.dirs = " + ext_dirs);
        ext_dirs = ext_dirs + File.pathSeparatorChar + System.getProperty("user.home") + File.separatorChar + ".vexi";
        StringTokenizer st = new StringTokenizer(ext_dirs, File.pathSeparatorChar + "");
        while (st.hasMoreTokens()) {
            String dir = st.nextToken();
            new File(dir).mkdirs();
            try {
                if (!st.hasMoreTokens()) modifyPolicyFile();
                implantInDirectory(dir, entries);
                return;
            } catch (IOException e) {
                log("Failed to implant in " + dir + " due to " + e);
            }
        }
        log("Failed to implant self!");
    }

    private void implantInDirectory(String dir, Vector entries) throws IOException {
        File f = new File(dir + File.separatorChar + "shoehorn.tmp");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(f));
        for (int i = 0; i < entries.size(); i++) {
            zos.putNextEntry(new ZipEntry(entries.elementAt(i).toString()));
            InputStream is = this.getClass().getClassLoader().getResourceAsStream(entries.elementAt(i).toString());
            byte[] buf = new byte[1024];
            while (true) {
                int read = is.read(buf, 0, buf.length);
                if (read == -1) break;
                zos.write(buf, 0, read);
            }
            is.close();
        }
        zos.close();
        f.renameTo(new File(dir + File.separatorChar + "launcher.jar"));
        log("Succeeded in implanting in " + dir);
    }

    public final void go() {
        try {
            update(0.0, "");
            implantSelf();
            File file;
            String os_name = System.getProperty("os.name", "").toLowerCase();
            log("os.name == " + os_name);
            Vector command = new Vector();
            String arch = null;
            if (os_name.indexOf("linux") != -1) {
                arch = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec("/bin/uname -m").getInputStream())).readLine();
                log("arch is " + arch);
            }
            if (coreurl != null && core != null) file = fetch(coreurl, core); else file = fetch(defaultUrl, "vexi-" + build + ".jar");
            command.addElement(findJvmBinary());
            if (getParameter("mem") != null) {
                command.addElement("-Xmx" + getParameter("mem"));
            }
            command.addElement("-jar");
            command.addElement(file.getAbsolutePath());
            if ("true".equals(getParameter("showrenders"))) command.addElement("-s");
            if ("true".equals(getParameter("verbose"))) command.addElement("-v");
            if (getParameter("log") != null) {
                command.addElement("-l");
                command.addElement(System.getProperty("user.home") + File.separatorChar + ".vexi" + File.separatorChar + getParameter("log"));
            }
            if (getParameter("logrpc") != null) {
                command.addElement("-l");
                command.addElement("rpc");
            }
            for (int i = 0; i < 10; i++) {
                if (getParameter("vexi" + i) != null) {
                    command.addElement(getParameter("vexi" + i));
                }
            }
            for (int i = 0; i < 10; i++) {
                if (getParameter("param" + i) != null) {
                    command.addElement(getParameter("param" + i));
                }
            }
            spawn(command);
        } catch (Exception e) {
            update(-1.0, "Error; please check the Java console");
            e.printStackTrace();
        }
    }

    /** searches for the JVM binary in the usual places */
    private String findJvmBinary() throws IOException {
        String jvmBinary = null;
        String os_name = System.getProperty("os.name", "").toLowerCase();
        String javaHome = null;
        if (new File("/System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Commands/java").exists()) jvmBinary = "/System/Library/Frameworks/JavaVM.framework/Versions/1.3.1/Commands/java";
        if (os_name.indexOf("windows 9") != -1 || os_name.indexOf("windows me") != -1) {
            javaHome = System.getProperty("java.home");
            if (jvmBinary == null && javaHome != null && !javaHome.equals("")) {
                jvmBinary = javaHome + File.separatorChar + "bin" + File.separatorChar + "java.exe";
                if (!new File(jvmBinary).exists()) jvmBinary = null;
            }
        }
        if (jvmBinary == null) {
            javaHome = getEnv("JAVA_HOME");
            if (javaHome != null && !javaHome.equals("")) {
                jvmBinary = javaHome + File.separatorChar + "bin" + File.separatorChar + "java";
                if (os_name.indexOf("windows") != -1) {
                    jvmBinary += ".exe";
                }
                if (!new File(jvmBinary).exists()) jvmBinary = null;
            }
        }
        if (jvmBinary == null) for (int i = 0; i < commonJavaLocations.length; i++) if (new File(commonJavaLocations[i]).exists()) {
            jvmBinary = commonJavaLocations[i];
            break;
        }
        if (jvmBinary == null) {
            String path = getEnv("PATH");
            if (path == null) {
                path = getEnv("Path");
            }
            StringTokenizer st = new StringTokenizer(path, File.pathSeparatorChar + "");
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                if (new File(s + File.separatorChar + "java").exists() || new File(s + File.separatorChar + "java.exe").exists()) {
                    jvmBinary = s + File.separatorChar + "java";
                    if (os_name.indexOf("windows") != -1) {
                        jvmBinary += ".exe";
                    }
                    break;
                }
            }
        }
        if (jvmBinary == null) {
            javaHome = System.getProperty("java.home");
            if (javaHome != null && !javaHome.equals("")) {
                jvmBinary = javaHome + File.separatorChar + "bin" + File.separatorChar + "java";
                if (os_name.indexOf("windows") != -1) {
                    jvmBinary += ".exe";
                }
                if (!new File(jvmBinary).exists()) jvmBinary = null;
            }
        }
        if (jvmBinary == null) throw new Error("couldn't find JVM binary! JAVA_HOME=" + getEnv("JAVA_HOME") + " PATH=" + getEnv("PATH")); else return jvmBinary;
    }

    private void spawn(Vector command) throws IOException {
        String proxy = detectProxy();
        log("proxy settings: " + proxy);
        String os = System.getProperty("os.name").toLowerCase();
        String[] command_vec;
        if (os.indexOf("windows 9") != -1 || os.indexOf("windows me") != -1) {
            command.insertElementAt("command.com", 0);
            command.insertElementAt("/c", 1);
        }
        command.copyInto(command_vec = new String[command.size()]);
        log("executing:");
        for (int i = 0; i < command_vec.length; i++) log("    \"" + command_vec[i] + "\"");
        Process p;
        if (proxy == null) {
            p = Runtime.getRuntime().exec(command_vec);
        } else if (os.indexOf("windows") != -1) {
            p = Runtime.getRuntime().exec(command_vec, new String[] { proxy });
        } else {
            p = Runtime.getRuntime().exec(command_vec, dumpEnv(proxy));
        }
        BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        if (os.indexOf("windows 9") != -1 || os.indexOf("windows me") != -1 || applog) {
            update(1.0, "Vexi Loaded");
        } else {
            String s = stderr.readLine();
            update(1.0, "Vexi Loaded");
            while (s != null) {
                log(s);
                s = stderr.readLine();
            }
        }
        log("exiting...");
    }

    /** Voodoo to extract the proxy settings from Sun's Java Plugin */
    private String detectProxy() {
        try {
            Vector ret = new Vector();
            Class PluginProxyHandler = Class.forName("sun.plugin.protocol.PluginProxyHandler");
            Method getDefaultProxyHandler = PluginProxyHandler.getMethod("getDefaultProxyHandler", new Class[] {});
            Object proxyHandler = getDefaultProxyHandler.invoke(null, new Object[] {});
            Class ProxyHandler = Class.forName("sun.plugin.protocol.ProxyHandler");
            Method getProxyInfo = ProxyHandler.getMethod("getProxyInfo", new Class[] { URL.class });
            Object proxyInfo = getProxyInfo.invoke(proxyHandler, new Object[] { new URL("http://localhost/Emanate/") });
            Class ProxyInfo = Class.forName("sun.plugin.protocol.ProxyInfo");
            if (((Boolean) ProxyInfo.getMethod("isProxyUsed", new Class[] {}).invoke(proxyInfo, new Object[] {})).booleanValue()) return "http_proxy=" + (String) ProxyInfo.getMethod("getProxy", new Class[] {}).invoke(proxyInfo, new Object[] {}) + ":" + ((Integer) ProxyInfo.getMethod("getPort", new Class[] {}).invoke(proxyInfo, new Object[] {})).intValue();
            if (((Boolean) ProxyInfo.getMethod("isSocksUsed", new Class[] {}).invoke(proxyInfo, new Object[] {})).booleanValue()) return "socks_proxy=" + (String) ProxyInfo.getMethod("getSocksProxy", new Class[] {}).invoke(proxyInfo, new Object[] {}) + ":" + ((Integer) ProxyInfo.getMethod("getSocksPort", new Class[] {}).invoke(proxyInfo, new Object[] {})).intValue();
            return null;
        } catch (Throwable e) {
            log("exception while querying sun.plugin.protocol.PluginProxyHandler: " + e);
            return null;
        }
    }

    private void safeWaitFor(final Process p) {
        final Object o = new Object();
        new Thread() {

            public void run() {
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                }
                synchronized (o) {
                    o.notify();
                }
            }
        }.start();
        try {
            synchronized (o) {
                o.wait(2000);
            }
        } catch (InterruptedException e) {
        }
    }

    /** fetches a file from the distribution site, writing it to the appropriate place */
    private File fetch(String urlname, String filename) throws IOException {
        String tmpdir = System.getProperty("user.home") + File.separatorChar + ".vexi";
        new File(tmpdir).mkdirs();
        URL u = new URL(urlname + filename);
        if (filename.endsWith(".gz")) filename = filename.substring(0, filename.length() - 3);
        if (filename.endsWith(".cab")) filename = filename.substring(0, filename.length() - 4) + ".exe";
        File target = new File(tmpdir + File.separatorChar + filename);
        if (target.exists()) return target;
        loadFromURL(u, target, filename);
        return target;
    }

    /** loads a file from a url, verifying that it was properly signed */
    private void loadFromURL(URL u, File target, String filename) throws IOException {
        final URLConnection uc = u.openConnection();
        InputStream is = uc.getInputStream();
        final int contentLength = uc.getContentLength();
        InputStream dis = new FilterInputStream(is) {

            int total = 0;

            public int read() throws IOException {
                int ret = super.read();
                if (ret != -1) total++;
                double loaded = ((double) total) / ((double) uc.getContentLength());
                update(loaded, "Loading Vexi: " + ((int) Math.ceil(loaded * 100)) + "%");
                return ret;
            }

            public int read(byte[] buf, int off, int len) throws IOException {
                int ret = super.read(buf, off, len);
                if (ret != -1) total += ret;
                double loaded = ((double) total) / ((double) uc.getContentLength());
                update(loaded, "Loading Vexi: " + ((int) Math.ceil(loaded * 100)) + "%");
                return ret;
            }
        };
        if (u.toString().endsWith(".gz")) dis = new GZIPInputStream(dis);
        new File(target + ".tmp").renameTo(target);
    }

    public static boolean eq(byte[] a, int aoff, byte[] b, int boff, int len) {
        for (int i = 0; i < len; i++) if (a[aoff + i] != b[boff + i]) return false;
        return true;
    }

    /** returns the value of the environment variable key, or null if no such key exists */
    public static String getEnv(String key) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Process p;
            String cmd;
            if (os.indexOf("windows 9") != -1 || os.indexOf("windows me") != -1) {
                cmd = "command.com /c set";
            } else if (os.indexOf("windows") > -1) {
                cmd = "cmd.exe /c set";
            } else {
                cmd = "env";
            }
            p = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s;
            while ((s = br.readLine()) != null) if (s.startsWith(key + "=")) return s.substring(key.length() + 1);
        } catch (Exception e) {
        }
        return null;
    }

    /** dumps the environment plus an additional variable */
    private static String[] dumpEnv(String newvar) throws IOException {
        Vector v = new Vector();
        String os = System.getProperty("os.name").toLowerCase();
        Process p;
        if (os.indexOf("windows 9") > -1) {
            p = Runtime.getRuntime().exec("command.com /c set");
        } else if ((os.indexOf("nt") > -1) || (os.indexOf("windows 2000") > -1)) {
            p = Runtime.getRuntime().exec("cmd.exe /c set");
        } else {
            p = Runtime.getRuntime().exec("env");
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s;
        while ((s = br.readLine()) != null) v.addElement(s);
        v.addElement(newvar);
        String[] ret;
        v.copyInto(ret = new String[v.size()]);
        return ret;
    }

    private Image backbuffer = null;

    public final void paint(Graphics g) {
        if (backbuffer != null) g.drawImage(backbuffer, 0, 0, null);
    }

    public final Graphics getGraphics() {
        return super.getGraphics();
    }

    public final Dimension getSize() {
        return super.getSize();
    }

    private void update(double loaded, String text) {
        Graphics g2 = getGraphics();
        String s = text;
        if (backbuffer == null || backbuffer.getWidth(null) != getSize().width || backbuffer.getHeight(null) != getSize().height) backbuffer = createImage(getSize().width, getSize().height);
        if (backbuffer == null) return;
        Graphics g = backbuffer.getGraphics();
        g.setColor(loaded < 0 ? Color.red : Color.blue);
        loaded = Math.abs(loaded);
        int w = (int) ((double) getSize().width * loaded);
        g.fillRect(0, 0, w, getSize().height);
        g.setColor(Color.darkGray);
        g.fillRect(w, 0, getSize().width - w, getSize().height);
        Font f = new Font("Sans-serif", Font.BOLD, 12);
        FontRenderContext frc = new FontRenderContext(null, true, true);
        LineMetrics lm = f.getLineMetrics(s, frc);
        g.setFont(f);
        int x = (int) ((getSize().width - f.getStringBounds(s, frc).getWidth()) / 2);
        int y = (int) (((getSize().height - lm.getAscent() - lm.getDescent()) / 2) + lm.getAscent());
        g.setColor(Color.white);
        g.drawString(s, x, y);
        if (g2 != null) {
            g2.setClip(0, 0, getSize().width, getSize().height);
            g2.drawImage(backbuffer, 0, 0, null);
        }
    }

    private static final String VEXI_PUBLIC_CERT = "MIIC8zCCAlygAwIBAgIBADANBgkqhkiG9w0BAQQFADBgMQswCQYDVQQGEwJVUzETMBEGA1UECBMK" + "Q2FsaWZvcm5pYTEWMBQGA1UEBxMNU2FuIEZyYW5jaXNjbzERMA8GA1UEChMIdmV4aS5vcmcxETAP" + "BgNVBAMTCHZleGkub3JnMB4XDTA0MDYwNDA4MzMxMFoXDTA1MDYwNDA4MzMxMFowYDELMAkGA1UE" + "BhMCVVMxEzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBGcmFuY2lzY28xETAPBgNV" + "BAoTCHZleGkub3JnMREwDwYDVQQDEwh2ZXhpLm9yZzCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkC" + "gYEAqgJWXZMq5o2PEdEJO4scUi4lL5Xo/nmfWl8dPwwPPinOOAOf13G36E49+s55G/SuyAaqVFVb" + "F8ZmoLord6Swyp+gmX3eewlu5Di4KiOUv+Q97UCj95i706ETtQklLELfWIKTKGC4ee0MVfbygnHl" + "AWMTwGDVm6ZDFi+e4v4qZo0CAwEAAaOBvDCBuTAdBgNVHQ4EFgQU+yuXGXymk6nv+Ixy8r3SzJwD" + "L1YwgYkGA1UdIwSBgTB/gBT7K5cZfKaTqe/4jHLyvdLMnAMvVqFkpGIwYDELMAkGA1UEBhMCVVMx" + "EzARBgNVBAgTCkNhbGlmb3JuaWExFjAUBgNVBAcTDVNhbiBGcmFuY2lzY28xETAPBgNVBAoTCHZl" + "eGkub3JnMREwDwYDVQQDEwh2ZXhpLm9yZ4IBADAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBAUA" + "A4GBAAv0Now+Hm8U7VYDZ1r2tw6pOdNHFM5A1Fi5ecQiHRANAnG/VoBTDXyRMo1++xmIFwg3btr1" + "8wC23MEIr8VMYmiwjdzDXryRPwQ01X+PQE1d/HhsepfUZQDqltD4iPxS7pAmzHIbZSjKk8fgi38k" + "M0zNiM0xsC/IknpXXOePhGzk";

    private static final String[] commonJavaLocations = new String[] { "/usr/bin/java", "/usr/java/bin/java", "/usr/local/bin/java", "/usr/local/java/bin/java", "/usr/lib/j2sdk1.4/bin/java", "/usr/lib/j2sdk1.3/bin/java", "/usr/lib/j2sdk1.2/bin/java" };
}
