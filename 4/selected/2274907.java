package net.sf.jerkbot.plugins.irclog;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.MessageEvent;
import jerklib.listeners.IRCEventListener;
import org.apache.log4j.Logger;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 *         Unfinished business
 * @version 0.0.1
 */
class IRCLogListener implements IRCEventListener {

    private static final Logger Log = Logger.getLogger(IRCLogListener.class);

    private final Set<String> channelList = new CopyOnWriteArraySet<String>();

    private final Map<String, IRCLogWriter> logsMap = new HashMap<String, IRCLogWriter>();

    public IRCLogListener() {
    }

    String getLoggedChannels() {
        return channelList.toString();
    }

    boolean hasNoLoggedChannels() {
        return channelList.isEmpty();
    }

    boolean addLoggedChannel(String logDir, String channelName) {
        if (isLogged(channelName)) {
            return false;
        }
        try {
            IRCLogWriter writer = new IRCLogWriter(new File(logDir), channelName);
            logsMap.put(channelName, writer);
            return true;
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
        }
        return false;
    }

    boolean isLogged(String channel) {
        if (channelList.isEmpty()) {
            return false;
        }
        return channelList.contains(channel);
    }

    private void removeLoggedChannel(String s) {
        boolean removed = channelList.remove(s);
        if (removed) {
            IRCLogWriter w = logsMap.get(s);
            w.close();
        }
    }

    public void receiveEvent(IRCEvent ircEvent) {
        if (ircEvent.getType() == IRCEvent.Type.CHANNEL_MESSAGE) {
            MessageEvent evt = (MessageEvent) ircEvent;
            if (!evt.isPrivate()) {
                if (channelList.isEmpty()) {
                    return;
                }
                final String channel = evt.getChannel().getName();
                if (isLogged(channel)) {
                    Session session = ircEvent.getSession();
                    Channel _channel = session.getChannel(channel);
                    if (_channel == null) {
                        removeLoggedChannel(channel);
                        return;
                    }
                    final String message = evt.getMessage();
                    final String sender = evt.getNick();
                    IRCLog ircLog = new IRCLog(channel, sender, message);
                    Log.info(ircLog);
                }
            }
        }
    }
}
