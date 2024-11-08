package jaxlib.net.ntp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import jaxlib.io.IO;
import jaxlib.lang.Longs;
import jaxlib.util.CheckArg;

/**
 * @author  jw
 * @since   JaXLib 1.0
 * @version $Id: NtpClient.java 2730 2009-04-21 01:12:29Z joerg_wassmer $
 */
public final class NtpClient extends Object implements Channel {

    /**
   * The NTP standard port.
   */
    public static final int PORT = 123;

    private final InetSocketAddress remoteAddress;

    private volatile DatagramChannel channel;

    private volatile DatagramSocket socket;

    public NtpClient(final InetSocketAddress remoteAddress) throws IOException {
        super();
        CheckArg.notNull(remoteAddress, "remoteAddress");
        this.remoteAddress = remoteAddress;
        this.channel = DatagramChannel.open();
        try {
            this.channel.connect(remoteAddress);
            this.socket = this.channel.socket();
        } catch (final RuntimeException ex) {
            IO.tryClose(this.channel);
            throw ex;
        } catch (final IOException ex) {
            IO.tryClose(this.channel);
            throw ex;
        }
    }

    public NtpClient(final InetAddress remoteAddress) throws IOException {
        this(new InetSocketAddress(remoteAddress, PORT));
    }

    private DatagramChannel getChannel() throws ClosedChannelException {
        final DatagramChannel channel = this.channel;
        if (channel == null) throw new ClosedChannelException();
        return channel;
    }

    private DatagramSocket getSocket() throws ClosedChannelException {
        final DatagramSocket socket = this.socket;
        if (socket == null) throw new ClosedChannelException();
        return socket;
    }

    @Override
    public final void close() throws IOException {
        final DatagramChannel channel = this.channel;
        if (channel != null) {
            this.channel = null;
            this.socket = null;
            channel.close();
        }
    }

    public final InetSocketAddress getRemoteAddress() {
        return this.remoteAddress;
    }

    public final int getSoTimeout() throws IOException {
        return getSocket().getSoTimeout();
    }

    @Override
    public final boolean isOpen() {
        return this.channel != null;
    }

    public final NtpResult poll() throws IOException {
        final DatagramChannel channel = getChannel();
        final byte[] a = NtpPacket.createRequest();
        final ByteBuffer buffer = ByteBuffer.wrap(a);
        final NtpTimestamp tsSend = NtpTimestamp.now();
        Longs.toBytes(tsSend.ntp, a, NtpPacket.TRANSMIT_TIMESTAMP_INDEX);
        channel.write(buffer);
        buffer.clear();
        channel.read(buffer);
        final NtpTimestamp tsReceive = NtpTimestamp.now();
        if (buffer.hasRemaining()) throw new ProtocolException("received incomplete packet, expected 48 bytes, got " + buffer.position());
        return new NtpResult(new NtpPacket(a), tsSend, tsReceive);
    }

    public final void setSoTimeout(final int millis) throws IOException {
        getSocket().setSoTimeout(millis);
    }
}
