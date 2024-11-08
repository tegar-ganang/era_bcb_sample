package logicswarm.net.irc.mods;

import logicswarm.net.irc.IrcBot;
import logicswarm.net.irc.IrcModule;

public class AutoReconnect extends IrcModule {

    public static final long serialVersionUID = 1;

    private int timeToSleep;

    private boolean verbose;

    private int max_attempts = 3;

    private int attempts;

    public AutoReconnect(int temp, IrcBot owner) {
        super(owner);
        initialize("AutoReconnect");
        timeToSleep = temp * 1000;
    }

    public AutoReconnect(int temp, int maxAttempts, IrcBot owner) {
        super(owner);
        initialize("AutoReconnect");
        timeToSleep = temp * 1000;
        max_attempts = maxAttempts;
    }

    public AutoReconnect(int temp, boolean verb, IrcBot owner) {
        super(owner);
        initialize("AutoReconnect");
        timeToSleep = temp * 1000;
        verbose = verb;
    }

    public AutoReconnect(int temp, int maxAttempts, boolean verb, IrcBot owner) {
        super(owner);
        initialize("AutoReconnect");
        timeToSleep = temp;
        max_attempts = maxAttempts;
        verbose = verb;
    }

    private void reconnect() {
        if (attempts++ >= max_attempts) return;
        try {
            Thread.sleep(timeToSleep);
            if (verbose) log("re-connecting to '" + parent.getServer() + "'");
            parent.connect();
            if (parent.isConnected()) {
                attempts = 0;
                for (int i = 0; i < getChannels().length; i++) parent.joinChannel(getChannels()[i][0], getChannels()[i][1]);
            } else {
                reconnect();
            }
        } catch (InterruptedException e) {
            if (verbose) log("could not reconnect: " + e.getMessage());
        }
    }

    @Override
    public void onDisconnect() {
        if (timeToSleep > 0) {
            if (verbose) log("Disconnected from server, AutoReconnect in " + (timeToSleep / 1000) + "sec");
            reconnect();
        }
    }

    @Override
    public void onNickInUse() {
        if (timeToSleep > 0) {
            if (verbose) log("Disconnected from server (Nick in use), AutoReconnect in " + (timeToSleep / 1000) + "sec");
            reconnect();
        }
    }

    @Override
    public void onHostUnreachable() {
        if (timeToSleep > 0) {
            if (verbose) log("Disconnected from server (Host unreachable), AutoReconnect in " + (timeToSleep / 1000) + "sec");
            reconnect();
        }
    }
}
