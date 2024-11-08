package ca.qc.adinfo.rouge.server.core.json;

import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import ca.qc.adinfo.rouge.command.RougeCommandProcessor;
import ca.qc.adinfo.rouge.data.RougeObject;
import ca.qc.adinfo.rouge.server.core.ServerHandler;
import ca.qc.adinfo.rouge.server.core.SessionContext;
import ca.qc.adinfo.rouge.server.core.SessionManager;

public class JsonChannelHandler extends ServerHandler {

    private static Logger log = Logger.getLogger(JsonChannelHandler.class);

    private String msgBuffer;

    public JsonChannelHandler(RougeCommandProcessor commandProcessor, SessionManager sessionManager) {
        super(commandProcessor, sessionManager);
        this.msgBuffer = "";
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        log.trace("Channel connected from " + e.getChannel().getRemoteAddress() + " is channel " + e.getChannel().getId());
        SessionContext session = new SessionContext(e.getChannel(), new JsonChannelWriter(e.getChannel()));
        this.onChannelConnected(e.getChannel(), session);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        log.trace("Channel disconnected from " + e.getChannel().getRemoteAddress() + " is channel " + e.getChannel().getId());
        this.onChannelDisconnected(e.getChannel());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        log.trace("Received message from " + e.getChannel().getId());
        try {
            this.msgBuffer += e.getMessage();
            int seperatorPos = this.msgBuffer.indexOf("|");
            if (seperatorPos != -1) {
                String message = this.msgBuffer.substring(0, seperatorPos);
                if (this.msgBuffer.length() == seperatorPos + 1) {
                    this.msgBuffer = "";
                } else {
                    this.msgBuffer = this.msgBuffer.substring(seperatorPos + 1);
                }
                JSONObject jsonObject = JSONObject.fromObject(message);
                String command = jsonObject.getString("command");
                RougeObject payload = new RougeObject(jsonObject.getJSONObject("payload"));
                this.onMessageReceived(e.getChannel(), command, payload);
            }
        } catch (Exception ex) {
            log.error("Error processing message " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        log.error("Caught exception " + e.getCause().getMessage());
        this.onExceptionCaught(e.getChannel(), e.getCause());
    }
}
