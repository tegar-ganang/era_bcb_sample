package osdep.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Code Copied from Matrex (I had to maintain the library separated)
 * Unzips a file to a directory 
 * @author SHZ Mar 10, 2008
 */
public class Unzipper {

    public static void unzip(String dirPath, ZipInputStream zipStream) throws ZipException {
        ZipEntry entry = null;
        try {
            while ((entry = zipStream.getNextEntry()) != null) {
                String entryPath = entry.getName();
                String completePath = dirPath + File.separator + entryPath;
                if (entry.isDirectory()) {
                    File newDir = new File(completePath);
                    if (!newDir.exists()) if (!newDir.mkdirs()) throw new IOException("Impossible to create the directory " + completePath);
                } else {
                    File file = new File(completePath);
                    File parentDir = file.getParentFile();
                    if (!parentDir.exists()) if (!parentDir.mkdirs()) throw new IOException("Impossible to create the directory " + parentDir.getAbsolutePath());
                    OutputStream outStream = new BufferedOutputStream(new FileOutputStream(file));
                    try {
                        copyInputStream(zipStream, outStream);
                    } finally {
                        outStream.close();
                    }
                }
            }
            zipStream.close();
        } catch (IOException e) {
            throw new ZipException("Unzipping the entry " + entry + " to the directory " + dirPath + " got", e);
        }
    }

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
    }
}
