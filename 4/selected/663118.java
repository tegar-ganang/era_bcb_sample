package jerklib;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import jerklib.ModeAdjustment.Action;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertTrue;

public class ChannelModeCacheTest extends EventTestBase {

    private Channel chan;

    public ChannelModeCacheTest() {
        super("/join.data", System.getProperty("user.home") + File.separator + "jerklib.tests.user.ouput");
    }

    @BeforeTest
    public void init() {
        createSession();
        addServerInfo(ServerInfo.HYPERION);
        addChannel("#test");
        chan = session.getChannel("#test");
    }

    @Test
    public void testAddNickModeCache() {
        chan.addNick("@mohadib");
        List<ModeAdjustment> modes = chan.getUsersModes("mohadib");
        assertTrue(modes != null);
        assertTrue(modes.size() == 1);
        ModeAdjustment ma = modes.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'o');
        assertTrue(ma.getArgument().equals(""));
    }

    @Test
    public void testUserModeListNotNull() {
        chan.addNick("foo");
        List<ModeAdjustment> modes = chan.getUsersModes("foo");
        assertTrue(modes != null);
        assertTrue(modes.size() == 0);
    }

    @Test
    public void testNickModeCacheMinusWithExistingPlus() {
        chan.removeNick("mohadib");
        chan.addNick("mohadib");
        List<ModeAdjustment> newModes = new ArrayList<ModeAdjustment>();
        newModes.add(new ModeAdjustment(Action.PLUS, 'o', "mohadib"));
        chan.updateModes(newModes);
        newModes = new ArrayList<ModeAdjustment>();
        newModes.add(new ModeAdjustment(Action.MINUS, 'o', "mohadib"));
        chan.updateModes(newModes);
        List<ModeAdjustment> modes = chan.getUsersModes("mohadib");
        assertTrue(modes.size() + "", modes.size() == 0);
    }
}
