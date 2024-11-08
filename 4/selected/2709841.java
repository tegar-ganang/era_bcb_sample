package source;

import java.io.File;
import java.util.List;
import org.jibble.pircbot.Colors;
import org.jibble.pircbot.DccFileTransfer;
import org.jibble.pircbot.PircBot;

public class Client extends PircBot {

    private Boolean Authenticated = false;

    private Server server;

    /**
	 * Costruttore del Bot
	 * 
	 */
    public Client(Server s) {
        this.server = s;
        this.setVersion("mIRC v6.21 Khaled Mardam-Bey");
        this.setAutoNickChange(true);
        this.setName(Config.getProperty("nickname"));
    }

    public void connect() {
        try {
            this.startIdentServer();
            this.connect(server.getServer(), Integer.parseInt(server.getPort()));
        } catch (Exception e) {
            System.out.println("Non posso connettermi a: " + server.getServer());
        }
    }

    protected void onConnect() {
        System.out.println("connesso a: " + server.getServer());
        List<String> channels = server.getChannels();
        for (String channel : channels) this.joinChannel(channel);
    }

    /**
	 * Metodo che parsa i messaggi ricevuti in un canale<br />
	 * Pattern: Chain of Responsibility
	 * 
     * @param channel The channel to which the message was sent.
     * @param sender The nick of the person who sent the message.
     * @param login The login of the person who sent the message.
     * @param hostname The hostname of the person who sent the message.
     * @param message The actual message sent to the channel.
     * 
	 */
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        Message msg = new Message(server.getNetwork(), channel, sender, login, hostname, Colors.removeFormattingAndColors(message));
        MessageHandler pack = new PackHandler();
        MessageHandler request = new RequestHandler();
        pack.setSuccessor(request);
        pack.handleRequest(msg);
    }

    /**
     * Metodo che parsa i messaggi privati
     * 
     */
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        System.out.println("PRIVMSG: " + message);
    }

    /**
     * Metodo base per salvare i file ricevuti in DCC
     * 
     */
    public void onIncomingFileTransfer(DccFileTransfer transfer) {
        File file = transfer.getFile();
        transfer.receive(file, true);
    }

    /**
     * Metodo per la gestione dei comandi non implementati dalla classe base.
     * @param line Stringa ricevuta dal server
     * 
     */
    public void onUnknown(String line) {
    }

    protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
        System.out.println("notice from " + sourceNick + " (" + target + "): " + Colors.removeFormattingAndColors(notice));
    }
}
