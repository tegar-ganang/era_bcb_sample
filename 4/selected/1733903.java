package org.jdamico.ircivelaclient.listener;

import java.util.ArrayList;
import java.util.Observer;
import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.Session;
import jerklib.events.ConnectionCompleteEvent;
import jerklib.events.JoinCompleteEvent;
import jerklib.events.JoinEvent;
import jerklib.events.MessageEvent;
import jerklib.events.QuitEvent;
import jerklib.listeners.DefaultIRCEventListener;
import org.jdamico.ircivelaclient.observer.IRCObservable;
import org.jdamico.ircivelaclient.view.HandleApplet;
import org.jdamico.ircivelaclient.view.StaticData;

public class ListenConversation extends DefaultIRCEventListener implements Runnable, IRCObservable {

    /**
	 * @param args
	 */
    private MessageEvent jce = null;

    private HandleApplet parent = null;

    private ArrayList<Observer> observerList;

    public Session session;

    static final String CHANNEL_TO_JOIN = StaticData.channel;

    public ListenConversation(HandleApplet parent) {
        this.parent = parent;
        this.observerList = new ArrayList<Observer>();
    }

    public void run() {
        ConnectionManager manager = new ConnectionManager(new Profile(StaticData.nick, StaticData.nick, StaticData.nick, StaticData.nick));
        session = manager.requestConnection(StaticData.server);
        session.addIRCEventListener(this);
        setSession(session);
        System.out.println("session: " + session);
    }

    @Override
    protected void handleConnectComplete(ConnectionCompleteEvent event) {
        event.getSession().join(CHANNEL_TO_JOIN);
        System.out.println("conectado");
        this.notifyObservers(event);
    }

    protected void handleJoinCompleteEvent(JoinCompleteEvent event) {
        event.getChannel().say("Connected");
        StaticData.nick = session.getNick();
        this.parent.getChatPanel().setSession(session);
        notifyObservers(event);
    }

    @Override
    protected void handleChannelMessage(MessageEvent event) {
        notifyObservers(event);
        this.setJce(event);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    protected void handlePrivateMessage(MessageEvent event) {
        this.notifyObservers(event);
        this.setJce(event);
    }

    @Override
    protected void handleJoinEvent(JoinEvent event) {
        this.notifyObservers(event);
    }

    @Override
    protected void handleQuitEvent(QuitEvent event) {
        this.notifyObservers(event);
    }

    public void addObserver(Observer observer) {
        this.observerList.add(observer);
    }

    public void notifyObservers(Object arg) {
        for (Observer observer : this.observerList) {
            observer.update(null, arg);
        }
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public MessageEvent getJce() {
        return jce;
    }

    public void setJce(MessageEvent jce) {
        this.jce = jce;
    }
}
