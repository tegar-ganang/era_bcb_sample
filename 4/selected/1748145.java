package org.dant.ant.extension.utils;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public final class FileUtil {

    private FileUtil() {
    }

    public static void writeFile(byte[] fileBuffer, String fullDestPath) throws Exception {
        ByteBuffer rBuf = ByteBuffer.allocate(fileBuffer.length);
        rBuf.put(fileBuffer);
        rBuf.rewind();
        WritableByteChannel channel = new FileOutputStream(fullDestPath).getChannel();
        channel.write(rBuf);
        channel.close();
    }

    public static void writeFile(byte[] fileBuffer, String fullDestPath, int position, boolean complete) throws Exception {
        String partFileName = fullDestPath + "-part";
        ByteBuffer rBuf = null;
        rBuf = ByteBuffer.allocate(fileBuffer.length);
        rBuf.put(fileBuffer);
        rBuf.flip();
        RandomAccessFile fos = new RandomAccessFile(partFileName, "rw");
        FileChannel channel = fos.getChannel();
        int written = 0;
        if (position == -1) {
            written = channel.write(rBuf);
        } else {
            written = channel.write(rBuf, (long) position);
        }
        fos.close();
        channel.close();
        System.out.println("Written " + written + " bytes from " + (long) position);
        System.out.println("Transfered  " + (position + written) + " bytes >>> " + partFileName);
        if (complete) {
            System.out.println((position + written) + " bytes transfered, Transfer completed!");
            File des = new File(fullDestPath);
            if (des.exists()) {
                System.out.println("Target file " + fullDestPath + " alread exist, deleting the file first");
                if (des.delete()) {
                    System.out.println("File " + fullDestPath + " deleted");
                } else {
                    System.out.println("Failed deleting file " + fullDestPath);
                }
            }
            File src = new File(partFileName);
            src.renameTo(des);
            System.out.println("File is stored in >>> " + fullDestPath);
        }
    }

    public static byte[] readFile(String fullFilename, int position, int length) throws IOException {
        FileChannel channel = new FileInputStream(fullFilename).getChannel();
        int chunksize = 0;
        ByteBuffer rBuf = null;
        if (position == -1 && length == -1) {
            rBuf = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int) channel.size());
            chunksize = (int) channel.size();
        } else {
            rBuf = channel.map(FileChannel.MapMode.READ_ONLY, position, length);
            chunksize = length;
        }
        byte[] fileBuffer = null;
        fileBuffer = new byte[chunksize];
        rBuf.rewind();
        rBuf.get(fileBuffer);
        channel.close();
        return fileBuffer;
    }

    public static byte[] readFile(String fullFilename) throws IOException {
        byte[] fileBuffer = null;
        FileChannel channel = new FileInputStream(fullFilename).getChannel();
        ByteBuffer rBuf = channel.map(FileChannel.MapMode.READ_ONLY, 0, (int) channel.size());
        fileBuffer = new byte[(int) channel.size()];
        rBuf.rewind();
        rBuf.get(fileBuffer);
        channel.close();
        return fileBuffer;
    }

    public static String resolveFileName(String dir, String file) {
        StringBuffer fullname = new StringBuffer();
        fullname.append(dir);
        if (!dir.endsWith("/") && !dir.endsWith("\\")) {
            fullname.append(System.getProperty("file.separator"));
        }
        fullname.append(file);
        return fullname.toString();
    }

    public static void printBytes(byte[] b) {
        for (int i = 0; i < b.length; i++) {
            System.out.print((char) b[i]);
        }
    }
}
