package de.tud.kom.nat.im.model;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import de.tud.kom.nat.comm.msg.IPeer;
import de.tud.kom.nat.comm.msg.IPeerID;
import de.tud.kom.nat.comm.msg.Peer;

/**
 * A chatpartner is a person which is on the chatter list. He has a nickname, an
 * address and a unique userID. Additionally, the channel which transports messages to him
 * is stored here.
 * 
 * @author Matthias Weinert
 */
class ChatPartner implements IChatPartner {

    /** The nickname of the chatter. */
    private String nickname;

    /** The socketaddress of the chatter. */
    private InetSocketAddress usedAddr;

    /** The used socket address and his userID. */
    private Peer peer;

    /** The relay address which might be used when the contact can not be established directly. */
    private InetSocketAddress relay;

    /** Channel to the chatpartner. */
    private DatagramChannel channel;

    /**
	 * Creates a chatpartner with a non-public connection.
	 * 
	 * @param nickname his nickname
	 * @param userID his userID
	 * @param addr his address
	 * @param relayHost the address of the relay host
	 */
    public ChatPartner(String nickname, IPeerID userID, InetSocketAddress publicAddr, InetSocketAddress usedAddr) {
        this(nickname, userID, publicAddr, usedAddr, null);
    }

    /**
	 * Creates a chatpartner whose connection is relayed.
	 * 
	 * @param nickname his nickname
	 * @param userID his userID
	 * @param addr his address
	 * @param relayHost the address of the relay host
	 */
    public ChatPartner(String nickname, IPeerID userID, InetSocketAddress publicAddr, InetSocketAddress usedAddr, InetSocketAddress relayHost) {
        this.nickname = nickname;
        this.peer = new Peer(userID, publicAddr);
        this.relay = relayHost;
        this.usedAddr = usedAddr;
    }

    /**
	 * Returns the nickname of the chatter.
	 * 
	 * @return nickname
	 */
    public String getNickname() {
        return nickname;
    }

    /**
	 * Returns the address of the chatter.
	 * 
	 * @return address
	 */
    public InetSocketAddress getPublicAddress() {
        return peer.getAddress();
    }

    public IPeerID getPeerID() {
        return peer.getPeerID();
    }

    /**
	 * Returns the address which we use to communicate with the chatpartner.
	 * @return address which we use to communicate with the chatpartner
	 */
    public InetSocketAddress getUsedAddress() {
        return usedAddr;
    }

    /**
	 * Returns the relay-host for this chatpartner.
	 * @return relayhost for this chatpartner
	 */
    public InetSocketAddress getRelayHost() {
        return relay;
    }

    /**
	 * Returns the channel to the chatpartner.
	 * @return channel to the chatpartner
	 */
    public DatagramChannel getChannel() {
        return channel;
    }

    /**
	 * Sets the channel to the chatpartner.
	 * @param channel channel to the chatpartner
	 */
    public void setChannel(DatagramChannel channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return nickname + " [" + peer.getAddress() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChatPartner)) return false;
        return ((ChatPartner) o).peer.equals(peer);
    }

    public IPeer getPeer() {
        return peer;
    }
}
