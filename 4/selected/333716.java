package ca.qc.adinfo.rouge.server.core.bencode;

import java.io.ByteArrayInputStream;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import ca.qc.adinfo.rouge.command.RougeCommandProcessor;
import ca.qc.adinfo.rouge.data.RougeObject;
import ca.qc.adinfo.rouge.server.core.ServerHandler;
import ca.qc.adinfo.rouge.server.core.SessionContext;
import ca.qc.adinfo.rouge.server.core.SessionManager;

public class BEncodeChannelHandler extends ServerHandler {

    private static Logger log = Logger.getLogger(BEncodeChannelHandler.class);

    public BEncodeChannelHandler(RougeCommandProcessor commandProcessor, SessionManager sessionManager) {
        super(commandProcessor, sessionManager);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        log.debug("Channel connected from " + e.getChannel().getRemoteAddress() + " is channel " + e.getChannel().getId());
        SessionContext session = new SessionContext(e.getChannel(), new BEncodeChannelWriter(e.getChannel()));
        this.onChannelConnected(e.getChannel(), session);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        log.debug("Channel disconnected from " + e.getChannel().getRemoteAddress() + " is channel " + e.getChannel().getId());
        this.onChannelDisconnected(e.getChannel());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        log.debug("Received message from " + e.getChannel().getId());
        try {
            ChannelBuffer channelBuffer = (ChannelBuffer) e.getMessage();
            byte[] bytes = channelBuffer.array();
            ByteArrayInputStream in = new ByteArrayInputStream(bytes);
            while (in.available() > 1) {
                RougeObject resp = BDecoder.bDecode(in);
                if (resp.hasKey("command") | resp.hasKey("payload")) {
                    String command = resp.getString("command");
                    RougeObject payload = resp.getRougeObject("payload");
                    this.onMessageReceived(e.getChannel(), command, payload);
                } else {
                    log.error("Missing command or payload in " + new String(bytes));
                }
            }
        } catch (Exception ex) {
            log.error("Error processing message " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
        Channel ch = e.getChannel();
        ch.close();
    }
}
