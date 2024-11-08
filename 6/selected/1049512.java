package edu.upf.da.p2p.sm.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.filter.ThreadFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.MultiUserChat;
import edu.upf.da.p2p.P2PConnection;
import edu.upf.da.p2p.sm.P2PMessage;
import edu.upf.da.p2p.sm.P2PServerClient;
import edu.upf.da.p2p.sm.client.manager.ServiceManager;
import edu.upf.da.p2p.sm.client.message.ServiceDefinition;
import edu.upf.da.p2p.sm.server.manager.PresenceManager;
import edu.upf.da.p2p.sm.server.manager.UserStatusManager;
import edu.upf.da.p2p.sm.server.message.UserGranted;

/**
 * Esta es la implementacion de u
 * @author Grupo A.
 */
public abstract class P2PGenericServer implements P2PServerClient {

    /**
	 * Preparamos el log.
	 */
    private static final Log log = LogFactory.getLog(P2PGenericServer.class);

    /**
	 * La conexi�n con el servidor.
	 */
    protected XMPPConnection con;

    /**
	 * El chat que creamos.
	 */
    private MultiUserChat chat;

    protected ServiceManager serviceListener;

    protected PresenceManager presenceListener;

    private String serviceName;

    protected UserStatusManager userStatusManager;

    /**
	 * El constructor de la clase inicializa algunas variables si es necesario.
	 */
    public P2PGenericServer(String serviceName) {
        P2PConnection.setUP(this);
        this.serviceName = serviceName;
    }

    public P2PGenericServer(String serviceName, UserStatusManager usm) {
        this.serviceName = serviceName;
        userStatusManager = usm;
    }

    public void connect() throws XMPPException {
        ConnectionConfiguration config = new ConnectionConfiguration(Constants.gtServer, Constants.gtPort, Constants.gtServiceName);
        config.setSASLAuthenticationEnabled(true);
        config.setReconnectionAllowed(true);
        con = new XMPPConnection(config);
        con.connect();
        con.login(Constants.gtUser, Constants.gtPass, serviceName);
        chat = new MultiUserChat(con, "p2p");
        setGenericListeners();
        setListeners();
        if (log.isTraceEnabled()) {
            log.trace("Login al GoogleTalk realizado correctamente con '" + Constants.gtUser + "'.");
        }
    }

    protected abstract void setListeners();

    protected void setGenericListeners() {
        presenceListener = new PresenceManager(this, serviceName);
        PacketFilter presFilter = new PacketTypeFilter(Presence.class);
        con.createPacketCollector(presFilter);
        con.addPacketListener(presenceListener, presFilter);
        serviceListener = new ServiceManager();
        PacketFilter servFilter = new ThreadFilter(ServiceDefinition.class.getCanonicalName());
        con.createPacketCollector(servFilter);
        con.addPacketListener(serviceListener, servFilter);
        if (userStatusManager == null) {
            userStatusManager = new UserStatusManager();
            PacketFilter usFilter = new ThreadFilter(UserGranted.class.getCanonicalName());
            con.createPacketCollector(usFilter);
            con.addPacketListener(userStatusManager, usFilter);
        }
    }

    public void disconnect() {
        con.disconnect();
        if (log.isTraceEnabled()) {
            log.trace("Se ha desconectado del servidor.");
        }
    }

    public String getSMUserID() {
        return con.getUser();
    }

    public boolean isConnected() {
        return con.isConnected();
    }

    public void sendMessage(Message m) {
        try {
            chat.sendMessage(m);
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }

    public ServiceManager getServiceManager() {
        return serviceListener;
    }

    public XMPPConnection getConnection() {
        return con;
    }

    public void sendMessage(P2PMessage ar) {
        ar.sendMessage(this);
    }

    public UserStatusManager getUserStatusManager() {
        return userStatusManager;
    }
}
