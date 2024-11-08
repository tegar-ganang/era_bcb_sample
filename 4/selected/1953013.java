package gov.sns.jca;

import gov.aps.jca.Channel;
import java.util.HashMap;
import java.util.Map;

/** Cache JCA native channels for reuse among several XAL channels.  JCA won't allow us to create more than one channel for the same PV signal. */
class JcaNativeChannelCache {

    /** JCA System */
    private final JcaSystem _jcaSystem;

    /** map of native channel's keyed by PV signal name */
    private final Map<String, Channel> _channelMap;

    /** Constructor */
    JcaNativeChannelCache(final JcaSystem jcaSystem) {
        _jcaSystem = jcaSystem;
        _channelMap = new HashMap<String, Channel>();
    }

    /**
     * Get an existing channel if available or else, create a new channel for specified signal name.
     * @param signalName the PV signal name.
     * @return the native JCA channel corresponding to the specified PV signal
     * @throws gov.aps.jca.CAException if the channel fails to be created
     */
    Channel getChannel(final String signalName) throws gov.aps.jca.CAException {
        Channel channel;
        synchronized (_channelMap) {
            channel = _channelMap.get(signalName);
            if (channel == null) {
                channel = _jcaSystem.getJcaContext().createChannel(signalName);
                _channelMap.put(signalName, channel);
            }
        }
        return channel;
    }
}
