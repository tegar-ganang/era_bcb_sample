package fr.x9c.cadmium.primitives.unix;

import java.net.InetSocketAddress;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for 'unix_getpeername' primitive.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Getpeername {

    /**
     * No instance of this class.
     */
    private Getpeername() {
    }

    /**
     * Returns the address of the remote endpoint of a socket.
     * @param ctxt context
     * @param sock socket to get address from
     * @return the address of the remote endpoint of a socket
     * @throws Fail.Exception if socket is invalid
     */
    @Primitive
    public static Value unix_getpeername(final CodeRunner ctxt, final Value sock) throws Fail.Exception {
        final Channel ch = ctxt.getContext().getChannel(sock.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "getpeername", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final java.net.Socket s = ch.asSocket();
        final java.net.DatagramSocket ds = ch.asDatagramSocket();
        if (s != null) {
            return Unix.createSockAddr(ctxt, (InetSocketAddress) s.getRemoteSocketAddress());
        } else if (ds != null) {
            return Unix.createSockAddr(ctxt, (InetSocketAddress) ds.getRemoteSocketAddress());
        } else {
            Unix.fail(ctxt, "getpeername", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
    }
}
