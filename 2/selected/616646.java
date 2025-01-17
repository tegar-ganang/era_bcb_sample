package com.sun.speech.freetts.util;

import java.io.PrintWriter;
import java.net.URL;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.IOException;
import android.content.Resources;
import com.l1ghtm4n.text2speech.SpeakReceiver;
import java.net.MalformedURLException;
import android.content.Resources.NotFoundException;

/**
 * Provides a set of generic utilities used by freetts.
 */
public class Utilities {

    private Utilities() {
    }

    /**
     * Returns a string with the given number of
     * spaces.
     *
     * @param padding the number of spaces in the string
     *
     * @return a string of length 'padding' containg only the SPACE
     * char.
     */
    public static String pad(int padding) {
        if (padding > 0) {
            StringBuffer sb = new StringBuffer(padding);
            for (int i = 0; i < padding; i++) {
                sb.append(' ');
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
     * Pads with spaces or truncates the given string to guarantee that it is
     * exactly the desired length.
     *
     * @param string the string to be padded
     * @param minLength the desired length of the string
     *
     * @return a string of length conntaining string
     * padded with whitespace or truncated
     */
    public static String pad(String string, int minLength) {
        String result = string;
        int pad = minLength - string.length();
        if (pad > 0) {
            result = string + pad(minLength - string.length());
        } else if (pad < 0) {
            result = string.substring(0, minLength);
        }
        return result;
    }

    /**
     * Removes all instances of the specified character from the given String.
     *
     * @param  fromString  the String to delete characters from
     * @param  charToDelete  the character to delete from the given String
     *
     * @return  a String with all instances of the specified char deleted
     */
    public static String deleteChar(String fromString, char charToDelete) {
        StringBuffer buffer = new StringBuffer(fromString.length());
        for (int i = 0; i < fromString.length(); i++) {
            if (fromString.charAt(i) != charToDelete) {
                buffer.append(fromString.charAt(i));
            }
        }
        return new String(buffer);
    }

    /**
     * Dumps padded text. This is a simple tool for helping dump text 
     * with padding to a Writer.
     *
     * @param pw the stream to send the output
     * @param padding the number of spaces in the string
     * @param string the string to output
     */
    public static void dump(PrintWriter pw, int padding, String string) {
        pw.print(pad(padding));
        pw.println(string);
    }

    public static URL getResourceURL(String name) {
        try {
            return new URL("file", "", name);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Returns an input stream for the given URL. If the URL
     * is pointing to a local file, returns a file input stream
     * suitable for MemoryMapped IO, otherwise, returns a buffered
     * input stream.
     *
     * @param url the url to open as a stream
     * @return the stream associated with the URL
     *
     * @throws IOException if there is trouble creating the stream
     */
    public static InputStream getInputStream(URL url) throws IOException {
        if (url.getProtocol().equals("resource")) {
            Resources res = SpeakReceiver._context.getResources();
            String resname = url.getFile();
            resname = resname.split("\\.[a-z0-9]{3}")[0];
            int id = res.getIdentifier(resname, "raw", "com.l1ghtm4n.text2speech");
            if (id == 0) {
                throw new NotFoundException();
            } else return res.openRawResource(id);
        } else {
            return url.openStream();
        }
    }

    /**
     * Outputs a string to the given stream.
     *
     * @param dos the stream
     * @param s the string to output
     *
     * @throws IOException if an I/O error occurs
     */
    public static void outString(DataOutputStream dos, String s) throws IOException {
        dos.writeShort((short) s.length());
        for (int i = 0; i < s.length(); i++) {
            dos.writeChar(s.charAt(i));
        }
    }

    /**
     * Inputs a string from a DataInputStream.
     *
     * @param dis the stream
     *
     * @return  the string 
     *
     * @throws IOException if an I/O error occurs
     */
    public static String getString(DataInputStream dis) throws IOException {
        int size = dis.readShort();
        char[] charBuffer = new char[size];
        for (int i = 0; i < size; i++) {
            charBuffer[i] = dis.readChar();
        }
        return new String(charBuffer, 0, size);
    }

    /**
     * Inputs a string from a ByteBuffer.
     *
     * @param bb the input byte buffer
     *
     * @return  the string 
     *
     * @throws IOException if an I/O error occurs
     */
    public static String getString(ByteBuffer bb) throws IOException {
        int size = bb.getShort();
        char[] charBuffer = new char[size];
        for (int i = 0; i < size; i++) {
            charBuffer[i] = bb.getChar();
        }
        return new String(charBuffer, 0, size);
    }

    /**
     * Gets a property by name and returns its value. If the property
     * cannot be found, the default is returned
     *
     * @param name the name of the property
     *
     * @param defaultValue the default value to use if the property
     * cannot be found.
     *
     * @return the string value for the property, or the defaultValue if 
     *  the property cannot be found
     */
    public static String getProperty(String name, String defaultValue) {
        String value;
        try {
            value = System.getProperty(name, defaultValue);
        } catch (SecurityException se) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Gets a boolean property by name. 
     *
     * @param name the name of the property
     *
     * @return  If there is no property with the specified name, or 
     *  if the specified name is empty or null, then false is returned. 
     *  otherwise the boolean value of the property is returned
     *
     */
    public static boolean getBoolean(String name) {
        boolean value;
        try {
            value = Boolean.getBoolean(name);
        } catch (SecurityException se) {
            value = false;
        }
        return value;
    }

    /**
     * Gets a long property by name. 
     *
     * @param name the name of the property
     *
     * @param defaultValue the default value to use if the property
     * cannot be found.
     *
     * @return the long value for the property, or the defaultValue if 
     *  the property cannot be found
     *
     */
    public static Long getLong(String name, long defaultValue) {
        Long value;
        try {
            value = Long.getLong(name, defaultValue);
        } catch (SecurityException se) {
            value = new Long(defaultValue);
        }
        return value;
    }

    /**
     * Gets an Integer property by name. 
     *
     * @param name the name of the property
     *
     * @param defaultValue the default value to use if the property
     * cannot be found.
     *
     * @return the Integer value for the property, or the defaultValue if 
     *  the property cannot be found
     *
     */
    public static Integer getInteger(String name, int defaultValue) {
        Integer value;
        try {
            value = Integer.getInteger(name, defaultValue);
        } catch (SecurityException se) {
            value = new Integer(defaultValue);
        }
        return value;
    }

    private static boolean wantDebug = getBoolean("freetts.debug");

    /**
     * A very simple debug log mechanism. If the system property
     * <code>freetts.debug</code> is <code>true</code>, print the string to
     * stderr; else, dont.
     *
     */
    public static void debug(String string) {
        if (wantDebug) System.err.println(string);
    }
}
