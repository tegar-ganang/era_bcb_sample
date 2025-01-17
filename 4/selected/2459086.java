package org.jmik.asterisk.hello;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.AsteriskQueue;
import org.asteriskjava.live.AsteriskServer;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.ManagerCommunicationException;
import org.asteriskjava.live.MeetMeRoom;

public class HelloLive {

    private AsteriskServer asteriskServer;

    public HelloLive() {
        asteriskServer = new DefaultAsteriskServer("localhost", "mark", "mysecret");
    }

    public void run() throws ManagerCommunicationException {
        for (AsteriskChannel asteriskChannel : asteriskServer.getChannels()) {
            System.out.println(asteriskChannel);
        }
        for (AsteriskQueue asteriskQueue : asteriskServer.getQueues()) {
            System.out.println(asteriskQueue);
        }
        for (MeetMeRoom meetMeRoom : asteriskServer.getMeetMeRooms()) {
            System.out.println(meetMeRoom);
        }
    }

    public static void main(String[] args) throws Exception {
        HelloLive helloLive = new HelloLive();
        helloLive.run();
    }
}
