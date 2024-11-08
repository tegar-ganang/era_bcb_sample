package info.reflectionsofmind.connexion.platform.core.transport.jabber;

import info.reflectionsofmind.connexion.util.AbstractListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

public final class JabberCore extends AbstractListener<JabberCore.IListener> implements PacketListener {

    private final JabberConnectionInfo info;

    private XMPPConnection connection;

    public JabberCore(final JabberConnectionInfo info) {
        this.info = info;
        SASLAuthentication.supportSASLMechanism("PLAIN", 0);
    }

    public void send(String address, String contents) {
        final Message message = new Message(address);
        message.setBody(contents);
        this.connection.sendPacket(message);
    }

    public void start() throws XMPPException {
        final ConnectionConfiguration configuration = new ConnectionConfiguration(getInfo().getHost(), getInfo().getPort());
        this.connection = new XMPPConnection(configuration);
        this.connection.connect();
        if (getInfo().getResource() != null) {
            this.connection.login(getInfo().getNode(), getInfo().getPassword(), getInfo().getResource());
        } else {
            this.connection.login(getInfo().getNode(), getInfo().getPassword());
        }
        this.connection.addPacketListener(this, null);
    }

    public void stop() {
        this.connection.disconnect();
        this.connection = null;
    }

    @Override
    public void processPacket(Packet packet) {
        if (!(packet instanceof Message)) return;
        for (IListener listener : getListeners()) {
            listener.onPacket(packet.getFrom(), ((Message) packet).getBody());
        }
    }

    public JabberConnectionInfo getInfo() {
        return this.info;
    }

    public interface IListener {

        void onPacket(String from, String content);
    }
}
