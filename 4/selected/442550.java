package erki.abcpeter.bots;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.LinkedList;
import erki.abcpeter.Bot;
import erki.abcpeter.Parser;
import erki.abcpeter.msgs.receive.IndirectMessage;
import erki.abcpeter.msgs.receive.LoginMessage;
import erki.abcpeter.msgs.receive.LogoutMessage;
import erki.abcpeter.msgs.receive.Message;
import erki.abcpeter.msgs.receive.NickchangeMessage;
import erki.abcpeter.msgs.receive.PingMessage;
import erki.abcpeter.msgs.receive.QueryMessage;
import erki.abcpeter.msgs.receive.RawMessage;
import erki.abcpeter.msgs.receive.TextMessage;
import erki.abcpeter.msgs.response.IndirectResponseMessage;
import erki.abcpeter.msgs.response.QueryResponseMessage;
import erki.abcpeter.msgs.response.RawResponseMessage;
import erki.abcpeter.msgs.response.ResponseMessage;
import erki.abcpeter.util.Logger;

/**
 * Extends {@link Bot} to understand the IRC protocol.
 * 
 * @author Edgar Kalkowski
 */
public class IrcBot extends Bot {

    private static final String PASS_FILE = "config" + System.getProperty("file.separator") + "pass.txt";

    private final String channel;

    private LinkedList<String> users;

    private String pass, user;

    /**
     * Create a new {@code IrcBot} that joins a specific channel.
     * 
     * @param channel
     * @see Bot#Bot(String, int, String, Collection)
     */
    public IrcBot(String server, int port, String channel, String name, Collection<Class<? extends Parser>> parsers) {
        super(server, port, name, parsers);
        loadPass();
        this.channel = channel;
    }

