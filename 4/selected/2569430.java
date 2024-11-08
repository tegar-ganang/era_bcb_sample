package util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Sebastian Kuerten (sebastian.kuerten@fu-berlin.de)
 * 
 */
public class TextFileUtil {

    /**
	 * Read the complete content available from the denoted InputStream into a stringl
	 * 
	 * @param is the InputStream to read from.
	 * @return the String created
	 * @throws IOException on I/O failure.
	 */
    public static String readText(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        byte[] buffer = new byte[1024];
        while (true) {
            int read = bis.read(buffer);
            if (read < 0) {
                break;
            }
            baos.write(buffer, 0, read);
        }
        byte[] bytes = baos.toByteArray();
        return new String(bytes);
    }
}
