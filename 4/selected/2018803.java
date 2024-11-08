package net.jetrix.clients;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Logger;
import net.jetrix.*;
import net.jetrix.protocols.*;
import net.jetrix.config.*;

/**
 * Command line console.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 857 $, $Date: 2010-05-04 13:55:19 -0400 (Tue, 04 May 2010) $
 */
public class ConsoleClient implements Client {

    private Console console = System.console();

    private ServerConfig conf;

    private Protocol protocol;

    private User user;

    private Channel channel;

    private Logger log = Logger.getLogger("net.jetrix");

    private boolean closed = false;

    public ConsoleClient() {
        conf = Server.getInstance().getConfig();
        protocol = ProtocolManager.getInstance().getProtocol(ConsoleProtocol.class);
        user = new User();
        user.setName("Admin");
        user.setAccessLevel(100);
        user.setLocale(conf.getLocale());
        user.setSpectator();
    }

    public Protocol getProtocol() {
        return this.protocol;
    }

    public void run() {
        if (console == null) {
            log.info("Console interface unavailable");
            return;
        }
        while (conf.isRunning() && !closed) {
            try {
                Message message = receive();
                if (message != null) {
                    Server.getInstance().send(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (closed) {
            log.info("Standard input closed, shutting down the console...");
        }
    }

    public void send(Message message) {
        String msg = protocol.translate(message, user.getLocale());
        if (msg != null) {
            console.writer().println(msg);
        }
    }

    public Message receive() throws IOException {
        String line = console.readLine();
        if (line == null) {
            closed = true;
        }
        Message message = protocol.getMessage(line);
        if (message != null) {
            message.setSource(this);
        }
        return message;
    }

    public InetAddress getInetAddress() {
        return conf.getHost();
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean supportsMultipleChannels() {
        return false;
    }

    public boolean supportsAutoJoin() {
        return true;
    }

    public User getUser() {
        return user;
    }

    public String getAgent() {
        return "Console";
    }

    public String getVersion() {
        return "1.0";
    }

    public Date getConnectionTime() {
        return null;
    }

    public long getIdleTime() {
        return 0;
    }

    public String getEncoding() {
        return Charset.defaultCharset().name();
    }

    public void disconnect() {
        try {
            System.in.close();
        } catch (IOException e) {
            log.warning("Unable to close the standard input : " + e.getMessage());
        }
    }
}
