package org.thole.phiirc.client.controller;

import org.thole.phiirc.client.model.Channel;
import org.thole.phiirc.client.model.User;
import org.thole.phiirc.client.model.UserChannelPermission;
import junit.framework.TestCase;

public class UserTest extends TestCase {

    UserWatcher uw;

    User user;

    Channel channel;

    Channel channel2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        uw = new UserWatcher();
        channel = new Channel("#ourtestchannel");
        channel2 = new Channel("#anothertestchan");
        user = new User("tester", "TesterName", "TesterRealName", "testing.com", channel, UserChannelPermission.VOICE);
        uw.addUser(user);
        channel.getUserList().add(user);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        uw = null;
        channel = null;
        user = null;
    }

    public void testAddUserStringChannel() {
        uw.addUser("tester", channel);
        assertEquals(UserChannelPermission.VOICE, user.getChannels().get(channel));
        assertEquals(UserChannelPermission.VOICE, uw.getUser("tester").getChannels().get(channel));
        assertNotNull(uw.getUser("tester"));
        assertEquals("tester", user.getNick());
        assertEquals("tester", uw.getUser("tester").getNick());
        assertEquals("TesterName", user.getName());
        assertEquals("TesterRealName", user.getRealname());
        assertEquals("testing.com", user.getHost());
        assertEquals("testing.com", uw.getUser("tester").getHost());
        assertSame(user, uw.getUser("tester"));
    }

    public void testAddUserStringChannelUserChannelPermission() {
        assertEquals(UserChannelPermission.VOICE, user.getChannels().get(channel));
        assertEquals(UserChannelPermission.VOICE, uw.getUser("tester").getChannels().get(channel));
        uw.addUser("tester", channel, UserChannelPermission.STANDARD);
        assertEquals(UserChannelPermission.STANDARD, user.getChannels().get(channel));
        assertEquals(UserChannelPermission.STANDARD, uw.getUser("tester").getChannels().get(channel));
        uw.addUser("tester", channel, UserChannelPermission.OPERATOR);
        assertEquals(UserChannelPermission.OPERATOR, user.getChannels().get(channel));
        assertEquals(UserChannelPermission.OPERATOR, uw.getUser("tester").getChannels().get(channel));
        assertNotNull(uw.getUser("tester"));
        assertEquals("tester", user.getNick());
        assertEquals("tester", uw.getUser("tester").getNick());
        assertEquals("TesterName", user.getName());
        assertEquals("TesterRealName", user.getRealname());
        assertEquals("testing.com", user.getHost());
        assertEquals("testing.com", uw.getUser("tester").getHost());
    }

    public void testAddUserString() {
        uw.addUser("tester");
        uw.addUser("waldo");
        assertEquals(UserChannelPermission.VOICE, user.getChannels().get(channel));
        assertEquals(UserChannelPermission.VOICE, uw.getUser("tester").getChannels().get(channel));
        assertNotNull(uw.getUser("tester"));
        assertEquals("tester", user.getNick());
        assertEquals("tester", uw.getUser("tester").getNick());
        assertEquals("TesterName", user.getName());
        assertEquals("TesterRealName", user.getRealname());
        assertEquals("testing.com", user.getHost());
        assertEquals("testing.com", uw.getUser("tester").getHost());
        assertTrue(uw.getUser("waldo").getChannels().isEmpty());
    }

    public void testAddUserStringStringString() {
        uw.addUser("tester", "ludwig", "sf.net");
        assertEquals(UserChannelPermission.VOICE, user.getChannels().get(channel));
        assertEquals(UserChannelPermission.VOICE, uw.getUser("tester").getChannels().get(channel));
        assertNotNull(uw.getUser("tester"));
        assertEquals("tester", user.getNick());
        assertEquals("tester", uw.getUser("tester").getNick());
        assertEquals("ludwig", user.getName());
        assertEquals("TesterRealName", user.getRealname());
        assertEquals("sf.net", user.getHost());
        assertEquals("sf.net", uw.getUser("tester").getHost());
    }

    public void testAddUserStringStringStringStringChannel() {
        uw.addUser("tester", "ludwig", "john doe", "sf.net", channel);
        assertEquals(UserChannelPermission.VOICE, user.getChannels().get(channel));
        assertEquals(UserChannelPermission.VOICE, uw.getUser("tester").getChannels().get(channel));
        assertNotNull(uw.getUser("tester"));
        assertEquals("tester", user.getNick());
        assertEquals("tester", uw.getUser("tester").getNick());
        assertEquals("ludwig", user.getName());
        assertEquals("john doe", user.getRealname());
        assertEquals("sf.net", user.getHost());
        assertEquals("sf.net", uw.getUser("tester").getHost());
    }

    public void testAddUserStringStringStringChannel() {
        uw.addUser("tester", "ludwig", "sf.net", channel);
        assertEquals(UserChannelPermission.VOICE, user.getChannels().get(channel));
        assertEquals(UserChannelPermission.VOICE, uw.getUser("tester").getChannels().get(channel));
        assertNotNull(uw.getUser("tester"));
        assertEquals("tester", user.getNick());
        assertEquals("tester", uw.getUser("tester").getNick());
        assertEquals("ludwig", user.getName());
        assertEquals("TesterRealName", user.getRealname());
        assertEquals("sf.net", user.getHost());
        assertEquals("sf.net", uw.getUser("tester").getHost());
    }

    public void testAddUserStringStringStringStringChannelUserChannelPermission() {
        assertEquals(UserChannelPermission.VOICE, user.getChannels().get(channel));
        assertEquals(UserChannelPermission.VOICE, uw.getUser("tester").getChannels().get(channel));
        uw.addUser("tester", "waldo", "John Doe", "sourceforge.net", channel, UserChannelPermission.OPERATOR);
        assertEquals(UserChannelPermission.OPERATOR, user.getChannels().get(channel));
        assertEquals(UserChannelPermission.OPERATOR, uw.getUser("tester").getChannels().get(channel));
        assertNotNull(uw.getUser("tester"));
        assertEquals("tester", user.getNick());
        assertEquals("tester", uw.getUser("tester").getNick());
        assertEquals("waldo", user.getName());
        assertEquals("John Doe", user.getRealname());
        assertEquals("sourceforge.net", user.getHost());
        assertEquals("sourceforge.net", uw.getUser("tester").getHost());
    }

    public void testNickChange() {
        assertEquals("tester", user.getNick());
        assertEquals("tester", uw.getUser("tester").getNick());
        uw.nickChange("tester", "john");
        assertEquals("john", user.getNick());
        assertEquals(user, uw.getUser("john"));
    }
}
