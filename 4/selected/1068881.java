package net.sf.insim4j.client.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;
import net.sf.insim4j.InSimHost;
import net.sf.insim4j.client.ResponseHandler;
import net.sf.insim4j.client.impl.ChangeRequest.ChangeRequestType;
import net.sf.insim4j.i18n.LogMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP client.
 *
 * @author Jiří Sotona
 *
 */
public class TCPClient extends AbstractInSimNioClient {

    /**
	 * Log for this class. NOT static. For more details see
	 * http://commons.apache.org/logging/guide.html#Obtaining%20a%20Log%20Object
	 */
    private final Logger logger = LoggerFactory.getLogger(TCPClient.class);

    /**
	 * Constructor.
	 */
    TCPClient() {
        super();
    }

    @Override
    protected byte[] read(final SelectionKey selectionKey) throws IOException {
        final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        fReadBuffer.clear();
        int numRead;
        try {
            numRead = socketChannel.read(fReadBuffer);
        } catch (final IOException e) {
            selectionKey.cancel();
            socketChannel.close();
            return new byte[0];
        }
        if (numRead == -1) {
            selectionKey.channel().close();
            selectionKey.cancel();
            return new byte[0];
        }
        final byte[] rspData = new byte[numRead];
        System.arraycopy(fReadBuffer.array(), 0, rspData, 0, numRead);
        return rspData;
    }

    @SuppressWarnings("boxing")
    @Override
    protected void write(final SelectionKey selectionKey) throws IOException {
        final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        synchronized (fPendingRequestData) {
            final List<ByteBuffer> queue = fPendingRequestData.get(socketChannel);
            while (!queue.isEmpty()) {
                final ByteBuffer buf = queue.get(0);
                socketChannel.write(buf);
                if (buf.remaining() > 0) {
                    logger.debug(FORMAT_UTILS.format(LogMessages.getString("Client.channel.fullBuffer"), socketChannel, buf.remaining()));
                    buf.compact();
                } else {
                    queue.remove(0);
                }
                logger.debug(FORMAT_UTILS.format(LogMessages.getString("Client.send.dataWritten"), socketChannel, STRING_UTILS.arrayToString(buf.array())));
            }
            if (queue.isEmpty()) {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
        }
    }

    @Override
    protected ConnectionInfo initiateConnection(final int localPort, final InSimHost host, final ResponseHandler responseHandler) throws UnknownHostException, IOException {
        final SocketChannel socketChannel = SocketChannel.open();
        final InetSocketAddress localAddress = new InetSocketAddress(InetAddress.getLocalHost(), localPort);
        socketChannel.socket().bind(localAddress);
        socketChannel.configureBlocking(false);
        final InetSocketAddress hostAddress = host.getSocketAddress();
        socketChannel.connect(hostAddress);
        final ConnectionInfo connectionInfo = new ConnectionInfo(host, socketChannel, localPort, responseHandler);
        final ChangeRequest changeRequest = new ChangeRequest(connectionInfo, ChangeRequestType.REGISTER, SelectionKey.OP_CONNECT);
        this.addChangeRequest(changeRequest);
        logger.info(FORMAT_UTILS.format(LogMessages.getString("Client.initConnection"), localAddress, hostAddress));
        return connectionInfo;
    }

    @Override
    protected void finishConnection(final SelectionKey selectionKey) throws IOException {
        final SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        try {
            socketChannel.finishConnect();
        } catch (final IOException e) {
            selectionKey.cancel();
            throw e;
        }
        final Socket socket = socketChannel.socket();
        final SocketAddress localAddress = socket.getLocalSocketAddress();
        final SocketAddress hostAddress = socket.getRemoteSocketAddress();
        logger.info(FORMAT_UTILS.format(LogMessages.getString("Client.connected"), localAddress, hostAddress));
    }

    @Override
    public boolean isConnected(final InSimHost host) {
        final ConnectionInfo connInfo = fConnectionsInfo.get(host);
        final SocketChannel channel = (SocketChannel) connInfo.getChannel();
        if (channel == null) {
            return false;
        }
        return channel.isConnected();
    }
}
