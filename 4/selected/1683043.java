package ch.skyguide.tools.requirement.loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JOptionPane;
import ch.skyguide.tools.requirement.data.Preferences;
import ch.skyguide.tools.requirement.hmi.RequirementTool;

/**
 * This bootstrap class loads OpenOffice jars inside the WebStart runtime and starts the application
 */
public class RequirementToolLoader {

    private static final String MAIN_CLASS_NAME = RequirementTool.class.getName();

    private static final String OPEN_OFFICE_DIR_PREFIX = "OpenOffice.org ";

    public static void main(String[] _args) throws MalformedURLException, ClassNotFoundException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        System.setSecurityManager(null);
        List<File> openOfficeJars = getOpenOfficeJars();
        URLClassLoader classLoader = new RequirementToolClassLoader(toArray(toUrls(openOfficeJars)), RequirementToolLoader.class.getClassLoader());
        Class<?> mainClass = classLoader.loadClass(MAIN_CLASS_NAME);
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, new Object[] { _args });
    }

    private static List<File> getOpenOfficeJars() {
        File f = findOpenOfficeInstallation();
        System.out.println("Using Open Office installation: " + f);
        File jarDir = new File(f, "program/classes");
        List<File> l = Arrays.asList(jarDir.listFiles(new FileFilter() {

            public boolean accept(File _pathname) {
                return _pathname.getName().endsWith(".jar");
            }
        }));
        return l;
    }

    private static File findOpenOfficeInstallation() {
        boolean updatePref = false;
        File ooFolder = Preferences.instance.getOpenOfficeFolder();
        String dir = "the existing Preferences";
        while (ooFolder == null || !new File(ooFolder, "program/classes").exists()) {
            System.out.println("OpenOffice.org not found in " + dir + ".");
            updatePref = false;
            String propertyName = "ProgramFiles";
            String property = System.getenv(propertyName);
            if (property == null) {
                System.out.println("Warning: missing system property " + propertyName);
            } else {
                File pf = new File(property).getAbsoluteFile();
                dir = pf.getAbsolutePath();
                File[] dirs = pf.listFiles(new FileFilter() {

                    public boolean accept(File _pathname) {
                        return _pathname.isDirectory() && _pathname.getName().startsWith(OPEN_OFFICE_DIR_PREFIX);
                    }
                });
                Arrays.sort(dirs);
                int i = dirs.length;
                while (i > 0 && !new File(ooFolder, "program/classes").exists()) {
                    ooFolder = dirs[--i];
                }
            }
            if (ooFolder == null || !new File(ooFolder, "program/classes").exists()) {
                System.out.println("OpenOffice.org not found in " + dir + ".");
                dir = JOptionPane.showInputDialog(null, "OpenOffice.org directory not found.\nPlease input the local OOo install directory.", "OOo directory missing", JOptionPane.WARNING_MESSAGE);
                if (dir == null) {
                    System.exit(0);
                }
                ooFolder = new File(dir);
                dir = ooFolder.getAbsolutePath();
                updatePref = true;
            }
        }
        if (updatePref) {
            Preferences.instance.setOpenOfficeFolder(ooFolder);
        }
        return ooFolder;
    }

    private static URL[] toArray(List<URL> _urls) {
        return _urls.toArray(new URL[_urls.size()]);
    }

    private static List<URL> toUrls(List<File> _files) throws MalformedURLException {
        List<URL> result = new ArrayList<URL>();
        for (File file : _files) {
            result.add(file.toURL());
        }
        return result;
    }

    private static byte[] read(InputStream _in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buff = new byte[8 * 1024];
        for (; ; ) {
            int read = _in.read(buff);
            if (read == -1) {
                break;
            }
            out.write(buff, 0, read);
        }
        out.close();
        return out.toByteArray();
    }

    private static byte[] loadResource(final ClassLoader _classLoader, String _name) throws IOException {
        InputStream in = _classLoader.getResourceAsStream(_name);
        if (in == null) {
            return null;
        }
        byte[] data;
        try {
            data = read(in);
            in.close();
            in = null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable t) {
                }
            }
        }
        return data;
    }

    private static class RequirementToolClassLoader extends URLClassLoader {

        private RequirementToolClassLoader(URL[] _urls, ClassLoader _parent) {
            super(_urls, _parent);
        }

        @Override
        protected synchronized Class<?> loadClass(String _name, boolean _resolve) throws ClassNotFoundException {
            if (!_name.startsWith("ch.skyguide.tools.requirement.")) {
                return super.loadClass(_name, _resolve);
            }
            Class<?> c = findLoadedClass(_name);
            if (c == null) {
                try {
                    c = findClass(_name);
                } catch (ClassNotFoundException e) {
                    byte[] data;
                    String resourceName = _name.replace('.', '/') + ".class";
                    try {
                        data = loadResource(getParent(), resourceName);
                    } catch (IOException ioe) {
                        throw new ClassNotFoundException("Failed to load " + _name, ioe);
                    }
                    if (data == null) {
                        throw new ClassNotFoundException("Resource not found: " + resourceName);
                    }
                    c = defineClass(_name, data, 0, data.length);
                }
            }
            if (_resolve) {
                resolveClass(c);
            }
            return c;
        }
    }
}
