package jerklib.events;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.assertTrue;
import jerklib.EventTestBase;
import jerklib.events.IRCEvent.Type;
import jerklib.tasks.TaskImpl;

public class NoticeEventTest extends EventTestBase {

    private List<NoticeEvent> events = new ArrayList<NoticeEvent>();

    public NoticeEventTest() {
        super("/notice.data", System.getProperty("user.home") + File.separator + "jerklib.tests.user.ouput");
    }

    @BeforeTest
    public void init() {
        createSession();
        addServerInfo(ServerInfo.ALL);
        for (String name : new String[] { "#perkosa", "#tvtorrents", "#ubuntu", "	#cod4.wars", "#jerklib", "#testing", "#test", "#foooo" }) {
            addChannel(name);
        }
        session.onEvent(new TaskImpl("notice") {

            public void receiveEvent(IRCEvent e) {
                events.add((NoticeEvent) e);
            }
        }, Type.NOTICE);
        conMan.start(session);
    }

    @Test
    public void testBahamutChannelNotice() {
        NoticeEvent event = events.get(24);
        assertTrue(event.getChannel().getName(), event.getChannel().getName().equals("#testing"));
        assertTrue(event.getNoticeMessage(), event.getNoticeMessage().equals("test"));
        assertTrue(event.byWho(), event.byWho().equals("mohadib"));
        assertTrue(event.toWho(), event.toWho().equals(""));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testUnrealChannelNotice() {
        NoticeEvent event = events.get(25);
        assertTrue(event.getChannel().getName(), event.getChannel().getName().equals("#test"));
        assertTrue(event.getNoticeMessage(), event.getNoticeMessage().equals("testss"));
        assertTrue(event.byWho(), event.byWho().equals("mohadib"));
        assertTrue(event.toWho(), event.toWho().equals(""));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testHyperionChannelNotice() {
        NoticeEvent event = events.get(23);
        assertTrue(event.getChannel().getName(), event.getChannel().getName().equals("#jerklib"));
        assertTrue(event.getNoticeMessage(), event.getNoticeMessage().equals("testtt"));
        assertTrue(event.byWho(), event.byWho().equals("mohadib__"));
        assertTrue(event.toWho(), event.toWho().equals(""));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testSnircdChannelNotice() {
        NoticeEvent event = events.get(26);
        assertTrue(event.getChannel().getName(), event.getChannel().getName().equals("#foooo"));
        assertTrue(event.getNoticeMessage(), event.getNoticeMessage().equals("heloooo"));
        assertTrue(event.byWho(), event.byWho().equals("mohadib"));
        assertTrue(event.toWho(), event.toWho().equals(""));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testUltimateChannelNotice() {
    }

    @Test
    public void testBahamutUserNotice() {
        NoticeEvent event = events.get(27);
        assertTrue(event.getChannel() == null);
        assertTrue(event.getNoticeMessage().equals("TEST?"));
        assertTrue(event.byWho().equals("mohadib_"));
        assertTrue(event.toWho().equals("mohadib__"));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testUnrealUserNotice() {
        NoticeEvent event = events.get(28);
        assertTrue(event.getChannel() == null);
        assertTrue(event.getNoticeMessage().equals("TESTTSS"));
        assertTrue(event.byWho().equals("mohadib_"));
        assertTrue(event.toWho().equals("mohadib__"));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testHyperionUserNotice() {
        NoticeEvent event = events.get(18);
        assertTrue(event.getChannel() == null);
        assertTrue(event.getNoticeMessage().equals("This nickname is owned by someone else"));
        assertTrue(event.byWho().equals("NickServ"));
        assertTrue(event.toWho(), event.toWho().equals("scripy"));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testSnircdUserNotice() {
        NoticeEvent event = events.get(29);
        assertTrue(event.getChannel() == null);
        assertTrue(event.getNoticeMessage().equals("HELLO"));
        assertTrue(event.byWho().equals("mohadib_"));
        assertTrue(event.toWho().equals("mohadib__"));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testBahamutServerNotice() {
        NoticeEvent event = events.get(2);
        assertTrue(event.getChannel() == null);
        assertTrue(event.getNoticeMessage().equals("*** Found your hostname, cached"));
        assertTrue(event.byWho(), event.byWho().equals("swiftco.wa.us.dal.net"));
        assertTrue(event.toWho().equals(""));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testUnrealServerNotice() {
        NoticeEvent event = events.get(14);
        assertTrue(event.getChannel() == null);
        assertTrue(event.getNoticeMessage().equals("*** Looking up your hostname..."));
        assertTrue(event.byWho(), event.byWho().equals("irc.nixgeeks.com"));
        assertTrue(event.toWho().equals(""));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testHyperionServerNotice() {
        NoticeEvent event = events.get(30);
        assertTrue(event.getChannel() == null);
        assertTrue(event.getNoticeMessage().equals("NickServ set your hostname to \"unaffiliated/mohadib\""));
        assertTrue(event.byWho(), event.byWho().equals("kubrick.freenode.net"));
        assertTrue(event.toWho().equals("mohadib"));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testSnircdServerNotice() {
        NoticeEvent event = events.get(20);
        assertTrue(event.getChannel() == null);
        assertTrue(event.getNoticeMessage().equals("Highest connection count: 5736 (5735 clients)"));
        assertTrue(event.byWho(), event.byWho().equals("underworld2.no.quakenet.org"));
        assertTrue(event.toWho(), event.toWho().equals(session.getNick()));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testBahmutGenericNotices() {
    }

    @Test
    public void testUnrealGenericNotices() {
    }

    @Test
    public void testHyperionGenericNotices() {
        NoticeEvent event = events.get(7);
        assertTrue(event.getChannel() == null);
        assertTrue(event.getNoticeMessage().equals("*** No identd (auth) response"));
        assertTrue(event.byWho(), event.byWho().equals("anthony.freenode.net"));
        assertTrue(event.toWho().equals(""));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testSnircdGenericNotices() {
        NoticeEvent event = events.get(8);
        assertTrue(event.getChannel() == null);
        assertTrue(event.getNoticeMessage().equals("*** Checking Ident"));
        assertTrue(event.byWho(), event.byWho().equals("anthony.freenode.net"));
        assertTrue(event.toWho().equals(""));
        assertTrue(event.getSession().equals(session));
    }

    @Test
    public void testNumEventsDispatched() {
        assertTrue(String.valueOf(events.size()), events.size() == 31);
    }
}
