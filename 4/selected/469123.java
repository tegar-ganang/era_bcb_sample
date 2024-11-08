package org.encog.workbench.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import org.encog.workbench.EncogWorkBench;

public class FileUtil {

    public static String getFileName(File file) {
        String fileName = file.toString();
        int mid = fileName.lastIndexOf(".");
        if (mid == -1) {
            return fileName;
        }
        return fileName.substring(0, mid);
    }

    public static String getFileExt(File file) {
        String fileName = file.toString();
        int mid = fileName.lastIndexOf(".");
        if (mid == -1) return "";
        return fileName.substring(mid + 1, fileName.length());
    }

    public static String readFileAsString(File filePath) throws java.io.IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

    public static String forceExtension(String name, String ext) {
        String b = getFileName(new File(name));
        return b + "." + ext;
    }

    public static void writeFileAsString(File path, String str) throws IOException {
        BufferedWriter o = new BufferedWriter(new FileWriter(path));
        o.write(str);
        o.close();
    }

    public static boolean checkOverWrite(File path) {
        if (path.exists()) {
            return EncogWorkBench.askQuestion("Overwrite", "This file already exists.  Do you wish to overwrite it?");
        }
        return true;
    }
}
