package pl.java.ircbot.bot;

import java.io.IOException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.springframework.beans.factory.InitializingBean;
import pl.java.ircbot.KeepAliveThread;
import pl.java.ircbot.irc.IrcChannel;
import pl.java.ircbot.irc.IrcServer;
import pl.java.ircbot.irc.handler.join.UserJoinHandler;
import pl.java.ircbot.irc.handler.message.MessageHandler;

/**
 * 
 * @author activey
 */
public class IrcBot extends PircBot implements InitializingBean {

    private static Log logger = LogFactory.getLog(IrcBot.class);

    /**
     * Lista serwerow, do ktorych bot bedzie sie laczyl
     */
    private List<IrcServer> ircServers;

    /**
     * Lista odbiornikow wiadomosci
     */
    private List<MessageHandler> messageHandlers;

    private List<UserJoinHandler> joinHandlers;

    private boolean executed;

    private boolean connected;

    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        for (MessageHandler messageHandler : messageHandlers) {
            if (messageHandler == null) continue;
            logger.debug("message handler: " + messageHandler.getClass());
            messageHandler.handleMessage(this, channel, sender, login, hostname, message);
        }
    }

    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        onMessage(sender, sender, login, hostname, message);
    }

    protected void onJoin(String channel, String sender, String login, String hostname) {
        if (sender.equalsIgnoreCase(getNick())) return;
        for (UserJoinHandler joinHandler : joinHandlers) {
            if (joinHandler == null) continue;
            logger.debug("user join handler: " + joinHandler.getClass());
            IrcServer joinedServer = getJoinedServerDefinition();
            if (joinedServer == null) return;
            IrcChannel channelDefinition = joinedServer.getChannelByName(channel);
            if (channelDefinition == null) return;
            joinHandler.handlerUserJoin(this, channelDefinition, sender, login, hostname);
        }
    }

    protected void onConnect() {
        this.connected = true;
    }

    protected void onDisconnect() {
        this.connected = false;
    }

    /**
     * 
     * @return
     */
    public IrcServer getJoinedServerDefinition() {
        String joinedServer = getServer();
        for (IrcServer ircServer : ircServers) {
            if (ircServer == null) continue;
            if (joinedServer.equalsIgnoreCase(ircServer.getHost())) return ircServer;
        }
        return null;
    }

    public void afterPropertiesSet() throws Exception {
        logger.debug("IRC Bot initialization done ...");
        logger.debug("IRC Bot nick name = " + getName());
        dumpAddedServers();
        initializeConnection();
        executeHeartBeatThread();
    }

    /**
     * Metoda dolacza bota do zdefiniowanych kanalow na serwerze ircowym, ktory
     * zostal zdefiniowany w pliku konfiguracyjnym.
     */
    private void performJoinChannels(IrcServer ircServer) {
        if (ircServer == null) return;
        logger.debug("performing channels join ...");
        for (IrcChannel channel : ircServer.getChannels()) {
            logger.debug("joining channel: " + channel.getName());
            joinChannel(channel.getName());
        }
    }

    /**
     * Metoda uruchamia polaczenie z jednym z dostepnych serwerow.
     */
    private void initializeConnection() {
        for (IrcServer ircServer : ircServers) {
            if (connected) break;
            if (ircServer == null) continue;
            try {
                logger.info("Trying to connect with server: " + ircServer.getHost() + ":" + ircServer.getPort());
                connect(ircServer.getHost(), ircServer.getPort());
                logger.debug("Connection established.");
                performJoinChannels(ircServer);
            } catch (NickAlreadyInUseException e) {
                logger.error("Choosed nick name is already reserved", e);
            } catch (IOException e) {
                logger.error("IO Error, connection failed, trying other IRC server defined ...");
            } catch (IrcException e) {
                logger.error("General IRC server connection error occurred", e);
            }
        }
    }

    /**
     * Metoda wypisuje liste dodanych serwerow ircowych.
     */
    private void dumpAddedServers() {
        for (IrcServer ircServer : ircServers) {
            if (ircServer == null) continue;
            logger.debug("-----------------------");
            ircServer.dumpServerInfo();
        }
    }

    /**
     * TODO
     * 
     * @return Returns the ircServers.
     */
    public List<IrcServer> getIrcServers() {
        return ircServers;
    }

    /**
     * TODO
     * 
     * @param ircServers
     *            The ircServers to set.
     */
    public void setIrcServers(List<IrcServer> ircServers) {
        this.ircServers = ircServers;
    }

    /**
     * TODO
     * 
     * @return Returns the messageHandlers.
     */
    public List<MessageHandler> getMessageHandlers() {
        return messageHandlers;
    }

    /**
     * TODO
     * 
     * @param messageHandlers
     *            The messageHandlers to set.
     */
    public void setMessageHandlers(List<MessageHandler> messageHandlers) {
        this.messageHandlers = messageHandlers;
    }

    public void setBotNickName(String nickName) {
        setName(nickName);
    }

    /**
     * TODO
     * 
     * @return Returns the joinHandlers.
     */
    public List<UserJoinHandler> getJoinHandlers() {
        return joinHandlers;
    }

    /**
     * TODO
     * 
     * @param joinHandlers
     *            The joinHandlers to set.
     */
    public void setJoinHandlers(List<UserJoinHandler> joinHandlers) {
        this.joinHandlers = joinHandlers;
    }

    /**
     * Metoda uruchamia watek, ktory chodzi w kolko i utrzymuje aplikacje.
     */
    private void executeHeartBeatThread() {
        if (executed) return;
        KeepAliveThread keepAliveThread = new KeepAliveThread();
        keepAliveThread.start();
        executed = true;
    }
}
