package de.boardgamesonline.bgo2.webserver.wicket;

import de.boardgamesonline.bgo2.webserver.model.ChatChannel;
import de.boardgamesonline.bgo2.webserver.model.DataProvider;
import de.boardgamesonline.bgo2.webserver.model.User;
import wicket.protocol.http.WebApplication;
import wicket.protocol.http.WebSession;

/**
 * Subclass of WebSession for BGOApplication to allow easy and typesafe access
 * to session properties.
 * 
 * @author Ralf Niehaus/ Marc Lindenberg
 */
public class BGOSession extends WebSession {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    /**
	 * the Session's User Object
	 */
    private User user;

    /**
	 * the messag count for the lobby chat
	 */
    private int messageCount;

    /**
	 * Constructor
	 * 
	 * @param application
	 *            The application
	 */
    public BGOSession(final WebApplication application) {
        super(application);
    }

    /**
	 * Checks the given username and password, returning a User object if if the
	 * username and password identify a valid user.
	 * 
	 * @param username
	 *            The username
	 * @param password
	 *            The password
	 * @return True if the user was authenticated
	 */
    public boolean authenticate(String username, String password) {
        user = ((BGOApplication) getApplication()).getDATA().loginUser(username, password, this.getId());
        this.messageCount = Math.max(ChatChannel.getChannel(null).getMessages().size() - 10, 0);
        if (user != null) {
            ChatChannel.getChannel(null).postMessage(user.getName() + " has joined the Lobby");
            return true;
        }
        return false;
    }

    /**
	 * 
	 * @return true if the user is logged in
	 */
    public boolean loggedIn() {
        return user != null;
    }

    /**
	 * This method is used to log the user out, the sessons {@link User} object
	 * is set to <code>null</code>
	 * 
	 * @return <code>True</code> if the user was logged out
	 */
    public boolean logout() {
        ChatChannel.getChannel(null).postMessage(user.getName() + " has left the Lobby");
        DataProvider.getInstance().getPlayerList().remove(this.getId());
        user = null;
        return user == null;
    }

    /**
	 * @return the logged in user
	 */
    public synchronized User getUser() {
        return user;
    }

    /**
	 * 
	 * @return the message count of the lobby chat 
	 * 			of the moment when the user logged in
	 */
    public int getMessageCount() {
        return messageCount;
    }
}
