package jd.client.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 
 * @author Denis Migol
 * 
 */
public final class IOUtil {

    /**
	 * Don't let anyone instantiate this class.
	 */
    private IOUtil() {
    }

    /**
	 * 
	 * @param in
	 * @return
	 */
    public static final byte[] toByteArray(final InputStream in) throws IOException {
        final ByteArrayOutputStream bo = new ByteArrayOutputStream();
        int length = in.available();
        if (length > 0) {
            int read;
            byte[] buf;
            do {
                buf = new byte[length];
                while ((read = in.read(buf)) > 0) {
                    bo.write(buf, 0, read);
                }
            } while ((length = in.available()) > 0);
        } else {
            int b = 0;
            while ((b = in.read()) > -1) {
                bo.write(b);
            }
        }
        in.close();
        return bo.toByteArray();
    }

    /**
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
    public static final String toString(final InputStream in) throws IOException {
        final StringBuilder ret = new StringBuilder();
        final BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = null;
        while ((line = br.readLine()) != null) {
            ret.append(line + "\n");
        }
        br.close();
        return ret.toString();
    }
}
