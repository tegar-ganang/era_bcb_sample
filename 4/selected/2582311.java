package edu.univalle.lingweb.rest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UnZip {

    /**
	 * Extrae el contenido de un archivo Zip en un determinado directorio
	 * @param zipfile Archivo Zip
	 * @param destDir Directorio destino
	 */
    public static void extract(String zipfile, String destDir) {
        Enumeration entries;
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(zipfile);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    System.err.println("Extracting directory:" + destDir + entry.getName());
                    (new File(destDir + entry.getName())).mkdirs();
                    continue;
                }
                System.err.println("Extracting file: " + destDir + entry.getName());
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(destDir + entry.getName())));
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }

    /**
	 * Copia el contenido de un flujo de entrada en uno de salida
	 * @param in Flujo de entrada
	 * @param out Flujo de salida
	 * @throws IOException si ocurre una excepcion
	 */
    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
            in.close();
            out.close();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public static final void main(String[] args) {
    }
}
