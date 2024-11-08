package consciouscode.bonsai.components;

import consciouscode.bonsai.actions.ActionException;
import consciouscode.bonsai.actions.ActionRegistry;
import consciouscode.bonsai.actions.AutoAction;
import consciouscode.bonsai.channels.Channel;
import consciouscode.bonsai.channels.ChannelProvider;
import consciouscode.bonsai.channels.ChannelSupport;
import consciouscode.logging.PrefixedLog;
import consciouscode.swing.SwingUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.LayoutManager;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import org.apache.commons.logging.Log;

/**
   A <code>JPanel</code> that implements basic logging and configuration
   features.
*/
public abstract class GenericPanel extends JPanel implements ChannelProvider, GenericContainer {

    /**
       Create a panel with a BorderLayout.
    */
    public GenericPanel() {
        super(new BorderLayout());
    }

    /**
        Creates an new buffered panel with the specified layout manager.
    */
    public GenericPanel(LayoutManager layout) {
        super(layout);
    }

    /**
       Create a panel with the given Log and layout manager.
    */
    public GenericPanel(Log log, LayoutManager layout) {
        super(layout);
        setLog(log);
    }

    /**
       Define the container holding this panel.  The panel's log is set to
       that of the container (if it's not null).
    */
    public void setContainer(GenericContainer container) {
        myContainer = container;
        Log log = container.getLog();
        if (log != null) {
            setLog(log);
        }
        if (myActionRegistry != null) {
            ActionRegistry inherited = container.getActionRegistry();
            inherited.addAll(myActionRegistry);
            myActionRegistry = null;
        }
    }

    public Component getWidget(String name) {
        return SwingUtils.getChildComponentByName(this, name);
    }

    /**
       Dummy method to allow PropertyUtils to recognize 'widget' property.
       Does nothing.
    */
    public void setWidget(String name, Component value) {
    }

    public void defineChannel(String channelName, Channel channel) {
        myChannels.defineChannel(channelName, channel);
    }

    public Channel getChannel(String channelName) {
        return myChannels.getChannel(channelName);
    }

    public Object getChannelValue(String channelName) {
        return myChannels.getChannelValue(channelName);
    }

    public void setChannelValue(String channelName, Object value) {
        myChannels.setChannelValue(channelName, value);
    }

    public ActionRegistry getActionRegistry() {
        if (myContainer != null) {
            return myContainer.getActionRegistry();
        }
        if (myActionRegistry == null) {
            myActionRegistry = new ActionRegistry();
        }
        return myActionRegistry;
    }

    public void setAutoActions(AutoAction[] actions) {
        if (actions != null) {
            for (int i = 0; i < actions.length; i++) {
                actions[i].setLog(getLog());
            }
            getActionRegistry().addActions(actions);
        }
    }

    public AutoAction getAction(String actionId) {
        return getActionRegistry().getAction(actionId);
    }

    /**
       Adds an zero-pixel border to this panel, thus removing all insets.
    */
    public void clearBorder() {
        setBorder(BorderFactory.createEmptyBorder());
    }

    /**
       Layout the contents of this panel if it hasn't been prepared yet.
       <p>
       In general, this method should not be called until this component is
       fully configured.  In some cases, important properties cannot be set
       until <em>after</em> the node has been installed into a Seedling.
       <p>
       The first time this method is called, it invokes {@link #doPrepareGui}.
       It is safe to call this method multiple times.

       @throws ActionException when something goes wrong during
       the layout process.
    */
    public synchronized void prepareGui() throws ActionException {
        if (!myGuiIsPrepared) {
            doPrepareGui();
            myGuiIsPrepared = true;
        }
    }

    public GenericWindow getContainingWindow() {
        return (myContainer == null ? null : myContainer.getContainingWindow());
    }

    public Log getLog() {
        return myLog;
    }

    public void setLog(Log log) {
        myLog.setTargetLog(log);
    }

    /** @deprecated */
    @Deprecated
    public boolean isLoggingDebug() {
        return myLog.isLoggingDebug();
    }

    /** @deprecated */
    @Deprecated
    public void setLoggingDebug(boolean value) {
        myLog.setLoggingDebug(value);
    }

    /** @deprecated */
    @Deprecated
    public boolean isLoggingError() {
        return myLog.isLoggingError();
    }

    /** @deprecated */
    @Deprecated
    public void setLoggingError(boolean value) {
        myLog.setLoggingError(value);
    }

    /**
       Implementations of this method should fully layout the GUI widgets of
       this panel.  Note that this method will only be invoked once, unless it
       throws an {@link ActionException}.

       If a subclass uses nested GUI nodes ({@link GenericPanel}s), their
       {@link #prepareGui} methods should be called here to ensure that they
       are fully prepared for display.

       @throws ActionException when something goes wrong during
       the layout process.  In such a case, the method may be invoked a second
       time, so it should cleanup any partially-constructed components.
    */
    protected abstract void doPrepareGui() throws ActionException;

    /**
       Returns the ChannelSupport used by this panel.  Subclasses will
       typically use the support to call {@link ChannelSupport#defineChannel}
       when preparing the panel.
    */
    protected ChannelSupport getChannelSupport() {
        return myChannels;
    }

    private GenericContainer myContainer;

    private PrefixedLog myLog = new PrefixedLog();

    private ActionRegistry myActionRegistry;

    private ChannelSupport myChannels = new ChannelSupport(this);

    private boolean myGuiIsPrepared = false;
}
