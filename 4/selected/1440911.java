package jimo.modules.jgroups.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import jimo.modules.jgroups.api.JGroupsConstants;
import jimo.osgi.api.peer.Group;
import jimo.osgi.api.peer.Peer;
import jimo.osgi.api.peer.PeerAdmin;
import jimo.osgi.api.peer.PeerEvent;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelClosedException;
import org.jgroups.ChannelException;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.blocks.NotificationBus;
import org.jgroups.blocks.NotificationBus.Consumer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * An implementation of Group using JGroup`s NotificationBus.
 * The only problem here is that we can`t see which peer has
 * put the messages on the bus.
 * @author logicfish@hotmail.com
 *
 */
public class BusGroupImpl implements Group {

    NotificationBus bus;

    private String topic;

    /**
	 * The cache of the group properties.
	 * This is not the same as bus.getCacheFromOrganiser;
	 * the properties are set by the status event {@link PeerEvent.STATUS_GROUPUPDATED}.
	 */
    private Dictionary cache;

    /**
	 * Properties of the local host.  These will be sent to any
	 * peer that calls getProperties() on our local peer.
	 */
    private Dictionary localProperties = new Hashtable();

    /**
	 * The local peer.  
	 */
    private BusPeerImpl localPeer;

    private ServiceRegistration registration;

    private Consumer consumer;

    private PeerAdmin peerAdmin;

    LogService log;

    private ChannelListener listener;

    Set peers = Collections.synchronizedSet(new HashSet());

    void sendStatusEvent(String status) {
        Dictionary eventProperties;
        if (PeerEvent.STATUS_GROUPUPDATED.equals(status)) eventProperties = new Hashtable((Hashtable) cache); else eventProperties = new Hashtable();
        eventProperties.put(PeerEvent.EVENT_STATUS, status);
        PeerEvent event = new PeerEvent(localPeer, null, BusGroupImpl.this, PeerEvent.STATUS, eventProperties);
        peerAdmin.sendPeerEvent(event);
    }

    void sendEvent(String type, Dictionary eventProperties) {
        PeerEvent event = new PeerEvent(localPeer, null, BusGroupImpl.this, type, eventProperties);
        peerAdmin.sendPeerEvent(event);
    }

    public BusGroupImpl(BundleContext context, final Dictionary properties) throws ChannelException {
        topic = (String) properties.get(Group.TOPIC);
        registration = context.registerService(Group.class.getName(), this, properties);
        ServiceReference reference = context.getServiceReference(PeerAdmin.class.getName());
        peerAdmin = (PeerAdmin) context.getService(reference);
        reference = context.getServiceReference(LogService.class.getName());
        log = (LogService) context.getService(reference);
        String nameString = context.getProperty(PeerAdmin.KEY_LOCALNAME);
        if (nameString != null) localProperties.put(Peer.NAME, nameString);
        String hostString = context.getProperty(PeerAdmin.KEY_LOCALHOSTNAME);
        if (hostString != null) localProperties.put(Peer.HOST, hostString);
        localProperties.put(Peer.PROTOCOL, JGroupsConstants.PROTOCOL_JGROUPS);
        this.consumer = new Consumer() {

            public Serializable getCache() {
                return (Serializable) localProperties;
            }

            public void handleNotification(Serializable n) {
                log.log(LogService.LOG_DEBUG, "notification");
                Dictionary d = (Dictionary) n;
                String type = (String) d.get(PeerEvent.EVENT_TYPE);
                if (PeerEvent.EVENT_STATUS.equals(type)) {
                    handleStatusEvent(d);
                } else {
                    sendEvent(type, d);
                }
            }

            public void memberJoined(Address mbr) {
                if (bus.getLocalAddress().equals(mbr)) {
                    addPeer(localPeer);
                    return;
                }
                log.log(LogService.LOG_DEBUG, "memberJoined");
                Peer peer = new BusPeerImpl(mbr, BusGroupImpl.this);
                addPeer(peer);
                sendStatusEvent(PeerEvent.STATUS_GROUPUPDATED);
            }

            public void memberLeft(Address mbr) {
                if (bus.getLocalAddress().equals(mbr)) {
                    removePeer(localPeer);
                    return;
                }
                log.log(LogService.LOG_DEBUG, "memberLeft");
                Peer peer = new BusPeerImpl(mbr, BusGroupImpl.this);
                removePeer(peer);
                sendStatusEvent(PeerEvent.STATUS_GROUPUPDATED);
            }
        };
        this.listener = new ChannelListener() {

            public void channelClosed(Channel channel) {
                log.log(LogService.LOG_INFO, "group closed " + topic);
            }

            public void channelConnected(Channel channel) {
                if (isOwner()) {
                    log.log(LogService.LOG_INFO, "created group " + topic);
                } else {
                    log.log(LogService.LOG_INFO, "joined group " + topic);
                }
                Vector membership = bus.getMembership();
                for (Iterator iter = membership.iterator(); iter.hasNext(); ) {
                    Address address = (Address) iter.next();
                    if (!bus.getLocalAddress().equals(address)) addPeer(new BusPeerImpl(address, BusGroupImpl.this));
                }
            }

            public void channelDisconnected(Channel channel) {
                log.log(LogService.LOG_INFO, "disconnected from " + topic);
            }

            public void channelReconnected(Address addr) {
                log.log(LogService.LOG_INFO, "reconnected to " + topic);
            }

            public void channelShunned() {
                log.log(LogService.LOG_INFO, "shunned " + topic);
                bus.getChannel().disconnect();
                try {
                    bus.getChannel().connect(topic);
                } catch (ChannelClosedException e) {
                    log.log(LogService.LOG_ERROR, "Error reconnecting to " + topic, e);
                } catch (ChannelException e) {
                    log.log(LogService.LOG_ERROR, "Error reconnecting to " + topic, e);
                }
            }
        };
    }

