package de.boardgamesonline.bgo2.webserver.test.model;

import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import junit.framework.TestCase;
import de.boardgamesonline.bgo2.webserver.model.ChatChannel;
import de.boardgamesonline.bgo2.webserver.model.DataProvider;
import de.boardgamesonline.bgo2.webserver.model.Game;
import de.boardgamesonline.bgo2.webserver.model.User;

/**
 * 
 *
 */
public class GameTest extends TestCase implements Observer {

    /**
	 * State variable used in the observer tests.
	 */
    private Game.State state = Game.State.OPEN;

    /**
	 * A user object used in several tests.
	 */
    private User u1;

    /**
	 * Another user object.
	 */
    private User u2;

    /**
	 * @throws Exception something went wrong
	 */
    protected void setUp() throws Exception {
        u1 = new User("user1", "pass", "mail");
        u2 = new User("user2", "pass", "main");
    }

    /**
	 * @throws java.lang.Exception If the teardown process fails.
	 */
    public void tearDown() throws Exception {
        if (DataProvider.getInstance().getHibernateSessionFactory() != null) {
            DataProvider.getInstance().destroy();
        }
    }

    /**
	 * Tests the getMaxPlayers() method.
	 */
    public void testGetMaxPlayers() {
        Game g = new Game(u1, "type", "name", 10, true);
        assertEquals(g.getMaxPlayers(), 10);
    }

    /**
	 * Tests the setMaxPlayers() method.
	 */
    public void testSetMaxPlayers() {
        Game g = new Game(u1, "type", "name", 10, true);
        g.setMaxPlayers(2);
        assertEquals(g.getMaxPlayers(), 2);
    }

    /**
	 * Tests the getName() method.
	 */
    public void testGetName() {
        Game g = new Game(u1, "type", "name", 10, true);
        assertEquals(g.getName(), "name");
    }

    /**
	 * Tests the getType() method.
	 */
    public void testGetType() {
        Game g = new Game(u1, "type", "name", 10, true);
        assertEquals(g.getType(), "type");
    }

    /**
	 * Tests the getSeats() method.
	 */
    public void testGetSeats() {
        Game g = new Game(u1, "type", "name", 5, true);
        assertEquals(g.getSeats(), "0/5");
        g.addUser(u2);
        assertEquals(g.getSeats(), "1/5");
    }

    /**
	 * Tests the addUser() method.
	 */
    public void testAddUser() {
        Game g = new Game(u1, "type", "name", 2, true);
        assertFalse("isInGame() returned true although user was not added", g.isInGame(u1));
        assertTrue("addUser() returned false", g.addUser(u2));
        assertTrue("isInGame() returned false although user was added", g.isInGame(u2));
    }

    /**
	 * Tests the isSpectateable method.
	 */
    public void testIsSpectateable() {
        Game g = new Game(u1, "type", "name", 10, true);
        assertTrue(g.isSpectateable());
    }

    /**
	 * Tests the setSpectateable method.
	 */
    public void testSetSpectateable() {
        Game g = new Game(u1, "type", "name", 10, true);
        g.setSpectateable(false);
        assertFalse(g.isSpectateable());
    }

    /**
     * Tests the addSpectator method.
     */
    public void testAddSpectator() {
        Game g = new Game(u1, "type", "name", 10, true);
        assertTrue(g.addSpectator(u1));
        Game g2 = new Game(u1, "type", "name", 10, false);
        assertFalse(g2.addSpectator(u1));
    }

    /**
	 * Tests the setStatus() method.
	 */
    public void testSetStatus() {
        Game g = new Game(u1, "type", "name", 10, true);
        state = Game.State.OPEN;
        g.addObserver(this);
        g.setStatus(Game.State.STARTED);
        assertEquals(g.getStatus(), state);
        assertEquals(g.getStatus(), Game.State.STARTED);
        g.setStatus(Game.State.TERMINATED);
        assertEquals(g.getStatus(), state);
        assertEquals(g.getStatus(), Game.State.TERMINATED);
    }

    /**
	 * Tests the getSession() method.
	 */
    public void testGetSession() {
        Game g = new Game(u1, "type", "name", 10, true);
        assertNotNull(g.getSession());
    }

    /**
	 * Tests the isInGame() method.
	 */
    public void testIsInGame() {
        Game g = new Game(u1, "type", "name", 10, true);
        assertTrue("addUser() returned false", g.addUser(u2));
        assertTrue("user not in game although added", g.isInGame(u2));
    }

    /**
	 * Tests the removeUser() method.
	 */
    public void testRemoveUser() {
        Game g = new Game(u1, "type", "name", 10, true);
        state = Game.State.OPEN;
        g.addObserver(this);
        assertFalse("removeUser() returned true although there are no users", g.removeUser(u2));
        assertEquals("state changed", state, Game.State.OPEN);
        assertTrue("addUser() returned false", g.addUser(u2));
        assertTrue("removeUser() returned false", g.removeUser(u2));
        assertEquals("state changed", state, Game.State.OPEN);
        assertFalse("isInGame() returned true although user was removed", g.isInGame(u2));
    }

    /**
	 * Tests the IsCreator() method.
	 */
    public void testIsCreator() {
        Game g = new Game(u1, "type", "name", 10, true);
        assertTrue("isCreator() returned false for the creator", g.isCreator(u1));
    }

    /**
     * Tests is isFull() method.
     */
    public void testIsFull() {
        Game g = new Game(u1, "type", "name", 2, true);
        assertFalse("isFull() returned true for empty game", g.isFull());
        g.addUser(u1);
        g.addUser(u2);
        assertTrue("isFull() returned false for full game", g.isFull());
    }

    /**
     * Tests the associated chat.
     */
    public void testGetChannel() {
        Game g = new Game(u1, "type", "someothername", 10, true);
        ChatChannel channel = g.getChannel();
        assertNotNull("Chat channel is null", channel);
        assertFalse("creator initally member of chat channel", channel.isMember(u1));
        assertTrue("Chat channel contains users in an empty game", channel.getMembers().isEmpty());
        g.addUser(u1);
        assertTrue("master user not member of chat channel", channel.isMember(u1));
        g.addUser(u2);
        assertTrue("user not member of chat channel", channel.isMember(u2));
        assertEquals("Chat channel contains a wrong number of users", channel.getMembers().size(), 2);
    }

    /**
     * Tests the saving of games.
     */
    public void testSave() {
        DataProvider.getInstance().init("jdbc:hsqldb:mem:bgowebserverinittestdb", null);
        Game g = new Game(u1, "type", "someothername", 10, true);
        g.save();
        g = new Game(u1, "type", "someothername2", 10, true);
        g.save();
        Set<Game> games = DataProvider.getInstance().getGames(u1);
        assertEquals("Number of retrieved games not correct", 2, games.size());
        assertEquals("Retrieved games' creator wrong", u1, games.iterator().next().getCreator());
        games = DataProvider.getInstance().getGames(u2);
        assertEquals("Retrieved game wherer there is none", 0, games.size());
    }

    /**
     * Method for the observation test.
	 * @param o o
	 * @param arg arg
	 */
    public void update(Observable o, Object arg) {
        state = (Game.State) arg;
    }
}
