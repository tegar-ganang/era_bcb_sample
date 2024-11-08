package com.softntic.meetmemanager.data.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.OriginateAction;
import com.softntic.meetmemanager.data.bean.SNTMeetMeRoom;
import com.softntic.meetmemanager.data.bean.SNTMeetMeUser;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;

public class LiveConferenceDao {

    private static final String PROPERTY_USERID = "Conference user id";

    private static final String PROPERTY_CALLERID = "Caller Id";

    private static final String PROPERTY_CALLERNUM = "Caller Number";

    private static final String PROPERTY_CHANNEL = "Channel";

    private static final String PROPERTY_MUTE = "Mute";

    private static final String PROPERTY_DURATION = "Duration";

    private static String host = "sardior";

    private static int port = 5038;

    private static String user = "MeetMe";

    private static String pass = "meetme";

    private static ManagerConnection managerConnection;

    public enum Command {

        MUTE, UNMUTE, KICK, LOCK, UNLOCK
    }

    public static ManagerConnection getManagerConnection() {
        if (LiveConferenceDao.managerConnection == null) {
            ManagerConnectionFactory factory = new ManagerConnectionFactory(host, port, user, pass);
            LiveConferenceDao.managerConnection = factory.createManagerConnection();
        }
        return LiveConferenceDao.managerConnection;
    }

    public static Set<SNTMeetMeRoom> getSNTMeetMeRooms() {
        DefaultAsteriskServer asteriskServer = new DefaultAsteriskServer(LiveConferenceDao.getManagerConnection());
        List<String> roomsList = asteriskServer.executeCliCommand("meetme list concise");
        Set<SNTMeetMeRoom> sntRooms = new HashSet<SNTMeetMeRoom>();
        for (String line : roomsList) sntRooms.add(new SNTMeetMeRoom(line));
        return sntRooms;
    }

    public static SNTMeetMeRoom getSNTMeetMeRoom(String roomId) {
        SNTMeetMeRoom room = null;
        for (SNTMeetMeRoom rooms : getSNTMeetMeRooms()) if (roomId.equals(rooms.getRoomId())) room = rooms;
        return room;
    }

    public static Set<SNTMeetMeUser> getSNTMeetMeUsers(String roomId) {
        DefaultAsteriskServer asteriskServer = new DefaultAsteriskServer(LiveConferenceDao.getManagerConnection());
        Set<SNTMeetMeUser> sntUsers = new HashSet<SNTMeetMeUser>();
        List<String> usersList = asteriskServer.executeCliCommand("meetme list " + roomId + " concise");
        for (String userString : usersList) {
            sntUsers.add(new SNTMeetMeUser(roomId, userString));
        }
        return sntUsers;
    }

    public static SNTMeetMeUser getSNTMeetMeUser(String roomId, String userId) {
        SNTMeetMeUser userFound = null;
        for (SNTMeetMeUser user : getSNTMeetMeUsers(roomId)) {
            if (userId.equals(user.getUserId())) userFound = user;
        }
        return userFound;
    }

    public static String meetMeUserCommand(String roomNumber, String userId, Command com) {
        DefaultAsteriskServer asteriskServer = new DefaultAsteriskServer(LiveConferenceDao.getManagerConnection());
        StringBuffer returnString = new StringBuffer();
        List<String> stringList;
        switch(com) {
            case KICK:
                stringList = asteriskServer.executeCliCommand("meetme kick " + roomNumber + " " + userId);
                break;
            case MUTE:
                stringList = asteriskServer.executeCliCommand("meetme mute " + roomNumber + " " + userId);
                break;
            case UNMUTE:
                stringList = asteriskServer.executeCliCommand("meetme unmute " + roomNumber + " " + userId);
                break;
            case LOCK:
                stringList = asteriskServer.executeCliCommand("meetme lock " + roomNumber + " " + userId);
                break;
            case UNLOCK:
                stringList = asteriskServer.executeCliCommand("meetme unlock " + roomNumber + " " + userId);
                break;
            default:
                stringList = new ArrayList<String>();
                stringList.add("No action defined");
                break;
        }
        for (String str : stringList) returnString.append(str + "\n");
        return returnString.toString();
    }

    public static void joinMeetMeConference(String externalNumber, String roomId, String callerId) {
        OriginateAction oa = new OriginateAction();
        oa.setAccount("9990");
        oa.setCallerId(callerId);
        oa.setChannel("SIP/" + externalNumber + "@pstn_9990");
        oa.setApplication("MeetMe");
        oa.setData(roomId + ",dMI");
        try {
            getManagerConnection().sendAction(oa);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public static IndexedContainer getSNTMeetMeUsersContainer(Set<SNTMeetMeUser> users) {
        IndexedContainer c = new IndexedContainer();
        fillUsersContainer(c, users);
        return c;
    }

    private static void fillUsersContainer(IndexedContainer container, Set<SNTMeetMeUser> users) {
        container.addContainerProperty(PROPERTY_USERID, String.class, null);
        container.addContainerProperty(PROPERTY_CALLERNUM, String.class, null);
        container.addContainerProperty(PROPERTY_CALLERID, String.class, null);
        container.addContainerProperty(PROPERTY_CHANNEL, String.class, null);
        container.addContainerProperty(PROPERTY_MUTE, Boolean.class, null);
        container.addContainerProperty(PROPERTY_DURATION, String.class, null);
        for (SNTMeetMeUser user : users) {
            Item item = container.addItem(user.getUserId());
            item.getItemProperty(PROPERTY_USERID).setValue(user.getUserId());
            item.getItemProperty(PROPERTY_CALLERNUM).setValue(user.getCallerNum());
            item.getItemProperty(PROPERTY_CALLERID).setValue(user.getCallerId());
            item.getItemProperty(PROPERTY_CHANNEL).setValue(user.getChannel());
            item.getItemProperty(PROPERTY_MUTE).setValue(user.isMute());
            item.getItemProperty(PROPERTY_DURATION).setValue(user.getDate());
        }
        container.sort(new Object[] { PROPERTY_CALLERID }, new boolean[] { true });
    }

    public static String[] getSNTMeetMeUsersHeader() {
        return new String[] { PROPERTY_USERID, PROPERTY_CALLERNUM, PROPERTY_CALLERID, PROPERTY_CHANNEL, PROPERTY_MUTE, PROPERTY_DURATION };
    }
}
