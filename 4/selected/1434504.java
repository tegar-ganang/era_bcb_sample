package de.tud.kom.nat.nattrav.conn;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import de.tud.kom.nat.comm.ICommFacade;
import de.tud.kom.nat.comm.IMessageHandler;
import de.tud.kom.nat.comm.IMessageProcessor;
import de.tud.kom.nat.comm.msg.IEnvelope;
import de.tud.kom.nat.comm.msg.IMessage;
import de.tud.kom.nat.comm.msg.IPeerID;
import de.tud.kom.nat.comm.msg.UDPPing;
import de.tud.kom.nat.comm.util.BlockingHook;
import de.tud.kom.nat.nattrav.broker.IConnectionBroker;
import de.tud.kom.nat.nattrav.conn.msg.PerformUDPPunchAnswer;
import de.tud.kom.nat.nattrav.conn.msg.PerformUDPPunchRequest;
import de.tud.kom.nat.nattrav.conn.msg.MappedAddrAnswer;
import de.tud.kom.nat.nattrav.conn.msg.MappedAddrRequest;
import de.tud.kom.nat.nattrav.conn.msg.MappedAddressResult;
import de.tud.kom.nat.nattrav.conn.msg.RelayMessage;
import de.tud.kom.nat.nattrav.conn.msg.TCPConnAnswer;
import de.tud.kom.nat.nattrav.conn.msg.TCPConnRequest;
import de.tud.kom.nat.nattrav.conn.msg.UDPPunchAnswer;
import de.tud.kom.nat.nattrav.conn.msg.UDPPunchRequest;
import de.tud.kom.nat.util.InetAddressUtils;
import de.tud.kom.nat.util.Logger;

/**
 * Responsible for handling the control messages of NatTrav.
 *
 * @author Matthias Weinert
 */
public class NatMessageHandler implements IMessageHandler, IMappedAddrResolver {

    private NatConnector natConnector;

    private IConnectionBroker connBroker;

    private ICommFacade commFacade;

    private final IPeerID myPeerID;

    public NatMessageHandler(NatConnector natConnector, IConnectionBroker broker) {
        this.natConnector = natConnector;
        this.connBroker = broker;
        this.commFacade = broker.getCommFacade();
        this.myPeerID = broker.getCallback().getOwnPeerID();
        registerMessageTypes();
    }

    private void registerMessageTypes() {
        IMessageProcessor msgProc = commFacade.getMessageProcessor();
        msgProc.registerMessageHandler(UDPPunchRequest.class, this);
        msgProc.registerMessageHandler(UDPPunchAnswer.class, this);
        msgProc.registerMessageHandler(PerformUDPPunchRequest.class, this);
        msgProc.registerMessageHandler(PerformUDPPunchAnswer.class, this);
        msgProc.registerMessageHandler(MappedAddrRequest.class, this);
        msgProc.registerMessageHandler(MappedAddrAnswer.class, this);
        msgProc.registerMessageHandler(MappedAddressResult.class, this);
        msgProc.registerMessageHandler(RelayMessage.class, this);
        msgProc.registerMessageHandler(TCPConnRequest.class, this);
        msgProc.registerMessageHandler(TCPConnAnswer.class, this);
    }

    public void onMessageReceived(IEnvelope env) {
        IMessage msg = env.getMessage();
        if (msg instanceof UDPPunchRequest) onReceiveUDPPunch(env, (UDPPunchRequest) msg); else if (msg instanceof PerformUDPPunchRequest) onReceivePerformUDPPunch(env, (PerformUDPPunchRequest) msg); else if (msg instanceof PerformUDPPunchAnswer) onReceivePerformUDPAnswer(env, (PerformUDPPunchAnswer) msg); else if (msg instanceof MappedAddrRequest) onReceivePortPredRequest(env, (MappedAddrRequest) msg); else if (msg instanceof RelayMessage) onReceiveRelayMessage(env, (RelayMessage) msg); else if (msg instanceof TCPConnRequest) onReceiveTCPConnRequest(env, (TCPConnRequest) msg);
    }

    private void onReceiveTCPConnRequest(IEnvelope env, TCPConnRequest request) {
        SocketChannel sc = null;
        try {
            sc = SocketChannel.open(request.getTargetAddress());
            sc.configureBlocking(false);
            commFacade.getChannelManager().registerChannel(sc);
            if (sc == null || !sc.isOpen() || !sc.isConnected()) throw new IOException("Could not connect to " + request.getTargetAddress());
            TCPConnAnswer answer = new TCPConnAnswer(myPeerID, env.getSender().getPeerID(), request);
            commFacade.sendTCPMessage(sc, answer);
        } catch (IOException e) {
            Logger.logError(e, "Introduction with target of TCPConnRequest failed!");
            try {
                if (sc != null && sc.isOpen()) sc.close();
            } catch (IOException e1) {
            }
        }
    }

