package org.slasoi.common.messaging.pubsub.xmpp;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.*;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
import org.jivesoftware.smackx.provider.*;
import org.jivesoftware.smackx.search.UserSearch;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.Setting;
import org.slasoi.common.messaging.Settings;
import org.slasoi.common.messaging.pubsub.Channel;
import org.slasoi.common.messaging.pubsub.MessageEvent;
import org.slasoi.common.messaging.pubsub.PubSubMessage;
import org.slasoi.common.messaging.pubsub.Subscription;
import se.su.it.smack.packet.XMPPElement;
import se.su.it.smack.pubsub.PubSub;
import se.su.it.smack.pubsub.PubSubEvent;
import se.su.it.smack.pubsub.elements.*;
import se.su.it.smack.utils.XMPPUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PubSubManager extends org.slasoi.common.messaging.pubsub.PubSubManager {

    private static final Logger log = Logger.getLogger(PubSubManager.class);

    private static final String ENTRY = "entry";

    private static final String PAYLOAD = "payload";

    private XMPPConnection connection;

    public PubSubManager(Settings settings) throws MessagingException {
        super(settings);
        connect();
        addListener();
    }

    @Override
    public String getId() {
        return getFullJid();
    }

    @Override
    public void createChannel(Channel channel) throws MessagingException {
        String channelName = channel.getName();
        if (!isChannel(channelName)) {
            PubSub pubsub = createPubSub();
            pubsub.setType(Type.SET);
            CreateElement createElement = new CreateElement(prepareChannelName(channelName));
            ConfigureElement configure = new ConfigureElement();
            configure.addField("pubsub#send_item_subscribe", "0");
            configure.addField("pubsub#notify_config", "0");
            configure.addField("pubsub#notify_delete", "1");
            configure.addField("pubsub#notify_retract", "0");
            configure.addField("pubsub#publish_model", "open");
            pubsub.addChild(createElement);
            pubsub.addChild(configure);
            try {
                XMPPUtils.sendAndWait(connection, pubsub);
            } catch (XMPPException e) {
                if (e.getXMPPError().getCode() == 409) {
                    throw new MessagingException("Error creating new channel (channel already exists)");
                } else {
                    throw new MessagingException(e);
                }
            }
            unsubscribe(channelName);
        }
    }

    @Override
    public boolean isChannel(String channelName) throws MessagingException {
        String channel = prepareChannelName(channelName);
        ServiceDiscoveryManager sdm = new ServiceDiscoveryManager(connection);
        DiscoverItems discoverItems;
        try {
            discoverItems = sdm.discoverItems(getSetting(Setting.xmpp_pubsubservice));
            Iterator<Item> itor = discoverItems.getItems();
            while (itor.hasNext()) {
                Item item = itor.next();
                if (item.getNode().equals(channel)) {
                    return true;
                }
            }
        } catch (XMPPException e) {
            throw new MessagingException(e);
        }
        return false;
    }

    @Override
    public void deleteChannel(String channelName) throws MessagingException {
        try {
            unsubscribe(channelName);
        } catch (MessagingException err) {
            log.warn("Failed to unsubscribe when deleting the channel - you may not be subscribed!");
        }
        if (isChannel(channelName)) {
            Channel channel = new Channel(channelName);
            PubSub pubsub = createPubSub();
            pubsub.setType(Type.SET);
            pubsub.setNamespace("http://jabber.org/protocol/pubsub#owner");
            DeleteElement delete = new DeleteElement(prepareChannelName(channel.getName()));
            pubsub.addChild(delete);
            try {
                XMPPUtils.sendAndWait(connection, pubsub);
            } catch (XMPPException e) {
                throw new MessagingException(e);
            }
        }
    }

    @Override
    public void publish(PubSubMessage message) throws MessagingException {
        message.setFrom(getFullJid());
        PubSub pubsub = createPubSub();
        pubsub.setType(Type.SET);
        String buffer = encode(message);
        PublishElement publishElement = new PublishElement(prepareChannelName(message.getChannelName()));
        publishElement.addChild(new ItemElement(null, "<" + ENTRY + ">" + buffer + "</" + ENTRY + ">"));
        pubsub.addChild(publishElement);
        connection.sendPacket(pubsub);
    }

    @Override
    public void subscribe(String channelName) throws MessagingException {
        Channel channel = new Channel(channelName);
        unsubscribe(channel.getName());
        PubSub pubsub = createPubSub();
        pubsub.setType(Type.SET);
        SubscribeElement subscribeElement = new SubscribeElement(prepareChannelName(channel.getName()), getFullJid());
        pubsub.addChild(subscribeElement);
        try {
            XMPPUtils.sendAndWait(connection, pubsub);
        } catch (XMPPException e) {
            if (e.getXMPPError().getCode() == 404) {
                throw new MessagingException("Error subscribing (channel " + channelName + " does not exist)");
            } else {
                throw new MessagingException(e);
            }
        }
    }

    @Override
    public void unsubscribe(String channelName) throws MessagingException {
        Channel channel = new Channel(channelName);
        List<String> subids = getSubid(prepareChannelName(channel.getName()));
        if (subids.size() > 0) {
            try {
                for (int i = 0; i < subids.size(); i++) {
                    PubSub pubsub = createPubSub();
                    pubsub.setType(Type.SET);
                    UnsubscribeElement unsubscribeElement = new UnsubscribeElement(prepareChannelName(channel.getName()), getFullJid(), subids.get(i));
                    pubsub.addChild(unsubscribeElement);
                    XMPPUtils.sendAndWait(connection, pubsub);
                }
            } catch (XMPPException e) {
                throw new MessagingException(e);
            }
        } else {
            log.debug("Unsubscribe warning: not subscribed to the channel named " + channel.getName());
        }
    }

    @Override
    public void close() {
        connection.disconnect();
    }

    private PubSub createPubSub() {
        PubSub pubsub = new PubSub();
        pubsub.setNamespace("http://jabber.org/protocol/pubsub");
        pubsub.setTo(getSetting(Setting.xmpp_pubsubservice));
        pubsub.setFrom(getFullJid());
        return pubsub;
    }

    private String getFullJid() {
        return connection.getUser();
    }

    private List<String> getSubid(String node) throws MessagingException {
        PubSub pubsub = createPubSub();
        pubsub.setType(Type.GET);
        pubsub.setFrom(getFullJid());
        SubscriptionsElement subscriptions = new SubscriptionsElement();
        subscriptions.setNode(node);
        pubsub.addChild(subscriptions);
        try {
            ArrayList<String> subids = new ArrayList<String>();
            org.jivesoftware.smack.packet.Packet packet = XMPPUtils.sendAndWait(connection, pubsub);
            List<XMPPElement> subs = ((SubscriptionsElement) ((PubSub) packet).getChild()).getChildren();
            Iterator<XMPPElement> itor = subs.iterator();
            while (itor.hasNext()) {
                SubscriptionElement subscriprionElement = (SubscriptionElement) itor.next();
                if (subscriprionElement.getNode().equals(node)) {
                    subids.add(subscriprionElement.getSubid());
                }
            }
            return subids;
        } catch (XMPPException e) {
            throw new MessagingException(e);
        }
    }

    @Override
    protected void connect() throws MessagingException {
        configure(ProviderManager.getInstance());
        connection = new XMPPConnection(new ConnectionConfiguration(getSetting(Setting.xmpp_host), Integer.parseInt(getSetting(Setting.xmpp_port)), getSetting(Setting.xmpp_service)));
        XMPPConnection.addConnectionCreationListener(new ConnectHandler(connection, getSettings()));
        try {
            connection.connect();
        } catch (XMPPException e) {
            throw new MessagingException(e);
        }
    }

    private void configure(ProviderManager pm) {
        pm.addIQProvider("query", "jabber:iq:private", new PrivateDataManager.PrivateDataIQProvider());
        try {
            pm.addIQProvider("query", "jabber:iq:time", Class.forName("org.jivesoftware.smackx.packet.Time"));
        } catch (ClassNotFoundException e) {
        }
        pm.addExtensionProvider("x", "jabber:x:roster", new RosterExchangeProvider());
        pm.addExtensionProvider("x", "jabber:x:event", new MessageEventProvider());
        pm.addExtensionProvider("active", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("composing", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("paused", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("inactive", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("gone", "http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
        pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im", new XHTMLExtensionProvider());
        pm.addExtensionProvider("x", "jabber:x:conference", new GroupChatInvitation.Provider());
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
        pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());
        pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user", new MUCUserProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin", new MUCAdminProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner", new MUCOwnerProvider());
        pm.addExtensionProvider("x", "jabber:x:delay", new DelayInformationProvider());
        try {
            pm.addIQProvider("query", "jabber:iq:version", Class.forName("org.jivesoftware.smackx.packet.Version"));
        } catch (ClassNotFoundException e) {
        }
        pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());
        pm.addIQProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageRequest.Provider());
        pm.addExtensionProvider("offline", "http://jabber.org/protocol/offline", new OfflineMessageInfo.Provider());
        pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());
        pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());
        pm.addIQProvider("sharedgroup", "http://www.jivesoftware.org/protocol/sharedgroup", new SharedGroupsInfo.Provider());
        pm.addExtensionProvider("addresses", "http://jabber.org/protocol/address", new MultipleAddressesProvider());
        pm.addIQProvider("si", "http://jabber.org/protocol/si", new StreamInitiationProvider());
        pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams", new BytestreamsProvider());
        pm.addIQProvider("open", "http://jabber.org/protocol/ibb", new IBBProviders.Open());
        pm.addIQProvider("close", "http://jabber.org/protocol/ibb", new IBBProviders.Close());
        pm.addExtensionProvider("data", "http://jabber.org/protocol/ibb", new IBBProviders.Data());
        pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());
        pm.addIQProvider("command", "http://jabber.org/protocol/commands", new org.jivesoftware.smackx.provider.AdHocCommandDataProvider());
        pm.addExtensionProvider("bad-action", "http://jabber.org/protocol/commands", new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadActionError());
        pm.addExtensionProvider("bad-action", "http://jabber.org/protocol/commands", new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadActionError());
        pm.addExtensionProvider("malformed-action", "http://jabber.org/protocol/commands", new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.MalformedActionError());
        pm.addExtensionProvider("bad-locale", "http://jabber.org/protocol/commands", new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadLocaleError());
        pm.addExtensionProvider("bad-payload", "http://jabber.org/protocol/commands", new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadPayloadError());
        pm.addExtensionProvider("bad-sessionid", "http://jabber.org/protocol/commands", new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.BadSessionIDError());
        pm.addExtensionProvider("session-expired", "http://jabber.org/protocol/commands", new org.jivesoftware.smackx.provider.AdHocCommandDataProvider.SessionExpiredError());
        pm.addExtensionProvider("event", "http://jabber.org/protocol/pubsub#event", new se.su.it.smack.provider.PubSubEventProvider());
        pm.addExtensionProvider("x", "http://jabber.org/protocol/pubsub#event", new se.su.it.smack.provider.PubSubXEventProvider());
        pm.addIQProvider("pubsub", "http://jabber.org/protocol/pubsub", new se.su.it.smack.provider.PubSubProvider());
    }

    private void addListener() throws MessagingException {
        connection.addPacketListener(new PacketListener() {

            public void processPacket(Packet packet) {
                PubSubEvent event = XMPPUtils.getPubSubEvent(packet);
                if (event != null) {
                    XMPPElement e1 = event.getChild();
                    PubSubMessage message = extractMessage(e1);
                    message.setChannelName(extractChannelName(((ItemsElement) e1).getNode()));
                    MessageEvent messageEvent = new MessageEvent(this, message);
                    fireMessageEvent(messageEvent);
                }
            }
        }, getPacketFilter());
    }

    private PacketFilter getPacketFilter() throws MessagingException {
        String setting = getSetting(Setting.xmpp_publish_echo_enabled);
        if (setting == null || setting.equals(Settings.FALSE)) {
            return new SelfPacketFilter();
        } else if (setting.equals(Settings.TRUE)) {
            return null;
        }
        throw new MessagingException("Malformed " + Setting.xmpp_publish_echo_enabled.toString() + " setting");
    }

    private class SelfPacketFilter implements PacketFilter {

        public boolean accept(Packet packet) {
            PubSubEvent evt = XMPPUtils.getPubSubEvent(packet);
            if (evt == null) {
                return false;
            } else {
                if (fromSelf(evt)) {
                    return false;
                } else {
                    return true;
                }
            }
        }

        private boolean fromSelf(PubSubEvent evt) {
            boolean isFromSelf = true;
            PubSubMessage message = extractMessage(evt.getChild());
            if (message.getFrom().startsWith(getFullJid())) {
                isFromSelf = true;
            } else {
                isFromSelf = false;
            }
            return isFromSelf;
        }
    }

    private PubSubMessage extractMessage(XMPPElement e1) {
        String buffer = null;
        if (e1 instanceof ItemsElement) {
            XMPPElement e2 = ((ItemsElement) e1).getChild();
            if (e2 instanceof ItemElement) {
                buffer = ((ItemElement) e2).getContentXML();
                if (buffer.indexOf("<" + ENTRY) == 0) {
                    int index1 = buffer.indexOf(">");
                    int index2 = buffer.lastIndexOf("<");
                    buffer = buffer.substring(index1 + 1, index2);
                }
                String payloadStart = "<" + PAYLOAD;
                if (buffer.contains(payloadStart)) {
                    int index1 = buffer.indexOf(payloadStart);
                    int index2 = buffer.indexOf("</" + PAYLOAD);
                    String payload = buffer.substring(index1 + payloadStart.length() + 1, index2);
                    buffer = buffer.replace(payload, StringEscapeUtils.escapeXml(payload));
                }
            }
        }
        return decode(buffer);
    }

    @Override
    public List<Subscription> getSubscriptions() throws MessagingException {
        PubSub subsMsg = new PubSub();
        subsMsg.setTo(getSetting(Setting.xmpp_pubsubservice));
        subsMsg.setType(IQ.Type.GET);
        subsMsg.setFrom(getFullJid());
        SubscriptionsElement subs = new SubscriptionsElement();
        subsMsg.addChild(subs);
        Packet response = null;
        try {
            response = XMPPUtils.sendAndWait(connection, subsMsg);
        } catch (XMPPException e1) {
            e1.printStackTrace();
        }
        List<Subscription> subsList1 = new ArrayList<Subscription>();
        if (response != null && response instanceof PubSub) {
            PubSub res = (PubSub) response;
            List<XMPPElement> list = res.getChildren();
            for (int i = 0; i < list.size(); i++) {
                XMPPElement e = list.get(i);
                if (e instanceof SubscriptionsElement) {
                    SubscriptionsElement se = (SubscriptionsElement) e;
                    List<XMPPElement> slist = se.getChildren();
                    for (int j = 0; j < slist.size(); j++) {
                        Subscription sub = new Subscription(((SubscriptionElement) slist.get(j)).getSubid(), ((SubscriptionElement) slist.get(j)).getNode());
                        subsList1.add(sub);
                    }
                }
            }
        }
        return subsList1;
    }
}
