package org.neblipedia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Archivos {

    public static void copia(File nombreFuente, File nombreDestino) throws IOException {
        FileInputStream fis = new FileInputStream(nombreFuente);
        FileOutputStream fos = new FileOutputStream(nombreDestino);
        FileChannel canalFuente = fis.getChannel();
        FileChannel canalDestino = fos.getChannel();
        canalFuente.transferTo(0, canalFuente.size(), canalDestino);
        fis.close();
        fos.close();
    }

    public static void eliminar(File dd) {
        eliminar(dd, true);
    }

    public static void eliminar(File dd, boolean in) {
        System.out.println("Eliminando: " + dd.getAbsolutePath());
        if (dd.exists()) {
            for (File f : dd.listFiles()) {
                if (f.isDirectory()) {
                    eliminar(f, true);
                } else {
                    f.delete();
                }
            }
            if (in) {
                dd.delete();
            }
        }
    }

    /**
	 * borra todos los archivos de una carpeta pero no borra la estructura de
	 * directorios
	 * 
	 * @param carpeta
	 */
    public static void eliminarArchivos(File carpeta) {
        for (File f : carpeta.listFiles()) {
            if (f.isDirectory()) {
                eliminarArchivos(f);
            } else {
                f.delete();
            }
        }
    }
}
