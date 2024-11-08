package com.coloradoresearch.gridster;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * @author Jon Ford
 * 
 */
public class Utils {

    private static Logger logger = Logger.getLogger(Utils.class.getName());

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        logger.debug("loading file: " + file.getCanonicalPath());
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            logger.error("file too big");
            return new byte[0];
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

    public static void writeZipBytesToFile(byte[] bytes, String aFileName) {
        try {
            logger.debug("unzipping file: " + aFileName);
            if (bytes == null) {
                logger.debug("null bytes");
            }
            File f = new File(aFileName);
            FileOutputStream ostr = new FileOutputStream(f);
            BufferedOutputStream bstr = new BufferedOutputStream(ostr);
            bstr.write(bytes, 0, bytes.length);
            bstr.close();
        } catch (Exception e) {
            logger.error("Unable to write zipped file from byte stream:" + e);
        }
    }

    public static void zipDir(String dir2zip, ZipOutputStream zos, String topLevelDirectory, String topLevelPrefix) {
        try {
            File zipDir = new File(dir2zip);
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipDir(filePath, zos, topLevelDirectory, topLevelPrefix);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                String filePath = f.getPath();
                logger.debug("filepath: " + filePath);
                int length = topLevelDirectory.length() + 1;
                String shortFilePath = topLevelPrefix + "/" + filePath.substring(length);
                logger.debug(shortFilePath);
                ZipEntry anEntry = new ZipEntry(shortFilePath);
                logger.debug(f.getPath());
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        } catch (Exception e) {
            logger.error("unable to zip up directory: " + e);
        }
    }

    public static void unzipFile(String aFileName, String aDirectory) {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(aFileName));
            ZipInputStream zin = new ZipInputStream(in);
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.getName().equals(aFileName)) {
                    unzip(zin, aFileName);
                    break;
                }
                unzip(zin, aDirectory + e.getName());
            }
            zin.close();
        } catch (Exception e) {
            logger.error("unable to unzip file" + e);
        }
    }

    public static void unzipBytes(byte[] bytes, String aFileName, String aDirectory) {
        try {
            InputStream in = new BufferedInputStream(new ByteArrayInputStream(bytes));
            ZipInputStream zin = new ZipInputStream(in);
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.getName().equals(aFileName)) {
                    unzip(zin, aFileName);
                    break;
                }
                unzip(zin, aDirectory + e.getName());
            }
            zin.close();
        } catch (Exception e) {
            logger.error("unable to unzip file" + e);
        }
    }

    public static void unzip(ZipInputStream zin, String s) throws IOException {
        logger.debug("unzipping " + s);
        File f = new File(s);
        File parent = new File(f.getParent());
        parent.mkdirs();
        FileOutputStream out = new FileOutputStream(s);
        byte[] b = new byte[512];
        int len = 0;
        while ((len = zin.read(b)) != -1) {
            out.write(b, 0, len);
        }
        out.close();
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static boolean deleteDirectory(String aDir) {
        File f = new File(aDir);
        return Utils.deleteDirectory(f);
    }

    public static boolean createDirectory(String aDir) {
        return new File(aDir).mkdirs();
    }

    public static LinkedList getClasspathEntries() {
        LinkedList list = new LinkedList();
        Properties props = System.getProperties();
        logger.debug(props.toString());
        String classpath = props.getProperty("java.class.path");
        String separator = props.getProperty("path.separator");
        StringTokenizer st = new StringTokenizer(classpath, separator);
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        return list;
    }

    public static Hashtable getNamesAndFileBytes(LinkedList listOfFileNames) {
        int numElems = listOfFileNames.size();
        Hashtable ht = new Hashtable();
        Iterator it = listOfFileNames.iterator();
        int x = 0;
        while (it.hasNext()) {
            String item = (String) it.next();
            byte[] bytes = null;
            try {
                if ((item.endsWith(".jar")) || (item.endsWith(".zip"))) {
                    bytes = Utils.getBytesFromFile(new File(item));
                    ht.put("file" + x, bytes);
                    logger.debug("file:  " + item);
                } else {
                    bytes = Utils.getDirBytes(item);
                    ht.put("dir" + x, bytes);
                    logger.debug("dir:   " + item);
                }
            } catch (Exception e) {
                logger.error("Unable to ship resource: " + item);
            }
            x++;
        }
        return ht;
    }

    public static byte[] getDirBytes(String topLevelDirectory) {
        byte[] bytes = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);
            Utils.zipDir(topLevelDirectory, zos, topLevelDirectory, "");
            zos.close();
            bytes = baos.toByteArray();
        } catch (Exception e) {
            logger.error("Unable to zip directory: " + e);
        }
        return bytes;
    }
}
