package br.com.NoTraffic.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Util {

    public static byte[] getFileContent(String fileLocation) throws FileNotFoundException, IOException {
        ByteArrayOutputStream baos = null;
        InputStream is = new FileInputStream(fileLocation);
        baos = new ByteArrayOutputStream();
        while (is.available() > 0) {
            baos.write(is.read());
        }
        return baos.toByteArray();
    }

    public static void saveFile(String fileLocation, byte[] fileContent) throws FileNotFoundException, IOException {
        FileOutputStream mapFos = new FileOutputStream(fileLocation);
        mapFos.write(fileContent);
        mapFos.flush();
        mapFos.close();
    }

    public static void createDirectory(String dirLocation) throws FileSystemException {
        File directory = new File(dirLocation);
        if (!directory.exists()) {
            boolean success = directory.mkdirs();
            if (!success) {
                throw new FileSystemException();
            }
        }
    }

    public static void deleteDirectoryAndItsContent(String dirLocation) throws FileSystemException {
        File directory = new File(dirLocation);
        File[] files = directory.listFiles();
        boolean success = true;
        for (File f : files) {
            if (f.isDirectory()) {
                deleteDirectoryAndItsContent(dirLocation + "/" + f.getName());
            }
            success = f.delete();
            if (!success) {
                throw new FileSystemException();
            }
        }
        success = directory.delete();
        if (!success) {
            throw new FileSystemException();
        }
    }

    public byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
        }
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
