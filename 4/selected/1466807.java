package onepoint.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Helper class used to manipulate I/O streams and execute I/O operations.
 *
 * @author lucian.furtos
 */
public final class XIOHelper {

    private static final int BUFFER_SIZE = 4096;

    /**
    * This is just an "helper class"
    */
    private XIOHelper() {
    }

    /**
    * Serialize a given object
    *
    * @param obj the object to serialize
    * @return the <code>byte[]</code> representation of the object
    * @throws IOException if the serialization fails
    */
    public static byte[] objectToBytes(Object obj) throws IOException {
        byte[] objBytes;
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
            objOut.writeObject(obj);
            objOut.flush();
            objBytes = byteOut.toByteArray();
            objOut.close();
        } catch (IOException e) {
            throw new IOException("Object serialization failed. Reason: " + e);
        }
        return objBytes;
    }

    /**
    * Deserialize an object from an byte array
    *
    * @param bytes an <code>byte[]</code> that contains the data for the object.
    * @return an <code>Object</code> retrieved from the byte array
    * @throws IOException if the deserialization process fails
    */
    public static Object bytesToObject(byte[] bytes) throws IOException {
        ByteArrayInputStream input_stream = new ByteArrayInputStream(bytes);
        Object obj = streamToObject(input_stream);
        input_stream.close();
        return obj;
    }

    /**
    * Deserialize an object from an InputStream
    *
    * @param stream an <code>InputStream</code> that contains the data for the object. Stream is not closed.
    * @return an <code>Object</code> retrieved from the stream
    * @throws IOException if the deserialization process fails
    */
    public static Object streamToObject(InputStream stream) throws IOException {
        try {
            ObjectInputStream objIs = new ObjectInputStream(stream);
            return objIs.readObject();
        } catch (IOException e) {
            throw new IOException("Object deserialization failed. Reason: " + e);
        } catch (ClassNotFoundException e) {
            throw new IOException("Object deserialization failed. Reason: " + e);
        }
    }

    /**
    * Copy all the data from an ImputStream to an OutputStream. The output and input stream is not flushed or closed.
    *
    * @param src the source <code>InputStream</code>
    * @param dst the destination <code>OutputStream</code>
    * @return the number of bytes that are copied
    * @throws java.io.IOException If the copy operation fails due to I/O problems
    */
    public static long copy(InputStream src, OutputStream dst) throws IOException {
        byte[] buff = new byte[BUFFER_SIZE];
        long count = 0L;
        int readcount;
        while ((readcount = src.read(buff)) > -1) {
            dst.write(buff, 0, readcount);
            count += readcount;
        }
        return count;
    }

    /**
    * Copy a specified number of bytes from an ImputStream to an OutputStream. The output and input stream is not flushed or closed.
    *
    * @param src   the source <code>InputStream</code>
    * @param dst   the destination <code>OutputStream</code>
    * @param count the number of bytes to becopied
    * @throws IOException If the copy operation fails due to I/O problems
    */
    public static void copy(InputStream src, OutputStream dst, long count) throws IOException {
        byte[] buff = new byte[BUFFER_SIZE];
        while (count > 0) {
            int toread = count > BUFFER_SIZE ? BUFFER_SIZE : (int) count;
            int readcount = src.read(buff, 0, toread);
            dst.write(buff, 0, readcount);
            count -= readcount;
        }
    }

    /**
    * Creates a zip containing all files in the specified folder
    *
    * @param destination the zip file to create
    * @param directory   the directory to include in zip's content
    * @throws IOException if operation fails.
    */
    public static void zip(File destination, File directory) throws IOException {
        OutputStream fileOut = new FileOutputStream(destination);
        ZipOutputStream zos = new ZipOutputStream(fileOut);
        int pathLength = directory.getAbsolutePath().length();
        createZip(zos, directory, pathLength);
        zos.flush();
        zos.finish();
    }

    private static void createZip(ZipOutputStream zos, File directory, int pathLength) throws IOException {
        File[] fileArray = directory.listFiles();
        for (int i = 0; i < fileArray.length; i++) {
            File currFile = fileArray[i];
            if (currFile.isDirectory()) {
                createZip(zos, currFile, pathLength);
            } else {
                String pathInZip = currFile.getAbsolutePath().substring(pathLength + 1);
                ZipEntry entry = new ZipEntry(pathInZip);
                zos.putNextEntry(entry);
                entry.setMethod(ZipEntry.DEFLATED);
                FileInputStream fis = new FileInputStream(currFile);
                copy(fis, zos);
                fis.close();
                zos.flush();
                zos.closeEntry();
            }
        }
    }

    /**
    * Unzips the source input stream to the specified folder
    *
    * @param destination the foleder where to unzip the stream
    * @param source      the input stream for a ZIP fomated data
    * @param overwrite   flag for overwriting existing files
    * @throws IOException if the source is not valid, the data cannot be read or new files and folder cannot be created
    */
    public static void unzip(File destination, InputStream source, boolean overwrite) throws IOException {
        ZipInputStream zis = new ZipInputStream(source);
        int available = zis.available();
        ZipEntry zipEntry = zis.getNextEntry();
        if (available > 0 && zipEntry == null) {
            throw new ZipException("No zip entry found. Invalid zip file.");
        }
        while (zipEntry != null) {
            if (zipEntry.isDirectory()) {
                File folder = new File(destination, zipEntry.getName());
                if (!folder.exists()) {
                    folder.mkdirs();
                }
            } else {
                File destFile = new File(destination, zipEntry.getName());
                if (!destFile.exists() || overwrite) {
                    destFile.getParentFile().mkdirs();
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destFile));
                    copy(zis, bos);
                    bos.close();
                }
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
    }

    /**
    * Deletes a file or folder from disk
    *
    * @param file the file or folder to remove
    * @return true if operation succeeded.
    */
    public static boolean delete(File file) {
        boolean success = file.isDirectory() ? deleteDir(file) : file.delete();
        if (!success) {
            file.deleteOnExit();
        }
        return success;
    }

    /**
    * Deletes all files and subdirectories under dir.
    * Returns true if all deletions were successful.
    * If a deletion fails, the method stops attempting to delete and returns false.
    */
    public static boolean deleteDir(File dir) {
        boolean success = true;
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean ok = delete(children[i]);
                if (!ok) {
                    success = false;
                }
            }
        }
        if (success) {
            success = dir.delete();
        } else {
            dir.deleteOnExit();
        }
        return success;
    }

    /**
    * Copy a file or folder with all files and subfolders to a destination folder
    *
    * @param src    source file or folder
    * @param dstDir destination folder
    * @throws java.io.IOException throwned if files cannot be copied to the new folder
    */
    public static void copy(File src, File dstDir) throws IOException {
        if (src.isFile()) {
            copyFile(src, new File(dstDir, src.getName()));
        } else {
            File newDir = new File(dstDir, src.getName());
            copyDir(src, newDir);
        }
    }

    /**
    * Copy one file to another
    *
    * @param srcFile source file path
    * @param dstFile destination file path
    * @throws java.io.IOException throwned if the file cannot be copyed.
    */
    public static void copyFile(String srcFile, String dstFile) throws IOException {
        copyFile(new File(srcFile), new File(dstFile));
    }

    /**
    * Copy one file to another
    *
    * @param src  source file
    * @param dest destination file
    * @throws java.io.IOException throwned if the file cannot be copyed.
    */
    public static void copyFile(File src, File dest) throws IOException {
        if (src.getCanonicalPath().equals(dest.getCanonicalPath())) {
            return;
        }
        copyFile(new FileInputStream(src), new FileOutputStream(dest));
    }

    /**
    * Copy one file to another
    *
    * @param src  source file
    * @param dest destination file
    * @throws java.io.IOException throwned if the file cannot be copyed.
    */
    public static void copyFile(InputStream src, OutputStream dest) throws IOException {
        try {
            copy(src, dest);
        } catch (IOException e) {
            closeStream(src);
            closeStream(dest);
        }
    }

    /**
    * Copy a folder with all files and subfolders to a destination folder
    *
    * @param srcDir source folder
    * @param dstDir destination folder
    * @throws java.io.IOException throwned if files cannot be copied to the new folder
    */
    public static void copyDir(File srcDir, File dstDir) throws IOException {
        if (!dstDir.exists()) {
            dstDir.mkdirs();
        }
        File[] fileList = srcDir.listFiles();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].isDirectory()) {
                copyDir(fileList[i], new File(dstDir, fileList[i].getName()));
            } else {
                copyFile(fileList[i], new File(dstDir, fileList[i].getName()));
            }
        }
    }

    /**
    * Closes the input stream if available
    *
    * @param is the input stream to close, can be <code>null</code>
    * @throws java.io.IOException if errors occur
    */
    public static void closeStream(InputStream is) throws IOException {
        if (is != null) {
            is.close();
        }
    }

    /**
    * Closes the output stream if available.
    *
    * @param os the output stream to close, can be <code>null</code>
    * @throws java.io.IOException if any IO errors
    */
    public static void closeStream(OutputStream os) throws IOException {
        if (os != null) {
            os.close();
        }
    }
}
