package org.netbeans;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.JarFile;
import java.security.*;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Template;
import org.openide.util.lookup.Lookups;

/** Bootstrap main class.
 * @author Jaroslav Tulach, Jesse Glick
 */
public class Main extends Object {

    /** Starts the IDE.
     * @param args the command line arguments
     * @throws Exception for lots of reasons
     */
    public static void main(String args[]) throws Exception {
        java.lang.reflect.Method[] m = new java.lang.reflect.Method[1];
        int res = execute(args, System.in, System.out, System.err, m);
        if (res == -1) {
            return;
        } else if (res != 0) {
            System.exit(res);
        }
        m[0].invoke(null, new Object[] { args });
    }

    /** Returns string describing usage of the system. Does that by talking to
     * all registered handlers and asking them to show their usage.
     *
     * @return the usage string for the system
     */
    public static String usage() throws Exception {
        java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
        java.io.ByteArrayOutputStream err = new java.io.ByteArrayOutputStream();
        String[] newArgs = { "--help" };
        int res = execute(newArgs, System.in, os, err, null);
        return new String(os.toByteArray());
    }

    /** Constructs the correct ClassLoader, finds main method to execute 
     * and invokes all registered CLIHandlers.
     *
     * @param args the arguments to pass to the handlers
     * @param reader the input stream reader for the handlers
     * @param writer the output stream for the handlers
     * @param methodToCall null or array with one item that will be set to 
     *   a method that shall be executed as the main application
     */
    static int execute(String[] args, java.io.InputStream reader, java.io.OutputStream writer, java.io.OutputStream error, java.lang.reflect.Method[] methodToCall) throws Exception {
        new URLConnection(Main.class.getResource("Main.class")) {

            public void connect() throws IOException {
            }
        }.setDefaultUseCaches(false);
        ArrayList list = new ArrayList();
        HashSet processedDirs = new HashSet();
        String home = System.getProperty("netbeans.home");
        if (home != null) {
            build_cp(new File(home), list, processedDirs);
        }
        String nbdirs = System.getProperty("netbeans.dirs");
        if (nbdirs != null) {
            StringTokenizer tok = new StringTokenizer(nbdirs, File.pathSeparator);
            while (tok.hasMoreTokens()) {
                build_cp(new File(tok.nextToken()), list, processedDirs);
            }
        }
        String prepend = System.getProperty("netbeans.classpath");
        if (prepend != null) {
            StringTokenizer tok = new StringTokenizer(prepend, File.pathSeparator);
            while (tok.hasMoreElements()) {
                list.add(0, new File(tok.nextToken()));
            }
        }
        StringBuffer buf = new StringBuffer(1000);
        Iterator it = list.iterator();
        while (it.hasNext()) {
            if (buf.length() > 0) {
                buf.append(File.pathSeparatorChar);
            }
            buf.append(((File) it.next()).getAbsolutePath());
        }
        System.setProperty("netbeans.dynamic.classpath", buf.toString());
        ListIterator it2 = list.listIterator();
        while (it2.hasNext()) {
            File f = (File) it2.next();
            if (f.isFile()) {
                it2.set(new JarFile(f, false));
            }
        }
        BootClassLoader loader = new BootClassLoader(list, new ClassLoader[] { Main.class.getClassLoader() });
        if (!Boolean.getBoolean("netbeans.use-app-classloader")) Thread.currentThread().setContextClassLoader(loader);
        CLIHandler.Status result;
        result = CLIHandler.initialize(args, reader, writer, error, loader, true, false, loader);
        if (result.getExitCode() == CLIHandler.Status.CANNOT_CONNECT) {
            int value = javax.swing.JOptionPane.showConfirmDialog(null, java.util.ResourceBundle.getBundle("org/netbeans/Bundle").getString("MSG_AlreadyRunning"), java.util.ResourceBundle.getBundle("org/netbeans/Bundle").getString("MSG_AlreadyRunningTitle"), javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
            if (value == javax.swing.JOptionPane.OK_OPTION) {
                result = CLIHandler.initialize(args, reader, writer, error, loader, true, true, loader);
            }
        }
        String className = System.getProperty("netbeans.mainclass", "org.netbeans.core.startup.Main");
        Class c = loader.loadClass(className);
        Method m = c.getMethod("main", new Class[] { String[].class });
        if (methodToCall != null) {
            methodToCall[0] = m;
        }
        return result.getExitCode();
    }

    /**
     * Call when the system is up and running, to complete handling of
     * delayed command-line options like -open FILE.
     */
    public static void finishInitialization() {
        int r = CLIHandler.finishInitialization(false);
        if (r != 0) {
            System.err.println("Post-initialization command-line options could not be run.");
        }
    }

    static final class BootClassLoader extends JarClassLoader implements Runnable {

        private Lookup metaInf;

        private List handlers;

        public BootClassLoader(List cp, ClassLoader[] parents) {
            super(cp, parents);
            metaInf = Lookups.metaInfServices(this);
            String value = null;
            try {
                if (cp.isEmpty()) {
                    value = searchBuildNumber(this.getResources("META-INF/MANIFEST.MF"));
                } else {
                    value = searchBuildNumber(this.simpleFindResources("META-INF/MANIFEST.MF"));
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (value == null) {
                System.err.println("Cannot set netbeans.buildnumber property no OpenIDE-Module-Implementation-Version found");
            } else {
                System.setProperty("netbeans.buildnumber", value);
            }
        }

        /** @param en enumeration of URLs */
        private static String searchBuildNumber(Enumeration en) {
            String value = null;
            try {
                java.util.jar.Manifest mf;
                URL u = null;
                while (en.hasMoreElements()) {
                    u = (URL) en.nextElement();
                    InputStream is = u.openStream();
                    mf = new java.util.jar.Manifest(is);
                    is.close();
                    value = mf.getMainAttributes().getValue("OpenIDE-Module-Implementation-Version");
                    if (value != null) {
                        break;
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return value;
        }

        private boolean onlyRunRunOnce;

        /** Checks for new JARs in netbeans.user */
        public void run() {
            if (onlyRunRunOnce) return;
            onlyRunRunOnce = true;
            ArrayList toAdd = new ArrayList();
            String user = System.getProperty("netbeans.user");
            try {
                if (user != null) {
                    build_cp(new File(user), toAdd, new HashSet());
                    ListIterator it2 = toAdd.listIterator();
                    while (it2.hasNext()) {
                        File f = (File) it2.next();
                        if (f.isFile()) {
                            it2.set(new JarFile(f, false));
                        }
                    }
                }
                if (!toAdd.isEmpty()) {
                    addSources(toAdd);
                    metaInf = Lookups.metaInfServices(this);
                    if (handlers != null) {
                        handlers.clear();
                        handlers.addAll(metaInf.lookup(new Lookup.Template(CLIHandler.class)).allInstances());
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        /** Startup optimalization. See issue 27226. */
        protected PermissionCollection getPermissions(CodeSource cs) {
            return getAllPermission();
        }

        /** Startup optimalization. See issue 27226. */
        private static PermissionCollection modulePermissions;

        /** Startup optimalization. See issue 27226. */
        private static synchronized PermissionCollection getAllPermission() {
            if (modulePermissions == null) {
                modulePermissions = new Permissions();
                modulePermissions.add(new AllPermission());
                modulePermissions.setReadOnly();
            }
            return modulePermissions;
        }

        /** For a given classloader finds all registered CLIHandlers.
         */
        public final Collection allCLIs() {
            if (handlers == null) {
                handlers = new ArrayList(metaInf.lookup(new Lookup.Template(CLIHandler.class)).allInstances());
            }
            return handlers;
        }

        protected boolean isSpecialResource(String pkg) {
            boolean retValue = super.isSpecialResource(pkg);
            if (retValue) return true;
            return false;
        }
    }

    private static void append_jars_to_cp(File dir, Collection toAdd) {
        if (!dir.isDirectory()) return;
        File[] arr = dir.listFiles();
        for (int i = 0; i < arr.length; i++) {
            String n = arr[i].getName();
            if (n.endsWith("jar") || n.endsWith("zip")) {
                toAdd.add(arr[i]);
            }
        }
    }

    private static void build_cp(File base, Collection toAdd, Set processedDirs) throws java.io.IOException {
        base = base.getCanonicalFile();
        if (!processedDirs.add(base)) {
            return;
        }
        append_jars_to_cp(new File(base, "core/patches"), toAdd);
        append_jars_to_cp(new File(base, "core"), toAdd);
        append_jars_to_cp(new File(base, "core/locale"), toAdd);
    }
}
