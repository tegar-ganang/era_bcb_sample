package com.loribel.commons.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import com.loribel.commons.util.CTools;
import com.loribel.commons.util.FTools;

/**
 * Tools for Zip files.
 * <p>
 * Example to create a zip file.
 * <pre>
 *   FileOutputStream l_os = new FileOutputStream(zipFile);
 *	 ZipOutputStream l_zip = new ZipOutputStream(l_os);
 *	 GB_ZipTools.appendZipFile(l_file1);
 *	 ...
 *	 GB_ZipTools.appendZipFile(l_filen);
 *	 l_zip.close();
 *   l_os.close();
 * </pre>
 *
 * @author Gregory Borelli
 */
public final class GB_ZipTools {

    /**
     * Add a file to a Zip.
     */
    public static void appendZipFile(ZipOutputStream zos, File a_file) throws IOException {
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        FileInputStream fis = new FileInputStream(a_file);
        ZipEntry l_anEntry = new ZipEntry(a_file.getName());
        zos.putNextEntry(l_anEntry);
        while ((bytesIn = fis.read(readBuffer)) != -1) {
            zos.write(readBuffer, 0, bytesIn);
        }
        fis.close();
    }

    /**
     * Compress a byte array.
     */
    public static byte[] compressByteArray(byte[] a_array) throws IOException {
        ByteArrayOutputStream l_baos = new ByteArrayOutputStream(a_array.length);
        DeflaterOutputStream l_zos = new DeflaterOutputStream(l_baos);
        l_zos.write(a_array);
        l_zos.close();
        return l_baos.toByteArray();
    }

    /**
     * Extract a zip file to a directory.
     * If destDir is null use the name of the zip file
     * without extension as directory.
     * @return the destination directory
     */
    public static File[] extractZipToDir(File a_zipFile, File a_destDir) throws IOException {
        if (a_destDir == null) {
            a_destDir = new File(FTools.removeExtension(a_zipFile));
        }
        a_destDir.mkdirs();
        FileInputStream l_fileInputStream = null;
        BufferedInputStream l_bufferInputStream = null;
        ZipInputStream l_zipInputStream = null;
        List retour;
        try {
            Map l_entriesMap = getZipEntry(a_zipFile);
            retour = new ArrayList();
            l_fileInputStream = new FileInputStream(a_zipFile);
            l_bufferInputStream = new BufferedInputStream(l_fileInputStream);
            l_zipInputStream = new ZipInputStream(l_bufferInputStream);
            ZipEntry l_zipEntry = null;
            while ((l_zipEntry = l_zipInputStream.getNextEntry()) != null) {
                if (l_zipEntry.isDirectory()) {
                    continue;
                }
                String l_name = l_zipEntry.getName();
                int len = (int) l_zipEntry.getSize();
                if (len == -1) {
                    len = (int) ((ZipEntry) l_entriesMap.get(l_zipEntry.getName())).getSize();
                }
                byte[] b = new byte[len];
                int rb = 0;
                int chunk = 0;
                while ((len - rb) > 0) {
                    chunk = l_zipInputStream.read(b, rb, len - rb);
                    if (chunk == -1) {
                        break;
                    }
                    rb += chunk;
                }
                File l_destFile = new File(a_destDir, l_name);
                l_destFile.getParentFile().mkdirs();
                FileOutputStream os = new FileOutputStream(l_destFile);
                os.write(b);
                os.close();
                retour.add(l_destFile);
            }
        } finally {
            FTools.closeSafe(l_fileInputStream);
            FTools.closeSafe(l_bufferInputStream);
            FTools.closeSafe(l_zipInputStream);
        }
        return (File[]) retour.toArray(new File[retour.size()]);
    }

