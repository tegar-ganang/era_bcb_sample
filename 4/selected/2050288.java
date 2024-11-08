package com.hs.framework.db.jdbc;

import java.io.*;
import java.net.URL;
import com.hs.framework.common.util.LogUtil;

public final class FileUtil {

    private static FileUtil instance = new FileUtil();

    private static String servletClassesPath = null;

    private FileUtil() {
    }

    public static void createDir(String dir, boolean ignoreIfExitst) throws IOException {
        File file = new File(dir);
        if (ignoreIfExitst && file.exists()) {
            return;
        }
        if (file.mkdir() == false) {
            throw new IOException("Cannot create the directory = " + dir);
        }
    }

    public static void createDirs(String dir, boolean ignoreIfExitst) throws IOException {
        File file = new File(dir);
        if (ignoreIfExitst && file.exists()) {
            return;
        }
        if (file.mkdirs() == false) {
            throw new IOException("Cannot create directories = " + dir);
        }
    }

    public static void deleteFile(String filename) throws IOException {
        File file = new File(filename);
        LogUtil.getLogger().info("Delete file = " + filename);
        if (file.isDirectory()) {
            throw new IOException("IOException -> BadInputException: not a file.");
        }
        if (file.exists() == false) {
            throw new IOException("IOException -> BadInputException: file is not exist.");
        }
        if (file.delete() == false) {
            throw new IOException("Cannot delete file. filename = " + filename);
        }
    }

    public static void deleteDir(File dir) throws IOException {
        if (dir.isFile()) throw new IOException("IOException -> BadInputException: not a directory.");
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isFile()) {
                    file.delete();
                } else {
                    deleteDir(file);
                }
            }
        }
        dir.delete();
    }

    public static long getDirLength(File dir) throws IOException {
        if (dir.isFile()) throw new IOException("BadInputException: not a directory.");
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                long length = 0;
                if (file.isFile()) {
                    length = file.length();
                } else {
                    length = getDirLength(file);
                }
                size += length;
            }
        }
        return size;
    }

    public static long getDirLength_onDisk(File dir) throws IOException {
        if (dir.isFile()) throw new IOException("BadInputException: not a directory.");
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                long length = 0;
                if (file.isFile()) {
                    length = file.length();
                } else {
                    length = getDirLength_onDisk(file);
                }
                double mod = Math.ceil(((double) length) / 512);
                if (mod == 0) mod = 1;
                length = ((long) mod) * 512;
                size += length;
            }
        }
        return size;
    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
        byte[] block = new byte[512];
        while (true) {
            int readLength = inputStream.read(block);
            if (readLength == -1) break;
            byteArrayOutputStream.write(block, 0, readLength);
        }
        byte[] retValue = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();
        return retValue;
    }

    public static void outPutFile(InputStream is, String outPath) throws IOException {
        OutputStream outputStream = null;
        try {
            byte[] srcByte = getBytes(is);
            outputStream = new FileOutputStream(outPath);
            outputStream.write(srcByte);
        } catch (IOException e) {
            LogUtil.getLogger().error("Error", e);
            throw e;
        } finally {
            is.close();
            if (outputStream != null) outputStream.close();
        }
    }

    public static String getFileName(String fullFilePath) {
        if (fullFilePath == null) {
            return "";
        }
        int index1 = fullFilePath.lastIndexOf('/');
        int index2 = fullFilePath.lastIndexOf('\\');
        int index = (index1 > index2) ? index1 : index2;
        if (index == -1) {
            return fullFilePath;
        }
        String fileName = fullFilePath.substring(index + 1);
        return fileName;
    }

    /**
     * This method could be used to override the path to WEB-INF/classes
     * It can be set when the web app is inited
     * @param path String : new path to override the default path
     */
    public static void setServletClassesPath(String path) {
        LogUtil.getLogger().debug("FileUtil.setServletClassesPath called with path = " + path);
        servletClassesPath = path;
        if (servletClassesPath.endsWith(File.separator) == false) {
            servletClassesPath = servletClassesPath + File.separatorChar;
            LogUtil.getLogger().debug("FileUtil.setServletClassesPath change path to value = " + servletClassesPath);
        }
    }

    /**
     * This function is used to get the classpath of a reference of one class
     * First, this method tries to get the path from system properties
     * named "mvncore.context.path" (can be configed in web.xml). If it cannot
     * find this parameter, then it will tries to load from the ClassLoader
     * @todo FIXME: load from ClassLoader is not correct on Resin/Linux
     */
    public static String getServletClassesPath() {
        if (servletClassesPath == null) {
            String strPath = System.getProperty("mvncore.context.path");
            if (strPath != null && (strPath.length() > 0)) {
                servletClassesPath = strPath;
            } else {
                ClassLoader classLoader = instance.getClass().getClassLoader();
                URL url = classLoader.getResource("/");
                servletClassesPath = url.getPath();
            }
            LogUtil.getLogger().debug("servletClassesPath = " + servletClassesPath);
            if (servletClassesPath.endsWith(File.separator) == false) {
                servletClassesPath = servletClassesPath + File.separatorChar;
            }
        }
        return servletClassesPath;
    }
}
