package sf.net.sinve;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Teemu Kanstrén
 */
public class TestUtils {

    public static String getFileContents(Object o, String fileName) throws IOException {
        InputStream in = o.getClass().getResourceAsStream(fileName);
        String text = stringForStream(in);
        text = unifyLineSeparators(text);
        in.close();
        return text;
    }

    public static String unifyLineSeparators(String text) {
        text = text.replaceAll("\r\n", "\n");
        text = text.replaceAll("\r", "\n");
        return text;
    }

    public static String stringForStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = new byte[512];
        int readBytes;
        while ((readBytes = in.read(bytes)) > 0) {
            out.write(bytes, 0, readBytes);
        }
        return new String(out.toByteArray());
    }
}
