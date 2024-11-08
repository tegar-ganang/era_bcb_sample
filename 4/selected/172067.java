package utils;

import gui.display.ServerDisplay;

/**
 *
 * @author fuujin
 */
public class ServerThreadRunnable implements Runnable {

    private static final char NICK_EXTENSION_CHAR = '_';

    private final ServerConnection server_;

    private final ServerDisplay display_;

    public ServerThreadRunnable(ServerConnection server) {
        super();
        server_ = server;
        display_ = server.getDisplay();
    }

    private boolean writeToBuffer(String message) {
        try {
            server_.getWriter().write(message);
            server_.getWriter().flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String readFromBuffer() {
        try {
            return server_.getReader().readLine();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void run() {
        String msg;
        while (true) {
            try {
                msg = readFromBuffer();
                if (msg == null) {
                    display_.print("Got a NULL response from the server!");
                    return;
                }
                if (IRCParser.isPINGMessage(msg)) {
                    writeToBuffer(IRCParser.createPONGMessage(msg) + "\n");
                    continue;
                }
                if (server_.getLock().isLocked()) {
                    if (IRCParser.isStatusMessage(msg)) {
                        int status = IRCParser.getStatusCode(msg);
                        switch(status) {
                            case IRCParser.RPL_WELCOME:
                                server_.getLock().unlock();
                                continue;
                            case IRCParser.ERR_NICKNAMEINUSE:
                                if (server_.getCurrentNick().length() >= 8) server_.setCurrentNick(server_.getCurrentNick().substring(0, 7));
                                server_.setCurrentNick(server_.getCurrentNick() + NICK_EXTENSION_CHAR);
                                writeToBuffer("NICK " + server_.getCurrentNick() + "\n");
                                continue;
                            default:
                                break;
                        }
                    }
                }
                if (IRCParser.isCommandMessage(msg)) {
                    String command = IRCParser.getCommand(msg);
                    String author = IRCParser.getAuthor(msg);
                    if (command.equals("NICK") || command.equals("QUIT") || command.equals("PART")) {
                        if (author.equals(server_.getCurrentNick())) {
                            if (command.equals("NICK")) server_.setCurrentNick(IRCParser.getMessage(msg)); else if (command.equals("PART")) {
                                server_.removeChat(IRCParser.getChannel(msg));
                                continue;
                            }
                        }
                        if (command.equals("PART")) server_.sendToChat(IRCParser.getChannel(msg), msg); else server_.sendToAllChats(msg);
                    } else {
                        String channel = IRCParser.getChannel(msg);
                        if (command.equals("MODE") && channel.equals(author)) {
                            display_.print(msg);
                            continue;
                        }
                        server_.joinIfNotInChat(channel);
                        server_.sendToChat(channel, msg);
                    }
                } else if (IRCParser.isStatusMessage(msg)) {
                    int status = IRCParser.getStatusCode(msg);
                    switch(status) {
                        case IRCParser.RPL_TOPIC:
                        case IRCParser.RPL_TOPICWHOTIME:
                        case IRCParser.RPL_NAMREPLY:
                        case IRCParser.RPL_ENDOFNAMES:
                            String channel = IRCParser.getChannel(msg);
                            server_.joinIfNotInChat(channel);
                            server_.sendToChat(channel, msg);
                            break;
                        default:
                            display_.print(IRCParser.getMessage(msg));
                    }
                } else display_.print(msg);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
