package de.boardgamesonline.bgo2.webserver.model;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Vector;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.annotations.Index;

/**
 * This class represents the server-side state of a game.
 * 
 * @author BGO2 Webserverteam
 */
@Entity
public class Game extends Observable implements SessionObject, Serializable {

    /**
	 * Our UID. yay!
	 */
    private static final long serialVersionUID = 2L;

    /**
	 * the list of the users in the game
	 */
    @Transient
    private List<User> userList;

    /**
     * the list spectator users in the game.
     */
    @Transient
    private List<User> spectatorList;

    /**
	 * the type of the game could be:
	 */
    @Basic
    private String type;

    /**
	 * the gamename
	 */
    @Basic
    private String name;

    /**
     * the creator of the game
     */
    @Transient
    private User creator;

    /**
	 * the name of the creator of the game
	 */
    @SuppressWarnings("unused")
    @Index(name = "creatorNameIndex")
    @Basic
    private String creatorName;

    /**
	 * the maximum number of players in the game
	 */
    @Basic
    private int maxPlayers;

    /**
	 * The possible states of the game.
	 */
    public enum State {

        /**
		 * Indicates that the game is open for users to join.
		 */
        OPEN, /**
		 * Indicates that the game is running
		 */
        STARTED, /**
		 * Indicates that the game has finished and its object can be destroyed.
		 */
        TERMINATED
    }

    ;

    /**
	 * The game's state.
	 */
    @Basic
    private State status;

    /**
	 * Indicates if the game is spectateable.
	 */
    @Transient
    private boolean spectateable;

    /**
	 * The game's password
	 */
    @Transient
    private String password;

    /**
	 * The session used for identification by the
     * {@link de.boardgamesonline.bgo2.webserver.axis.GameManager}.
	 */
    @Id
    private String session;

    /**
	 * The exact date and time this game was created.
	 */
    @Temporal(value = TemporalType.TIMESTAMP)
    @Basic
    private Date created;

    /**
	 * The converter to transform <code>created</code> into a String.
	 */
    private static final DateFormat CREATED_FORMATTER = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss");

    /**
	 * The game data of a savegame. This is provided by the game server.
	 */
    @Lob
    private String gameData;

    /**
     * The associated chat channel.
     */
    @Transient
    private transient ChatChannel channel;

    /**
     * The chat channel users list.
     */
    @Transient
    private transient Collection<User> chatUsers;

    /**
	 * No-argument constructor as required by hibernate.
	 */
    private Game() {
        userList = new Vector<User>();
        spectatorList = new Vector<User>();
    }

    /**
	 * Creates a game object.
	 * @param creator
	 *            the user who created the game
	 * @param type
	 *            the type of the game
	 * @param name
	 *            a name or short description of the game
	 * @param maxPlayers
	 *            the maximum number of players in the game
	 * @param spectateable
	 *            whether the game allows spectators
	 */
    public Game(User creator, String type, String name, int maxPlayers, boolean spectateable) {
        super();
        userList = new Vector<User>();
        spectatorList = new Vector<User>();
        setType(type);
        setName(name);
        setMaxPlayers(maxPlayers);
        setCreator(creator);
        setSpectateable(spectateable);
        setStatus(Game.State.OPEN);
        setPassword(null);
        setCreated(new Date());
        setSession(randomString(20));
    }

    /**
     * Generates a random string.
	 * @param length the length of the random string to create.
	 * @return a random String with a length of <code>length</code>.
	 */
    private static String randomString(int length) {
        byte[] b = new byte[length];
        for (int i = 0; i < length; i++) {
            b[i] = (byte) (Math.random() * ('z' - 'a') + 'a');
        }
        return new String(b);
    }

    /**
     * Sets the Game's name.
     * 
	 * @param name the name to set
	 */
    private synchronized void setName(String name) {
        this.name = name;
        if (channel != null) {
            channel.postMessage("The game's name is now " + name);
        }
    }

    /**
	 * Called by the Garbage Collector just before the object is destroyed.
	 * The expectation was that this will remove the associated ChatChannel
	 * soon enough / at all. Testing suggests that it didn't.
	 * Therefore, finalize() is called explicitly
	 * from {@link GameList#remove(String)}, for now.)
	 */
    protected void finalize() {
        if (channel != null) {
            ChatChannel.removeChannel(channel);
            channel = null;
        }
    }

    /**
	 * @param type the type to set
	 */
    private synchronized void setType(String type) {
        this.type = type;
        if (channel != null) {
            channel.postMessage("The game's type is now " + type);
        }
    }

