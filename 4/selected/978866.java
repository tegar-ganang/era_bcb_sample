package fr.x9c.cadmium.primitives.unix;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for 'unix_bind' primitive.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Bind {

    /**
     * No instance of this class.
     */
    private Bind() {
    }

    /**
     * Binds a socket to an address and port.
     * @param ctxt context
     * @param socket socket to bind
     * @param addr <i>inet_addr, port</i> to bind to
     * @return <i>unit</i>
     * @throws Fail.Exception if bind fails
     */
    @Primitive
    public static Value unix_bind(final CodeRunner ctxt, final Value socket, final Value addr) throws Fail.Exception, FalseExit {
        final Channel ch = ctxt.getContext().getChannel(socket.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "bind", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final Block addrBlock = addr.asBlock();
        try {
            ch.socketBind(new InetSocketAddress(InetAddress.getByAddress(addrBlock.get(0).asBlock().getBytes()), addrBlock.get(1).asLong()));
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            Unix.fail(ctxt, "bind", ioe);
        }
        return Value.UNIT;
    }
}
