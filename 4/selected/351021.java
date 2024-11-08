package org.thole.phiirc.client.controller;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;
import org.apache.commons.lang.StringUtils;
import org.thole.phiirc.client.model.Channel;
import org.thole.phiirc.client.model.Server;
import org.thole.phiirc.client.model.User;
import org.thole.phiirc.irc.parser.IRCLPModeParser;

/**
 * This class delivers all lines etc for the GUI
 * @author hendrik
 *
 */
public class ChatListener {

    public ChatListener() {
    }

    /**
	 * Notifies the GUI of changes.
	 * @param target
	 * @param msg
	 */
    private void notifyGUI(final String target, final String msg) {
        final DateFormat df = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        final Calendar calender = Calendar.getInstance();
        final String time = df.format(calender.getTime());
        final Channel chan = Controller.getInstance().getCWatcher().create(target);
        Controller.getInstance().getCWatcher().getChan(target).getChat().append("[" + time + "] " + msg + "\n");
        if (chan.getName().equalsIgnoreCase(Controller.getInstance().getConnector().getServer().getCurrentTarget())) {
            Controller.getInstance().getClient().getChatPane().append("[" + time + "] " + msg + "\n");
        }
        if (chan.getName().charAt(0) != '#') {
            Controller.getInstance().getClient().getChannelList().setChannelData(new Vector<String>(Controller.getInstance().getCWatcher().chanList()));
        }
    }

    /**
	 * send stuff to Console
	 * @param msg
	 */
    public void onReply(final String msg) {
        notifyGUI(Server.CONSOLE, msg);
    }

    private void onDisconnected() {
        notifyGUI(Server.CONSOLE, "Disconnected.");
    }

    private void onInvite(final String chan, final User u) {
        notifyGUI(currentTarget(), "Invite: " + u.getNick() + " invited you to join " + chan);
    }

    /**
	 * A user joined the channel.
	 * @param chan
	 * @param u
	 */
    public void onJoin(final String chan, final User u) {
        notifyGUI(chan, "→ " + u.getNick() + " (" + userHostmask(u) + ") joined the channel.");
    }

    /**
	 * Announce a user was kicked from the channel
	 * @param chan Channel
	 * @param u User kicking
	 * @param nickPass kicked User
	 * @param msg kick message
	 */
    public void onKick(final String chan, final User u, final String nickPass, String msg) {
        String message = msg;
        if (StringUtils.isBlank(message)) {
            message = u.getNick();
        }
        notifyGUI(chan, "← " + u.getNick() + " kicked " + nickPass + " (" + message + ")");
    }

    private void onMode(String chan, User u, IRCLPModeParser mp) {
        notifyGUI(chan, u.getNick() + " sets mode: " + mp.getLine());
    }

    private void onMode(User u, String nickPass, String mode) {
        notifyGUI(currentTarget(), u.getNick() + " sets mode: " + mode + " " + nickPass);
    }

    /**
	 * Updates user list on end of /NAMES
	 * @param channel
	 */
    public void onNames(String channel) {
        if (Controller.getInstance().getConnector().getServer().getCurrentTarget().equalsIgnoreCase(channel)) {
            Controller.getInstance().getClient().getUserList().setUserListData(Controller.getInstance().getCWatcher().getChan(channel).getUserListNicks());
        }
    }

    /**
	 * announces new nickname of player
	 * @param u
	 * @param nickNew
	 */
    public void onNick(User u, String nickNew) {
        for (String currChan : Controller.getInstance().getCWatcher().chanList()) {
            if (Controller.getInstance().getCWatcher().getChan(currChan).getUserList().contains(u)) {
                notifyGUI(currChan, u.getNick() + " changed nick to: " + nickNew);
            }
        }
        Controller.getInstance().getClient().getUserList().setUserListData(Controller.getInstance().getCWatcher().getChan(currentTarget()).getUserListNicks());
    }

    /**
	 * Displays notice
	 * @param from
	 * @param msg
	 */
    public void onNotice(String from, String msg) {
        notifyGUI(currentTarget(), from + ": " + "(Notice) " + msg);
    }

    /**
	 * Announce a user parted from a channel.
	 * @param chan
	 * @param u
	 * @param msg
	 */
    public void onPart(String chan, User u, String msg) {
        String message = msg;
        if (StringUtils.isBlank(message)) {
            message = u.getNick();
        }
        notifyGUI(chan, "← " + u.getNick() + " left the channel. (" + message + ")");
    }

    /**
	 * PRIVMSG was received from a user.
	 * @param chan
	 * @param u
	 * @param msg
	 */
    public void onPrivmsg(String target, User u, String msg) {
        String myTarget = target;
        if (target.charAt(0) != '#') {
            myTarget = u.getNick();
        }
        notifyGUI(myTarget, "<" + u.getNick() + "> " + msg);
    }

    /**
	 * display the quit message in all channels the user was in.
	 * @param u
	 * @param msg
	 */
    public void onQuit(User u, String msg) {
        String message = msg;
        if (StringUtils.isBlank(message)) {
            message = u.getNick();
        }
        for (String currChan : Controller.getInstance().getCWatcher().chanList()) {
            if (Controller.getInstance().getCWatcher().getChan(currChan).getUserList().contains(u)) {
                notifyGUI(currChan, "← " + u.getNick() + " (" + userHostmask(u) + ") quits. (" + message + ")");
            }
        }
    }

    private void onTopic(String chan, User u, String topic) {
        notifyGUI(chan, u.getNick() + " changed the topic to: " + topic);
    }

    /**
	 * Announce the topic on join.
	 * @param chan
	 * @param topic
	 */
    public void onTopic(String chan, String topic) {
        notifyGUI(chan, "Topic is: " + topic);
    }

    /**
	 * Announce who set topic and when.
	 * @param chan
	 * @param nick
	 * @param date
	 */
    public void onTopicSetDate(String chan, String nick, Calendar date) {
        final Date theDate = date.getTime();
        notifyGUI(chan, "Topic was set by " + nick + " on " + theDate);
    }

    /**
	 * prints a hostmask
	 * @param user
	 * @return hostmask
	 */
    private String userHostmask(User u) {
        String hostmask = "unknown";
        if (!(u.getName().isEmpty() && u.getHost().isEmpty())) {
            hostmask = u.getName() + "@" + u.getHost();
        }
        return hostmask;
    }

    /**
	 * get the current target
	 * @return target
	 */
    private String currentTarget() {
        return Controller.getInstance().getConnector().getServer().getCurrentTarget();
    }
}
