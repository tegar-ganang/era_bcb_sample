package gov.sns.tools.pvlogger;

import gov.sns.ca.Channel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChannelGroupSP extends ChannelGroup {

    /** array of channel wrappers */
    protected ChannelWrapper[] _channelWrappersSP;

    public ChannelGroupSP(String label, String description, String[] pvs, String[] sppvs, double loggingPeriod) {
        super(label, description, pvs, loggingPeriod);
        wrapSPPVs(sppvs);
    }

    /**
	 * Constructor
	 * @param groupLabel The group's label
	 * @param pvs The PVs in the group
	 * @param loggingPeriod The default logging period for the group
	 */
    public ChannelGroupSP(String groupLabel, String[] pvs, String[] sppvs, double loggingPeriod) {
        this(groupLabel, "", pvs, sppvs, loggingPeriod);
    }

    /**
	 * Dispose of this channel group's resources
	 */
    @Override
    public void dispose() {
        super.dispose();
        for (int index = 0; index < _channelWrappersSP.length; index++) {
            _channelWrappersSP[index].removeConnectionListener(_connectionHandler);
        }
    }

    /**
	 * Request connections to the channel wrappers.
	 */
    @Override
    public void requestConnections() {
        super.requestConnections();
        for (int index = 0; index < _channelWrappersSP.length; index++) {
            _channelWrappersSP[index].requestConnection();
        }
    }

    /**
	 * Get the channel wrappers for the channels associated with this group.
	 * @return The array of channel wrappers of this group.
	 */
    public ChannelWrapper[] getChannelWrappersSP() {
        return _channelWrappersSP;
    }

    /**
	 * Get the collection of channels which we attempt to monitor
	 * @return a collection of channels corresponding to the channel wrappers
	 */
    public Collection<Channel> getChannelsSP() {
        Set<Channel> channels = new HashSet<Channel>(_channelWrappersSP.length);
        for (int index = 0; index < _channelWrappersSP.length; index++) {
            channels.add(_channelWrappersSP[index].getChannel());
        }
        return channels;
    }

    /**
	 * Get the number of channels in this group
	 * @return The number of channels in this group
	 */
    public int getChannelCountSP() {
        return _channelWrappersSP.length;
    }

    /**
	 * Create ChannelWrappers for the specified pvs.
	 * @param pvs the list of PVs to wrap
	 */
    protected void wrapSPPVs(final String[] sppvs) {
        List<ChannelWrapper> wrappers = new ArrayList<ChannelWrapper>(sppvs.length);
        for (int index = 0; index < sppvs.length; index++) {
            ChannelWrapper wrapper = new ChannelWrapper(sppvs[index]);
            wrapper.addConnectionListener(_connectionHandler);
            wrappers.add(wrapper);
        }
        _channelWrappersSP = wrappers.toArray(new ChannelWrapper[wrappers.size()]);
    }
}
