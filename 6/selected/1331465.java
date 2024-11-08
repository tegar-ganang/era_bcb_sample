package net.sourceforge.scrollrack;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Message;
import net.sourceforge.scrollrack.event.ConnectEvent;
import net.sourceforge.scrollrack.event.DisconnectEvent;
import net.sourceforge.scrollrack.event.MessageEvent;

public class JabberConnection extends Thread implements Connection {

    private String server;

    private String username;

    private String password;

    private EventQueue queue;

    private XMPPConnection connection;

    private String opponent;

    private List challengers;

    /**
 * Process these messages from any Jabber user.
 */
    public static final String CONNECT_MESSAGE = "CONNECT";

    public static final String GOODBYE_MESSAGE = "GOODBYE";

    /**
 * The resource identifies the Jabber client and whatnot.
 */
    private static final String RESOURCE = "ScrollRack";

    /**
 * Create a wrapper around XMPPConnection.
 */
    public JabberConnection(String server, String username, String password, EventQueue queue) {
        this.server = server;
        this.username = username;
        this.password = password;
        this.queue = queue;
        this.challengers = new ArrayList();
        start();
    }

    public boolean is_connected() {
        return ((connection != null) && connection.isConnected());
    }

    public void run() {
        PacketFilter filter;
        PacketCollector collector;
        Packet packet;
        Message message;
        String sender, text;
        try {
            connection = new XMPPConnection(server);
            connection.connect();
            connection.login(username, password, RESOURCE);
            queue.enqueue(new ConnectEvent(ConnectEvent.JABBER));
            filter = new PacketFilter() {

                public boolean accept(Packet packet) {
                    return true;
                }
            };
            collector = connection.createPacketCollector(filter);
            while (true) {
                packet = collector.nextResult();
                if (!(packet instanceof Message)) {
                    System.out.println("ignored: " + packet);
                    continue;
                }
                message = (Message) packet;
                sender = message.getFrom();
                if (sender == null) {
                    System.out.println("ignored: " + packet);
                    continue;
                }
                text = message.getBody();
                if (text == null) continue;
                if ((opponent == null) || !sender.equalsIgnoreCase(opponent)) {
                    if (!(text.equals(CONNECT_MESSAGE) || text.equals(GOODBYE_MESSAGE))) {
                        System.out.println("ignored: " + sender + ": " + text);
                        continue;
                    }
                }
                MessageEvent event = new MessageEvent(sender, text);
                queue.enqueue(event);
            }
        } catch (Exception exception) {
            System.out.println("exception: " + exception);
            disconnect(exception);
        }
        disconnect();
    }

    /**
 * Send a line of text over the connection.
 */
    public void send(String text) {
        if ((opponent == null) || (connection == null)) return;
        Message message = new Message(opponent, Message.Type.normal);
        message.setBody(text);
        connection.sendPacket(message);
    }

    /**
 * Break an established connection, or stop trying to connect.
 */
    public void disconnect() {
        int state = (connection == null ? IDLE : (!connection.isConnected() ? CONNECTING : CONNECTED));
        disconnect(new DisconnectEvent(state));
    }

    private void disconnect(Object event) {
        if ((connection != null) && connection.isConnected()) connection.disconnect();
        connection = null;
        opponent = null;
        challengers.clear();
        if (queue != null) queue.enqueue(event);
    }

    /**
 * Who has challenged me?
 */
    public List get_challengers() {
        return challengers;
    }

    public boolean add_challenger(String challenger) {
        Iterator iterator = challengers.iterator();
        while (iterator.hasNext()) {
            String item = (String) iterator.next();
            if (item.equalsIgnoreCase(challenger)) return (false);
        }
        challengers.add(challenger);
        return (true);
    }

    public boolean remove_challenger(String challenger) {
        Iterator iterator = challengers.iterator();
        while (iterator.hasNext()) {
            String item = (String) iterator.next();
            if (item.equalsIgnoreCase(challenger)) {
                iterator.remove();
                return (true);
            }
        }
        return (false);
    }

    /**
 * Who am I currently sending messages to?
 */
    public String get_opponent() {
        return opponent;
    }

    public void set_opponent(String name) {
        opponent = get_jabber_id(name);
    }

    public void clear_opponent() {
        opponent = null;
    }

    /**
 * Map "name" to "name@server/resource".
 */
    public String get_jabber_id(String name) {
        int idx = name.indexOf('@');
        if (idx < 0) {
            name += "@" + connection.getHost() + "/" + RESOURCE;
        } else {
            idx = name.indexOf("/");
            if (idx < 0) name += "/" + RESOURCE;
        }
        return (name);
    }
}
