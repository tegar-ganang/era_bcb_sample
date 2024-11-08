package com.flazr.rtmp;

import com.flazr.rtmp.RtmpDecoder.DecoderState;
import com.flazr.rtmp.message.ChunkSize;
import com.flazr.rtmp.message.MessageType;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtmpDecoder extends ReplayingDecoder<DecoderState> {

    private static final Logger logger = LoggerFactory.getLogger(RtmpDecoder.class);

    public static enum DecoderState {

        GET_HEADER, GET_PAYLOAD
    }

    public RtmpDecoder() {
        super(DecoderState.GET_HEADER);
    }

    private RtmpHeader header;

    private int channelId;

    private ChannelBuffer payload;

    private int chunkSize = 128;

    private final RtmpHeader[] incompleteHeaders = new RtmpHeader[RtmpHeader.MAX_CHANNEL_ID];

    private final ChannelBuffer[] incompletePayloads = new ChannelBuffer[RtmpHeader.MAX_CHANNEL_ID];

    private final RtmpHeader[] completedHeaders = new RtmpHeader[RtmpHeader.MAX_CHANNEL_ID];

    @Override
    protected Object decode(final ChannelHandlerContext ctx, final Channel channel, final ChannelBuffer in, final DecoderState state) {
        switch(state) {
            case GET_HEADER:
                header = new RtmpHeader(in, incompleteHeaders);
                channelId = header.getChannelId();
                if (incompletePayloads[channelId] == null) {
                    incompleteHeaders[channelId] = header;
                    incompletePayloads[channelId] = ChannelBuffers.buffer(header.getSize());
                }
                payload = incompletePayloads[channelId];
                checkpoint(DecoderState.GET_PAYLOAD);
            case GET_PAYLOAD:
                final byte[] bytes = new byte[Math.min(payload.writableBytes(), chunkSize)];
                in.readBytes(bytes);
                payload.writeBytes(bytes);
                checkpoint(DecoderState.GET_HEADER);
                if (payload.writable()) {
                    return null;
                }
                incompletePayloads[channelId] = null;
                final RtmpHeader prevHeader = completedHeaders[channelId];
                if (!header.isLarge()) {
                    header.setTime(prevHeader.getTime() + header.getDeltaTime());
                }
                final RtmpMessage message = MessageType.decode(header, payload);
                if (logger.isDebugEnabled()) {
                    logger.debug("<< {}", message);
                }
                payload = null;
                if (header.isChunkSize()) {
                    final ChunkSize csMessage = (ChunkSize) message;
                    logger.debug("decoder new chunk size: {}", csMessage);
                    chunkSize = csMessage.getChunkSize();
                }
                completedHeaders[channelId] = header;
                return message;
            default:
                throw new RuntimeException("unexpected decoder state: " + state);
        }
    }
}
