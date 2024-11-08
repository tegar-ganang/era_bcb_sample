package com.googlecode.acpj.internal.channels;

import com.googlecode.acpj.actors.Actor;
import com.googlecode.acpj.actors.ActorFactory;
import com.googlecode.acpj.channels.ChannelException;
import com.googlecode.acpj.channels.ChannelFactory;
import com.googlecode.acpj.channels.Port;

/**
 * <p>
 * Internal - implmementation of the {@link com.googlecode.acpj.channels.Port} interface.
 * </p>
 * 
 * @author Simon Johnston (simon@johnstonshome.org)
 * @since 0.1.0
 * 
 */
public class SimplePort<T> implements Port<T> {

    public static final int PORT_LIMIT_EXCEEDED = 0;

    protected SimpleChannel<T> channel = null;

    protected int limit = 0;

    protected Actor owner = null;

    protected boolean closed = false;

    public SimplePort(SimpleChannel<T> channel, int limit, Actor owner) {
        if (channel == null) {
            throw new IllegalArgumentException("Channel may not be null.");
        }
        this.channel = channel;
        if (limit == PORT_LIMIT_EXCEEDED) {
            throw new IllegalArgumentException("Port limit invalid for port.");
        }
        this.limit = limit;
        this.owner = owner;
    }

    public int getLimit() {
        if (isClosed()) {
            throw new IllegalStateException("Port is closed.");
        }
        return this.limit;
    }

    protected int decrementLimit() {
        if (this.limit == ChannelFactory.PORT_LIMIT_UNLIMITED) {
            return this.limit;
        } else {
            return this.limit--;
        }
    }

    protected SimpleChannel<T> getChannel() {
        return this.channel;
    }

    public Actor getOwningActor() {
        if (isClosed()) {
            throw new IllegalStateException("Port is closed.");
        }
        return this.owner;
    }

    protected boolean isMine() {
        if (isClosed()) {
            throw new IllegalStateException("Port is closed.");
        }
        if (this.owner == null) {
            return false;
        }
        return this.owner.equals(ActorFactory.getInstance().getCurrentActor());
    }

    public void release() throws ChannelException {
        if (isClosed()) {
            throw new IllegalStateException("Port is closed.");
        }
        if (!isMine()) {
            throw new ChannelException("Current actor does not own this port.");
        }
        this.owner = null;
    }

    public void claim() throws ChannelException {
        if (isClosed()) {
            throw new IllegalStateException("Port is closed.");
        }
        if (isMine()) {
            return;
        }
        if (this.owner != null) {
            throw new ChannelException("Port has not been released.");
        }
        this.owner = ActorFactory.getInstance().getCurrentActor();
    }

    public boolean isPoisoned() throws IllegalStateException {
        if (isClosed()) {
            throw new IllegalStateException("Port is closed.");
        }
        return getChannel().isPoisoned();
    }

    public void poison() throws IllegalStateException, ChannelException {
        if (isClosed()) {
            throw new IllegalStateException("Port is closed.");
        }
        getChannel().poison();
    }

    public void close() {
        this.closed = true;
        this.limit = 0;
        this.owner = null;
        this.channel.closePort(this);
        this.channel = null;
    }

    public boolean isClosed() {
        return this.closed;
    }
}