    protected void removePeer(Peer peer) {
        if (isOwner()) ownerRemovePeer(peer);
        peers.remove(peer);
        log.log(LogService.LOG_INFO, "Peer left:" + peer.getName());
    }

    public void addPeer(Peer peer) {
        if (isOwner()) ownerAddPeer(peer);
        peers.add(peer);
        log.log(LogService.LOG_INFO, "Peer joined:" + peer.getName());
    }

    protected void ownerRemovePeer(Peer peer) {
        updatePeerList();
    }

    protected void ownerAddPeer(Peer peer) {
        updatePeerList();
    }

    private void updatePeerList() {
        Dictionary props = getProperties();
        Set set = new HashSet(peers);
        String[] peerStrings = new String[set.size()];
        int inx = 0;
        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            Peer element = (Peer) iter.next();
            peerStrings[inx++] = element.getName();
        }
        props.put(Group.PEERLIST, peerStrings);
        setProperties(props);
    }

    protected boolean isOwner() {
        return bus.isCoordinator();
    }

    protected void handleStatusEvent(Dictionary d) {
        String status = (String) d.get(PeerEvent.EVENT_STATUS);
        if (PeerEvent.STATUS_GROUPUPDATED.equals(status)) {
            if (!isOwner()) {
                cache = d;
                setProperties(cache);
            }
        } else {
        }
        sendStatusEvent(status);
    }

    public String getName() {
        return topic;
    }

    public Peer[] getPeers() {
        return (Peer[]) peers.toArray(new Peer[peers.size()]);
    }

    public Dictionary getProperties() {
        Dictionary res = new Hashtable();
        ServiceReference reference = registration.getReference();
        String[] propertyKeys = reference.getPropertyKeys();
        for (int i = 0; i < propertyKeys.length; i++) {
            String key = propertyKeys[i];
            Object value = reference.getProperty(key);
            res.put(key, value);
        }
        return res;
    }

    public Dictionary getLocalProperties() {
        return localProperties;
    }

    public void setProperties(Dictionary properties) {
        registration.setProperties(properties);
        if (bus == null) {
            return;
        }
        if (isOwner()) {
            cache = new Hashtable((Map) properties);
            sendStatusEvent(PeerEvent.STATUS_GROUPUPDATED);
        }
    }

    public String getTopic() {
        return topic;
    }

    public void connect() {
        if (isConnected()) {
            return;
        }
        String protocolString = (String) registration.getReference().getProperty(JGroupsConstants.JGROUPS_PROTOCOLSTRING);
        if (protocolString == null) protocolString = JGroupsActivator.INSTANCE.getBundleContext().getProperty(JGroupsConstants.JGROUPS_PROTOCOLSTRING);
        if (protocolString == null) protocolString = JGroupsConstants.DEFAULT_PROTOCOLSTRING;
        try {
            log.log(LogService.LOG_INFO, "JGroups protocol = " + protocolString);
            bus = new NotificationBus(topic, protocolString);
            bus.getChannel().addChannelListener(listener);
            bus.setConsumer(consumer);
            bus.getChannel().setOpt(JChannel.AUTO_RECONNECT, Boolean.TRUE);
            bus.start();
            localPeer = new BusPeerImpl(bus.getLocalAddress(), this);
        } catch (Exception e) {
            e.printStackTrace();
            log.log(LogService.LOG_ERROR, e.getMessage(), e);
        }
    }

    public void disconnect() {
        if (bus == null) {
            return;
        }
        bus.stop();
    }

    public Peer getOwner() {
        if (bus.isCoordinator()) return localPeer;
        Address owner = (Address) bus.getMembership().firstElement();
        Peer peer = new BusPeerImpl(owner, this);
        return peer;
    }

    public void send(PeerEvent event) {
        event.setGroup(this);
        if (event.getTarget() == null) bus.sendNotification((Serializable) event.getProperties()); else bus.sendNotification(((BusPeerImpl) event.getTarget()).getAddress(), (Serializable) event.getProperties());
    }

    public Peer getLocalPeer() {
        return localPeer;
    }

    public void setRegistration(ServiceRegistration registration) {
        this.registration = registration;
    }

    public boolean isConnected() {
        if (bus == null || bus.getChannel() == null) {
            return false;
        }
        return bus.getChannel().isConnected();
    }
}
