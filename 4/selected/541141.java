package org.qtitools.mathqurate.utilities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ZipHelper {

    /**
	 * Unzips a file into a directory.
	 * WARNING: THE DESTINATION DIRECTORY WILL BE DELETED! So if you want your zip in
	 * /WINDOWS/TEMP you'd pass a destination dir of /WINDOWS/TEMP/MYZIP or
	 * similar. MYZIP would be deleted if it existed.
	 * @param zip
	 * @param extractTo
	 * @throws IOException
	 */
    public static void unzip(File zip, File extractTo) throws IOException {
        deleteDirectory(extractTo);
        ZipFile archive = new ZipFile(zip);
        Enumeration e = archive.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            File file = new File(extractTo, entry.getName());
            if (entry.isDirectory() && !file.exists()) {
                file.mkdirs();
            } else {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                InputStream in = archive.getInputStream(entry);
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                byte[] buffer = new byte[8192];
                int read;
                while (-1 != (read = in.read(buffer))) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.close();
            }
        }
    }

    public static void zipDirectory(File directory, File zip) throws IOException {
        if (zip.exists()) {
            zip.delete();
        }
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
        zip(directory, directory, zos);
        zos.close();
    }

    private static void zip(File directory, File base, ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles();
        byte[] buffer = new byte[8192];
        int read = 0;
        for (int i = 0, n = files.length; i < n; i++) {
            if (files[i].isDirectory()) {
                zip(files[i], base, zos);
            } else {
                FileInputStream in = new FileInputStream(files[i]);
                String entryStr = files[i].getPath().substring(base.getPath().length() + 1);
                entryStr = entryStr.replaceAll("\\\\", "/");
                ZipEntry entry = new ZipEntry(entryStr);
                zos.putNextEntry(entry);
                while (-1 != (read = in.read(buffer))) {
                    zos.write(buffer, 0, read);
                }
                in.close();
            }
        }
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

    public static boolean deleteDirectory(String path) {
        return (deleteDirectory(new File(path)));
    }
}
