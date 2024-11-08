package org.javver;

import java.io.IOException;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import junit.framework.TestCase;

public class OtherTest {

    private static String username = "smack.test";

    private static String password = "ignite";

    public static class MessageParrot implements PacketListener {

        private XMPPConnection xmppConnection;

        public MessageParrot(XMPPConnection conn) {
            xmppConnection = conn;
        }

        public void processPacket(Packet packet) {
            Message message = (Message) packet;
            if (message.getBody() != null) {
                String fromName = StringUtils.parseBareAddress(message.getFrom());
                System.out.println("Message from " + fromName + "\n" + message.getBody() + "\n");
                Message reply = new Message();
                reply.setTo(fromName);
                reply.setBody("I am a Java bot. You said: " + message.getBody());
                xmppConnection.sendPacket(reply);
            }
        }
    }

    ;

    public void test() {
        String[] args = {};
        System.out.println("Starting IM client");
        ConnectionConfiguration connConfig = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
        XMPPConnection connection = new XMPPConnection(connConfig);
        try {
            connection.connect();
            System.out.println("Connected to " + connection.getHost());
        } catch (XMPPException ex) {
            System.out.println("Failed to connect to " + connection.getHost());
            System.exit(1);
        }
        try {
            connection.login(username, password);
            System.out.println("Logged in as " + connection.getUser());
            Presence presence = new Presence(Presence.Type.available);
            connection.sendPacket(presence);
        } catch (XMPPException ex) {
            System.out.println("Failed to log in as " + username);
            System.exit(1);
        }
        PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        connection.addPacketListener(new MessageParrot(connection), filter);
        if (args.length > 0) {
            Message msg = new Message(args[0], Message.Type.chat);
            msg.setBody("Test");
            connection.sendPacket(msg);
        }
        System.out.println("Press enter to disconnect\n");
        try {
            System.in.read();
        } catch (IOException ex) {
        }
        connection.disconnect();
    }
}
