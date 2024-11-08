package org.eoti.io.file;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class FileUtil {

    public static void copy(File from, File to) throws FileNotFoundException, IOException {
        FileInputStream fromStream = new FileInputStream(from);
        FileOutputStream toStream = new FileOutputStream(to);
        copy(fromStream, toStream);
        fromStream.close();
        toStream.close();
    }

    public static void copy(FileInputStream from, FileOutputStream to) throws IOException {
        FileChannel fromChannel = from.getChannel();
        FileChannel toChannel = to.getChannel();
        copy(fromChannel, toChannel);
        fromChannel.close();
        toChannel.close();
    }

    public static void copy(FileChannel from, FileChannel to) throws IOException {
        from.transferTo(0, from.size(), to);
    }

    public static void copy(InputStream resource, File to) throws IOException {
        FileChannel channel = (new FileOutputStream(to)).getChannel();
        byte[] bytes = loadBytes(resource);
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        channel.write(buf);
        channel.close();
    }

    public static ArrayList<File> recurse(File fromDir, File toDir) {
        ArrayList<File> dirs = new ArrayList<File>();
        if (!toDir.isDirectory()) toDir = toDir.getParentFile();
        if (!fromDir.isDirectory()) fromDir = fromDir.getParentFile();
        File dir = toDir;
        while (!dir.getAbsolutePath().equals(fromDir.getAbsolutePath())) {
            dirs.add(dir);
            dir = dir.getParentFile();
        }
        dirs.add(fromDir);
        Collections.reverse(dirs);
        return dirs;
    }

    public static byte[] loadFileBytes(File file) throws IOException {
        return loadBytes(new FileInputStream(file));
    }

    public static byte[] loadFileBytes(String fileName) throws IOException {
        return loadBytes(new FileInputStream(fileName));
    }

    public static byte[] loadBytes(InputStream stream) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(stream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean done = false;
        while (!done) {
            int b = bis.read();
            if (b == -1) done = true; else baos.write(b);
        }
        baos.flush();
        bis.close();
        return baos.toByteArray();
    }

    public static String getFileExtension(File file) {
        return getFileExtension(file.getName());
    }

    public static String getFileExtension(String fileName) {
        int lastPeriod = fileName.lastIndexOf(".");
        if (lastPeriod == -1) return null;
        if ((lastPeriod + 1) > (fileName.length() - 1)) return null;
        return fileName.substring(lastPeriod + 1);
    }

    public static void touch(File file) {
        file.setLastModified(new Date().getTime());
    }
}
