package net.sourceforge.olduvai.lrac.drawer.strips;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * This represents a map of a strip channel prefix that maps to a potentially large 
 * number of channel names of the same general type.  
 * 
 * For example cpu_sys could map to cpu_sys.[0-9][0-9]
 * 
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 *
 */
public class StripChannelGroup {

    private static final String CHANNELTOKENSTRINGESCAPED = "\\.";

    private static final String CHANNELTOKENSTRING = ".";

    public static final String GROUPDESCRIPTION = " group";

    private String prefix;

    private List<StripChannel> channels = new ArrayList<StripChannel>();

    public StripChannelGroup(String prefix) {
        this.prefix = prefix;
    }

    public void addChannel(StripChannel channel) {
        channels.add(channel);
    }

    public String getPrefix() {
        return prefix;
    }

    public List<StripChannel> getChannelNames() {
        return channels;
    }

    /**
	 * Creates a map between channel prefixes and channel groups which contain the full channel names.
	 *   
	 * @param stripChannels
	 * @return
	 */
    static final HashMap<String, StripChannelGroup> createChannelGroupMap(List<StripChannel> stripChannels) {
        HashMap<String, StripChannelGroup> map = new HashMap<String, StripChannelGroup>();
        Iterator<StripChannel> it = stripChannels.iterator();
        while (it.hasNext()) {
            final StripChannel sc = it.next();
            final String channelId = sc.getChannelID();
            final String channelPrefix = getChannelPrefix(channelId);
            if (channelPrefix == null) continue;
            StripChannelGroup group = map.get(channelPrefix);
            if (group == null) {
                group = new StripChannelGroup(channelPrefix);
                map.put(channelPrefix, group);
            }
            group.addChannel(sc);
        }
        return map;
    }

    /**
	 * Extracts the prefix of a specified channelID.  
	 * 
	 * @param channelId
	 * @return Null if channel is 'independent' and has no prefix.  Otherwise returns a string of the channelId's prefix.
	 */
    public static String getChannelPrefix(String channelId) {
        if (!channelId.contains(CHANNELTOKENSTRING)) return null;
        return channelId.split(CHANNELTOKENSTRINGESCAPED)[0];
    }

    /**
	 * Extracts the suffix of a specified channelID.  
	 * 
	 * @param channelId
	 * @return Null if channel is 'independent' and has no suffix.  Otherwise returns a string of the channelId's suffix.
	 */
    public static String getChannelSuffix(String channelId) {
        if (!channelId.contains(CHANNELTOKENSTRING)) return null;
        return channelId.split(CHANNELTOKENSTRINGESCAPED)[1];
    }
}
