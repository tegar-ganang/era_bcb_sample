package peakml.util;

import java.io.*;

/**
 * 
 */
public class IO {

    /**
	 * 
	 * 
	 * @param src
	 * @param dest
	 * @throws IOException
	 */
    public static void copyfile(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }
}
