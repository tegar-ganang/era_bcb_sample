package fr.x9c.cadmium.primitives.unix;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for 'unix_connect' primitive.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Connect {

    /**
     * No instance of this class.
     */
    private Connect() {
    }

    /**
     * Connects a socket to an address.
     * @param ctxt context
     * @param socket socket to connect
     * @param addr address to connect to
     * @return <i>unit</i>
     * @throws Fail.Exception if connection fails
     */
    @Primitive
    public static Value unix_connect(final CodeRunner ctxt, final Value socket, final Value addr) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        final Channel ch = context.getChannel(socket.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "connect", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final Block addrBlock = addr.asBlock();
        try {
            context.enterBlockingSection();
            ch.socketConnect(new InetSocketAddress(InetAddress.getByAddress(addrBlock.get(0).asBlock().getBytes()), addrBlock.get(1).asLong()));
            context.leaveBlockingSection();
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            context.leaveBlockingSection();
            Unix.fail(ctxt, "connect", ioe);
        }
        return Value.UNIT;
    }
}
