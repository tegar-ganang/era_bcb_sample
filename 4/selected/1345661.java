package net.sf.insim4j.client.impl.communicator;

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
import javax.inject.Inject;
import net.sf.insim4j.client.communicator.Communicator;
import net.sf.insim4j.client.impl.communicator.ChangeRequest.ChangeRequestType;
import net.sf.insim4j.i18n.LogMessages;
import net.sf.insim4j.utils.FormatUtils;
import net.sf.insim4j.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP {@link Communicator}.
 *
 * @author Jiří Sotona
 *
 */
public class TcpCommunicator extends AbstractCommunicator {

    /**
	 * Log for this class. NOT static. For more details see
	 * http://commons.apache.org/logging/guide.html#Obtaining%20a%20Log%20Object
	 */
    private final Logger logger = LoggerFactory.getLogger(TcpCommunicator.class);

    /**
	 * Constructor.
	 */
    @Inject
    private TcpCommunicator(final ChangeRequestFactory changeRequestFactory, final StringUtils stringUtils, final FormatUtils formatUtils) {
        super(changeRequestFactory, stringUtils, formatUtils);
    }

    @Override
    protected ConnectionInfo initiateConnection(final int localPort, final InetSocketAddress hostAddress) throws UnknownHostException, IOException {
        final SocketChannel socketChannel = SocketChannel.open();
        final InetSocketAddress localAddress = new InetSocketAddress(InetAddress.getLocalHost(), localPort);
        socketChannel.socket().bind(localAddress);
        socketChannel.configureBlocking(false);
        socketChannel.connect(hostAddress);
        final ConnectionInfo connectionInfo = new ConnectionInfo(hostAddress, socketChannel);
        final ChangeRequest changeRequest = fChangeRequestFactory.createChangeRequest(connectionInfo, ChangeRequestType.REGISTER, SelectionKey.OP_CONNECT);
        this.addChangeRequest(changeRequest);
        logger.info(fFormatUtils.format(LogMessages.getString("Client.initConnection"), localAddress, hostAddress));
        return connectionInfo;
    }

    @Override
    public boolean isConnected(final InetSocketAddress hostAddress) {
        final ConnectionInfo connInfo = fConnectionsInfo.get(hostAddress);
        final SocketChannel channel = (SocketChannel) connInfo.getChannel();
        if (channel == null) {
            return false;
        }
        return channel.isConnected();
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
            final List<ByteBuffer> sendQueue = fPendingRequestData.get(socketChannel);
            while (!sendQueue.isEmpty()) {
                final ByteBuffer buf = sendQueue.get(0);
                socketChannel.write(buf);
                if (buf.remaining() > 0) {
                    logger.debug(fFormatUtils.format(LogMessages.getString("Client.channel.fullBuffer"), socketChannel, buf.remaining()));
                    buf.compact();
                } else {
                    sendQueue.remove(0);
                }
                logger.debug(fFormatUtils.format(LogMessages.getString("Client.send.dataWritten"), socketChannel, fStringUtils.arrayToString(buf.array())));
            }
            if (sendQueue.isEmpty()) {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
        }
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
        logger.info(fFormatUtils.format(LogMessages.getString("Client.connected"), localAddress, hostAddress));
    }
}
