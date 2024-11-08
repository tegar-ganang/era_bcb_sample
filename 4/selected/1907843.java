package gov.sns.apps.xyzcorrelator;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelTimeRecord;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.Monitor;
import gov.sns.ca.correlator.*;
import gov.sns.tools.correlator.*;
import gov.sns.tools.messaging.MessageCenter;
import gov.sns.tools.data.*;
import gov.sns.xal.smf.NodeChannelRef;
import gov.sns.xal.smf.Accelerator;
import java.util.*;

/** main model for managing correlations */
public class CorrelationModel implements DataListener {

    /** the data adaptor label used for reading and writing this model */
    public static final String DATA_LABEL = "CorrelationModel";

    /** message center for dispatching events */
    private final MessageCenter MESSAGE_CENTER;

    /** proxy to forward events to registered listeners */
    private final CorrelationModelListener EVENT_PROXY;

    /** channel wrappers for the channels to correlate */
    private final List<ChannelWrapper> CHANNEL_WRAPPERS;

    /** list of plot channel IDs */
    private final List<String> PLOT_CHANNEL_IDs;

    /** list of monitored channel IDs */
    private final List<String> MONITOR_CHANNEL_IDs;

    /** correlator */
    private final ChannelCorrelator CORRELATOR;

    /** accelerator */
    private Accelerator _accelerator;

    /** Constructor */
    public CorrelationModel() {
        MESSAGE_CENTER = new MessageCenter("Correlation Model");
        EVENT_PROXY = MESSAGE_CENTER.registerSource(this, CorrelationModelListener.class);
        PLOT_CHANNEL_IDs = new ArrayList<String>();
        MONITOR_CHANNEL_IDs = new ArrayList<String>();
        CORRELATOR = new ChannelCorrelator(0.001);
        CHANNEL_WRAPPERS = new ArrayList<ChannelWrapper>(3);
        CHANNEL_WRAPPERS.add(null);
        CHANNEL_WRAPPERS.add(null);
        CHANNEL_WRAPPERS.add(null);
        CORRELATOR.addListener(new NoticeMonitor());
        CORRELATOR.startMonitoring();
        _accelerator = null;
    }

    /** provides the name used to identify the class in an external data source. */
    public String dataLabel() {
        return DATA_LABEL;
    }

    /** Instructs the receiver to update its data based on the given adaptor. */
    public void update(final DataAdaptor adaptor) {
        final List<DataAdaptor> channelAdaptors = adaptor.childAdaptors(ChannelWrapper.DATA_LABEL);
        while (CHANNEL_WRAPPERS.size() < channelAdaptors.size()) {
            addChannelPlaceholder();
        }
        int iChannel = 0;
        for (final DataAdaptor channelAdaptor : channelAdaptors) {
            final ChannelWrapper wrapper = ChannelWrapper.getInstance(channelAdaptor, _accelerator);
            CHANNEL_WRAPPERS.set(iChannel, wrapper);
            setChannelEnable(iChannel, wrapper.isEnabled());
            setChannelPlotting(iChannel, wrapper.isPlotting());
            ++iChannel;
        }
        EVENT_PROXY.monitoredChannelsChanged(getMonitoredChannels());
        EVENT_PROXY.monitoredChannelsChanged(getPlottingChannels());
    }

    /** Instructs the receiver to write its data to the adaptor for external storage. */
    public void write(final DataAdaptor adaptor) {
        for (final ChannelWrapper wrapper : CHANNEL_WRAPPERS) {
            if (wrapper != null) {
                adaptor.writeNode(wrapper);
            }
        }
    }

    /** set the accelerator to the specified value */
    public void setAccelerator(final Accelerator accelerator) {
        _accelerator = accelerator;
    }

    /** register to listen for correlation model events from this object */
    public void addCorrelationModelListener(final CorrelationModelListener listener) {
        MESSAGE_CENTER.registerTarget(listener, this, CorrelationModelListener.class);
    }

