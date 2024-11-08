package net.sf.jerkbot.plugins.ircsession;

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.StandardMBean;
import jerklib.Channel;
import jerklib.Session;

public class IRCSessionManager extends StandardMBean implements IRCSessionManagerMBean {

    private Session session;

    private final Map<String, String[]> methodParamsMap = new HashMap<String, String[]>();

    public IRCSessionManager(Session session) throws NotCompliantMBeanException {
        super(IRCSessionManagerMBean.class);
        this.session = session;
        methodParamsMap.put("kick", new String[] { "channel", "user", "reason" });
        methodParamsMap.put("op", new String[] { "channel", "nick" });
        methodParamsMap.put("deop", new String[] { "channel", "nick" });
        methodParamsMap.put("speak", new String[] { "channel", "message" });
        methodParamsMap.put("action", new String[] { "channel", "message" });
        methodParamsMap.put("part", new String[] { "channel" });
        methodParamsMap.put("say", new String[] { "messageForAllChannels" });
        methodParamsMap.put("join", new String[] { "channel" });
        methodParamsMap.put("part", new String[] { "channel" });
    }

    @Override
    protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
        String[] methodParamNames = methodParamsMap.get(op.getName());
        if (methodParamNames != null) {
            return methodParamNames[sequence];
        }
        return param.getName();
    }

    @Override
    protected String getDescription(MBeanInfo info) {
        return "MBean to perform IRC operations(kick, speak, part, etc.)";
    }

    public void say(String messageForAllChannels) {
        for (Channel ch : session.getChannels()) {
            ch.say(messageForAllChannels);
        }
    }

    public void quit() {
        session.close("");
    }

    public void kick(String channel, String user, String reason) {
        Channel aChannel = session.getChannel(channel);
        if (aChannel == null) {
            throw new RuntimeOperationsException(new NullPointerException(String.format("No such channel '%s'", channel)));
        }
        aChannel.kick(user, reason);
    }

    public void join(String channel) {
        session.join(channel);
    }

    public void op(String channel, String nick) {
        Channel aChannel = session.getChannel(channel);
        if (aChannel == null) {
            throw new RuntimeOperationsException(new NullPointerException(String.format("No such channel '%s'", channel)));
        }
        aChannel.op(nick);
    }

    public void deop(String channel, String nick) {
        Channel aChannel = session.getChannel(channel);
        if (aChannel == null) {
            throw new RuntimeOperationsException(new NullPointerException(String.format("No such channel '%s'", channel)));
        }
        aChannel.deop(nick);
    }

    public void part(String channel) {
        Channel aChannel = session.getChannel(channel);
        if (aChannel == null) {
            throw new RuntimeOperationsException(new NullPointerException(String.format("No such channel '%s'", channel)));
        }
        aChannel.part("");
    }

    public void speak(String channel, String str) {
        Channel aChannel = session.getChannel(channel);
        if (aChannel == null) {
            throw new RuntimeOperationsException(new NullPointerException(String.format("No such channel '%s'", channel)));
        }
        aChannel.say(str);
    }

    public void action(String channel, String message) {
        Channel aChannel = session.getChannel(channel);
        if (aChannel == null) {
            throw new RuntimeOperationsException(new NullPointerException(String.format("No such channel '%s'", channel)));
        }
        aChannel.action(message);
    }
}
