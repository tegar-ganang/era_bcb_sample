package com.jasbro.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

public class Jinks implements Runnable {

    private Hashtable<String, File> masterMap = new Hashtable<String, File>();

    private Hashtable<String, File> slaveMap = new Hashtable<String, File>();

    private File masterBase = null;

    private File slaveBase = null;

    private boolean twoWay = false;

    private long sleepInterval = 60;

    public static void main(String[] args) throws IOException {
        System.out.println(System.getProperty("user.dir"));
        if (args.length == 0) {
            System.out.println("Usage: java [-cp <classpath>] com.jasbro.Jinks <properties path>");
            return;
        }
        if (args[0].equalsIgnoreCase("-setup")) {
            return;
        }
        Properties props = new Properties();
        props.load(new FileReader(args[0]));
        String[] confNames = props.getProperty("jinks.configurations", "").split(",");
        for (String conf : confNames) {
            new Jinks(conf, props);
        }
    }

    protected void log(String msg) {
        System.out.println(msg);
    }

    public Jinks(String confName, Properties props) {
        String masterPath = props.getProperty("jinks." + confName + ".masterLocation");
        if (masterPath != null) {
            masterBase = new File(masterPath.trim());
            if (!masterBase.exists() || !masterBase.isDirectory()) {
                String msg = "jinks." + confName + ".masterLocation is not valid or does not exist (value found was " + masterPath + "), this configuration is being ignored";
                log(msg);
                return;
            }
        }
        String slavePath = props.getProperty("jinks." + confName + ".slaveLocation");
        if (slavePath != null) {
            slaveBase = new File(slavePath.trim());
            if (!slaveBase.exists() || !slaveBase.isDirectory()) {
                String msg = "jinks." + confName + ".slaveLocation is not valid or does not exist (value found was " + slavePath + "), this configuration is being ignored";
                log(msg);
                return;
            }
        }
        String twoWayValue = props.getProperty("jinks." + confName + ".twoWay");
        if ("true".equals(twoWayValue)) {
            twoWay = true;
        } else if ("false".equals(twoWayValue)) {
            twoWay = false;
        } else {
            String msg = "jinks." + confName + ".twoWay is not valid or does not exist (value found was " + twoWayValue + "), defaulting to false";
            log(msg);
        }
        String sleepValue = props.getProperty("jinks." + confName + ".sleepInterval");
        if (sleepValue == null) {
            sleepValue = "600";
            String msg = "jinks." + confName + ".sleepInterval does not exist, defaulting to 10 minutes (600 milliseconds)";
            log(msg);
        }
        try {
            sleepInterval = Long.parseLong(sleepValue);
        } catch (NumberFormatException nfe) {
            String msg = "jinks." + confName + ".sleepInterval is not valid (value found was " + sleepValue + "), defaulting to 10 minutes (600 seconds)";
            log(msg);
            sleepInterval = 600;
        }
        new Thread(this).start();
    }

    protected void synchronize(Map<String, File> sourceMap, Map<String, File> destMap, File destBase) {
        for (String sourceKey : sourceMap.keySet()) {
            File sourceFile = sourceMap.get(sourceKey);
            if (destMap.containsKey(sourceKey)) {
                if (sourceFile.isDirectory()) continue;
                File destFile = destMap.get(sourceKey);
                if (sourceFile.lastModified() > destFile.lastModified()) {
                    log(sourceFile.getAbsolutePath() + " is newer than " + destFile.getAbsolutePath() + ", updating " + destFile.getAbsolutePath());
                    copyFile(sourceFile, destFile);
                }
            } else {
                File destFile = new File(destBase.getAbsolutePath() + sourceKey);
                log("file does not exist, creating " + destFile.getAbsolutePath());
                copyFile(sourceFile, destFile);
            }
        }
    }

    protected void synchronize() {
        masterMap.clear();
        slaveMap.clear();
        String base = masterBase.getAbsolutePath();
        addAll(masterBase, masterBase.getAbsolutePath().length(), masterMap);
        addAll(slaveBase, slaveBase.getAbsolutePath().length(), slaveMap);
        synchronize(masterMap, slaveMap, slaveBase);
        if (twoWay) {
            synchronize(slaveMap, masterMap, masterBase);
        }
    }

    protected boolean verifyOrCreateParentPath(File path) {
        if (path == null) return false;
        if (path.exists()) return true;
        return path.mkdirs();
    }

    protected void copyFile(File sourceFile, File destFile) {
        FileChannel in = null;
        FileChannel out = null;
        try {
            if (!verifyOrCreateParentPath(destFile.getParentFile())) {
                throw new IOException("Parent directory path " + destFile.getAbsolutePath() + " did not exist and could not be created");
            }
            if (destFile.exists() || destFile.createNewFile()) {
                in = new FileInputStream(sourceFile).getChannel();
                out = new FileOutputStream(destFile).getChannel();
                in.transferTo(0, in.size(), out);
            } else {
                throw new IOException("Couldn't create file for " + destFile.getAbsolutePath());
            }
        } catch (IOException ioe) {
            if (destFile.exists() && destFile.length() < sourceFile.length()) {
                destFile.delete();
            }
            ioe.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (Throwable t) {
            }
            try {
                out.close();
            } catch (Throwable t) {
            }
            destFile.setLastModified(sourceFile.lastModified());
        }
    }

    protected void addAll(File parent, int baseLen, Map<String, File> map) {
        for (File child : parent.listFiles()) {
            if (child.isDirectory()) {
                addAll(child, baseLen, map);
            } else {
                String key = child.getAbsolutePath().substring(baseLen).replaceAll("\\\\", "/");
                if (!key.startsWith("/")) key = "/" + key;
                map.put(key, child);
            }
        }
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(sleepInterval * 1000);
            } catch (Exception e) {
            }
            synchronize();
        }
    }
}
