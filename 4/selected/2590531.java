package gov.sns.apps.scope;

import gov.sns.ca.*;
import gov.sns.tools.correlator.RecordFilter;
import gov.sns.tools.data.*;
import gov.sns.tools.messaging.MessageCenter;

/**
 * The Trigger manages the scope trigger which is based on a channel chosen to 
 * act as a trigger.  When enabled, the trigger's channel is correlated with the other channels 
 * of the scope and is required in the correlation.  When the trigger has a trigger filter applied to
 * it then its correlation is accepted or vetoed based on the trigger filter's associated record filter.
 *
 * @author  tap
 */
public class Trigger implements DataListener, ConnectionListener {

    static final String dataLabel = "Trigger";

    protected Channel channel;

    protected boolean isEnabled;

    protected TriggerFilter triggerFilter;

    protected volatile boolean isSettingChannel;

    protected MessageCenter messageCenter;

    protected TriggerListener triggerChangeProxy;

    protected SettingListener settingProxy;

    /** Creates a new instance of Trigger */
    public Trigger() {
        messageCenter = new MessageCenter("Trigger Model");
        triggerChangeProxy = messageCenter.registerSource(this, TriggerListener.class);
        settingProxy = messageCenter.registerSource(this, SettingListener.class);
        triggerFilter = null;
        setChannel(null);
    }

    /** 
     *  dataLabel() provides the name used to identify the class in an 
     *  external data source.
     */
    public String dataLabel() {
        return dataLabel;
    }

    /**
     *  Instructs the receiver to update its data based on the given adaptor.
     */
    public void update(DataAdaptor adaptor) {
        if (adaptor.hasAttribute("channel")) {
            setChannel(adaptor.stringValue("channel"));
        }
        DataAdaptor filterAdaptor = adaptor.childAdaptor(TriggerFilter.dataLabel);
        if (filterAdaptor != null) {
            triggerFilter = TriggerFilterFactory.decodeFilter(filterAdaptor);
        }
        setEnabled(adaptor.booleanValue("enabled"));
    }

    /**
     *  Instructs the receiver to write its data to the adaptor for external
     *  storage.
     */
    public void write(DataAdaptor adaptor) {
        if (channel != null) {
            adaptor.setValue("channel", channel.channelName());
        }
        adaptor.setValue("enabled", isEnabled);
        if (triggerFilter != null) {
            adaptor.writeNode(triggerFilter);
        }
    }

    /**
     * Add a listener for TriggerListener events from this trigger.
     * @param listener Listener to register for TriggerListener events.
     */
    public void addTriggerListener(TriggerListener listener) {
        messageCenter.registerTarget(listener, this, TriggerListener.class);
    }

    /**
     * Remove the listener for TriggerListener events from this trigger.
     * @param listener Listener to remove for TriggerListener events.
     */
    public void removeTriggerListener(TriggerListener listener) {
        messageCenter.removeTarget(listener, this, TriggerListener.class);
    }

    /**
     * Add the listener to be notified when a setting has changed.
     * @param listener Object to receive setting change events.
     */
    void addSettingListener(SettingListener listener) {
        messageCenter.registerTarget(listener, this, SettingListener.class);
    }

    /**
     * Remove the listener as a receiver of setting change events.
     * @param listener Object to remove from receiving setting change events.
     */
    void removeSettingListener(SettingListener listener) {
        messageCenter.removeTarget(listener, this, SettingListener.class);
    }

    /**
     * Get whether the model is in a state of attempting to set a new channel.
     * @return true if the channel is being set and false otherwise.
     */
    public boolean isSettingChannel() {
        return isSettingChannel;
    }

