package sunlabs.brazil.xmpp;

import sunlabs.brazil.server.Server;
import sunlabs.brazil.template.RewriteContext;
import sunlabs.brazil.template.Template;
import sunlabs.brazil.util.StringMap;
import sunlabs.brazil.template.QueueTemplate;
import java.util.Properties;
import java.util.Hashtable;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smackx.packet.VCard;

/**
 * XMPP client template. (Incomplete - still under construction.)
 * This uses the SMACK library at:
 * {@link http://www.igniterealtime.org/projects/smack/}
 * It has been tested with version 3.0.4.
 */
public class IMTemplate extends Template {

    boolean debug = false;

    static Hashtable connections = new Hashtable();

    public static final String[] vcardFields = new String[] { "FN", "NICKNAME", "PHOTO", "BDAY", "JABBERID", "MAILER", "TZ", "GEO", "TITLE", "ROLE", "LOGO", "NOTE", "PRODID", "REV", "SORT-STRING", "SOUND", "UID", "URL", "DESC" };

    /**
     * <dl>
     * <dt><code>&lt;im command=login id=xx server=xx
     *     user=xx pass=xx
     *     [host=xx port=nn prepend=xxx]
     *     [subscribe=reject|decide]
     *     [qid=xx]
     * &gt;</code>
     * <dd>Log into an XMPP server. "host" and "port" are only required
     *     when the values implied by the "server" attribute are not correct.
     *     "id" is an arbirary identifier used to identify this session.  It
     *     must be used with all other "im" commands, and is also the name of
     *     the Queue used to receive messages from.
     *     If "subscribe" is specified, it indicates the buddy subscription
     *     mode, which defaults to automatic.
     *     If "decide" is used, then it is up to the client to accept or
     *     reject subscriptions. (The default is to accept all subscriptions).
     *     <p>
     *     The value of "qid" can be used to override the Q name, allowing
     *     multiple accounts (id) to be used with the same Q (qid).
     * <dt><code>&lt;im command=logout id=xx&gt;</code>
     * <dd>Log out of this session.  Any entries in the Queue remain.
     * <dt><code>&lt;im command=message id=xx to=xxx body=xxx
     *     [thread=xxx]&gt;</code>
     * <dd>Send a message to another user (e.g. jow.blow@gmail.com).
     * <dt><code>&lt;im command=presence id=xx
     *    [mode=available|away|chat|dnd|xa
     *     type=available|unavailable|subscribe|subscribed|unsubscribe|unsubscribed
     *     body=xxx to=xxx]&gt;</code>
     * <dd>Send presence information.
     *     <dl>
     *     <dt>mode<dd>If type is "available", meaning the client is accepting
     *     messages, the mode specifies the client's presence status.
     *     (defaults to "available")
     *     <dt>type<dd>This sets the message type.  "available", and "unavailable"
     *     are used to indicate the client state, which is "available" if the client
     *     is connected (even if the "mode" is unavailable), and "unavailable" if
     *     they are disconnected.  "available" is the default.
     *     <p>
     *     The other types are for managing subscriptions:<br>
     *     - subscribe: request a subscription ("to" is required)<br>
     *     - subscribed: grant a subscription request<br>
     *     - unsubscribe: request subscription removal ("to" is required)<br>
     *     - unsubscribed: grant a subscription removal request, or reject a subscription
     *       request.
     *     <dt>body<dd>The presence message (for type=available)
     *     </dl>
     * <dt><code>&lt;im command=getroster id=xx [prepend=xx]&gt;</code>
     * <dd>Retrieve your buddy list (roster in smack-speak).
     * <dt><code>&lt;im command=addroster id=xx user=xx
     *     [name=xx groups="g1 g2..."]&gt;</code>
     * <dd>Add a buddy to the buddy list, in the specified groups.  Groups are
     * created as-needed.
     * <dt><code>&lt;im command=deluser id=xx user=xx&gt;</code>
     * <dd>Remove a buddy from the buddy list.
     * <dt><code>&lt;im command=vcard id=xx [user=xx prepend=xx]&gt;</code>
     * <dd>Retrieve VCARD information.  If "user" isn' specified, then the
     * currently logged-in user's vCard is returned.
     * </dl>
     *
     * Notes:
     * <ul>
     * <li>[prefix].status will contain the result of the command
     * <li>All received messages will be available from the queue "id" (or qid if
     *     provided) , as in:
     *     <dequeue name=[id]>
     * <li>The "debug" attribute will cause the template to spew more stuff to
     *     the console.
     * <li>Logging in to an already logged-in "id" forces the preexisting connection
     * to be dropped
     * </ul>
     */
    public void tag_im(RewriteContext hr) {
        hr.killToken();
        debug = hr.isTrue("debug");
        debug(hr);
        if (debug) {
            System.out.println(hr.getToken());
        }
        String command = hr.get("command");
        String id = hr.get("id");
        hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, " id=" + id + " cmd=" + command);
        if (id == null || command == null) {
            debug(hr, "id or command attributes missing");
            return;
        }
        if (command.equals("login")) {
            XMPPConnection con = doLogin(hr, id);
            if (con != null) {
                Roster roster = con.getRoster();
                String sub = hr.get("subscribe");
                if (sub != null) {
                    if (sub.equals("reject")) {
                        roster.setSubscriptionMode(Roster.SubscriptionMode.reject_all);
                    } else if (sub.equals("decide")) {
                        roster.setSubscriptionMode(Roster.SubscriptionMode.manual);
                    }
                }
                mapRoster(hr, roster);
                con.addPacketListener(new MessageRelay(con, hr.get("qid", id)), new MyFilter());
            }
            return;
        }
        XMPPConnection con = (XMPPConnection) connections.get(id);
        if (con == null) {
            logStatus(hr, "error: id (" + id + ") not logged in for: " + command);
            return;
        }
        if (command.equals("logout")) {
            con.sendPacket(new Presence(Presence.Type.unavailable));
            con.disconnect();
            connections.remove(id);
            logStatus(hr, "logout succeeded");
        } else if (command.equals("message")) {
            String to = hr.get("to");
            if (to == null) {
                logStatus(hr, "error: 'to' required");
            }
            Message msg = new Message(to);
            msg.setBody(hr.get("body", "hello"));
            String thread = hr.get("thread");
            if (thread != null) {
                msg.setThread(thread);
            }
            con.sendPacket(msg);
            logStatus(hr, "message sent");
        } else if (command.equals("presence")) {
            String mode = hr.get("mode", "available");
            String type = hr.get("type");
            String body = hr.get("body");
            String to = hr.get("to");
            Presence.Type pt = Presence.Type.available;
            try {
                pt = Presence.Type.valueOf(hr.get("type", "available"));
            } catch (IllegalArgumentException e) {
            }
            Presence pr = new Presence(pt);
            if (body != null) {
                pr.setStatus(body);
            }
            try {
                pr.setMode(Presence.Mode.valueOf(mode));
            } catch (IllegalArgumentException e) {
            }
            if (to != null) {
                pr.setTo(to);
            }
            con.sendPacket(pr);
            logStatus(hr, "presence sent");
        } else if (command.equals("getroster")) {
            mapRoster(hr, con.getRoster());
        } else if (command.equals("addroster")) {
            String user = hr.get("user");
            String name = hr.get("name", user);
            String groups = hr.get("groups");
            if (user == null) {
                logStatus(hr, "error: user required");
                return;
            }
            Roster roster = con.getRoster();
            String[] groupA = toArray(groups);
            for (int i = 0; groupA != null && i < groupA.length; i++) {
                if (roster.getGroup(groupA[i]) == null) {
                    roster.createGroup(groupA[i]);
                }
            }
            try {
                roster.createEntry(user, name, toArray(groups));
                logStatus(hr, "roster entry created");
            } catch (XMPPException e) {
                logStatus(hr, "error: " + e.getMessage());
            }
        } else if (command.equals("deluser")) {
            RosterEntry re = con.getRoster().getEntry(hr.get("user"));
            if (re != null) {
                try {
                    con.getRoster().removeEntry(re);
                    logStatus(hr, "roster entry removed");
                } catch (XMPPException e) {
                    logStatus(hr, "error: " + e.getMessage());
                }
            } else {
                logStatus(hr, "error:  + invalid user");
            }
        } else if (command.equals("vcard")) {
            VCard vcard = new VCard();
            String user = hr.get("user");
            try {
                if (user != null) {
                    vcard.load(con, user);
                } else {
                    vcard.load(con);
                }
            } catch (XMPPException e) {
                logStatus(hr, "error in vcard: " + e);
                return;
            }
            String prepend = getPrepend(hr);
            Dictionary map = hr.request.props;
            setEntry(map, prepend + "xml", vcard.toXML());
            for (int i = 0; i < vcardFields.length; i++) {
                String field = vcardFields[i];
                setEntry(map, prepend + field.toLowerCase(), vcard.getField(field));
            }
        } else {
            logStatus(hr, "error: command " + command + "not implemented");
        }
        return;
    }

    /**
     * Turn a space delimited String into an array
     */
    String[] toArray(String list) {
        if (list == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(list);
        String[] result = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens()) {
            result[i++] = st.nextToken();
        }
        return result;
    }

    /**
     * Login a user
     */
    XMPPConnection doLogin(RewriteContext hr, String id) {
        String server = hr.get("server", null);
        String user = hr.get("user");
        String pass = hr.get("pass");
        int port = 5269;
        if (server == null || user == null || pass == null) {
            logStatus(hr, "error: login reqires server, user, and pass");
            return null;
        }
        try {
            port = Integer.parseInt(hr.get("port"));
        } catch (Exception e) {
        }
        XMPPConnection con = (XMPPConnection) connections.get(id);
        if (con != null) {
            System.out.println("disconnecting " + id);
            hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, "Connection already exists for (" + id + ") - killing");
            con.disconnect();
        }
        con = connect(id, server, user, pass, hr.get("host"), port);
        if (con != null) {
            connections.put(id, con);
            logStatus(hr, "login: Connection succeeded");
        } else {
            connections.remove(id);
            logStatus(hr, "error: login failed");
        }
        return con;
    }

    void logStatus(RewriteContext hr, String s) {
        hr.request.props.put(hr.prefix + "status", s);
        hr.request.log(Server.LOG_DIAGNOSTIC, hr.prefix, s);
        debug(hr, s);
    }

    XMPPConnection connect(String id, String server, String user, String pass, String host, int port) {
        ConnectionConfiguration config;
        if (host == null) {
            config = new ConnectionConfiguration(server);
        } else {
            config = new ConnectionConfiguration(host, (port <= 0 ? 5269 : port), server);
        }
        XMPPConnection con = new XMPPConnection(config);
        try {
            con.connect();
            con.login(user, pass, "brazil" + id);
        } catch (XMPPException e) {
            System.out.println("Connect failure: " + e);
            return null;
        }
        return con;
    }

    /**
     * Set an entry into map, dealing with nulls
     */
    void setEntry(Dictionary map, String name, String value) {
        if (value != null && !value.equals("")) {
            map.put(name, value);
            if (debug) {
                System.out.println("  set: " + name + "=" + value);
            }
        }
    }

    /**
     * Dump a roster into properties.
     */
    void mapRoster(RewriteContext hr, Roster roster) {
        Properties map = hr.request.props;
        String users = null;
        String prepend = getPrepend(hr);
        Iterator it = roster.getEntries().iterator();
        while (it.hasNext()) {
            RosterEntry re = (RosterEntry) it.next();
            String user = re.getUser();
            users = users == null ? user : users + " " + user;
            String pre = prepend + user + ".";
            Presence p = roster.getPresence(user);
            setPresenceEntries(map, pre, roster.getPresence(user));
            setEntry(map, pre + "name", re.getName());
            Iterator groups = re.getGroups().iterator();
            StringBuffer sb = new StringBuffer();
            while (groups.hasNext()) {
                sb.append(((RosterGroup) groups.next()).getName()).append(" ");
            }
            setEntry(map, pre + "groups", sb.toString().trim());
        }
        if (users != null) {
            setEntry(map, prepend + "users", users);
        }
        StringBuffer sb = new StringBuffer();
        it = roster.getGroups().iterator();
        while (it.hasNext()) {
            sb.append(((RosterGroup) it.next()).getName()).append(" ");
        }
        setEntry(map, prepend + "groups", sb.toString().trim());
    }

    void setPresenceEntries(Dictionary map, String pre, Presence p) {
        setEntry(map, pre + "body", p.getStatus());
        setEntry(map, pre + "priority", "" + p.getPriority());
        if (p.getType() != null) {
            setEntry(map, pre + "type", p.getType().toString());
        }
        if (p.getMode() != null) {
            setEntry(map, pre + "state", p.getMode().toString());
        }
    }

    /**
     * Listen for XMP packets, turn the results into name/value pairs, and
     * forward the "map" to the proper Queue.
     */
    public class MessageRelay implements PacketListener {

        XMPPConnection con;

        String qid;

        public MessageRelay(XMPPConnection con, String qid) {
            this.con = con;
            this.qid = qid;
        }

        public void processPacket(Packet pkt) {
            StringMap map = new StringMap();
            setEntry(map, "host", con.getHost());
            setEntry(map, "conId", con.getConnectionID());
            setEntry(map, "user", con.getUser());
            setEntry(map, "sender", pkt.getFrom());
            setEntry(map, "xml", pkt.toXML());
            Iterator it = pkt.getPropertyNames().iterator();
            while (it.hasNext()) {
                String name = (String) it.next();
                String value = pkt.getProperty(name).toString();
                map.put(name, value);
            }
            if (pkt instanceof Presence) {
                Presence p = (Presence) pkt;
                setEntry(map, "category", "presence");
                setPresenceEntries(map, "", p);
            } else if (pkt instanceof Message) {
                Message p = (Message) pkt;
                setEntry(map, "category", "message");
                setEntry(map, "type", p.getType().toString());
                setEntry(map, "body", p.getBody());
                setEntry(map, "thread", p.getThread());
                setEntry(map, "subject", p.getSubject());
            } else if (pkt instanceof RosterPacket) {
                RosterPacket rp = (RosterPacket) pkt;
                setEntry(map, "category", "roster");
                setEntry(map, "type", rp.getType().toString());
                setEntry(map, "entryCount", "" + rp.getRosterItemCount());
                it = rp.getRosterItems().iterator();
                int i = 0;
                String entries = "";
                while (it.hasNext()) {
                    RosterPacket.Item re = (RosterPacket.Item) it.next();
                    setEntry(map, "entry." + i + ".type", re.getItemType().toString());
                    setEntry(map, "entry." + i + ".status", re.getItemStatus().toString());
                    setEntry(map, "entry." + i + ".name", re.getName());
                    setEntry(map, "entry." + i + ".user", re.getUser());
                    entries += i + " ";
                    i++;
                }
                setEntry(map, "entries", entries.trim());
            } else {
                System.out.println(qid + ") packet?: " + pkt.toXML());
                setEntry(map, "type", "unknown");
            }
            String from = pkt.getFrom();
            from = from == null ? "unknown" : from;
            if (debug) {
                System.out.println(from + "->" + qid + ": " + map);
            }
            QueueTemplate.enqueue(qid, from, map, false, false);
        }
    }

    String getPrepend(RewriteContext hr) {
        String prepend = hr.get("prepend", hr.prefix);
        if (!prepend.endsWith(".")) {
            prepend += ".";
        }
        return prepend;
    }

    static class MyFilter implements PacketFilter {

        public boolean accept(Packet pkt) {
            return (pkt instanceof Presence) || (pkt instanceof Message) || (pkt instanceof RosterPacket);
        }
    }

    /**
     * command line interface?.
     * user pass server [host port]
     */
    public static void main(String[] args) throws Exception {
    }
}
