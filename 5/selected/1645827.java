package uk.ac.ebi.metabolights.utils;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Zipper {

    public static void zip(String thisFileOrDir, String toThisFile) throws IOException {
        try {
            FileUtil.fileExists(thisFileOrDir, true);
            FileOutputStream fout = new FileOutputStream(toThisFile);
            ZipOutputStream zout = new ZipOutputStream(fout);
            File fileSource = new File(thisFileOrDir);
            addDirectory(zout, fileSource, "");
            zout.close();
            System.out.println("Zip file has been created!");
        } catch (IOException ioe) {
            System.out.println("IOException :" + ioe);
            throw ioe;
        }
    }

    private static void addDirectory(ZipOutputStream zout, File fileSource, String innerFolder) throws IOException {
        if (fileSource.isHidden()) {
            System.out.println("Skiping hidden folder " + fileSource.getName());
            return;
        }
        File[] files = fileSource.listFiles();
        System.out.println("Adding directory " + fileSource.getName());
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                addDirectory(zout, files[i], innerFolder + files[i].getName() + "/");
                continue;
            }
            try {
                System.out.println("Adding file " + files[i].getName());
                byte[] buffer = new byte[1024];
                FileInputStream fin = new FileInputStream(files[i]);
                zout.putNextEntry(new ZipEntry(innerFolder + files[i].getName()));
                int length;
                while ((length = fin.read(buffer)) > 0) {
                    zout.write(buffer, 0, length);
                }
                zout.closeEntry();
                fin.close();
            } catch (IOException ioe) {
                System.out.println("IOException :" + ioe);
                throw ioe;
            }
        }
    }

    public static void unzip(String strZipFile) throws IOException, ArchiveException {
        FileUtil.fileExists(strZipFile, true);
        String zipPath = StringUtils.truncate(strZipFile, 4);
        File temp = new File(zipPath);
        temp.mkdir();
        System.out.println(zipPath + " created");
        unzip2(strZipFile, zipPath);
    }

    public static void unzip2(String strZipFile, String folder) throws IOException, ArchiveException {
        FileUtil.fileExists(strZipFile, true);
        final InputStream is = new FileInputStream(strZipFile);
        ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream("zip", is);
        ZipArchiveEntry entry = null;
        OutputStream out = null;
        while ((entry = (ZipArchiveEntry) in.getNextEntry()) != null) {
            File zipPath = new File(folder);
            File destinationFilePath = new File(zipPath, entry.getName());
            destinationFilePath.getParentFile().mkdirs();
            if (entry.isDirectory()) {
                continue;
            } else {
                out = new FileOutputStream(new File(folder, entry.getName()));
                IOUtils.copy(in, out);
                out.close();
            }
        }
        in.close();
    }

    public static void unzip(String strZipFile, String folder) throws IOException {
        try {
            FileUtil.fileExists(strZipFile, true);
            File fSourceZip = new File(strZipFile);
            File zipPath = new File(folder);
            ZipFile zipFile = new ZipFile(fSourceZip);
            Enumeration e = zipFile.entries();
            while (e.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                File destinationFilePath = new File(zipPath, entry.getName());
                destinationFilePath.getParentFile().mkdirs();
                if (entry.isDirectory()) {
                    continue;
                } else {
                    System.out.println("Extracting " + destinationFilePath);
                    BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                    int b;
                    byte buffer[] = new byte[1024];
                    FileOutputStream fos = new FileOutputStream(destinationFilePath);
                    BufferedOutputStream bos = new BufferedOutputStream(fos, 1024);
                    while ((b = bis.read(buffer, 0, 1024)) != -1) {
                        bos.write(buffer, 0, b);
                    }
                    bos.flush();
                    bos.close();
                    bis.close();
                }
            }
        } catch (IOException ioe) {
            System.out.println("IOError :" + ioe);
            throw ioe;
        }
    }
}
