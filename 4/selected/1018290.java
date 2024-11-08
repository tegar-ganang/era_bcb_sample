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

public class JoinCompleteEventTest extends EventTestBase {

    private List<JoinCompleteEvent> events = new ArrayList<JoinCompleteEvent>();

    public JoinCompleteEventTest() {
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
                events.add((JoinCompleteEvent) e);
            }
        }, JOIN_COMPLETE);
        conMan.start(session);
    }

    @Test
    public void testNumEventsDispatched() {
        assertTrue(events.size() + "", events.size() == 4);
    }

    @Test
    public void testBahamutJoinComplete() {
        JoinCompleteEvent je = (JoinCompleteEvent) events.get(0);
        assertTrue(je.getChannel().getName(), je.getChannel().getName().equals("#perkosa"));
    }

    @Test
    public void testUnrealJoinComplete() {
        JoinCompleteEvent je = (JoinCompleteEvent) events.get(1);
        assertTrue(je.getChannel().getName(), je.getChannel().getName().equals("#tvtorrents"));
    }

    @Test
    public void testSnircdJoinComplete() {
        JoinCompleteEvent je = (JoinCompleteEvent) events.get(2);
        assertTrue(je.getChannel().getName(), je.getChannel().getName().equals("#cod4.wars"));
    }

    @Test
    public void testHyperionJoinComplete() {
        JoinCompleteEvent je = (JoinCompleteEvent) events.get(3);
        assertTrue(je.getChannel().getName(), je.getChannel().getName().equals("#ubuntu"));
    }
}
