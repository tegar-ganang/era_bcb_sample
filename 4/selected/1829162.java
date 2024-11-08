package org.monet.deployservice.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("rawtypes")
public class Zip {

    private void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public void unCompress(String fileName, String dir) {
        Enumeration entries;
        ZipFile zipFile;
        if (!dir.equals("")) dir = dir + "/";
        try {
            zipFile = new ZipFile(fileName);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    (new File(dir + entry.getName())).mkdir();
                    continue;
                }
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(dir + entry.getName())));
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }

    public void unCompressOnlyFile(String fileZip, String fileFrom, String fileTo) {
        Enumeration entries;
        ZipFile zipFile;
        if (!new File(fileZip).exists()) {
            System.err.println("File not exits: " + fileZip);
            return;
        }
        try {
            zipFile = new ZipFile(fileZip);
            entries = zipFile.entries();
            String fileDest;
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.getName().length() >= fileFrom.length()) {
                    if (entry.getName().substring(0, fileFrom.length()).equals(fileFrom)) {
                        if (entry.getName().length() == fileFrom.length()) fileDest = fileTo; else {
                            File fileToReal = new File(fileTo);
                            if (!fileToReal.isDirectory()) continue;
                            fileDest = fileTo + "/" + entry.getName().substring(fileFrom.length() + 1);
                        }
                        if (entry.isDirectory()) {
                            (new File(fileDest)).mkdirs();
                        } else {
                            copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(fileDest)));
                        }
                    }
                }
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }

    public void compress(String fileName, String dir) {
        Files files = new Files();
        String[] filenames = files.directoryList(dir);
        byte[] buf = new byte[1024];
        try {
            String outFilename = fileName;
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));
            for (int i = 0; i < filenames.length; i++) {
                FileInputStream in = new FileInputStream(filenames[i]);
                String file = filenames[i].substring(dir.length() + 1);
                out.putNextEntry(new ZipEntry(file));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
        } catch (IOException e) {
        }
    }
}
