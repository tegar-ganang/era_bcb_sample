package ru.opedge.notifical.bot.xmpp;

import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import ru.opedge.notifical.bot.BotException;
import ru.opedge.notifical.bot.INotificationBot;
import ru.opedge.notifical.notification.service.INotificationService;

/**
 * @author opedge
 * 
 */
public class XMPPNotificationBot implements INotificationBot {

    private final XMPPConnection connection;

    private final int delay;

    private final INotificationService service;

    private final String resource;

    /**
	 * @return the connection
	 */
    public XMPPConnection getConnection() {
        return connection;
    }

    public XMPPNotificationBot(final String host, final int port, final String server, final String resource, final INotificationService service, final int delay) {
        final ConnectionConfiguration configuration = new ConnectionConfiguration(host, port, server);
        connection = new XMPPConnection(configuration);
        this.service = service;
        this.delay = delay;
        this.resource = resource;
    }

    @Override
    public void connect(final String username, final String password) throws BotException {
        try {
            connection.connect();
            connection.login(username, password, resource);
        } catch (final XMPPException e) {
            throw new BotException("Bot connection to service fail " + e.getMessage());
        } catch (final NullPointerException e) {
            throw new BotException("Jid and password should not be empty.");
        }
        final ChatManager chatManager = connection.getChatManager();
        chatManager.addChatListener(new NotificationBotChatListener(service, delay));
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public void disconnect() {
        connection.disconnect();
    }
}
