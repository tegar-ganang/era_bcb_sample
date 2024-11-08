package jerklib.examples;

import jerklib.ConnectionManager;
import jerklib.ProfileImpl;
import jerklib.Session;
import jerklib.events.ConnectionCompleteEvent;
import jerklib.events.JoinCompleteEvent;
import jerklib.events.MessageEvent;
import jerklib.events.listeners.BaseListener;

public class BaseListenerExample extends BaseListener implements Runnable {

    public BaseListenerExample() {
    }

    Session session;

    public void run() {
        ConnectionManager manager = new ConnectionManager(new ProfileImpl("ble", "ble", "ble_", "ble__"));
        session = manager.requestConnection("irc.freenode.net");
        session.addIRCEventListener(this);
    }

    @Override
    protected void handleJoinCompleteEvent(JoinCompleteEvent event) {
        event.getChannel().say("Hello from BaseListenerExample");
    }

    @Override
    protected void handleConnectComplete(ConnectionCompleteEvent event) {
        event.getSession().join("#jerklib");
    }

    @Override
    protected void handleChannelMessage(MessageEvent event) {
        log.info(event.getChannel().getName() + ":" + event.getNick() + ":" + event.getMessage());
        if ("now die".equalsIgnoreCase(event.getMessage())) {
            event.getChannel().say("Okay, fine, I'll die");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        BaseListenerExample ble = new BaseListenerExample();
        Thread t = new Thread(ble);
        t.start();
        try {
            Thread.sleep(30000L);
        } catch (InterruptedException e) {
        }
        ble.sayGoodbye();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
        System.exit(0);
    }

    private void sayGoodbye() {
        for (String channel : session.getChannelNames()) {
            session.sayChannel(channel, "I'm melting! (built-in sword of Damocles... or bucket of water, whatever)");
        }
    }
}
