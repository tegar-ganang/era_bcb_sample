package net.sf.jerkbot.plugins.irclog;

import jerklib.Channel;
import jerklib.Session;
import net.sf.jerkbot.bot.BotService;
import org.apache.commons.lang.StringUtils;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 *         [INSERT DESCRIPTION HERE]
 * @version 0.0.1
 */
public class IRCLogger extends StandardMBean implements IRCLoggerMBean {

    private BotService botService;

    private IRCLogListener IRCLogListener;

    private String logsDir;

    private volatile boolean started = false;

    public IRCLogger(BotService botService) throws NotCompliantMBeanException {
        super(IRCLoggerMBean.class);
        this.botService = botService;
    }

    protected String getDescription(MBeanAttributeInfo info) {
        if (info.getName().equals("LogsDir")) {
            return "The IRC logs directory";
        }
        return info.getDescription();
    }

    protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
        if (op.getName().equals("addLoggedChannel") || op.getName().equals("removeLoggedChannel")) {
            return "channelName";
        }
        return param.getName();
    }

    protected String getDescription(MBeanInfo info) {
        return "MBean to log IRC channels";
    }

    public String displayLoggedChannels() {
        if (!started) {
            return "No channel configured for logging yet! The logging service is not started!";
        }
        return IRCLogListener.getLoggedChannels();
    }

    public String addLoggedChannel(String channelName) {
        if (!started) {
            return "No channel configured for logging yet! The logging service is not started!";
        }
        if (StringUtils.isEmpty(logsDir)) {
            return "You must configure the logs directory first!";
        }
        Session session = botService.getSession();
        if (session == null) {
            return "Internal error no IRC session. Please make the bot join a channel first";
        }
        Channel channel = session.getChannel(channelName);
        if (channel == null) {
            return String.format("The bot must join the channel '%s', first!", channelName);
        }
        boolean channelAdded = IRCLogListener.addLoggedChannel(logsDir, channelName);
        if (channelAdded) {
            return String.format("The channel '%s' will be logged.", channelName);
        } else {
            return String.format("The bot might already be logging the channel '%s'", channelName);
        }
    }

    public String removeLoggedChannel(String channelName) {
        if (!IRCLogListener.isLogged(channelName)) {
            return (String.format("The channel '%s' is not logged!", channelName));
        }
        return String.format("Removing logging for channel '%s'", channelName);
    }

    public void purgeLogs() {
    }

    public String getLogsDir() {
        if (StringUtils.isEmpty(logsDir)) {
            return "No directory configured for logs";
        }
        return logsDir;
    }

    public void setLogsDir(String logsDir) {
        this.logsDir = logsDir;
    }

    public void stop() {
        if (started) {
            Session session = botService.getSession();
            if (session != null) {
                session.removeIRCEventListener(IRCLogListener);
                started = false;
            }
        }
    }

    public void start() {
    }

    public String status() {
        return started ? "Started" : "Stopped";
    }
}
