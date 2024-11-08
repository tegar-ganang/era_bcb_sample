package fr.x9c.cadmium.primitives.unix;

import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for 'unix_select' primitive.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Select {

    /**
     * No instance of this class.
     */
    private Select() {
    }

    /**
     * Minimal implementation (as used by system thread for delay). <br/>
     * Suspend execution for given time if all descriptor lists are empty.
     * @param ctxt context
     * @param readfds list of descriptor to wait for (read)
     * @param writefds list of descriptor to wait for (write)
     * @param exceptfds list of descriptor to wait for (except)
     * @param timeout maximum time to wait
     * @return <i>(readfds, writefds, exceptfds)</i>
     * @throws FalseExit if another thread exits the program
     * @throws Fail.Exception if an asynchronous exception has been thrown
     */
    @Primitive
    public static Value unix_select(final CodeRunner ctxt, final Value readfds, final Value writefds, final Value exceptfds, final Value timeout) throws FalseExit, Fail.Exception {
        final Context context = ctxt.getContext();
        if ((readfds == Value.EMPTY_LIST) && (writefds == Value.EMPTY_LIST) && (exceptfds == Value.EMPTY_LIST)) {
            final double t = timeout.asBlock().asDouble();
            if (t > 0.0) {
                context.enterBlockingSection();
                try {
                    Thread.sleep((long) (t * Unix.MILLISECS_PER_SEC));
                } catch (final InterruptedException ie) {
                    final FalseExit fe = FalseExit.createFromContext(context);
                    fe.fillInStackTrace();
                    throw fe;
                }
                context.leaveBlockingSection();
            }
        }
        final Block res = Block.createBlock(0, readfds, writefds, exceptfds);
        return Value.createFromBlock(res);
    }
}
