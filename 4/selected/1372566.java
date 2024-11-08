package gov.sns.tools.pvlogger;

import java.util.ArrayList;
import java.util.List;

public class ChannelBufferGroup extends ChannelGroup {

    /** array of channel wrappers */
    protected ChannelBufferWrapper[] _channelWrappers;

    public ChannelBufferGroup(String label, String description, String[] pvs, double loggingPeriod) {
        super(label, description, pvs, loggingPeriod);
        this.wrapPVs(pvs);
    }

    public ChannelBufferGroup(String groupLabel, String[] pvs, double loggingPeriod) {
        this(groupLabel, "", pvs, loggingPeriod);
    }

    /**
	 * Create ChannelWrappers for the specified pvs.
	 * @param pvs the list of PVs to wrap
	 */
    @Override
    protected void wrapPVs(final String[] pvs) {
        List<ChannelBufferWrapper> wrappers = new ArrayList<ChannelBufferWrapper>(pvs.length);
        for (int index = 0; index < pvs.length; index++) {
            ChannelBufferWrapper wrapper = new ChannelBufferWrapper(pvs[index]);
            wrapper.addConnectionListener(_connectionHandler);
            wrappers.add(wrapper);
        }
        _channelWrappers = wrappers.toArray(new ChannelBufferWrapper[wrappers.size()]);
    }

    /**
	 * Get the channel wrappers for the channels associated with this group.
	 * @return The array of channel wrappers of this group.
	 */
    @Override
    public ChannelWrapper[] getChannelWrappers() {
        return _channelWrappers;
    }

    /**
	 * Dispose of this channel group's resources
	 */
    @Override
    public void dispose() {
        for (int index = 0; index < _channelWrappers.length; index++) {
            _channelWrappers[index].removeConnectionListener(_connectionHandler);
        }
    }

    /**
	 * Request connections to the channel wrappers.
	 */
    @Override
    public void requestConnections() {
        for (int index = 0; index < _channelWrappers.length; index++) {
            _channelWrappers[index].requestConnection();
        }
    }
}
