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
 * This class provides implementation for 'unix_dup2' primitive.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Dup2 {

    /**
     * No instance of this class.
     */
    private Dup2() {
    }

    /**
     * Duplicates a file descriptor.
     * @param ctxt context
     * @param fd1 file descriptor to duplicate
     * @param fd2 new file descriptor (closed if already used)
     * @return <i>unit</i>
     * @throws Fail.Exception if file descriptor cannot be duplicated
     */
    @Primitive
    public static Value unix_dup2(final CodeRunner ctxt, final Value fd1, final Value fd2) throws Fail.Exception, FalseExit {
        final Context c = ctxt.getContext();
        final Channel old = c.removeChannel(fd2.asLong());
        if (old != null) {
            try {
                old.setFD(-1);
                old.close();
            } catch (final InterruptedIOException iioe) {
                final FalseExit fe = FalseExit.createFromContext(c);
                fe.fillInStackTrace();
                throw fe;
            } catch (final IOException ioe) {
                Unix.fail(ctxt, "dup2", ioe);
            }
        }
        final Channel ch = c.getChannel(fd1.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "dup2", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        c.setChannel(fd2.asLong(), ch);
        return Value.UNIT;
    }
}
