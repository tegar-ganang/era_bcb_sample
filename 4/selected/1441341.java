package IRC;

import gui.StatusListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import data.Data;
import data.configuration;

/**
 * @author Finne
 *
 */
public class IRC extends Thread {

    MediawikiIRC wikimediaBot;

    VandalismIRC freenodeBot;

    configuration config;

    String channel, server, port;

    String MediawikiEnc = "UTF-8";

    String FreenodeEnc = "UTF-8";

    String nick;

    Data data;

    StatusListener listener;

    private String createNick() {
        String tstr = "" + System.currentTimeMillis();
        String nick = "vf" + config.verint + tstr.substring(8);
        return nick;
    }

    public void resolveVChannel(String channel, boolean status) {
        try {
            if (status) freenodeBot.joinChanneln(channel); else freenodeBot.partChannel(channel);
        } catch (Exception e) {
            System.out.println("channel " + channel + " status " + status);
        }
    }

    public void sendMsg(String proj, String msg) {
        freenodeBot.sendMsg(proj, msg);
    }

    public IRC(String c, String se, String p, Data d, StatusListener l) {
        super();
        data = d;
        config = configuration.getConfigurationObject();
        listener = l;
        nick = createNick();
        channel = c;
        server = se;
        port = p;
        start();
    }

    /**
	 * Joins a new VANILLA mediawikiircchanel, using the standard parser (IrcMediawikiParser)
	 * @param text projectname (equals channelname, march 2006)
	 */
    public void printStatus() {
        String[] chs;
        try {
            if (wikimediaBot != null) {
                System.out.println("wikimediaBot connected    : " + wikimediaBot.isConnected());
                System.out.println("wikimediaBot connected to : " + wikimediaBot.getServer());
                chs = wikimediaBot.getChannels();
                for (int j = 0; j < chs.length; j++) {
                    System.out.println("wikimediaBot at           : " + chs[j]);
                }
            }
            if (freenodeBot != null) {
                System.out.println("\n");
                System.out.println("freenodeBot connected    : " + freenodeBot.isConnected());
                System.out.println("freenodeBot connected to : " + freenodeBot.getServer());
                chs = freenodeBot.getChannels();
                for (int j = 0; j < chs.length; j++) {
                    System.out.println("freenodeBot at           : " + chs[j]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void joinNewMediaWikiChannel(String text) {
        wikimediaBot.joinChannel(text);
        listener.updateStatus("joined channel " + text);
    }

    public void JoinChannels() {
        if (config.getBooleanProp("vandalism_en_box")) {
            freenodeBot.joinChanneln("#vandalism-en-wp-2");
        }
        if (config.getBooleanProp("vandalism_nl_box")) {
            freenodeBot.joinChanneln("#wikipedia-nl-vandalism");
        }
        if (config.getBooleanProp("vandalism_it_box")) {
            String ch = "#wikipedia-it-vandalism";
            freenodeBot.joinChanneln(ch);
        }
        if (config.getBooleanProp("vandalism_fr_box")) {
            freenodeBot.joinChanneln("#vandalism-fr-wp");
        }
    }

    public void pauze() {
        freenodeBot.pauze();
        wikimediaBot.pauze();
        listener.updateStatus("pauzed");
    }

    public void unpauze() {
        listener.updateStatus("unpauzed");
        wikimediaBot.unpauze();
        freenodeBot.unpauze();
    }

    private void connectWikimedia(String server, int iPort) {
        wikimediaBot.changeNick(createNick());
        try {
            wikimediaBot.setEncoding(MediawikiEnc);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            wikimediaBot.connect(server, iPort);
        } catch (NickAlreadyInUseException e) {
            wikimediaBot.changeNick(createNick());
        } catch (IOException e) {
            System.out.println("ioexception");
        } catch (IrcException e) {
            System.out.println("ircexception");
        }
        listener.updateStatus("connected to server");
    }

    public void connect(String c) {
        int iPort = 6667;
        if (wikimediaBot == null || !wikimediaBot.isConnected()) {
            try {
                iPort = Integer.parseInt(port);
            } catch (Exception e) {
                System.out.println("IRC.java, problem parsing integer: " + e.getMessage());
            }
            wikimediaBot = new MediawikiIRC(nick, data);
            connectWikimedia(server, iPort);
            try {
                wikimediaBot.setEncoding(MediawikiEnc);
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
            try {
                wikimediaBot.setEncoding(MediawikiEnc);
            } catch (Exception e) {
                System.out.println("wrong encoding: " + MediawikiEnc);
            }
            System.out.println("encoded: " + wikimediaBot.getEncoding());
        }
        String[] channels = c.split(",");
        for (int i = 0; i < channels.length; i++) {
            if (channels[i].charAt(0) != '#') {
                channels[i] = '#' + channels[i];
            }
            wikimediaBot.joinChannel(channels[i]);
        }
        listener.updateStatus("joined channel");
        if (freenodeBot == null || !freenodeBot.isConnected()) {
            freenodeBot = new VandalismIRC(nick, data);
            connectFreenode(server, iPort);
            try {
                freenodeBot.setEncoding(FreenodeEnc);
            } catch (Exception e) {
                System.out.println("wrong encoding: " + MediawikiEnc);
            }
            System.out.println("encoded: " + freenodeBot.getEncoding());
        }
        JoinChannels();
        listener.updateStatus("joined channel");
    }

    public void run() {
        connect(channel);
    }

    public void joinVandalismChannel(String text) {
        freenodeBot.joinChannel(text);
    }

    public void quit(String string) {
        freenodeBot.quitServer(string);
        wikimediaBot.quitServer(string);
        listener.updateStatus("quit ircstream");
    }

    private void connectFreenode(String server, int iPort) {
        try {
            freenodeBot.changeNick(createNick());
            System.out.println("vandalism: connecting " + server);
            freenodeBot.setEncoding(FreenodeEnc);
            freenodeBot.connect("irc.freenode.org", iPort);
            listener.updateStatus("connected to server");
        } catch (NickAlreadyInUseException e) {
            freenodeBot.changeNick(createNick());
            freenodeBot.disconnect();
            listener.updateStatus("nick already in use, try again");
        } catch (IOException e) {
            System.out.println("ioexception");
        } catch (IrcException e) {
            System.out.println("ircexception");
        }
    }

    public boolean isConnected() {
        return wikimediaBot.isConnected() || freenodeBot.isConnected();
    }

    public void updateConfig() {
        String[] channels = wikimediaBot.getChannels();
        String channelStr = "";
        for (int i = 0; i < channels.length; i++) if (i == 0) channelStr += channels[i]; else channelStr += ", " + channels[i];
        config.setProperty("channel", channelStr);
        String[] vchannels = wikimediaBot.getChannels();
        String vchannelStr = "";
        for (int i = 0; i < vchannels.length; i++) if (i == 0) vchannelStr += vchannels[i]; else vchannelStr += ", " + vchannels[i];
        config.setProperty("vandalismchannel", vchannelStr);
    }
}
