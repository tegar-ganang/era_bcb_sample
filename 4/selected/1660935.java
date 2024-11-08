package phex.net.repres.i2p;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.channels.ByteChannel;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFull;
import net.i2p.client.streaming.I2PServerSocket;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.data.Destination;
import net.i2p.data.DataFormatException;
import net.i2p.I2PException;
import phex.common.address.I2PDestAddress;
import phex.common.address.DestAddress;
import phex.io.channels.StreamingByteChannel;
import phex.net.repres.SocketFacade;

public class I2PSocketFacade implements SocketFacade {

    private I2PSocket socket;

    private StreamingByteChannel channel;

    private DestAddress remoteAddress;

    public I2PSocketFacade(I2PSocket aSocket) {
        this.socket = aSocket;
    }

    public void setSoTimeout(int socketRWTimeout) throws SocketException {
        socket.setReadTimeout((long) socketRWTimeout);
    }

    public ByteChannel getChannel() throws IOException {
        if (channel == null) {
            channel = new StreamingByteChannel(socket.getInputStream(), socket.getOutputStream());
        }
        return channel;
    }

    public void close() throws IOException {
        socket.close();
    }

    public DestAddress getRemoteAddress() {
        if (remoteAddress == null) {
            remoteAddress = new I2PDestAddress(socket.getPeerDestination());
        }
        return remoteAddress;
    }
}
