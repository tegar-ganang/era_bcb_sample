package consciouscode.bonsai.actions;

import consciouscode.bonsai.channels.Channel;
import consciouscode.junit.MockLogger;
import consciouscode.seedling.auth.DummyAuthenticator;
import consciouscode.seedling.auth.User;
import consciouscode.swing.SwingTestCase;

public class LoginActionTest extends SwingTestCase {

    public static final String[] USERS = { "todd" };

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myLog = new MockLogger();
        myAction = new LoginAction(new DummyAuthenticator(USERS));
        myAction.setLog(myLog);
        myUsernameChannel = myAction.getChannel(LoginAction.CHANNEL_USERNAME);
        myPasswordChannel = myAction.getChannel(LoginAction.CHANNEL_PASSWORD);
        myUserChannel = myAction.getChannel(LoginAction.CHANNEL_USER);
    }

    @Override
    public void tearDown() throws Exception {
        myLog = null;
        myAction = null;
        myUsernameChannel = null;
        myPasswordChannel = null;
        myUserChannel = null;
        super.tearDown();
    }

    public void testInitialState() {
        assertFalse(myAction.isEnabled());
        assertNull(myUsernameChannel.getValue());
        assertNull(myPasswordChannel.getValue());
        assertNull(myUserChannel.getValue());
    }

    public void testActionEnabling() {
        myUsernameChannel.setValue("todd");
        assertFalse(myAction.isEnabled());
        myPasswordChannel.setValue("todd");
        assertTrue(myAction.isEnabled());
        myPasswordChannel.setValue("");
        assertFalse(myAction.isEnabled());
        myUsernameChannel.setValue("");
        assertFalse(myAction.isEnabled());
        myPasswordChannel.setValue("todd");
        assertFalse(myAction.isEnabled());
        myUsernameChannel.setValue("todd");
        assertTrue(myAction.isEnabled());
        assertNull(myUserChannel.getValue());
    }

    public void testLogin() {
        myUsernameChannel.setValue("todd");
        myPasswordChannel.setValue("todd");
        performAction(myAction);
        User user = (User) myUserChannel.getValue();
        assertNotNull("No user", user);
        assertEquals("todd", user.getUsername());
        myLog.expectError();
        myPasswordChannel.setValue("badpass");
        performAction(myAction);
        myLog.verify();
    }

    private MockLogger myLog;

    private LoginAction myAction;

    private Channel myUsernameChannel;

    private Channel myPasswordChannel;

    private Channel myUserChannel;
}
