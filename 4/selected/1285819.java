package fr.x9c.cadmium.primitives.unix;

import java.io.IOException;
import java.io.InterruptedIOException;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for 'unix_listen' primitive.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Listen {

    /**
     * No instance of this class.
     */
    private Listen() {
    }

    /**
     * Configures a socket such that it will listen for connections.
     * @param ctxt context
     * @param socket socket to modify
     * @param backlog maxmium number of pending connections
     * @return <i>unit</i>
     * @throws Fail.Exception if an i/o error occurs
     */
    @Primitive
    public static Value unix_listen(final CodeRunner ctxt, final Value socket, final Value backlog) throws Fail.Exception, FalseExit {
        final Channel ch = ctxt.getContext().getChannel(socket.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "listen", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        try {
            ch.socketListen(backlog.asLong());
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            Unix.fail(ctxt, "listen", ioe);
        }
        return Value.UNIT;
    }
}
