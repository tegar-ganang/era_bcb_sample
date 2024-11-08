package org.fao.fenix.services.compression;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.fao.fenix.services.utility.CommunicationLogger;

public class ZipFactory {

    /** Most of it has been copied from an internet's tutorial. */
    public static void createZipFromDataset(String localResourceId, File dataset, File metadata) {
        CommunicationLogger.warning("System entered ZipFactory");
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            String outFilename = tmpDir + "/" + localResourceId + ".zip";
            CommunicationLogger.warning("File name: " + outFilename);
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));
            byte[] buf = new byte[1024];
            FileInputStream in = new FileInputStream(dataset);
            out.putNextEntry(new ZipEntry(dataset.getName()));
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in = new FileInputStream(metadata);
            out.putNextEntry(new ZipEntry(metadata.getName()));
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            out.closeEntry();
            in.close();
            out.close();
        } catch (IOException e) {
            System.out.println("IO EXCEPTION: " + e.getMessage());
        }
    }

    public static void unzip(String filename) {
        try {
            String inFilename = filename;
            CommunicationLogger.warning("ZIP FILE NAME " + inFilename);
            ZipInputStream in = new ZipInputStream(new FileInputStream(inFilename));
            List list = list(inFilename);
            for (int i = 0; i < list.size(); i++) {
                ZipEntry entry = in.getNextEntry();
                String outFilename = System.getProperty("java.io.tmpdir") + "/" + (String) list.get(i);
                System.out.println(outFilename);
                OutputStream out = new FileOutputStream(outFilename);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
            }
            in.close();
        } catch (IOException e) {
            CommunicationLogger.error(e.getMessage());
        }
    }

    private static List list(String resourceName) {
        ArrayList list = new ArrayList();
        try {
            CommunicationLogger.warning("LIST ZIP " + resourceName);
            ZipFile zipfile = new ZipFile(resourceName);
            for (Enumeration entries = zipfile.entries(); entries.hasMoreElements(); ) {
                list.add(((ZipEntry) entries.nextElement()).getName());
            }
        } catch (IOException e) {
            System.out.println("IO EXCEPTION: " + e.getMessage());
        }
        return list;
    }
}
