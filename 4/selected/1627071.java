package fr.loria.ecoo.wootEngine;

import org.apache.ws.commons.util.Base64;
import org.apache.ws.commons.util.Base64.DecodingException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * DOCUMENT ME!
 *
 * @author nabil
 */
public class FileUtil {

    /** DOCUMENT ME! */
    public static final int BUFFER = 2048;

    /**
     * DOCUMENT ME!
     *
     * @param dirPath DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    public static String zipDirectory(String dirPath) throws Exception {
        File dir = new File(dirPath);
        String[] files = dir.list();
        if (files.length < 1) {
            return null;
        }
        File file = File.createTempFile(dir.getName(), "zip");
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        ZipOutputStream zos = new ZipOutputStream(bos);
        zos.setMethod(ZipOutputStream.DEFLATED);
        zos.setLevel(Deflater.BEST_COMPRESSION);
        byte[] data = new byte[BUFFER];
        for (int i = 0; i < files.length; i++) {
            FileInputStream fi = new FileInputStream(dirPath + File.separator + files[i]);
            BufferedInputStream buffer = new BufferedInputStream(fi, BUFFER);
            ZipEntry entry = new ZipEntry(normalizeName(files[i]));
            zos.putNextEntry(entry);
            int count;
            while ((count = buffer.read(data, 0, BUFFER)) != -1) {
                zos.write(data, 0, count);
            }
            zos.closeEntry();
            buffer.close();
        }
        zos.flush();
        bos.flush();
        fos.flush();
        zos.close();
        bos.close();
        fos.close();
        return file.getAbsolutePath();
    }

    /**
     * DOCUMENT ME!
     *
     * @param zipFile DOCUMENT ME!
     * @param dirPath DOCUMENT ME!
     *
     * @throws Exception DOCUMENT ME!
     */
    public static void unzipInDirectory(ZipFile zipFile, String dirPath) throws Exception {
        Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(dirPath + File.separator + entry.getName())));
        }
        zipFile.close();
    }

    /**
     * DOCUMENT ME!
     *
     * @param strPath DOCUMENT ME!
     * @param dstPath DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public static void copyFiles(String strPath, String dstPath) throws IOException {
        File sourceDir = new File(strPath);
        for (File file : sourceDir.listFiles()) {
            if (file.isFile() && file.canRead()) {
                FileUtil.copyFile(file.toString(), dstPath + File.separator + file.getName());
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param fileName DOCUMENT ME!
     * @param dstPath DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public static void copyFile(String fileName, String dstPath) throws IOException {
        FileChannel sourceChannel = new FileInputStream(fileName).getChannel();
        FileChannel destinationChannel = new FileOutputStream(dstPath).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }

    /**
     * DOCUMENT ME!
     *
     * @param dir DOCUMENT ME!
     */
    public static void deleteDirectory(File dir) {
        if (dir.exists()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
            }
            dir.delete();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param s DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public static String removeAccents(String s) {
        String sSemAcento = s;
        sSemAcento = sSemAcento.replaceAll("[áàâãä]", "a");
        sSemAcento = sSemAcento.replaceAll("[ÁÀÂÃÄ]", "A");
        sSemAcento = sSemAcento.replaceAll("[éèêë]", "e");
        sSemAcento = sSemAcento.replaceAll("[ÉÈÊË]", "E");
        sSemAcento = sSemAcento.replaceAll("[íìîï]", "i");
        sSemAcento = sSemAcento.replaceAll("[ÍÌÎÏ]", "I");
        sSemAcento = sSemAcento.replaceAll("[óòôõö]", "o");
        sSemAcento = sSemAcento.replaceAll("[ÓÒÔÕÖ]", "O");
        sSemAcento = sSemAcento.replaceAll("[úùûü]", "u");
        sSemAcento = sSemAcento.replaceAll("[ÚÙÛÜ]", "U");
        sSemAcento = sSemAcento.replaceAll("ç", "c");
        sSemAcento = sSemAcento.replaceAll("Ç", "C");
        sSemAcento = sSemAcento.replaceAll("ñ", "n");
        sSemAcento = sSemAcento.replaceAll("Ñ", "N");
        return sSemAcento;
    }

    private static String normalizeName(String string) {
        String temp = removeAccents(string);
        return temp.replaceAll("[^\\p{ASCII}]", "");
    }

    private static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.flush();
        out.close();
    }

    /**
     * DOCUMENT ME!
     *
     * @param pageId DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws UnsupportedEncodingException DOCUMENT ME!
     */
    public static String getEncodedFileName(String pageId) throws UnsupportedEncodingException {
        return Base64.encode(pageId.getBytes("UTF-8")).trim();
    }

    /**
     * DOCUMENT ME!
     *
     * @param filename DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws UnsupportedEncodingException DOCUMENT ME!
     */
    public static String getDecodedFileName(String filename) throws UnsupportedEncodingException {
        try {
            String result = new String(Base64.decode(filename), "UTF-8");
            return result;
        } catch (DecodingException e) {
            e.printStackTrace();
        }
        return "";
    }
}
