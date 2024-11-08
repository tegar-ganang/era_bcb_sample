package it.csi.otre.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Class that implements some general utility methods as static methods. 
 * 
 * @author oscar ghersi (email: oscar.ghersi@csi.it)
 *
 */
public class Utility {

    /**
	 * Deletes a non-empty directory.
	 * 
	 * @param path The pathname to delete.
	 * @return true if succeeded to delete the directory.
	 */
    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) deleteDirectory(files[i]); else files[i].delete();
            }
        }
        return (path.delete());
    }

    /**
	 * Zips (creates) the content of a directory into an archive with specified name. 
	 * 
	 * @param dirToZip Directory to zip.
	 * @param zipName Zip file name.
	 */
    public static void zipDir(String dirToZip, String zipName) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipName));
        List<String> l = new ArrayList<String>();
        listOfFiles(new File(dirToZip), l);
        byte[] readBuffer = null;
        RandomAccessFile raf = null;
        for (Iterator iterator = l.iterator(); iterator.hasNext(); ) {
            String filename = (String) iterator.next();
            raf = new RandomAccessFile(filename, "r");
            readBuffer = new byte[(int) raf.length()];
            raf.read(readBuffer);
            ZipEntry anEntry = new ZipEntry(filename.substring(dirToZip.length()));
            zos.putNextEntry(anEntry);
            zos.write(readBuffer, 0, (int) raf.length());
            raf.close();
        }
        zos.close();
    }

    /**
	 * Unzips a zip file under a directory.
	 * 
	 * @param zipFile Full pathname of the zip file to unzip.
	 * @param unzipDir Directory under which unzip the file.
	 */
    public static void unzipToDir(String zipFile, String unzipDir) throws Exception {
        ZipFile zip = new ZipFile(zipFile);
        String zipName = zipFile.substring(zipFile.lastIndexOf("/"), zipFile.lastIndexOf("."));
        new File(unzipDir + zipName).mkdirs();
        for (Enumeration en = zip.entries(); en.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) en.nextElement();
            String name = entry.getName();
            System.out.println("entry name: " + name);
            String newname = unzipDir + zipName + name;
            System.out.println("new entry name: " + newname);
            if (entry.isDirectory()) {
                (new File(newname)).mkdirs();
            } else {
                System.out.println("parent: " + new File(newname).getParentFile());
                new File(newname).getParentFile().mkdirs();
            }
        }
        for (Enumeration en = zip.entries(); en.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) en.nextElement();
            String name = entry.getName();
            String newname = unzipDir + zipName + name;
            if (!entry.isDirectory()) {
                int len = 0;
                byte[] buffer = new byte[2048];
                InputStream in = zip.getInputStream(entry);
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newname));
                while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
                in.close();
                out.close();
            }
        }
        zip.close();
    }

    private static void listOfFiles(File dir, List<String> l) {
        String[] ret = null;
        File[] list = dir.listFiles();
        for (File f : list) {
            if (f.isDirectory()) listOfFiles(f, l); else if (f.isFile()) l.add(f.getAbsolutePath());
        }
    }

    public static void main(String[] args) {
        try {
            Utility.deleteDirectory(new File("/home/oscar/temp/prove"));
        } catch (Exception e) {
            System.out.println("ERROR: " + e);
        }
        System.exit(0);
    }
}
