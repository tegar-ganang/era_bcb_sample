package org.jgroups.mux;

import java.io.Serializable;
import java.util.Map;
import org.jgroups.Address;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelFactory;
import org.jgroups.ChannelNotConnectedException;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.JChannelFactory;
import org.jgroups.Message;
import org.jgroups.StateTransferException;
import org.jgroups.View;
import org.jgroups.annotations.Experimental;
import org.jgroups.conf.ClassConfigurator;
import org.jgroups.stack.ProtocolStack;

/**
 * Multiplexer channel is a lightweight version of a regular channel where
 * multiple MuxChannel(s) share the same underlying regular channel.
 * 
 * <p>
 * MuxChannel has to be created with a unique application id. The multiplexer
 * keeps track of all registered applications and tags messages belonging to a
 * specific application with that id for sent messages. When receiving a message
 * from a remote peer, the multiplexer will dispatch a message to the
 * appropriate MuxChannel depending on the id attached to the message.
 * 
 * <p>
 * MuxChannel is created using
 * {@link ChannelFactory#createMultiplexerChannel(String, String)}.
 * 
 * @author Bela Ban, Vladimir Blagojevic
 * @see ChannelFactory#createMultiplexerChannel(String, String)
 * @see JChannelFactory#createMultiplexerChannel(String, String)
 * @see Multiplexer
 * @since 2.4
 * @version $Id: MuxChannel.java,v 1.57 2010/03/05 08:59:12 belaban Exp $
 */
@Experimental(comment = "because of impedance mismatches between a MuxChannel and JChannel, this might get deprecated " + "in the future. The replacement would be a shared transport (see the documentation for details)")
@Deprecated
public class MuxChannel extends JChannel {

    private static final short ID = ClassConfigurator.getProtocolId(MuxChannel.class);

    private final String id;

    private final String stack_name;

    private final MuxHeader hdr;

    private final Multiplexer mux;

    MuxChannel(String id, String stack_name, Multiplexer mux) {
        super(false);
        if (id == null || id.length() == 0) throw new IllegalArgumentException("Cannot create MuxChannel with id " + id);
        if (stack_name == null || stack_name.length() == 0) throw new IllegalArgumentException("Cannot create MuxChannel with stack_name " + stack_name);
        if (mux == null) throw new IllegalArgumentException("Cannot create MuxChannel with Multiplexer " + mux);
        this.stack_name = stack_name;
        this.id = id;
        this.hdr = new MuxHeader(id);
        this.mux = mux;
        closed = !mux.isOpen();
    }

    public String getStackName() {
        return stack_name;
    }

    public String getId() {
        return id;
    }

    public Multiplexer getMultiplexer() {
        return mux;
    }

    public String getChannelName() {
        return mux.getChannel().getClusterName();
    }

    public String getClusterName() {
        return mux.getChannel().getClusterName();
    }

    public Address getAddress() {
        return mux.getLocalAddress();
    }

    public String getProperties() {
        return mux.getChannel().getProperties();
    }

    /** This should never be used (just for testing) ! */
    public JChannel getChannel() {
        return mux.getChannel();
    }

    /**
     * Returns the <em>service</em> view, ie. the cluster view (see
     * {@link #getView()}) <em>minus</em> the nodes on which this service is
     * not running, e.g. if S1 runs on A and C, and the cluster view is {A,B,C},
     * then the service view is {A,C}
     * 
     * @return The service view (list of nodes on which this service is running)
     */
    public View getView() {
        return closed || !connected ? null : mux.getServiceView(id);
    }

    /**
     * Returns the JGroups view of a cluster, e.g. if we have nodes A, B and C,
     * then the view will be {A,B,C}
     * 
     * @return The JGroups view
     */
    public View getClusterView() {
        return mux.getChannel().getView();
    }

    public ProtocolStack getProtocolStack() {
        return mux.getChannel().getProtocolStack();
    }

    public Map<String, Object> dumpStats() {
        Map<String, Object> retval = mux.getChannel().getProtocolStack().dumpStats();
        if (retval != null) {
            Map<String, Long> tmp = dumpChannelStats();
            if (tmp != null) retval.put("channel", tmp);
        }
        return retval;
    }

    protected void setClosed(boolean f) {
        closed = f;
    }

    protected void setConnected(boolean f) {
        connected = f;
    }

    public synchronized void connect(String channel_name) throws ChannelException, ChannelClosedException {
        checkClosed();
        if (isConnected()) {
            if (log.isTraceEnabled()) log.trace("already connected to " + channel_name);
            return;
        }
        mux.addServiceIfNotPresent(getId(), this);
        if (!mux.isConnected()) {
            mux.connect(getStackName());
        }
        try {
            if (mux.flushSupported()) {
                boolean successfulFlush = mux.startFlush(false);
                if (!successfulFlush && log.isWarnEnabled()) {
                    log.warn("Flush failed at " + mux.getLocalAddress() + ":" + getId());
                }
            }
            try {
                mux.sendServiceUpMessage(getId());
                setClosed(false);
                setConnected(true);
                notifyChannelConnected(this);
            } catch (Exception e) {
                if (log.isErrorEnabled()) log.error("failed sending SERVICE_UP message", e);
                throw new ChannelException("MuxChannel.connect() failed", e);
            }
        } finally {
            if (mux.flushSupported()) mux.stopFlush();
        }
    }

