package net.sf.wwusmart.algorithms.framework;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is responsible for addressing all algorithm plugins written in Java.
 * 
 * @author thilo
 * @version $Rev: 777 $
 */
public class JavaPluginConnector extends ClassLoader {

    /** Singleton instance */
    private static final JavaPluginConnector instance = new JavaPluginConnector();

    /**
     * To aviod loading a class more than once (which would cause LinkageError)
     * we keep track of the files of which we have already loaded classes within
     * this data structure.
     * We map filenames of the .class/.jar files to the according class objects.
     */
    private Map<String, Class> loadedClassFiles = new HashMap();

    /**
     * To keep track in which files we can find supporting classes of our plugins:
     * Map class names to the filenames the classes have been seen in.
     */
    private Map<String, String> class2fileName = new HashMap();

    private JavaPluginConnector() {
    }

    /** get Singleton instance */
    protected static JavaPluginConnector getInstance() {
        return instance;
    }

    protected Class loadClassFromFile(String filename) {
        Class c = loadedClassFiles.get(filename);
        if (c == null) {
            try {
                Logger.getLogger(ParametersSetting.class.getName()).log(Level.FINEST, "Trying to load plugin from file `" + filename + "'...");
                byte[] b = loadFileData(filename);
                c = defineClass(null, b, 0, b.length);
                resolveClass(c);
                loadedClassFiles.put(filename, c);
                class2fileName.put(c.getName(), filename);
                Logger.getLogger(ParametersSetting.class.getName()).log(Level.FINEST, "... successfully loades class `" + c.getName() + "' from file `" + filename + "'");
                return c;
            } catch (FileNotFoundException ex) {
                throw new RuntimeException("Could not find `" + filename + "', " + ex.getMessage());
            } catch (IOException ex) {
                throw new RuntimeException("IO failure on `" + filename + "', " + ex.getMessage());
            } catch (Throwable t) {
                throw new RuntimeException("Failed loading `" + filename + "', " + t.getMessage());
            }
        }
        return c;
    }

    @Override
    protected Class findClass(String name) {
        Logger.getLogger(ParametersSetting.class.getName()).log(Level.FINER, "Trying to load class `" + name + "'...");
        String filename = class2fileName.get(name);
        if (filename != null) {
            return loadClassFromFile(filename);
        } else {
            String[] tmp = name.split("\\.");
            String lastName = tmp[tmp.length - 1].toLowerCase();
            for (String dirName : AlgorithmManager.getInstance().getPluginDirectories()) {
                File dir = new File(dirName);
                for (File file : dir.listFiles()) {
                    if (file.isFile() && knownFileType(file.getName())) {
                        if (file.getName().toLowerCase().contains(lastName)) {
                            Logger.getLogger(ParametersSetting.class.getName()).log(Level.FINEST, "... checking file `" + file.getPath() + "'...");
                            try {
                                Class c = loadClassFromFile(file.getPath());
                                if (c.getName().equals(name)) {
                                    Logger.getLogger(ParametersSetting.class.getName()).log(Level.FINER, "... successfully loaded class `" + name + "' from file `" + file.getPath() + "'");
                                    return c;
                                }
                            } catch (RuntimeException e) {
                                Logger.getLogger(ParametersSetting.class.getName()).log(Level.FINER, "... failed loading class from `" + file.getPath() + "': ", e);
                            }
                        }
                    } else {
                        Logger.getLogger(ParametersSetting.class.getName()).log(Level.FINEST, "... skipping `" + file.getPath() + "'...");
                    }
                }
            }
            Logger.getLogger(ParametersSetting.class.getName()).log(Level.WARNING, "Cannot find any file containing class `" + name + "'");
            return null;
        }
    }

    private byte[] loadFileData(String name) throws FileNotFoundException, IOException {
        File file = new File(name);
        FileInputStream fis = new FileInputStream(file);
        long filesize = fis.getChannel().size();
        byte[] result = new byte[(int) filesize];
        fis.read(result);
        fis.close();
        return result;
    }

    /**
     * Check if this plugin connector can handle the given plugin file.
     * Check is based only on filename extension!
     * @param filename the name of the file to check for
     * @return true if the filename suffix denotes a plugin this connector can handle,
     * false otherwise
     */
    public static boolean knownFileType(String filename) {
        for (String ext : ".class".toLowerCase().split(";")) {
            if (filename.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
