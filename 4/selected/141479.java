package jFileLib.common;

public class File {

    public boolean createNewFile(String path) {
        if (Path.isValidePath(path) == false) return false;
        try {
            java.io.File file = new java.io.File(path);
            return file.createNewFile();
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean rename(String path, String newName) {
        return false;
    }

    public static boolean delete(String path) {
        if (Path.isFile(path) == false && File.canWrite(path) == false) return false;
        java.io.File file = new java.io.File(path);
        return file.delete();
    }

    public static boolean exists(String path) {
        if (Path.isFile(path) == false) return false;
        java.io.File file = new java.io.File(path);
        return file.exists();
    }

    public static boolean isHidden(String path) {
        if (Path.isFile(path) == false) return false;
        java.io.File file = new java.io.File(path);
        return file.isHidden();
    }

    public static boolean canRead(String path) {
        if (Path.isFile(path) == false) return false;
        java.io.File file = new java.io.File(path);
        return file.canRead();
    }

    public static boolean canWrite(String path) {
        if (Path.isFile(path) == false) return false;
        java.io.File file = new java.io.File(path);
        return file.canWrite();
    }

    public static boolean canExecute(String path) {
        if (Path.isFile(path) == false) return false;
        java.io.File file = new java.io.File(path);
        return file.canExecute();
    }

    public static String getFileExtension(String filePath) {
        String[] parts = filePath.split("\\.");
        String ext = null;
        if (parts != null) ext = parts[parts.length - 1];
        return ext;
    }

    public static String getName(String path) {
        if (Path.isFile(path) == false) return null;
        java.io.File file = new java.io.File(path);
        return file.getName();
    }

    public static void copyTo(String source, String dest) throws Exception {
        java.io.File sourceFile = new java.io.File(source);
        java.io.File destFile = new java.io.File(dest);
        copyTo(sourceFile, destFile);
    }

    public static void copyTo(java.io.File source, java.io.File dest) throws Exception {
        java.io.FileInputStream inputStream = null;
        java.nio.channels.FileChannel sourceChannel = null;
        java.io.FileOutputStream outputStream = null;
        java.nio.channels.FileChannel destChannel = null;
        long size = source.length();
        long bufferSize = 1024;
        long count = 0;
        if (size < bufferSize) bufferSize = size;
        Exception exception = null;
        try {
            if (dest.exists() == false) dest.createNewFile();
            inputStream = new java.io.FileInputStream(source);
            sourceChannel = inputStream.getChannel();
            outputStream = new java.io.FileOutputStream(dest);
            destChannel = outputStream.getChannel();
            while (count < size) count += sourceChannel.transferTo(count, bufferSize, destChannel);
        } catch (Exception e) {
            exception = e;
        } finally {
            closeFileChannel(sourceChannel);
            closeFileChannel(destChannel);
        }
        if (exception != null) throw exception;
    }

    private static void closeFileChannel(java.nio.channels.FileChannel channel) {
        try {
            if (channel != null) channel.close();
        } catch (Exception e) {
        }
    }
}
