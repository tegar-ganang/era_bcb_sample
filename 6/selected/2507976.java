package uk.co.gidley.teamAlert.listenerServices;

import org.apache.commons.lang.StringUtils;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.gidley.teamAlert.vo.Alert;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: ben Date: Jun 10, 2008 Time: 5:16:59 PM
 * <p/>
 * This provides a spring bean that listens to and issues alerts
 */
public class AlertController {

    private Collection<AlertListener> listeners;

    private String hostname;

    private int port;

    private String serviceName;

    private String username;

    private String password;

    private MessageDecoder messageDecoder;

    private final Logger logger = LoggerFactory.getLogger(AlertController.class);

    private XMPPConnection connection;

    public void onInit() throws XMPPException {
        ConnectionConfiguration connectionConfiguration = new ConnectionConfiguration(hostname, port, serviceName);
        connection = new XMPPConnection(connectionConfiguration);
        connection.connect();
        connection.login(username, password);
        PacketListener myListener = new PacketListener() {

            public void processPacket(Packet packet) {
                try {
                    if (packet instanceof Message) {
                        Message message = (Message) packet;
                        if (!StringUtils.isEmpty(message.getBody())) {
                            Alert alert = messageDecoder.decodeMessage((Message) packet);
                            for (AlertListener alertListener : listeners) {
                                alertListener.onAlert(alert);
                            }
                        }
                    } else {
                        logger.error("Impossible has occured :( a message of type message is not a message:{}", packet);
                    }
                } catch (Throwable e) {
                    logger.error("Exception in listener block", e);
                }
            }
        };
        connection.addPacketListener(myListener, new PacketTypeFilter(Message.class));
    }

    public void onDestroy() {
        connection.disconnect();
    }

    public Collection<AlertListener> getListeners() {
        return listeners;
    }

    public void setListeners(Collection<AlertListener> listeners) {
        this.listeners = listeners;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setMessageDecoder(MessageDecoder messageDecoder) {
        this.messageDecoder = messageDecoder;
    }
}
