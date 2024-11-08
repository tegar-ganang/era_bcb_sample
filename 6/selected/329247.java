package org.localstorm.punjab;

import java.io.IOException;
import java.util.Collection;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

/**
 *
 * @author Alexey Kuznetsov
 */
public class PJBService {

    private XmppHandler handler;

    private ConnectionConfiguration config;

    private XMPPConnection conn;

    private boolean anonAllowed;

    public PJBService(ConnectionConfiguration cc, XmppHandler handler, Collection<JID> authenticatedJids, boolean anonymous) {
        if (anonymous) {
            this.handler = handler;
        } else {
            this.handler = new SecurityXmppFilter(handler, authenticatedJids);
        }
        this.config = cc;
        this.anonAllowed = anonymous;
    }

    public void start(JID jid, String password) throws IOException {
        try {
            this.conn = new XMPPConnection(this.config);
            this.conn.connect();
            this.conn.login(jid.toString(), password);
            System.out.println("Logged in as " + jid.toString());
            Presence presence = new Presence(Presence.Type.available);
            this.conn.sendPacket(presence);
            this.conn.getChatManager().addChatListener(new ChatManagerListener() {

                public void chatCreated(Chat chat, boolean locally) {
                    String peer = chat.getParticipant();
                    if (!PJBService.this.handler.isAllowed(peer)) {
                        String reply = PJBService.this.handler.getDenialMessage();
                        XmppUtils.sendSilently(reply, chat);
                    } else {
                        MessageListener ml = new MessageListener() {

                            public void processMessage(Chat chat, Message message) {
                                try {
                                    String peer = chat.getParticipant();
                                    String reply = PJBService.this.handler.onMessage(message.getBody(), peer);
                                    XmppUtils.sendSilently(reply, chat);
                                } catch (Exception e) {
                                    System.err.println("Error: " + e.getMessage());
                                }
                                chat.removeMessageListener(this);
                            }
                        };
                        chat.addMessageListener(ml);
                    }
                }
            });
        } catch (XMPPException e) {
            throw new IOException(e);
        }
    }

    public void join() throws InterruptedException {
        while (this.conn.isConnected()) {
            Thread.sleep(5000);
        }
    }

    public void stop() {
        System.out.println("Stopping...");
        Presence presence = new Presence(Presence.Type.unsubscribed);
        this.conn.disconnect(presence);
    }

    public void sendMessage(JID to, String message, String subj, boolean ignoreOffline) throws IOException {
        if (!ignoreOffline) {
            Roster roster = this.conn.getRoster();
            Presence presence = roster.getPresence(to.toString());
            if (!presence.isAvailable()) {
                throw new IOException(to + " contact is offline.");
            }
        }
        Message msg = new Message();
        msg.setTo(to.toString());
        msg.setBody(message);
        msg.setSubject(subj);
        msg.setType(Message.Type.chat);
        this.conn.sendPacket(msg);
    }
}
