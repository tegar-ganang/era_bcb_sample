package org.boticelli.plugin.dist;

import java.util.List;
import org.apache.log4j.Logger;
import org.boticelli.Bot;
import org.boticelli.dao.LogDAO;
import org.boticelli.logsearch.LogIndexer;
import org.boticelli.model.LogEntry;
import org.boticelli.model.LogType;
import org.boticelli.plugin.HelpfulBoticelliPlugin;
import org.boticelli.plugin.PluginResult;
import org.boticelli.util.Util;
import org.springframework.beans.factory.annotation.Required;
import f00f.net.irc.martyr.InCommand;
import f00f.net.irc.martyr.commands.ChannelModeCommand;
import f00f.net.irc.martyr.commands.CtcpMessage;
import f00f.net.irc.martyr.commands.JoinCommand;
import f00f.net.irc.martyr.commands.KickCommand;
import f00f.net.irc.martyr.commands.MessageCommand;
import f00f.net.irc.martyr.commands.NickCommand;
import f00f.net.irc.martyr.commands.QuitCommand;
import f00f.net.irc.martyr.commands.TopicCommand;

public class MessageLogger implements HelpfulBoticelliPlugin {

    protected static Logger log = Logger.getLogger(MessageLogger.class);

    private LogDAO logDAO;

    private LogIndexer logIndexer;

    @Required
    public void setLogIndexer(LogIndexer logIndexer) {
        this.logIndexer = logIndexer;
    }

    @Required
    public void setLogDAO(LogDAO logDAO) {
        this.logDAO = logDAO;
    }

    public PluginResult handle(Bot bot, InCommand command) throws Exception {
        log.debug("LOG " + command.getClass() + ":" + Util.toString(command));
        if (command instanceof CtcpMessage) {
            CtcpMessage msg = (CtcpMessage) command;
            String action = msg.getAction();
            if (action.equals("ACTION")) {
                log(msg.getSource().getSource(), msg.getMessage(), LogType.ACTION);
            }
        } else if (command instanceof MessageCommand) {
            MessageCommand msg = (MessageCommand) command;
            if (!msg.isPrivateToUs(bot.getState())) {
                log(msg.getSource().getSource(), msg.getMessage(), LogType.PRIVMSG);
            }
        } else if (command instanceof JoinCommand) {
            JoinCommand join = (JoinCommand) command;
            log(join.getUser().getSource(), "has joined " + join.getChannel(), LogType.INFO);
        } else if (command instanceof QuitCommand) {
            QuitCommand quit = (QuitCommand) command;
            log(quit.getUser().getSource(), "has left " + bot.getChannelName() + " : " + quit.getReason(), LogType.INFO);
        } else if (command instanceof KickCommand) {
            KickCommand kick = (KickCommand) command;
            log(kick.getKicker().getSource(), "has kicked " + kick.getKicked().getSource() + " from " + kick.getChannel() + " : " + kick.getComment(), LogType.INFO);
        } else if (command instanceof NickCommand) {
            NickCommand nick = (NickCommand) command;
            log(nick.getOldNick(), "is now known as " + nick.getNick(), LogType.INFO);
        } else if (command instanceof ChannelModeCommand) {
            ChannelModeCommand mode = (ChannelModeCommand) command;
            String source = mode.getPrefix();
            if (source == null) {
                source = "null";
            }
            log(source, " sets " + mode.render(), LogType.INFO);
        } else if (command instanceof TopicCommand) {
            TopicCommand topic = (TopicCommand) command;
            String source = Util.split(topic.getSourceString().substring(1), " ").get(0);
            log(source, "changes the topic to: " + topic.getTopic(), LogType.INFO);
        }
        return PluginResult.NEXT;
    }

    private void log(String ident, String text, LogType type) {
        List<LogEntry> logs = LogEntry.create(ident, text, type);
        for (LogEntry log : logs) {
            logDAO.create(log);
        }
        logIndexer.index(logs, false);
    }

    public boolean supports(Class<? extends InCommand> inCommandClass) {
        return true;
    }

    public String getHelpName() {
        return "log";
    }

    public String helpText(Bot bot, List<String> args) {
        return "The log module logs all conversation in the channel";
    }
}
