package net.genaud.vicaya.launch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProfileManager {

    private static Log log = LogFactory.getLog(ProfileManager.class);

    private ConfigBean config = null;

    private String slash = null;

    public ProfileManager() {
        slash = System.getProperty("file.separator");
        PropertyReader propReader = new PropertyReader();
        propReader.init();
        config = propReader.getConfigPropertyBean();
        if (config.isVerbose()) log.info("config\n======\n" + PropertyReader.configToString(config, false));
    }

    public ConfigBean getConfigBean() {
        return config;
    }

    public void createCatalinaBase(String pathReadBase, String pathWriteBase) {
        if (pathReadBase.equalsIgnoreCase(pathWriteBase)) {
            log.warn("CatalinaBase (writebase) was not written because it is the same as readbase");
            return;
        }
        String path = null;
        File file = null;
        FileWriter writer = null;
        file = new File(pathWriteBase + slash + "temp");
        if (!file.exists()) file.mkdir();
        file = new File(pathWriteBase + slash + "logs");
        if (!file.exists()) file.mkdir();
        file = new File(pathWriteBase + slash + "temp");
        if (!file.exists()) file.mkdir();
        file = new File(pathWriteBase + slash + "webapps");
        if (!file.exists()) file.mkdir();
        file = new File(pathWriteBase + slash + "work");
        if (!file.exists()) file.mkdir();
        path = pathWriteBase + slash + "config.properties";
        file = new File(path);
        if (!file.exists() || !config.isLockconfig()) {
            try {
                writer = new FileWriter(file);
                writer.write(PropertyReader.configToString(config, true));
                writer.close();
            } catch (IOException ioe) {
                System.out.println("Failed to create " + path);
                ioe.printStackTrace();
            }
        }
        File catalinaLocalhost = new File(pathWriteBase + slash + "conf" + slash + "Catalina" + slash + "localhost");
        System.out.println("catalinaLocalhost.exists(): " + catalinaLocalhost.exists());
        System.out.println("config.isLockconfig(): " + config.isLockconfig());
        if (catalinaLocalhost.exists() && !config.isLockconfig()) {
            File[] contexts = catalinaLocalhost.listFiles();
            System.out.println("contexts.length: " + contexts.length);
            for (int i = 0; i < contexts.length; i++) {
                System.out.println("for: " + i + " deleting: " + contexts[i].getAbsolutePath());
                contexts[i].delete();
            }
        }
        recurisiveCopy(new File(pathReadBase + slash + "conf"), new File(pathWriteBase + slash + "conf"));
    }

    public void recurisiveCopy(String pathSource, String pathDestination) {
        recurisiveCopy(new File(pathSource), new File(pathDestination));
    }

    public void recurisiveCopy(File source, File destination) {
        if (source.isFile()) {
            try {
                copyFile(source, destination);
            } catch (Exception e) {
                System.out.println("problems copying " + source.getAbsolutePath() + " to " + destination.getAbsolutePath());
            }
        } else if (source.isDirectory()) {
            if (!destination.exists()) destination.mkdir();
            File[] childSources = source.listFiles();
            for (int cs = 0; cs < childSources.length; cs++) {
                File child = childSources[cs];
                child.getName();
                recurisiveCopy(child, new File(destination.getAbsoluteFile() + slash + child.getName()));
            }
        }
    }

    public void copyFile(String pathSource, String pathDestination) throws FileNotFoundException, IOException {
        copyFile(new File(pathSource), new File(pathDestination));
    }

    public void copyFile(File source, File destination) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(source);
        FileOutputStream fos = new FileOutputStream(destination);
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }
}