    /** remove the listener from receiving correlation model events from this object */
    public void removeCorrelationModelListener(final CorrelationModelListener listener) {
        MESSAGE_CENTER.removeTarget(listener, this, CorrelationModelListener.class);
    }

    /** start the correlator */
    public void startCorrelator() {
        CORRELATOR.startMonitoring();
    }

    /** stop the correlator */
    public void stopCorrelator() {
        CORRELATOR.stopMonitoring();
    }

    /** determine whether the correlator is running */
    public boolean isRunning() {
        return CORRELATOR.isRunning();
    }

    /** Get the correlation time resolution in seconds */
    public double getCorrelationResolution() {
        return CORRELATOR.binTimespan();
    }

    /** set the correlation time resolution in seconds */
    public void setCorrelationResolution(final double resolution) {
        CORRELATOR.setBinTimespan(resolution);
    }

    /** set the channel for the specified index */
    public void setChannel(final int index, final Channel channel) {
        setChannelWrapper(index, new ChannelWrapper(channel));
        setChannelPlotting(index, getPlottingChannelCount() < 3);
    }

    /** add a channel ref to monitor */
    public void setChannelRef(final int index, final NodeChannelRef channelRef) {
        setChannelWrapper(index, new ChannelWrapper(channelRef));
        setChannelPlotting(index, getPlottingChannelCount() < 3);
    }

    /** add a channel wrapper */
    private void setChannelWrapper(final int index, final ChannelWrapper wrapper) {
        setChannelEnable(index, false);
        CHANNEL_WRAPPERS.set(index, wrapper);
        CORRELATOR.addChannel(wrapper.getChannel());
        setChannelEnable(index, true);
    }

    /** set the channel by PV for the specified index */
    public void setChannelPV(final int index, final String channelPV) {
        if (channelPV != null && channelPV.length() > 0) {
            setChannel(index, ChannelFactory.defaultFactory().getChannel(channelPV));
        }
    }

    /** get the number of channel placeholders */
    public int getChannelPlaceholderCount() {
        return CHANNEL_WRAPPERS.size();
    }

    /** add a channel placeholder and return the index of the new placeholder */
    public int addChannelPlaceholder() {
        CHANNEL_WRAPPERS.add(null);
        return CHANNEL_WRAPPERS.size() - 1;
    }

    /** insert a channel placeholder at the specified index */
    public void insertChannelPlaceholder(final int index) {
        CHANNEL_WRAPPERS.add(index, null);
    }

    /** Add the channel references at available slots at the end of the list and making new slots as needed. */
    public void addChannelRefs(final List<NodeChannelRef> channelRefs) {
        int slot = CHANNEL_WRAPPERS.size();
        while (slot > 0 && CHANNEL_WRAPPERS.get(slot - 1) == null) {
            --slot;
        }
        addChannelRefs(slot, channelRefs);
    }

    /** Add the channel references at available slots starting at the specified start index and adding additional placeholders as needed. */
    public void addChannelRefs(final int startIndex, final List<NodeChannelRef> channelRefs) {
        final int wrapperCount = CHANNEL_WRAPPERS.size();
        final int numRefs = channelRefs.size();
        int refPointer = 0;
        int slot = Math.min(startIndex, wrapperCount);
        for (; slot < wrapperCount; slot++) {
            final ChannelWrapper wrapper = CHANNEL_WRAPPERS.get(slot);
            if (wrapper == null && refPointer < numRefs) {
                final NodeChannelRef channelRef = channelRefs.get(refPointer++);
                setChannelRef(slot, channelRef);
            } else {
                break;
            }
        }
        for (; refPointer < numRefs; refPointer++) {
            final NodeChannelRef channelRef = channelRefs.get(refPointer);
            insertChannelPlaceholder(slot);
            setChannelRef(slot, channelRef);
            ++slot;
        }
    }

