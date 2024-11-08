package org.open.force.common;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

/**
 * @author nlugert@openforcesoftware.org
 *
 */
public class OFFileUtil {

    public static String getFileAsString(String fileName) {
        return new String(readFileToByteArray(fileName));
    }

    public static byte[] readFileToByteArray(String fileName) {
        DataInputStream dis;
        try {
            File file = new File(fileName);
            dis = new DataInputStream(new FileInputStream(file));
            if (file.length() <= Integer.MAX_VALUE) {
                byte[] data = new byte[(int) file.length()];
                dis.readFully(data);
                dis.close();
                return data;
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveFile(String fileName, String data) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
            out.write(data);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyFile(String inFile, String outFile) {
        File in = new File(inFile);
        File out = new File(outFile);
        try {
            FileChannel inChannel = new FileInputStream(in).getChannel();
            FileChannel outChannel = new FileOutputStream(out).getChannel();
            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } finally {
                if (inChannel != null) inChannel.close();
                if (outChannel != null) outChannel.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteFile(String fileName) {
        File f = new File(fileName);
        if (f.exists()) {
            if (!f.delete()) throw new RuntimeException("Unable to delete file " + fileName);
        }
    }

    public static String getDirectoryListing(String dirName) {
        File dir = new File(dirName);
        if (!dir.isDirectory()) throw new RuntimeException(dirName + " is not a directory!");
        StringBuffer listing = new StringBuffer();
        String[] files = dir.list();
        for (int i = 0; i < files.length; i++) {
            File f = new File(files[i]);
            if (!f.isDirectory() && !f.isHidden()) {
                listing.append(files[i] + System.getProperty("line.separator"));
            }
        }
        return listing.toString();
    }

    public static String[] getFileNamesInDirectory(String dirName) {
        File dir = new File(dirName);
        if (!dir.isDirectory()) throw new RuntimeException(dirName + " is not a directory!");
        return dir.list();
    }

    public static void deleteAllFilesInDirectory(String dirName) {
        String[] allFiles = getFileNamesInDirectory(dirName);
        if (allFiles != null && allFiles.length > 0) {
            for (int i = 0; i < allFiles.length; i++) {
                File f = new File(dirName + File.separator + allFiles[i]);
                if (f.exists()) f.delete();
            }
        }
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }
}
