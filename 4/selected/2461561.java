package jssh;

/**
 * Sent by either party in interactive mode, this message indicates
 * that a connection has been opened to a forwarded TCP/IP port.
 */
class MSG_PORT_OPEN extends Packet implements IInteractivePacket {

    /** Use this constructor when receiving a packet from the network.
     */
    MSG_PORT_OPEN(byte[] data_) {
        super(data_);
    }

    /**
     * @param local_channel_ the channel number that the sending 
     * party has allocated for the connection.
     */
    MSG_PORT_OPEN(int local_channel_, String hostname_, int port_) {
        super();
        byte[] host_name = SSHOutputStream.string2Bytes(hostname_);
        int block_length = 1 + 4 + host_name.length + 4;
        super._data = new byte[block_length];
        int offset = 0;
        super._data[offset++] = SSH_MSG_PORT_OPEN;
        SSHOutputStream.insertInteger(local_channel_, offset, super._data);
        offset += 4;
        for (int i = 0; i < host_name.length; i++) super._data[offset++] = host_name[i];
        SSHOutputStream.insertInteger(port_, offset, super._data);
    }

    int getChannelNumber() {
        int offset = 1;
        return SSHInputStream.getInteger(offset, super._data);
    }

    String getHostname() {
        int offset = 1;
        offset += 4;
        return SSHInputStream.getString(offset, super._data);
    }

    int getPort() {
        int offset = 1;
        offset += 4;
        int string_length = SSHInputStream.getInteger(offset, super._data);
        offset += (4 + string_length);
        return SSHInputStream.getInteger(offset, super._data);
    }

    /** Implements the IInteractivePacket interface.
     */
    public void processPacket(IProtocolHandler handler_) {
        boolean allowed = handler_.isPortOpenAllowed(getHostname(), getPort());
        if (allowed) {
            OpenChannel channel = new OpenChannel(handler_, getHostname(), getPort(), getChannelNumber());
            Thread channelThread = new Thread(channel);
            channelThread.setDaemon(false);
            channelThread.start();
        } else {
            MSG_CHANNEL_OPEN_FAILURE reply = new MSG_CHANNEL_OPEN_FAILURE(getChannelNumber());
            handler_.enqueueToRemote(reply);
        }
    }
}
