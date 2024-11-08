package flipsky.util;

import java.io.*;

/** Utility functions.
 * 
 * This class implements a number of utility functions as static methods
 * that can be used to simplify MIDlet development.
 */
public class ResourceUtilities {

    /** Encoding to use for string conversion */
    public static final String PORTABLE_ENCODING = "UTF-8";

    /** Get a resource as a stream
	 */
    public static final InputStream getResourceAsStream(String resourceName) {
        return resourceName.getClass().getResourceAsStream(resourceName);
    }

    /** Get a resource as a reader
	 */
    public static final Reader getResourceAsReader(String resourceName) {
        InputStream ins = getResourceAsStream(resourceName);
        if (ins == null) return null;
        return new InputStreamReader(ins);
    }

    /** Get a resource as an array of bytes
	*/
    public static final byte[] getResourceAsBytes(String resourceName) throws IOException {
        InputStream ins = getResourceAsStream(resourceName);
        if (ins == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] nextByte = new byte[1];
        while ((ins.read(nextByte, 0, 1)) != (-1)) baos.write(nextByte[0]);
        if (baos.size() > 0) return baos.toByteArray();
        return null;
    }

    /** Get a resource as a single large string
	 * 
	 */
    public static final String getResourceAsString(String resourceName) throws IOException {
        byte[] data = getResourceAsBytes(resourceName);
        if (data == null) return null;
        try {
            return new String(data, PORTABLE_ENCODING);
        } catch (UnsupportedEncodingException ex) {
        }
        return null;
    }
}
