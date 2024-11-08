package common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtilities {

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static String readFileAsString(String filePath) throws java.io.IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            fileData.append(buf, 0, numRead);
        }
        reader.close();
        return fileData.toString();
    }

    public static void writeFileFromString(String filePath, String contents) throws java.io.IOException {
        FileWriter fstream = new FileWriter(filePath);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write(contents);
        out.close();
    }

    public static String safeRename(File oldFile, File newFile) {
        newFile = new File(newFile.getParent() + File.separator + safeFileName(newFile.getName()));
        String sRoot = Profile.getStackRoot(newFile);
        String sAppendage = newFile.getName().substring(sRoot.length());
        String parentDir = newFile.getParent();
        int nAlt = 0;
        String sNewFile = newFile.getAbsolutePath();
        while (newFile.exists()) {
            sNewFile = parentDir + File.separator + sRoot + String.format("_%02d", nAlt) + sAppendage;
            nAlt++;
            newFile = new File(sNewFile);
        }
        boolean x = oldFile.renameTo(newFile);
        return (newFile.getAbsolutePath());
    }

    public static String renameMovie(File oldFile, File newFile) {
        if (oldFile.equals(newFile)) {
            return newFile.getAbsolutePath();
        }
        return safeRename(oldFile, newFile);
    }

    public static String renameMovieSet(File oldFile, File newFile, boolean checkStacking) {
        if (oldFile.equals(newFile)) {
            return newFile.getAbsolutePath();
        }
        newFile.getParentFile().mkdirs();
        newFile = new File(safeRename(oldFile, newFile));
        File oldDir = oldFile.getParentFile();
        File newDir = newFile.getParentFile();
        File[] files = oldDir.listFiles();
        String sCheckRoot = Profile.getStackRoot(oldFile);
        String sFanArt = sCheckRoot + "-fanart";
        String sTrailer = sCheckRoot + "-trailer";
        if (files != null) {
            for (File checkFile : files) {
                String sBaseName = getNameOnly(checkFile);
                String sExtension = getExtension(checkFile);
                if (sBaseName.equalsIgnoreCase(sCheckRoot)) {
                    if (Profile.getValidExtensions().contains(sExtension)) {
                        continue;
                    }
                    File renameFile = null;
                    renameFile = new File(newDir.getAbsolutePath() + File.separator + sBaseName + "." + sExtension);
                    checkFile.getParentFile().mkdirs();
                    checkFile.renameTo(renameFile);
                } else if (sBaseName.equalsIgnoreCase(sFanArt)) {
                    File renameFile = new File(newDir.getAbsolutePath() + File.separator + sFanArt + "." + sExtension);
                    checkFile.getParentFile().mkdirs();
                    checkFile.renameTo(renameFile);
                } else if (sBaseName.equalsIgnoreCase(sTrailer)) {
                    File renameFile = new File(newDir.getAbsolutePath() + File.separator + sTrailer + "." + sExtension);
                    checkFile.getParentFile().mkdirs();
                    checkFile.renameTo(renameFile);
                }
            }
        }
        return newFile.getAbsolutePath();
    }

    public static final void copy(File source, File destination) throws IOException {
        if (source.isDirectory()) {
            copyDirectory(source, destination);
        } else {
            copyFile(source, destination);
        }
    }

    public static final void copyDirectory(File source, File destination) throws IOException {
        if (!source.isDirectory()) {
            throw new IllegalArgumentException("Source (" + source.getPath() + ") must be a directory.");
        }
        if (!source.exists()) {
            throw new IllegalArgumentException("Source directory (" + source.getPath() + ") doesn't exist.");
        }
        if (destination.exists()) {
            throw new IllegalArgumentException("Destination (" + destination.getPath() + ") exists.");
        }
        destination.mkdirs();
        File[] files = source.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                copyDirectory(file, new File(destination, file.getName()));
            } else {
                copyFile(file, new File(destination, file.getName()));
            }
        }
    }

    public static final void copyFile(File source, File destination) throws IOException {
        FileChannel sourceChannel = new FileInputStream(source).getChannel();
        FileChannel targetChannel = new FileOutputStream(destination).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), targetChannel);
        sourceChannel.close();
        targetChannel.close();
    }

    private static void iterate(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (bIncludeDirectories) {
                        nFileCount++;
                    }
                    iterate(file);
                } else {
                    nFileCount++;
                }
            }
        }
    }

    static int nFileCount = 0;

    static boolean bIncludeDirectories;

    public static int getFileCount(File dir, boolean includeDirectories) {
        bIncludeDirectories = includeDirectories;
        nFileCount = 0;
        iterate(dir);
        return nFileCount;
    }

    public static String getNameOnly(File file) {
        String strFileName = file.getName();
        int dotPlace = strFileName.lastIndexOf('.');
        if (dotPlace >= 0) {
            return strFileName.substring(0, dotPlace);
        } else {
            return strFileName;
        }
    }

    public static void cleanupMovie(String movie) {
        File cleanup = new File(movie);
        String sCname = Profile.getStackRoot(cleanup);
        File nfo = new File(cleanup.getParent() + File.separator + sCname + ".nfo");
        nfo.delete();
        File tbn = new File(cleanup.getParent() + File.separator + sCname + ".tbn");
        tbn.delete();
        File fanart = new File(cleanup.getParent() + File.separator + sCname + "-fanart.jpg");
        fanart.delete();
        File trailer = new File(cleanup.getParent() + File.separator + sCname + "-trailer.flv");
        trailer.delete();
    }

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        if (ext == null) {
            return "";
        }
        return ext;
    }

    static final Pattern pattern = Pattern.compile("[\\\\:/?\"'<>*]");

    public static String safeFileName(String fileName) {
        final Matcher matcher = pattern.matcher(fileName);
        return matcher.replaceAll("_");
    }
}
