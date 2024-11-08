package fr.x9c.cadmium.primitives.unix;

import java.io.OutputStream;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for tty-related primitives.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Isatty {

    /**
     * No instance of this class.
     */
    private Isatty() {
    }

    /**
     * Tests whether the passed file descriptor denotes a tty.
     * @param ctxt context
     * @param fd file descriptor to test
     * @return <i>true</i> if and only if the passed descriptor is equal to one
     * @throws Fail.Exception if the passed file descriptor is invalid
     */
    @Primitive
    public static Value unix_isatty(final CodeRunner ctxt, final Value fd) throws Fail.Exception {
        final Channel ch = ctxt.getContext().getChannel(fd.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "iastty", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final OutputStream os = ch.asOutputStream();
        return (os == System.out) || (os == System.err) ? Value.TRUE : Value.FALSE;
    }
}
