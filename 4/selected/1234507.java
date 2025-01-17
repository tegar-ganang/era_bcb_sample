package com.outbrain.pajamasproxy.memcached.server.protocol.binary;

import java.nio.ByteOrder;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.outbrain.pajamasproxy.memcached.adapter.CacheElement;
import com.outbrain.pajamasproxy.memcached.server.protocol.exceptions.UnknownCommandException;
import com.outbrain.pajamasproxy.memcached.server.protocol.value.Op;
import com.outbrain.pajamasproxy.memcached.server.protocol.value.ResponseMessage;

@ChannelHandler.Sharable
public class MemcachedBinaryResponseEncoder extends SimpleChannelUpstreamHandler {

    private final ConcurrentHashMap<Integer, ChannelBuffer> corkedBuffers = new ConcurrentHashMap<Integer, ChannelBuffer>();

    final Logger logger = LoggerFactory.getLogger(MemcachedBinaryResponseEncoder.class);

    public static enum ResponseCode {

        OK(0x0000), KEYNF(0x0001), KEYEXISTS(0x0002), TOOLARGE(0x0003), INVARG(0x0004), NOT_STORED(0x0005), UNKNOWN(0x0081), OOM(0x00082);

        public short code;

        ResponseCode(final int code) {
            this.code = (short) code;
        }
    }

    public ResponseCode getStatusCode(final ResponseMessage command) {
        final Op cmd = command.cmd.op;
        if (cmd == Op.GET || cmd == Op.GETS) {
            return ResponseCode.OK;
        } else if (cmd == Op.SET || cmd == Op.CAS || cmd == Op.ADD || cmd == Op.REPLACE || cmd == Op.APPEND || cmd == Op.PREPEND) {
            switch(command.response) {
                case EXISTS:
                    return ResponseCode.KEYEXISTS;
                case NOT_FOUND:
                    return ResponseCode.KEYNF;
                case NOT_STORED:
                    return ResponseCode.NOT_STORED;
                case STORED:
                    return ResponseCode.OK;
            }
        } else if (cmd == Op.INCR || cmd == Op.DECR) {
            return command.incrDecrResponse == null ? ResponseCode.KEYNF : ResponseCode.OK;
        } else if (cmd == Op.DELETE) {
            switch(command.deleteResponse) {
                case DELETED:
                    return ResponseCode.OK;
                case NOT_FOUND:
                    return ResponseCode.KEYNF;
            }
        } else if (cmd == Op.STATS) {
            return ResponseCode.OK;
        } else if (cmd == Op.VERSION) {
            return ResponseCode.OK;
        } else if (cmd == Op.FLUSH_ALL) {
            return ResponseCode.OK;
        }
        return ResponseCode.UNKNOWN;
    }

