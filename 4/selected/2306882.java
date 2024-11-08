package org.softmed.filehandling;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.io.*;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Map;
import org.softmed.filehandling.cache.FileCache;

public class FileUtil {

    private static Map<String, String> filePathCache = new HashMap<String, String>();

    private static Map<String, String> getFilePathCache = new HashMap<String, String>();

    private static FileCache fileCache;

    private static String hackRootPath;

    public static String getHackRootPath() {
        return hackRootPath;
    }

    public static void resetFilePathCache() {
        filePathCache.clear();
        getFilePathCache.clear();
    }

    public static void setHackRootPath(String hackRootPath) {
        FileUtil.hackRootPath = hackRootPath;
    }

    private String rootPath;

    public FileUtil() {
        if (hackRootPath != null) {
            rootPath = hackRootPath;
            return;
        }
        try {
            rootPath = (new File(".")).getCanonicalPath() + "/";
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public FileUtil(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getRealPath(String path) {
        try {
            return this.getClass().getClassLoader().getResource(path).toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return path;
    }

    public String fixPath(String path) {
        if (path.equals(".")) return "/";
        String correctPath = filePathCache.get(path);
        if (correctPath != null) return correctPath;
        correctPath = path;
        int index = path.indexOf(".\\");
        if (index >= 0) correctPath = correctPath.substring(index + 2);
        correctPath = correctPath.replace('\\', '/');
        correctPath = correctPath.replace("//", "/");
        filePathCache.put(path, correctPath);
        return correctPath;
    }

    public File getFile(String path) {
        String cpath = getFilePathCache.get(path);
        if (cpath != null) return new File(cpath);
        cpath = fixPath(path);
        File file = null;
        cpath = rootPath + cpath;
        file = new File(cpath);
        getFilePathCache.put(path, cpath);
        return file;
    }

    public void saveToFile(File file, String text) throws IOException, FileNotFoundException {
        if (!file.exists()) file.createNewFile();
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        out.write(text);
        out.flush();
        out.close();
    }

    public void saveToFile(String path, String text) throws Throwable {
        File file = getFile(path);
        saveToFile(file, text);
    }

    public String readFromFile(String path) throws Throwable {
        if (fileCache == null) {
            File file = getFile(path);
            return actuallyReadContent(file);
        }
        String content = fileCache.readFromFile(path, this);
        if (content != null) return content;
        return actuallyReadContent(getFile(path));
    }

    public String readFromFile(File file) throws Throwable {
        if (fileCache == null) return actuallyReadContent(file); else {
            String content = fileCache.readFromFile(file, this);
            if (content != null) return content;
            return actuallyReadContent(file);
        }
    }

    public String actuallyReadContent(String path) throws Throwable {
        return actuallyReadContent(getFile(path));
    }

    public String actuallyReadContent(File file) throws Throwable {
        BufferedReader inStream = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        String text = "";
        String line = null;
        while ((line = inStream.readLine()) != null) {
            text += line + '\n';
        }
        inStream.close();
        return text;
    }

    public void deleteFile(String path) throws Throwable {
        File file = getFile(path);
        deleteFile(file);
    }

    public void deleteFile(File file) {
        if (!file.exists()) throw new RuntimeException("File " + file.getAbsolutePath() + " wasn't found!");
        file.delete();
    }

    public void copyFile(String in, String out) throws IOException {
        copyFile(getFile(in), getFile(out));
    }

    public void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            int maxCount = (64 * 1024 * 1024) - (32 * 1024);
            long size = inChannel.size();
            long position = 0;
            while (position < size) {
                position += inChannel.transferTo(position, maxCount, outChannel);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    public String getRootPath() {
        return rootPath;
    }

    public static FileCache getFileCache() {
        return fileCache;
    }

    public static void setFileCache(FileCache fileCache) {
        FileUtil.fileCache = fileCache;
    }

    public boolean isCacheValid(String filePath) {
        if (fileCache == null) return false;
        return fileCache.isCacheValid(filePath);
    }
}
