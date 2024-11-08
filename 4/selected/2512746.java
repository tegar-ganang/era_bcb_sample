package de.enough.polish.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>A convenience class for accessing resources.</p>
 *
 * <p>Copyright Enough Software 2008</p>
 * @author Robert Virkus, j2mepolish@enough.de
 */
public class ResourceStreamUtil {

    /**
	 * Disallow instantiation.
	 */
    private ResourceStreamUtil() {
    }

    /**
	 * Retrieves a resource as a byte array.
	 * @param url the URL of the resource within the JAR file, e.g. /resource.xml
	 * @return the resource as byte array
	 * @throws IOException when the resource could not be read
	 * TODO: This method should also handle file:// urls.
	 */
    public static byte[] getResourceAsByteArray(String url) throws IOException {
        return toByteArray(url.getClass().getResourceAsStream(url));
    }

    /**
	 * Retrieves a resource as a byte array.
	 * @param in the input stream of the resource, the input stream will be closed automatically
	 * @return the resource as byte array
	 * @throws IOException when the resource could not be read
	 */
    public static byte[] toByteArray(InputStream in) throws IOException {
        try {
            int bufferSize = in.available();
            if (bufferSize <= 0) {
                bufferSize = 8 * 1024;
            }
            byte[] buffer = new byte[bufferSize];
            ByteArrayOutputStream out = new ByteArrayOutputStream(bufferSize);
            int read;
            while ((read = in.read(buffer, 0, bufferSize)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e.toString());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }
    }
}