    /** delete the placeholder at the specified index */
    public void deleteChannelPlaceholder(final int index) {
        final int wrapperCount = CHANNEL_WRAPPERS.size();
        if (wrapperCount > 3 && index < wrapperCount) {
            CHANNEL_WRAPPERS.remove(index);
        }
    }

    /** get the list of monitored channels */
    public List<Channel> getMonitoredChannels() {
        final List<Channel> channels = new ArrayList<Channel>(3);
        for (final ChannelWrapper wrapper : CHANNEL_WRAPPERS) {
            if (wrapper != null && wrapper.isEnabled()) {
                channels.add(wrapper.getChannel());
            }
        }
        return channels;
    }

    /** get the count of plotting channels */
    public int getPlottingChannelCount() {
        int count = 0;
        for (final ChannelWrapper wrapper : CHANNEL_WRAPPERS) {
            if (wrapper != null && wrapper.isPlotting()) {
                ++count;
            }
        }
        return count;
    }

    /** get the list of channels to be plotted */
    public List<Channel> getPlottingChannels() {
        final List<Channel> channels = new ArrayList<Channel>(3);
        for (final ChannelWrapper wrapper : CHANNEL_WRAPPERS) {
            if (wrapper != null && wrapper.isPlotting()) {
                channels.add(wrapper.getChannel());
            }
        }
        return channels;
    }

    /** get the channel wrapper for the specified index */
    private ChannelWrapper getChannelWrapper(final int index) {
        return CHANNEL_WRAPPERS.get(index);
    }

    /** get the channel associated with the specified index */
    public Channel getChannel(final int index) {
        final ChannelWrapper wrapper = getChannelWrapper(index);
        return wrapper != null ? wrapper.getChannel() : null;
    }

    /** get the name of the channel associated with the specified index */
    public String getChannelName(final int index) {
        final Channel channel = getChannel(index);
        return channel != null ? channel.channelName() : null;
    }

    /** get the ID of the channel associated with the specified index */
    public String getChannelID(final int index) {
        final Channel channel = getChannel(index);
        return channel != null ? channel.getId() : null;
    }

    /** get the channel source ID which is the channel name for a simple channel or a channel reference */
    public String getChannelSourceID(final int index) {
        final ChannelWrapper wrapper = CHANNEL_WRAPPERS.get(index);
        return wrapper != null ? wrapper.getChannelSourceID() : null;
    }

    /** set whether to enable the channel at the specified index */
    public void setChannelEnable(final int index, final boolean enable) {
        if (!enable && isChannelPlotting(index)) {
            setChannelPlotting(index, false);
        }
        final ChannelWrapper wrapper = getChannelWrapper(index);
        if (wrapper != null) {
            wrapper.setEnable(enable);
            final Channel channel = wrapper.getChannel();
            if (enable) {
                CORRELATOR.addChannel(channel);
            } else {
                if (CORRELATOR.hasSource(channel.getId())) {
                    CORRELATOR.removeChannel(channel);
                }
            }
        }
        final List<Channel> monitoredChannels = getMonitoredChannels();
        synchronized (MONITOR_CHANNEL_IDs) {
            MONITOR_CHANNEL_IDs.clear();
            for (final Channel channel : monitoredChannels) {
                MONITOR_CHANNEL_IDs.add(channel.getId());
            }
        }
        EVENT_PROXY.monitoredChannelsChanged(monitoredChannels);
    }

    /** determine whether the channel at the specified index is enabled */
    public boolean isChannelEnabled(final int index) {
        final ChannelWrapper wrapper = getChannelWrapper(index);
        return wrapper != null ? wrapper.isEnabled() : false;
    }

    /** set whether to plot the values of the channel at the specified index */
    public void setChannelPlotting(final int index, final boolean plot) {
        if (plot && !isChannelEnabled(index)) {
            setChannelEnable(index, true);
        }
        final ChannelWrapper wrapper = getChannelWrapper(index);
        if (wrapper != null) {
            wrapper.setPlotting(plot);
        }
        final List<Channel> plotChannels = getPlottingChannels();
        synchronized (PLOT_CHANNEL_IDs) {
            PLOT_CHANNEL_IDs.clear();
            for (final Channel channel : plotChannels) {
                PLOT_CHANNEL_IDs.add(channel.getId());
            }
        }
        EVENT_PROXY.plottingChannelsChanged(plotChannels);
    }