    /**
	 * Unlike the UDPPunchMessage, this message is received by the RelayHost for the
	 * target peer. The relay host sends the necessary punch requests to the target
	 * host and returns the appropriate answer to the requesting host.
	 * 
	 * @param env envelope
	 * @param request request
	 */
    private void onReceivePerformUDPPunch(IEnvelope env, PerformUDPPunchRequest request) {
        IPeerID senderID = env.getSender().getPeerID();
        UDPPunchAnswer punchAnswer = writePunch(request.getTargetPeer(), request.getFromPort(), request.getPunchTargets(), true);
        PerformUDPPunchAnswer answer = null;
        if (punchAnswer == null) {
            answer = new PerformUDPPunchAnswer(myPeerID, senderID, request, false, "Did not receive punch answer!", new InetSocketAddress[0]);
        } else if (!punchAnswer.isSuccessful()) {
            answer = new PerformUDPPunchAnswer(myPeerID, senderID, request, false, "Punching failed: " + punchAnswer.getReason(), new InetSocketAddress[0]);
        } else answer = new PerformUDPPunchAnswer(myPeerID, senderID, request, true, "", punchAnswer.getPredicatedAddr());
        try {
            connBroker.getCommFacade().sendTCPMessage((SocketChannel) env.getChannel(), answer);
        } catch (IOException e) {
            Logger.logError(e, "Failed to write PerformUDPPunchAnswer back to " + env.getSender());
        }
    }

    private void onReceivePerformUDPAnswer(IEnvelope env, PerformUDPPunchAnswer answer) {
    }

    private void onReceiveUDPPunch(IEnvelope env, UDPPunchRequest message) {
        final DatagramChannel dc = natConnector.getRegisteredUDPChannel(message.getFromPort());
        if (dc == null) {
            UDPPunchAnswer answer = new UDPPunchAnswer(myPeerID, env.getSender().getPeerID(), message, false, "Channel with that port is not registered!");
            try {
                commFacade.sendTCPMessage((SocketChannel) env.getChannel(), answer);
            } catch (IOException e) {
            }
        } else {
            try {
                InetSocketAddress pred[] = null;
                if (message.needPortPrediction()) {
                    pred = connBroker.getMappedAddrResolver().getPossiblePorts(dc, message.getPunchTargets()[0]);
                }
                for (InetSocketAddress addr : message.getPunchTargets()) commFacade.sendUDPMessage(dc, new UDPPing(myPeerID), addr);
                commFacade.sendTCPMessage((SocketChannel) env.getChannel(), new UDPPunchAnswer(myPeerID, env.getSender().getPeerID(), message, true, "", pred));
            } catch (IOException e) {
                Logger.logError(e, "Exception durion port prediction!");
            }
        }
    }

    private UDPPunchAnswer writePunch(IPeerID target, int fromPort, InetSocketAddress punchTargets[], boolean pred) {
        SocketChannel controlToTarget = natConnector.getControlChannelWith(target);
        if (controlToTarget == null) {
            Logger.logWarning("Unable to punch, no control channel to target peer!");
            return null;
        }
        UDPPunchRequest punch = new UDPPunchRequest(myPeerID, target, fromPort, pred, punchTargets);
        BlockingHook bh = BlockingHook.createAwaitMessageHook(controlToTarget, UDPPunchAnswer.class);
        IMessageProcessor msgProc = connBroker.getCommFacade().getMessageProcessor();
        msgProc.installHook(bh, bh.getPredicate());
        try {
            try {
                connBroker.getCommFacade().sendTCPMessage(controlToTarget, punch);
            } catch (IOException e) {
            }
            IEnvelope env = bh.waitForMessage();
            if (env == null) {
                Logger.logWarning("Have not received punch answer!");
                return null;
            }
            return (UDPPunchAnswer) env.getMessage();
        } finally {
            msgProc.removeHook(bh);
        }
    }

    private void onReceiveRelayMessage(IEnvelope env, RelayMessage message) {
        SocketChannel sc = natConnector.getControlChannelWith(message.getRelayTarget());
        if (sc == null) {
            Logger.logError(new IllegalStateException(), "We do not have a control connection to the target of the RelayMessage!");
            return;
        }
        try {
            commFacade.sendTCPMessage(sc, message.getMessage());
        } catch (IOException e) {
        }
    }

