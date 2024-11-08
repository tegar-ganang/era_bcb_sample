package org.openmim.irc.channel_list;

import java.util.Hashtable;

public class IndexedChannelList extends OrderedChannelList {

    private Hashtable channelNameLow2channelItem;

    public IndexedChannelList() {
        channelNameLow2channelItem = new Hashtable();
    }

    /**
   * @associates <{ChannelListItem}>
   * @supplierCardinality 0..*
   * @supplierRole list items
   */
    public int add(ChannelListItem channellistitem) {
        int i = super.add(channellistitem);
        channelNameLow2channelItem.put(channellistitem.getChannelNameLowercased(), channellistitem);
        return i;
    }

    public ChannelListItem getItemByLowercasedName(String s) {
        return (ChannelListItem) channelNameLow2channelItem.get(s);
    }

    public ChannelListItem getItemByName(String s) {
        return getItemByLowercasedName(s.toLowerCase());
    }
}
