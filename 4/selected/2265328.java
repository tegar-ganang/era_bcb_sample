package org.furthurnet.xmlparser;

import java.util.Vector;

public class IrcSpec {

    private Vector addresses;

    private Vector channels;

    private IrcSpec() {
    }

    public IrcSpec(XmlObject obj) throws XmlException {
        if (!obj.getName().equals(XmlTags.CHAT_SPECS)) throw new XmlException("Could not read element " + XmlTags.CHAT_SPECS);
        addresses = new Vector();
        channels = new Vector();
        for (int i = 0; i < obj.numAttributes(); i++) {
            XmlObject next = obj.getAttribute(i);
            if (next.getName().equals(XmlTags.CHAT_ADDRESS)) addresses.add(next.getValue()); else if (next.getName().equals(XmlTags.CHAT_CHANNEL_SPEC)) channels.add(new IrcChannel(next));
        }
    }

    public int numAddresses() {
        return addresses == null ? 0 : addresses.size();
    }

    public String getAddress(int i) {
        return (String) addresses.elementAt(i);
    }

    public String addressesToString() {
        return addresses == null ? "" : addresses.toString();
    }

    public int numChannels() {
        return channels == null ? 0 : channels.size();
    }

    public IrcChannel getChannel(int i) {
        return (IrcChannel) channels.elementAt(i);
    }
}
