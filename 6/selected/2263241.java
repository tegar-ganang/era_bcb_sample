package ru.xalba.sportbot;

import ru.xalba.sportbot.User;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.filter.*;

public class SimpleJabberClient {

    public XMPPConnection connection;

    public boolean login(String loginJID, String password) {
        boolean result = true;
        ConnectionConfiguration config = new ConnectionConfiguration("jabber.ru", 5222);
        try {
            connection = new XMPPConnection(config);
            connection.connect();
            connection.login("kreab0t", password);
        } catch (XMPPException exc) {
            System.out.println("Connection Error: " + exc.toString());
            result = false;
        } catch (Exception exc) {
            System.out.println("fail: " + exc.toString());
        }
        return result;
    }

    public boolean sendMessage(String contact, String message) {
        ChatManager chatmanager = connection.getChatManager();
        Chat newChat = chatmanager.createChat("xalba@jabber.ru", new MessageListener() {

            public void processMessage(Chat chat, Message message) {
                System.out.println("Received message: " + message);
            }
        });
        try {
            newChat.sendMessage("Howdy!");
        } catch (XMPPException e) {
            System.out.println("Error Delivering block");
        }
        return true;
    }

    public void makeDialog() {
        User myUser = new User();
        ChatManager chatManager = connection.getChatManager();
        myUser.setJid("xalba@jabber.ru");
        myUser.setFilter(new OrFilter(new PacketTypeFilter(Message.class), new FromContainsFilter(myUser.getJid())));
        final PacketCollector collector = connection.createPacketCollector(myUser.getFilter());
        PacketListener listener = new PacketListener() {

            public void processPacket(Packet packet) {
                System.out.println("hello!");
            }
        };
        connection.addPacketListener(listener, myUser.getFilter());
        Packet currentPacket;
        boolean exit = false;
        while (!exit) {
            currentPacket = collector.nextResult();
            Message message = (Message) currentPacket;
            String exitString = message.getBody();
            if (exitString.equalsIgnoreCase("!exit")) {
                exit = true;
            }
        }
    }
}
