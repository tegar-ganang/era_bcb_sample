package cmspider.utilities.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.channels.FileChannel;

public class FileUtilities {

    public static final String NL = System.getProperty("line.separator");

    public static final char SEPARATOR = File.separator.charAt(0);

    public static void createDirectory(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            if (!directory.mkdir()) System.out.println("[Error] creating directory: " + directory.getName());
        }
    }

    public static String loadTextFromFile(String pathname) {
        File file = new File(pathname);
        return loadTextFromFile(file);
    }

    public static String loadTextFromFile(File file) {
        FileInputStream fileStream = null;
        try {
            fileStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println("[ERRORE] Apertura del file: " + e.getMessage());
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(fileStream));
        StringBuffer stringBuffer = new StringBuffer();
        String line;
        try {
            while ((line = input.readLine()) != null) {
                stringBuffer.append((line + NL));
            }
        } catch (IOException e) {
            System.out.println("[ERRORE] Lettura del file: " + e.getMessage());
        }
        try {
            if (fileStream != null) fileStream.close();
        } catch (IOException e) {
            System.out.println("[ERRORE] Chiusura del file: " + e.getMessage());
        }
        return stringBuffer.toString();
    }

    public static void writeTextToFile(String pathname, String text) {
        File file = new File(pathname);
        writeTextToFile(file, text);
    }

    public static void writeTextToFile(File file, String text) {
        Writer output = null;
        try {
            if (text == null) throw new IOException("la stringa non � stata inizializzata");
            output = new BufferedWriter(new FileWriter(file));
            output.write(text);
        } catch (IOException e) {
            System.out.println("[ERRORE] Scrittura su file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (output != null) output.close();
            } catch (IOException e) {
                System.out.println("[ERRORE] Chiusura del file: " + e.getMessage());
            }
        }
    }

    public static void writeBytesToFile(String fileName, byte[] buffer) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            fileOutputStream.write(buffer);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            System.out.println("[ERRORE] Apertura file: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("[ERRORE] Scrittura file: " + e.getMessage());
        }
    }

    public static String getExtension(File file) {
        String fileName = file.getName();
        int index = fileName.indexOf('.');
        return fileName.substring(index);
    }

    public static void copyFiles(String strPath, String trgPath) {
        File src = new File(strPath);
        File trg = new File(trgPath);
        if (src.isDirectory()) {
            if (trg.exists() != true) trg.mkdirs();
            String list[] = src.list();
            for (int i = 0; i < list.length; i++) {
                String strPath_1 = src.getAbsolutePath() + SEPARATOR + list[i];
                String trgPath_1 = trg.getAbsolutePath() + SEPARATOR + list[i];
                copyFiles(strPath_1, trgPath_1);
            }
        } else {
            try {
                FileChannel srcChannel = new FileInputStream(strPath).getChannel();
                FileChannel dstChannel = new FileOutputStream(trgPath).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                srcChannel.close();
                dstChannel.close();
            } catch (FileNotFoundException e) {
                System.out.println("[Error] File not found: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("[Error] " + e.getMessage());
            }
        }
    }
}
