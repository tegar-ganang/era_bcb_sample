package org.slasoi.common.messaging.pointtopoint.xmpp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.slasoi.common.messaging.MessagingException;
import org.slasoi.common.messaging.Setting;
import org.slasoi.common.messaging.Settings;
import org.slasoi.common.messaging.pointtopoint.Message;
import org.slasoi.common.messaging.pointtopoint.MessageEvent;
import org.slasoi.common.messaging.pointtopoint.MessageListener;

public class Messaging extends org.slasoi.common.messaging.pointtopoint.Messaging {

    protected XMPPConnection connection;

    public Messaging(Settings settings) throws MessagingException {
        super(settings);
        connection = new XMPPConnection(new ConnectionConfiguration(getSetting(Setting.xmpp_host), Integer.parseInt(getSetting(Setting.xmpp_port)), getSetting(Setting.xmpp_service)));
        try {
            connection.connect();
        } catch (XMPPException e) {
            throw new MessagingException(e);
        }
        try {
            connection.login(getSetting(Setting.xmpp_username), getSetting(Setting.xmpp_password), getSetting(Setting.xmpp_resource));
        } catch (XMPPException e) {
            throw new MessagingException(e);
        }
        connection.addPacketListener(new PacketListener() {

            public void processPacket(Packet packet) {
                org.jivesoftware.smack.packet.Message rawMessage = (org.jivesoftware.smack.packet.Message) packet;
                Message message = new Message();
                message.setFrom(rawMessage.getFrom());
                message.setPayload(rawMessage.getBody());
                message.setTo(rawMessage.getTo());
                MessageEvent messageEvent = new MessageEvent(this, message);
                fireMessageEvent(messageEvent);
            }
        }, new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class));
    }

    public void close() {
        connection.disconnect();
    }

    public void addMessageListener(MessageListener messageListener) {
        listenerList.add(MessageListener.class, messageListener);
    }

    public void sendMessage(Message message) {
        org.jivesoftware.smack.packet.Message smackMessage = new org.jivesoftware.smack.packet.Message(message.getTo());
        smackMessage.setBody(message.getPayload());
        connection.sendPacket(smackMessage);
    }

    public String getAddress() {
        return connection.getUser();
    }
}
