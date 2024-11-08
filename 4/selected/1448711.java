package com.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;

public class MysqlSyncProperties {

    private static Logger log = Logger.getLogger(MysqlSyncProperties.class);

    private static String HOME_FOLDER = "";

    private static String SYNC_FILENAME = "sync.properties";

    private static Properties properties = null;

    public static String getProperty(String name) {
        if (properties == null) {
            loadSetupProperties();
        }
        if (properties == null) {
            return null;
        }
        return properties.getProperty(name);
    }

    public static void setProperty(String name, String value) {
        if (properties == null) {
            loadSetupProperties();
        }
        if (properties != null) {
            properties.setProperty(name, value);
            String proFile = HOME_FOLDER + File.separator + SYNC_FILENAME;
            try {
                properties.store(new FileOutputStream(proFile), proFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getProperty(String name, String defaultValue) {
        if (properties == null) {
            loadSetupProperties();
        }
        if (properties == null) {
            return null;
        }
        String value = properties.getProperty(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public static int getProperty(String name, int defaultValue) {
        String value = getProperty(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException nfe) {
            }
        }
        return defaultValue;
    }

    public static String getHomeDirectory() {
        return HOME_FOLDER;
    }

    public static void setHomeDirectory(String pathname) {
        File mh = new File(pathname);
        if (!mh.exists()) {
            log.error("Error - the specified home directory does not exist (" + pathname + ")");
        } else if (!mh.canRead() || !mh.canWrite()) {
            log.error("Error - the user running this application can not read " + "and write to the specified home directory (" + pathname + "). " + "Please grant the executing user read and write permissions.");
        } else {
            HOME_FOLDER = pathname;
        }
    }

    private static synchronized void loadSetupProperties() {
        if (properties == null) {
            try {
                properties = new Properties();
                properties.load(new FileInputStream(HOME_FOLDER + File.separator + SYNC_FILENAME));
            } catch (IOException ioe) {
                ioe.printStackTrace();
                log.error(ioe.getMessage());
            }
        }
    }
}
