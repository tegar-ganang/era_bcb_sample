package org.judo.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class HotDeployer extends Thread {

    private boolean restart = false;

    private String basePath;

    private HashMap<String, Long> restarts = new HashMap<String, Long>();

    private long startUp;

    public HotDeployer() {
        basePath = new File("").getAbsolutePath();
        startUp = (new Date()).getTime();
    }

    public void run() {
        while (true) {
            restart = false;
            checkFiles();
            try {
                if (restart) WebAppServer.redeploy();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
    }

    public void checkFiles() {
        try {
            checkSourceFiles("bin", "server\\webapp\\WEB-INF\\classes");
            checkSourceFiles("webfiles", "server\\webapp");
            checkSourceFiles("applib", "server\\webapp\\WEB-INF\\lib");
            checkWebXml();
            checkConfigFile("runtime/databases.xml");
            checkConfigFile("runtime/db_mapping.properties");
            checkConfigFile("runtime/judo_config.properties");
            checkConfigFile("runtime/judo.properties");
            checkConfigFile("runtime/menu.xml");
            checkConfigFile("log4j/log4j.xml");
            checkConfigFile("runtime/manual_mappings.xml");
            checkConfigFile("runtime/pretty_urls.xml");
            checkConfigFile("runtime/simple_urls.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkConfigFile(String devVersion) throws Exception {
        File devFile = new File(basePath + "\\config\\" + devVersion);
        long last = devFile.lastModified();
        long lastRestart = 0;
        if (restarts.containsKey(devVersion)) lastRestart = restarts.get(devVersion);
        if (lastRestart == 0) lastRestart = startUp;
        if (last > lastRestart) {
            restart = true;
            System.out.println("Been Modified: " + devVersion);
            restarts.put(devVersion, last);
        }
    }

    private void checkWebXml() throws Exception {
        File webXMLDev = new File(basePath + "\\config\\web.xml");
        File webXMLServer = new File(basePath + "\\server\\webapp\\WEB-INF\\web.xml");
        if (webXMLServer.exists()) {
            if (webXMLDev.lastModified() > webXMLServer.lastModified()) {
                copyFile(webXMLDev, webXMLServer);
                restart = true;
            }
        } else {
            copyFile(webXMLDev, webXMLServer);
            restart = true;
        }
    }

    public void checkSourceFiles(String sourceDir, String destDir) throws Exception {
        HashMap sourceFiles = new HashMap();
        HashMap sourceDirs = new HashMap();
        defineFiles(sourceFiles, sourceDirs, sourceDir, sourceDir);
        HashMap destFiles = new HashMap();
        HashMap destDirs = new HashMap();
        defineFiles(destFiles, destDirs, destDir, destDir);
        Iterator sourceDirList = sourceDirs.keySet().iterator();
        while (sourceDirList.hasNext()) {
            String filePath = (String) sourceDirList.next();
            ProjectFile dir = (ProjectFile) sourceDirs.get(filePath);
            if (filePath.contains(".svn")) continue;
            if (!destDirs.containsKey(dir.relativePath)) {
                String createDir = basePath + "\\" + destDir + dir.relativePath;
                File newDir = new File(createDir);
                newDir.mkdirs();
                restart = true;
            }
        }
        Iterator sourceFileList = sourceFiles.keySet().iterator();
        while (sourceFileList.hasNext()) {
            String filePath = (String) sourceFileList.next();
            ProjectFile file = (ProjectFile) sourceFiles.get(filePath);
            if (!destDirs.containsKey(file.relativePath)) {
                String from = basePath + "\\" + sourceDir + file.relativePath;
                String to = basePath + "\\" + destDir + file.relativePath;
                if (from.contains(".svn")) continue;
                if (!destFiles.containsKey(file.relativePath)) {
                    File fromFile = new File(from);
                    File toFile = new File(to);
                    copyFile(fromFile, toFile);
                    restart = true;
                } else {
                    ProjectFile destFile = (ProjectFile) destFiles.get(file.relativePath);
                    if (file.lastModified > destFile.lastModified) {
                        System.out.println("Been Modified: " + file.relativePath);
                        File fromFile = new File(from);
                        File toFile = new File(to);
                        copyFile(fromFile, toFile);
                        restart = true;
                    }
                }
            }
        }
        File deleteCheckDirs = new File(basePath + "\\" + destDir);
        File dirs[] = deleteCheckDirs.listFiles();
        for (int i = 0; i < dirs.length; i++) {
            File cur = dirs[i];
            checkForDelete(cur, sourceDirs, sourceFiles, destDir);
        }
    }

    private void checkForDelete(File cur, HashMap sourceDirs, HashMap sourceFiles, String startingDir) {
        if (cur.getAbsolutePath().contains("WEB-INF") && startingDir.equals("server\\webapp")) return;
        if (cur.getAbsolutePath().contains("judo.jar")) return;
        if (cur.getAbsolutePath().contains("jdom.jar")) return;
        if (cur.getAbsolutePath().contains("log4j")) return;
        if (cur.getAbsolutePath().contains("environment.properties")) return;
        String curBase = basePath + "\\" + startingDir;
        String relativePath = cur.getAbsolutePath().replace(curBase, "");
        if (cur.isDirectory()) {
            File dirs[] = cur.listFiles();
            for (int i = 0; i < dirs.length; i++) {
                File child = dirs[i];
                checkForDelete(child, sourceDirs, sourceFiles, startingDir);
            }
            if (cur.list().length == 0) {
                if (!sourceDirs.containsKey(relativePath)) {
                    System.out.println("deleteing dir: " + cur.getAbsolutePath());
                    cur.delete();
                    restart = true;
                }
            }
        } else {
            if (!sourceFiles.containsKey(relativePath)) {
                System.out.println("deleteing file: " + cur.getAbsolutePath());
                cur.delete();
                restart = true;
            }
        }
    }

    protected void defineFiles(HashMap files, HashMap dirs, String directory, String startingDir) {
        File dir = new File(directory);
        File fileList[] = dir.listFiles();
        for (int i = 0; i < fileList.length; i++) {
            File cur = fileList[i];
            try {
                if (cur.exists()) {
                    String curBase = basePath + "\\" + startingDir;
                    String relativePath = cur.getAbsolutePath().replace(curBase, "");
                    ProjectFile pf = new ProjectFile(cur.getName(), cur.getAbsolutePath(), relativePath, cur.lastModified(), cur);
                    if (cur.isDirectory()) dirs.put(relativePath, pf); else files.put(relativePath, pf);
                    if (cur.isDirectory()) {
                        defineFiles(files, dirs, directory + "\\" + cur.getName(), startingDir);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error reading file: " + cur);
            }
        }
    }

    class ProjectFile {

        File file;

        String name;

        String fullPath;

        String relativePath;

        long lastModified;

        public ProjectFile(String name, String fullPath, String relativePath, long lastModified, File file) {
            super();
            this.name = name;
            this.fullPath = fullPath;
            this.relativePath = relativePath;
            this.lastModified = lastModified;
            this.file = file;
        }
    }

    public void copyFile(File in, File out) throws Exception {
        FileChannel sourceChannel = new FileInputStream(in).getChannel();
        FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }
}
