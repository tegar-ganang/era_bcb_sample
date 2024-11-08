package tjacobs.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AllPermission;
import java.util.HashMap;
import java.util.Properties;
import java.util.PropertyPermission;

public abstract class Main {

    public static final String PROD = "Prod";

    public static final String QA = "QA";

    public static final String DEV = "Dev";

    public static final String CODE_ZONE = "CODE ZONE";

    private static Main sSingleton;

    private Properties mProps;

    private HashMap<Object, Serializable> mSerializableData;

    private static File sAppDir = null;

    private static boolean hasAllPermission = false;

    private static boolean hasFilePermission = false;

    private String projectName = null;

    public enum APP_ZONE {

        DEV, QA, PROD
    }

    /**
	 * Provides easy Application persistance via the file system
	 * @see App
	 */
    public Main() {
        this(null);
    }

    public Main(String projectName) {
        if (projectName != null) setProjectName(projectName);
        if (sSingleton == null) {
            sSingleton = this;
            SecurityManager sm = System.getSecurityManager();
            try {
                if (sm != null) sm.checkPermission(new AllPermission());
                hasAllPermission = true;
                hasFilePermission = true;
            } catch (SecurityException ex) {
                try {
                    sm.checkPermission(new PropertyPermission("user.home", "read"));
                    sm.checkPermission(new FilePermission(new File(new File(System.getProperty("user.home")), "*").getAbsolutePath(), "read,write"));
                    hasFilePermission = true;
                } catch (SecurityException ex2) {
                }
            }
            loadProperties();
            loadData();
        }
    }

    public String getProjectName() {
        if (projectName == null) return getClass().getName();
        return projectName;
    }

    protected void setProjectName(String name) {
        projectName = name;
    }

    /**
	 * Default app directory is user.home/TUS-Data/[project_name]/, unless changed
	 * @return
	 */
    public static File getDefaultAppDir() {
        if (sAppDir == null) {
            String homestr = System.getProperty("user.home");
            File home = new File(homestr);
            sAppDir = new File(new File(home, "TUS-Data" + File.separatorChar), sSingleton.getProjectName() + File.separatorChar);
        }
        return sAppDir;
    }

    /**
	 * Default app directory is user.home/TUS-Data/[project_name]/, unless changed
	 */
    public static void setDefaultAppDir(File f) {
        if (f.isDirectory() || !f.exists()) {
            sAppDir = f;
        }
    }

    /**
	 * get this application's properties as a Properties object
	 * note that if subcomponents are also using Main, all properties
	 * will be in the same Properties data object, so good
	 * naming is important
	 * 
	 * Properties can also typically be modified outside of the
	 * application by editing the persistant xml property file
	 * @return
	 */
    protected Properties getProperties() {
        return mProps;
    }

    /**
	 * set this application's properties
	 * note that if subcomponents are also using Main, all properties
	 * will be in the same Properties data object, so good
	 * naming is important
	 * 
 	 * Properties can also typically be modified outside of the
	 * application by editing the persistant xml property file

	 * @return
	 */
    protected void setProperties(Properties p) {
        mProps = p;
    }

    /**
	 * set an application property
	 * note that if subcomponents are also using Main, all properties
	 * will be in the same Properties data object, so good
	 * naming is important
	 * 
	 * Properties can also typically be modified outside of the
	 * application by editing the persistant xml property file
	 * @return
	 */
    public void setProperty(String property, String value) {
        if (mProps == null) {
            mProps = new Properties();
        }
        mProps.setProperty(property, value);
    }

    /**
	 * remove an application property
	 * note that if subcomponents are also using Main, all properties
	 * will be in the same Properties data object, so good
	 * naming is important
	 * 
	 * Properties can also typically be modified outside of the
	 * application by editing the persistant xml property file
	 * @return
	 */
    public void removeProperty(String property) {
        mProps.remove(property);
    }

    /**
	 * get an application property
	 * note that if subcomponents are also using Main, all properties
	 * will be in the same Properties data object, so good
	 * naming is important
	 * 
	 * Properties can also typically be modified outside of the
	 * application by editing the persistant xml property file
	 * @return
	 */
    public String getProperty(String property) {
        if (mProps == null) return null;
        return mProps.getProperty(property);
    }

    /**
	 * generates a property file name based on the given class's name
	 * and the user's home directory
	 * @param c
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public static File GetDefaultPropertyFile(Class c) {
        return new File(getDefaultAppDir(), "config.xml");
    }

    /**
	 * specify a File for saving the applications data.
	 * By default, this file is created based off of the
	 * properties file, and given a .dat extension
	 * @return
	 */
    public File getDataFile() {
        File f = getPropertyFile();
        String filename = IOUtils.getFilenameNoExtension(f);
        File f2 = new File(f.getParent(), filename + ".dat");
        return f2;
    }

    /**
	 * Application data works just like the application properties
	 * getting and setting, only data works with Serializables, not Strings
	 * Because it saves serialized java objects and not xml, modification of 
	 * data outside of the application is harder than it is with properties.
	 * Also, while properties are automatically loaded, application data
	 * must be actively loaded before it can be access in a java program
	 * @param key
	 * @param s
	 */
    public void setData(Object key, Serializable s) {
        if (mSerializableData == null) {
            mSerializableData = new HashMap<Object, Serializable>();
        }
        mSerializableData.put(key, s);
    }

