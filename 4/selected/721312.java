package ca.qc.adinfo.rouge.bencode;

import java.io.ByteArrayInputStream;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import ca.qc.adinfo.rouge.RougeDriver;
import ca.qc.adinfo.rouge.data.RougeObject;

public class BEncodeChannelHandler extends SimpleChannelUpstreamHandler {

    private static Logger log = Logger.getLogger(BEncodeChannelHandler.class);

    private RougeDriver driver;

    public BEncodeChannelHandler(RougeDriver driver) {
        this.driver = driver;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        log.trace("Channel connected " + e.getChannel().getId());
        if (this.driver.listener != null) {
            driver.listener.onConnect();
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        log.trace("Message received from channel " + e.getChannel().getId());
        ChannelBuffer channelBuffer = (ChannelBuffer) e.getMessage();
        ByteArrayInputStream in = new ByteArrayInputStream(channelBuffer.array());
        while (in.available() > 0) {
            RougeObject resp = BDecoder.bDecode(in);
            String command = resp.getString("command");
            RougeObject payload = resp.getRougeObject("payload");
            this.driver.handle(command, payload);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        log.error("Unexpected exception from downstream.");
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}