    /**
     * Set the channel based on the specified channel name.  Attempts to 
     * connect the channel and enable the trigger.
     * @param channelName The name of the channel to act as the trigger.
     * @throws gov.sns.apps.scope.ChannelSetException if the channel connection fails
     */
    public void setChannel(String channelName) throws ChannelSetException {
        isSettingChannel = true;
        try {
            if (channel != null) {
                channel.removeConnectionListener(this);
            }
            if (channelName == null) {
                setEnabled(false);
                channel = null;
                return;
            }
            channel = ChannelFactory.defaultFactory().getChannel(channelName);
            setEnabled(false);
            triggerChangeProxy.channelStateChanged(this);
            channel.addConnectionListener(this);
            setEnabled(true);
            settingProxy.settingChanged(this);
        } finally {
            isSettingChannel = false;
            triggerChangeProxy.channelStateChanged(this);
        }
    }

    /**
     * Get the channel which is the scope trigger.
     * @return The channel which is the scope trigger.
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Get the name of the channel used to trigger the scope.
     * @return The name of the channel used to trigger the scope.
     */
    public String getChannelName() {
        return (channel != null) ? channel.channelName() : "";
    }

    /**
	 * Determine whether the channel is connected
	 * @return true if the channel is connected and false if not
	 */
    public boolean isConnected() {
        return channel != null && channel.isConnected();
    }

    /**
     * State of whether the trigger can be enabled.  Tests if a channel has 
     * been selected and if that channel is connected.
     * @return True if the scope can be triggered; false otherwise.
     */
    public boolean canEnable() {
        return channel != null;
    }

    /**
     * Gets the enable state.
     * @return true if the scope can be triggered.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Toggles the enable state of the trigger.
     */
    public void toggleEnable() {
        setEnabled(!isEnabled);
    }

    /**
     * Attempts to sets the enable state of the trigger to that specified.
     * @param state The desired enable state of the trigger.
     */
    public void setEnabled(boolean state) {
        if (canEnable() || !state && isEnabled != state) {
            isEnabled = state;
            if (isEnabled) {
                triggerChangeProxy.triggerEnabled(this);
            } else {
                triggerChangeProxy.triggerDisabled(this);
            }
        }
        settingProxy.settingChanged(this);
    }

    /**
     * Sets the filter to be applied to the trigger.
     */
    public void setTriggerFilter(TriggerFilter filter) {
        boolean enableState = isEnabled;
        setEnabled(false);
        triggerFilter = filter;
        setEnabled(enableState);
    }

    /**
     * Forces the trigger filter to be updated based on parameter changes.
     */
    public void refresh() {
        if (triggerFilter != null) {
            triggerFilter.updateFilter();
        }
        setTriggerFilter(triggerFilter);
    }

    /**
     * Get the filter that is used to either accept or veto the trigger.
     * @return The filter applied to the trigger.
     */
    public TriggerFilter getTriggerFilter() {
        return triggerFilter;
    }

    /**
     * Get the filter parameters used in the trigger's filter.
     * @return the trigger filter's parameter or null if there is no trigger filter
     */
    public gov.sns.tools.Parameter[] getFilterParameters() {
        return (triggerFilter != null) ? triggerFilter.getParameters() : new gov.sns.tools.Parameter[0];
    }

    /**
     * Get the label of the trigger filter.
     * @return the trigger filter's label or null if there is no trigger filter
     */
    public String getFilterLabel() {
        return (triggerFilter != null) ? triggerFilter.getLabel() : "None";
    }

    /**
     * Get the record filter that is used to determine if the trigger should
     * be provided.  If there is no filter the trigger is provided whenever 
     * the trigger channel's monitor fires.  The filter provides a stronger 
     * requirement.  Not only must the trigger channel monitor fire, but its 
     * value must also pass the record filter test.
     * @return trigger's record filter or null if there is no filter.
     */
    public RecordFilter getRecordFilter() {
        return (triggerFilter != null) ? triggerFilter.getRecordFilter() : null;
    }

    /**
     * Indicates that a connection to the specified channel has been established.
     * @param channel The channel which has been connected.
     */
    public void connectionMade(Channel channel) {
        triggerChangeProxy.channelStateChanged(this);
    }

    /**
     * Indicates that a connection to the specified channel has been dropped.
     * @param channel The channel which has been disconnected.
     */
    public void connectionDropped(Channel channel) {
        triggerChangeProxy.channelStateChanged(this);
    }
}
