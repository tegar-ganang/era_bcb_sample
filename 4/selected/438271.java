package org.thole.phiirc.client.controller;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.thole.phiirc.client.model.Channel;
import org.thole.phiirc.client.model.User;
import org.thole.phiirc.client.model.UserChannelPermission;

/**
 * has a list of all known users we see in the network
 * has to update on nickchange, join, part, etc
 * @author hendrik
 *
 */
public class UserWatcher {

    private List<User> userList = new ArrayList<User>();

    /**
	 * Adds a user to the UserWatcher if user doesn't exist yet.
	 * @param nick
	 * @param channel
	 * @return user
	 */
    public User addUser(final String nick, final Channel chan) {
        return addUser(nick, "", "", "", chan, UserChannelPermission.UNKNOWN);
    }

    /**
	 * Adds a user to the UserWatcher if user doesn't exist yet.
	 * @param nick
	 * @param chan
	 * @param chanRole
	 * @return
	 */
    public User addUser(final String nick, final Channel chan, final UserChannelPermission chanRole) {
        return addUser(nick, "", "", "", chan, chanRole);
    }

    /**
	 * Adds a user to the UserWatcher if user doesn't exist yet.
	 * This method is only called on queries. There is the chance to be queried
	 * by a user not being in a channel you are.
	 * @param nick
	 * @return user
	 */
    public User addUser(final String nick) {
        return addUser(nick, "", "", "", null, UserChannelPermission.UNKNOWN);
    }

    /**
	 * Adds a user to the UserWatcher if user doesn't exist yet.
	 *  ** Mainly used to update or add additional user info. **
	 *  
	 * @param nick
	 * @param name
	 * @param host
	 * @return user
	 */
    public User addUser(final String nick, final String name, final String host) {
        return addUser(nick, name, "", host, null, UserChannelPermission.UNKNOWN);
    }

    /**
	 * Adds a user to the UserWatcher if user doesn't exist yet.
	 * ** Called on WHO to not add wrong channels **
	 * @param nick
	 * @param name
	 * @param realname
	 * @param host
	 * @return
	 */
    public User addUser(final String nick, final String name, final String realname, final String host) {
        return addUser(nick, name, realname, host, null, UserChannelPermission.UNKNOWN);
    }

    /**
	 * Adds a user to the UserWatcher if user doesn't exist yet.
	 * @param nick
	 * @param name
	 * @param host
	 * @param chan
	 * @return user
	 */
    public User addUser(final String nick, final String name, final String realname, final String host, final Channel chan) {
        return addUser(nick, name, realname, host, chan, UserChannelPermission.UNKNOWN);
    }

    /**
	 * Adds a user to the UserWatcher if user doesn't exist yet.
	 * @param nick
	 * @param name
	 * @param host
	 * @param chan
	 * @return
	 */
    public User addUser(final String nick, final String name, final String host, final Channel chan) {
        return addUser(nick, name, "", host, chan, UserChannelPermission.UNKNOWN);
    }

    /**
	 * Adds a user to the UserWatcher if user doesn't exist yet.
	 * @param nick
	 * @param name
	 * @param realname
	 * @param host
	 * @param chan
	 * @param chanRole
	 * @return user
	 */
    protected User addUser(final String nick, final String name, final String realname, final String host, final Channel chan, final UserChannelPermission chanRole) {
        final User selectedUser = getUser(nick);
        User theUser;
        if (selectedUser == null) {
            theUser = new User(nick, name, realname, host, chan, chanRole);
            this.getUserList().add(theUser);
        } else {
            theUser = selectedUser;
            if (StringUtils.isNotBlank(name)) theUser.setName(name);
            if (StringUtils.isNotBlank(realname)) theUser.setRealname(realname);
            if (StringUtils.isNotBlank(host)) theUser.setHost(host);
            if (chan != null && !theUser.getChannels().containsKey(chan)) {
                theUser.getChannels().put(chan, chanRole);
            } else if (chan != null && theUser.getChannels().containsKey(chan) && theUser.getChannels().get(chan).equals(UserChannelPermission.UNKNOWN)) {
                theUser.getChannels().put(chan, chanRole);
            } else if (chan != null && chanRole != UserChannelPermission.UNKNOWN) {
                theUser.getChannels().put(chan, chanRole);
            }
        }
        return theUser;
    }

    /**
	 * add user object directly (used in unit tests)
	 * @param user
	 */
    protected void addUser(User user) {
        this.getUserList().add(user);
    }

    /**
	 * Remove a user from the UW and destroy the object. Phasers on kill.
	 * @param nick
	 */
    public void removeUser(String nick) {
        User selectedUser = this.getUser(nick);
        final ArrayList<String> chanList = Controller.getInstance().getCWatcher().chanList();
        for (String currChan : chanList) {
            final Channel myChan = Controller.getInstance().getCWatcher().getChan(currChan);
            if (myChan.getUserList().contains(selectedUser)) {
                myChan.getUserList().remove(selectedUser);
            }
        }
        if (selectedUser != null) {
            this.getUserList().remove(selectedUser);
        }
        selectedUser = null;
    }

    /**
	 * Changes a users nick.
	 * @param nickOld
	 * @param nickNew
	 */
    protected void nickChange(String nickOld, String nickNew) {
        for (User user : this.getUserList()) {
            if (user.getNick().equalsIgnoreCase(nickOld)) {
                user.setNick(nickNew);
                break;
            }
        }
    }

    /**
	 * check if the user still is somewhere visible for us, if not, destroy him.
	 * @param nick
	 * @param chan
	 */
    public void userParted(String nick, Channel chan) {
        User selectedUser = getUser(nick);
        if (selectedUser != null) {
            if (selectedUser.getChannels().containsKey(chan)) {
                selectedUser.getChannels().remove(chan);
            }
            chan.getUserList().remove(selectedUser);
            if (selectedUser.getChannels().isEmpty()) {
                this.getUserList().remove(selectedUser);
                selectedUser = null;
            }
        }
    }

    /**
	 * If a user joins a channel, we check if it already exists and the chan needs to be added.
	 * @param nick
	 * @param chan
	 */
    public void userJoined(String nick, Channel chan) {
        final User selectedUser = getUser(nick);
        if (selectedUser == null) {
            this.getUserList().add(addUser(nick, chan));
        } else {
            if (!selectedUser.getChannels().containsKey(chan)) {
                selectedUser.getChannels().put(chan, UserChannelPermission.STANDARD);
            }
        }
        chan.getUserList().add(selectedUser);
    }

    /**
	 * Called when a user quits the network. Object can be destroyed.
	 * @param nick
	 */
    public void userQuits(String nick) {
        removeUser(nick);
    }

    /**
	 * get a certain user by it's nick
	 * @param nick
	 * @return user
	 */
    public User getUser(String nick) {
        for (User user : this.getUserList()) {
            if (user.getNick().equalsIgnoreCase(nick)) {
                return user;
            }
        }
        return null;
    }

    private List<User> getUserList() {
        return userList;
    }
}
