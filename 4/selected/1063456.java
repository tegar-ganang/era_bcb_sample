package org.openmim.irc.channel_list;

import squirrel_util.Lang;
import squirrel_util.Order;
import squirrel_util.Ordered;

public class OrderedChannelList extends ChannelList implements Ordered {

    protected Order order;

    public OrderedChannelList() {
        order = new Order(this);
    }

    /**
   * @associates <{ChannelListItem}>
   * @supplierCardinality 0..*
   * @supplierRole list items
   */
    public synchronized int add(ChannelListItem channellistitem) {
        Lang.ASSERT_NOT_NULL(channellistitem, "channelListItem");
        return order.add(channellistitem);
    }

    public int compareTo(Object obj, Object obj1) {
        ChannelListItem channellistitem = (ChannelListItem) obj;
        ChannelListItem channellistitem1 = (ChannelListItem) obj1;
        return channellistitem.getChannelNameLowercased().compareTo(channellistitem1.getChannelNameLowercased());
    }

    public synchronized Object getItem(int i) throws IndexOutOfBoundsException {
        return items.elementAt(i);
    }

    public synchronized int getItemCount() {
        return size();
    }

    public synchronized void insertAt(int i, Object obj) throws IndexOutOfBoundsException {
        items.insertElementAt(obj, i);
    }
}
