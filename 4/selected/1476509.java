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
 * This class provides implementation for 'unix_shutdown' primitive.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Shutdown {

    /** Used to close socket input stream. */
    private static final int SHUTDOWN_RECEIVE = 0;

    /** Used to close socket output stream. */
    private static final int SHUTDOWN_SEND = 1;

    /** Used to close both socket input and output streams. */
    private static final int SHUTDOWN_ALL = 2;

    /**
     * No instance of this class.
     */
    private Shutdown() {
    }

    /**
     * Shutdowns a socket.
     * @param ctxt context
     * @param socket socket to shutdown
     * @param cmd action to perform : <br/>
     *            <ul>
     *              <li><tt>0</tt>: closes input</li>
     *              <li><tt>1</tt>: closes output</li>
     *              <li><tt>2</tt>: closes both input and output</li>
     *            </ul>
     * @return <i>unit</i>
     * @throws Fail.Exception if shutdown fails
     */
    @Primitive
    public static Value unix_shutdown(final CodeRunner ctxt, final Value socket, final Value cmd) throws Fail.Exception, FalseExit {
        final Channel ch = ctxt.getContext().getChannel(socket.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "shutdown", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final java.net.Socket s = ch.asSocket();
        if (s != null) {
            try {
                switch(cmd.asLong()) {
                    case Shutdown.SHUTDOWN_RECEIVE:
                        s.shutdownInput();
                        break;
                    case Shutdown.SHUTDOWN_SEND:
                        s.shutdownOutput();
                        break;
                    case Shutdown.SHUTDOWN_ALL:
                        s.shutdownOutput();
                        s.shutdownInput();
                        break;
                    default:
                        assert false : "invalid shutdown command";
                }
            } catch (final InterruptedIOException iioe) {
                final FalseExit fe = FalseExit.createFromContext(ctxt.getContext());
                fe.fillInStackTrace();
                throw fe;
            } catch (final IOException ioe) {
                Unix.fail(ctxt, "shutdown", ioe);
            }
        } else {
            Unix.fail(ctxt, "shutdown", Unix.INVALID_DESCRIPTOR_MSG);
        }
        return Value.UNIT;
    }
}
