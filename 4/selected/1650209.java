package org.furthurnet.xmlparser;

public class IrcChannel {

    private String channel = null;

    private String description = null;

    public IrcChannel(XmlObject obj) throws XmlException {
        if (!obj.getName().equals(XmlTags.CHAT_CHANNEL_SPEC)) throw new XmlException("Could not read element " + XmlTags.CHAT_CHANNEL_SPEC);
        channel = obj.getAttribute(XmlTags.CHAT_CHANNEL).getValue();
        description = obj.getAttribute(XmlTags.CHAT_CHANNEL_NAME).getValue();
    }

    public String getChannel() {
        return channel;
    }

    public String getDescription() {
        return description;
    }
}
