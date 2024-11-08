package sf.net.sinve.trace;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import static org.junit.Assert.assertTrue;

/**
 * @author Teemu Kanstren
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

    @Test
    public void numberGenerationValidation() {
        for (int i = 0; i < 100; i++) {
            int test = cInt(22, 44);
            assertTrue("expected >= 22, actual " + test, test >= 22);
            assertTrue("expected <= 44, actual " + test, test <= 44);
        }
    }

    public int cInt(int min, int max) {
        double rnd = Math.random();
        rnd *= (max - min);
        rnd += min;
        return (int) Math.round(rnd);
    }

    public static final String ln = "\n";

    public static String prefixWith(String content, String prefix) {
        String[] lines = content.split(ln);
        String result = "";
        for (String line : lines) {
            result += prefix + line + ln;
        }
        return result;
    }

    /**
   * Capitalizes the first letter of given string.
   *
   * @param text The text to capitalize.
   * @return Same stuff as given in input, but with first letter in uppercase.
   */
    public static String capitalizeFirstLetter(String text) {
        String temp = text.substring(0, 1).toUpperCase();
        text = temp + text.substring(1);
        return text;
    }

    /**
   * Lowercases the first letter of given string.
   *
   * @param text The text to lowercase.
   * @return Same stuff as given in input, but with first letter in lowercase.
   */
    public static String lowerCaseFirstLetter(String text) {
        String temp = text.substring(0, 1).toLowerCase();
        text = temp + text.substring(1);
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
