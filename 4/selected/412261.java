package org.junit.remote.internal.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author  Steven Stallion
 * @version $Revision: 2 $
 */
public class Strings {

    public static boolean isEmpty(String s) {
        return isEmpty(s, true);
    }

    public static boolean isEmpty(String s, boolean trim) {
        if (s == null) {
            return true;
        }
        if (trim) {
            s = s.trim();
        }
        return s.length() == 0;
    }

    public static String toString(InputStream in) throws IOException {
        assert in != null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[16384];
        int read;
        do {
            read = in.read(b);
            if (read > 0) {
                out.write(b, 0, read);
            }
        } while (read != -1);
        return out.toString();
    }

    private Strings() {
    }
}
