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
 * This class provides implementation for write-related primitives.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Write {

    /**
     * No instance of this class.
     */
    private Write() {
    }

    /**
     * Writes buffer content to file.
     * @param ctxt context
     * @param fd destination file descriptor
     * @param buf source buffer
     * @param vofs offset of content to write
     * @param vlen length of content to write
     * @return the actual number of written bytes
     * @throws Fail.Exception if an i/o error occurs
     */
    @Primitive
    public static Value unix_write(final CodeRunner ctxt, final Value fd, final Value buf, final Value vofs, final Value vlen) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        final Channel ch = context.getChannel(fd.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "write", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final int ofs = vofs.asLong();
        final int len = vlen.asLong();
        final byte[] buff = buf.asBlock().getBytes();
        context.enterBlockingSection();
        try {
            ch.asDataOutput().write(buff, ofs, len);
            context.leaveBlockingSection();
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            context.leaveBlockingSection();
            Unix.fail(ctxt, "write", ioe);
        }
        return vlen;
    }

    /**
     * <b>Exact synonym of {@link fr.x9c.cadmium.primitives.unix.Write#unix_write(CodeRunner, Value, Value, Value, Value)}.</b> <br/>
     * Writes buffer content to file.
     * @param ctxt context
     * @param fd destination file descriptor
     * @param buf source buffer
     * @param vofs offset of content to write
     * @param vlen length of content to write
     * @return the actual number of written bytes
     * @throws Fail.Exception if an i/o error occurs
     */
    @Primitive
    public static Value unix_single_write(final CodeRunner ctxt, final Value fd, final Value buf, final Value vofs, final Value vlen) throws Fail.Exception, FalseExit {
        return unix_write(ctxt, fd, buf, vofs, vlen);
    }
}
