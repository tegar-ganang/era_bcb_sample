package de.boardgamesonline.bgo2.webserver.test.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import junit.framework.TestCase;
import de.boardgamesonline.bgo2.webserver.model.ChatChannel;
import de.boardgamesonline.bgo2.webserver.model.ChatMessage;
import de.boardgamesonline.bgo2.webserver.model.User;
import de.boardgamesonline.bgo2.webserver.model.ChatChannel.Changed;

/**
 * JUnit test case for {@link ChatChannel}.
 * 
 * @author Fabian Pietsch
 *
 */
public class ChatChannelTest extends TestCase {

    /** Name for main channel */
    private static final String MAIN_CHANNEL_NAME = "test";

    /** Name for other channel */
    private static final String OTHER_CHANNEL_NAME = "other";

    /** Main channel for tests */
    private ChatChannel mainChannel;

    /** Other channel for some tests */
    private ChatChannel otherChannel;

    /** Members of main channel */
    private Collection<User> mainMembers;

    /** Members of other channel */
    private Collection<User> otherMembers;

    /** Some test user */
    private User firstUser;

    /** Some test user */
    private User secondUser;

    /** Some test user */
    private User otherUser;

    /** Messages posted during some tests */
    private List<ChatMessage> messages;

    /** Test message 1 */
    private static final String MSG1 = "message one";

    /** Test message 2 */
    private static final String MSG2 = "second mesage ...";

    /** Test message 3 */
    private static final String MSG3 = "This should fail.";

    /** Test system message 1 */
    private static final String SYSMSG1 = "Channel created";

    /** Test system message 2 */
    private static final String SYSMSG2 = "Game created";

    /**
	 * Prepares the common environment before each test.
	 * @throws Exception Might be thrown by superclass method.
	 * @see junit.framework.TestCase#setUp()
	 */
    protected void setUp() throws Exception {
        super.setUp();
        firstUser = new User("nick1", null, null);
        secondUser = new User("nick2", null, null);
        otherUser = new User("Other User", null, null);
        mainMembers = new HashSet<User>();
        mainMembers.add(firstUser);
        mainMembers.add(secondUser);
        otherMembers = new HashSet<User>(mainMembers);
        otherMembers.remove(1);
        otherMembers.add(otherUser);
        mainChannel = ChatChannel.createChannel(MAIN_CHANNEL_NAME, mainMembers);
    }

    /**
	 * Cleans up the common environment after each test.
	 * @throws Exception Might be thrown by superclass method.
	 * @see junit.framework.TestCase#tearDown()
	 */
    protected void tearDown() throws Exception {
        ChatChannel.removeChannel(MAIN_CHANNEL_NAME);
        super.tearDown();
    }

    /**
	 * Test method for
	 * {@link ChatChannel#createChannel(String, java.util.Collection)}.
	 */
    public void testCreateChannel() {
        otherChannel = ChatChannel.createChannel(OTHER_CHANNEL_NAME, otherMembers);
        assertNotNull("A channel should have been created and returned.", otherChannel);
        assertEquals("Auto-posted message at channel creation time missing", otherChannel.getMessages().size(), 1);
        otherChannel = ChatChannel.createChannel(OTHER_CHANNEL_NAME, otherMembers);
        assertNull("A channel should not have been created because it already" + " existed. Null should have been returned.", otherChannel);
    }

    /**
	 * Test method for {@link ChatChannel#removeChannel(java.lang.String)}.
	 */
    public void testRemoveChannel() {
        boolean removed = ChatChannel.removeChannel(MAIN_CHANNEL_NAME);
        assertTrue("Return value should indicate that main channel was removed.", removed);
        assertNull("Main channel should really have been removed.", ChatChannel.getChannel(MAIN_CHANNEL_NAME));
        assertNotNull("A new channel with the removed one's ID should be creatible.", ChatChannel.createChannel(MAIN_CHANNEL_NAME, null));
    }

    /**
	 * Test method for {@link ChatChannel#removeChannel(ChatChannel)}.
	 */
    public void testRemoveChannelObject() {
        boolean removed = ChatChannel.removeChannel(mainChannel);
        assertTrue("Main channel should have been removed.", removed);
    }

    /**
	 * Test method for {@link ChatChannel#getChannel(java.lang.String)}.
	 */
    public void testGetChannel() {
        ChatChannel channel = ChatChannel.getChannel(MAIN_CHANNEL_NAME);
        assertEquals("Main channel should be retrievable via getChannel().", mainChannel, channel);
    }

    /**
	 * Test method for {@link ChatChannel#getMembers()}.
	 */
    public void testIsMember() {
        assertTrue("Main channel should report first user as member.", mainChannel.isMember(firstUser));
        assertFalse("Main channel should NOT report other user as member.", mainChannel.isMember(otherUser));
        mainMembers.remove(1);
        mainMembers.add(otherUser);
        assertTrue("Member list should support automatic updates.", mainChannel.isMember(otherUser));
        mainMembers.add(null);
        assertFalse("Null user shall never be a member.", mainChannel.isMember(null));
        mainChannel.setMembers(null);
        assertTrue("Null member list should mean no restrictions.", mainChannel.isMember(secondUser));
    }

    /**
	 * Test method for {@link ChatChannel#getMembers()}.
	 */
    public void testGetMembers() {
        assertEquals("Main channel's member list's content should be " + "as passed to createChannel().", mainMembers, mainChannel.getMembers());
    }

