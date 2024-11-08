package consciouscode.bonsai.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import org.apache.commons.logging.Log;
import consciouscode.bonsai.channels.BasicChannel;
import consciouscode.bonsai.channels.Channel;
import consciouscode.bonsai.channels.ChannelProvider;
import consciouscode.bonsai.channels.ChannelSupport;
import consciouscode.logging.PrefixedLog;
import consciouscode.seedling.LocatableNode;
import consciouscode.seedling.NodeLocation;

/**
    An {@link javax.swing.Action} that uses Bonsai {@link Channel}s.
    <p>
    Typically, a <code>ChannelAction</code> will define several input or output
    channels. As input channels change value, the action can dynamically enable
    or disable itself.  When invoked, the action reads any input channels,
    processes them as necessary, and then sets any output channels as
    appropriate.
*/
public class ChannelAction extends AbstractAction implements ChannelProvider, LocatableNode {

    public ChannelAction() {
    }

    public ChannelAction(String name) {
        super(name);
    }

    public ChannelAction(String name, Icon icon) {
        super(name, icon);
    }

    /**
       This implementation simply calls {@link #actionPerformed()} and handles
       any exceptions; unless a subclass needs to use the <code>event</code>,
       it should override that version instead of this one.
    */
    public void actionPerformed(ActionEvent event) {
        try {
            actionPerformed();
        } catch (ActionException e) {
            if (getLog().isErrorEnabled()) {
                getLog().error(e);
            }
        }
    }

    /**
       Handle an action invocation.  Subclasses will generally override this
       to perform the necessary actions.
       <p>
       This implementation always throws
       {@link UnsupportedOperationException}.
    */
    public void actionPerformed() throws ActionException {
        throw new UnsupportedOperationException("ChannelAction.actionPerformed() not overriden, " + "use actionPerformed(ActionEvent) instead");
    }

    public Channel getChannel(String name) {
        return myChannels.getChannel(name);
    }

    public Object getChannelValue(String channelName) {
        return myChannels.getChannelValue(channelName);
    }

    public void setChannelValue(String channelName, Object value) {
        myChannels.setChannelValue(channelName, value);
    }

    public void nodeInstalled(NodeLocation location) {
        myNodeLocation = location;
    }

    public final NodeLocation getNodeLocation() {
        return myNodeLocation;
    }

    public Log getLog() {
        return myLog;
    }

    public void setLog(Log log) {
        myLog.setTargetLog(log);
    }

    public void setLoggingDebug(boolean value) {
        myLog.setLoggingDebug(value);
    }

    public void setLoggingError(boolean value) {
        myLog.setLoggingError(value);
    }

    /**
       Associate a new {@link BasicChannel} with a given name.
       Note that the name is only associated with the channel within this
       provider.

       @param channelName must be unique within this provider.  It must not be
       null or empty.
       @return the newly-created channel.
    */
    protected BasicChannel defineChannel(String channelName) {
        return myChannels.defineChannel(channelName);
    }

    /**
       Associate a given channel with a given name.
       Note that the name is only associated with the channel within this
       provider.
       <p>
       If this object's update observer has a method of the form
       <code>public void update<i>Channel</i>()</code> (where
       <code><i>Channel</i></code> is the capitalized channel name), a
       {@link consciouscode.bonsai.channels.ChannelListener} will be attached to
       invoke it.

       @param channelName must be unique within this provider.  It must not be
       null or empty.
       @param channel is the channel to associate with the name. It must not be
       null.
    */
    protected void defineChannel(String channelName, Channel channel) {
        myChannels.defineChannel(channelName, channel);
    }

    private NodeLocation myNodeLocation;

    private PrefixedLog myLog = new PrefixedLog();

    private ChannelSupport myChannels = new ChannelSupport(this);
}