    /**
	 * @return the creator
	 */
    public synchronized User getCreator() {
        return creator;
    }

    /**
	 * @param creator the creator to set
	 */
    public synchronized void setCreator(User creator) {
        this.creator = creator;
        this.creatorName = (creator == null ? null : creator.getName());
        if (channel != null) {
            channel.postMessage("The game's creator is now " + creatorName);
        }
    }

    /**
     * Returns the maximum number of players in the game.
	 * @return the maxPlayers
	 */
    public synchronized int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Sets the maximum number of players in the game.
	 * @param maxPlayers
	 *            the maxPlayers to set
	 */
    public synchronized void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
        if (channel != null) {
            channel.postMessage("Maximum players is now " + maxPlayers);
        }
    }

    /**
     * Returns the name of the game.
	 * @return the name
	 */
    public synchronized String getName() {
        return name;
    }

    /**
     * Returns the type name of the game.
	 * @return the type
	 */
    public synchronized String getType() {
        return type;
    }

    /**
	 * Adds a user to the player list.
	 * 
	 * @param newUser a user to join the game
	 * @return <code>true</code> if the user could be added,
     *         <code>false</code> otherweise. 
	 */
    public synchronized boolean addUser(User newUser) {
        if (userList.add(newUser)) {
            chatUsers.add(newUser);
            if (channel != null) {
                channel.postMessage("New player " + newUser.getName());
            }
            return true;
        }
        return false;
    }

    /**
     * Adds a user to the spectator list.
     * @param newSpectator a user to view the game.
     * @return <code>true</code> if the user was added to the list,
     *         <code>false</code> otherwise.
     */
    public synchronized boolean addSpectator(User newSpectator) {
        if (spectateable && spectatorList.add(newSpectator)) {
            chatUsers.add(newSpectator);
            if (channel != null) {
                channel.postMessage("New spectator " + newSpectator.getName());
            }
            return true;
        }
        return false;
    }

    /**
	 * Removes a user from the game. This version of the method also
     * removes spectator users.
	 * @param user The user to be removed from the game.
	 * @return <code>true</code> if the user could be removed,
     *         <code>false</code> otherwise.
	 */
    public synchronized boolean removeUser(User user) {
        if (userList.remove(user) || spectatorList.remove(user)) {
            chatUsers.remove(user);
            if (channel != null) {
                channel.postMessage("User left: " + user.getName());
            }
            return true;
        }
        return false;
    }

    /**
     * Indicates if the game is allowed to have spectators.
	 * @return the spectateable
	 */
    public synchronized boolean isSpectateable() {
        return spectateable;
    }

    /**
     * Sets the game's spectateability.
	 * @param spectateable
	 *            the spectateable to set
	 */
    public synchronized void setSpectateable(boolean spectateable) {
        final boolean wasSpectateable = this.spectateable;
        this.spectateable = spectateable;
        if (channel != null && wasSpectateable != spectateable) {
            channel.postMessage("The game is now " + (spectateable ? "" : "not ") + "spectateable");
        }
    }

    /**
     * Returns the current state.
	 * @return the status
	 */
    public synchronized State getStatus() {
        return status;
    }

    /**
     * Sets the game's state and notifies the observers.
 	 *  
	 * @param status
	 *            the status to set
	 */
    public synchronized void setStatus(State status) {
        this.status = status;
        setChanged();
        notifyObservers(status);
        if (channel != null) {
            channel.postMessage("The game's status is now " + status.toString());
        }
    }

    /**
     * Returns a list of the users currently being in the game.
	 * @return the userList
	 */
    public synchronized List getUserList() {
        return userList;
    }

    /**
	 * Returns the game's session ID.
	 * @return The game's session ID.
	 */
    public synchronized String getSession() {
        return this.session;
    }

    /**
	 * Builds a string listing the users of a specific type,
	 * for use with the associated channel.
	 * 
	 * @param usersType A description of what type of users are listed.
	 * @param users     The list of users to stringify.
	 * @return The built message.
	 */
    private String buildUsersMessage(final String usersType, final List<User> users) {
        final StringBuilder str = new StringBuilder(20 * users.size());
        str.append("There are ");
        str.append(users.size());
        str.append(" " + usersType);
        if (!users.isEmpty()) {
            str.append(": ");
            final Iterator<User> i = users.iterator();
            str.append(i.next().getName());
            while (i.hasNext()) {
                str.append(", " + i.next().getName());
            }
        }
        return str.toString();
    }

    /**
	 * Sets the game's session ID and initializes the associated channel.
	 * 
	 * @param session the session to set
	 */
    private synchronized void setSession(String session) {
        this.session = session;
        chatUsers = new Vector<User>();
        chatUsers.addAll(userList);
        chatUsers.addAll(spectatorList);
        channel = ChatChannel.createChannel(ChatChannel.GAME_CHAN_PREFIX + session, chatUsers);
        if (channel == null) {
            ;
        } else {
            channel.postMessage((type == null ? "(unknown)" : type) + " game " + (name == null ? "(unknown name)" : "'" + name.replace("\\", "\\\\").replace("'", "\\'") + "'") + " created " + (created == null ? "(unknown)" : CREATED_FORMATTER.format(created)) + " by " + (creatorName == null ? "(unknown)" : creatorName));
            channel.postMessage("The game has a maximum of " + maxPlayers + " players, is " + (isPasswordProtected() ? "" : "not ") + "password protected and is " + (spectateable ? "" : "not ") + "spectateable");
            if (!userList.isEmpty()) {
                channel.postMessage(buildUsersMessage("users", userList));
            }
            if (!spectatorList.isEmpty()) {
                channel.postMessage(buildUsersMessage("spectators", spectatorList));
            }
            channel.postMessage("Status is " + status.toString());
        }
    }

    /**
	 * @return the gameData
	 */
    public synchronized String getGameData() {
        return gameData;
    }

    /**
	 * @param gameData the gameData to set
	 */
    public synchronized void setGameData(String gameData) {
        this.gameData = gameData;
    }

    /**
	 * Indicates if the specified player is in the game.
	 * @param u
	 *            The user to check.
	 * @return <code>true</code> if the player is in the game,
	 *         <code>false</code> otherwise.
	 */
    public boolean isInGame(User u) {
        if (userList != null) {
            return userList.contains(u);
        }
        return false;
    }

    /**
	 * Returns a string representing current and maximum player numbers.
	 * @return A <code>String</code> representing the number of players in the
	 *         game and the maximum players in the format "players in
	 *         game"/"maximum players"
	 */
    public String getSeats() {
        return getUserList().size() + "/" + getMaxPlayers();
    }

    /**
     * Returns the game's password (if set).
	 * @return the password the game's password
	 */
    public synchronized String getPassword() {
        return password;
    }

    /**
     * Sets the game's password.
	 * @param password
	 *            the password to set
	 */
    public synchronized void setPassword(String password) {
        final boolean wasProtected = isPasswordProtected();
        this.password = password;
        final boolean isProtected = isPasswordProtected();
        if (channel != null && wasProtected != isProtected) {
            channel.postMessage("The game is now " + (isProtected ? "" : "not ") + "password protected");
        }
    }

    /**
	 * @return the created
	 */
    public synchronized Date getCreated() {
        return this.created;
    }

    /**
	 * Sets the game's creation date.
	 * 
	 * @param created the created to set
	 */
    private synchronized void setCreated(Date created) {
        this.created = created;
        if (channel != null) {
            channel.postMessage("Game created at " + CREATED_FORMATTER.format(created));
        }
    }

    /**
	 * Indicates if the game is password protected.
	 * @return <code>true</code> if the game is protected with a password,
     *         <code>false</code> if no password was set.
	 */
    public boolean isPasswordProtected() {
        if (password == null || "".equals(password)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if <code>u</code> is the creator of this game. 
     * @param u The user to check.
     * @return Whether u is the creator of this game.
     */
    public boolean isCreator(User u) {
        return creator.equals(u);
    }

    /**
	 * @param obj
	 *            The object to test
	 * 
	 * @return <code>True</code> if <code>obj</code> is a {@link Game} with
	 *         the same session
	 */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Game) {
            return ((Game) obj).getSession().equals(getSession());
        }
        return false;
    }

    /**
	 * @return the hascode of the session
	 */
    @Override
    public int hashCode() {
        return getSession().hashCode();
    }

    public boolean isFull() {
        return getMaxPlayers() <= getUserList().size();
    }

    /**
     * Returns the associated chat channel.
     * @return The associated chat channel.
     */
    public ChatChannel getChannel() {
        return channel;
    }

    /**
     * Saves a Game to the database.
     * @return True iff saving was successful.
     */
    public synchronized boolean save() {
        Session hibernateSession = DataProvider.getInstance().getHibernateSessionFactory().getCurrentSession();
        hibernateSession.beginTransaction();
        try {
            hibernateSession.saveOrUpdate(this);
            hibernateSession.getTransaction().commit();
            return true;
        } catch (HibernateException e) {
            hibernateSession.getTransaction().rollback();
            return false;
        }
    }
}
