package jaxlib.io.stream;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import jaxlib.lang.UnexpectedError;
import org.jaxlib.io.XDataInputTestCase;

/**
 * TODO: comment
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: ByteBufferInputStreamTest.java 1044 2004-04-06 16:37:29Z joerg_wassmer $
 */
public final class ByteBufferInputStreamTest extends XDataInputTestCase {

    public static void main(String[] args) {
        runSuite(ByteBufferInputStreamTest.class);
    }

    public ByteBufferInputStreamTest(String name) {
        super(name);
    }

    protected XDataInput createStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());
        for (int b = in.read(); b != -1; b = in.read()) out.write(b);
        return new ByteBufferInputStream(ByteBuffer.wrap(out.toByteArray()));
    }
}
