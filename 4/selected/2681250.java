package de.tud.kom.nat.comm.msg;

import java.io.Serializable;
import java.nio.channels.Channel;

/**
 * The envelope contains information about the source of a message.
 *
 * @author Matthias Weinert
 */
public class Envelope implements IEnvelope, Serializable {

    /** serial ID */
    private static final long serialVersionUID = 8969524893109471203L;

    /**
	 * Creates an outgoing envelope.
	 * @param msg the message which is to send
	 * @param receiver the receiver of this message
	 * @return the resulting <tt>Envelope</tt> object
	 */
    public static Envelope createOutgoingEnv(IMessage msg, IPeer receiver) {
        return new Envelope(msg, null, receiver, null);
    }

    /**
	 * Create an incoming envelope.
	 * @param msg the message which has been received
	 * @param sender the sender of this message
	 * @param chan the channel which received the data
	 * @return the resulting <tt>Envelope</tt> object
	 */
    public static Envelope createIncomingEnv(IMessage msg, IPeer sender, Channel chan) {
        return new Envelope(msg, sender, null, chan);
    }

    /** The message. */
    private final IMessage message;

    /** The sender. Can be null if the sender is the local client. */
    private final IPeer sender;

    /** The receiver. Can be null if the receiver is the local client. */
    private final IPeer receiver;

    /** The channel. Can be null if the envelope was not received. */
    private final Channel channel;

    /**
	 * Creates a envelope.
	 * @param msg the message
	 * @param sender the sender
	 * @param receiver the receiver
	 * @param channel the channel
	 */
    private Envelope(IMessage msg, IPeer sender, IPeer receiver, Channel channel) {
        this.message = msg;
        this.sender = sender;
        this.receiver = receiver;
        this.channel = channel;
    }

    public IMessage getMessage() {
        return message;
    }

    public IPeer getSender() {
        return sender;
    }

    public IPeer getReceiver() {
        return receiver;
    }

    public Channel getChannel() {
        return channel;
    }

    public String toString() {
        return (sender == null ? "unknown" : sender) + " => " + (receiver == null ? "unknown" : receiver) + ": " + message.toString();
    }
}
