package net.boogie.calamari.domain.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Random;
import net.boogie.calamari.domain.exception.ExceptionUtils;

public class DataGenUtils {

    /** String containing characters for generating random strings */
    public static final String RANDOM_STRING_DATA = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM[]\\;',/`1234567890-={}|:\"<>?~!@#$%^&*()_+";

    /** String containing characters for generating random numeric strings */
    public static final String RANDOM_STRING_DATA_NUMERIC = "1234567890";

    /** String containing characters for generating random alpha strings */
    public static final String RANDOM_STRING_DATA_ALPHA = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** String containing characters for generating random alphanumeric strings */
    public static final String RANDOM_STRING_DATA_ALPHANUM = RANDOM_STRING_DATA_ALPHA + RANDOM_STRING_DATA_NUMERIC;

    /** Random generator */
    private static Random s_random = new Random(System.currentTimeMillis());

    /**
     * Create and return a temp File with the specified extension and content.
     * 
     * @param content the content to store in the File; must not be <code>null</code>
     * @param ext the extension to use for the File; if <code>null</code>, then ".tmp" is used
     * @return the File
     * @throws IOException if any errors are encountered
     */
    public static File createTempFile(String content, String ext) throws IOException {
        ExceptionUtils.throwIfNull(content, "content");
        File file = File.createTempFile("test", ext);
        PrintWriter pw = new PrintWriter(new FileWriter(file));
        try {
            pw.print(content);
            pw.flush();
        } finally {
            pw.close();
        }
        return file;
    }

    /**
     * Create and return a temp File with the specified extension and content.
     * 
     * @param contentStream the InputStream containing the content to store in the File; must not be
     *            <code>null</code>
     * @param ext the extension to use for the File; if <code>null</code>, then ".tmp" is used
     * @return the File
     * @throws IOException if any errors are encountered
     */
    public static File createTempFile(InputStream contentStream, String ext) throws IOException {
        ExceptionUtils.throwIfNull(contentStream, "contentStream");
        File file = File.createTempFile("test", ext);
        FileOutputStream fos = new FileOutputStream(file);
        try {
            IOUtils.copy(contentStream, fos, false);
        } finally {
            fos.close();
        }
        return file;
    }

    /**
     * Create and return a temp File with the specified extension and content.
     * 
     * @param contentUrl the URL to the content to store in the File; must not be <code>null</code>
     * @param ext the extension to use for the File; if <code>null</code>, then ".tmp" is used
     * @return the File
     * @throws IOException if any errors are encountered
     */
    public static File createTempFile(URL contentUrl, String ext) throws IOException {
        ExceptionUtils.throwIfNull(contentUrl, "contentUrl");
        InputStream is = contentUrl.openStream();
        try {
            return createTempFile(is, ext);
        } finally {
            is.close();
        }
    }

    /**
     * Generate a String of random alpha data (no numerics).
     */
    public static String genString(int length) {
        return genString(RANDOM_STRING_DATA, length);
    }

    /**
     * Generate a String of random alpha data.
     */
    public static String genAlphaString(int length) {
        return genString(RANDOM_STRING_DATA_ALPHA, length);
    }

    /**
     * Generate a String of random alpha-numeric data.
     */
    public static String genAlphaNumString(int length) {
        return genString(RANDOM_STRING_DATA_ALPHANUM, length);
    }

    /**
     * Generate a String of random numberic data.
     */
    public static String genNumString(int length) {
        return genString(RANDOM_STRING_DATA_NUMERIC, length);
    }

    /**
     * Generate a String of random data.
     */
    public static String genString(String sourceData, int length) {
        StringWriter sw = new StringWriter(length);
        int maxRand = sourceData.length();
        for (int i = 0; i < length; ++i) {
            sw.write(sourceData, s_random.nextInt(maxRand), 1);
        }
        return sw.toString();
    }

    /**
     * Generate some random bytes.
     */
    public static byte[] genBytes(int length) {
        byte[] bytes = new byte[length];
        s_random.nextBytes(bytes);
        return bytes;
    }

    /**
     * Generate a random boolean.
     */
    public static boolean genBoolean() {
        return s_random.nextBoolean();
    }

    /**
     * Generate a random integer.
     */
    public static int genInt() {
        return s_random.nextInt();
    }

    /**
     * Generate a random integer up to some max.
     */
    public static int genInt(int max) {
        return s_random.nextInt(max);
    }
}
