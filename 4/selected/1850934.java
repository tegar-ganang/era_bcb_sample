package com.bambamboo.st.socket.server.netty;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bambamboo.st.IHandler;
import com.bambamboo.st.IProcessor;
import com.bambamboo.st.logging.Markers;
import com.bambamboo.st.util.HexString;

/**
 * DOCME
 * 
 * @author Nick.Tan
 * @since 1.0.0
 */
@ChannelPipelineCoverage("one")
public class DefaultChannelHandler extends SimpleChannelUpstreamHandler implements IHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultChannelHandler.class);

    private IProcessor processor;

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        ChannelBuffer buffer = (ChannelBuffer) event.getMessage();
        logger.info("[PROCESSING] data length: [{}]", buffer.readableBytes());
        logger.debug(Markers.DUMP, "data: \n{}", HexString.dump(buffer));
        IProcessor processor = getProcessor();
        byte[] requestBuf = new byte[buffer.readableBytes()];
        buffer.readBytes(requestBuf);
        byte[] byResp = processor.getResponse(requestBuf);
        if (byResp != null && byResp.length > 0) {
            ChannelBuffer out = ChannelBuffers.buffer(byResp.length);
            out.writeBytes(byResp);
            event.getChannel().write(out);
        }
    }

    /**
     * @return the processor
     */
    public IProcessor getProcessor() {
        return processor;
    }

    /**
     * @param processor the processor to set
     */
    public void setProcessor(IProcessor processor) {
        this.processor = processor;
    }

    @Override
    public String toString() {
        return "Default IoHandler with processor: " + getProcessor();
    }
}
