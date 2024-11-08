package fr.x9c.cadmium.primitives.unix;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for truncate-related primitives.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Ftruncate {

    /**
     * No instance of this class.
     */
    private Ftruncate() {
    }

    /**
     * Changes the length of a file.
     * @param ctxt context
     * @param fd file descriptor to modify
     * @param len new file length
     * @return <i>unit</i>
     * @throws Fail.Exception if file cannot be truncated
     */
    @Primitive
    public static Value unix_ftruncate(final CodeRunner ctxt, final Value fd, final Value len) throws Fail.Exception, FalseExit {
        try {
            final Channel ch = ctxt.getContext().getChannel(fd.asLong());
            if (ch == null) {
                Unix.fail(ctxt, "ftruncate", Unix.INVALID_DESCRIPTOR_MSG);
                return Value.UNIT;
            }
            final RandomAccessFile raf = ch.asStream();
            if (raf == null) {
                Unix.fail(ctxt, "ftruncate", Unix.INVALID_DESCRIPTOR_MSG);
                return Value.UNIT;
            }
            raf.setLength(len.asLong());
            return Value.UNIT;
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            Unix.fail(ctxt, "ftruncate", ioe);
            return Value.UNIT;
        }
    }

    /**
     * Changes the length of a file.
     * @param ctxt context
     * @param fd file descriptor to modify
     * @param len new file length
     * @return <i>unit</i>
     * @throws Fail.Exception if file cannot be truncated
     */
    @Primitive
    public static Value unix_ftruncate_64(final CodeRunner ctxt, final Value fd, final Value len) throws Fail.Exception, FalseExit {
        try {
            final Channel ch = ctxt.getContext().getChannel(fd.asLong());
            if (ch == null) {
                Unix.fail(ctxt, "ftruncate_64", Unix.INVALID_DESCRIPTOR_MSG);
                return Value.UNIT;
            }
            final RandomAccessFile raf = ch.asStream();
            if (raf == null) {
                Unix.fail(ctxt, "ftruncate_64", Unix.INVALID_DESCRIPTOR_MSG);
                return Value.UNIT;
            }
            raf.setLength(len.asBlock().asInt64());
            return Value.UNIT;
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            Unix.fail(ctxt, "ftruncate_64", ioe);
            return Value.UNIT;
        }
    }
}
