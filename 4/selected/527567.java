package jerklib;

import jerklib.events.*;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class IRCEventFactoryTest {

    private ConnectionManager conMan;

    private Session session;

    private Connection connection;

    private Profile profile = new ProfileImpl("DIBLET", "DIBLET1", "DIBLET2", "DIBLET3");

    @BeforeTest
    void setup() {
        conMan = new ConnectionManager(profile) {
        };
        connection = new Connection(null, null, null) {

            @Override
            Channel getChannel(String name) {
                return new Channel(name, session);
            }
        };
        session = new Session(null) {

            @Override
            Connection getConnection() {
                return connection;
            }
        };
    }

    @AfterTest
    void teardown() {
        connection = null;
    }

    @Test
    public void testKickEvent() {
        KickEvent ke = IRCEventFactory.kick(":mohadib!~mohadib@67.41.102.162 KICK #test scab :bye!", connection);
        assertNotNull(ke);
        assertEquals("mohadib", ke.byWho());
        assertEquals("~mohadib", ke.getUserName());
        assertEquals("67.41.102.162", ke.getHostName());
        assertEquals("#test", ke.getChannel().getName());
        assertEquals("scab", ke.getWho());
        assertEquals("bye!", ke.getMessage());
    }

    @Test
    public void testConnectionCompleteEvent() {
        ConnectionCompleteEvent cce = IRCEventFactory.connectionComplete(":irc.nmglug.org 001 DIBLET1 :Welcome to the nmglug.org", connection);
        assertNotNull(cce);
        assertEquals("irc.nmglug.org", cce.getActualHostName());
    }

    @Test
    public void testInviteEvent() {
        InviteEvent ie = IRCEventFactory.invite(":r0bby!n=wakawaka@guifications/user/r0bby INVITE scripy1 :#jerklib2", connection);
        assertNotNull(ie);
        assertEquals("r0bby", ie.getNick());
        assertEquals("n=wakawaka", ie.getUserName());
        assertEquals("guifications/user/r0bby", ie.getHostName());
        assertEquals("#jerklib2", ie.getChannelName());
    }

    @Test
    public void testNickChangeEvent() {
        NickChangeEvent nce = IRCEventFactory.nickChange(":raving!n=raving@74.195.43.119 NICK :Sir_Fawnpug", connection);
        assertNotNull(nce);
        assertEquals("Sir_Fawnpug", nce.getNewNick());
        assertEquals("raving", nce.getOldNick());
        assertEquals("n=raving", nce.getUserName());
        assertEquals("74.195.43.119", nce.getHostName());
    }

    @Test
    public void testPartEvent() {
        PartEvent pe = IRCEventFactory.part(":r0bby!n=wakawaka@guifications/user/r0bby PART #test :FOO", connection);
        assertNotNull(pe);
        assertEquals("r0bby", pe.getWho());
        assertEquals("n=wakawaka", pe.getUserName());
        assertEquals("guifications/user/r0bby", pe.getHostName());
        assertEquals("#test", pe.getChannelName());
        assertEquals("FOO", pe.getPartMessage());
    }

    @Test
    public void testChannelListEvent() {
        ChannelListEvent cle = IRCEventFactory.chanList(":anthony.freenode.net 322 mohadib_ #jerklib 5 :JerkLib IRC Library - https://sourceforge.net/projects/jerklib", connection);
        assertNotNull(cle);
        assertEquals("#jerklib", cle.getChannelName());
        assertEquals(5, cle.getNumberOfUser());
        assertEquals("JerkLib IRC Library - https://sourceforge.net/projects/jerklib", cle.getTopic());
    }

    @Test
    public void testJoinEvent() {
        JoinEvent je = IRCEventFactory.regularJoin(":r0bby!n=wakawaka@guifications/user/r0bby JOIN :#test", connection);
        assertNotNull(je);
        assertEquals("r0bby", je.getNick());
        assertEquals("n=wakawaka", je.getUserName());
        assertEquals("guifications/user/r0bby", je.getHostName());
        assertEquals("#test", je.getChannelName());
    }

    @Test
    public void testQuitEvent() {
        QuitEvent qe = IRCEventFactory.quit(":mohadib!n=dib@cpe-24-164-167-171.hvc.res.rr.com QUIT :Client Quit", connection);
        assertNotNull(qe);
        assertEquals("mohadib", qe.getWho());
        assertEquals("n=dib", qe.getUserName());
        assertEquals("cpe-24-164-167-171.hvc.res.rr.com", qe.getHostName());
        assertEquals("Client Quit", qe.getQuitMessage());
    }

    @Test
    public void testNoticeEvent() {
        NoticeEvent ne = IRCEventFactory.notice(":DIBLET!n=fran@c-68-35-11-181.hsd1.nm.comcast.net NOTICE #test :test", connection);
        assertNotNull(ne);
        assertEquals("channel", ne.getNoticeType());
        assertEquals("#test", ne.getChannel().getName());
        assertEquals("DIBLET", ne.byWho());
        assertEquals("test", ne.getNoticeMessage());
        assertEquals("", ne.toWho());
    }

    @Test
    public void testUserNoticeEvent() {
        NoticeEvent ne = IRCEventFactory.notice(":NickServ!NickServ@services. NOTICE mohadib_ :This nickname is owned by someone else", connection);
        assertNotNull(ne);
        assertEquals("user", ne.getNoticeType());
        assertNull(ne.getChannel());
        assertEquals("NickServ", ne.byWho());
        assertEquals("This nickname is owned by someone else", ne.getNoticeMessage());
        assertEquals("mohadib_", ne.toWho());
    }

    @Test
    public void testGenericNoticeEvent() {
        NoticeEvent ne = IRCEventFactory.notice("NOTICE AUTH :*** No identd (auth) response", connection);
        assertNotNull(ne);
        assertEquals("generic", ne.getNoticeType());
        assertNull(ne.getChannel());
        assertEquals("", ne.byWho());
        assertEquals("AUTH :*** No identd (auth) response", ne.getNoticeMessage());
        assertEquals("", ne.toWho());
    }

    @Test
    public void testMotdEvent() {
        MotdEvent me = IRCEventFactory.motd(":anthony.freenode.net 375 DIBLET1 :- anthony.freenode.net Message of the Day -", connection);
        assertNotNull(me);
        assertEquals("- anthony.freenode.net Message of the Day -", me.getMotdLine());
        assertEquals("anthony.freenode.net", me.getHostName());
        me = IRCEventFactory.motd(":anthony.freenode.net 372 DIBLET1 :- Welcome to anthony.freenode.net in Irvine, CA, USA!  Thanks to", connection);
        assertNotNull(me);
        assertEquals("- Welcome to anthony.freenode.net in Irvine, CA, USA!  Thanks to", me.getMotdLine());
        me = IRCEventFactory.motd(":anthony.freenode.net 376 DIBLET1 :End of /MOTD command.", connection);
        assertNotNull(me);
        assertEquals("End of /MOTD command.", me.getMotdLine());
    }

    @Test
    public void testNickInUseEvent() {
        NickInUseEvent nie = IRCEventFactory.nickInUse(":simmons.freenode.net 433 * fran :Nickname is already in use.", connection);
        assertNotNull(nie);
        assertEquals("fran", nie.getInUseNick());
    }
}
