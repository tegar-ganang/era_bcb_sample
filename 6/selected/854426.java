package org.localstorm.mcc.xmpp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListMap;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

/**
 *
 * @author Alexey Kuznetsov
 */
public class ConnectionRespawnerTimerTask extends TimerTask {

    private static final Logger log = Logger.getLogger(ConnectionRespawnerTimerTask.class);

    private final ConcurrentSkipListMap<JID, XmppAgent> agents;

    private final ConcurrentSkipListMap<JID, XMPPConnection> connections;

    private final XmppHandler handler;

    public ConnectionRespawnerTimerTask(XmppHandler handler, ConcurrentSkipListMap<JID, XmppAgent> agents, ConcurrentSkipListMap<JID, XMPPConnection> connections) {
        this.handler = handler;
        this.agents = agents;
        this.connections = connections;
    }

    @Override
    public void run() {
        Map<JID, XmppAgent> _agentsBackup = new HashMap<JID, XmppAgent>();
        Map<JID, XmppAgent> _agents = new HashMap<JID, XmppAgent>();
        _agents.putAll(agents);
        _agentsBackup.putAll(_agents);
        Set<JID> _toRespawn = new HashSet<JID>();
        Set<JID> _toShutdown = new HashSet<JID>();
        for (Map.Entry<JID, XMPPConnection> entry : connections.entrySet()) {
            JID jid = entry.getKey();
            XMPPConnection conn = entry.getValue();
            XmppAgent agent = _agents.remove(jid);
            if (agent != null) {
                if (!conn.isConnected()) {
                    _toRespawn.add(jid);
                }
            } else {
                _toShutdown.add(jid);
            }
        }
        for (Map.Entry<JID, XmppAgent> entry : _agents.entrySet()) {
            _toRespawn.add(entry.getKey());
        }
        for (JID jid : _toShutdown) {
            XmppUtils.closeQuietlySync(connections.remove(jid));
            log.info("XMPP connection closed: " + jid);
        }
        for (JID jid : _toRespawn) {
            this.respawn(_agentsBackup.get(jid));
        }
    }

    private void respawn(XmppAgent agent) {
        XMPPConnection conn = connections.get(agent.getJID());
        if (conn != null && conn.isConnected()) {
            XmppUtils.closeQuietly(conn);
        }
        ConnectionConfiguration cc = XmppUtils.getConnectionCfg(agent.getJID(), agent.getHost(), agent.getPort(), agent.isSecure());
        conn = null;
        try {
            final JID jid = agent.getJID();
            final int uid = agent.getUid();
            conn = new XMPPConnection(cc);
            conn.connect();
            conn.login(jid.toString(), agent.getPassword());
            Presence presence = new Presence(Presence.Type.available);
            conn.sendPacket(presence);
            conn.getRoster().setSubscriptionMode(SubscriptionMode.accept_all);
            conn.getChatManager().addChatListener(new ChatManagerListener() {

                @Override
                public void chatCreated(Chat chat, boolean locally) {
                    MessageListener ml = new MessageListener() {

                        @Override
                        public void processMessage(Chat chat, Message message) {
                            try {
                                String peer = chat.getParticipant();
                                JID from = new JID(peer);
                                String reply = handler.handle(uid, from, jid, message.getBody());
                                if (reply != null) {
                                    XmppUtils.sendSilently(reply, chat);
                                }
                            } catch (Exception e) {
                                log.warn(e.getMessage(), e);
                            }
                        }
                    };
                    chat.addMessageListener(ml);
                }
            });
            this.connections.put(jid, conn);
            log.info("XMPP connection respawned: " + jid);
        } catch (Exception e) {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
