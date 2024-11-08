package crony.services.xmpp.facebook;

import crony.services.xmpp.facebook.mechanisms.FBSASLDigestMD5Mechanism;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.jivesoftware.smack.Chat;
import java.lang.String;
import java.util.Collection;
import java.util.Scanner;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;

;

/**
 *
 * @author Abhishek
 */
public class FBConnectMD5 implements Runnable {

    XMPPConnection connection;

    private String username = new String();

    int i;

    private String password = new String();

    String onlineUsers[] = new String[50];

    public FBConnectMD5(String un, String pw) {
        username = un;
        password = pw;
    }

    @Override
    public void run() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String msg;
            Scanner input = new Scanner(System.in);
            XMPPConnection.DEBUG_ENABLED = true;
            login();
            Thread.sleep(2000);
            processBuddyList();
            System.out.println("-----");
            String talkTo = br.readLine();
            System.out.println("-----");
            System.out.println("All messages will be sent to " + talkTo);
            System.out.println("Enter your message in the console:");
            System.out.println("-----\n");
        } catch (Exception ex) {
        }
        ;
    }

    public void login() {
        Presence presence = new Presence(Presence.Type.available);
        presence.setMode(Presence.Mode.available);
        presence.setStatus("Gone fishing");
        try {
            SASLAuthentication.registerSASLMechanism("DIGEST-MD5", FBSASLDigestMD5Mechanism.class);
            ConnectionConfiguration config = new ConnectionConfiguration("chat.facebook.com", 5222);
            config.setRosterLoadedAtLogin(true);
            config.setDebuggerEnabled(true);
            config.setSASLAuthenticationEnabled(true);
            connection = new XMPPConnection(config);
            connection.connect();
            connection.login(username, password, "cronyim");
            connection.sendPacket(presence);
        } catch (XMPPException ex) {
            System.out.println(ex.getMessage());
            disconnect();
        }
    }

    public void processBuddyList() {
        Roster roster = connection.getRoster();
        try {
            Thread.sleep(3000);
        } catch (Exception ex) {
        }
        ;
        Collection<RosterEntry> entries = roster.getEntries();
        System.out.println("Connected!");
        System.out.println("\n\n" + entries.size() + " buddy(ies):");
        String temp[] = new String[50];
        int i = 0;
        for (RosterEntry entry : entries) {
            String user = entry.getUser();
            Presence bestPresence = roster.getPresence(user);
            if (bestPresence.getType() == Presence.Type.available) {
                temp[i] = entry.getName() + "," + bestPresence.getFrom();
                i++;
            }
        }
        copyBuddyList(temp);
        showBuddyList();
        roster.addRosterListener(new RosterListener() {

            public void entriesAdded(Collection<String> addresses) {
            }

            public void entriesDeleted(Collection<String> addresses) {
            }

            public void entriesUpdated(Collection<String> addresses) {
            }

            public void presenceChanged(Presence presence) {
                Roster tempRoster = connection.getRoster();
                Collection<RosterEntry> entries = tempRoster.getEntries();
                String temp[] = new String[50];
                int i = 0;
                for (RosterEntry entry : entries) {
                    String user = entry.getUser();
                    Presence bestPresence = tempRoster.getPresence(user);
                    if (bestPresence.getType() == Presence.Type.available) {
                        temp[i] = entry.getName() + "," + bestPresence.getFrom();
                        i++;
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (Exception ex) {
                }
                ;
                System.out.print("------\n");
                System.out.println("ROSTER UPDATED\n ");
                copyBuddyList(temp);
                showBuddyList();
            }
        });
    }

    public void copyBuddyList(String a[]) {
        for (int i = 0; i < a.length; i++) onlineUsers[i] = a[i];
    }

    public void showBuddyList() {
        System.out.println("\nINSIDE FACEBOOK\n");
        for (int i = 0; onlineUsers[i] != null; i++) System.out.println(onlineUsers[i]);
    }

    public void disconnect() {
        connection.disconnect();
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.println("Connecting...");
        FBConnectMD5 myFB = new FBConnectMD5("sharma.anusha", "w3rt98hkaditi.");
        Thread mythread = new Thread(myFB);
        mythread.start();
    }
}
