package org.metastatic.net.ssh2;

/**
 * <p>This is a packet as it waits in the queue to be sent over the wire.
 * It contains only two fields, the {@link #channel}, which provides the
 * SSH2 channel's recipient ID and window size, and the {@link
 * #payload}, which is the raw data to be sent over the connection. This
 * class gives merely the convenience of having both in the same place.</p>
 */
class ChannelPacket {

    /**
    * The channel this packet will be sent over.
    */
    protected Channel channel;

    /**
    * The payload of this packet.
    */
    protected byte[] payload;

    /**
    * The size of window-consumable data.
    */
    protected int data_length;

    ChannelPacket(Channel channel, byte[] payload, int data_length) {
        this.channel = channel;
        this.payload = payload;
        this.data_length = data_length;
    }

    Channel getChannel() {
        return channel;
    }

    byte[] getPayload() {
        return payload;
    }

    int getDataLength() {
        return data_length;
    }

    boolean consumesWindow() {
        return data_length > 0;
    }
}
