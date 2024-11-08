package modmanager.utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.Logger;

/**
 * Operations with the .hondmod files.
 * @author Shirkit
 */
public class ZIP {

    static Logger logger = Logger.getLogger(ZIP.class.getPackage().getName());

    /**
     * Retrives only one file given by it's relative path and name and retrives a byte array of it.
     * @param zip is the zip file to search in.
     * @param filename is the file to look for.
     * @return byte[] of the file.
     * @throws IOException if an I/O error has occurred.
     * @throws FileNotFoundException if a file is missing. Use the Exception.getMessage(). Or the zip file wasn't found, or the filename wasn't found inside the zip.
     * @throws ZipException if a random ZipException occourred.
     */
    public static byte[] getFile(File zip, String fileName) throws FileNotFoundException, ZipException, IOException {
        String filename = fileName;
        if (!zip.exists()) {
            throw new FileNotFoundException(zip.getName());
        }
        while (filename.charAt(0) == '/' || filename.charAt(0) == '\\') {
            filename = filename.substring(1);
        }
        if (filename.contains("\\")) {
            filename = filename.replace("\\", "/");
        }
        ZipFile zipFile = new ZipFile(zip);
        Enumeration entries = zipFile.entries();
        ByteArrayOutputStream output;
        byte[] result = null;
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().equalsIgnoreCase(filename)) {
                FileUtils.copyInputStream(zipFile.getInputStream(entry), output = new ByteArrayOutputStream());
                result = output.toByteArray();
                zipFile.close();
                output.close();
                return result;
            }
        }
        zipFile.close();
        throw new FileNotFoundException(filename);
    }

    public static ArrayList<String> getAllFolders(File zip) throws ZipException, FileNotFoundException, IOException {
        ArrayList<String> returnValue = new ArrayList<String>();
        if (!zip.exists()) {
            throw new FileNotFoundException(zip.getName());
        }
        ZipFile zipFile = new ZipFile(zip);
        Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            int last = entry.getName().lastIndexOf("/");
            if (last != -1) {
                returnValue.add(entry.getName().substring(0, last));
            }
        }
        return returnValue;
    }

    public static boolean fileExists(File zip, String fileName) throws ZipException, IOException {
        String filename = fileName;
        if (!zip.exists()) {
            return false;
        }
        while (filename.charAt(0) == '/') {
            filename = filename.substring(1);
        }
        if (filename.contains("\\")) {
            filename = filename.replace("\\", "/");
        }
        ZipFile zipFile = new ZipFile(zip);
        Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.getName().equalsIgnoreCase(filename)) {
                return true;
            }
        }
        return false;
    }

    public static long getLastModified(File zip, String filename) throws FileNotFoundException, ZipException, IOException {
        if (!zip.exists()) {
            throw new FileNotFoundException(zip.getName());
        }
        while (filename.charAt(0) == '/' || filename.charAt(0) == '\\') {
            filename = filename.substring(1);
        }
        if (filename.contains("\\")) {
            filename = filename.replace("\\", "/");
        }
        ZipFile zipFile = new ZipFile(zip);
        Enumeration entires = zipFile.entries();
        while (entires.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entires.nextElement();
            if (entry.getName().equalsIgnoreCase(filename)) {
                return entry.getTime();
            }
        }
        throw new FileNotFoundException(filename);
    }

    /**
     * This is the main method. It unzips a zuo file.
     * @param zip the zip file to be extracted.
     * @param folder the folder to where the .honmod file will be extracted.
     * @return folder with the files extracted.
     * @throws IOException if an I/O error has occurred
     * @throws FileNotFoundException if a file is missing. Use the Exception.getMessage().
     */
    public static File openZIP(File zip, String folder) throws FileNotFoundException, IOException, ZipException {
        if (!zip.exists()) {
            throw new FileNotFoundException(zip.getAbsolutePath());
        }
        ZipFile zipFile = new ZipFile(zip.getAbsolutePath());
        Enumeration entries = zipFile.entries();
        File file = new File(folder);
        if (!file.exists()) {
            file.mkdirs();
        }
        FileOutputStream output;
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
                new File(folder + File.separator + entry.getName().substring(0, entry.getName().lastIndexOf("/"))).mkdirs();
            } else if (entry.getName().contains("/")) {
                new File(folder + File.separator + entry.getName().substring(0, entry.getName().lastIndexOf("/"))).mkdirs();
            } else {
                FileUtils.copyInputStream(zipFile.getInputStream(entry), output = new FileOutputStream(file.getAbsolutePath() + File.separator + entry.getName()));
                output.close();
            }
        }
        zipFile.close();
        return file;
    }

    /**
     *
     * @param source Path to the folder to be compressed.
     * @param file Path to where the .zip file will be created.
     * @throws FileNotFoundException if coudln't create/open a extracted file.
     * @throws IOException if an I/O error has occurred
     */
    public static void createZIP(String source, String file) throws FileNotFoundException, IOException, ZipException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
        zipDir(source, zos, source);
        zos.flush();
        zos.close();
    }

    /**
     *
     * @param source Path to the folder to be compressed.
     * @param file Path to where the .zip file will be created.
     * @throws FileNotFoundException if coudln't create/open a extracted file.
     * @throws IOException if an I/O error has occurred
     */
    public static void createZIP(String source, String file, String comment) throws FileNotFoundException, IOException, ZipException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
        zipDir(source, zos, source);
        zos.setComment(comment);
        zos.flush();
        zos.close();
    }

    /**
     * Method that actually does the zipping job.
     * @param dir2zip
     * @param zos
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void zipDir(String dir2zip, ZipOutputStream zos, String originalFolder) throws FileNotFoundException, IOException, ZipException {
        File zipDir = new File(dir2zip);
        String[] dirList = zipDir.list();
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                String filePath = f.getPath();
                zipDir(filePath, zos, originalFolder);
                continue;
            }
            FileInputStream fis = new FileInputStream(f);
            String path = f.getPath();
            path = path.replace(originalFolder + File.separator, "");
            while (path.contains("\\")) {
                path = path.replace("\\", "/");
            }
            ZipEntry anEntry = new ZipEntry(path);
            anEntry.setTime(f.lastModified());
            zos.putNextEntry(anEntry);
            while ((bytesIn = fis.read(readBuffer)) != -1) {
                zos.write(readBuffer, 0, bytesIn);
            }
            zos.closeEntry();
            anEntry.setTime(f.lastModified());
            fis.close();
        }
    }

    public static String extractZipComment(String filename) throws FileNotFoundException {
        String retStr = null;
        try {
            File file = new File(filename);
            int fileLen = (int) file.length();
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = new byte[Math.min(fileLen, 81920)];
            int len;
            in.skip(fileLen - buffer.length);
            if ((len = in.read(buffer)) > 0) {
                retStr = getZipCommentFromBuffer(buffer, len);
            }
            in.close();
        } catch (Exception e) {
        }
        return retStr;
    }

    private static String getZipCommentFromBuffer(byte[] buffer, int len) {
        byte[] magicDirEnd = { 0x50, 0x4b, 0x05, 0x06 };
        int buffLen = Math.min(buffer.length, len);
        for (int i = buffLen - magicDirEnd.length - 22; i >= 0; i--) {
            boolean isMagicStart = true;
            for (int k = 0; k < magicDirEnd.length; k++) {
                if (buffer[i + k] != magicDirEnd[k]) {
                    isMagicStart = false;
                    break;
                }
            }
            if (isMagicStart) {
                int commentLen = buffer[i + 20] + buffer[i + 21] * 256;
                if (commentLen < 0) {
                    commentLen = commentLen * -1;
                }
                int realLen = buffLen - i - 22;
                if (commentLen != realLen) {
                }
                String comment = new String(buffer, i + 22, realLen);
                return comment;
            }
        }
        return null;
    }
}
