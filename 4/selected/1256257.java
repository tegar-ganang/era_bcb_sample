package org.xmatthew.spy2servers.jmx;

import java.util.List;
import java.util.Map;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.xmatthew.spy2servers.core.Component;
import org.xmatthew.spy2servers.core.Message;
import org.xmatthew.spy2servers.core.MessageAlertChannel;
import org.xmatthew.spy2servers.core.MessageAlertChannelActiveAwareComponent;
import org.xmatthew.spy2servers.util.CollectionUtils;
import org.xmatthew.spy2servers.util.ComponentUtils;

/**
 * @author Matthew Xie
 *
 */
public class ChannelAwareComponentView extends AbstractComponentViewMBean implements ChannelAwareComponentViewMBean {

    private MessageAlertChannelActiveAwareComponent channelAwareComponent;

    /**
     * 
     */
    public ChannelAwareComponentView(MessageAlertChannelActiveAwareComponent channelAwareComponent) {
        this.channelAwareComponent = channelAwareComponent;
    }

    public TabularData browseMessageAsTable() throws OpenDataException {
        List<MessageAlertChannel> channels = channelAwareComponent.getChannels();
        if (CollectionUtils.isBlankCollection(channels)) {
            return null;
        }
        return tabularDataWrapFromChannels(channels);
    }

    @SuppressWarnings("unchecked")
    protected TabularData tabularDataWrapFromChannels(List<MessageAlertChannel> channels) throws OpenDataException {
        if (CollectionUtils.isBlankCollection(channels)) {
            return null;
        }
        String fromKey = "from";
        String toKey = "to";
        List<String> keys = Message.getKeys();
        keys.add(fromKey);
        keys.add(toKey);
        String[] itemNames = keys.toArray(new String[keys.size()]);
        String[] itemDescriptions = itemNames;
        OpenType[] itemTypes = new OpenType[] { SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING };
        CompositeType compositeType = new CompositeType("MessageList", "list spyed messages", itemNames, itemDescriptions, itemTypes);
        TabularType tt = new TabularType("MessageList", "MessageList", compositeType, itemNames);
        TabularDataSupport rc = new TabularDataSupport(tt);
        Map filelds;
        for (MessageAlertChannel channel : channels) {
            try {
                if (channel.getMessage() == null) {
                    continue;
                }
                filelds = channel.getMessage().getFileds();
                filelds.put(fromKey, ComponentUtils.getComponentName(channel.getSpyComponent()));
                filelds.put(toKey, ComponentUtils.getComponentName(channel.getAlertComponent()));
                rc.put(new CompositeDataSupport(compositeType, filelds));
            } catch (Exception e) {
                e.printStackTrace();
                throw new OpenDataException(e.getMessage());
            }
        }
        return rc;
    }

    public int getChannelCount() {
        if (channelAwareComponent == null) {
            return 0;
        }
        List<MessageAlertChannel> channels = channelAwareComponent.getChannels();
        if (CollectionUtils.isBlankCollection(channels)) {
            return 0;
        }
        return channels.size();
    }

    @Override
    protected Component getComponent() {
        return channelAwareComponent;
    }

    public void purge() {
        if (channelAwareComponent != null && channelAwareComponent.getChannels() != null) {
            channelAwareComponent.getChannels().clear();
        }
    }

    public int getMessageCount() {
        List<MessageAlertChannel> channels = channelAwareComponent.getChannels();
        if (CollectionUtils.isBlankCollection(channels)) {
            return 0;
        }
        return channels.size();
    }
}
