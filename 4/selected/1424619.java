package com.monad.homerun.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Properties;
import org.osgi.framework.Bundle;
import com.monad.homerun.core.GlobalProps;

/**
 * Installer contains static utility methods for installing resources
 * into the server. These resources can include: data object load files,
 * java libraries (jar files), help files, icons, configuration files, etc.
 */
public class Installer {

    public static void checkInstall(Bundle b) {
        Dictionary dict = b.getHeaders();
        String bName = (String) dict.get("Bundle-Name");
        String version = (String) dict.get("Bundle-Version");
        if (canInstall(bName, version)) {
            installConf(b);
        }
    }

    public static boolean canInstall(String name, String version) {
        if (GlobalProps.DEBUG) {
            System.out.println("CanInstall - testing: " + name);
        }
        String instPath = GlobalProps.getHomeDir() + File.separator + "conf" + File.separator + "installed";
        Properties instProps = new Properties();
        try {
            File instFile = new File(instPath);
            if (instFile.exists()) {
                instProps.load(new FileInputStream(instFile));
            } else {
                instFile.createNewFile();
            }
            if (instProps.containsKey(name)) {
                return false;
            }
            instProps.put(name, version);
            instProps.store(new FileOutputStream(instFile), "Do not edit!");
            return true;
        } catch (Exception e) {
            if (GlobalProps.DEBUG) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static final void installConf(Bundle b) {
        Dictionary dict = b.getHeaders();
        String bName = (String) dict.get("Bundle-Name");
        String confName = "conf/" + bName + ".xml";
        URL resUrl = b.getResource(confName);
        if (resUrl != null) {
            String instPath = "conf" + File.separator + (String) dict.get("Bundle-Category");
            String fileName = bName + ".xml";
            try {
                installFile(resUrl.openStream(), instPath, fileName, false);
            } catch (IOException ioe) {
                if (GlobalProps.DEBUG) {
                    System.out.println("Install failure! - bundle: " + bName);
                    ioe.printStackTrace();
                }
            }
        }
    }

    private static void installFile(InputStream in, String instPath, String fileName, boolean update) throws IOException {
        String path = GlobalProps.getHomeDir() + File.separator + instPath;
        File instDir = new File(path);
        if (!instDir.isDirectory()) {
            instDir.mkdirs();
        }
        String inst = path + File.separator + fileName;
        File instFile = new File(inst);
        if (instFile.exists()) {
            if (update) {
                instFile.delete();
            } else {
                return;
            }
        }
        FileOutputStream fileOut = new FileOutputStream(instFile);
        int read = 0;
        byte[] buf = new byte[1024];
        while ((read = in.read(buf)) != -1) {
            fileOut.write(buf, 0, read);
        }
        in.close();
        fileOut.close();
    }
}
