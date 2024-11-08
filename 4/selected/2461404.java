package org.jmetis.messaging.channel;

import java.net.URI;
import org.jmetis.messaging.core.IMessageChannel;

/**
 * {@code MessageChannel}
 * 
 * @author era
 */
public abstract class MessageChannel<T> implements IMessageChannel<T> {

    /**
	 * Constructs a new {@code MessageChannel} instance.
	 * 
	 */
    protected MessageChannel() {
        super();
    }

    public URI getChannelIdentifier() {
        return null;
    }
}