    public InetSocketAddress[] getPossiblePorts(DatagramChannel channelToUse, InetSocketAddress newTarget) {
        ICommFacade commFacade = connBroker.getCommFacade();
        Iterator<SocketChannel> it = natConnector.getControlChannelIterator();
        if (!it.hasNext()) {
            Logger.logWarning("We cannot use port prediction due to the fact that we have not established a single control channel!");
            return new InetSocketAddress[0];
        }
        final SocketChannel controlChannel = it.next();
        BlockingHook bh = BlockingHook.createAwaitMessageHook(controlChannel, MappedAddrAnswer.class);
        InetSocketAddress testAddress = null;
        commFacade.getMessageProcessor().installHook(bh, bh.getPredicate());
        try {
            try {
                commFacade.sendTCPMessage(controlChannel, new MappedAddrRequest(myPeerID, null));
                IEnvelope env = bh.waitForMessage();
                testAddress = ((MappedAddrAnswer) env.getMessage()).getTestAddress();
            } catch (IOException e) {
                Logger.logError(e, "Could not send port prediction request.");
                return new InetSocketAddress[0];
            }
        } finally {
            commFacade.getMessageProcessor().removeHook(bh);
        }
        if (testAddress == null) {
            Logger.logError(new IllegalStateException("TestAddress must not be null in the MappedAddrAnswer!"), "");
            return new InetSocketAddress[0];
        }
        BlockingHook hookResults = BlockingHook.createAwaitMessageHook(controlChannel, MappedAddressResult.class);
        InetSocketAddress mappedAddress = executeTest(channelToUse, testAddress, hookResults);
        if (mappedAddress == null) {
            return new InetSocketAddress[0];
        }
        return new InetSocketAddress[] { mappedAddress };
    }

    /**
	 * Sends a ping to the given test address and waits till the <tt>MappedAddressResult</tt> is received
	 * (using the given <tt>hook</tt>).
	 * @param channelToUse 
	 * 
	 * @param testAddress address to send the ping
	 * @param hook hook which waits until the results are received
	 * @return the received mapped address or null, if something went wrong
	 */
    private InetSocketAddress executeTest(DatagramChannel channelToUse, InetSocketAddress testAddress, BlockingHook hook) {
        ICommFacade commFacade = connBroker.getCommFacade();
        IMessageProcessor msgProc = commFacade.getMessageProcessor();
        DatagramChannel dc = channelToUse;
        boolean unregisterAfterTest = commFacade.getChannelManager().getSelectionKey(channelToUse) == null;
        if (!unregisterAfterTest) commFacade.getChannelManager().registerChannel(dc);
        msgProc.installHook(hook, hook.getPredicate());
        try {
            try {
                InetSocketAddress result = null;
                commFacade.getChannelManager().registerChannel(dc);
                commFacade.sendUDPMessage(dc, new UDPPing(myPeerID), testAddress);
                IEnvelope env = hook.waitForMessage();
                if (env != null) result = ((MappedAddressResult) env.getMessage()).getMappedAddress();
                return result;
            } catch (IOException e) {
                Logger.logError(e, "Unable to send ping to test address!");
                return null;
            }
        } finally {
            msgProc.removeHook(hook);
            if (unregisterAfterTest) commFacade.getChannelManager().unregisterChannel(channelToUse);
        }
    }

    private void onReceivePortPredRequest(IEnvelope env, MappedAddrRequest request) {
        DatagramChannel dc = openDatagramChannel();
        try {
            BlockingHook bh = BlockingHook.createAwaitMessageHook(dc, UDPPing.class);
            IEnvelope ping = null;
            connBroker.getCommFacade().getMessageProcessor().installHook(bh, bh.getPredicate());
            try {
                try {
                    InetSocketAddress addr = new InetSocketAddress(InetAddressUtils.getMostProbableExternalAddress(), dc.socket().getLocalPort());
                    connBroker.getCommFacade().sendTCPMessage((SocketChannel) env.getChannel(), new MappedAddrAnswer(myPeerID, env.getSender().getPeerID(), addr));
                } catch (IOException e) {
                    Logger.logError(e, "Error handling port prediction request.");
                    return;
                }
                ping = bh.waitForMessage();
            } finally {
                connBroker.getCommFacade().getMessageProcessor().removeHook(bh);
            }
            if (ping == null) Logger.logWarning("PortPrediction did not receive the required Ping-message!");
            MappedAddressResult result = new MappedAddressResult(myPeerID, env.getSender().getPeerID(), ping != null ? ping.getSender().getAddress() : null);
            try {
                connBroker.getCommFacade().sendTCPMessage((SocketChannel) env.getChannel(), result);
            } catch (IOException e) {
                Logger.logError(e, "Error sending results of port prediction!");
            }
        } finally {
            try {
                dc.close();
            } catch (IOException e) {
            }
        }
    }

    private DatagramChannel openDatagramChannel() {
        DatagramChannel dc = null;
        try {
            dc = DatagramChannel.open();
            dc.socket().bind(null);
            dc.configureBlocking(false);
            connBroker.getCommFacade().getChannelManager().registerChannel(dc);
            return dc;
        } catch (IOException e) {
            Logger.logError(e, "Error creating datagram channel!");
            throw new IllegalStateException("Error creating datagram channel!");
        }
    }
}
