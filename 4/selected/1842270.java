package net.sf.zorobot.irc;

import net.sf.zorobot.core.Message;

public class DummyIrc implements IrcInterface {

    public boolean isRegisteredNick(String nick) {
        return true;
    }

    public void message(int channelId, int msgId, String sender, Message message) {
    }

    public int getChannelId(String channelName) {
        return 0;
    }

    public String getChannelName(int channelId) {
        return null;
    }

    public String getServer() {
        return null;
    }

    public String ping(String nick) {
        return null;
    }

    public int joinChannel(String channelName) throws Exception {
        return 0;
    }

    public int leaveChannel(int channelId) {
        return 0;
    }

    public boolean ensureJoin(String channelName) {
        return false;
    }
}
