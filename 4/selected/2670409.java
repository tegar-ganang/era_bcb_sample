package org.ctor.dev.llrps2.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SessionStub {

    private static final Log LOG = LogFactory.getLog(SessionStub.class);

    private static final int READ_BUFFER_SIZE = 8024;

    private static final String LINE_TERMINATER = "\r\n";

    private static final Charset CHARSET = Charset.forName("US-ASCII");

    private final SocketChannel channel;

    private final RpsRole oppositeRole;

    private StringBuilder readBuffer = new StringBuilder();

    private StringBuilder writeBuffer = new StringBuilder();

    public SessionStub(SocketChannel channel, RpsRole oppositeRole) {
        this.channel = channel;
        this.oppositeRole = oppositeRole;
    }

    public void sendMessage(RpsCommand command, String... rest) {
        if (ArrayUtils.contains(rest, null)) {
            throw new IllegalArgumentException("command contains null");
        }
        final RpsMessage message = RpsMessage.create(command, rest);
        writeMessage(message.dump());
    }

    public RpsMessage receiveMessage() throws RpsSessionException {
        int pos = readBuffer.indexOf(LINE_TERMINATER);
        if (pos == -1) {
            return null;
        }
        final String line = readBuffer.substring(0, pos);
        readBuffer.delete(0, pos + LINE_TERMINATER.length());
        final RpsMessage message = RpsMessage.parse(oppositeRole, line);
        LOG.debug("received message: " + message.dump());
        return message;
    }

    public boolean hasCachedMessage() {
        return (readBuffer.indexOf(LINE_TERMINATER) != -1);
    }

    public void checkNoCachedMessage() {
        if (readBuffer.length() != 0) {
            throw new RpsCommunicationException("unexpected extra message: " + readBuffer.toString());
        }
    }

    public long read() throws IOException {
        try {
            final ByteBuffer buffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
            final long readSize = channel.read(buffer);
            if (readSize == -1) {
                close();
                LOG.info("reached end of stream");
                throw new IOException("reached end of stream");
            } else {
                buffer.flip();
                readBuffer.append(CHARSET.decode(buffer));
            }
            return readSize;
        } catch (IOException ioe) {
            close();
            LOG.info(ioe.getMessage(), ioe);
            throw ioe;
        }
    }

    public void flushWrite() throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(StringUtils.chop("sending message(s): " + writeBuffer.toString()));
        }
        try {
            channel.write(CHARSET.encode(writeBuffer.toString()));
        } catch (IOException ioe) {
            writeBuffer.setLength(0);
            LOG.debug(ioe.getMessage(), ioe);
            throw ioe;
        }
        writeBuffer.setLength(0);
    }

    public void close() throws IOException {
        LOG.info("closing channel");
        try {
            channel.close();
        } catch (ClosedChannelException cce) {
            LOG.debug(cce.getMessage(), cce);
        }
    }

    public SocketChannel getChannel() {
        return channel;
    }

    private void writeMessage(String message) {
        writeLine(message + LINE_TERMINATER);
    }

    private void writeLine(String line) {
        writeBuffer.append(line);
    }
}
