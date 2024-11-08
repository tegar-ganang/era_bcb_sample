package de.boardgamesonline.bgo2.webserver.test.axis;

import java.util.Arrays;
import java.util.List;
import de.boardgamesonline.bgo2.webserver.axis.AccessDeniedException;
import de.boardgamesonline.bgo2.webserver.axis.ChatManager;
import de.boardgamesonline.bgo2.webserver.model.ChatMessage;
import de.boardgamesonline.bgo2.webserver.model.DataProvider;
import de.boardgamesonline.bgo2.webserver.model.Game;
import de.boardgamesonline.bgo2.webserver.model.GameList;
import de.boardgamesonline.bgo2.webserver.model.PlayerList;
import de.boardgamesonline.bgo2.webserver.model.User;
import junit.framework.TestCase;

/**
 * A test case for the ChatManager class.
 * @author Ulrich Rhein
 */
public class ChatManagerTest extends TestCase {

    /**
     * A user used in several tests.
     */
    private User u1;

    /**
     * Another user.
     */
    private User u2;

    /**
     * Yet another user.
     */
    private User u3;

    /**
     * A game used in several tests.
     */
    private Game g;

    /**
     * The game list.
     */
    private GameList glist;

    /**
     * The player list.
     */
    private PlayerList plist;

    /**
     * Our chat manager object.
     */
    private ChatManager cm;

    /**
     * Constructor to keep test data that will be invariant
     * during and between tests out of setUp().
     */
    public ChatManagerTest() {
        DataProvider dp = DataProvider.getInstance();
        cm = new ChatManager();
        glist = dp.getGameList();
        plist = dp.getPlayerList();
        u1 = new User("user1", "pass", "mail", "user1");
        u2 = new User("user2", "pass", "mail", "user2");
        u3 = new User("user3", "pass", "mail", "user3");
        plist.add(u1);
        plist.add(u2);
        plist.add(u3);
    }

    /**
     * Sets up common objects.
     * @throws Exception Possibly thrown by JUnit's super class.
     */
    public void setUp() throws Exception {
        super.setUp();
        g = new Game(u1, "alhambra", "name1", 8, false);
        glist.add(g);
        g.addUser(u1);
    }

    /**
     * Cleans up after a test.
     * @throws Exception Possibly thrown by JUnit's super class.
     */
    public void tearDown() throws Exception {
        super.tearDown();
        glist.remove(g);
    }

    /**
     * Tests whether the postMessage() method works at all / if used properly.
     */
    public void testPostMessageWorks() {
        try {
            assertTrue("postMessage() returned false", cm.postMessage("user1", g.getSession(), "foobar"));
        } catch (AccessDeniedException e) {
            fail("Got access denied exception.");
        }
    }

    /**
     * Tests whether postMessage() raises an AccessDenied exception when appropriate.
     */
    public void testPostMessageAccessDenied1() {
        try {
            cm.postMessage("user2", g.getSession(), "foobar");
            fail("Should have got an access denied exception. (User not in channel)");
        } catch (AccessDeniedException e) {
            ;
        }
    }

    /**
     * Tests whether postMessage() raises an AccessDenied exception when appropriate.
     */
    public void testPostMessageAccessDenied2() {
        try {
            cm.postMessage("user1", "1 2 3", "foobar");
            fail("Should have got an access denied exception. (Channel does not exist)");
        } catch (AccessDeniedException e) {
            ;
        }
    }

    /**
     * Tests whether the getNewMessages() method works at all / if used properly.
     */
    public void testGetNewMessagesWorks() {
        final String gSession = g.getSession();
        try {
            if (cm.getNewMessages("user1", gSession) == null) {
                fail("getNewMessages() returned null while preparing the real test");
            }
        } catch (AccessDeniedException e1) {
            fail("Got AccessDeniedException while preparing the real test");
        }
        testPostMessageWorks();
        final List<ChatMessage> msgs = g.getChannel().getMessages();
        String[] expected = { msgs.get(msgs.size() - 1).toString() };
        try {
            String[] result = cm.getNewMessages("user1", gSession);
            assertTrue("Unexpected getNewMessages() result, was " + Arrays.deepToString(result) + ", not " + Arrays.deepToString(expected), Arrays.deepEquals(result, expected));
        } catch (AccessDeniedException e) {
            fail("Got access denied exception.");
        }
    }

    /**
     * Tests whether getNewMessages() raises an AccessDenied exception when appropriate.
     */
    public void testGetNewMessagesAccessDenied1() {
        try {
            cm.getNewMessages("user2", g.getSession());
            fail("Should have got an access denied exception. (User not in channel)");
        } catch (AccessDeniedException e) {
            ;
        }
    }

    /**
     * Tests whether getNewMessages() raises an AccessDenied exception when appropriate.
     */
    public void testGetNewMessagesAccessDenied2() {
        try {
            cm.getNewMessages("user1", "1 2 3");
            fail("Should have got an access denied exception. (Channel does not exist)");
        } catch (AccessDeniedException e) {
            ;
        }
    }

    /**
     * Tests whether getNewMessages() really returns messages incrementally.
     */
    public void testGetNewMessagesIncrementally() {
        testGetNewMessagesWorks();
        try {
            assertTrue("postMessage() returned false", cm.postMessage("user1", g.getSession(), "blubb"));
        } catch (AccessDeniedException e) {
            fail("Got access denied exception while posting another message.");
            return;
        }
        final List<ChatMessage> msgs = g.getChannel().getMessages();
        String[] expected = { msgs.get(msgs.size() - 1).toString() };
        try {
            String[] result = cm.getNewMessages("user1", g.getSession());
            assertTrue("Unexpected getNewMessages() result, was " + Arrays.deepToString(result) + ", not " + Arrays.deepToString(expected), Arrays.deepEquals(result, expected));
        } catch (AccessDeniedException e) {
            fail("Got access denied exception while retrieving new messages.");
        }
    }
}
