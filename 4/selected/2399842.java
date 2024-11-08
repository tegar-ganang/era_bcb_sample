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

public class MessageEventTest extends EventTestBase {

    private List<MessageEvent> events = new ArrayList<MessageEvent>();

    public MessageEventTest() {
        super("/privmsg.data", System.getProperty("user.home") + File.separator + "jerklib.tests.user.ouput");
    }

    @BeforeTest
    public void init() {
        createSession();
        addServerInfo(ServerInfo.ALL);
        for (String name : new String[] { "#perkosa", "#tvtorrents", "#ubuntu", "#cod4.wars", "#jerklib", "#testing", "#test", "#foooo" }) {
            addChannel(name);
        }
        session.onEvent(new TaskImpl("msg") {

            public void receiveEvent(IRCEvent e) {
                events.add((MessageEvent) e);
            }
        }, CHANNEL_MESSAGE);
        session.onEvent(new TaskImpl("msg") {

            public void receiveEvent(IRCEvent e) {
                events.add((MessageEvent) e);
            }
        }, PRIVATE_MESSAGE);
        session.onEvent(new TaskImpl("ctcp") {

            public void receiveEvent(IRCEvent e) {
                events.add((MessageEvent) e);
            }
        }, CTCP_EVENT);
        conMan.start(session);
    }

    @Test
    public void testBahamutChannelMessage() {
        MessageEvent me = events.get(22);
        assertTrue(me.getType() == CHANNEL_MESSAGE);
        assertTrue(me.getNick().equals("p3rkosa"));
        assertTrue(me.getUserName(), me.getUserName().equals("perkosa"));
        assertTrue(me.getHostName().equals("72.20.54.161"));
        assertTrue(me.getChannel().getName().equals("#perkosa"));
        assertTrue(me.getMessage().equals("1230 06secs remaining"));
    }

    @Test
    public void testBahamutPrivateMessage() {
        MessageEvent me = events.get(24109);
        assertTrue(me.getType() == PRIVATE_MESSAGE);
        assertTrue(me.getChannel() == null);
        assertTrue(me.getNick().equals("mohadib"));
        assertTrue(me.getMessage().equals("HEYE THAR"));
        assertTrue(me.getUserName().equals("~mohadib"));
        assertTrue(me.getHostName().equals("71-33-61-22.albq.qwest.net"));
    }

    @Test
    public void testUnrealChannelMessage() {
        MessageEvent me = events.get(576);
        assertTrue(me.getType() == CHANNEL_MESSAGE);
        assertTrue(me.getNick().equals("steff"));
        assertTrue(me.getUserName().equals("~steff.hah"));
        assertTrue(me.getHostName().equals("nix-F0F6441A.static.tpgi.com.au"));
        assertTrue(me.getChannel().getName().equals("#tvtorrents"));
        assertTrue(me.getMessage().equals("InCrediBot credits tori_live"));
    }

    @Test
    public void testUnrealPrivateMessage() {
        MessageEvent me = events.get(24110);
        assertTrue(me.getType() == PRIVATE_MESSAGE);
        assertTrue(me.getChannel() == null);
        assertTrue(me.getNick().equals("mohadib"));
        assertTrue(me.getMessage().equals("HEYE THAR!"));
        assertTrue(me.getUserName().equals("~mohadib"));
        assertTrue(me.getHostName().equals("nix-C604915.albq.qwest.net"));
    }

    @Test
    public void testHyperionChannelMessage() {
        MessageEvent me = events.get(15);
        assertTrue(me.getType() == CHANNEL_MESSAGE);
        assertTrue(me.getNick().equals("DrIP"));
        assertTrue(me.getUserName().equals("n=amrit"));
        assertTrue(me.getHostName().equals("ip68-6-164-241.sd.sd.cox.net"));
        assertTrue(me.getChannel().getName().equals("#ubuntu"));
        assertTrue(me.getMessage().equals("amenado: the wireless is wlan0 and 192.168.1.55"));
    }

    @Test
    public void testHyperionPrivateMessage() {
        MessageEvent me = events.get(24111);
        assertTrue(me.getType() == PRIVATE_MESSAGE);
        assertTrue(me.getChannel() == null);
        assertTrue(me.getNick().equals("mohadib"));
        assertTrue(me.getMessage().equals("HELLO"));
        assertTrue(me.getUserName().equals("n=mohadib"));
        assertTrue(me.getHostName().equals("unaffiliated/mohadib"));
    }

    @Test
    public void testSnircdChannelMessage() {
        MessageEvent me = events.get(24108);
        assertTrue(me.getType() == CHANNEL_MESSAGE);
        assertTrue(me.getNick().equals("FAH|SONYS"));
        assertTrue(me.getMessage().equals("3v3 srv on! low+ msg!!"));
        assertTrue(me.getUserName().equals("fah13"));
        assertTrue(me.getHostName().equals("SONYS.users.quakenet.org"));
        assertTrue(me.getChannel().getName(), me.getChannel().getName().equals("#cod4.wars"));
    }

    @Test
    public void testSnircdPrivateMessage() {
        MessageEvent me = events.get(24112);
        assertTrue(me.getType() == PRIVATE_MESSAGE);
        assertTrue(me.getChannel() == null);
        assertTrue(me.getNick().equals("mohadib"));
        assertTrue(me.getMessage().equals("TESTING?"));
        assertTrue(me.getUserName().equals("~mohadib"));
        assertTrue(me.getHostName().equals("71-33-61-22.albq.qwest.net"));
    }

    @Test
    public void testNumEventsDispatched() {
        assertTrue(events.size() + "", events.size() == 24113);
    }
}
