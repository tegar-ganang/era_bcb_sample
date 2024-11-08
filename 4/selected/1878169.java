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

public class JoinEventTest extends EventTestBase {

    private List<JoinEvent> events = new ArrayList<JoinEvent>();

    public JoinEventTest() {
        super("/join.data", System.getProperty("user.home") + File.separator + "jerklib.tests.user.ouput");
    }

    @BeforeTest
    public void init() {
        createSession();
        addServerInfo(ServerInfo.ALL);
        for (String name : new String[] { "#perkosa", "#tvtorrents", "#ubuntu", "#cod4.wars", "#jerklib", "#testing", "#test", "#foooo" }) {
            addChannel(name);
        }
        session.onEvent(new TaskImpl("join") {

            public void receiveEvent(IRCEvent e) {
                events.add((JoinEvent) e);
            }
        }, JOIN);
        conMan.start(session);
    }

    @Test
    public void testNumEventsDispatched() {
        assertTrue(events.size() + "", events.size() == 5688);
    }

    @Test
    public void testBahamutJoin() {
        JoinEvent je = (JoinEvent) events.get(6);
        assertTrue(je.getChannel().getName(), je.getChannel().getName().equals("#perkosa"));
        assertTrue(je.getNick().equals("David`KereN"));
        assertTrue(je.getHostName().equals("we.wish.you.a.merry.christmas.pp.ru"));
        assertTrue(je.getUserName(), je.getUserName().equals("real"));
    }

    @Test
    public void testUnrealJoinComplete() {
        JoinEvent je = (JoinEvent) events.get(18);
        assertTrue(je.getChannel().getName(), je.getChannel().getName().equals("#tvtorrents"));
        assertTrue(je.getNick().equals("TVTorrentsBot"));
        assertTrue(je.getHostName().equals("nix-555C426C.cust.blixtvik.net"));
        assertTrue(je.getUserName(), je.getUserName().equals("~PircBot"));
    }

    @Test
    public void testSnircdJoinComplete() {
        JoinEvent je = (JoinEvent) events.get(25);
        assertTrue(je.getChannel().getName(), je.getChannel().getName().equals("#cod4.wars"));
        assertTrue(je.getNick().equals("kalleKula"));
        assertTrue(je.getHostName().equals("90-227-48-98-no88.tbcn.telia.com"));
        assertTrue(je.getUserName(), je.getUserName().equals("~fa"));
    }

    @Test
    public void testHyperionJoinComplete() {
        JoinEvent je = (JoinEvent) events.get(23);
        assertTrue(je.getChannel().getName(), je.getChannel().getName().equals("#ubuntu"));
        assertTrue(je.getNick().equals("markl_"));
        assertTrue(je.getHostName().equals("c-24-10-221-6.hsd1.co.comcast.net"));
        assertTrue(je.getUserName(), je.getUserName().equals("n=mark"));
    }
}
