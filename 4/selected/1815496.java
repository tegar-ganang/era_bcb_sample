package uk.org.ogsadai.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Random;
import junit.framework.TestCase;

/**
 * A set of file utilities. 
 *
 * @author The OGSA-DAI Team.
 */
public class FileUtilities {

    /** Copyright statement */
    private static final String COPYRIGHT_NOTICE = "Copyright (c) The University of Edinburgh 2007";

    /**
     * Reads the content of a file and returns it as a string.
     * 
     * @param path
     *            the path of the file to read
     * @return content of the file
     * @throws IOException
     *             if there was an error opening or reading from the file
     */
    public static String toString(String path) throws IOException {
        StringWriter writer = new StringWriter();
        readFileAndWriteToWriter(path, writer);
        return writer.toString();
    }

    /**
     * Reads the content of a file and returns it as a character array.
     * 
     * @param path
     *            the path of the file to read
     * @return content of the file
     * @throws IOException 
     */
    public static char[] toCharArray(String path) throws IOException {
        final CharArrayWriter writer = new CharArrayWriter();
        readFileAndWriteToWriter(path, writer);
        return writer.toCharArray();
    }

    /**
     * Reads a character file on the specified path and writes the contents to
     * the given writer.
     * 
     * @param path
     *            path of the file whose contents to read
     * @param writer
     *            destination of contents
     * @throws IOException
     *             if there was an error opening or reading from the file
     */
    private static void readFileAndWriteToWriter(String path, Writer writer) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(path));
        char[] buffer = new char[2048];
        int chars = 0;
        while ((chars = in.read(buffer)) >= 0) {
            writer.write(buffer, 0, chars);
        }
        in.close();
    }

    /**
     * Asserts that two input streams are equal.  A JUnit assertion will fail
     * if the data streams differ at any point.
     * 
     * @param expectedInputStream expected input stream
     * 
     * @param actualInputStream actual input stream
     * 
     * @throws IOException if an error occurs while reading from the steams.
     */
    public static void assertInputStreamEquality(InputStream expectedInputStream, InputStream actualInputStream) throws IOException {
        InputStream bufferedExpectedInputStream = new BufferedInputStream(expectedInputStream);
        InputStream bufferedActualInputStream = new BufferedInputStream(actualInputStream);
        long byteIndex = 0;
        int expectedValue;
        int actualValue;
        boolean finished = false;
        while (!finished) {
            expectedValue = bufferedExpectedInputStream.read();
            actualValue = bufferedActualInputStream.read();
            byteIndex++;
            TestCase.assertEquals("Input Stream Equality error at byte number " + byteIndex, expectedValue, actualValue);
            if (expectedValue == -1) {
                finished = true;
            }
        }
    }

    /**
     * Asserts the two readers give access to identical data. A JUnit assertion
     * will fail if the data streams differ at any point listing the character
     * of difference. A JUnit assertion will fail if the streams do not contain
     * the same volume of data.
     * 
     * @param firstReader     first reader for comparison
     * @param secondReader    second reader for comparison
     * 
     * @throws IOException  - reading problem
     */
    public static void assertReaderEquality(Reader firstReader, Reader secondReader) throws IOException {
        int iCharNumber = 0;
        int fisChar;
        int sisChar;
        boolean finished = false;
        while (!finished) {
            fisChar = firstReader.read();
            sisChar = secondReader.read();
            TestCase.assertEquals("Error at character number: " + iCharNumber, fisChar, sisChar);
            iCharNumber++;
            if (fisChar == -1) {
                finished = true;
            }
        }
    }

    /**
     * Creates a binary data file containing random data.
     * 
     * @param numBytes number of bytes to write to the binary data file.
     * 
     * @return reference to the binary data file.
     * 
     * @throws IOException if an error occurs while creating the file.
     */
    public static File createBinaryDataFile(long numBytes) throws IOException {
        Random random = new Random();
        File tmpFile = File.createTempFile("testBinaryFile", "dat");
        OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpFile));
        for (long i = 0; i < numBytes; ++i) {
            os.write(random.nextInt(256));
        }
        os.close();
        return tmpFile;
    }
}
