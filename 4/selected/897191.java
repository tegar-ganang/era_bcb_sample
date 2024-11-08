package jssh;

import java.net.*;
import java.io.*;

/**
 * This class represents an open SSH channel.
 */
class OpenChannel implements Runnable, IPacketConstants {

    /** Use this constructor for a channel that implements remote-to-local
     * port-forwarding.
     */
    OpenChannel(IProtocolHandler handler_, String hostname_, int localport_, int remoteChannelNumber_) {
        _handler = handler_;
        _hostname = hostname_;
        _localport = localport_;
        _remoteChannelNumber = remoteChannelNumber_;
    }

    /** Use this constructor for a channel that implements local-to-remote
     * port-forwarding. We have to send a SSH_MSG_PORT_OPEN to the remote 
     * side, containing the specified hostname and port,  and wait
     * for a SSH_MSG_CHANNEL_OPEN_CONFIRMATION in reply.
     */
    OpenChannel(IProtocolHandler handler_, Socket socket_, String hostname_, int remoteport_) {
        _handler = handler_;
        _socket = socket_;
        _hostname = hostname_;
        _remoteport = remoteport_;
    }

    void setLocalChannelNumber(int id_) {
        _localChannelNumber = id_;
    }

    int getLocalChannelNumber() {
        return _localChannelNumber;
    }

    int getRemoteChannelNumber() {
        return _remoteChannelNumber;
    }

    String getHostname() {
        return _hostname;
    }

    /** Enqueue an SSH packet onto the input queue of this channel.
     */
    void enqueue(Packet packet_) {
        _queue.enqueue(packet_);
    }

    public void run() {
        if (_socket != null) {
            MSG_PORT_OPEN open_packet = new MSG_PORT_OPEN(_localChannelNumber, _hostname, _remoteport);
            _handler.enqueueToRemote(open_packet);
            Packet reply = _queue.getNextPacket();
            if (reply.getPacketType() != SSH_MSG_CHANNEL_OPEN_CONFIRMATION) {
                try {
                    _socket.close();
                } catch (IOException e) {
                }
                return;
            }
            MSG_CHANNEL_OPEN_CONFIRMATION confirm = (MSG_CHANNEL_OPEN_CONFIRMATION) reply;
            _remoteChannelNumber = confirm.getLocalChannelNumber();
        } else {
            try {
                InetAddress address = InetAddress.getByName(_hostname);
                _socket = new Socket(address, _localport);
            } catch (IOException e) {
                MSG_CHANNEL_OPEN_FAILURE reply = new MSG_CHANNEL_OPEN_FAILURE(_remoteChannelNumber);
                _handler.enqueueToRemote(reply);
                return;
            }
            _handler.registerOpenChannel(this);
            MSG_CHANNEL_OPEN_CONFIRMATION reply = new MSG_CHANNEL_OPEN_CONFIRMATION(_remoteChannelNumber, getLocalChannelNumber());
            _handler.enqueueToRemote(reply);
        }
        try {
            ChannelReader channel_reader = new ChannelReader(_socket.getInputStream());
            channel_reader.setDaemon(false);
            channel_reader.start();
            OutputStream out = _socket.getOutputStream();
            for (; ; ) {
                Packet packet = _queue.getNextPacket();
                int type = packet.getPacketType();
                if (type == SSH_MSG_CHANNEL_DATA) {
                    MSG_CHANNEL_DATA data_packet = (MSG_CHANNEL_DATA) packet;
                    out.write(data_packet.getChannelData());
                } else if (type == SSH_MSG_CHANNEL_CLOSE) {
                    _socket.shutdownOutput();
                    _socket.close();
                    MSG_CHANNEL_CLOSE_CONFIRMATION confirmation = new MSG_CHANNEL_CLOSE_CONFIRMATION(_remoteChannelNumber);
                    _handler.enqueueToRemote(confirmation);
                    break;
                } else break;
            }
        } catch (IOException e) {
            MSG_CHANNEL_CLOSE close_packet = new MSG_CHANNEL_CLOSE(_remoteChannelNumber);
            _handler.enqueueToRemote(close_packet);
            try {
                _socket.close();
            } catch (Exception e2) {
            }
        }
        _handler.removeOpenChannel(this);
        return;
    }

    /** A nonstatic inner class that runs in its own thread, reading data
     * from the network and forwarding it to the remote side via the 
     * encrypted channel.
     */
    private class ChannelReader extends Thread {

        /** Constructor
	 */
        ChannelReader(InputStream in_) {
            _in = in_;
        }

        /** Implements the Runnable interface
	 */
        public void run() {
            byte[] buf = new byte[1024];
            int n;
            for (; ; ) {
                try {
                    n = _in.read(buf);
                } catch (IOException e) {
                    n = -1;
                }
                if (n == -1) {
                    MSG_CHANNEL_CLOSE close_packet = new MSG_CHANNEL_CLOSE(_remoteChannelNumber);
                    _handler.enqueueToRemote(close_packet);
                    break;
                } else {
                    MSG_CHANNEL_DATA data_packet = new MSG_CHANNEL_DATA(_remoteChannelNumber, buf, n);
                    _handler.enqueueToRemote(data_packet);
                }
            }
        }

        private InputStream _in;
    }

    private IProtocolHandler _handler;

    private PacketQueue _queue = new PacketQueue();

    private int _localChannelNumber;

    private int _remoteChannelNumber;

    private Socket _socket;

    private String _hostname = null;

    private int _remoteport;

    private int _localport;
}
