package org.furthurnet.furi;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.SwingUtilities;
import org.furthurnet.xmlparser.IrcSpec;
import org.furthurnet.xmlparser.RegistrationManager;

public class IrcManager {

    private Vector mChannels = new Vector();

    private Hashtable mPrivateChats = new Hashtable();

    private IrcChannel mFindTempVar = new IrcChannel("dummy");

    private DataChanger mServerChanger = new DataChanger();

    private DataChanger mChannelChanger = new DataChanger();

    private DataChanger mUserChanger = new DataChanger();

    private DataChanger mMsgChanger = new DataChanger();

    private DataChanger mLogChanger = new DataChanger();

    private Host mRemoteHost = null;

    private SendManager mSendMgr = ServiceManager.getSendManager();

    private Object[] mHandlerParams = new Object[2];

    private Hashtable mHandlerMethods = new Hashtable();

    private StringBuffer mWhoisResult = new StringBuffer();

    private boolean mAuth = false;

    public IrcManager() {
    }

    public void serializeChannels() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < mChannels.size(); i++) {
            IrcChannel channel = (IrcChannel) mChannels.elementAt(i);
            buf.append(channel.getName());
            buf.append(" ");
        }
        ServiceManager.getCfg().mIrcChannels = buf.toString();
    }

    public void addIrcChannels() {
        try {
            IrcSpec spec = ServiceManager.getManager().getMainFrame().getIrcSpec();
            for (int i = 0; i < spec.numChannels(); i++) {
                org.furthurnet.xmlparser.IrcChannel channel = spec.getChannel(i);
                addIrcChannel(channel.getChannel(), channel.getDescription());
            }
        } catch (Exception e) {
        }
    }

    public void addServerChangedListener(IDataChangedListener listener) {
        mServerChanger.addListener(listener);
    }

    public void addChannelChangedListener(IDataChangedListener listener) {
        mChannelChanger.addListener(listener);
    }

    public void addUserChangedListener(IDataChangedListener listener) {
        mUserChanger.addListener(listener);
    }

    public void addMsgChangedListener(IDataChangedListener listener) {
        mMsgChanger.addListener(listener);
    }

    public void addLogChangedListener(IDataChangedListener listener) {
        mLogChanger.addListener(listener);
    }

    public Vector getChannels() {
        return mChannels;
    }

    public synchronized void addIrcChannel(String channelName, String channelDescription) {
        IrcChannel channel = new IrcChannel(channelName, channelDescription);
        int index = SortUtil.orderedFind(mChannels, channel);
        if (index != -1) return;
        SortUtil.orderedInsert(mChannels, channel);
        channel.addUserChangedListener(new UserChangedListener());
        channel.addMsgChangedListener(new MsgChangedListener());
        mChannelChanger.dataChanged(channel);
    }

    public synchronized void removeIrcChannel(IrcChannel channel) {
        int index = SortUtil.orderedFind(mChannels, channel);
        if (index == -1) return;
        mChannels.removeElementAt(index);
        mChannelChanger.dataChanged(channel);
    }

    public synchronized void addIrcMsg(String channelName, IrcMsg msg) {
        mFindTempVar.setName(channelName);
        int index = SortUtil.orderedFind(mChannels, mFindTempVar);
        if (index == -1) {
            return;
        }
        IrcChannel channel = (IrcChannel) mChannels.elementAt(index);
        channel.addMsg(msg);
    }

    public synchronized void addIrcUser(String channelName, IrcUser user) {
        mFindTempVar.setName(channelName);
        int index = SortUtil.orderedFind(mChannels, mFindTempVar);
        if (index == -1) {
            return;
        }
        IrcChannel channel = (IrcChannel) mChannels.elementAt(index);
        channel.addUser(user);
    }

    public synchronized void addIrcUser(String channel, String nick) {
        addIrcUser(channel, new IrcUser(nick));
    }

    public synchronized void removeIrcUser(String channelName, String nick) {
        mFindTempVar.setName(channelName);
        int index = SortUtil.orderedFind(mChannels, mFindTempVar);
        if (index == -1) {
            return;
        }
        IrcChannel channel = (IrcChannel) mChannels.elementAt(index);
        channel.deleteUser(nick);
    }

    public synchronized void renameIrcUser(String oldNick, String newNick) {
        for (int i = 0; i < mChannels.size(); i++) {
            IrcChannel channel = (IrcChannel) mChannels.elementAt(i);
            channel.renameUser(oldNick, newNick);
        }
        if (oldNick.equals(ServiceManager.getCfg().mIrcNickname)) {
            ServiceManager.getCfg().mIrcNickname = newNick;
            mServerChanger.dataChanged(null);
        }
    }

    public synchronized Vector getChannelsContainingUser(String nick) {
        Vector result = new Vector();
        for (int i = 0; i < mChannels.size(); i++) {
            IrcChannel channel = (IrcChannel) mChannels.elementAt(i);
            if (channel.findUser(nick) != null) {
                result.addElement(channel);
            }
        }
        return result;
    }

    public String getChannelName(String channel) {
        for (int i = 0; i < mChannels.size(); i++) {
            if (((IrcChannel) mChannels.elementAt(i)).getName().equals(channel)) return ((IrcChannel) mChannels.elementAt(i)).getDescription();
        }
        return null;
    }

    public void sendMsg(MsgIRC msg) {
        mSendMgr.queueMsgToSend(mRemoteHost, msg, false);
    }

    public synchronized void connect() throws Exception {
        mRemoteHost = new Host(ServiceManager.getHostManager());
        mRemoteHost.setType(Host.sTypeIRC);
        mRemoteHost.setStatus(Host.sStatusConnecting, "");
        mRemoteHost.addStatusChangedListener(new HostStatusChangedListener());
        new ReadWorker(mRemoteHost, 2000);
        RegistrationManager regMgr = new RegistrationManager();
        regMgr.loginNick(ServiceManager.getCfg().mIrcNickname, ServiceManager.getCfg().mIrcKey);
    }

    public void disconnect() {
        mAuth = false;
        ServiceManager.getManager().getMainFrame().chatPane().mAuthenticated = false;
        ServiceManager.getManager().getMainFrame().chatPane().enableIrcButtons();
        ServiceManager.getManager().getMainFrame().chatPane().clearIrcWindows();
        if (mRemoteHost == null) {
            return;
        }
        mRemoteHost.closeConnection();
        mRemoteHost = null;
        for (int i = 0; i < mChannels.size(); i++) {
            IrcChannel channel = (IrcChannel) mChannels.elementAt(i);
            channel.setJoined(false);
        }
        mChannelChanger.dataChanged(null);
        mServerChanger.dataChanged(null);
    }

    public void processIncomingMsg(String text, MsgIRC inMsg) {
        try {
            String command = inMsg.getCommand().toUpperCase();
            if ((!mAuth) && (command != null) && (command.length() > 0) && (!command.equals("NOTICE"))) {
                mAuth = true;
                ServiceManager.getManager().getMainFrame().chatPane().mAuthenticated = true;
                ServiceManager.getManager().getMainFrame().chatPane().enableIrcButtons();
                ServiceManager.getManager().getMainFrame().chatPane().updateIrcInfoLabel();
            }
            if (command.equals("311")) irchandler_311(text, inMsg); else if (command.equals("312")) irchandler_312(text, inMsg); else if (command.equals("317")) irchandler_317(text, inMsg); else if (command.equals("318")) irchandler_318(text, inMsg); else if (command.equals("319")) irchandler_319(text, inMsg); else if (command.equals("332")) irchandler_332(text, inMsg); else if (command.equals("333")) irchandler_333(text, inMsg); else if (command.equals("353")) irchandler_353(text, inMsg); else if (command.equals("431")) irchandler_431(text, inMsg); else if (command.equals("432")) irchandler_432(text, inMsg); else if (command.equals("433")) irchandler_433(text, inMsg); else if (command.equals("ERROR")) irchandler_ERROR(text, inMsg); else if (command.equals("JOIN")) irchandler_JOIN(text, inMsg); else if (command.equals("NICK")) irchandler_NICK(text, inMsg); else if (command.equals("NOTICE")) irchandler_NOTICE(text, inMsg); else if (command.equals("PART")) irchandler_PART(text, inMsg); else if (command.equals("PING")) irchandler_PING(text, inMsg); else if (command.equals("PRIVMSG")) irchandler_PRIVMSG(text, inMsg); else if (command.equals("QUIT")) irchandler_QUIT(text, inMsg); else if (command.startsWith("4")) handleError(text, inMsg); else {
                defaultMethod(text, inMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean getIsConnected() {
        return (mRemoteHost != null);
    }

    public Host getRemoteHost() {
        return mRemoteHost;
    }

    public synchronized void addPrivateChatMsgChangedListener(String localUser, String remoteUser, IDataChangedListener listener) {
        PrivateChat pchat = (PrivateChat) mPrivateChats.get(remoteUser);
        pchat.addMsgChangedListener(listener);
    }

    public synchronized void removePrivateChat(String remoteUser) {
        mPrivateChats.remove(remoteUser);
    }

    public synchronized PrivateChat getPrivateChat(String remoteUser) {
        return (PrivateChat) mPrivateChats.get(remoteUser);
    }

    private synchronized void handlePrivateMsg(String localUser, String remoteUser, String chatText) {
        PrivateChat pchat = (PrivateChat) mPrivateChats.get(remoteUser);
        IrcCtcp ctcp = new IrcCtcp(localUser, remoteUser, chatText, this);
        if (ctcp.isCtcp()) {
            if (ctcp.isValid()) {
                ctcp.handleRequest();
            }
        } else {
            if (pchat == null) {
                pchat = createPrivateChat(localUser, remoteUser);
            }
            if (remoteUser.endsWith(ServiceManager.getCfg().mIrcNickIdentifier)) remoteUser = remoteUser.substring(0, remoteUser.length() - 1);
            pchat.addMsg(remoteUser + ": " + chatText);
        }
    }

    public synchronized PrivateChat createPrivateChat(String localUser, String remoteUser) {
        if ((localUser == null) || (localUser.length() == 0) || (remoteUser == null) || (remoteUser.length() == 0)) return null;
        PrivateChat pchat = new PrivateChat(localUser, remoteUser);
        mPrivateChats.put(remoteUser, pchat);
        SwingUtilities.invokeLater(new CreatePrivateChatFrame(localUser, remoteUser));
        return pchat;
    }

    public void sendWhois(String nick) {
        mWhoisResult = new StringBuffer();
        sendMsg(new MsgIRC(null, "WHOIS", nick));
    }

    private class CreatePrivateChatFrame implements Runnable {

        private String mLocalUser;

        private String mRemoteUser;

        public CreatePrivateChatFrame(String localUser, String remoteUser) {
            mLocalUser = localUser;
            mRemoteUser = remoteUser;
        }

        public void run() {
            FurthurThread.logPid("furi.IrcManager " + hashCode());
            PrivateChatFrame frame = new PrivateChatFrame(null);
            frame.setup(mLocalUser, mRemoteUser);
            frame.setVisible(true);
        }
    }

    private class UserChangedListener implements IDataChangedListener {

        public void dataChanged(Object source) {
            mUserChanger.dataChanged(source);
            mChannelChanger.dataChanged(source);
        }
    }

    private class MsgChangedListener implements IDataChangedListener {

        public void dataChanged(Object source) {
            mMsgChanger.dataChanged(source);
            mChannelChanger.dataChanged(source);
        }
    }

    private void handleError(String text, MsgIRC inMsg) {
        mLogChanger.dataChanged(text);
    }

    private void defaultMethod(String text, MsgIRC inMsg) {
        StringBuffer buf = new StringBuffer();
        String sender = inMsg.getSender();
        String command = inMsg.getCommand();
        Vector params = inMsg.getParamsInVector();
        if (sender != null) {
            buf.append(sender);
            buf.append(":  ");
        }
        if (!Character.isDigit(command.charAt(0))) {
            buf.append(command);
            buf.append(" ");
        }
        for (int i = 1; i < params.size(); i++) {
            buf.append((String) params.elementAt(i));
            buf.append(" ");
        }
        mLogChanger.dataChanged(buf.toString());
    }

    private void irchandler_JOIN(String text, MsgIRC inMsg) {
        String user = inMsg.getSender();
        String channel = inMsg.getParam1();
        addIrcMsg(channel, new IrcMsg(null, user + " has joined " + channel));
        if (!user.equals(ServiceManager.getCfg().mIrcNickname + ServiceManager.getCfg().mIrcNickIdentifier)) {
            addIrcUser(channel, user);
        }
    }

    private void irchandler_PART(String text, MsgIRC inMsg) {
        String user = inMsg.getSender();
        String channel = inMsg.getParam1();
        removeIrcUser(channel, user);
        addIrcMsg(channel, new IrcMsg(null, user + " has left " + channel));
    }

    private void irchandler_QUIT(String text, MsgIRC inMsg) {
        String user = inMsg.getSender();
        String channel = inMsg.getParam1();
        IrcMsg msg = new IrcMsg(null, user + " has quit IRC (" + channel + ")");
        Vector channels = getChannelsContainingUser(user);
        for (int i = 0; i < channels.size(); i++) {
            IrcChannel ch = (IrcChannel) channels.elementAt(i);
            ch.addMsg(msg);
            ch.deleteUser(user);
        }
    }

    private void irchandler_PRIVMSG(String text, MsgIRC inMsg) {
        String user = inMsg.getSender();
        Vector params = inMsg.getParamsInVector();
        String target = (String) params.elementAt(0);
        if (target.equals(ServiceManager.getCfg().mIrcNickname + ServiceManager.getCfg().mIrcNickIdentifier)) {
            handlePrivateMsg(target, user, (String) params.elementAt(1));
        } else {
            addIrcMsg(target, new IrcMsg(user, (String) params.elementAt(1)));
        }
    }

    private void irchandler_NICK(String text, MsgIRC inMsg) {
        String user = inMsg.getSender();
        String newNick = inMsg.getParam1();
        renameIrcUser(user, newNick);
    }

    private void irchandler_PING(String text, MsgIRC inMsg) {
        sendMsg(new MsgIRC(null, "PONG", inMsg.getParams()));
    }

    private void irchandler_NOTICE(String text, MsgIRC inMsg) {
        mLogChanger.dataChanged(text);
    }

    private void irchandler_ERROR(String text, MsgIRC inMsg) {
        mLogChanger.dataChanged(text);
    }

    private void irchandler_332(String text, MsgIRC inMsg) {
        Vector params = inMsg.getParamsInVector();
        addIrcMsg((String) params.elementAt(1), new IrcMsg(null, "Topic is '" + (String) params.elementAt(2) + "'"));
    }

    private void irchandler_333(String text, MsgIRC inMsg) {
        Vector params = inMsg.getParamsInVector();
        addIrcMsg((String) params.elementAt(1), new IrcMsg(null, "Set by " + (String) params.elementAt(2)));
    }

    private void irchandler_353(String text, MsgIRC inMsg) {
        Vector params = inMsg.getParamsInVector();
        String channel = (String) params.elementAt(2);
        String names = (String) params.elementAt(3);
        StringTokenizer tokens = new StringTokenizer(names);
        while (tokens.hasMoreTokens()) {
            addIrcUser(channel, tokens.nextToken());
        }
    }

    private void irchandler_311(String text, MsgIRC inMsg) {
        Vector params = inMsg.getParamsInVector();
        mWhoisResult.append((String) params.elementAt(1));
        mWhoisResult.append(" is ");
        mWhoisResult.append((String) params.elementAt(2));
        mWhoisResult.append("@");
        mWhoisResult.append((String) params.elementAt(3));
        mWhoisResult.append(", whose name is ");
        mWhoisResult.append((String) params.elementAt(5));
        mWhoisResult.append("\n");
    }

    private void irchandler_319(String text, MsgIRC inMsg) {
        Vector params = inMsg.getParamsInVector();
        mWhoisResult.append((String) params.elementAt(1));
        mWhoisResult.append(" is in the following channels: ");
        for (int i = 2; i < params.size(); i++) {
            mWhoisResult.append((String) params.elementAt(1));
            mWhoisResult.append(" ");
        }
        mWhoisResult.append("\n");
    }

    private void irchandler_312(String text, MsgIRC inMsg) {
        Vector params = inMsg.getParamsInVector();
        mWhoisResult.append((String) params.elementAt(1));
        mWhoisResult.append(" connects to IRC server ");
        mWhoisResult.append((String) params.elementAt(2));
        mWhoisResult.append(" (");
        mWhoisResult.append((String) params.elementAt(3));
        mWhoisResult.append(")\n");
    }

    private void irchandler_317(String text, MsgIRC inMsg) {
        Vector params = inMsg.getParamsInVector();
        mWhoisResult.append((String) params.elementAt(1));
        mWhoisResult.append(" has been idle for ");
        mWhoisResult.append((String) params.elementAt(2));
        mWhoisResult.append(" seconds, signed on ");
        java.util.Date date = new java.util.Date(Long.valueOf((String) params.elementAt(3)).longValue());
        mWhoisResult.append(date.toString());
        mWhoisResult.append("\n");
    }

    private void irchandler_318(String text, MsgIRC inMsg) {
        SwingUtilities.invokeLater(new Invoker(this, "popupWhoisResult"));
    }

    private void irchandler_431(String text, MsgIRC inMsg) {
        this.disconnect();
        ServiceManager.getManager().getMainFrame().chatPane().invalidIrcNickname();
    }

    private void irchandler_432(String text, MsgIRC inMsg) {
        this.disconnect();
        ServiceManager.getManager().getMainFrame().chatPane().invalidIrcNickname();
    }

    private void irchandler_433(String text, MsgIRC inMsg) {
        this.disconnect();
        ServiceManager.getManager().getMainFrame().chatPane().invalidIrcNickname();
    }

    void popupWhoisResult() {
        DlgLog dlg = new DlgLog(ServiceManager.getManager().getMainFrame(), "Whois ", "Result: ", mWhoisResult.toString());
        dlg.setVisible(true);
        dlg.dispose();
    }

    public void ping() {
        if (ServiceManager.getCfg().mChatPing && getIsConnected()) {
            sendMsg(new MsgIRC(null, "PING", Res.getStr("Program.Version")));
        }
    }

    private class HostStatusChangedListener implements IDataChangedListener {

        public void dataChanged(Object source) {
            Host rhost = (Host) source;
            if (rhost.getStatus() == Host.sStatusError) {
                mLogChanger.dataChanged(rhost.getStatusName());
                disconnect();
            }
            mServerChanger.dataChanged(null);
        }
    }
}
