package fr.x9c.cadmium.primitives.unix;

import java.io.IOException;
import java.io.InterruptedIOException;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for 'unix_read' primitive.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Read {

    /**
     * No instance of this class.
     */
    private Read() {
    }

    /**
     * Reads buffer content from file.
     * @param ctxt context
     * @param fd source file descriptor
     * @param buf destination buffer
     * @param vofs offset of content to read
     * @param vlen length of content to read
     * @return the actual number of read bytes
     * @throws Fail.Exception if an i/o error occurs
     */
    @Primitive
    public static Value unix_read(final CodeRunner ctxt, final Value fd, final Value buf, final Value vofs, final Value vlen) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        final Channel ch = context.getChannel(fd.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "read", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final int ofs = vofs.asLong();
        final int len = vlen.asLong();
        final byte[] buff = buf.asBlock().getBytes();
        try {
            context.enterBlockingSection();
            ch.asDataInput().readFully(buff, ofs, len);
            context.leaveBlockingSection();
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            context.leaveBlockingSection();
            Unix.fail(ctxt, "read", ioe);
        }
        return vlen;
    }
}
