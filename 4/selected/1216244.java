package phex.net.repres.def;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ByteChannel;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.io.channels.StreamingByteChannel;
import phex.net.repres.SocketFacade;

public class DefaultSocketFacade implements SocketFacade {

    private DestAddress remoteAddress;

    private Socket socket;

    private StreamingByteChannel channel;

    public DefaultSocketFacade(Socket aSocket) {
        socket = aSocket;
    }

    public ByteChannel getChannel() throws IOException {
        if (channel == null) {
            channel = new StreamingByteChannel(socket);
        }
        return channel;
    }

    public void setSoTimeout(int socketRWTimeout) throws SocketException {
        socket.setSoTimeout(socketRWTimeout);
    }

    public void close() throws IOException {
        socket.close();
    }

    public DestAddress getRemoteAddress() {
        if (remoteAddress == null) {
            remoteAddress = new DefaultDestAddress(socket.getInetAddress().getHostAddress(), socket.getPort());
        }
        return remoteAddress;
    }
}
