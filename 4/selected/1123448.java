package com.clanwts.bncs.server.session;

import java.util.HashMap;
import java.util.logging.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import com.clanwts.bnet.ProtocolType;

@ChannelPipelineCoverage("one")
public class ServerMessageDecoder extends SimpleChannelHandler {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(ServerMessageDecoder.class.getName());

    private static final HashMap<ProtocolType, Class<? extends FrameDecoder>> protocolDecoderMap = new HashMap<ProtocolType, Class<? extends FrameDecoder>>();

    static {
        protocolDecoderMap.put(ProtocolType.GAME, com.clanwts.bncs.codec.standard.MessageDecoder.class);
        protocolDecoderMap.put(ProtocolType.FTP, com.clanwts.bnftp.protocol.w3.MessageDecoder.class);
    }

    private static final FrameDecoder getDecoderFor(ProtocolType pt) {
        Class<? extends FrameDecoder> cls = protocolDecoderMap.get(pt);
        try {
            return cls.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean readProtocolRequest;

    private boolean installProtocolDecoder;

    private ProtocolType protocolType;

    public ServerMessageDecoder() {
        this.readProtocolRequest = true;
        this.installProtocolDecoder = true;
        this.protocolType = ProtocolType.UNKNOWN;
    }

    public ServerMessageDecoder(ProtocolType pt) {
        this.readProtocolRequest = false;
        this.installProtocolDecoder = true;
        this.protocolType = pt;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (!(msg instanceof ChannelBuffer) || !(readProtocolRequest || installProtocolDecoder)) {
            ctx.sendUpstream(e);
        }
        ChannelBuffer buf = (ChannelBuffer) msg;
        if (readProtocolRequest) {
            if (buf.readableBytes() == 0) {
                ctx.sendUpstream(e);
            }
            this.protocolType = ProtocolType.forCode(buf.readByte());
            this.readProtocolRequest = false;
            Channels.fireMessageReceived(ctx, ctx.getChannel(), this.protocolType);
        }
        if (installProtocolDecoder) {
            FrameDecoder decoder = getDecoderFor(this.protocolType);
            if (decoder == null) {
                throw new RuntimeException("No decoder available for protocol type '" + this.protocolType.getDescription() + "'.");
            }
            ctx.getPipeline().addAfter(ctx.getName(), ctx.getName() + "-subdecoder", decoder);
            this.installProtocolDecoder = false;
            Channels.fireMessageReceived(ctx, ctx.getChannel(), buf);
        }
    }
}