    /** determine whether the values of the channel at index is plotting */
    public boolean isChannelPlotting(final int index) {
        final ChannelWrapper wrapper = getChannelWrapper(index);
        return wrapper != null ? wrapper.isPlotting() : false;
    }

    /** correlation notice handling */
    class NoticeMonitor implements CorrelationNotice {

        /**
		 * Handle the correlation event.  This method gets called when a correlation was posted.
		 * @param sender The poster of the correlation event.
		 * @param correlation The correlation that was posted.
		 */
        public void newCorrelation(final Object sender, final Correlation correlation) {
            final List<ChannelTimeRecord> monitoredRecords = new ArrayList<ChannelTimeRecord>();
            final List<ChannelTimeRecord> plotRecords = new ArrayList<ChannelTimeRecord>();
            synchronized (CorrelationModel.this) {
                synchronized (MONITOR_CHANNEL_IDs) {
                    for (final String channelID : MONITOR_CHANNEL_IDs) {
                        final ChannelTimeRecord record = (ChannelTimeRecord) correlation.getRecord(channelID);
                        if (record != null) {
                            monitoredRecords.add(record);
                        }
                    }
                }
                synchronized (PLOT_CHANNEL_IDs) {
                    for (final String channelID : PLOT_CHANNEL_IDs) {
                        final ChannelTimeRecord record = (ChannelTimeRecord) correlation.getRecord(channelID);
                        if (record != null) {
                            plotRecords.add(record);
                        }
                    }
                }
            }
            EVENT_PROXY.correlationCaptured(correlation, plotRecords);
        }

        /**
		 * Handle the no correlation event.  This method gets called when no correlation was found within some prescribed time period.
		 * @param sender The poster of the "no correlation" event.
		 */
        public void noCorrelationCaught(final Object sender) {
        }
    }
}

/** wrap a channel to provide additional attributes */
class ChannelWrapper implements DataListener {

    /** the data adaptor label used for reading and writing this channel */
    public static final String DATA_LABEL = "Channel";

    /** channel source to wrap */
    private final ChannelSource CHANNEL_SOURCE;

    /** indicates whether the channel is enabled */
    private boolean _enabled;

    /** indicates whether the channel values are being plotted */
    private boolean _plotting;

    /** PrimaryConstructor */
    public ChannelWrapper(final ChannelSource channelSource) {
        CHANNEL_SOURCE = channelSource;
        _enabled = true;
    }

    /** Constructor */
    public ChannelWrapper(final Channel channel) {
        this(new SimpleChannelSource(channel));
    }

    /** Constructor */
    public ChannelWrapper(final NodeChannelRef channelRef) {
        this(new NodeChannelSource(channelRef));
    }

    /** Get an instance from a data adaptor */
    public static ChannelWrapper getInstance(final DataAdaptor adaptor, final Accelerator accelerator) {
        final ChannelSource channelSource = getChannelSource(adaptor, accelerator);
        if (channelSource != null) {
            final ChannelWrapper wrapper = new ChannelWrapper(channelSource);
            if (adaptor.hasAttribute("plotting")) {
                final boolean plotting = adaptor.booleanValue("plotting");
                wrapper.setPlotting(plotting);
            }
            if (adaptor.hasAttribute("enabled")) {
                final boolean enabled = adaptor.booleanValue("enabled");
                wrapper.setEnable(enabled);
                if (enabled && !adaptor.hasAttribute("plotting")) {
                    wrapper.setPlotting(true);
                }
            }
            return wrapper;
        } else {
            return null;
        }
    }

