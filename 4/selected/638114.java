package sk.yw.azetclient.azet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import sk.yw.azetclient.HttpURLHandler;
import sk.yw.azetclient.Main;
import sk.yw.azetclient.connection.AbstractSiteConnection;
import sk.yw.azetclient.connection.ConnectionException;
import sk.yw.azetclient.connection.Parser;
import sk.yw.azetclient.model.Buddy;
import sk.yw.azetclient.model.Message;
import sk.yw.azetclient.model.MessageBean;

/**
 *
 * @author error216
 */
public class AzetConnection extends AbstractSiteConnection {

    private static final Logger logger = Logger.getLogger(AzetConnection.class);

    private boolean authenticated = false;

    private Buddy user;

    private String i9 = "";

    private int roomNo = 1;

    private Parser<String> authenticationHandler = new AuthenticationParser();

    private Parser<List<Buddy>> buddyListHandler = new BuddyListParser();

    private Parser<List<AzetMessageBean>> messagesHandler = new MessagesParser();

    private Parser<Boolean> messageSender = new MessageSender();

    private AzetMessageSplitterAndCondenser splitterCondenser = new AzetMessageSplitterAndCondenser();

    private String getProperty(String key) {
        String property = Main.getProperty("azetclient.azet." + key);
        if (property != null) property = property.replace("$I9$", i9).replace("$ROOM_NO$", String.valueOf(roomNo));
        return property;
    }

    public AzetConnection() {
        HttpURLHandler.setProxy(Main.getProxy());
    }

