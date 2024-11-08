package com.clanwts.bncs.server.session;

import java.util.HashMap;
import java.util.Map;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import com.clanwts.bncs.codec.UnknownMessage;
import com.clanwts.bncs.codec.standard.messages.AccountLogonClient;
import com.clanwts.bncs.codec.standard.messages.AccountLogonProofClient;
import com.clanwts.bncs.codec.standard.messages.AuthCheckClient;
import com.clanwts.bncs.codec.standard.messages.AuthInfoClient;
import com.clanwts.bncs.codec.standard.messages.ChatCommand;
import com.clanwts.bncs.codec.standard.messages.EnterChatClient;
import com.clanwts.bncs.codec.standard.messages.FriendsListClient;
import com.clanwts.bncs.codec.standard.messages.FriendsUpdateClient;
import com.clanwts.bncs.codec.standard.messages.GetAdvListExClient;
import com.clanwts.bncs.codec.standard.messages.GetFileTimeClient;
import com.clanwts.bncs.codec.standard.messages.GetIconDataClient;
import com.clanwts.bncs.codec.standard.messages.JoinChannel;
import com.clanwts.bncs.codec.standard.messages.NetGamePort;
import com.clanwts.bncs.codec.standard.messages.NotifyJoin;
import com.clanwts.bncs.codec.standard.messages.Null;
import com.clanwts.bncs.codec.standard.messages.Ping;
import com.clanwts.bncs.codec.standard.messages.StartAdvEx3Client;
import com.clanwts.bncs.codec.standard.messages.StopAdv;
import com.clanwts.bncs.codec.standard.messages.WarcraftGeneralClient;
import com.clanwts.bnet.ProtocolType;
import com.clanwts.bnftp.protocol.w3.messages.ClientInfo;
import com.clanwts.bnftp.protocol.w3.messages.FileRequest;
import edu.cmu.ece.agora.codecs.Message;

@ChannelPipelineCoverage("one")
class ChannelHandler extends SimpleChannelHandler {

    private final Map<Class<? extends Message>, MessageHandler> messageHandlers = new HashMap<Class<? extends Message>, MessageHandler>();

    private final SessionManager sessionManager;

    private ProtocolType protocol = null;

    private SessionContext context = null;

    private SessionHandler handler = null;

    ChannelHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        registerGameProtocolHandlers();
        registerFTPHandlers();
    }

    private void registerGameProtocolHandlers() {
        messageHandlers.put(AuthInfoClient.class, new AuthInfoClientHandler());
        messageHandlers.put(AuthCheckClient.class, new AuthCheckClientHandler());
        messageHandlers.put(AccountLogonClient.class, new AccountLogonClientHandler());
        messageHandlers.put(AccountLogonProofClient.class, new AccountLogonProofClientHandler());
        messageHandlers.put(GetAdvListExClient.class, new GetAdvListExClientHandler());
        messageHandlers.put(GetFileTimeClient.class, new GetFileTimeClientHandler());
        messageHandlers.put(GetIconDataClient.class, new GetIconDataClientHandler());
        messageHandlers.put(NetGamePort.class, new NetGamePortHandler());
        messageHandlers.put(EnterChatClient.class, new EnterChatClientHandler());
        messageHandlers.put(JoinChannel.class, new JoinChannelHandler());
        messageHandlers.put(ChatCommand.class, new ChatCommandHandler());
        messageHandlers.put(Ping.class, new PingHandler());
        messageHandlers.put(StartAdvEx3Client.class, new StartAdvEx3ClientHandler());
        messageHandlers.put(StopAdv.class, new StopAdvHandler());
        messageHandlers.put(WarcraftGeneralClient.class, new WarcraftGeneralClientHandler());
        messageHandlers.put(NotifyJoin.class, new NotifyJoinHandler());
        messageHandlers.put(Null.class, new NullHandler());
        messageHandlers.put(FriendsListClient.class, new FriendsListClientHandler());
        messageHandlers.put(FriendsUpdateClient.class, new FriendsUpdateClientHandler());
        messageHandlers.put(UnknownMessage.class, new UnknownMessageHandler());
    }

    private void registerFTPHandlers() {
        messageHandlers.put(ClientInfo.class, new ClientInfoHandler());
        messageHandlers.put(FileRequest.class, new FileRequestHandler());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        SessionManager.logger.info("Connection from " + ctx.getChannel().getRemoteAddress().toString() + " opened.");
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        SessionManager.logger.info("Connection from " + ctx.getChannel().getRemoteAddress().toString() + " closed.");
        if (this.context != null && this.handler != null) {
            this.handler.sessionEnded(this.context);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        SessionManager.logger.warning("Caught exception on connection to " + ctx.getChannel().getRemoteAddress().toString() + " " + e.getCause().toString() + ".");
        for (StackTraceElement ste : e.getCause().getStackTrace()) SessionManager.logger.warning("\tAt " + ste.toString());
        ctx.getChannel().close();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        SessionManager.logger.info("Received message of type " + msg.getClass().getName() + " from " + ctx.getChannel().getRemoteAddress().toString() + ".");
        if (this.protocol == null) {
            if (!(msg instanceof ProtocolType)) throw new IllegalStateException("Received message but protocol not yet specified.");
            this.protocol = (ProtocolType) msg;
            if (!this.sessionManager.sessionHandlers.containsKey(this.protocol)) throw new IllegalStateException("Protocol has no handler defined.");
            this.context = new SessionContext(ctx.getChannel());
            this.handler = this.sessionManager.getSessionHandler(this.protocol);
            this.handler.sessionStarted(this.context);
            return;
        }
        MessageHandler msgHandler = messageHandlers.get(msg.getClass());
        if (msgHandler == null) throw new IllegalArgumentException("No handler is registered for this message type.");
        msgHandler.handleMessage((Message) msg);
    }

    private static interface MessageHandler {

        public void handleMessage(Message msg) throws Exception;
    }

    /*******************************
  | BEGIN GAME PROTOCOL HANDLERS |
  *******************************/
    private class WarcraftGeneralClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            WarcraftGeneralClient m = (WarcraftGeneralClient) msg;
            handler.handleWarcraftGeneralClient(ChannelHandler.this.context, m);
        }
    }

    private class AuthInfoClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            AuthInfoClient m = (AuthInfoClient) msg;
            handler.handleAuthInfoClient(ChannelHandler.this.context, m);
        }
    }

    private class AuthCheckClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            AuthCheckClient m = (AuthCheckClient) msg;
            handler.handleAuthCheckClient(ChannelHandler.this.context, m);
        }
    }

    private class AccountLogonClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            AccountLogonClient m = (AccountLogonClient) msg;
            handler.handleAccountLogonClient(ChannelHandler.this.context, m);
        }
    }

    private class AccountLogonProofClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            AccountLogonProofClient m = (AccountLogonProofClient) msg;
            handler.handleAccountLogonProofClient(ChannelHandler.this.context, m);
        }
    }

    private class GetAdvListExClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            GetAdvListExClient m = (GetAdvListExClient) msg;
            handler.handleGetAdvListExClient(ChannelHandler.this.context, m);
        }
    }

    private class GetFileTimeClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            GetFileTimeClient m = (GetFileTimeClient) msg;
            handler.handleGetFileTimeClient(ChannelHandler.this.context, m);
        }
    }

    private class GetIconDataClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            GetIconDataClient m = (GetIconDataClient) msg;
            handler.handleGetIconDataClient(ChannelHandler.this.context, m);
        }
    }

    private class NetGamePortHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            NetGamePort m = (NetGamePort) msg;
            handler.handleNetGamePort(ChannelHandler.this.context, m);
        }
    }

    private class EnterChatClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            EnterChatClient m = (EnterChatClient) msg;
            handler.handleEnterChatClient(ChannelHandler.this.context, m);
        }
    }

    private class JoinChannelHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            JoinChannel m = (JoinChannel) msg;
            handler.handleJoinChannel(ChannelHandler.this.context, m);
        }
    }

    private class ChatCommandHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            ChatCommand m = (ChatCommand) msg;
            handler.handleChatCommand(ChannelHandler.this.context, m);
        }
    }

    private class PingHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            Ping m = (Ping) msg;
            handler.handlePing(ChannelHandler.this.context, m);
        }
    }

    private class StartAdvEx3ClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            StartAdvEx3Client m = (StartAdvEx3Client) msg;
            handler.handleStartAdvEx3Client(ChannelHandler.this.context, m);
        }
    }

    private class StopAdvHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            StopAdv m = (StopAdv) msg;
            handler.handleStopAdv(ChannelHandler.this.context, m);
        }
    }

    private class NotifyJoinHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            NotifyJoin m = (NotifyJoin) msg;
            handler.handleNotifyJoin(ChannelHandler.this.context, m);
        }
    }

    private class NullHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            Null m = (Null) msg;
            handler.handleNull(ChannelHandler.this.context, m);
        }
    }

    private class FriendsListClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            FriendsListClient m = (FriendsListClient) msg;
            handler.handleFriendsListClient(ChannelHandler.this.context, m);
        }
    }

    private class FriendsUpdateClientHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            FriendsUpdateClient m = (FriendsUpdateClient) msg;
            handler.handleFriendsUpdateClient(ChannelHandler.this.context, m);
        }
    }

    private class UnknownMessageHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            GameSessionHandler handler = (GameSessionHandler) ChannelHandler.this.handler;
            UnknownMessage m = (UnknownMessage) msg;
            handler.handleUnknownMessage(ChannelHandler.this.context, m);
        }
    }

    /******************************
  | BEGIN FTP PROTOCOL HANDLERS |
  ******************************/
    private class ClientInfoHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            FTPSessionHandler handler = (FTPSessionHandler) ChannelHandler.this.handler;
            ClientInfo m = (ClientInfo) msg;
            handler.handleClientInfo(ChannelHandler.this.context, m);
        }
    }

    private class FileRequestHandler implements MessageHandler {

        @Override
        public void handleMessage(Message msg) throws Exception {
            FTPSessionHandler handler = (FTPSessionHandler) ChannelHandler.this.handler;
            FileRequest m = (FileRequest) msg;
            handler.handleFileRequest(ChannelHandler.this.context, m);
        }
    }
}
