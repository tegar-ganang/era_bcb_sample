package fr.x9c.cadmium.primitives.unix;

import java.io.IOException;
import java.io.InterruptedIOException;
import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Custom;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for seek-related primitives.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Lseek {

    /**
     * No instance of this class.
     */
    private Lseek() {
    }

    /**
     * Moves a file pointer.
     * @param ctxt context
     * @param fd file descriptor
     * @param ofs offset
     * @param cmd <ul>
     *              <li>if <i>0</i> then file pointer is set to <i>ofs</i></li>
     *              <li>if <i>1</i> then file pointer is set to <i>ofs + current position</i></li>
     *              <li>if <i>2</i> then file pointer is set to <i>ofs + file size</i></li>
     *            </ul>
     * @return new position of file pointer
     * @throws Fail.Exception if seek fails
     */
    @Primitive
    public static Value unix_lseek(final CodeRunner ctxt, final Value fd, final Value ofs, final Value cmd) throws Fail.Exception, FalseExit {
        try {
            final Channel ch = ctxt.getContext().getChannel(fd.asLong());
            if (ch == null) {
                Unix.fail(ctxt, "lseek", Unix.INVALID_DESCRIPTOR_MSG);
                return Value.UNIT;
            }
            final long res = ch.seek(ofs.asLong(), cmd.asLong());
            return Value.createFromLong((int) res);
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            Unix.fail(ctxt, "lseek", ioe);
            return Value.UNIT;
        }
    }

    /**
     * Moves a file pointer.
     * @param ctxt context
     * @param fd file descriptor
     * @param ofs offset
     * @param cmd <ul>
     *              <li>if <i>0</i> then file pointer is set to <i>ofs</i></li>
     *              <li>if <i>1</i> then file pointer is set to <i>ofs + current position</i></li>
     *              <li>if <i>2</i> then file pointer is set to <i>ofs + file size</i></li>
     *            </ul>
     * @return new position of file pointer
     * @throws Fail.Exception if seek fails
     */
    @Primitive
    public static Value unix_lseek_64(final CodeRunner ctxt, final Value fd, final Value ofs, final Value cmd) throws Fail.Exception, FalseExit {
        try {
            final Channel ch = ctxt.getContext().getChannel(fd.asLong());
            if (ch == null) {
                Unix.fail(ctxt, "lseek_64", Unix.INVALID_DESCRIPTOR_MSG);
                return Value.UNIT;
            }
            final long res = ch.seek(ofs.asBlock().asInt64(), cmd.asLong());
            final Block b = Block.createCustom(Custom.INT_64_SIZE, Custom.INT_64_OPS);
            b.setInt64(res);
            return Value.createFromBlock(b);
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            Unix.fail(ctxt, "lseek_64", ioe);
            return Value.UNIT;
        }
    }
}