    @Override
    public boolean authenticate(Buddy user, String password) throws ConnectionException {
        if (user == null) throw new IllegalArgumentException("Null pointer in user");
        if (user.getName() == null) throw new IllegalArgumentException("Null pointer in user name");
        if (password == null) throw new IllegalArgumentException("Null pointer in password");
        this.user = user;
        ((MessagesParser) messagesHandler).setUser(user);
        URL url;
        try {
            url = new URL(getProperty("authentication.action"));
        } catch (NullPointerException ex) {
            throw new ConnectionException("No azet authentication action url was found.", ex);
        } catch (MalformedURLException ex) {
            throw new ConnectionException("Azet authentication action url is malformed", ex);
        }
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(getProperty("authentication.username"), user.getName());
        parameters.put(getProperty("authentication.password"), password);
        for (int i = 0; getProperty("authentication.hidden" + i + ".name") != null; i++) {
            parameters.put(getProperty("authentication.hidden" + i + ".name"), getProperty("authentication.hidden" + i + ".value"));
        }
        try {
            HttpURLHandler urlHandler = new HttpURLHandler(url, getProperty("authentication.method"), parameters, getProperty("encoding"));
            i9 = authenticationHandler.parse(urlHandler.getInputStream());
            authenticated = !i9.isEmpty();
            ((MessagesParser) messagesHandler).setMessageUrl(getProperty("messages.oneUrl"));
            ((MessagesParser) messagesHandler).setAdvancedReceiving(true);
        } catch (IOException ex) {
            throw new ConnectionException("Unable to read from/write to azet authenticaton action url", ex);
        }
        return authenticated;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void disconnect() throws ConnectionException {
        if (!authenticated) throw new IllegalStateException("Must be authenticated to disconnect.");
        URL url;
        try {
            url = new URL(getProperty("disconnect.url"));
        } catch (NullPointerException ex) {
            throw new ConnectionException("No azet disconnect url was found.", ex);
        } catch (MalformedURLException ex) {
            throw new ConnectionException("Azet disconnect url is malformed.", ex);
        }
        try {
            HttpURLHandler urlHandler = new HttpURLHandler(url);
            urlHandler.getInputStream();
        } catch (IOException ex) {
            throw new ConnectionException("Unable to read from/write to azet disconnect url", ex);
        }
    }

    @Override
    public List<Buddy> getBuddies() throws ConnectionException {
        if (!authenticated) throw new IllegalStateException("Must be authenticated to obtain buddy list.");
        URL url;
        try {
            url = new URL(getProperty("buddyList.url"));
        } catch (NullPointerException ex) {
            throw new ConnectionException("No azet buddy list url was found.", ex);
        } catch (MalformedURLException ex) {
            throw new ConnectionException("Azet buddy list url is malformed", ex);
        }
        try {
            HttpURLHandler urlHandler = new HttpURLHandler(url);
            return buddyListHandler.parse(urlHandler.getInputStream());
        } catch (IOException ex) {
            throw new ConnectionException("Unable to read from/write to azet buddy list url", ex);
        }
    }

    @Override
    public List<MessageBean> getMessages() throws ConnectionException {
        if (!authenticated) throw new IllegalStateException("Must be authenticated to receive messages.");
        URL url;
        try {
            url = new URL(getProperty("messages.allUrl"));
        } catch (NullPointerException ex) {
            throw new ConnectionException("No azet getMessages url was found.", ex);
        } catch (MalformedURLException ex) {
            throw new ConnectionException("Azet getMessages url is malformed", ex);
        }
        try {
            HttpURLHandler urlHandler = new HttpURLHandler(url);
            return new ArrayList<MessageBean>(splitterCondenser.condense(messagesHandler.parse(urlHandler.getInputStream())));
        } catch (IOException ex) {
            throw new ConnectionException("Unable to read from/write to azet getMessages url", ex);
        }
    }

    @Override
    public boolean sendMessage(Message message) throws ConnectionException {
        if (!authenticated) throw new IllegalStateException("Must be authenticated to send a message.");
        if (message == null) throw new IllegalArgumentException("Null pointer in message");
        if (!(message instanceof AzetMessage)) throw new IllegalArgumentException("Illegal type of message: " + message.getClass());
        AzetMessage aMessage = (AzetMessage) message;
        if (aMessage.getReceiver() == null) throw new IllegalArgumentException("Null pointer in message receiver");
        if (aMessage.getReceiver().getName() == null) throw new IllegalArgumentException("Null pointer in message receiver name");
        if (aMessage.getContent() == null) throw new IllegalArgumentException("Null pointer in message content");
        if (aMessage.getContent().isEmpty()) throw new IllegalArgumentException("Message content is empty");
        if (!user.equals(aMessage.getSender())) throw new IllegalArgumentException("Message sender does not corespond to the authenticated user.");
        URL url;
        try {
            url = new URL(getProperty("messages.send.action"));
        } catch (NullPointerException ex) {
            throw new ConnectionException("No azet sendMessage url was found.", ex);
        } catch (MalformedURLException ex) {
            throw new ConnectionException("Azet sendMessage url is malformed", ex);
        }
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(getProperty("messages.send.receiver.id"), String.valueOf(aMessage.getReceiver().getId()));
        parameters.put(getProperty("messages.send.receiver.name"), aMessage.getReceiver().getName());
        if (aMessage.getSign() != null) {
            parameters.put(getProperty("messages.send.sign"), aMessage.getSign());
        }
        if (aMessage.getTimeOfPrevious() != null) {
            parameters.put(getProperty("messages.send.timeOfPrevious"), MessagesParser.ADVANCED_SEND_TIME_FORMAT.format(aMessage.getTimeOfPrevious()));
        }
        if (aMessage.getTextOfPrevious() != null) {
            parameters.put(getProperty("messages.send.textOfPrevious"), aMessage.getTextOfPrevious());
        }
        for (int i = 0; getProperty("messages.send.hidden" + i + ".name") != null; i++) {
            parameters.put(getProperty("messages.send.hidden" + i + ".name"), getProperty("messages.send.hidden" + i + ".value"));
        }
        List<String> contents = splitterCondenser.split(aMessage);
        aMessage.setLastFragment(contents.get(contents.size() - 1));
        for (String content : contents) {
            parameters.put(getProperty("messages.send.content"), content);
            try {
                HttpURLHandler urlHandler = new HttpURLHandler(url, getProperty("messages.send.method"), parameters, getProperty("encoding"));
                if (!messageSender.parse(urlHandler.getInputStream())) {
                    return false;
                }
            } catch (IOException ex) {
                throw new ConnectionException("Unable to read from/write to azet sendMessage url", ex);
            }
        }
        return true;
    }

    @Override
    public Buddy getUser() {
        return user;
    }
}
