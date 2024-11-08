package com.techno.chatpipe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.proxy.ProxyInfo;

public class JabberIM implements MessageListener {

    XMPPConnection connection;

    public ConnectionConfiguration getProxyForXMPP(boolean useProxy) {
        String proxyAddress = "genproxy";
        int proxyPort = 8080;
        if (useProxy) return new ConnectionConfiguration("talk.google.com", 5222, "gmail.com", ProxyInfo.forHttpProxy(proxyAddress, proxyPort, "", "")); else return new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
    }

    public void login(String userName, String password) throws XMPPException {
        ConnectionConfiguration config = getProxyForXMPP(false);
        config.setCompressionEnabled(true);
        config.setSASLAuthenticationEnabled(true);
        SASLAuthentication.supportSASLMechanism("PLAIN", 0);
        connection = new XMPPConnection(config);
        connection.connect();
        connection.login(userName, password);
    }

    public void sendMessage(String message, String to) throws XMPPException {
        Chat chat = connection.getChatManager().createChat(to, this);
        chat.sendMessage(message);
    }

    public void displayBuddyList() {
        Roster roster = connection.getRoster();
        Collection<RosterEntry> entries = roster.getEntries();
        System.out.println("\n\n" + entries.size() + " buddy(ies):");
        for (RosterEntry r : entries) {
            System.out.println(r.getUser());
        }
    }

    public void disconnect() {
        connection.disconnect();
    }

    public void processMessage(Chat chat, Message message) {
        if (message.getType() == Message.Type.chat) System.out.println(chat.getParticipant() + " says: " + message.getBody());
    }

    public static void main(String args[]) throws XMPPException, IOException {
        JabberIM c = new JabberIM();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String msg;
        c.login("enigma.united@gmail.com", "ybrations");
        c.displayBuddyList();
        System.out.println("-----");
        System.out.println("Who do you want to talk to? - Type contacts full email address:");
        String talkTo = br.readLine();
        System.out.println("-----");
        System.out.println("All messages will be sent to " + talkTo);
        System.out.println("Enter your message in the console:");
        System.out.println("-----\n");
        while (!(msg = br.readLine()).equals("bye")) {
            c.sendMessage(msg, talkTo);
        }
        c.disconnect();
        System.exit(0);
    }
}
