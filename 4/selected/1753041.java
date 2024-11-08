package org.equanda.util.io;

import javolution.lang.TextBuilder;
import java.io.*;

/**
 * Utility code for handling streams.
 *
 * @author <a href="mailto:joachim@progs.be">Joachim Van der Auwera</a>
 */
public final class StreamUtil {

    private StreamUtil() {
    }

    /**
     * Convert an inputstream to a string by reading the streams content. Using UTF-8 character encoding.
     * A newline is added at te end of the document if there was none.
     *
     * @param stream stream to convert
     * @return string containing the streams content, ending in newline
     * @throws IOException oops
     */
    public static String slurp(InputStream stream) throws IOException {
        return slurp(stream, "UTF-8");
    }

    /**
     * Convert an inputstream to a string by reading the streams content.
     * A newline is added at te end of the document if there was none.
     *
     * @param stream stream to convert
     * @param encoding character encoding to use for stream
     * @return string containing the streams content, ending in newline
     * @throws IOException oops
     */
    public static String slurp(InputStream stream, String encoding) throws IOException {
        if (stream == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, encoding));
        TextBuilder sb = TextBuilder.newInstance();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }
        br.close();
        return sb.toString();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        int data;
        while ((data = in.read()) >= 0) out.write(data);
    }
}
