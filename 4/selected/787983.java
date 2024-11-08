package org.dlib.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOLib {

    private static final int BUFFER_LEN = 2 * 1024;

    public static boolean copy(String source, String dest) {
        int bytes;
        byte array[] = new byte[BUFFER_LEN];
        try {
            InputStream is = new FileInputStream(source);
            OutputStream os = new FileOutputStream(dest);
            while ((bytes = is.read(array, 0, BUFFER_LEN)) > 0) os.write(array, 0, bytes);
            is.close();
            os.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Given a string, changes all chars that cannot be used as a filename
	  */
    public static String getFNString(String filename) {
        char ca[] = filename.toCharArray();
        for (int i = 0; i < filename.length(); i++) {
            if (ca[i] >= 'a' && ca[i] <= 'z') continue;
            if (ca[i] >= 'A' && ca[i] <= 'Z') continue;
            if (ca[i] >= '0' && ca[i] <= '9') continue;
            if (ca[i] == '-' || ca[i] == '_') continue;
            ca[i] = '_';
        }
        return new String(ca);
    }

    public static String loadTextFile(String fileName) {
        try {
            FileReader in = new FileReader(fileName);
            char array[] = new char[BUFFER_LEN];
            StringBuffer sb = new StringBuffer();
            int bytes;
            while ((bytes = in.read(array)) > 0) sb.append(new String(array, 0, bytes));
            in.close();
            return sb.toString();
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean saveTextFile(String fileName, String text) {
        try {
            FileWriter out = new FileWriter(fileName);
            out.write(text);
            out.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
