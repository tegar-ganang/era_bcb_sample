package com.aptana.ia.fileutils;

import java.io.*;

public class OtherUtils {

    private static final int BUFFER_SIZE = 64 * 1024;

    private static byte[] BUFFER = null;

    /**
	 *	Create a buffer of the size indicated, and copy the InputStream to the
	 *	OutputStream fully.
	 *	
	 *	@returns the number of bytes copied
	 */
    public static long bufStreamCopy(InputStream is, OutputStream os, int bufferSizeInBytes) throws IOException {
        byte[] b = new byte[bufferSizeInBytes];
        return bufStreamCopy(is, os, b);
    }

    /**
	 *	Copy the InputStream to the OutputStream fully, using a shared buffer.
	 *	
	 *	@returns the number of bytes copied
	 */
    public static long bufStreamCopy(InputStream is, OutputStream os) throws IOException {
        if (BUFFER == null) {
            BUFFER = new byte[BUFFER_SIZE];
        }
        synchronized (BUFFER) {
            return bufStreamCopy(is, os, BUFFER);
        }
    }

    /**
	 *	Copy the InputStream to the OutputStream fully, using the
	 *	indicated buffer.
	 *	
	 *	@returns the number of bytes copied
	 */
    public static long bufStreamCopy(InputStream is, OutputStream os, byte[] b) throws IOException {
        long bytes = 0;
        int read = is.read(b, 0, b.length);
        while (read != -1) {
            os.write(b, 0, read);
            bytes += read;
            read = is.read(b, 0, b.length);
        }
        os.flush();
        return bytes;
    }
}
