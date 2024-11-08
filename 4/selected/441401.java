package com.jix.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.jix.JixException;

public class UnixInstallerExec {

    private final byte buffer[] = new byte[200 * 1024];

    private final File installDir;

    private final File desktopFolder;

    private final boolean desktopShortcutsEnabled;

    private final Collection textFiles = extractFilesList("/text-files.txt");

    private final Collection executableFiles = extractFilesList("/executable-files.txt");

    private final TreeMap varsMap = new TreeMap();

    public UnixInstallerExec(File javaHome, File installDir, File desktopFolder, boolean desktopShortcutsEnabled) throws IOException {
        this.installDir = installDir;
        this.desktopFolder = desktopFolder;
        this.desktopShortcutsEnabled = desktopShortcutsEnabled;
        this.varsMap.put("JAVAHOME", javaHome.getAbsolutePath());
        this.varsMap.put("INSTALLDIR", installDir.getAbsolutePath());
        this.varsMap.put("DESKTOPDIR", desktopFolder.getAbsolutePath());
    }

    private void uncompress() throws FileNotFoundException, IOException, JixException {
        ZipInputStream zis = new ZipInputStream(UnixInstallerExec.class.getResourceAsStream("/unix-package.zip"));
        ZipEntry zipEntry;
        while ((zipEntry = zis.getNextEntry()) != null) {
            File file = new File(installDir.getPath() + File.separator + zipEntry.getName());
            file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            if (textFiles.contains(zipEntry.getName())) {
                BufferedReader br = new BufferedReader(new InputStreamReader(zis));
                String line;
                while ((line = br.readLine()) != null) fos.write(scapeText(line + "\n").getBytes());
            } else {
                int length;
                while ((length = zis.read(buffer)) != -1) fos.write(buffer, 0, length);
            }
            if (executableFiles.contains(zipEntry.getName())) {
                setExecutable(file);
            }
            fos.close();
            zis.closeEntry();
        }
        zis.close();
    }

    private Collection extractFilesList(String resourceName) throws IOException {
        LinkedList filesList = new LinkedList();
        BufferedReader br = new BufferedReader(new InputStreamReader(UnixInstallerExec.class.getResourceAsStream(resourceName)));
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() > 0 && line.charAt(0) != '#') filesList.add(line);
        }
        return filesList;
    }

    private void setExecutable(File file) throws JixException {
        int exitValue;
        try {
            String[] command = new String[] { "chmod", "a+x", file.getAbsolutePath() };
            exitValue = Runtime.getRuntime().exec(command).waitFor();
        } catch (Exception e) {
            throw new JixException(e);
        }
        if (exitValue != 0) throw new JixException(new RuntimeException("Error during chmod execution: " + exitValue));
    }

    private String scapeText(String line) {
        Iterator i = varsMap.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry entry = (Map.Entry) i.next();
            String var = ":[" + entry.getKey() + "]";
            String value = (String) entry.getValue();
            int index = line.indexOf(var);
            if (index != -1) line = line.substring(0, index) + value + line.substring(index + var.length());
        }
        return line;
    }

    public void copyShortcuts() {
        if (desktopShortcutsEnabled) {
            File[] desktopFiles = new File(installDir.getPath() + File.separator + "jigen-desktop").listFiles();
            for (int i = 0; i < desktopFiles.length; i++) {
                desktopFiles[i].renameTo(new File(desktopFolder.getAbsolutePath() + File.separator + desktopFiles[i].getName()));
            }
        }
    }

    public void install() throws FileNotFoundException, IOException, JixException {
        uncompress();
        copyShortcuts();
    }

    public void dispose() {
    }
}
