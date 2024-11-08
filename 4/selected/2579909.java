package monitor.hooks;

import java.io.IOException;
import irc.authentication.SecondaryIRCAuthenticator;
import irc.authentication.SecondaryMessageIRCAuthenticator;
import irc.parsers.BTNWhatAutoParser;
import irc.parsers.BasicIRCParser;
import manager.Manager;
import monitor.Monitor;
import objects.BatchSettings;
import org.apache.log4j.Logger;
import org.jibble.pircbot.Colors;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import release.parser.SentinelReleaseParser;

public class IRCHook extends PircBot {

    private Logger LOGGER;

    private Manager _Mngr;

    private Monitor _Monitor;

    public BatchSettings SETTINGS;

    private BasicIRCParser PARSER = null;

    private SecondaryIRCAuthenticator AUTHENTICATOR = null;

    private SentinelReleaseParser SRP = null;

    protected boolean NickServAuthenicated = false;

    protected boolean SecondaryAuthenticated = false;

    public IRCHook(Manager Mngr, Monitor Monitor, BatchSettings Settings) {
        _Monitor = Monitor;
        SETTINGS = Settings;
        LOGGER = Logger.getLogger(IRCHook.class + "|" + SETTINGS._Label);
        _Mngr = Mngr;
        this.setName(SETTINGS.getBotName());
        SRP = new SentinelReleaseParser(SETTINGS);
        getParserAndAuthenticator();
    }

    public void print(String message) {
        System.out.println("[" + SETTINGS._Label + " | IRC ] " + message);
    }

    public void connect() {
        try {
            super.connect(SETTINGS.getIRCServer(), SETTINGS.getIRCPort());
        } catch (NickAlreadyInUseException e) {
            LOGGER.error("Nick is Already in Use!");
        } catch (IOException e) {
            LOGGER.error("IOException, This usually means your net is down or something.");
        } catch (IrcException e) {
            LOGGER.error("IRC Exception!");
        }
    }

    @Override
    protected void onDisconnect() {
        print("Disconnected!");
        NickServAuthenicated = false;
    }

    protected void onConnect() {
        print("Connected!");
        DoAuthenticate();
        joinOthers();
    }

    @Override
    protected void onJoin(String channel, String sender, String login, String hostname) {
        if (sender.equals(super.getNick())) {
            print("Joined Channel: " + channel);
        }
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (channel.toLowerCase().contains(SETTINGS.getAnnounceChannel().toLowerCase()) && sender.toLowerCase().contains(SETTINGS.getAnnouncerNick().toLowerCase()) && PARSER != null) {
            print("Found new Release!");
            _Mngr.AddRLS(SRP.ParseRelease(message));
        }
        if (sender.equalsIgnoreCase(SETTINGS.getBotOwner())) {
            OwnerCommandHandler(channel, Colors.removeFormattingAndColors(message));
        }
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        if (sender.equalsIgnoreCase(SETTINGS.getBotOwner())) {
            OwnerCommandHandler(sender, Colors.removeFormattingAndColors(message));
        }
    }

    private void OwnerCommandHandler(String source, String message) {
        String[] Cmd = message.split(" ");
        if (Cmd[0].equalsIgnoreCase(CMDPREFIX + "disconnect")) {
            this.disconnect();
        }
        if (Cmd[0].equalsIgnoreCase(CMDPREFIX + "shutdown")) {
            super.sendMessage(source, "Bye Bye :-(");
            _Mngr.Shutdown();
        }
        if (Cmd[0].equalsIgnoreCase(CMDPREFIX + "reconnect")) {
            try {
                super.reconnect();
            } catch (NickAlreadyInUseException e) {
                LOGGER.error("Nick is Already in Use!");
            } catch (IOException e) {
                LOGGER.error("IOException, This usually means your net is down or something.");
            } catch (IrcException e) {
                LOGGER.error("IRC Exception, This usually happens when the IRC server hates you :-(.");
            }
        }
        if (Cmd[0].equalsIgnoreCase(CMDPREFIX + "disconnect")) {
            this.disconnect();
        }
        if (Cmd[0].equalsIgnoreCase(CMDPREFIX + "join") && Cmd.length > 1) {
            super.joinChannel(Cmd[1]);
        }
        if (Cmd[0].equalsIgnoreCase(CMDPREFIX + "part") && Cmd.length > 1) {
            super.partChannel(Cmd[1]);
        }
        if (Cmd[0].equalsIgnoreCase(CMDPREFIX + "statcpu")) {
            super.sendMessage(source, "Available Processors (Cores): " + Runtime.getRuntime().availableProcessors());
        }
        if (Cmd[0].equalsIgnoreCase(CMDPREFIX + "stat")) {
            if (Cmd.length > 1 && Cmd[1].equalsIgnoreCase("channels")) {
                StringBuffer tmpBuffer = new StringBuffer("");
                String[] tmp = super.getChannels();
                for (int i = 0; i < tmp.length; i++) {
                    tmpBuffer.append(tmp[i]);
                    if (i < tmp.length - 1) {
                        tmpBuffer.append(",");
                    }
                }
                super.sendMessage(source, "Current Channels: " + tmpBuffer.toString());
            } else if (Cmd.length > 1 && Cmd[1].equalsIgnoreCase("watcher")) {
                super.sendMessage(source, "(Announce Channel | Announcer)");
                super.sendMessage(source, "(" + SETTINGS.getAnnounceChannel() + "|" + SETTINGS.getAnnouncerNick() + ")");
            } else {
                super.sendMessage(source, "Stat requires more arguments.");
            }
        }
    }

    /**
	 * Authentication Below
	 */
    private void DoAuthenticate() {
        AuthNickServ();
        if (AUTHENTICATOR != null) {
            AUTHENTICATOR.Authenticate();
        }
    }

    private void AuthNickServ() {
        super.sendMessage("nickserv", "identify " + SETTINGS.getNickServPW());
    }

    /**
	 * Handle Notices
	 */
    @Override
    protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
        if (sourceNick.equalsIgnoreCase("nickserv")) {
            HandleNickServ(notice);
        }
        if (AUTHENTICATOR != null) {
            AUTHENTICATOR.onAuthenticatorNotice(notice);
        }
    }

    private void HandleNickServ(String Notice) {
        if (Notice.equalsIgnoreCase("Password accepted - you are now recognized.")) {
            NickServAuthenicated = true;
            print("IRC Bot has Authenticated!");
        }
    }

    private void getParserAndAuthenticator() {
        AUTHENTICATOR = new SecondaryMessageIRCAuthenticator(this);
    }

    @Deprecated
    private void getStaticParser() {
        if (SETTINGS._Label.equalsIgnoreCase("btn")) {
            print("IRC Set to BTN");
            PARSER = new BTNWhatAutoParser();
        }
    }

    private void joinOthers() {
        String[] Channels = SETTINGS.getOtherChannels().split(",");
        for (String Current : Channels) {
            super.joinChannel(Current);
        }
    }

    private static String CMDPREFIX = "$";

    private static String CHANNELPREFIXS = "#&+!";
}
