package jerklib.events;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import jerklib.EventTestBase;
import jerklib.ModeAdjustment;
import jerklib.ModeAdjustment.Action;
import jerklib.events.IRCEvent.Type;
import jerklib.tasks.TaskImpl;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertTrue;

public class ModeEventTest extends EventTestBase {

    private List<ModeEvent> events = new ArrayList<ModeEvent>();

    public ModeEventTest() {
        super("/mode.data", System.getProperty("user.home") + File.separator + "jerklib.tests.user.ouput");
    }

    @BeforeTest
    public void init() {
        createSession();
        addServerInfo(ServerInfo.ALL);
        for (String name : new String[] { "#perkosa", "#tvtorrents", "#ubuntu", "#cod4.wars", "#jerklib", "#testing", "#test", "#foooo" }) {
            addChannel(name);
        }
        session.onEvent(new TaskImpl("mode") {

            public void receiveEvent(IRCEvent e) {
                events.add((ModeEvent) e);
            }
        }, Type.MODE_EVENT);
        conMan.start(session);
    }

    @Test
    public void testNumDispatched() {
        assertTrue(events.size() + "", events.size() == 546);
    }

    @Test
    public void testBahamutChannelModes() {
        ModeEvent me = events.get(20);
        assertTrue(me.getChannel().getName(), me.getChannel().getName().equals("#perkosa"));
        assertTrue(me.getModeType() == ModeEvent.ModeType.CHANNEL);
        assertTrue(me.setBy().equals("FoNix"));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 2);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.MINUS);
        assertTrue(ma.getMode() == 'k');
        assertTrue(ma.getArgument().equals("9identified.avoice"));
        ma = adjs.get(1);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'v');
        assertTrue(ma.getArgument().equals("indrii"));
        me = events.get(30);
        assertTrue(me.getChannel().getName(), me.getChannel().getName().equals("#perkosa"));
        assertTrue(me.getModeType() == ModeEvent.ModeType.CHANNEL);
        assertTrue(me.setBy().equals("ChanServ"));
        adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 1);
        ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'o');
        assertTrue(ma.getArgument().equals("rONx"));
    }

    @Test
    public void testUnrealChannelModes() {
        ModeEvent me = events.get(11);
        assertTrue(me.getChannel().getName(), me.getChannel().getName().equals("#tvtorrents"));
        assertTrue(me.getModeType() == ModeEvent.ModeType.CHANNEL);
        assertTrue(me.setBy().equals("ChanServ"));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 1);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'h');
        assertTrue(ma.getArgument().equals("TVTorrentsBot"));
    }

    @Test
    public void testSnircdChannelModes() {
        ModeEvent me = events.get(4);
        assertTrue(me.getChannel().getName(), me.getChannel().getName().equals("#cod4.wars"));
        assertTrue(me.getModeType() == ModeEvent.ModeType.CHANNEL);
        assertTrue(me.setBy().equals("|cod4-wars|"));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 2);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.MINUS);
        assertTrue(ma.getMode() == 'm');
        assertTrue(ma.getArgument().equals(""));
        ma = adjs.get(1);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'c');
        assertTrue(ma.getArgument().equals(""));
    }

    @Test
    public void testHyperionChannelModes() {
        ModeEvent me = events.get(72);
        assertTrue(me.getChannel().getName(), me.getChannel().getName().equals("#ubuntu"));
        assertTrue(me.getModeType() == ModeEvent.ModeType.CHANNEL);
        assertTrue(me.setBy().equals("nickrud"));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 1);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'b');
        assertTrue(ma.getArgument().equals("*!*@tejava.dreamhost.com"));
        me = events.get(149);
        assertTrue(me.getChannel().getName(), me.getChannel().getName().equals("#ubuntu"));
        assertTrue(me.getModeType() == ModeEvent.ModeType.CHANNEL);
        assertTrue(me.setBy().equals("FloodBot3"));
        adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 2);
        ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'z');
        assertTrue(ma.getArgument().equals(""));
        ma = adjs.get(1);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'b');
        assertTrue(ma.getArgument().equals("%ani1!*@*"));
    }

    @Test
    public void testBahamutUserModes() {
        ModeEvent me = events.get(0);
        assertTrue(me.getChannel() == null);
        assertTrue(me.getModeType() == ModeEvent.ModeType.USER);
        assertTrue(me.setBy(), me.setBy().equals(me.getSession().getConnectedHostName()));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 1);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'i');
        assertTrue(ma.getArgument().equals(""));
    }

    @Test
    public void testUnrealUserModes() {
        ModeEvent me = events.get(1);
        assertTrue(me.getChannel() == null);
        assertTrue(me.getModeType() == ModeEvent.ModeType.USER);
        assertTrue(me.setBy(), me.setBy().equals(me.getSession().getConnectedHostName()));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 3);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'i');
        assertTrue(ma.getArgument().equals(""));
        ma = adjs.get(1);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'w');
        assertTrue(ma.getArgument().equals(""));
        ma = adjs.get(2);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'x');
        assertTrue(ma.getArgument().equals(""));
    }

    @Test
    public void testSnircdUserModes() {
        ModeEvent me = events.get(2);
        assertTrue(me.getChannel() == null);
        assertTrue(me.getModeType() == ModeEvent.ModeType.USER);
        assertTrue(me.setBy(), me.setBy().equals(me.getSession().getConnectedHostName()));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 1);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'i');
        assertTrue(ma.getArgument().equals(""));
    }

    @Test
    public void testHyperionUserModes() {
        ModeEvent me = events.get(541);
        assertTrue(me.getChannel() == null);
        assertTrue(me.getModeType() == ModeEvent.ModeType.USER);
        assertTrue(me.setBy(), me.setBy().equals(me.getSession().getConnectedHostName()));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 1);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'e');
        assertTrue(ma.getArgument().equals(""));
    }

    @Test
    public void testBahamutNumericChannelModeReply() {
        ModeEvent me = events.get(544);
        assertTrue(me.getChannel().getName(), me.getChannel().getName().equals("#perkosa"));
        assertTrue(me.getModeType() == ModeEvent.ModeType.CHANNEL);
        assertTrue(me.setBy().equals(""));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 2);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 't');
        assertTrue(ma.getArgument().equals(""));
        ma = adjs.get(1);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'n');
        assertTrue(ma.getArgument().equals(""));
    }

    @Test
    public void testUnrealNumericChannelModeReply() {
        ModeEvent me = events.get(543);
        assertTrue(me.getChannel().getName(), me.getChannel().getName().equals("#tvtorrents"));
        assertTrue(me.getModeType() == ModeEvent.ModeType.CHANNEL);
        assertTrue(me.setBy().equals(""));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 6);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'n');
        assertTrue(ma.getArgument().equals(""));
        ma = adjs.get(1);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 't');
        assertTrue(ma.getArgument().equals(""));
        ma = adjs.get(5);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'f');
        assertTrue(ma.getArgument().equals("[30j#R10,30k#K10,40m#M10,10n#N10]:15"));
    }

    @Test
    public void testSnircdNumericChannelModeReply() {
        ModeEvent me = events.get(542);
        assertTrue(me.getChannel().getName(), me.getChannel().getName().equals("#cod4.wars"));
        assertTrue(me.getModeType() == ModeEvent.ModeType.CHANNEL);
        assertTrue(me.setBy().equals(""));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 6);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 't');
        assertTrue(ma.getArgument().equals(""));
        ma = adjs.get(1);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'n');
        assertTrue(ma.getArgument().equals(""));
    }

    @Test
    public void testHyperionNumericChannelModeReply() {
        ModeEvent me = events.get(545);
        assertTrue(me.getChannel().getName(), me.getChannel().getName().equals("#jerklib"));
        assertTrue(me.getModeType() == ModeEvent.ModeType.CHANNEL);
        assertTrue(me.setBy().equals(""));
        List<ModeAdjustment> adjs = me.getModeAdjustments();
        assertTrue(adjs != null);
        assertTrue(adjs.size() == 2);
        ModeAdjustment ma = adjs.get(0);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 't');
        assertTrue(ma.getArgument().equals(""));
        ma = adjs.get(1);
        assertTrue(ma.getAction() == Action.PLUS);
        assertTrue(ma.getMode() == 'n');
        assertTrue(ma.getArgument().equals(""));
    }
}
