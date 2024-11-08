package com.alianzamedica.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 
 * @author Carlos
 */
public class ZipUtility {

    @SuppressWarnings("unchecked")
    private Hashtable zipentries = new Hashtable();

    /**
     * 
     */
    public void resetHash() {
        this.zipentries.clear();
    }

    /**
	 * procesa las entradas de un archivo zip.
	 * 
	 * @param file
	 *            archivo zip a procesar.
	 * @throws ZipException
	 *             si ocurre error.
	 * @throws IOException
	 *             si ocurre error.
	 */
    @SuppressWarnings("unchecked")
    public void processZipEntries(File file) throws ZipException, IOException {
        ZipFile zip = null;
        FileInputStream in = null;
        try {
            zip = new ZipFile(file);
            in = new FileInputStream(file);
            Enumeration entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                String name = entry.getName();
                byte[] data = getData(entry, zip);
                this.zipentries.put(name, data);
            }
        } finally {
            try {
                zip.close();
            } catch (Exception e) {
            }
            try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * cambia la entrada de un documento.
	 * 
	 * @param entryName
	 *            nombre de la entrada.
	 * @param data
	 *            valor en datos de la entrada.
	 */
    @SuppressWarnings("unchecked")
    public void setDocumentEntry(String entryName, byte[] data) {
        this.zipentries.put(entryName, data);
    }

    /**
	 * procesa la salida de un documento.
	 * 
	 * @param file
	 *            archivo escribir en la configuración.
	 * @throws IOException
	 *             si ocurre error.
	 */
    @SuppressWarnings("unchecked")
    public void processOutput(File file) throws IOException {
        ZipOutputStream zipOut = null;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            zipOut = new ZipOutputStream(out);
            zipOut.setLevel(1);
            Enumeration keys = zipentries.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                byte[] data = (byte[]) zipentries.get(key);
                ZipEntry entry = new ZipEntry((String) key);
                zipOut.putNextEntry(entry);
                zipOut.write(data, 0, data.length);
            }
        } finally {
            try {
                zipOut.close();
            } catch (Exception e) {
            }
            try {
                out.close();
            } catch (Exception e) {
            }
        }
    }

    private byte[] getData(ZipEntry entry, ZipFile zipFile) {
        InputStream in = null;
        byte[] data = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            in = zipFile.getInputStream(entry);
            int read = 0;
            while ((read = in.read()) != -1) {
                baos.write((byte) read);
            }
            data = baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
        return data;
    }
}
