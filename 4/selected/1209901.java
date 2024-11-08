package consciouscode.bonsai.tags;

import consciouscode.bonsai.channels.BasicChannel;
import consciouscode.bonsai.channels.Channel;
import consciouscode.bonsai.channels.ChannelProvider;
import java.util.Map;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.Tag;
import org.apache.commons.jelly.TagSupport;

/**
    Class documentation.
*/
public class BonsaiTagUtils {

    public static Channel evalChannelAttribute(String attributeName, Map<String, ?> attributes, Tag tag) throws JellyException {
        Channel channel;
        Object rawChannel = attributes.get(attributeName);
        if (rawChannel == null) {
            channel = new BasicChannel();
        } else if (rawChannel instanceof Channel) {
            channel = (Channel) rawChannel;
        } else if (rawChannel instanceof String) {
            channel = findChannelInParentTags((String) rawChannel, tag);
        } else {
            throw new JellyException("Bad value for attribute " + attributeName + ": " + rawChannel);
        }
        return channel;
    }

    public static Channel findChannelInParentTags(String channelName, Tag tag) throws JellyException {
        PanelTag panelTag = (PanelTag) TagSupport.findAncestorWithClass(tag, PanelTag.class);
        if (panelTag == null) {
            throw new JellyException("No enclosing tag supplies channel '" + channelName + "'");
        }
        ChannelProvider provider = (ChannelProvider) panelTag.getComponent();
        try {
            return provider.getChannel(channelName);
        } catch (IllegalArgumentException e) {
        }
        return findChannelInParentTags(channelName, tag.getParent());
    }
}
