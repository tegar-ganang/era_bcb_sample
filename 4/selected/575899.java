package org.t2framework.executablewar;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * 実行可能WARファイルのためのランチャクラスです。
 * <p>
 * <a href="http://hudson.dev.java.net/">Hudson</a>のexecutable-warモジュールの
 * Mainクラスのコードをベースにしています。
 * </p>
 * 
 * @author YOKOTA Takehiko
 * @author shot
 */
public class Main {

    private static final String WINSTONE_JAR = "winstone.jar";

    private static final String EXECUTABLEWAR_HOME = "EXECUTABLEWAR_HOME";

    private static final String OPTION_WEBROOT = "--webroot=";

    private static final String OPTION_DEPLOY = "deploy";

    private static final String OPTION_COMMON_LIB_FOLDER = "--commonLibFolder=";

    private static final String OPTION_START = "start";

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        File me = whoAmI();
        List<String> arguments = new ArrayList<String>(Arrays.asList(args));
        File webRoot = null;
        boolean deploy = false;
        boolean start = true;
        for (Iterator<String> itr = arguments.iterator(); itr.hasNext(); ) {
            String s = itr.next();
            if (s.startsWith(OPTION_WEBROOT)) {
                webRoot = new File(s.substring(OPTION_WEBROOT.length()).trim());
            } else if (s.equals(OPTION_DEPLOY)) {
                deploy = true;
                start = false;
            } else if (s.equals(OPTION_START)) {
                start = true;
            }
        }
        if (webRoot == null) {
            webRoot = new File(getHomeDir(), me.getName());
            arguments.add(OPTION_WEBROOT + webRoot);
        }
        arguments.add(OPTION_COMMON_LIB_FOLDER + new File(webRoot, "META-INF/winstone/lib"));
        if (deploy || !webRoot.exists()) {
            deleteContents(webRoot);
            webRoot.mkdirs();
            try {
                unpackWar(me, webRoot, WINSTONE_JAR, "org");
            } catch (IOException ex) {
                IOException x = new IOException("Cannot unpack WAR: " + me);
                x.initCause(ex);
                throw x;
            }
            System.out.println("Successfully deployed " + me + " to " + webRoot);
        } else {
            System.out.println("**NOT DEPLOYED** because " + webRoot + " already exists. If you want to re-deploy, run with 'deploy' option.");
        }
        if (!start) {
            return;
        }
        URL jar = Main.class.getClassLoader().getResource(WINSTONE_JAR);
        File tmpJar;
        try {
            tmpJar = File.createTempFile("winstone", "jar");
        } catch (IOException e) {
            String tmpdir = System.getProperty("java.io.tmpdir");
            IOException x = new IOException("Failed to create a temporary file in " + tmpdir);
            x.initCause(e);
            throw x;
        }
        copyStream(jar.openStream(), new FileOutputStream(tmpJar));
        tmpJar.deleteOnExit();
        File tempFile = File.createTempFile("dummy", "dummy");
        deleteContents(new File(tempFile.getParent(), "winstone/" + me.getName()));
        tempFile.delete();
        ClassLoader cl = new URLClassLoader(new URL[] { tmpJar.toURL() });
        Class<?> launcher = cl.loadClass("winstone.Launcher");
        Method mainMethod = launcher.getMethod("main", new Class[] { String[].class });
        arguments.add("--prefix=/" + me.getName());
        mainMethod.invoke(null, new Object[] { arguments.toArray(new String[0]) });
        openBrowser("http://localhost:8080/" + me.getName() + "/index.jsp");
    }

    public static void openBrowser(String url) throws IOException {
        final Properties properties = System.getProperties();
        String s = (String) properties.get("os.name");
        final String os = s.toLowerCase();
        if (os.indexOf("windows") != -1) {
            if (os.indexOf("2000") != -1) {
                if (url.toLowerCase().endsWith(".html") || url.toLowerCase().endsWith(".htm")) {
                    url += "#";
                }
            }
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
        } else if (os.indexOf("mac") != -1) {
            Runtime.getRuntime().exec(new String[] { "open", url });
        } else {
            System.out.println("open browser,  and go to " + url);
        }
    }

    private static void unpackWar(File warFile, File destination, String... excludes) throws IOException {
        Set<String> excludeSet = new HashSet<String>(Arrays.asList(excludes));
        JarFile jarFile = new JarFile(warFile);
        for (Enumeration<JarEntry> enm = jarFile.entries(); enm.hasMoreElements(); ) {
            JarEntry entry = enm.nextElement();
            String name = entry.getName();
            if (excludeSet.contains(getFirstSegment(name))) {
                continue;
            }
            File to = new File(destination, name);
            if (name.endsWith("/")) {
                to.mkdirs();
            } else {
                copyStream(jarFile.getInputStream(entry), new FileOutputStream(to));
            }
        }
        jarFile.close();
    }

    private static String getFirstSegment(String name) {
        if (name == null) {
            return null;
        }
        int slash = name.indexOf('/');
        if (slash < 0) {
            return name;
        } else {
            return name.substring(0, slash);
        }
    }

    /**
	 * Figures out the URL of <tt>hudson.war</tt>.
	 */
    public static File whoAmI() throws IOException, URISyntaxException {
        URL classFile = Main.class.getClassLoader().getResource(Main.class.getName().replace('.', '/').concat(".class"));
        return new File(((JarURLConnection) classFile.openConnection()).getJarFile().getName());
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        in = new BufferedInputStream(in);
        out = new BufferedOutputStream(out);
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }

    private static void deleteContents(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) deleteContents(files[i]);
            }
        }
        file.delete();
    }

    /**
	 * ホームディレクトリを決定して返します。
	 * 
	 * 設定を良く間違えられるため、{@link String#trim()}をして問題がなくなるようにしています。
	 */
    private static File getHomeDir() {
        try {
            InitialContext iniCtxt = new InitialContext();
            Context env = (Context) iniCtxt.lookup("java:comp/env");
            String value = (String) env.lookup(EXECUTABLEWAR_HOME);
            if (value != null && value.trim().length() > 0) return new File(value.trim());
            value = (String) iniCtxt.lookup(EXECUTABLEWAR_HOME);
            if (value != null && value.trim().length() > 0) return new File(value.trim());
        } catch (NamingException e) {
        }
        String sysProp = System.getProperty(EXECUTABLEWAR_HOME);
        if (sysProp != null) return new File(sysProp.trim());
        try {
            String env = System.getenv(EXECUTABLEWAR_HOME);
            if (env != null) return new File(env.trim()).getAbsoluteFile();
        } catch (Throwable _) {
        }
        return new File(new File(System.getProperty("user.home")), ".executablewar");
    }
}
