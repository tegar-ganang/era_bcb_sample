package org.foxtalkz.google;

import java.util.HashMap;
import org.foxtalkz.Main;
import org.foxtalkz.Services;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;

/**
 *
 * @author Tharindu
 */
public class GoogleAgent extends Thread implements PacketListener {

    private static ConnectionConfiguration connConfig;

    private static XMPPConnection connection;

    public static HashMap<String, GoogleGroup> googleLANGroupList;

    public static HashMap<Integer, String> interfaceToGooglePrivateChatList;

    public static HashMap<String, Integer> googleToInterfaceList;

    public static HashMap<String, GoogleUser> googleUserList;

    private static String myUserName;

    public static Services services;

    private static final Object lock = new Object();

    private static int lastPrivateChatID = 500;

    public GoogleAgent(Services services) throws XMPPException {
        GoogleAgent.services = services;
        googleLANGroupList = new HashMap<String, GoogleGroup>();
        googleUserList = new HashMap<String, GoogleUser>();
        interfaceToGooglePrivateChatList = new HashMap<Integer, String>();
        googleToInterfaceList = new HashMap<String, Integer>();
    }

    public void createGoogleGroup(String groupID, boolean isGooglePrivateChat) {
        GoogleGroup tempGroup = new GoogleGroup(myUserName, groupID, isGooglePrivateChat);
        googleLANGroupList.put(groupID, tempGroup);
    }

    public void addUserToGroup(String groupID, String username, String displayName, Presence userPresence) {
        GoogleGroup tempGroup = googleLANGroupList.remove(groupID);
        tempGroup.addUser(username);
        googleLANGroupList.put(groupID, tempGroup);
    }

    public void login(String user, String passwd) throws XMPPException {
        myUserName = user;
        if (!myUserName.contains("@gmail.com")) {
            myUserName += "@gmail.com";
        }
        connConfig = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
        connection = new XMPPConnection(connConfig);
        connection.connect();
        connection.login(myUserName, passwd);
        PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
        connection.addPacketListener((PacketListener) this, filter);
        this.start();
    }

    public void removeUserFromGroup(String groupID, String userName) {
        GoogleGroup tempGroup = googleLANGroupList.remove(groupID);
        tempGroup.removeUser(userName);
        googleLANGroupList.put(groupID, tempGroup);
    }

    public void changeUserPresence(String groupID, String userName, Presence userPresence) {
    }

    Presence getPresence(String user) {
        return connection.getRoster().getPresence(user);
    }

    public void processPacket(Packet packet) {
        Message message = (Message) packet;
        String userFrom = message.getFrom().split("/")[0];
        String authCode = message.getFrom().split("/")[1];
        if (message.getBody() != null) {
            System.out.println("Google Message received from " + userFrom + " message: " + message.getBody());
            if (authCode.contains("Smack")) {
                GoogleUser gu = googleUserList.remove(userFrom);
                gu.setIsFoxTalkzUser(true);
                googleUserList.put(userFrom, gu);
                if (googleToInterfaceList.containsKey(userFrom)) {
                    receivePrivateMessage(googleToInterfaceList.get(userFrom).toString(), message.getBody());
                } else {
                    createGoogleGroup(userFrom, true);
                    addUserToGroup(userFrom, userFrom, userFrom.split("@")[0], null);
                }
            } else {
                if (googleToInterfaceList.containsKey(userFrom)) {
                    receivePrivateMessage(googleToInterfaceList.get(userFrom).toString(), message.getBody());
                } else {
                    googleToInterfaceList.put(userFrom, (++lastPrivateChatID));
                    interfaceToGooglePrivateChatList.put(lastPrivateChatID, userFrom);
                    Main.write(Main.CREATE_GROUP_SUCCESS + "" + Services.lastGroupIndex + Main.DELIMITER_LEVEL_1 + userFrom + Main.DELIMITER_LEVEL_1 + "1");
                    receivePrivateMessage(lastPrivateChatID + "", message.getBody());
                }
            }
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void sendMessage(String groupID, String message) {
        for (String googleUser : googleLANGroupList.get(groupID).getGoogleUserList()) {
            GoogleUser gu = googleUserList.get(googleUser);
            Message msg = new Message(gu.getUserName(), Message.Type.chat);
            msg.setBody(message);
            connection.sendPacket((Packet) msg);
        }
    }

    public void disconnect() {
        connection.disconnect();
    }

    public void sendPresence(int status) {
        Presence presence = new Presence(Presence.Type.available);
        presence.setMode(Presence.Mode.available);
        connection.sendPacket(presence);
    }

    public void recvRoster() {
        Roster roster = connection.getRoster();
        for (RosterEntry rosterEntry : roster.getEntries()) {
            Presence presence = roster.getPresence(rosterEntry.getUser());
            String name = "";
            if (rosterEntry.getName() == null) {
                name = rosterEntry.getUser().split("@")[0];
            } else {
                name = rosterEntry.getName();
            }
            if (!googleUserList.containsKey(rosterEntry.getUser())) {
                GoogleUser gu = new GoogleUser(rosterEntry.getUser(), name, presence, false);
                googleUserList.put(gu.getUserName(), gu);
                Services.googleUserTointerfaceMap.put(gu.getUserName(), (++Services.lastGoogleUserIndex));
                Services.interfaceToGoogleUserMap.put(Services.lastGoogleUserIndex, gu.getUserName());
                System.out.println("NEW GOOGLE ROSTER : " + rosterEntry.getUser() + " == " + name);
            } else {
                GoogleUser gu = googleUserList.remove(rosterEntry.getUser());
                gu.setUserPresence(presence);
                googleUserList.put(rosterEntry.getUser(), gu);
                System.out.println("OLD GOOGLE ROSTER : " + rosterEntry.getUser() + " == " + name);
            }
        }
    }

    public boolean isGoogleGroup(String groupID) {
        return googleLANGroupList.containsKey(groupID);
    }

    public boolean isGooglePrivateChat(String groupID) {
        if (isGoogleGroup(groupID)) {
            return isGooglePrivateChat(groupID);
        }
        return false;
    }

    public GoogleGroup getGoogleGroup(String grpID) {
        return googleLANGroupList.get(grpID);
    }

    private GoogleGroup getGroupFromUser(String userFrom) {
        return null;
    }

    private void receivePrivateMessage(String grpID, String message) {
        if (googleLANGroupList.containsKey(grpID)) {
            services.getGroupManager().getGroup(Services.interfaceToGroupMap.get(grpID)).sendMessage(message);
        } else {
            Main.write(Main.GROUP_MSG_DISPLAY + grpID + Main.DELIMITER_LEVEL_1 + interfaceToGooglePrivateChatList.get(Integer.parseInt(grpID)) + Main.DELIMITER_LEVEL_1 + message);
        }
    }
}
