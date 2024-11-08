package com.peterhi.client.managers;

import java.net.PasswordAuthentication;
import java.util.Set;
import com.peterhi.beans.ChannelBean;
import com.peterhi.beans.TalkState;
import com.peterhi.client.Manager;
import com.peterhi.client.voice.Mic;
import com.peterhi.client.voice.Speaker;
import java.util.HashSet;

/**
 * Manages the setting and retrieval of global objects. Those objects usually
 * reflects the state of the application.
 * 
 * @author YUN TAO HAI
 * 
 */
public class StoreManager implements Manager {

    private Integer id;

    private ChannelBean channel;

    private PasswordAuthentication auth;

    private boolean voiceEnabled = true;

    private boolean voicePassed;

    private TalkState talkState;

    private boolean textEnabled = true;

    private Mic mic;

    private Speaker speaker;

    private Set<StoreListener> ls = new HashSet<StoreListener>();

    public StoreManager() {
        try {
            mic = new Mic();
            speaker = new Speaker();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean isTextEnabled() {
        return textEnabled;
    }

    public void setTextEnabled(boolean textEnabled) {
        this.textEnabled = textEnabled;
        fireOnTextEnabled(new StoreEvent(this));
    }

    public Speaker getSpeaker() {
        return speaker;
    }

    public Mic getMic() {
        return mic;
    }

    public boolean isVoiceEnabled() {
        return voiceEnabled;
    }

    public void setVoiceEnabled(boolean voiceEnabled) {
        this.voiceEnabled = voiceEnabled;
        fireOnVoiceEnabled(new StoreEvent(this));
    }

    public TalkState getTalkState() {
        return talkState;
    }

    public void setTalkState(TalkState talkState) {
        this.talkState = talkState;
        fireOnTalking(new StoreEvent(this));
    }

    public void addStoreListener(StoreListener l) {
        if (l == null) throw new NullPointerException();
        ls.add(l);
    }

    public void removeStoreListener(StoreListener l) {
        if (l == null) throw new NullPointerException();
        ls.remove(l);
    }

    public boolean isVoicePassed() {
        return voicePassed;
    }

    public synchronized void setVoicePassed(boolean voicePassed) {
        this.voicePassed = voicePassed;
        fireOnVoiceTestFinished(new StoreEvent(this));
    }

    private void fireOnVoiceTestFinished(StoreEvent e) {
        for (StoreListener l : ls) l.onVoiceTest(e);
    }

    private void fireOnVoiceEnabled(StoreEvent e) {
        for (StoreListener l : ls) l.onVoiceEnabled(e);
    }

    private void fireOnTextEnabled(StoreEvent e) {
        for (StoreListener l : ls) l.onTextEnabled(e);
    }

    private void fireOnTalking(StoreEvent e) {
        for (StoreListener l : ls) l.onTalking(e);
    }

    /**
	 * Gets the current {@link ChannelBean}.
	 * 
	 * @return The {@link ChannelBean}, or <c>null</c> if not set. <c>null</c>
	 *         can indicate that the user is not present in any channel.
	 */
    public ChannelBean getChannel() {
        return channel;
    }

    /**
	 * Sets the current {@link ChannelBean}.
	 * 
	 * @param channel
	 *            The {@link ChannelBean}.
	 */
    public synchronized void setChannel(ChannelBean channel) {
        this.channel = channel;
    }

    /**
	 * Gets the user credential as a {@link PasswordAuthentication} object.
	 * 
	 * @return The user credential, or <c>null</c> if not set. <c>null</c> can
	 *         indicate that the user is not logged in.
	 */
    public PasswordAuthentication getAuth() {
        return auth;
    }

    /**
	 * Sets the user credential by passing in a {@link PasswordAuthentication}.
	 * 
	 * @param auth
	 *            The user credential, as a {@link PasswordAuthentication}.
	 */
    public synchronized void setAuth(PasswordAuthentication auth) {
        this.auth = auth;
    }

    /**
	 * Gets the unique user ID given by the server.
	 * 
	 * @return The unique user ID, or <c>null</c> if not set. <c>null</c> can
	 *         indicate that the user is not logged in.
	 */
    public Integer getID() {
        return id;
    }

    /**
	 * Sets the unique user ID. This method is called automatically by the
	 * server and should not be used by the developer.
	 * 
	 * @param id
	 *            The unique user ID.
	 */
    public synchronized void setID(Integer id) {
        this.id = id;
    }

    /**
	 * Shorthand for {@link PasswordAuthentication#getUserName()}.
	 * 
	 * @return The user name.
	 */
    public String getUserName() {
        if (auth == null) return null;
        return auth.getUserName();
    }

    public void onConfigure() {
    }
}
