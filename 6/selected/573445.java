package org.ss.mobot.adapter;

import java.io.File;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.ss.util.IOUtils;

public class XmppClient {

    private static Logger log = Logger.getLogger(XmppClient.class.getSimpleName());

    private boolean isGuiDebug;

    private XMPPConnection connection;

    private String server;

    private int port;

    private String domain;

    private String username;

    private String password;

    private ConnectionListener conlistener = new ConnectionListener() {

        public void reconnectingIn(int arg0) {
            log.info("reconnectingIn " + arg0);
        }

        public void reconnectionFailed(Exception arg0) {
            log.warn("reconnectionFailed " + arg0.getMessage());
        }

        public void reconnectionSuccessful() {
            log.info("reconnectionSuccessful");
        }

        public void connectionClosed() {
            log.info("connection closed");
        }

        public void connectionClosedOnError(Exception arg0) {
            log.error("connection closed on error " + arg0.getMessage());
        }
    };

    private PacketListener pktListener = new PacketListener() {

        public void processPacket(Packet packet) {
            if (packet instanceof Message) {
                log.info("get a message typed packet from " + ((Message) packet).getFrom() + ", [msg] " + ((Message) packet).getBody());
            } else {
                log.info("get a packet but it is not a message, skipped");
            }
        }
    };

    private PacketFilter pktFilter = new PacketTypeFilter(Message.class);

    public XmppClient(String server, int port, String domain, String username, String password, boolean isGuiDebug) {
        this.server = server;
        this.port = port;
        this.domain = domain;
        this.isGuiDebug = isGuiDebug;
        this.username = username;
        this.password = password;
    }

    public void setGuiDebug(boolean b) {
        XMPPConnection.DEBUG_ENABLED = b;
    }

    public boolean login() {
        XMPPConnection.DEBUG_ENABLED = isGuiDebug;
        ConnectionConfiguration connectionConfig = new ConnectionConfiguration(server, port, domain);
        connectionConfig.setReconnectionAllowed(true);
        connection = new XMPPConnection(connectionConfig);
        try {
            log.info("connecting ...");
            if (!connection.isConnected()) connection.connect();
            log.info("login ...");
            if (!connection.isAuthenticated()) connection.login(username, password);
            connection.addConnectionListener(conlistener);
            connection.addPacketListener(pktListener, pktFilter);
            if (connection.isAuthenticated()) {
                log.info("authenticated.");
            }
            log.info("ready to receive messages.");
        } catch (XMPPException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void logout() {
        log.info("disconnecting ...");
        connection.removePacketListener(pktListener);
        connection.removeConnectionListener(conlistener);
        connection.disconnect();
        log.info("gtalk stops");
    }

    public void sendMessage(String jid, String msg) {
        try {
            ChatManager chatmanager = connection.getChatManager();
            Chat newChat = chatmanager.createChat(jid, new MessageListener() {

                public void processMessage(Chat chat, Message message) {
                    log.info("Received message: " + message);
                }
            });
            newChat.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(String jid, String fileFQN, String msg) {
        try {
            log.debug("sending " + jid + " file(" + fileFQN + ")");
            FileTransferManager manager = new FileTransferManager(connection);
            OutgoingFileTransfer transfer = manager.createOutgoingFileTransfer(jid);
            if (IOUtils.isFileExist(fileFQN)) {
                transfer.sendFile(new File(fileFQN), msg);
                while (!transfer.isDone()) {
                    if (transfer.getStatus().equals(FileTransfer.Status.error)) {
                        log.error("transfer file error: " + transfer.getError());
                        break;
                    } else {
                        log.debug("send file " + fileFQN + ", stat:" + transfer.getStatus());
                        log.debug("send file " + fileFQN + ", progress:" + transfer.getProgress());
                    }
                    Thread.sleep(2000);
                }
            } else {
                log.warn("sending file is missing (" + fileFQN + ")");
                sendMessage(jid, "missing file " + fileFQN);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        XmppClient g = new XmppClient("talk.google.com", 5222, "mobot", "gbook2008", "gbook@2008", false);
        g.login();
        g.logout();
    }

    public void setPktListener(PacketListener pktListener) {
        this.pktListener = pktListener;
    }
}
