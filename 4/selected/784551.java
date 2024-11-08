package de.shandschuh.jaolt.launcher;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.UIManager;
import de.shandschuh.jaolt.tools.log.Logger;

public class ApplicationLauncher {

    private static final String[] BLACKLIST = new String[] { "swingx-0.9.1.jar", "js.jar", "cobra.jar", "csv.jar", "uif_lite.jar", "h2.jar", "jdom.jar", "balloontip.jar", "ekit.jar", "swingx-1.0.jar" };

    public static final String APPLICATION_PATH = getApplicationPath();

    public static URLClassLoader urlLoader;

    public static void main(String[] args) {
        if (args != null && args.length == 1 && "--help".equals(args[0])) {
            System.out.println("Usage:\n\n jaolt [-Djaolt.users.dir=/path/to/user/data | -Djaolt.data.dir=/path/to/db/files | -Djaolt.applicationdata.dir=/path/to/config/files]");
        } else {
            launch("de.shandschuh.jaolt.gui.Lister", true);
        }
    }

    public static void launch(final String mainClassName, boolean gui) {
        File lockFile = new File(System.getProperty("user.home") + File.separator + "." + mainClassName.hashCode() + ".lock");
        try {
            FileChannel channel = new RandomAccessFile(lockFile, "rw").getChannel();
            FileLock lock = channel.tryLock();
            if (lock == null) {
                return;
            } else {
                lockFile.deleteOnExit();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        File[] libs = new File(APPLICATION_PATH + "lib").listFiles(new JarFileNameFilter(BLACKLIST));
        int libsLength = libs != null ? libs.length : 0;
        URL[] libUrls = new URL[libsLength];
        for (int n = 0, i = libUrls.length; n < i; n++) {
            try {
                libUrls[n] = libs[n].toURI().toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        urlLoader = new URLClassLoader(libUrls, ClassLoader.getSystemClassLoader());
        if (gui && !System.getProperty("java.fullversion", "").contains("GNU")) {
            try {
                urlLoader.loadClass("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
                UIManager.installLookAndFeel("JGoodies Plastic XP", "com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
                Hashtable<Object, Object> uiDefaults = UIManager.getDefaults();
                uiDefaults.put("ClassLoader", urlLoader);
                Enumeration<?> enumeration = uiDefaults.keys();
                while (enumeration.hasMoreElements()) {
                    String className = enumeration.nextElement().toString();
                    if (className.endsWith("UI")) {
                        Class<?> uiClass = urlLoader.loadClass(uiDefaults.get(className).toString());
                        uiDefaults.put(uiClass.getName(), uiClass);
                    }
                }
            } catch (Exception exception) {
            }
        }
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    urlLoader.loadClass(mainClassName).newInstance();
                } catch (Exception exception) {
                    Logger.log(exception);
                }
            }
        });
    }

    private static String getApplicationPath() {
        String path = System.getProperty("jaolt.application.path");
        if (path != null && path.length() > 0) {
            return new File(path).getAbsolutePath() + File.separator;
        } else {
            String classPath = System.getProperty("java.class.path");
            if (classPath == null || classPath.length() == 0) {
                return "";
            } else if (classPath.indexOf(';') > 0 || classPath.indexOf(':') > -1) {
                String[] chars = classPath.indexOf(';') > 0 ? classPath.split("\\;") : classPath.split("\\:");
                for (int n = 0, i = chars != null ? chars.length : 0; n < i; n++) {
                    if (chars[n].endsWith("launcher.jar")) {
                        return new File(chars[n]).getParent() + File.separator;
                    }
                }
                return "";
            } else if (new File(classPath).getParent() == null) {
                return "";
            } else {
                return new File(classPath).getParent() + File.separator;
            }
        }
    }

    public static URLClassLoader getUrlLoader() {
        return urlLoader;
    }
}