    /**
	 * Test method for {@link ChatChannel#setMembers(java.util.Collection)}.
	 */
    public void testSetMembers() {
        Collection<User> oldMembers = mainChannel.setMembers(otherMembers);
        assertEquals("Main channel's previous member list should match " + "its initial member list.", mainMembers, oldMembers);
        assertEquals("Main channel's new member list should match " + "the one passed to setMembers().", otherMembers, mainChannel.getMembers());
    }

    /**
	 * Test method for {@link ChatChannel#getID()}.
	 */
    public void testGetID() {
        String returnedID = mainChannel.getID();
        assertEquals("Main channel's channelID should equal the one " + "passed to createChannel().", MAIN_CHANNEL_NAME, returnedID);
    }

    /**
	 * Test method for {@link ChatChannel#getAllMessages()}.
	 */
    public void testGetAllMessages() {
        testPostMessageForUser();
        List<ChatMessage> chanMessages = mainChannel.getAllMessages();
        assertEquals(messages, chanMessages);
        try {
            ChatMessage oldMsg = chanMessages.remove(0);
            chanMessages.add(0, oldMsg);
        } catch (Exception e) {
            fail("Modifying the modifiable messages list should succeed.");
        }
        mainChannel.postMessage(firstUser, MSG2);
        assertEquals("This messages list should NOT be auto-updating.", messages, chanMessages);
    }

    /**
	 * Test method for {@link ChatChannel#getNewMessages(int)}.
	 */
    public void testGetNewMessages() {
        testPostMessageForUser();
        List<ChatMessage> subMessages = messages.subList(1, messages.size());
        List<ChatMessage> newMessages = mainChannel.getNewMessages(1);
        assertEquals(subMessages, newMessages);
        try {
            ChatMessage oldMsg = newMessages.remove(0);
            newMessages.add(0, oldMsg);
        } catch (Exception e) {
            fail("Modifying a modifiable messages list should succeed.");
        }
        mainChannel.postMessage(firstUser, MSG2);
        assertEquals("This messages list should NOT be auto-updating.", subMessages, newMessages);
    }

    /**
	 * Test method for {@link ChatChannel#getMessages()}.
	 */
    public void testGetMessages() {
        testPostMessageForUser();
        List<ChatMessage> chanMessages = mainChannel.getMessages();
        assertEquals(messages, chanMessages);
        try {
            chanMessages.add(new ChatMessage(null, null, ""));
            fail("Modifying the unmodifiable messages list should fail.");
        } catch (Exception e) {
            new Object();
        }
        try {
            chanMessages.remove(0);
            fail("Modifying the unmodifiable messages list should fail.");
        } catch (Exception e) {
            new Object();
        }
        mainChannel.postMessage(firstUser, MSG2);
        assertFalse("This messages list should be auto-updating.", messages.equals(chanMessages));
    }

    /**
	 * Test method for {@link ChatChannel#postMessage(User, java.lang.String)}.
	 */
    public void testPostMessageForUser() {
        messages = mainChannel.getAllMessages();
        messages.add(mainChannel.postMessage(firstUser, MSG1));
        messages.add(mainChannel.postMessage(secondUser, MSG2));
        assertFalse("Posting of the first two test messages should succeed.", messages.contains(null));
        assertEquals("Returned messages don't match those in channel.", messages, mainChannel.getAllMessages());
        ChatMessage msg = mainChannel.postMessage(otherUser, MSG3);
        assertNull("Posting to a channel for a user who is not a member should fail.", msg);
    }

    /**
	 * Test method for {@link ChatChannel#postMessage(java.lang.String)}.
	 */
    public void testPostMessageForSystem() {
        messages = mainChannel.getAllMessages();
        messages.add(mainChannel.postMessage(SYSMSG1));
        messages.add(mainChannel.postMessage(SYSMSG2));
        assertFalse("Posting of the test messages should succeed.", messages.contains(null));
        assertEquals("Returned messages don't match those in channel.", messages, mainChannel.getAllMessages());
    }

    /**
	 * Test method for ChatChannel's observability.
	 */
    public void testObservable() {
        /** A test observer. */
        class TestObserver implements Observer {

            /** Tracks whether a MEMBERS update occurred. */
            private boolean gotMembers = false;

            /** Tracks whether a MESSAGES update occurred. */
            private boolean gotMessages = false;

            /** Resets the update indicators. */
            void reset() {
                gotMembers = false;
                gotMessages = false;
            }

            /** A test update method.
			 * @param o   the observed channel
			 * @param arg a change indicator
			 */
            public void update(Observable o, Object arg) {
                if (arg instanceof Changed) {
                    Changed c = (Changed) arg;
                    switch(c) {
                        case MEMBERS:
                            gotMembers = true;
                            break;
                        case MESSAGES:
                            gotMessages = true;
                            break;
                        default:
                            fail("There should be no other Changed values.");
                    }
                } else {
                    fail("There should be no other update() arg types.");
                }
            }
        }
        Observable observedChannel = mainChannel;
        TestObserver obs = new TestObserver();
        observedChannel.addObserver(obs);
        assertFalse("There should have been no updates so far.", obs.gotMembers || obs.gotMessages);
        mainChannel.postMessage(firstUser, MSG1);
        assertFalse("Should have got NO members update.", obs.gotMembers);
        assertTrue("Should have got a messages update.", obs.gotMessages);
        obs.reset();
        mainChannel.postMessage(otherUser, MSG3);
        assertFalse("Should have got NO updates at all.", obs.gotMembers || obs.gotMessages);
        obs.reset();
        mainChannel.setMembers(null);
        assertTrue("Should have got a members update.", obs.gotMembers);
        assertFalse("Should have got NO messages update.", obs.gotMessages);
        obs.reset();
    }
}
