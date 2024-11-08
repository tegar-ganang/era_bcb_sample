package googletalkxmpp;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;

/**
 *
 * @author Dmitriy
 */
public class GTalkClient {

    private XMPPConnection con;

    private Chat chat;

    public void connect() {
        LoadConfig loadConfigFile = new LoadConfig();
        Config conf = loadConfigFile.getConfig();
        ConnectionConfiguration config = new ConnectionConfiguration(conf.getServer(), conf.getport(), conf.getHost());
        con = new XMPPConnection(config);
        try {
            con.connect();
            System.out.println("Connect " + con.getHost());
            con.login(conf.getUserName() + "@" + conf.getHost(), conf.getPassword());
            System.out.println("Logged in as " + con.getUser());
            createRoom(conf.getUser());
        } catch (XMPPException e) {
            System.out.println("Failed to connection " + con.getHost());
        }
    }

    private void createRoom(String User) {
        try {
            chat = con.getChatManager().createChat(User, new MessageListener() {

                @Override
                public void processMessage(Chat chat, Message msg) {
                    if (msg.getType() == Type.chat && msg.getBody() != null) {
                        System.out.println(msg.getFrom() + " : " + msg.getBody());
                        if (isQuite(msg.getBody())) close();
                    }
                }
            });
            Message msg = new Message();
            msg.setBody("Hi");
            chat.sendMessage(msg);
        } catch (XMPPException e) {
            System.out.println(e.getMessage());
        }
    }

    private void close() {
        con.disconnect();
        System.exit(0);
    }

    public void sendMessage(String msg) {
        try {
            if (isQuite(msg)) close();
            chat.sendMessage(msg);
        } catch (XMPPException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private Boolean isQuite(String msg) {
        Boolean result = false;
        if (msg.contentEquals("quite") || msg.contentEquals("Quite") || msg.contentEquals("exit") || msg.contentEquals("Exit")) result = true;
        return result;
    }
}
