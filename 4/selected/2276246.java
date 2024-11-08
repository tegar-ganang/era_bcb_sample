package com.cronopista.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Eduardo Rodr�guez
 * 
 */
public class Utils {

    public static String getApplicationPath() {
        try {
            URL url = Utils.class.getProtectionDomain().getCodeSource().getLocation();
            String szUrl = url.toString();
            szUrl = szUrl.substring(0, szUrl.lastIndexOf("/"));
            URI uri = new URI(szUrl);
            return new File(uri).getAbsolutePath() + File.separator;
        } catch (Exception e) {
        }
        return new File("").getAbsolutePath() + File.separator;
    }

    public static void copyFile(InputStream fis, File out) throws Exception {
        FileOutputStream fos = null;
        fos = new FileOutputStream(out);
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    public static void copyFileToPlatformCharacterEncoding(File inFile, String encoding, File outFile) throws Exception {
        BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), encoding));
        String line = null;
        while ((line = in.readLine()) != null) {
            out.write(line);
            out.write('\n');
        }
        in.close();
        out.close();
    }

    public static void unzip(File fileFrom, File fileTo) throws Exception {
        Enumeration entries;
        ZipFile zipFile;
        zipFile = new ZipFile(fileFrom);
        entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            InputStream in = zipFile.getInputStream(entry);
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileTo));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
            in.close();
            out.close();
        }
        zipFile.close();
    }

    public static void main(String[] args) {
        System.out.println(getApplicationPath());
    }
}