    /** Get the source for the specified adaptor */
    private static ChannelSource getChannelSource(final DataAdaptor adaptor, final Accelerator accelerator) {
        if (adaptor.hasAttribute("pv")) {
            final String pv = adaptor.stringValue("pv");
            if (pv != null) {
                return new SimpleChannelSource(pv);
            } else {
                return null;
            }
        } else if (adaptor.hasAttribute("channelRef")) {
            final String channelRefID = adaptor.stringValue("channelRef");
            return new NodeChannelSource(accelerator, channelRefID);
        } else {
            return null;
        }
    }

    /** provides the name used to identify the class in an external data source. */
    public String dataLabel() {
        return DATA_LABEL;
    }

    /** Instructs the receiver to update its data based on the given adaptor. */
    public void update(final DataAdaptor adaptor) {
    }

    /** Instructs the receiver to write its data to the adaptor for external storage. */
    public void write(final DataAdaptor adaptor) {
        adaptor.setValue("enabled", _enabled);
        adaptor.setValue("plotting", _plotting);
        CHANNEL_SOURCE.write(adaptor);
    }

    /** determine whether the channel is enabled */
    public boolean isEnabled() {
        return _enabled;
    }

    /** set whether to enable the channel */
    public void setEnable(final boolean enable) {
        _enabled = enable;
    }

    /** determine whether the channel values are being plotted */
    public boolean isPlotting() {
        return _plotting;
    }

    /** set whether to plot the channel values */
    public void setPlotting(final boolean plot) {
        _plotting = plot;
    }

    /** get a signature identifying the channel source */
    public String getChannelSourceID() {
        return CHANNEL_SOURCE.getChannelSourceID();
    }

    /** get the wrapped channel */
    public Channel getChannel() {
        return CHANNEL_SOURCE.getChannel();
    }
}

/** interface for channel adaptors */
abstract class ChannelSource {

    /** get the wrapped channel */
    public abstract Channel getChannel();

    /** get a signature identifying the channel source */
    public abstract String getChannelSourceID();

    /** write the channel source information to the adaptor */
    public abstract void write(final DataAdaptor adaptor);
}

/** Adaptor for channel wrapper of a simple channel */
class SimpleChannelSource extends ChannelSource {

    /** channel */
    private final Channel CHANNEL;

    /** Primary Constructor */
    public SimpleChannelSource(final Channel channel) {
        CHANNEL = channel;
    }

    /** Constructor */
    public SimpleChannelSource(final String pv) {
        this(ChannelFactory.defaultFactory().getChannel(pv));
    }

    /** Instructs the receiver to write its data to the adaptor for external storage. */
    public void write(final DataAdaptor adaptor) {
        if (CHANNEL != null) {
            adaptor.setValue("pv", CHANNEL.channelName());
        }
    }

    /** get the channel */
    public Channel getChannel() {
        return CHANNEL;
    }

    /** get a signature identifying the channel source */
    public String getChannelSourceID() {
        return CHANNEL.channelName();
    }
}

/** Adaptor for channel wrapper of a node channel reference */
class NodeChannelSource extends ChannelSource {

    /** channel reference */
    private final NodeChannelRef CHANNEL_REF;

    /** Primary Constructor */
    public NodeChannelSource(final NodeChannelRef channelRef) {
        CHANNEL_REF = channelRef;
    }

    /** Constructor */
    public NodeChannelSource(final Accelerator accelerator, final String channelRefID) {
        this(NodeChannelRef.getInstance(accelerator, channelRefID));
    }

    /** Instructs the receiver to write its data to the adaptor for external storage. */
    public void write(final DataAdaptor adaptor) {
        if (CHANNEL_REF != null) {
            adaptor.setValue("channelRef", CHANNEL_REF.toString());
        }
    }

    /** get the channel */
    public Channel getChannel() {
        return CHANNEL_REF.getChannel();
    }

    /** get a signature identifying the channel source */
    public String getChannelSourceID() {
        return CHANNEL_REF.toString();
    }
}
