package ajim;

import java.util.*;
import java.io.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;

/**
 *
 * @author Kris
 */
public class Main implements MessageListener {

    XMPPConnection connection;

    public void login(String userName, String password) throws XMPPException {
        ConnectionConfiguration config = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
        connection = new XMPPConnection(config);
        connection.connect();
        System.out.println("ez meg megy 1");
        SASLAuthentication.supportSASLMechanism("PLAIN", 0);
        connection.login(userName, password, "proba");
        System.out.println(connection.isAuthenticated());
    }

    public void sendMessage(String message, String to) throws XMPPException {
        Chat chat = connection.getChatManager().createChat(to, this);
        chat.sendMessage(message);
    }

    public void displayBuddyList() {
        Roster roster = connection.getRoster();
        Collection<RosterEntry> entries = roster.getEntries();
        System.out.println("\n\n" + entries.size() + " Buddylist:");
        for (RosterEntry r : entries) {
            if (roster.getPresence(r.getUser()).toString().equals("unavailable")) {
            } else System.out.println(r.getUser());
        }
    }

    public void disconnect() {
        connection.disconnect();
    }

    public void processMessage(Chat chat, Message message) {
        if (message.getType() == Message.Type.chat) {
            System.out.println(chat.getParticipant() + " : " + message.getBody());
        }
    }

    public static void main(String args[]) throws XMPPException, IOException {
        Main c = new Main();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String msg;
        XMPPConnection.DEBUG_ENABLED = false;
        c.login("xxxxxxxx@gmail.com", "xxxxxxxx");
        System.out.println("Kivel akarsz chatelni? (ird be hogy bl):");
        String talkTo = br.readLine();
        if (talkTo.equals("bl")) {
            c.displayBuddyList();
            talkTo = br.readLine();
        }
        System.out.println();
        System.out.println("Irhatod az uzeneteket:");
        System.out.println();
        while (!(msg = br.readLine()).equals("exit")) {
            c.sendMessage(msg, talkTo);
        }
        c.disconnect();
        System.exit(0);
    }
}
