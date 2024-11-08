package org.perfectday.communication.XmppPluginsCommunicator;

import com.thoughtworks.xstream.XStream;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Presence;
import org.perfectday.communication.XmppPluginsCommunicator.model.PerfectDayPacketListener;
import org.perfectday.communication.masterCommunication.MasterCommunication;
import org.perfectday.communication.model.plugcommunication.PerfectDayMessage;
import org.perfectday.communication.model.plugcommunication.PlugCommunication;
import org.perfectday.message.model.Message;

/**
 *
 * @author Miguel Angel Lopez Montellano (alakat@gmail.com)
 */
public class XMPPPluginsCommunicator extends PlugCommunication {

    public static final String OK_STATUS = "OK";

    public static final String FAULT_STATUS = "fault";

    private static final Logger logger = Logger.getLogger(XMPPPluginsCommunicator.class);

    XMPPConnection connection;

    private String userName;

    private String pass;

    private String destiny;

    private PacketListener inListener;

    private String status;

    private Chat chat;

    public XMPPPluginsCommunicator(Chat chat) {
        this.chat = chat;
    }

    public XMPPPluginsCommunicator(String userName, String destiny) {
        this.destiny = destiny;
        this.userName = userName;
        this.connect(destiny, "dummy");
    }

    public XMPPPluginsCommunicator(String user, String pass, String dest) {
        this.destiny = dest;
        this.userName = user;
        this.pass = pass;
        this.connect(destiny, "dummy");
    }

    @Override
    public void connect(String destiny, String typeDestiny) {
        ConnectionConfiguration connConfig = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
        this.connection = new XMPPConnection(connConfig);
        try {
            logger.info("Conectando.....");
            connection.connect();
            if (this.pass != null) connection.login(userName, pass);
            logger.info("Conectado!!");
            this.status = XMPPPluginsCommunicator.OK_STATUS;
            Presence p = new Presence(Presence.Type.available);
            p.setMode(Presence.Mode.available);
            logger.info("Enviando presencia..");
            connection.sendPacket(p);
            logger.info("Presencia enviada!!");
            logger.info("Listening paquetes...");
            PacketFilter filter = new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class);
            this.inListener = new PerfectDayPacketListener();
            this.chat = this.connection.getChatManager().createChat(destiny, (MessageListener) inListener);
            logger.info("registe packetFilter...");
            this.connection.addPacketListener(inListener, filter);
            logger.info("Registro de Listener!!!");
            Roster roster = connection.getRoster();
            for (RosterEntry entry : roster.getEntries()) {
                logger.info("roster: " + entry.getUser());
            }
        } catch (XMPPException ex) {
            org.apache.log4j.Logger.getLogger(XMPPPluginsCommunicator.class).error("XMPP Communication error", ex);
            this.status = XMPPPluginsCommunicator.FAULT_STATUS;
        }
    }

    @Override
    public void disconnect() {
        if (connection != null) connection.disconnect();
    }

    @Override
    public void exposeService() {
        logger.info("dummy exposeService");
    }

    @Override
    public Message receiveMessage() {
        logger.info("Dummy method, implementado en  PerfectDayPacketListener");
        return null;
    }

    @Override
    public void sendMessage(Object message) {
        try {
            ((PerfectDayMessage) message).setSendtime(System.currentTimeMillis());
            org.jivesoftware.smack.packet.Message mes = new org.jivesoftware.smack.packet.Message();
            String data = new XStream().toXML(message);
            mes.setProperty(MasterCommunication.NAME_GAME_MESSAGE, data);
            this.chat.sendMessage(mes);
        } catch (XMPPException ex) {
            logger.error("ERROR EN COMUNICACIONES", ex);
        }
    }

    /**
     * Antigua implementación del send
     * @param message
     */
    public void sendMessage_(Message message) {
        org.jivesoftware.smack.packet.Message mes = new org.jivesoftware.smack.packet.Message();
        String data = new XStream().toXML(message);
        mes.setProperty("PD", data);
        logger.info("Send message:" + mes.getBody());
        try {
            this.chat.sendMessage(mes);
        } catch (XMPPException ex) {
            logger.error("Communication error", ex);
        }
    }

    @Override
    public void startUp() {
        logger.info("Dummy startup!!");
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public void setConnection(XMPPConnection connection) {
        this.connection = connection;
    }

    public String getDestiny() {
        return destiny;
    }

    public void setDestiny(String destiny) {
        this.destiny = destiny;
    }

    public PacketListener getInListener() {
        return inListener;
    }

    public void setInListener(PacketListener inListener) {
        this.inListener = inListener;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
