package jssh;

/**
 * Data is transmitted in a channel in these messages. A channel is
 * bidirectional, and both sides can transmit these messages.
 */
class MSG_CHANNEL_DATA extends Packet implements IInteractivePacket {

    /** Use this constructor when receiving a packet from the network.
     */
    MSG_CHANNEL_DATA(byte[] data_) {
        super(data_);
    }

    /** Use this constructor when creating a packet to be sent on
     * the network.
     * Construct a SSH_MSG_CHANNEL_DATA packet containing the 
     * specified data.
     * @param channel_ the remote channel number
     * @param data_ an array of bytes containing the data.
     * @param nbytes_ the number of relevant bytes in the array.
     */
    MSG_CHANNEL_DATA(int channel_, byte[] data_, int nbytes_) {
        super();
        int block_length = 1 + 4 + 4 + nbytes_;
        super._data = new byte[block_length];
        int offset = 0;
        super._data[offset++] = SSH_MSG_CHANNEL_DATA;
        SSHOutputStream.insertInteger(channel_, offset, super._data);
        offset += 4;
        SSHOutputStream.insertInteger(nbytes_, offset, super._data);
        offset += 4;
        for (int i = 0; i < nbytes_; i++) super._data[offset++] = data_[i];
    }

    int getRemoteChannelNumber() {
        int offset = 1;
        return SSHInputStream.getInteger(offset, super._data);
    }

    byte[] getChannelData() {
        int offset = 5;
        int length = SSHInputStream.getInteger(offset, super._data);
        offset += 4;
        byte[] channel_data = new byte[length];
        System.arraycopy(super._data, offset, channel_data, 0, length);
        return channel_data;
    }

    /** Implements the IInteractivePacket interface.
     */
    public void processPacket(IProtocolHandler handler_) {
        OpenChannel channel = handler_.findOpenChannel(getRemoteChannelNumber());
        if (channel != null) channel.enqueue(this);
    }
}
