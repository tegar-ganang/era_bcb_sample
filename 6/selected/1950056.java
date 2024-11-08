package com.halotis.taskLauncher.im;

import java.util.Collection;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import com.halotis.taskLauncher.engine.MyPacketListener;

public class IMClient {

    private IMUser imUser;

    private static XMPPConnection conn1;

    private static IMClient client;

    private IMClient() throws Exception {
        imUser = new IMUser();
        try {
            conn1 = new GoogleTalkConnection();
            conn1.connect();
        } catch (Exception e) {
            throw new Exception("Failed to connect to server");
        }
        try {
            conn1.login(imUser.getUsername(), imUser.getPassword());
        } catch (Exception e) {
            throw new Exception("Failed to login as " + imUser.getUsername());
        }
        Roster roster = conn1.getRoster();
        Collection<RosterEntry> entries = roster.getEntries();
        for (RosterEntry entry : entries) {
            System.out.println(entry + " " + entry.getStatus());
        }
        try {
            PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
            PacketListener myListener = new MyPacketListener();
            conn1.addPacketListener(myListener, filter);
        } catch (Exception e) {
            throw new Exception("Failed to add packetListener");
        }
    }

    public static void sendMessage(String user, String message) throws Exception {
        if (client == null) getInstance();
        ChatManager chatmanager = conn1.getChatManager();
        Chat newChat = chatmanager.createChat(user + "@" + "gmail.com", new MessageListener() {

            public void processMessage(Chat chat, Message message) {
                ;
            }
        });
        newChat.sendMessage(message);
    }

    public static IMClient getInstance() throws Exception {
        if (client != null) return client;
        client = new IMClient();
        return client;
    }
}