    public ChannelBuffer constructHeader(final MemcachedBinaryCommandDecoder.BinaryOp bcmd, final ChannelBuffer extrasBuffer, final ChannelBuffer keyBuffer, final ChannelBuffer valueBuffer, final short responseCode, final int opaqueValue, final long casUnique) {
        final ChannelBuffer header = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, 24);
        header.writeByte((byte) 0x81);
        header.writeByte(bcmd.code);
        final short keyLength = (short) (keyBuffer != null ? keyBuffer.capacity() : 0);
        header.writeShort(keyLength);
        final int extrasLength = extrasBuffer != null ? extrasBuffer.capacity() : 0;
        header.writeByte((byte) extrasLength);
        header.writeByte((byte) 0);
        header.writeShort(responseCode);
        final int dataLength = valueBuffer != null ? valueBuffer.capacity() : 0;
        header.writeInt(dataLength + keyLength + extrasLength);
        header.writeInt(opaqueValue);
        header.writeLong(casUnique);
        return header;
    }

    /**
   * Handle exceptions in protocol processing. Exceptions are either client or internal errors.  Report accordingly.
   *
   * @param ctx
   * @param e
   * @throws Exception
   */
    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) throws Exception {
        try {
            throw e.getCause();
        } catch (final UnknownCommandException unknownCommand) {
            if (ctx.getChannel().isOpen()) {
                ctx.getChannel().write(constructHeader(MemcachedBinaryCommandDecoder.BinaryOp.Noop, null, null, null, (short) 0x0081, 0, 0));
            }
        } catch (final Throwable err) {
            logger.error("error", err);
            if (ctx.getChannel().isOpen()) {
                ctx.getChannel().close();
            }
        }
    }

    @Override
    public void messageReceived(final ChannelHandlerContext channelHandlerContext, final MessageEvent messageEvent) throws Exception {
        final ResponseMessage command = (ResponseMessage) messageEvent.getMessage();
        final MemcachedBinaryCommandDecoder.BinaryOp bcmd = MemcachedBinaryCommandDecoder.BinaryOp.forCommandMessage(command.cmd);
        ChannelBuffer extrasBuffer = null;
        ChannelBuffer keyBuffer = null;
        if (bcmd.addKeyToResponse && command.cmd.keys != null && command.cmd.keys.size() != 0) {
            keyBuffer = ChannelBuffers.wrappedBuffer(command.cmd.keys.get(0).bytes);
        }
        ChannelBuffer valueBuffer = null;
        if (command.elements != null) {
            extrasBuffer = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, 4);
            final CacheElement element = command.elements[0];
            extrasBuffer.writeShort((short) (element != null ? element.getExpire() : 0));
            extrasBuffer.writeShort((short) (element != null ? element.getFlags() : 0));
            if ((command.cmd.op == Op.GET || command.cmd.op == Op.GETS)) {
                if (element != null) {
                    valueBuffer = ChannelBuffers.wrappedBuffer(element.getData());
                } else {
                    valueBuffer = ChannelBuffers.buffer(0);
                }
            } else if (command.cmd.op == Op.INCR || command.cmd.op == Op.DECR) {
                valueBuffer = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, 8);
                valueBuffer.writeLong(command.incrDecrResponse);
            }
        } else if (command.cmd.op == Op.INCR || command.cmd.op == Op.DECR) {
            valueBuffer = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, 8);
            valueBuffer.writeLong(command.incrDecrResponse);
        }
        long casUnique = 0;
        if (command.elements != null && command.elements.length != 0 && command.elements[0] != null) {
            casUnique = command.elements[0].getCasUnique();
        }
        if (command.cmd.op == Op.STATS) {
            if (corkedBuffers.containsKey(command.cmd.opaque)) {
                uncork(command.cmd.opaque, messageEvent.getChannel());
            }
            for (final Map.Entry<String, Set<String>> statsEntries : command.stats.entrySet()) {
                for (final String stat : statsEntries.getValue()) {
                    keyBuffer = ChannelBuffers.wrappedBuffer(ByteOrder.BIG_ENDIAN, statsEntries.getKey().getBytes(MemcachedBinaryCommandDecoder.USASCII));
                    valueBuffer = ChannelBuffers.wrappedBuffer(ByteOrder.BIG_ENDIAN, stat.getBytes(MemcachedBinaryCommandDecoder.USASCII));
                    final ChannelBuffer headerBuffer = constructHeader(bcmd, extrasBuffer, keyBuffer, valueBuffer, getStatusCode(command).code, command.cmd.opaque, casUnique);
                    writePayload(messageEvent, extrasBuffer, keyBuffer, valueBuffer, headerBuffer);
                }
            }
            keyBuffer = null;
            valueBuffer = null;
            final ChannelBuffer headerBuffer = constructHeader(bcmd, extrasBuffer, keyBuffer, valueBuffer, getStatusCode(command).code, command.cmd.opaque, casUnique);
            writePayload(messageEvent, extrasBuffer, keyBuffer, valueBuffer, headerBuffer);
        } else {
            final ChannelBuffer headerBuffer = constructHeader(bcmd, extrasBuffer, keyBuffer, valueBuffer, getStatusCode(command).code, command.cmd.opaque, casUnique);
            if (bcmd.noreply) {
                final int totalCapacity = headerBuffer.capacity() + (extrasBuffer != null ? extrasBuffer.capacity() : 0) + (keyBuffer != null ? keyBuffer.capacity() : 0) + (valueBuffer != null ? valueBuffer.capacity() : 0);
                final ChannelBuffer corkedResponse = cork(command.cmd.opaque, totalCapacity);
                corkedResponse.writeBytes(headerBuffer);
                if (extrasBuffer != null) {
                    corkedResponse.writeBytes(extrasBuffer);
                }
                if (keyBuffer != null) {
                    corkedResponse.writeBytes(keyBuffer);
                }
                if (valueBuffer != null) {
                    corkedResponse.writeBytes(valueBuffer);
                }
            } else {
                if (corkedBuffers.containsKey(command.cmd.opaque)) {
                    uncork(command.cmd.opaque, messageEvent.getChannel());
                }
                writePayload(messageEvent, extrasBuffer, keyBuffer, valueBuffer, headerBuffer);
            }
        }
    }

    private ChannelBuffer cork(final int opaque, final int totalCapacity) {
        ChannelBuffer corkedResponse = corkedBuffers.get(opaque);
        if (corkedResponse == null) {
            final ChannelBuffer buffer = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, totalCapacity);
            corkedBuffers.put(opaque, buffer);
            return buffer;
        } else {
            final ChannelBuffer oldBuffer = corkedResponse;
            corkedResponse = ChannelBuffers.buffer(ByteOrder.BIG_ENDIAN, totalCapacity + corkedResponse.capacity());
            corkedResponse.writeBytes(oldBuffer);
            oldBuffer.clear();
            corkedBuffers.remove(opaque);
            corkedBuffers.put(opaque, corkedResponse);
            return corkedResponse;
        }
    }

    private void uncork(final int opaque, final Channel channel) {
        final ChannelBuffer corkedBuffer = corkedBuffers.get(opaque);
        assert corkedBuffer != null;
        channel.write(corkedBuffer);
        corkedBuffers.remove(opaque);
    }

    private void writePayload(final MessageEvent messageEvent, final ChannelBuffer extrasBuffer, final ChannelBuffer keyBuffer, final ChannelBuffer valueBuffer, final ChannelBuffer headerBuffer) {
        if (messageEvent.getChannel().isOpen()) {
            messageEvent.getChannel().write(headerBuffer);
            if (extrasBuffer != null) {
                messageEvent.getChannel().write(extrasBuffer);
            }
            if (keyBuffer != null) {
                messageEvent.getChannel().write(keyBuffer);
            }
            if (valueBuffer != null) {
                messageEvent.getChannel().write(valueBuffer);
            }
        }
    }
}
