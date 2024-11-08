package com.maiereni.common.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stream utility class
 * 
 * @author Petre Maierean
 */
public class StreamUtils {

    public static final String CLASSPATH = "classpath:";

    /**
	 * Get a string template holder from the specified resource
	 * @param resource the path of the resource relative to the classpath 
	 * @return a string template holder
	 * @throws Exception
	 */
    public static StringTemplateHolder getTemplateForResource(String resource) throws Exception {
        return new StringTemplateHolder(readBytesFromClasspath(resource));
    }

    /**
	 * Get a string template holder from the specified file
	 * @param file the file containing the resource
	 * @return a string template holder
	 * @throws Exception
	 */
    public static StringTemplateHolder getTemplate(File file) throws Exception {
        return new StringTemplateHolder(readFile(file));
    }

    /**
	 * Stores the content of an array of bytes to a temporary location
	 * @param tmp the temporary File
	 * @param buffer the buffer
	 * @throws Exception
	 */
    public static void storeToTmp(File tmp, byte[] buffer) throws Exception {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(tmp);
            os.write(buffer);
            os.close();
            os = null;
        } finally {
            if (os != null) try {
                os.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * Stores the content of a stream to a temporary location
	 * @param tmp the temporary file that receives the content
	 * @param is the stream to persist
	 * @throws Exception
	 */
    public static void storeToTmp(File tmp, InputStream is) throws Exception {
        storeToTmp(tmp, StreamUtils.readBytes(is));
    }

    /**
	 * Persist the array of bytes to a temporary location. The temporary file is created
	 * in the system's temporary directory and assigned the specified suffix
	 * @param suffix the suffix
	 * @param buffer the array of bytes
	 * @return the temporary file object
	 * @throws Exception
	 */
    public static File storeToTmp(String suffix, byte[] buffer) throws Exception {
        File ret = File.createTempFile("ret", suffix);
        storeToTmp(ret, buffer);
        return ret;
    }

    /**
	 * Stores the content of the input stream to a temporary location The temporary file is created
	 * in the system's temporary directory and assigned the specified suffix
	 * @param suffix the suffix
	 * @param is the input stream
	 * @return
	 * @throws Exception
	 */
    public static File storeToTmp(String suffix, InputStream is) throws Exception {
        return storeToTmp(suffix, StreamUtils.readBytes(is));
    }

    /**
	 * Read the content of a file as an array of bytes
	 * @param file the file to read
	 * @return an array of bytes
	 * @throws Exception
	 */
    public static byte[] readFile(File file) throws Exception {
        InputStream is = null;
        try {
            is = new java.io.FileInputStream(file);
            return readBytes(is);
        } finally {
            if (is != null) try {
                is.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * Read the content of a file stored as a resource relative to the classpath
	 * @param resource the name of the resource
	 * @return
	 * @throws Exception
	 */
    public static byte[] readBytesFromClasspath(String resource) throws Exception {
        java.io.InputStream is = null;
        if (resource == null) return null;
        if (resource.startsWith(CLASSPATH)) {
            resource = resource.substring(CLASSPATH.length());
        }
        try {
            return StreamUtils.readBytes(is = StreamUtils.class.getResourceAsStream(resource));
        } finally {
            if (is != null) try {
                is.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * Copy the content of an input stream of bytes to an output stream of bytes
	 * @param is the input stream
	 * @param os the output stream
	 * @throws Exception
	 */
    public static void copyBytes(InputStream is, OutputStream os) throws Exception {
        byte[] buffer = new byte[4098];
        for (int i = 0; (i = is.read(buffer)) > 0; ) os.write(buffer, 0, i);
    }

    /**
	 * Copy the content of a resource in the classpath to an output stream
	 * @param resource the name of the resource
	 * @param os the output stream to copy to
	 * @throws Exception
	 */
    public static void copyBytesFromClasspath(String resource, OutputStream os) throws Exception {
        if (resource == null) return;
        if (resource.startsWith(CLASSPATH)) {
            resource = resource.substring(CLASSPATH.length());
        }
        InputStream is = null;
        try {
            is = StreamUtils.class.getResourceAsStream(resource);
            StreamUtils.copyBytes(is, os);
        } finally {
            if (is != null) try {
                is.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * Read the content of an input stream as an array of bytes
	 * @param is the input stream
	 * @return an array of bytes
	 * @throws Exception
	 */
    public static byte[] readBytes(InputStream is) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copyBytes(is, os);
        return os.toByteArray();
    }

    /**
	 * Read the content of an input stream as a string
	 * @param is the input stream
	 * @return the read string
	 * @throws Exception
	 */
    public static String read(InputStream is) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        copyBytes(is, os);
        return os.toString();
    }

    /**
	 * 
	 * @param s
	 * @return
	 * @throws Exception
	 */
    public static String toJavaScriptFriendly(String s) throws Exception {
        if (s == null) return null;
        int max = s.length();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < max; i++) {
            char c = s.charAt(i);
            switch(c) {
                case '\'':
                    sb.append("\\");
                    break;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
	 * Replaces all the occurances of a toReplace with value in the input str. If value is null or blank then
	 * the effect is the removal of all the toReplace substrings in the str
	 * @param str the input string
	 * @param toReplace the substring to replace
	 * @param value the new substring
	 * @return
	 */
    public static String replaceString(String str, String toReplace, String value) {
        if (str == null) return null;
        if (toReplace == null || "".equals(toReplace)) return str;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < str.length(); i++) {
            int ix = str.indexOf(toReplace, i);
            if (ix < 0) {
                sb.append(str.substring(i));
                break;
            }
            sb.append(str.substring(i, ix));
            if (value != null) sb.append(value);
            i = ix + toReplace.length();
        }
        return sb.toString();
    }

    private static final byte[] QUOT = "&quot;".getBytes();

    private static final byte[] LT = "&lt;".getBytes();

    private static final byte[] GT = "&gt;".getBytes();

    private static final byte[] AMP = "&amp;".getBytes();

    /**
	 * Replace the special characters &quot;, &lt;, &gt;, and &amp; in the string so 
	 * that the resulting string is HTML compatible that is it can be dispayed as is
	 * in a web browser 
	 * @param s the input string
	 * @return the fiendly form of the string
	 * @throws Exception
	 */
    public static String toHtmlFriendly(String s) throws Exception {
        return toHtmlFriendly(s, null);
    }

    /**
	 * Replace the special characters &quot;, &lt;, &gt;, and &amp; in the string so 
	 * that the resulting string is HTML compatible that is it can be dispayed as is
	 * in a web browser 
	 * @param s the input string
	 * @param chrSet the character set the input string is represented with
	 * @return
	 * @throws Exception
	 */
    public static String toHtmlFriendly(String s, String chrSet) throws Exception {
        if (s == null) return null;
        byte[] inString = null;
        if (chrSet == null) inString = s.getBytes(); else inString = s.getBytes(chrSet);
        return toHtmlFriendly(inString);
    }

    /**
	 * Replace the special characters &quot;, &lt;, &gt;, and &amp; in the input array of bytes so 
	 * that the resulting string is HTML compatible that is it can be dispayed as is in a web browser
	 * or passed as a text in an XML payload 
	 * @param buffer the array of bytes containing the string
	 * @return
	 * @throws Exception
	 */
    public static String toHtmlFriendly(byte[] buffer) throws Exception {
        return toHtmlFriendly(buffer, 0, buffer.length);
    }

    /**
	 * Replace the special characters &quot;, &lt;, &gt;, and &amp; in the input array of bytes so 
	 * that the resulting string is HTML compatible that is it can be dispayed as is in a web browser 
	 * as a text in an XML payload 
	 * @param buffer the array of bytes containing the string
	 * @param beinging the start index
	 * @param length the length
	 * @return
	 * @throws Exception
	 */
    public static String toHtmlFriendly(byte[] buffer, int begining, int length) throws Exception {
        if (buffer == null) return null;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        int marker = begining;
        byte[] b = null;
        boolean omit = false;
        int max = length + begining;
        for (int i = begining; i < max; i++) {
            switch(buffer[i]) {
                case '\"':
                    b = QUOT;
                    break;
                case '&':
                    b = AMP;
                    break;
                case '<':
                    b = LT;
                    break;
                case '>':
                    b = GT;
                    break;
                case '\n':
                case '\r':
                    omit = true;
                default:
                    b = null;
            }
            if (b != null || omit) {
                if (marker < i) {
                    os.write(buffer, marker, i - marker);
                }
                marker = i + 1;
                if (b != null) os.write(b);
                omit = false;
            }
        }
        if (marker < max) {
            os.write(buffer, marker, max - marker);
        }
        return os.toString();
    }
}
