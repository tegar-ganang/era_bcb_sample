package net.moep.ircd.test;

import java.util.Collection;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import junit.framework.TestCase;
import net.moep.irc.serverevents.JoinEventParameter;
import net.moep.irc.serverevents.NewUserEventParameter;
import net.moep.irc.serverevents.PartEventParameter;
import net.moep.irc.serverevents.QuitUserEventParameter;
import net.moep.ircservices.common.Labels;
import net.moep.ircservices.common.Sources;
import net.moep.ircservices.ejb.IrcServer;
import net.moep.ircservices.ejb.ReperaturWerkstatt;
import net.moep.ircservices.ejb.Zustandsabfrage;
import net.moep.ircservices.par.IRCChannelBean;
import net.moep.ircservices.par.IRCUserBean;
import net.moep.ircservices.par.common.Message;
import net.moep.ircservices.par.common.ReturnValue;

/**
 * @author schuppi
 * 
 */
public class CoreTest extends TestCase {

    public static final String BLA = "bla";

    public final String[] users = { "user1", "user2", "user3", "user4", "user5" };

    public final String[] channels = { "#chan1", "#chan2", "#chan3" };

    Zustandsabfrage zustandsabfrage = null;

    IrcServer ircServer = null;

    ReperaturWerkstatt reperatur = null;

    private NewUserEventParameter[] connetcts;

    private QuitUserEventParameter[] quits;

    protected void setUp() throws Exception {
        Hashtable hs = new Hashtable();
        hs.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
        hs.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        hs.put("java.naming.provider.url", "localhost:1099");
        Context ctx = new InitialContext(hs);
        zustandsabfrage = (Zustandsabfrage) ctx.lookup("moepservicesNG/ZustandsabfrageBean/remote");
        ircServer = (IrcServer) ctx.lookup("moepservicesNG/IrcServerBean/remote");
        reperatur = (ReperaturWerkstatt) ctx.lookup("moepservicesNG/ReperaturWerkstattBean/remote");
        connetcts = new NewUserEventParameter[users.length];
        for (int i = 0; i < connetcts.length; i++) {
            connetcts[i] = new NewUserEventParameter(users[i], BLA, BLA, BLA, BLA);
        }
        quits = new QuitUserEventParameter[users.length];
        for (int i = 0; i < quits.length; i++) {
            quits[i] = new QuitUserEventParameter(users[i]);
        }
        aufraeumen();
    }

    public void testConnectQuit() throws Exception {
        aufraeumen();
        int currentCount = zustandsabfrage.getUserCount();
        IRCUserBean[] userz = new IRCUserBean[users.length];
        for (int i = 0; i < userz.length; i++) {
            userz[i] = (IRCUserBean) ircServer.handleEvent(connetcts[i].getEvent()).getValue();
        }
        assertEquals(currentCount + connetcts.length, zustandsabfrage.getUserCount().intValue());
        ircServer.handleEvent(quits[0].getEvent());
        assertEquals(currentCount + connetcts.length - 1, zustandsabfrage.getUserCount().intValue());
        ircServer.handleEvent(quits[1].getEvent());
        ircServer.handleEvent(quits[2].getEvent());
        assertEquals(currentCount + connetcts.length - 3, zustandsabfrage.getUserCount().intValue());
    }

    public void testJoinPart() throws Exception {
        aufraeumen();
        ircServer.handleEvent(connetcts[1].getEvent());
        ircServer.handleEvent(connetcts[2].getEvent());
        String channel = channels[0];
        ircServer.handleEvent((new JoinEventParameter(users[1], channel)).getEvent());
        IRCChannelBean c = zustandsabfrage.getChannelWithJoins(channel).getValue();
        assertEquals(1, c.getJoins().size());
        ircServer.handleEvent(new JoinEventParameter(users[2], channel).getEvent());
        c = zustandsabfrage.getChannelWithJoins(channel).getValue();
        assertEquals(2, c.getJoins().size());
        ircServer.handleEvent((new PartEventParameter(users[1], channel)).getEvent());
        ircServer.handleEvent((new PartEventParameter(users[2], channel)).getEvent());
        c = zustandsabfrage.getChannelWithJoins(channel).getValue();
        assertNull(c);
    }

    public void testMultiConnect() throws Exception {
        aufraeumen();
        int count = zustandsabfrage.getUserCount();
        ircServer.handleEvent(connetcts[1].getEvent());
        boolean okay = false;
        try {
            ircServer.handleEvent(connetcts[1].getEvent());
            okay = false;
        } catch (Exception e) {
            okay = true;
        }
        assertTrue(okay);
        assertEquals(count + 1, zustandsabfrage.getUserCount().intValue());
    }

    public void testDoppelJoin() throws Exception {
        aufraeumen();
        String channel = channels[0];
        ircServer.handleEvent(connetcts[1].getEvent());
        ircServer.handleEvent((new JoinEventParameter(users[1], channel)).getEvent());
        IRCChannelBean c = zustandsabfrage.getChannelWithJoins(channel).getValue();
        assertEquals(1, c.getJoins().size());
        ReturnValue retval = ircServer.handleEvent((new JoinEventParameter(users[1], channel)).getEvent());
        assertNull(retval.getValue());
        assertTrue(retval.getMessages().contains(new Message(Labels.ALREADY_EXISTS, Sources.JOIN)));
    }

    public void testDoppelPart() throws Exception {
        aufraeumen();
        String channel = channels[0];
        ircServer.handleEvent(connetcts[1].getEvent());
        ircServer.handleEvent((new JoinEventParameter(users[1], channel)).getEvent());
        ircServer.handleEvent((new PartEventParameter(users[1], channel)).getEvent());
        ReturnValue retval = ircServer.handleEvent((new PartEventParameter(users[1], channel)).getEvent());
        assertNull(retval.getValue());
        assertTrue(retval.getMessages().contains(new Message(Labels.NOTFOUND, Sources.JOIN)) || retval.getMessages().contains(new Message(Labels.NOTFOUND, Sources.CHANNEL)));
    }

    /**
     * 
     */
    private void aufraeumen() {
        for (String a : users) {
            ircServer.handleEvent(new QuitUserEventParameter(a).getEvent());
        }
        for (String c : channels) {
            ReturnValue<IRCChannelBean> chan = zustandsabfrage.getChannel(c);
            if (chan.getValue() != null) {
                System.out.println("found chan -> will remove it");
                if (!reperatur.removeChannel(c)) System.out.println(" didnt work... :(");
            }
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