    public synchronized void connect(String cluster_name, Address target, String state_id, long timeout) throws ChannelException {
        checkClosed();
        if (isConnected()) {
            if (log.isTraceEnabled()) log.trace("already connected to " + cluster_name);
            return;
        }
        mux.addServiceIfNotPresent(getId(), this);
        if (!mux.isConnected()) {
            mux.connect(getStackName());
        }
        try {
            if (mux.flushSupported()) {
                boolean successfulFlush = mux.startFlush(false);
                if (!successfulFlush && log.isWarnEnabled()) {
                    log.warn("Flush failed at " + mux.getLocalAddress() + ":" + getId());
                }
            }
            try {
                mux.sendServiceUpMessage(getId());
                setClosed(false);
                setConnected(true);
                notifyChannelConnected(this);
            } catch (Exception e) {
                if (log.isErrorEnabled()) log.error("failed sending SERVICE_UP message", e);
                throw new ChannelException("MuxChannel.connect() failed", e);
            }
            View serviceView = mux.getServiceView(getId());
            boolean stateTransferOk = false;
            boolean fetchState = serviceView != null && serviceView.size() > 1;
            if (fetchState) {
                stateTransferOk = getState(target, state_id, timeout, false);
                if (!stateTransferOk) {
                    throw new StateTransferException("Could not retrieve state " + state_id + " from " + target);
                }
            }
        } finally {
            if (mux.flushSupported()) mux.stopFlush();
        }
    }

    public synchronized void disconnect() {
        if (!isConnected()) return;
        setClosed(false);
        setConnected(false);
        notifyServiceDown();
        mux.disconnect();
        notifyChannelDisconnected(this);
    }

    public synchronized void close() {
        if (closed) return;
        if (isConnected()) {
            setConnected(false);
            notifyServiceDown();
        }
        setClosed(true);
        closeMessageQueue(true);
        notifyChannelClosed(this);
    }

    protected void notifyServiceDown() {
        try {
            if (mux.flushSupported()) {
                boolean successfulFlush = mux.startFlush(false);
                if (!successfulFlush && log.isWarnEnabled()) {
                    log.warn("Flush failed at " + mux.getLocalAddress() + ":" + getId());
                }
            }
            try {
                mux.sendServiceDownMessage(getId());
            } catch (Exception e) {
                if (log.isErrorEnabled()) log.error("failed sending SERVICE_DOWN message", e);
            }
        } catch (Throwable t) {
            log.error("closing channel failed", t);
        } finally {
            if (mux.flushSupported()) mux.stopFlush();
        }
    }

    public synchronized void open() throws ChannelException {
        setClosed(false);
        setConnected(false);
        if (!mux.isOpen()) {
            mux.open();
        }
    }

    public synchronized void shutdown() {
        if (closed) return;
        setClosed(true);
        setConnected(false);
        try {
            if (mux.flushSupported()) {
                boolean successfulFlush = mux.startFlush(false);
                if (!successfulFlush && log.isWarnEnabled()) {
                    log.warn("Flush failed at " + mux.getLocalAddress() + ":" + getId());
                }
            }
            try {
                mux.sendServiceDownMessage(getId());
            } catch (Exception e) {
                if (log.isErrorEnabled()) log.error("failed sending SERVICE_DOWN message", e);
            }
        } catch (Throwable t) {
            log.error("shutdown channel failed", t);
        } finally {
            if (mux.flushSupported()) mux.stopFlush();
        }
        closeMessageQueue(true);
        notifyChannelClosed(this);
    }

    public void send(Message msg) throws ChannelNotConnectedException, ChannelClosedException {
        msg.putHeader(ID, hdr);
        mux.getChannel().send(msg);
        if (stats) {
            sent_msgs++;
            sent_bytes += msg.getLength();
        }
    }

    public void send(Address dst, Address src, Serializable obj) throws ChannelNotConnectedException, ChannelClosedException {
        send(new Message(dst, src, obj));
    }

    public void down(Event evt) {
        if (evt.getType() == Event.MSG) {
            Message msg = (Message) evt.getArg();
            msg.putHeader(ID, hdr);
        }
        mux.getChannel().down(evt);
    }

    public Object downcall(Event evt) {
        if (evt.getType() == Event.MSG) {
            Message msg = (Message) evt.getArg();
            msg.putHeader(ID, hdr);
        }
        return mux.getChannel().downcall(evt);
    }

    public boolean getState(Address target, String state_id, long timeout, boolean useFlushIfPresent) throws ChannelNotConnectedException, ChannelClosedException {
        String my_id = id;
        if (state_id != null) my_id += "::" + state_id;
        Address service_view_coordinator = mux.getStateProvider(target, id);
        Address tmp = getAddress();
        if (service_view_coordinator != null) target = service_view_coordinator;
        if (tmp != null && tmp.equals(target)) target = null;
        if (!mux.stateTransferListenersPresent()) return mux.getChannel().getState(target, my_id, timeout, useFlushIfPresent); else {
            View serviceView = mux.getServiceView(getId());
            boolean fetchState = serviceView != null && serviceView.size() > 1;
            return fetchState && mux.getState(target, my_id, timeout);
        }
    }

    public void returnState(byte[] state) {
        mux.getChannel().returnState(state, id);
    }

    public void returnState(byte[] state, String state_id) {
        String my_id = id;
        if (state_id != null) my_id += "::" + state_id;
        mux.getChannel().returnState(state, my_id);
    }
}
