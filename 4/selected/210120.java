package org.subethamail.rtest.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeff Schnitzer
 */
public class Samples {

    /** */
    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(Samples.class);

    /**
	 * Gets a message.
	 * 
	 * @param file is the filename without path of the entire message.
	 */
    public static byte[] getMessage(String file) throws IOException {
        InputStream inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/subethamail/rtest/msg/" + file);
        if (inStream == null) throw new IllegalStateException("Couldn't find resource");
        return streamToBytes(inStream);
    }

    /**
	 * Creates a byte array from a stream.
	 */
    public static byte[] streamToBytes(InputStream inStream) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        while (inStream.available() > 0) outStream.write(inStream.read());
        return outStream.toByteArray();
    }
}
