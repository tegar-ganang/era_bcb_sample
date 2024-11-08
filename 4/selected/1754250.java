package org.thole.phiirc.client.controller;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Vector;
import org.thole.phiirc.client.model.Channel;
import org.thole.phiirc.client.view.swing.actions.ActiveChannel;
import org.thole.phiirc.irc.IRCConnection;

/**
 * this class receives pre-parsed commands from the GUI.
 * it will forward commands to the server and handle what has to be elsewhere
 * @author hendrik
 *
 */
public class UserAction {

    private IRCConnection irccon;

    public UserAction() {
        this.setIrccon(Controller.getInstance().getConnector().getConnection());
    }

    /**
	 * send commands to server
	 * @param msg
	 * @throws IOException 
	 * @throws BufferOverflowException 
	 */
    private void sendToServer(final String msg) {
        this.getIrccon().getWriteHandler().getMh().write(msg);
    }

    /**
	 * join a channel
	 * @param chan
	 */
    public void doJoin(final String chan) {
        doJoin(chan, "");
        new ChannelWorker(chan);
    }

    /**
	 * join a passworded channel
	 * @param chan
	 * @param key
	 */
    public void doJoin(final String chan, final String key) {
        this.sendToServer("JOIN " + chan + " " + key);
        Controller.getInstance().getCWatcher().create(chan);
        Controller.getInstance().getClient().getChannelList().setChannelData(new Vector<String>(Controller.getInstance().getCWatcher().chanList()));
        ActiveChannel.active(chan);
    }

    /**
	 * leave a channel
	 */
    public void doPart() {
        doPart("");
    }

    /**
	 * leave a channel and a message
	 * @param msg
	 */
    public void doPart(final String msg) {
        final String myChan = getTarget();
        this.sendToServer("PART " + myChan + " " + msg);
        Controller.getInstance().getCWatcher().remove(myChan);
        Controller.getInstance().getClient().getChannelList().setChannelData(new Vector<String>(Controller.getInstance().getCWatcher().chanList()));
    }

    /**
	 * send a message to a channel or user
	 * @param msg
	 */
    public void doPrivmsg(final String msg) {
        doPrivmsg(getTarget(), msg);
    }

    /**
	 * send a message to certain channel or user
	 * @param target
	 * @param msg
	 */
    public void doPrivmsg(final String target, final String msg) {
        DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        Calendar calender = Calendar.getInstance();
        String time = df.format(calender.getTime());
        Channel channel = Controller.getInstance().getCWatcher().create(target);
        ActiveChannel.active(target);
        String myMsg = "[" + time + "] <" + Controller.getInstance().getConnector().getMyUser().getNick() + "> " + msg.substring(1) + "\n";
        this.sendToServer("PRIVMSG " + target + " " + msg);
        channel.getChat().append(myMsg);
        Controller.getInstance().getClient().getChatPane().append(myMsg);
        if (channel.getName().charAt(0) != '#') {
            Controller.getInstance().getClient().getChannelList().setChannelData(new Vector<String>(Controller.getInstance().getCWatcher().chanList()));
        }
    }

    /**
	 * unsets away
	 */
    public void doAway() {
        this.sendToServer("AWAY");
    }

    /**
	 * set away and an away message
	 * @param awayMsg
	 */
    public void doAway(final String awayMsg) {
        this.sendToServer("AWAY" + awayMsg);
    }

    /**
	 * invites a user to a channel
	 * @param nick
	 * @param chan
	 */
    public void doInvite(final String nick, final String chan) {
        this.sendToServer("INVITE " + nick + " " + chan);
    }

    /**
	 * ison
	 * @param nick
	 */
    public void doIson(final String nick) {
        this.sendToServer("ISON " + nick);
    }

    /**
	 * kick a user from current chan
	 * @param nick
	 */
    public void doKick(final String nick) {
        doKick(nick, "bye!");
    }

    /**
	 * kick a user from current chan adding a kick message
	 * @param nick
	 * @param msg
	 */
    public void doKick(final String nick, final String msg) {
        this.sendToServer("KICK " + getTarget() + " " + nick + " " + msg);
    }

    /**
	 * get all users from a given target
	 * @param chan
	 */
    public void doNames(final String chan) {
        this.sendToServer("NAMES " + chan);
    }

    /**
	 * get all users from the current active target
	 */
    public void doNames() {
        doNames(getTarget());
    }

    /**
	 * Sends a mode to the server. 
	 * @param target
	 * @param mode
	 */
    public void doMode(final String target, final String mode) {
        this.sendToServer("MODE " + target + " " + mode);
    }

    /**
	 * Requests a Reply 324 for the modes of a given channel.
	 * @param chan
	 */
    public void doMode(final String chan) {
    }

    /**
	 * send a notice to a channel or user
	 * @param target
	 * @param msg
	 */
    public void doNotice(final String target, final String msg) {
        this.sendToServer("NOTICE " + target + " " + msg);
    }

    /**
	 * Quit the IRC
	 */
    public void doQuit() {
        this.sendToServer("QUIT " + "I quit. You try the cool phiIRC!");
    }

    /**
	 * Quit leaving a message.
	 * @param msg
	 */
    public void doQuit(final String msg) {
        this.sendToServer("QUIT " + msg);
    }

    /**
	 * Set the topic
	 * @param topic
	 */
    public void doTopic(final String topic) {
        this.sendToServer("TOPIC " + getTarget() + " " + topic);
    }

    /**
	 * Request the topic
	 */
    public void doTopic() {
        this.sendToServer("TOPIC " + getTarget());
    }

    /**
	 * request a whois
	 * @param user
	 */
    public void doWhois(final String user) {
        this.sendToServer("WHOIS " + user);
    }

    /**
	 * set a new nickname
	 * @param nick
	 */
    public void doNick(final String nick) {
        this.sendToServer("NICK " + nick);
        Controller.getInstance().getConnector().getMyUser().setNick(nick);
    }

    /**
	 * get the chan we are currently in
	 * @return channel
	 */
    private String getTarget() {
        return Controller.getInstance().getConnector().getServer().getCurrentTarget();
    }

    private IRCConnection getIrccon() {
        return irccon;
    }

    private void setIrccon(final IRCConnection irccon) {
        this.irccon = irccon;
    }

    /**
	 * requests more info about user
	 * @param nick
	 */
    public void doWho(String nick) {
        this.sendToServer("WHO " + nick);
    }
}
