package org.bigbluebutton.pbx;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.AsteriskQueue;
import org.asteriskjava.live.AsteriskServer;
import org.asteriskjava.live.AsteriskServerListener;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.ManagerCommunicationException;
import org.asteriskjava.live.MeetMeRoom;
import org.asteriskjava.live.MeetMeUser;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class PbxLive implements AsteriskServerListener, PropertyChangeListener {

    private AsteriskServer asteriskServer;

    public PbxLive() {
    }

    public void setAsteriskServer(AsteriskServer server) {
        asteriskServer = server;
    }

    public void startup() throws ManagerCommunicationException {
        asteriskServer.addAsteriskServerListener(this);
        for (AsteriskChannel asteriskChannel : asteriskServer.getChannels()) {
        }
        for (AsteriskQueue asteriskQueue : asteriskServer.getQueues()) {
            for (AsteriskChannel asteriskChannel : asteriskQueue.getEntries()) {
            }
        }
        for (MeetMeRoom meetMeRoom : asteriskServer.getMeetMeRooms()) {
            System.out.println(meetMeRoom);
            for (MeetMeUser user : meetMeRoom.getUsers()) {
                user.addPropertyChangeListener(this);
            }
        }
    }

    public void onNewAsteriskChannel(AsteriskChannel channel) {
    }

    public void onNewMeetMeUser(MeetMeUser user) {
        System.out.println(user);
        user.addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
        if (propertyChangeEvent.getSource() instanceof MeetMeUser) {
            MeetMeUser user = (MeetMeUser) propertyChangeEvent.getSource();
            System.out.println(user.getChannel().getCallerId().getName() + " " + user.getChannel().getCallerId().getNumber());
        }
    }

    public void shutdown() {
        asteriskServer.shutdown();
    }
}
