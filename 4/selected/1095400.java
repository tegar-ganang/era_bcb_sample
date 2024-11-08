package com.senn.magic.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class contains useful methods for working with streams and other IO stuff.
 * 
 * @author Bart Thierens
 * 
 * <br>
 * 
 * Last modification: 28/05/2011
 * 
 * @since 3.6
 */
public class IOUtils {

    private IOUtils() {
        throw new java.lang.UnsupportedOperationException();
    }

    /**
	 * Generic method that converts an {@link InputStream} to an {@link OutputStream} of choice.
	 * <br/>
	 * Method is <b>not</b> guaranteed to work for every type of {@link OutputStream}, but most will work.
	 * 
	 * @param in	an instance of any subclass of {@link InputStream}
	 * @param outClass	 a subclass of {@link OutputStream}
	 * @return <S> - an instance of the subclass provided containing your data
	 * 
	 * @throws Exception
	 */
    public static <S extends OutputStream> S convertToOutputStream(InputStream in, Class<S> outClass) throws Exception {
        OutputStream out = outClass.newInstance();
        while (in.available() != 0) out.write(in.read());
        return outClass.cast(out);
    }

    /**
	 * Closes any {@link Closeable} implementation.  
	 * 
	 * @param closeable		the closeable to close.  if null, nothing is done, if already closed, this method has no effect.
	 * 
	 * @throws IOException
	 */
    public static void close(Closeable closeable) throws IOException {
        if (closeable != null) closeable.close();
    }

    /**
	 * Closes any {@link Closeable} implementation and swallows any {@link IOException} that might occur while closing. 
	 * 
	 *  @see IOUtils#close(Closeable)
	 * 
	 * @param closeable		the closeable to close.  if null, nothing is done, if already closed, this method has no effect.
	 */
    public static void closeSilently(Closeable closeable) {
        try {
            close(closeable);
        } catch (IOException ioe) {
        }
    }
}
