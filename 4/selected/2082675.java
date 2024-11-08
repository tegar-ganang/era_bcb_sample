package com.hrstc.utils;

import java.io.*;

/**
 *
 * @author  wani
 */
public class FileUtils {

    /** Creates a new instance of FileUtils */
    public FileUtils() {
    }

    /**
     *
     * @return copy of a file
     */
    public static File copy(String inFileName, String outFileName) throws IOException {
        File inputFile = new File(inFileName);
        File outputFile = new File(outFileName);
        FileReader in = new FileReader(inputFile);
        FileWriter out = new FileWriter(outputFile);
        int c;
        while ((c = in.read()) != -1) out.write(c);
        in.close();
        out.close();
        return outputFile;
    }

    public static String fileToString(String inFileName) throws IOException {
        File inputFile = new File(inFileName);
        FileReader in = new FileReader(inputFile);
        StringBuffer strb = new StringBuffer();
        int c;
        while ((c = in.read()) != -1) {
            strb.append((char) c);
        }
        in.close();
        return strb.toString();
    }
}
