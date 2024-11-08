package com.sshtools.tunnel;

import javax.swing.Timer;
import com.sshtools.j2ssh.forwarding.ForwardingChannel;

public class ActiveChannelWrapper {

    private boolean sending;

    private boolean receiving;

    private ForwardingChannel channel;

    private javax.swing.Timer sentTimer;

    private javax.swing.Timer receivedTimer;

    public ActiveChannelWrapper(ForwardingChannel channel) {
        this.channel = channel;
    }

    public void setSending(boolean sending) {
        this.sending = sending;
    }

    public void setReceiving(boolean receiving) {
        this.receiving = receiving;
    }

    public void setSentTimer(Timer timer) {
        sentTimer = timer;
    }

    public void setReceivedTimer(Timer timer) {
        receivedTimer = timer;
    }

    public boolean isSending() {
        return sending;
    }

    public boolean isReceiving() {
        return receiving;
    }

    public Timer getSentTimer() {
        return sentTimer;
    }

    public Timer getReceivedTimer() {
        return receivedTimer;
    }

    public ForwardingChannel getChannel() {
        return channel;
    }
}
