package fr.x9c.cadmium.primitives.unix;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Context;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.FalseExit;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for socket i/o-related primitives.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.0
 * @since 1.0
 */
@PrimitiveProvider
public final class Sendrecv {

    /**
     * No instance of this class.
     */
    private Sendrecv() {
    }

    /**
     * Receives a packet on a connected socket.
     * @param ctxt context
     * @param socket socket
     * @param buff buffer of data to receive
     * @param ofs offset of data to receive
     * @param len maximum length of data to receive
     * @param flags ignored
     * @return number of bytes received
     * @throws Fail.Exception if an i/o error occurs
     */
    @Primitive
    public static Value unix_recv(final CodeRunner ctxt, final Value socket, final Value buff, final Value ofs, final Value len, final Value flags) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        try {
            final Channel ch = context.getChannel(socket.asLong());
            if (ch == null) {
                Unix.fail(ctxt, "recv", Unix.INVALID_DESCRIPTOR_MSG);
                return Value.UNIT;
            }
            final java.net.Socket s = ch.asSocket();
            final DatagramSocket ds = ch.asDatagramSocket();
            if (s != null) {
                context.enterBlockingSection();
                final int res = s.getInputStream().read(buff.asBlock().getBytes(), ofs.asLong(), len.asLong());
                context.leaveBlockingSection();
                return Value.createFromLong(res);
            } else if (ds != null) {
                final DatagramPacket p = new DatagramPacket(buff.asBlock().getBytes(), ofs.asLong(), len.asLong());
                context.enterBlockingSection();
                ds.receive(p);
                context.leaveBlockingSection();
                return Value.createFromLong(p.getLength());
            } else {
                Unix.fail(ctxt, "recv", Unix.INVALID_DESCRIPTOR_MSG);
                return Value.UNIT;
            }
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            context.leaveBlockingSection();
            Unix.fail(ctxt, "recv", ioe);
            return Value.UNIT;
        }
    }

    /**
     * Sends a packet on a connected socket.
     * @param ctxt context
     * @param socket socket
     * @param buff buffer of data to send
     * @param ofs offset of data to send
     * @param len length of data to send
     * @param flags ignored
     * @return number of bytes sent
     * @throws Fail.Exception if an i/o error occurs
     */
    @Primitive
    public static Value unix_send(final CodeRunner ctxt, final Value socket, final Value buff, final Value ofs, final Value len, final Value flags) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        try {
            final Channel ch = context.getChannel(socket.asLong());
            if (ch == null) {
                Unix.fail(ctxt, "send", Unix.INVALID_DESCRIPTOR_MSG);
                return Value.UNIT;
            }
            final java.net.Socket s = ch.asSocket();
            final DatagramSocket ds = ch.asDatagramSocket();
            if (s != null) {
                context.enterBlockingSection();
                s.getOutputStream().write(buff.asBlock().getBytes(), ofs.asLong(), len.asLong());
                context.leaveBlockingSection();
                return len;
            } else if (ds != null) {
                final DatagramPacket p = new DatagramPacket(buff.asBlock().getBytes(), ofs.asLong(), len.asLong(), ds.getRemoteSocketAddress());
                context.enterBlockingSection();
                ds.send(p);
                context.leaveBlockingSection();
                return len;
            } else {
                Unix.fail(ctxt, "send", Unix.INVALID_DESCRIPTOR_MSG);
                return Value.UNIT;
            }
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            context.leaveBlockingSection();
            Unix.fail(ctxt, "send", ioe);
            return Value.UNIT;
        }
    }

    /**
     * Receives a packet on a datagram socket.
     * @param ctxt context
     * @param socket socket
     * @param buff buffer of data to receive
     * @param ofs offset of data to receive
     * @param len maximum length of data to receive
     * @param flags ignored
     * @return <i>(number_of_bytes_received, source_address)</i>
     * @throws Fail.Exception if an i/o error occurs
     */
    @Primitive
    public static Value unix_recvfrom(final CodeRunner ctxt, final Value socket, final Value buff, final Value ofs, final Value len, final Value flags) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        final Channel ch = context.getChannel(socket.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "recvfrom", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final DatagramSocket ds = ch.asDatagramSocket();
        if (ds == null) {
            Unix.fail(ctxt, "recvfrom", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final DatagramPacket packet = new DatagramPacket(buff.asBlock().getBytes(), ofs.asLong(), len.asLong());
        try {
            context.enterBlockingSection();
            ds.receive(packet);
            context.leaveBlockingSection();
            final Block res = Block.createBlock(0, Value.createFromLong(packet.getLength()), Unix.createSockAddr(ctxt, (InetSocketAddress) packet.getSocketAddress()));
            return Value.createFromBlock(res);
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            context.leaveBlockingSection();
            Unix.fail(ctxt, "recvfrom", ioe);
            return Value.UNIT;
        }
    }

    /**
     * Sends a packet on a datagram socket.
     * @param ctxt context
     * @param socket socket
     * @param buff buffer of data to send
     * @param ofs offset of data to send
     * @param len length of data to send
     * @param flags ignored
     * @param dest destination socket
     * @return number of bytes sent
     * @throws Fail.Exception if an i/o error occurs
     */
    @Primitive
    public static Value unix_sendto(final CodeRunner ctxt, final Value socket, final Value buff, final Value ofs, final Value len, final Value flags, final Value dest) throws Fail.Exception, FalseExit {
        final Context context = ctxt.getContext();
        final Block destBlock = dest.asBlock();
        DatagramPacket packet;
        try {
            final SocketAddress addr = new InetSocketAddress(InetAddress.getByAddress(destBlock.get(0).asBlock().getBytes()), destBlock.get(1).asLong());
            packet = new DatagramPacket(buff.asBlock().getBytes(), ofs.asLong(), len.asLong(), addr);
        } catch (final IOException ioe) {
            Unix.fail(ctxt, "sendto", ioe);
            return Value.UNIT;
        }
        final Channel ch = context.getChannel(socket.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "sendto", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final DatagramSocket ds = ch.asDatagramSocket();
        if (ds == null) {
            Unix.fail(ctxt, "sendto", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        context.enterBlockingSection();
        try {
            ds.send(packet);
            context.leaveBlockingSection();
            return len;
        } catch (final InterruptedIOException iioe) {
            final FalseExit fe = FalseExit.createFromContext(context);
            fe.fillInStackTrace();
            throw fe;
        } catch (final IOException ioe) {
            context.leaveBlockingSection();
            Unix.fail(ctxt, "sendto", ioe);
            return Value.UNIT;
        }
    }

    /**
     * Exact synonym of {@link #unix_sendto(CodeRunner, Value, Value, Value, Value, Value, Value)}.
     */
    @Primitive
    public static Value unix_sendto_native(final CodeRunner ctxt, final Value socket, final Value buff, final Value ofs, final Value len, final Value flags, final Value dest) throws Fail.Exception, FalseExit {
        return unix_sendto(ctxt, socket, buff, ofs, len, flags, dest);
    }
}
