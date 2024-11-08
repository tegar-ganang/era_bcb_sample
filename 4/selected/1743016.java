package fr.x9c.cadmium.primitives.unix;

import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for 'unix_dup' primitive.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Dup {

    /**
     * No instance of this class.
     */
    private Dup() {
    }

    /**
     * Duplicates a file descriptor.
     * @param ctxt context
     * @param fd file descriptor to duplicate
     * @return new file descriptor referencing the same file
     * @throws Fail.Exception if file descriptor cannot be duplicated
     */
    @Primitive
    public static Value unix_dup(final CodeRunner ctxt, final Value fd) throws Fail.Exception {
        final Context c = ctxt.getContext();
        final Channel ch = c.getChannel(fd.asLong());
        if (ch != null) {
            return Value.createFromLong(c.addChannel(ch));
        } else {
            Unix.fail(ctxt, "dup", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
    }
}
