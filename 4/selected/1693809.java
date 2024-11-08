package jerklib;

import java.io.File;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertTrue;

public class ChannelNameStoreCaseInsensitiveTest extends EventTestBase {

    public ChannelNameStoreCaseInsensitiveTest() {
        super("/join.data", System.getProperty("user.home") + File.separator + "jerklib.tests.user.ouput");
    }

    @BeforeTest
    public void init() {
        createSession();
        addServerInfo(ServerInfo.HYPERION);
        Channel chan = new Channel("#foo", session);
        session.addChannel(chan);
    }

    @Test
    public void testCaseInsensitivity() {
        assertTrue(session.getChannel("#FOO") != null);
        assertTrue(session.getChannel("#foo") != null);
        assertTrue(session.getChannel("#FoO") != null);
        assertTrue(session.getChannel("#fOO") != null);
    }
}
