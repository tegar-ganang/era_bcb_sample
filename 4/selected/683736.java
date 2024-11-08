package net.mjrz.fm.utils;

import java.io.*;
import java.util.zip.*;
import java.util.*;
import org.apache.log4j.Logger;
import net.mjrz.fm.Main;

public class ImportProfileUtil {

    private File importFile = null;

    private ArrayList<String> fileNames = null;

    private static Logger logger = Logger.getLogger(ImportProfileUtil.class.getName());

    private String destinationDirectory = null;

    public ImportProfileUtil(File importFile, File destination) {
        this.importFile = importFile;
        this.destinationDirectory = destination.getAbsolutePath();
        fileNames = new ArrayList<String>();
    }

    public void importFile() {
        try {
            loadFileNames();
            String pname = getProfileName();
            logger.info("Importing profile " + pname);
            logger.info("Importing to " + getBaseDirectory(pname));
            unzip();
            Profiles.getInstance().addProfile(pname, destinationDirectory + Main.PATH_SEPARATOR + pname);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unzip() throws Exception {
        FileInputStream fis = new FileInputStream(importFile);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            copyInputStream(zis, ze.getName());
        }
        zis.close();
    }

    private final void copyInputStream(InputStream in, String zipEntryName) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        File dirs = new File(destinationDirectory);
        File f = new File(dirs, zipEntryName);
        makeDirs(f.getAbsolutePath(), zipEntryName);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        out.close();
    }

    private void makeDirs(String path, String zipEntryName) {
        int pos = path.lastIndexOf(Main.PATH_SEPARATOR);
        if (pos >= 0) {
            String dirPath = path.substring(0, pos);
            File dir = new File(dirPath);
            dir.mkdirs();
        }
    }

    private void loadFileNames() throws Exception {
        FileInputStream fis = new FileInputStream(importFile);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            String name = ze.getName();
            fileNames.add(name);
            zis.closeEntry();
        }
        zis.close();
    }

    private String getBaseDirectory(String profileName) {
        StringBuilder homedir = new StringBuilder(destinationDirectory);
        homedir.append(Main.PATH_SEPARATOR);
        homedir.append(profileName);
        return homedir.toString();
    }

    private String getProfileName() {
        if (fileNames.size() == 0) return null;
        String first = fileNames.get(0);
        int pos = first.indexOf(Main.PATH_SEPARATOR);
        if (pos >= 0) return first.substring(0, pos);
        return null;
    }
}
