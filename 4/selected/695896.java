package jerklib.events;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertTrue;
import jerklib.EventTestBase;
import jerklib.tasks.TaskImpl;
import static jerklib.events.IRCEvent.Type.*;

public class PartEventTest extends EventTestBase {

    private List<PartEvent> events = new ArrayList<PartEvent>();

    public PartEventTest() {
        super("/part.data", System.getProperty("user.home") + File.separator + "jerklib.tests.user.ouput");
    }

    @BeforeTest
    public void init() {
        createSession();
        addServerInfo(ServerInfo.ALL);
        for (String name : new String[] { "#perkosa", "#tvtorrents", "#ubuntu", "#cod4.wars", "#jerklib", "#testing", "#test", "#foooo" }) {
            addChannel(name);
        }
        session.onEvent(new TaskImpl("part") {

            public void receiveEvent(IRCEvent e) {
                events.add((PartEvent) e);
            }
        }, PART);
        conMan.start(session);
    }

    @Test
    public void testNumEventsDispatched() {
        assertTrue(events.size() + "", events.size() == 748);
    }

    @Test
    public void testHyperionPart() {
        PartEvent pe = events.get(0);
        assertTrue(pe.getChannelName().equals("#ubuntu"));
        assertTrue(pe.getNick().equals("Tmcarr89"));
        assertTrue(pe.getUserName().equals("n=tmcarr"));
        assertTrue(pe.getHostName().equals("dhcp-128-194-18-51.resnet.tamu.edu"));
        assertTrue(pe.getPartMessage().equals(""));
        pe = events.get(5);
        assertTrue(pe.getChannelName().equals("#ubuntu"));
        assertTrue(pe.getNick().equals("egc"));
        assertTrue(pe.getUserName().equals("n=gcarrill"));
        assertTrue(pe.getHostName().equals("cpe-66-25-187-182.austin.res.rr.com"));
        assertTrue(pe.getPartMessage().equals("\"Ex-Chat\""));
    }

    @Test
    public void testSnircdPart() {
        PartEvent pe = events.get(2);
        assertTrue(pe.getChannelName().equals("#cod4.wars"));
        assertTrue(pe.getNick().equals("AlbCMCSG"));
        assertTrue(pe.getUserName().equals("jenna"));
        assertTrue(pe.getHostName().equals("stop.t.o.shit.la"));
        assertTrue(pe.getPartMessage().equals(""));
    }

    @Test
    public void testBahamutPart() {
        PartEvent pe = events.get(61);
        assertTrue(pe.getChannelName().equals("#perkosa"));
        assertTrue(pe.getNick().equals("KeiKo"));
        assertTrue(pe.getUserName().equals("DI"));
        assertTrue(pe.getHostName().equals("s3xy.biz"));
        assertTrue(pe.getPartMessage().equals("14Looking for Inviter!"));
        pe = events.get(4);
        assertTrue(pe.getChannelName(), pe.getChannelName().equals("#perkosa"));
        assertTrue(pe.getNick().equals("_^Gracia^_"));
        assertTrue(pe.getUserName().equals("~Hatiyayan"));
        assertTrue(pe.getHostName().equals("124.195.18.42"));
        assertTrue(pe.getPartMessage().equals(""));
    }

    @Test
    public void testUnrealPart() {
        PartEvent pe = events.get(240);
        assertTrue(pe.getChannelName().equals("#tvtorrents"));
        assertTrue(pe.getNick().equals("Meph"));
        assertTrue(pe.getUserName().equals("~Meph"));
        assertTrue(pe.getHostName().equals("nix-1637EA46.zone2.bethere.co.uk"));
        assertTrue(pe.getPartMessage().equals("Leaving"));
        pe = events.get(357);
        assertTrue(pe.getChannelName().equals("#tvtorrents"));
        assertTrue(pe.getNick().equals("steeltx"));
        assertTrue(pe.getUserName().equals("d0b46199"));
        assertTrue(pe.getHostName().equals("nix-AFABC4F2.com"));
        assertTrue(pe.getPartMessage().equals(""));
    }
}