    private void loadPass() {
        try {
            Logger.debug(this, "Trying to load pass file from " + PASS_FILE + ".");
            BufferedReader fileIn = new BufferedReader(new InputStreamReader(new FileInputStream(PASS_FILE), "UTF-8"));
            String line;
            while ((line = fileIn.readLine()) != null) {
                if (line.toUpperCase().startsWith("PASS=")) {
                    pass = line.substring("PASS=".length());
                } else if (line.toUpperCase().startsWith("USER=")) {
                    user = line.substring("USER=".length());
                }
            }
            Logger.debug(this, "Pass file found.");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        } catch (FileNotFoundException e) {
            Logger.debug(this, "Pass file could not be found.");
            pass = "pass";
            user = getName() + " " + getName().toLowerCase() + " " + getName().toUpperCase() + " " + getName();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    @Override
    public void connect() throws IOException {
        super.connect();
        users = new LinkedList<String>();
        loadPass();
        Logger.info(this, "Sending login information.");
        Logger.debug(this, "Sending »PASS " + pass + "«");
        socketOut.println("PASS " + pass);
        Logger.debug(this, "Sending »USER " + user + "«");
        socketOut.println("USER " + user);
        Logger.debug(this, "Sending »NICK " + getName() + "«");
        socketOut.println("NICK " + getName());
        String fromServer;
        int noticecounter = 0, authcounter = 0;
        while ((fromServer = socketIn.readLine()) != null) {
            Logger.debug(this, "Received from server: " + fromServer);
            if (fromServer.startsWith("PING :")) {
                socketOut.println("PONG :" + fromServer.substring(6));
            }
            if (fromServer.startsWith("NOTICE AUTH :***")) {
                authcounter++;
            }
            if (fromServer.startsWith(":Notice!")) {
                noticecounter++;
            }
            if (authcounter >= 4 || noticecounter >= 3) {
                break;
            }
        }
        socketOut.println("JOIN " + getChannel());
        Logger.info(this, "Logged in an joined channel " + getChannel() + ".");
        Logger.info(this, "Requesting user list");
        socketOut.println("NAMES " + getChannel());
        String line;
        while ((line = socketIn.readLine()) != null) {
            Logger.debug(this, "Received from server: " + line);
            String nameLine = ".*? 353 .*? = " + getChannel() + " :(.*)";
            String endLine = ".*? 366 .*? " + getChannel() + " :End of /NAMES list.";
            if (line.matches(nameLine)) {
                for (String name : line.replaceAll(nameLine, "$1").split(" ")) {
                    users.add(name);
                }
            } else if (line.matches(endLine)) {
                Logger.info(this, "User list received: " + users.toString());
                break;
            }
        }
    }

    @Override
    public Message getMessage(String line) {
        if (line == null || line.equals("")) {
            return null;
        }
        Logger.debug(this, "Received from server: " + line);
        String textLine = ":(.*?)!.*? PRIVMSG " + getChannel() + " :(.*)";
        String indirectLine = ":(.*?)!.*? PRIVMSG " + getChannel() + " :\001ACTION (.*?)\001";
        String nickLine = ":(.*?)!.*? NICK :(.*)";
        String joinLine = ":(.*?)!.*? JOIN " + getChannel();
        String partLine = ":(.*?)!.*? PART " + getChannel();
        String queryLine = ":(.*?)!.*? PRIVMSG " + getName() + " :(.*)";
        String quitLine = ":(.*?)!.*? QUIT :(.*)";
        if (line.toUpperCase().startsWith("PING")) {
            socketOut.println("PONG" + line.substring("PING".length()));
            return new PingMessage();
        } else if (line.matches(textLine)) {
            return new TextMessage(line.replaceAll(textLine, "$1"), line.replaceAll(textLine, "$2"));
        } else if (line.matches(indirectLine)) {
            return new IndirectMessage(line.replaceAll(indirectLine, "$1"), line.replaceAll(indirectLine, "$2"));
        } else if (line.matches(nickLine)) {
            users.remove(line.replaceAll(nickLine, "$1"));
            users.add(line.replaceAll(nickLine, "$2"));
            return new NickchangeMessage(line.replaceAll(nickLine, "$1"), line.replaceAll(nickLine, "$2"));
        } else if (line.matches(joinLine)) {
            users.add(line.replaceAll(joinLine, "$1"));
            return new LoginMessage(line.replaceAll(joinLine, "$1"));
        } else if (line.matches(partLine)) {
            users.remove(line.replaceAll(partLine, "$1"));
            return new LogoutMessage(line.replaceAll(partLine, "$1"), "");
        } else if (line.matches(queryLine)) {
            return new QueryMessage(line.replaceAll(queryLine, "$1"), line.replaceAll(queryLine, "$2"));
        } else if (line.matches(quitLine)) {
            users.remove(line.replaceAll(quitLine, "$1"));
            return new LogoutMessage(line.replaceAll(quitLine, "$1"), line.replaceAll(quitLine, "$2"));
        } else {
            return new RawMessage(line);
        }
    }

    @Override
    public Collection<String> getUserList() {
        LinkedList<String> nicknames = new LinkedList<String>();
        nicknames.addAll(users);
        return nicknames;
    }

    @Override
    public void send(IndirectResponseMessage message) {
        send(new RawResponseMessage("PRIVMSG " + getChannel() + " :\001ACTION " + message.getText() + "\001", message.getProbability(), message.getSleepTime()));
    }

    @Override
    public void send(ResponseMessage message) {
        send(new RawResponseMessage("PRIVMSG " + getChannel() + " :" + message.getText(), message.getProbability(), message.getSleepTime()));
    }

    @Override
    public void send(QueryResponseMessage message) {
        send(new RawResponseMessage("PRIVMSG " + message.getReceiver() + " :" + message.getText(), message.getProbability(), message.getSleepTime()));
    }

    @Override
    public String getProtocol() {
        return "irc";
    }

    /** @return The channel this bot has joined. */
    public String getChannel() {
        return channel;
    }
}
