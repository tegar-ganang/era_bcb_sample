package com.oranda.util;

import java.io.*;

public class FileUtils {

    public static void copy(String path1, String path2) throws IOException {
        FileReader in = new FileReader(path1);
        FileWriter out = new FileWriter(path2);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
    }

    public static void writeToFile(String path, String str) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(path));
        out.write(str, 0, str.length());
        out.close();
    }

    public static String readFile(String path) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(path));
        StringBuffer allText = new StringBuffer();
        String strLine;
        while ((strLine = in.readLine()) != null) {
            allText.append(strLine);
            allText.append("\n\r");
        }
        in.close();
        return allText.toString();
    }
}
