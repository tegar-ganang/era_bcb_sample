package fr.x9c.cadmium.primitives.unix;

import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for 'unix_getsockname' primitive.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Getsockname {

    /**
     * No instance of this class.
     */
    private Getsockname() {
    }

    /**
     * Returns the address of the local endpoint of a socket.
     * @param ctxt context
     * @param sock socket to get address from
     * @return the address of the local endpoint of a socket
     * @throws Fail.Exception if socket is invalid
     */
    @Primitive
    public static Value unix_getsockname(final CodeRunner ctxt, final Value sock) throws Fail.Exception {
        final Channel ch = ctxt.getContext().getChannel(sock.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "getsockname", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        return Unix.createSockAddr(ctxt, ch.getSocketAddress());
    }
}
