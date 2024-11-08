package de.tudresden.inf.rn.mobilis.jclient.xhunt;

import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import de.tudresden.inf.rn.mobilis.jclient.xhunt.packet.InitGameIQ;
import de.tudresden.inf.rn.mobilis.jclient.xhunt.packet.PlayerExitGameIQ;
import de.tudresden.inf.rn.mobilis.jclient.xhunt.packet.TargetIQ;
import de.tudresden.inf.rn.mobilis.jclient.xhunt.packet.UpdatePlayerIQ;
import de.tudresden.inf.rn.mobilis.jclient.xhunt.packet.XHuntLocationIQ;
import de.tudresden.inf.rn.mobilis.jclient.xhunt.services.IQService;
import de.tudresden.inf.rn.mobilis.jclient.xhunt.services.MessageService;

public class Connection {

    private XMPPConnection xmppCon;

    private Settings set;

    private MainView mw;

    public Connection(Settings set) {
        this.set = set;
    }

    /**
	 * Opens a connection to the XMPP-Server and registrates the packet listener
	 * @return
	 */
    public boolean startConnection() {
        ConnectionConfiguration conf = new ConnectionConfiguration(set.getServer(), Integer.valueOf(set.getPort()).intValue());
        xmppCon = new XMPPConnection(conf);
        try {
            xmppCon.connect();
            xmppCon.login(set.getLogin(), set.getPassword());
        } catch (XMPPException e) {
            e.printStackTrace();
            return false;
        }
        MessageService mesServ = new MessageService(mw, set);
        PacketTypeFilter mesFil = new PacketTypeFilter(Message.class);
        xmppCon.addPacketListener(mesServ, mesFil);
        IQService iqServ = new IQService(mw, set);
        PacketTypeFilter locFil = new PacketTypeFilter(IQ.class);
        xmppCon.addPacketListener(iqServ, locFil);
        set.setJid(xmppCon.getUser());
        return true;
    }

    public void setMainView(MainView mw) {
        this.mw = mw;
    }

    /**
	 * Sends an InitGameIQ
	 */
    public void sendInitIQ() {
        InitGameIQ iIQ = new InitGameIQ();
        iIQ.setName(set.getName());
        iIQ.setTo(set.getServerJid());
        xmppCon.sendPacket(iIQ);
    }

    /**
	 * Sends an UpdatePlayerIQ
	 */
    public void sendUpdatePlayerIQ() {
        UpdatePlayerIQ upIQ = new UpdatePlayerIQ();
        upIQ.setTo(set.getServerJid());
        upIQ.setJid(set.getJid());
        upIQ.setName(set.getName());
        upIQ.setIsModerator(set.isModerator());
        upIQ.setIsMrX(set.isMisterX());
        upIQ.setIsReady(true);
        xmppCon.sendPacket(upIQ);
    }

    /**
	 * Sends an TargetIQ to the chosen target
	 */
    public void sendTargetIQ(String target) {
        TargetIQ tarIQ = new TargetIQ();
        tarIQ.setTo(set.getServerJid());
        tarIQ.setTarget(target);
        tarIQ.setFinalDecision(true);
        tarIQ.setShowMe(true);
        tarIQ.setJid(set.getJid());
        tarIQ.setTicketType("jclient");
        tarIQ.setRound(mw.getRound());
        xmppCon.sendPacket(tarIQ);
    }

    /**
	 * Sends an XHuntLocationIQ to the chosen position
	 */
    public void sendXHuntLocationIQ(GeoPosition point) {
        XHuntLocationIQ locIQ = new XHuntLocationIQ();
        locIQ.setTo(set.getServerJid());
        locIQ.setJid(set.getJid());
        locIQ.setLatitude(point.getLatitude());
        locIQ.setLongitude(point.getLongitude());
        xmppCon.sendPacket(locIQ);
    }

    /**
	 * Sends an PlayerExitGameIQ
	 */
    public void sendPlayerExitGameIQ() {
        PlayerExitGameIQ pegIQ = new PlayerExitGameIQ();
        pegIQ.setTo(set.getServerJid());
        pegIQ.setJid(set.getJid());
        xmppCon.sendPacket(pegIQ);
    }
}
