package com.thyante.thelibrarian.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.channels.FileChannel;

/**
 * Utility functions for dealing with files.
 * 
 * @author Matthias-M. Christen
 */
public class FileUtil {

    /**
	 * Copies the file <code>fileIn</code> to the file <code>fileOut</code>.
	 * @param fileIn The input file
	 * @param fileOut The output file
	 * @throws IOException
	 */
    public static void copyFile(File fileIn, File fileOut) throws IOException {
        FileChannel chIn = new FileInputStream(fileIn).getChannel();
        FileChannel chOut = new FileOutputStream(fileOut).getChannel();
        try {
            chIn.transferTo(0, chIn.size(), chOut);
        } catch (IOException e) {
            throw e;
        } finally {
            if (chIn != null) chIn.close();
            if (chOut != null) chOut.close();
        }
    }

    /**
	 * Reads the text file <code>file</code> and stores the contents in a {@link StringBuffer},
	 * which is returned.
	 * @param file The file to read
	 * @return A string buffer containing the contents of <code>file</code>
	 */
    public static StringBuffer readFileToStringBuffer(File file) {
        try {
            return readFileToStringBuffer(new FileReader(file));
        } catch (FileNotFoundException e) {
        }
        return new StringBuffer();
    }

    /**
	 * Reads the text file <code>file</code> and stores the contents in a {@link StringBuffer},
	 * which is returned.
	 * @param file The file to read
	 * @return A string buffer containing the contents of <code>file</code>
	 */
    public static StringBuffer readFileToStringBuffer(Reader reader) {
        StringBuffer sb = new StringBuffer();
        char[] rgBuf = new char[1024];
        try {
            BufferedReader in = new BufferedReader(reader);
            for (; ; ) {
                int nCharsRead = in.read(rgBuf);
                if (nCharsRead == -1) break;
                sb.append(rgBuf, 0, nCharsRead);
            }
        } catch (IOException e) {
        }
        return sb;
    }

    /**
	 * Writes the string <code>strContents</code> to the file <code>file</code>.
	 * @param file The file to write to
	 * @param strContents The string to write to file
	 */
    public static void writeStringToFile(File file, String strContents) {
        try {
            PrintWriter out = new PrintWriter(file);
            out.print(strContents);
            out.flush();
            out.close();
        } catch (IOException e) {
        }
    }
}
