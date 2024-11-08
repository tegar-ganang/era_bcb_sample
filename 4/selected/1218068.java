package jerklib.examples;

import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.Session;
import jerklib.events.*;
import jerklib.events.IRCEvent.Type;
import jerklib.listeners.IRCEventListener;

/**
 *  A simple example that demonsrates how to use JerkLib
 *  @author mohadib 
 */
public class Example implements IRCEventListener {

    private ConnectionManager manager;

    public Example() {
        manager = new ConnectionManager(new Profile("scripy"));
        Session session = manager.requestConnection("irc.freenode.net");
        session.addIRCEventListener(this);
    }

    public void receiveEvent(IRCEvent e) {
        if (e.getType() == Type.CONNECT_COMPLETE) {
            e.getSession().join("#jerklib");
        } else if (e.getType() == Type.CHANNEL_MESSAGE) {
            MessageEvent me = (MessageEvent) e;
            System.out.println(me.getNick() + ":" + me.getMessage());
            me.getChannel().say("Modes :" + me.getChannel().getUsersModes(me.getNick()).toString());
        } else if (e.getType() == Type.JOIN_COMPLETE) {
            JoinCompleteEvent jce = (JoinCompleteEvent) e;
            jce.getChannel().say("Hello from Jerklib!");
        } else {
            System.out.println(e.getType() + " " + e.getRawEventData());
        }
    }

    public static void main(String[] args) {
        new Example();
    }
}
