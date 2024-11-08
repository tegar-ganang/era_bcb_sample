package net.hypotenubel.jaicwain.session.irc;

import java.util.*;
import bsh.*;
import org.apache.log4j.Logger;
import net.hypotenubel.ctcp.*;
import net.hypotenubel.irc.*;
import net.hypotenubel.irc.msgutils.*;
import net.hypotenubel.irc.net.*;
import net.hypotenubel.jaicwain.*;

/**
 * Implements Jaic Wain's IRC connection logic. To initialize this session,
 * don't use any set methods. Instead, initialize it using the constructor
 * by supplying a network and an identity.
 *
 * @author Christoph Daniel Schulze
 * @version $Id: JaicWainIRCSession.java 156 2006-10-14 22:05:50Z captainnuss $
 */
public class JaicWainIRCSession extends AbstractIRCSession {

    /**
     * The network to connect to.
     */
    private IRCNetwork network = null;

    /**
     * The server to connect to.
     */
    private IRCServer server = null;

    /**
     * The BeanShell interpreter that's used to interprete user commands.
     */
    private Interpreter beanShell = null;

    /**
     * Creates a new instance and initializes it.
     * 
     * @param network the network to connect to.
     * @param identity the identity to connect with.
     * @throws NullPointerException if {@code network == null}
     *                              or {@code identity == null}.
     * @throws IllegalArgumentException if {@code network.getServers().size() == 0}.
     */
    public JaicWainIRCSession(IRCNetwork network, IRCIdentity identity) {
        super();
        if (network == null) {
            throw new NullPointerException("network can't be null");
        }
        if (identity == null) {
            throw new NullPointerException("identity can't be null");
        }
        if (network.getServers().size() == 0) {
            throw new IllegalArgumentException("network has no servers");
        }
        beanShell = new Interpreter();
        try {
            beanShell.eval("importCommands(\".\");");
            beanShell.set("session", this);
        } catch (EvalError e) {
            Logger.getLogger(JaicWainIRCSession.class).error("Unable to initialize BeanShell.", e);
        }
        this.network = network;
        this.setPrimaryLoginNick(identity.getPrimaryNick());
        this.setSecondaryLoginNick(identity.getSecondaryNick());
        this.setLoginUserName(identity.getUserName());
        this.setRealName(identity.getRealName());
        this.server = network.getDefaultServer();
        if (this.server == null) {
            this.server = network.getServers().get((int) (Math.random() * network.getServers().size()));
        }
        this.getConnection().setServerName(this.server.getAddress());
        this.getConnection().setRemotePort(this.server.getPort());
        this.getConnection().setSSL(this.server.isSslConnection());
    }

    /**
     * Returns the network this session should connect to.
     * 
     * @return the network.
     */
    public IRCNetwork getNetwork() {
        return this.network;
    }

    /**
     * Returns the server this session should connect to.
     * 
     * @return the server.
     */
    public IRCServer getServer() {
        return this.server;
    }

    /**
     * Processes a command. Session commands begin with a {@code /}
     * character, followed by the command's name and, depending on
     * the command, parameters.
     *
     * @param command the command.
     * @return {@code null} if everything went well, or an error
     *         message.
     */
    public String execute(String command) {
        if (command == null) {
            throw new NullPointerException("command can't be null");
        }
        if (command.startsWith("/")) {
            StringBuffer line = new StringBuffer(Utils.getArg(command, " ", 0).substring(1));
            line.append("(\"" + Utils.escape(Utils.getFromArg(command, 1)) + "\");");
            try {
                Object result = beanShell.eval(line.toString());
                if (result != null) {
                    if (result instanceof String) {
                        return (String) result;
                    }
                }
            } catch (EvalError e) {
                Logger.getLogger(JaicWainIRCSession.class).error("BeanShell script error: '" + e.getMessage() + "' File: '" + e.getErrorSourceFile() + "' Line: " + e.getErrorLineNumber() + "'", e);
                return App.localization.localize("app", "beanshell.messages.evalerror.simple", "The command couldn't be processed");
            }
        } else {
            return App.localization.localize("app", "defaultircsession.messages.noprivatemessages", "You must be on a channel to send chat messages");
        }
        return null;
    }

    @Override
    protected AbstractIRCChannel getChannelImpl(String name) {
        return new JaicWainIRCChannel(name, this);
    }

    @Override
    public void messageReceived(IRCMessageEvent e) {
        super.messageReceived(e);
        if (e.isConsumed()) {
            return;
        }
        fireMessageProcessedEvent(e.getMessage());
    }

    @Override
    protected void setupMessageMap() {
        super.setupMessageMap();
        this.addMessageHandler(IRCMessageTypes.MSG_PRIVMSG, new IRCMessageHandler() {

            public void handleMessage(IRCMessageEvent e) {
                CTCPMessage ctcp = CTCPMessage.parseMessageString(PrivateMessage.getText(e.getMessage()));
                if (ctcp.getType().equals(CTCPMessageTypes.CTCP_CLIENTINFO)) {
                } else if (ctcp.getType().equals(CTCPMessageTypes.CTCP_PING)) {
                    CTCPMessage cMsg = net.hypotenubel.ctcp.msgutils.PingMessage.createReply(ctcp);
                    IRCMessage iMsg = NoticeMessage.createMessage("", "", "", e.getMessage().getNick(), cMsg.toString());
                    conn.send(iMsg);
                } else if (ctcp.getType().equals(CTCPMessageTypes.CTCP_TIME)) {
                    CTCPMessage cMsg = net.hypotenubel.ctcp.msgutils.TimeMessage.createReply((new Date()).toString());
                    IRCMessage iMsg = NoticeMessage.createMessage("", "", "", e.getMessage().getNick(), cMsg.toString());
                    conn.send(iMsg);
                } else if (ctcp.getType().equals(CTCPMessageTypes.CTCP_VERSION)) {
                    String version = App.APP_SHORT_NAME + " " + App.APP_VERSION + " (" + App.APP_WEBSITE + ")";
                    CTCPMessage cMsg = net.hypotenubel.ctcp.msgutils.VersionMessage.createReply(version);
                    IRCMessage iMsg = NoticeMessage.createMessage("", "", "", e.getMessage().getNick(), cMsg.toString());
                    conn.send(iMsg);
                }
            }
        });
    }
}
