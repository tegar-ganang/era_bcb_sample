package gcr.mmm2.util;

import java.util.*;
import java.io.*;
import java.util.zip.*;

public class JpegUploadProcessor {

    /**
     * returns true if filename ends with jpeg or jpg
     * 
     * @param filename
     * @return
     */
    public static boolean isJpeg(String filename) {
        if (filename == null || filename.length() < 4) return false;
        filename = filename.toLowerCase();
        String extension = filename.substring(filename.length() - 4);
        if (extension.equals(".jpg") || extension.equals("jpeg")) return true;
        return false;
    }

    /**
     * Returns true if filename string ends with zip
     * 
     * @param filename
     * @return
     */
    public static boolean isZip(String filename) {
        if (filename == null || filename.length() < 4) return false;
        filename = filename.toLowerCase();
        String extension = filename.substring(filename.length() - 4);
        if (extension.equals(".zip")) return true;
        return false;
    }

    /**
     * Takes a filename string and returns the path only
     * 
     * @param filename
     * @return
     */
    public static String pathOnly(String filename) {
        int lastSlash = filename.lastIndexOf("\\");
        int lastSlash2 = filename.lastIndexOf("/");
        lastSlash = (lastSlash > lastSlash2 ? lastSlash : lastSlash2);
        if (lastSlash < 0) return null;
        return filename.substring(0, lastSlash + 1);
    }

    /**
     * Returns an arraylist of all the jpegs found for this file Just returns
     * strings indicating their paths
     * 
     * @param filename
     * @return
     */
    public static ArrayList getJpegs(String filename) {
        String filePathOnly = pathOnly(filename);
        ArrayList photos = new ArrayList();
        if (isJpeg(filename)) {
            photos.add(filename);
        } else if (isZip(filename)) {
            try {
                ZipFile zipFile = new ZipFile(filename);
                Enumeration entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    if ((!entry.isDirectory()) && (isJpeg(entry.getName()))) {
                        System.out.println("Extracting file: " + entry.getName());
                        copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(filePathOnly + entry.getName())));
                        photos.add(filePathOnly + entry.getName());
                    }
                }
                zipFile.close();
                (new File(zipFile.getName())).delete();
            } catch (IOException ioe) {
                System.out.println("Unhandled exception:");
                ioe.printStackTrace();
            }
        }
        return photos;
    }

    /**
     * copies stream to file
     * 
     * @param in
     * @param out
     * @throws IOException
     */
    private static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }
}
