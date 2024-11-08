package org.gruposp2p.aularest.utils;

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
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

public class FileUtils {

    public static final String jpeg = "jpeg";

    public static final String jpg = "jpg";

    public static final String gif = "gif";

    public static final String tiff = "tiff";

    public static final String tif = "tif";

    public static final String png = "png";

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
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

    public static void copyInputstreamToFile(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static void unzipFile(ZipFile zipFile, String unzipDir) throws IOException {
        Enumeration entries = null;
        entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            if (zipEntry.isDirectory()) {
                (new File(zipEntry.getName())).mkdir();
                continue;
            }
            copyInputstreamToFile(zipFile.getInputStream(zipEntry), new BufferedOutputStream(new FileOutputStream(unzipDir + zipEntry.getName())));
        }
        zipFile.close();
    }

    public static void unzipFile(String zipFileName, String unzipDir) throws IOException {
        ZipFile zipFile = new ZipFile(zipFileName);
        unzipFile(zipFile, unzipDir);
    }

    /** Zip the contents of the directory, and save it in the zipfile */
    public static void zipDirectory(String dir, String zipfile) throws IOException, IllegalArgumentException {
        File d = new File(dir);
        if (!d.isDirectory()) throw new IllegalArgumentException("Not a directory:  " + dir);
        String[] entries = d.list();
        byte[] buffer = new byte[4096];
        int bytesRead;
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
        for (int i = 0; i < entries.length; i++) {
            File f = new File(d, entries[i]);
            if (f.isDirectory()) continue;
            FileInputStream in = new FileInputStream(f);
            ZipEntry entry = new ZipEntry(f.getPath());
            out.putNextEntry(entry);
            while ((bytesRead = in.read(buffer)) != -1) out.write(buffer, 0, bytesRead);
            in.close();
        }
        out.close();
    }

    public static Schema getSchema(String schemaPath) throws SAXException {
        SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        InputStream dbis = FileUtils.class.getClassLoader().getResourceAsStream(schemaPath);
        Schema schema = sf.newSchema(new StreamSource(dbis));
        return schema;
    }
}
