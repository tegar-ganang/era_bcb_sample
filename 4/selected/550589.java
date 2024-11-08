package de.tud.kom.nat.comm.util;

import java.nio.channels.SelectableChannel;
import de.tud.kom.nat.comm.IMessageHook;
import de.tud.kom.nat.comm.msg.IAnswer;
import de.tud.kom.nat.comm.msg.IEnvelope;
import de.tud.kom.nat.comm.msg.IMessage;
import de.tud.kom.nat.util.IPredicate;

/**
 * This class is used to simulate blocking behavior of the asynchronous messaging system
 * of the comm-package. It can be installed using <tt>IMessageProcessor.installHook()</tt>. When the
 * hook is installed, the first envelope which applies to the predicate will be saved.
 * 
 * Start to wait for the message using the <tt>waitForMessage()</tt> method.
 *
 * @author Matthias Weinert
 */
public class BlockingHook implements IMessageHook {

    /** The predicate. */
    private IPredicate<IEnvelope> pred;

    /** The envelope which will possibly be received. */
    private IEnvelope envelope = null;

    /**
	 * Creates a BlockingHook with the given predicate.
	 * @param pred
	 */
    public BlockingHook(IPredicate<IEnvelope> pred) {
        this.pred = pred;
    }

    /**
	 * This method is called by the message processor to note that we got our message.
	 */
    public void notify(IEnvelope env, IPredicate<IEnvelope> predicate) {
        if (predicate != this.pred) return;
        synchronized (this) {
            envelope = env;
            notifyAll();
        }
    }

    /**
	 * Waits for the first message which applies to the predicate, using the default timeout time.
	 * @return first envelope which applies to the predicate or null, if we got a timeout
	 */
    public IEnvelope waitForMessage() {
        return waitForMessage(DEFAULT_TIMEOUT);
    }

    /**
	 * Wait for max. <tt>timeout</tt> milliseconds until a message is received which fits the predicate.
	 * @param timeout max wait in milliseconds
	 * @return first envelope which applies to the predicate or null, if we got a timeout
	 */
    public IEnvelope waitForMessage(int timeout) {
        long waitUntil = System.currentTimeMillis() + timeout;
        while (envelope == null) {
            synchronized (this) {
                long maxWait = waitUntil - System.currentTimeMillis();
                if (maxWait <= 0) break;
                try {
                    wait(maxWait);
                } catch (InterruptedException e) {
                }
            }
        }
        return envelope;
    }

    /**
	 * Returns the predicate of this hook.
	 * @return predicate
	 */
    public IPredicate<IEnvelope> getPredicate() {
        return pred;
    }

    /**
	 * Returns a hook that will wait for the message on the given channel of the given type.
	 * @param channel the channel where the message has to be received
	 * @param msgClazz the class of the message which has to be received
	 * @return BlockingHook that will wait until the answer arrives or a timeout occurs
	 */
    public static <T extends IMessage> BlockingHook createAwaitMessageHook(final SelectableChannel channel, final Class<T> msgClazz) {
        IPredicate<IEnvelope> pred = new IPredicate<IEnvelope>() {

            public boolean appliesTo(IEnvelope obj) {
                return obj.getChannel().equals(channel) && obj.getMessage().getClass().equals(msgClazz);
            }
        };
        return new BlockingHook(pred);
    }

    /**
	 * Returns a hook that will wait for the answer of the given message.
	 * @param msg a message / request
	 * @return BlockingHook that will wait until the answer arrives or a timeout occurs
	 */
    public static BlockingHook createMessageAnswerHook(final IMessage msg) {
        IPredicate<IEnvelope> pred = new IPredicate<IEnvelope>() {

            public boolean appliesTo(IEnvelope obj) {
                return obj.getMessage() instanceof IAnswer && ((IAnswer) obj.getMessage()).getRequestID().equals(msg.getMessageID());
            }
        };
        return new BlockingHook(pred);
    }

    /** The default timeout in milliseconds. */
    public static final int DEFAULT_TIMEOUT = 700;
}
