package de.tud.kom.nat.tests.relay;

import java.io.File;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import junit.framework.TestCase;
import de.tud.kom.nat.comm.ICommFacade;
import de.tud.kom.nat.comm.msg.IEnvelope;
import de.tud.kom.nat.comm.msg.IPeer;
import de.tud.kom.nat.comm.msg.IPeerID;
import de.tud.kom.nat.im.model.ChatModel;
import de.tud.kom.nat.im.model.files.msg.SendFileRequest;
import de.tud.kom.nat.nattrav.broker.ConnectionManagerFactory;
import de.tud.kom.nat.nattrav.broker.ConnectionRequestType;
import de.tud.kom.nat.nattrav.broker.IApplicationCallback;
import de.tud.kom.nat.nattrav.broker.IConnectionBroker;
import de.tud.kom.nat.nattrav.conn.NatConnector;
import de.tud.kom.nat.tests.TestUtils.MessageHook;
import de.tud.kom.nat.tests.filetransfer.DummyUserInterface;

/**
 * This class just answers that EVERY remote host can be relayed by the local relay daemon.
 *
 * @author Matthias Weinert
 */
class DummyCallback implements IApplicationCallback {

    /** Relay hosts - just ME. */
    private ArrayList<InetSocketAddress> relays = new ArrayList<InetSocketAddress>();

    /** Own ID. */
    private IPeerID peerID;

    /**
	 * Creates a dummy callback which pretends that it can relay to every host.
	 */
    public DummyCallback(IPeerID peerID) {
        this.peerID = peerID;
        relays.add(new InetSocketAddress("localhost", NatConnector.DEFAULT_PORT));
    }

    public Collection<InetSocketAddress> getRelayHostsFor(IPeerID target) {
        return relays;
    }

    public void sendUDPStayAlive(DatagramChannel connectedChannel) {
    }

    public boolean testUDPConnectivity(DatagramChannel datagramChannel) {
        return testUDPConnectivity(datagramChannel, (InetSocketAddress) datagramChannel.socket().getRemoteSocketAddress());
    }

    public IPeerID getOwnPeerID() {
        return peerID;
    }

    public boolean testUDPConnectivity(DatagramChannel datagramChannel, InetSocketAddress addr) {
        return true;
    }
}

/**
 * TODO Relay test cases have to be reimplemented due to a restructurization of the relay system [using control channels].
 *
 * @author Matthias Weinert
 */
public class RelayTest extends TestCase {

    public void testRelay() throws Exception {
        try {
            ChatModel cm1 = new ChatModel(22345, null);
            ICommFacade commFacade = cm1.getCommFacade();
            DummyUserInterface dummyUI = new DummyUserInterface();
            cm1.startSelectionProcess();
            cm1.setUserInterface(dummyUI);
            IPeer peer1 = cm1.getMyself().getPeer();
            IConnectionBroker connBroker = ConnectionManagerFactory.createConnectionBroker(new DummyCallback(cm1.getMyself().getPeerID()));
            SocketChannel sc = connBroker.requestTCPChannel(peer1, ConnectionRequestType.FORCE_RELAY);
            sc.configureBlocking(false);
            commFacade.getChannelManager().registerChannel(sc);
            commFacade.getChannelManager().setSelectedOps(sc, SelectionKey.OP_READ);
            assertTrue(sc != null);
            SendFileRequest sfr = new SendFileRequest(peer1.getPeerID(), null, "bl", new File("testing/test.jpg"));
            commFacade.getMessageProcessor().enableHooks(true);
            MessageHook hook = new MessageHook(sfr);
            commFacade.getMessageProcessor().installHook(hook, hook.getPredicate());
            commFacade.sendTCPMessage(sc, sfr);
            IEnvelope envelope = hook.waitForEnvelope(15);
            assertTrue(envelope != null);
            cm1.shutdown();
        } catch (BindException e) {
            e.printStackTrace();
            assertTrue("Got a bindexception", false);
        }
    }
}
