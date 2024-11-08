package free.chess.conn;

import java.io.*;

/**
 * A class defining various utility methods useful for chess server bots.
 */
public class Utils {

    /**
   * Loads a text file with the given name from the local drive, converts it to
   * a String and returns the String. Returns null if didn't find such a file or
   * an IOException was thrown while reading it.
   */
    public static String loadTextFile(String filename) {
        try {
            InputStream in = new FileInputStream(filename);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] arr = new byte[1000];
            int size;
            while ((size = in.read(arr)) != -1) buf.write(arr, 0, size);
            return buf.toString();
        } catch (IOException e) {
            return null;
        }
    }
}