    /**
     * Extract a zip file to a temporary directory.
     * Delete extracted files on exit.
     */
    public static File[] extractZipToTempDir(File a_zipFile) throws IOException {
        File l_tempDir = File.createTempFile("~zip", ".temp");
        File[] retour = extractZipToDir(a_zipFile, l_tempDir);
        int len = CTools.getSize(retour);
        for (int i = 0; i < len; i++) {
            File l_file = retour[i];
            l_file.deleteOnExit();
        }
        return retour;
    }

    /**
     * Map :
     *   key: entryName
     *   value: entry
     */
    public static Map getZipEntry(File a_zipFile) throws IOException {
        Map retour = new HashMap();
        ZipFile l_zf = new ZipFile(a_zipFile);
        Enumeration e = l_zf.entries();
        while (e.hasMoreElements()) {
            ZipEntry l_entry = (ZipEntry) e.nextElement();
            retour.put(l_entry.getName(), l_entry);
        }
        l_zf.close();
        return retour;
    }

    /**
     * Uncompress a byte array.
     */
    public static byte[] uncompressByteArray(byte[] a_array) throws IOException {
        List l_byteArrays = new ArrayList();
        ByteArrayInputStream l_bais = new ByteArrayInputStream(a_array);
        InflaterInputStream l_zis = new InflaterInputStream(l_bais);
        byte[] l_bytes = new byte[2048];
        int index = 0;
        int l_val;
        while ((l_val = l_zis.read()) != -1) {
            l_bytes[index++] = (byte) l_val;
            if (index == 2048) {
                l_byteArrays.add(l_bytes);
                l_bytes = new byte[2048];
                index = 0;
            }
        }
        byte[] l_finalArray = new byte[l_byteArrays.size() * 2048 + index];
        int finalIndex = 0;
        for (int i = 0; i < l_byteArrays.size(); i++) {
            byte[] l_a = (byte[]) l_byteArrays.get(i);
            System.arraycopy(l_a, 0, l_finalArray, finalIndex, l_a.length);
            finalIndex += l_a.length;
        }
        System.arraycopy(l_bytes, 0, l_finalArray, finalIndex, index);
        return l_finalArray;
    }

    /**
     * Zip a file and delete it after zip it.
     */
    public static void zipAndDelete(File a_file, File a_zipFile) throws IOException {
        File l_zipFile = a_zipFile;
        if (l_zipFile == null) {
            l_zipFile = FTools.replaceExtension(a_file, "zip");
        }
        FileOutputStream l_os = new FileOutputStream(l_zipFile);
        ZipOutputStream l_zip = new ZipOutputStream(l_os);
        GB_ZipTools.appendZipFile(l_zip, a_file);
        l_zip.close();
        l_os.close();
        a_file.delete();
    }

    /**
     * Zip files and delete it after zip it.
     */
    public static void zipAndDelete(File[] a_files, File a_zipFile) throws IOException {
        zipFiles(a_files, a_zipFile, true);
    }

    /**
     * Zip Dir and delete it after zip them if a_deleteAfter is true.
     */
    public static void zipDir(File a_dir, File a_zipFile, boolean a_deleteAfter) throws IOException {
        File[] l_files = GB_DirTools.listFiles(a_dir, null, true);
        zipFiles(l_files, a_zipFile, a_deleteAfter);
    }

    /**
     * Zip files and delete it after zip them if a_deleteAfter is true.
     */
    public static void zipFiles(File[] a_files, File a_zipFile, boolean a_deleteAfter) throws IOException {
        File l_zipFile = a_zipFile;
        FileOutputStream l_os = new FileOutputStream(l_zipFile);
        ZipOutputStream l_zip = new ZipOutputStream(l_os);
        try {
            int len = CTools.getSize(a_files);
            for (int i = 0; i < len; i++) {
                File l_file = a_files[i];
                GB_ZipTools.appendZipFile(l_zip, l_file);
                if (a_deleteAfter) {
                    l_file.delete();
                }
            }
        } catch (IOException ex) {
            throw ex;
        } finally {
            l_zip.close();
            l_os.close();
        }
    }

    private GB_ZipTools() {
    }
}