    /**
	 * Application data works just like the application properties
	 * getting and setting, only data works with Serializables, not Strings
	 * Because it saves serialized java objects and not xml, modification of 
	 * data outside of the application is harder than it is with properties.
	 * Also, while properties are automatically loaded, application data
	 * must be actively loaded before it can be access in a java program
	 * @param key
	 * @param s
	 */
    public Serializable getData(Object key) {
        if (mSerializableData == null) return null;
        return (Serializable) mSerializableData.get(key);
    }

    protected File getPropertyFile() {
        return GetDefaultPropertyFile(getClass());
    }

    /**
	 * Application data works just like the application properties
	 * getting and setting, only data works with Serializables, not Strings
	 * Because it saves serialized java objects and not xml, modification of 
	 * data outside of the application is harder than it is with properties.
	 * Also, while properties are automatically loaded, application data
	 * must be actively loaded before it can be access in a java program
	 */
    public boolean saveData() {
        return saveData(getDataFile());
    }

    /**
	 * Application data works just like the application properties
	 * getting and setting, only data works with Serializables, not Strings
	 * Because it saves serialized java objects and not xml, modification of 
	 * data outside of the application is harder than it is with properties.
	 * Also, while properties are automatically loaded, application data
	 * must be actively loaded before it can be access in a java program
	 */
    public boolean saveData(File f) {
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(new FileOutputStream(f));
            out.writeObject(mSerializableData);
            out.close();
            return true;
        } catch (IOException iox) {
            iox.printStackTrace();
        }
        return false;
    }

    /**
	 * Application data works just like the application properties
	 * getting and setting, only data works with Serializables, not Strings
	 * Because it saves serialized java objects and not xml, modification of 
	 * data outside of the application is harder than it is with properties.
	 * Also, while properties are automatically loaded, application data
	 * must be actively loaded before it can be access in a java program
	 */
    public boolean loadData() {
        return loadData(getDataFile());
    }

    /**
	 * Application data works just like the application properties
	 * getting and setting, only data works with Serializables, not Strings
	 * Because it saves serialized java objects and not xml, modification of 
	 * data outside of the application is harder than it is with properties.
	 * Also, while properties are automatically loaded, application data
	 * must be actively loaded before it can be access in a java program
	 */
    @SuppressWarnings("unchecked")
    public boolean loadData(File f) {
        if (!Main.isHasFilePermission()) return false;
        ObjectInputStream in;
        if (!f.exists()) return false;
        try {
            in = new ObjectInputStream(new FileInputStream(f));
            mSerializableData = (HashMap) in.readObject();
            in.close();
        } catch (InvalidClassException ice) {
            f.delete();
            return false;
        } catch (IOException iox) {
            iox.printStackTrace();
            return false;
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean isHasAllPermission() {
        return hasAllPermission;
    }

    public static boolean isHasFilePermission() {
        return hasFilePermission;
    }

    public void loadProperties() {
        loadProperties(getPropertyFile());
    }

    /**
	 * Load application properties. This is useful
	 * if application properties have been changed but you
	 * want to revert to the previous saved state
	 * @param f
	 */
    public void loadProperties(File f) {
        if (!isHasFilePermission()) return;
        if (!f.exists()) return;
        Properties p = new Properties();
        try {
            p.loadFromXML(new FileInputStream(f));
            mProps = p;
        } catch (IOException iox) {
        }
    }

    /**
	 * Save the application's properties to the default properties file
	 * @return
	 */
    public boolean saveProperties() {
        return saveProperties(getPropertyFile());
    }

    /**
	 * Save the application's properties to the given properties file
	 * @return
	 */
    public boolean saveProperties(File f) {
        File dir = f.getParentFile();
        if (!dir.exists()) ;
        dir.mkdirs();
        try {
            if (f.exists()) f.delete();
            FileOutputStream out = new FileOutputStream(f);
            mProps.storeToXML(out, "");
            return true;
        } catch (IOException iox) {
            iox.printStackTrace();
            return false;
        }
    }

    public static Main getSingleton() {
        return sSingleton;
    }

    /**
	 * WARNING: This is a dangerous method
	 * The purpose of clearSingleton is to
	 * be able to start another app and have it use its own
	 * app properties and data instead of the callers.
	 * @return
	 */
    public static Main clearSingleton() {
        Main m = sSingleton;
        sSingleton = null;
        return m;
    }

    public APP_ZONE getZone() {
        Properties p = getProperties();
        if (p == null || p.getProperty(CODE_ZONE) == null) return APP_ZONE.DEV;
        String zone = p.getProperty(CODE_ZONE);
        if (zone.equalsIgnoreCase("QA")) return APP_ZONE.QA;
        if (zone.equalsIgnoreCase("PROD")) return APP_ZONE.PROD;
        return APP_ZONE.DEV;
    }

    public static void setZone(APP_ZONE zone) {
        String z = (zone == APP_ZONE.DEV) ? "DEV" : (zone == APP_ZONE.QA) ? "QA" : "PROD";
        if (sSingleton != null) {
            sSingleton.setProperty(CODE_ZONE, z);
            sSingleton.saveProperties();
        }
    }
}
