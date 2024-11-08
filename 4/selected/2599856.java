package jimm.twice.xml.dom;

import jimm.twice.util.Base64;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * This element writes the contents of a file without reading the contents
 * into memory all at once. By default, it base64-encodes the file
 * contents. If the encoding is set to <code>ENCODING_RAW</code>, then the
 * contents will be surrounded by "&lt;![CDATA[" and "]]&gt;" when output
 * as XML.
 * <p>
 * This is a subclass of <code>Text</code> for type identification only;
 * the text instance variable is unused.
 *
 * @author Jim Menard, <a href="mailto:jimm@io.com">jimm@io.com</a>
 */
public class InlineFile extends Text {

    public static final int ENCODING_BASE64 = 0;

    public static final int ENCODING_RAW = 1;

    public static final int BUFSIZ = 4096;

    protected File file;

    protected int encoding;

    /**
 * Constructor. The file's contents will be base64 encoded.
 *
 * @param f a file
 */
    public InlineFile(File f) {
        this(f, ENCODING_BASE64);
    }

    /**
 * Constructor.
 *
 * @param f a file
 * @param encoding either <code>ENCODING_BASE64</code> (the default)
 * or <code>ENCODING_RAW</code>.
 */
    public InlineFile(File f, int encoding) {
        super(null);
        file = f;
        this.encoding = encoding;
    }

    public int getEncoding() {
        return encoding;
    }

    public void setEncoding(int e) {
        encoding = e;
    }

    public void writeTo(OutputStream out, int openOrClose) throws IOException {
        if (openOrClose != CLOSE_TAG && file != null) {
            FileInputStream in = new FileInputStream(file);
            if (encoding == ENCODING_RAW) {
                out.write("<![CDATA[".getBytes());
                copyFileContents(in, out);
                out.write("]]>".getBytes());
            } else {
                Base64.encode(in, out);
            }
        }
    }

    protected void copyFileContents(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[BUFSIZ];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
    }
}
