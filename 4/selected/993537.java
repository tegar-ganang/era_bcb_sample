package net.emotivecloud.vrmm.vtm.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Initializes the configuration file.
 * @author goirix
 */
public class ConfigManager {

    private Log log = LogFactory.getLog(ConfigManager.class);

    private static String pathConf = "/etc/VtM";

    private static String vtmConfName = "vtm.properties";

    private static String hadoopConfName = "hadoop.xml";

    private static String poolConfName = "poolEnv.cfg";

    private static String vtmConf = pathConf + "/" + vtmConfName;

    private static String hadoopConf = pathConf + "/" + hadoopConfName;

    private static String poolConf;

    private static final String POOL_DEFAULT = "/aplic/brein/pool/";

    private static final String CHECKPOINT_DEFAULT = "/aplic/brein/checkpoint/";

    private static final String HDFS_DEFAULT = "hdfs://localhost:9000";

    private static final int MAX_NUM_CACHE_DEFAULT = 10;

    private static final int SWAP_SIZE = 512;

    private static final int HOME_SIZE = 100;

    private static final String NETWORK = "172.20.202";

    private static final String GATEWAY = "172.20.0.1";

    private PropertiesConfiguration vtmProperties;

    public ConfigManager() {
        URL url = ConfigManager.class.getResource("ConfigManager.class");
        if (url != null) {
            String path = url.toString();
            if (path.startsWith("jar:")) {
                path = path.replaceFirst("jar:", "");
                if (path.startsWith("file:")) path = path.replaceFirst("file:", "");
                String jarPath = path.substring(0, path.indexOf("!"));
                vtmConf = FilenameUtils.normalize(unJar(jarPath, "etc/" + vtmConfName));
                hadoopConf = FilenameUtils.normalize(unJar(jarPath, "etc/" + hadoopConfName));
                poolConf = FilenameUtils.normalize(unJar(jarPath, "etc/" + poolConfName));
                log.info("Configuration files extracted to: " + vtmConf);
                log.info("                                  " + hadoopConf);
                log.info("                                  " + poolConf);
            }
        } else {
            log.error("Configuration file not found. Creating a new one.");
            try {
                new File(vtmConf).createNewFile();
            } catch (IOException e) {
                log.error("Cannot write a new " + vtmConf + " file. Permissions problems?");
            }
        }
        if (new File(pathConf).isDirectory()) {
            vtmConf = setConfigFile(vtmConfName, vtmConf);
            hadoopConf = setConfigFile(hadoopConfName, hadoopConf);
            log.info("VtM configuration:    " + vtmConf);
            log.info("Hadoop configuration: " + hadoopConf);
        }
        try {
            vtmProperties = new PropertiesConfiguration(vtmConf);
            if (vtmProperties.getString("hostname") == null) vtmProperties.addProperty("hostname", InetAddress.getLocalHost().getHostName());
            if (vtmProperties.getString("pool") == null) vtmProperties.addProperty("pool", POOL_DEFAULT);
            if (vtmProperties.getString("checkpoint.path") == null) vtmProperties.addProperty("checkpoint.path", CHECKPOINT_DEFAULT);
            if (vtmProperties.getString("hdfs.address") == null) vtmProperties.addProperty("hdfs.address", HDFS_DEFAULT);
            if (vtmProperties.getString("cache.max") == null) vtmProperties.addProperty("cache.max", new Integer(MAX_NUM_CACHE_DEFAULT));
            if (vtmProperties.getString("swap.size") == null) vtmProperties.addProperty("swap.size", new Integer(SWAP_SIZE));
            if (vtmProperties.getString("home.size") == null) vtmProperties.addProperty("home.size", new Integer(HOME_SIZE));
            vtmProperties.save();
        } catch (ConfigurationException e) {
            log.error("Reading configuration file \"" + vtmConf + "\": " + e.getMessage());
        } catch (UnknownHostException e) {
            log.error("Obtaining host name: using default value.");
        }
        addPropertiesToBash(vtmConf, poolConf);
    }

    /**
	 * Checks if the configuration file already exists in the system.
	 * Otherwise, it installs the default one.
	 * @param configFileName
	 * @return
	 */
    private String setConfigFile(String configFileName, String defaultConf) {
        String ret = "";
        if (new File(pathConf + "/" + configFileName).exists()) ret = FilenameUtils.normalize(pathConf + "/" + configFileName); else {
            try {
                FileUtils.copyFile(new File(defaultConf), new File(pathConf + "/" + configFileName));
            } catch (IOException e) {
                log.error("Copying configuration file: " + e.getMessage());
            }
            ret = FilenameUtils.normalize(pathConf + "/" + configFileName);
        }
        return ret;
    }

    /**
	 * Obtains the configuration file location.
	 * @return Configuration file.
	 */
    public static String getVtMConfigurationPath() {
        return vtmConf;
    }

    /**
	 * Obtains the configuration file location.
	 * @return Configuration file.
	 */
    public static String getHadoopConfigurationPath() {
        return hadoopConf;
    }

    /**
	 * Extract a given entry from its JAR file.
	 * @param jarPath
	 * @param jarEntry
	 */
    private String unJar(String jarPath, String jarEntry) {
        String path;
        if (jarPath.lastIndexOf("lib/") >= 0) path = jarPath.substring(0, jarPath.lastIndexOf("lib/")); else path = jarPath.substring(0, jarPath.lastIndexOf("/"));
        String relPath = jarEntry.substring(0, jarEntry.lastIndexOf("/"));
        try {
            new File(path + "/" + relPath).mkdirs();
            JarFile jar = new JarFile(jarPath);
            ZipEntry ze = jar.getEntry(jarEntry);
            File bin = new File(path + "/" + jarEntry);
            IOUtils.copy(jar.getInputStream(ze), new FileOutputStream(bin));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path + "/" + jarEntry;
    }

    public static boolean getIgnoreVMs() {
        boolean ret = false;
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(vtmConf);
            ret = config.getBoolean("ignorevms");
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String getProperty(String name) {
        String ret = null;
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(vtmConf);
            ret = config.getString(name);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static int getCheckpointPeriod() {
        int ret = 300;
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(vtmConf);
            ret = config.getInt("checkpoint.period");
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<String> getIpPool() {
        List<String> ret = null;
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(vtmConf);
            ret = config.getList("public.ip.pool");
        } catch (ConfigurationException ex) {
            Logger.getLogger(ConfigManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    public void addPropertiesToBash(String properties, String bash) {
        try {
            PropertiesConfiguration config = new PropertiesConfiguration(properties);
            BufferedWriter output = new BufferedWriter(new FileWriter(bash, true));
            Iterator<String> it = config.getKeys();
            output.append("\n\n# Configuration parameters\n");
            while (it.hasNext()) {
                String k = it.next();
                String v = config.getString(k);
                output.append("export " + k.toUpperCase().replaceAll("\\.", "_") + "=" + v + "\n");
            }
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
