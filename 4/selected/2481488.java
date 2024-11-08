package net.slashie.utils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtil {

    public static int filasEnArchivo(String pArchivo) throws FileNotFoundException, IOException {
        Debug.enterMethod("", "FileUtil.filasEnArchivo", pArchivo);
        File vArchivo = new File(pArchivo);
        BufferedReader inx = new BufferedReader(new FileReader(vArchivo));
        int lines = 0;
        while (inx.readLine() != null) lines++;
        inx.close();
        Debug.exitMethod(lines + "");
        return lines;
    }

    public static void deleteFile(String what) {
        new File(what).delete();
    }

    public static void copyFile(File origen, File destino) throws Exception {
        FileInputStream fis = new FileInputStream(origen);
        FileOutputStream fos = new FileOutputStream(destino);
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    public static BufferedReader getReader(String fileName) throws IOException {
        Debug.enterStaticMethod("FileUtil", "getReader");
        BufferedReader x = new BufferedReader(new FileReader(fileName));
        Debug.exitMethod(x);
        return x;
    }

    public static BufferedWriter getWriter(String fileName) throws IOException {
        Debug.enterStaticMethod("FileUtil", "getWriter");
        BufferedWriter x = new BufferedWriter(new FileWriter(fileName));
        Debug.exitMethod(x);
        return x;
    }

    public static boolean fileExists(String filename) {
        return new File(filename).exists();
    }

    public static void extractZipFile(String filename) {
        try {
            byte[] buf = new byte[1024];
            ZipInputStream zipinputstream = null;
            ZipEntry zipentry;
            zipinputstream = new ZipInputStream(new FileInputStream(filename));
            zipentry = zipinputstream.getNextEntry();
            while (zipentry != null) {
                String entryName = zipentry.getName();
                int n;
                FileOutputStream fileoutputstream;
                File newFile = new File(entryName);
                String directory = newFile.getParent();
                if (directory == null) {
                    if (newFile.isDirectory()) break;
                }
                fileoutputstream = new FileOutputStream(entryName);
                while ((n = zipinputstream.read(buf, 0, 1024)) > -1) fileoutputstream.write(buf, 0, n);
                fileoutputstream.close();
                zipinputstream.closeEntry();
                zipentry = zipinputstream.getNextEntry();
            }
            zipinputstream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
