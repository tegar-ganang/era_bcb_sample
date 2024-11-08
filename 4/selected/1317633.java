package jaxlib.io.stream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Common i/o utilities.
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">J�rg Wa�mer</a>
 * @since   JaXLib 1.0
 * @version $Id: IO.java 1044 2004-04-06 16:37:29Z joerg_wassmer $.00
 */
public class IO extends IOImpl {

    protected IO() throws InstantiationException {
        throw new InstantiationException();
    }

    /**
   * Writes all remaining elements in specified buffer to specified stream.
   * When this method returns the buffer has no more remaining elements.
   * <p>
   * The buffer's position is undefined if an I/O error occurs.
   * </p>
   *
   * @param out       the stream to write elements to.
   * @param data      the buffer containing the elements to write.
   *
   * @throws IOException                if one occurs in the <tt>writeLong</tt> method of the stream.
   * @throws NullPointerException       if <tt>(out == null) || (data == null)</tt>.
   *
   * @since JaXLib 1.0
   */
    public static int write(OutputStream out, ByteBuffer data) throws IOException {
        WritableByteChannel ch;
        if (out instanceof WritableByteChannel) ch = (WritableByteChannel) out; else if (out instanceof FileOutputStream) ch = ((FileOutputStream) out).getChannel(); else ch = null;
        if (ch != null) {
            int count = data.remaining();
            while (data.hasRemaining()) ch.write(data);
            return count;
        } else return IOImpl.write(out, data);
    }
}
