package fr.x9c.cadmium.primitives.unix;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
 * This class provides implementation for 'unix_accept' primitive.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Accept {

    /**
     * No instance of this class.
     */
    private Accept() {
    }

    /**
     * Listens for a connection (blocking).
     * @param ctxt context
     * @param socket socket to listen from
     * @return <i>(file_descriptor, socket_address)</i>
     * @throws Fail.Exception if an i/o error occurs
     */
    @Primitive
    public static Value unix_accept(final CodeRunner ctxt, final Value socket) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        final Channel ch = context.getChannel(socket.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "accept", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final ServerSocket serv = ch.asServerSocket();
        if (serv == null) {
            Unix.fail(ctxt, "accept", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        try {
            context.enterBlockingSection();
            final java.net.Socket s = serv.accept();
            context.leaveBlockingSection();
            final int fd = ctxt.getContext().addChannel(new Channel(s));
            final Block res = Block.createBlock(0, Value.createFromLong(fd), Unix.createSockAddr(ctxt, (InetSocketAddress) s.getRemoteSocketAddress()));
            return Value.createFromBlock(res);
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            context.leaveBlockingSection();
            Unix.fail(ctxt, "accept", ioe);
            return Value.UNIT;
        }
    }
}
