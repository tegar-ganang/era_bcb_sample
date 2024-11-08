package net.jabbra.core;

import net.jabbra.core.account.JabbraAccount;
import net.jabbra.core.roster.*;
import net.jabbra.gui.JabbraChatTab;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.packet.Message;

public class JabbraConnection {

    private XMPPConnection connection;

    private JabbraAccount account;

    private JabbraRoster roster;

    public JabbraConnection(JabbraAccount account, JabbraRoster roster) throws XMPPException {
        this.account = account;
        this.roster = roster;
        ConnectionConfiguration config;
        if (!account.getService().equals("")) {
            config = new ConnectionConfiguration(account.getHost(), account.getPort(), account.getService());
        } else {
            config = new ConnectionConfiguration(account.getHost(), account.getPort());
        }
        connection = new XMPPConnection(config);
        JabbraConnectionContainer.addConnection(getJID(), this);
    }

    public JabbraAccount getAccount() {
        return account;
    }

    private void loadRosterFromServer() {
        for (RosterGroup group : connection.getRoster().getGroups()) {
            JabbraGroup jabbraGroup = new JabbraGroup(group.getName(), 0);
            roster.addGroup(jabbraGroup);
            for (RosterEntry entry : group.getEntries()) {
                JabbraContact jabbraContact = roster.findContact(entry.getUser());
                if (jabbraContact == null) {
                    jabbraContact = new JabbraContact(entry.getName(), entry.getUser(), null, 0);
                }
                jabbraGroup.addContact(jabbraContact);
            }
        }
    }

    public void connect() throws Exception {
        connection.connect();
        connection.login(account.getLogin(), account.getPassword());
        if (!roster.loadRoster()) {
            loadRosterFromServer();
            roster.saveRoster();
        }
        connection.addPacketListener(JabbraController.getInstance().getPacketListener(), new MessageTypeFilter(Message.Type.chat));
        connection.getRoster().addRosterListener(JabbraController.getInstance().getRosterListener());
    }

    public void disconnect() {
        JabbraConnectionContainer.removeConnection(getJID());
        connection.disconnect();
    }

    public void sendMessage(String to, String text) {
        Message message = new Message(to, Message.Type.chat);
        message.setBody(text);
        connection.sendPacket(message);
    }

    public String getJID() {
        return account.getJID();
    }

    public JabbraChatTab createChatTab(String JID, String chatName) {
        return new JabbraChatTab(this, JID, chatName);
    }

    public JabbraRoster getRoster() {
        return roster;
    }

    public String toString() {
        return getJID();
    }
}
