package Networking.server;

import java.io.IOException;
import java.util.ArrayList;

public class Channel {

    private boolean closeIfEmpty = true;

    private ArrayList<User> users = null;

    private String name = null;

    /**
	 * Create a new channel with closeIfEmpty set to true
	 * @param n The name of the channel
	 * @param u A list of users currently occupying the channel
	 */
    public Channel(String n, ArrayList<User> u) {
        this(n, u, true);
    }

    /**
	 * Create a new channel
	 * @param n The name of the channel
	 * @param u A list of users currently occupying the channel
	 * @param cie Determines whether this channel closes if empty
	 */
    public Channel(String n, ArrayList<User> u, boolean cie) {
        users = u;
        name = n;
        closeIfEmpty = cie;
    }

    /**
	 * @return returns whether this channel closes if empty
	 */
    public boolean isCloseIfEmpty() {
        return closeIfEmpty;
    }

    /**
	 * @param closeIfEmpty determines whether this channel will close if no one remains - used to keep the lobby channel up and running
	 */
    public void setCloseIfEmpty(boolean closeIfEmpty) {
        this.closeIfEmpty = closeIfEmpty;
    }

    /**
	 * @return the list of users currently occupying the channel
	 */
    public ArrayList<User> getUsers() {
        return users;
    }

    /**
	 * @param users the new list of users in the channel
	 */
    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    /**
	 * @return the channel name
	 */
    public String getName() {
        return name;
    }

    /**
	 * @param name the new channel name
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Remove a user from its current channel and add it to this one
     * @param u The user to add
     */
    public void addUser(User u) {
        if (u.getChannel() != null) u.getChannel().removeUser(u);
        u.setChannel(this);
        for (User ou : users) try {
            ServerThread.broadcast(u, "adduser " + ou.getUserName(), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        users.add(u);
        channelcast("adduser " + u.getUserName(), 0);
        channelcast(u.getUserName() + " has joined the channel.", 2);
    }

    /**
     * Remove a user from this channel - if a user is removed from all channels they will no longer be able to chat, but can still create/join channels and games unless flagged otherwise
     * @param u The user to remove
     */
    public void removeUser(User u) {
        users.remove(u);
        u.setChannel(null);
        channelcast("removeuser " + u.getUserName(), 0);
        channelcast(u.getUserName() + " has left the channel.", 2);
    }

    /**
     * Send a message to all channel occupants
     * @param s The message to send
     */
    public void channelcast(String s, int i) {
        if (!users.isEmpty()) {
            for (User u : users) try {
                ServerThread.broadcast(u, s, i);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove a user without displaying a message
     * @param u The user to remove
     */
    public void removeUserQuietly(User u) {
        users.remove(u);
        channelcast("removeuser " + u.getUserName(), 0);
    }
}
