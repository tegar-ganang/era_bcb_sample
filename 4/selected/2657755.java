package net.sourceforge.epoint.io;

import java.io.*;

/**
 * <code>Writer</code> for canonical text documents
 *
 * @author <a href="mailto:nagydani@users.sourceforge.net">Daniel A. Nagy</a>
 */
public class CanonicalWriter extends Writer {

    private OutputStream out;

    String buffer;

    public CanonicalWriter(OutputStream output) {
        out = output;
        buffer = "";
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        buffer += new String(cbuf, off, len).replaceAll("\r", "").replaceAll("[\t ]*\n", "\r\n");
        String notail = buffer.replaceFirst("[\t ]*\\z", "");
        out.write(notail.getBytes("UTF-8"));
        buffer = buffer.substring(notail.length());
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        out.close();
        out = null;
    }

    /**
     * for debugging purposes
     */
    public static void main(String[] args) throws Exception {
        byte[] c = new byte[1024];
        int i;
        Writer w = new CanonicalWriter(System.out);
        while ((i = System.in.read(c)) > 0) w.write(new String(c, 0, i));
    }
}
