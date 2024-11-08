package edu.upf.da.p2p.sm.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromContainsFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.ThreadFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import edu.upf.da.p2p.P2PConnection;
import edu.upf.da.p2p.sm.IMClient;
import edu.upf.da.p2p.sm.P2PMessage;
import edu.upf.da.p2p.sm.P2PUserClient;
import edu.upf.da.p2p.sm.client.manager.AuthManager;
import edu.upf.da.p2p.sm.client.manager.SearchResultManager;
import edu.upf.da.p2p.sm.client.manager.ServiceManager;
import edu.upf.da.p2p.sm.client.message.AuthResponse;
import edu.upf.da.p2p.sm.client.message.SearchRequest;
import edu.upf.da.p2p.sm.client.message.SearchResult;
import edu.upf.da.p2p.sm.client.message.ServiceDefinition;

/**
 * Esta es la implementaci�n propia del sistema de mensajeria del grupo A.
 * Hemos hecho esta segunda implementaci�n del sistema de mensajeria cogiendo
 * ideas del primer entregable pero implementando a nuestra manera.
 *
 * Por esta raz�n no usamos la API proporcionada en el primer entregable:
 * - Mantenemos la interfaz {@link IMClient} para poder hacer distintas
 * implementaciones de lo mismo y usar una u otra independientemente.
 *
 * No obstante los listeners/managers usaran metodos propios de la
 * implementaci�n, asi que para cada sistema de mensajeria que se implemente,
 * se deber� implementar todos sus listeners de nuevo.
 *
 * - Los mensajes pasan a extender la clase {@link Message} para hacer la
 * creaci�n, envio y recepci�n de mensajes m�s f�cil para el desarrollador.
 * A�n as� siguen siendo reutilizables.
 *
 * Asi se mantiene la implementaci�n mas limpia, ordenada y facil
 * de entender para nosotros.
 * @author Grupo A.
 */
public class P2PClient implements P2PUserClient {

    /**
	 * Preparamos el log.
	 */
    private static final Log log = LogFactory.getLog(P2PClient.class);

    /**
	 * La conexi�n con el servidor.
	 */
    private XMPPConnection con;

    /**
	 * El chat que creamos.
	 */
    private MultiUserChat chat;

    private FileTransferManager manager;

    private ServiceManager serviceListener;

    private AuthManager authListener;

    /**
	 * El constructor de la clase inicializa algunas variables si es necesario.
	 */
    public P2PClient() {
        P2PConnection.setUP(this);
    }

    public void connect() throws XMPPException {
        ConnectionConfiguration config = new ConnectionConfiguration(Constants.gtServer, Constants.gtPort, Constants.gtServiceName);
        config.setSASLAuthenticationEnabled(true);
        config.setReconnectionAllowed(true);
        con = new XMPPConnection(config);
        con.connect();
        con.login(Constants.gtUser, Constants.gtPass, "p2p");
        serviceListener = new ServiceManager();
        PacketFilter servFilter = new ThreadFilter(ServiceDefinition.class.getCanonicalName());
        con.createPacketCollector(servFilter);
        con.addPacketListener(serviceListener, servFilter);
        authListener = new AuthManager(this);
        PacketFilter[] filters = new PacketFilter[2];
        filters[0] = new ThreadFilter(AuthResponse.class.getCanonicalName());
        filters[1] = new NotFilter(new FromContainsFilter(getSMUserID()));
        PacketFilter filterf = new AndFilter(filters);
        con.createPacketCollector(filterf);
        con.addPacketListener(authListener, filterf);
        chat = new MultiUserChat(con, "p2p");
        manager = new FileTransferManager(con);
        if (log.isTraceEnabled()) {
            log.trace("Login al GoogleTalk realizado correctamente con '" + Constants.gtUser + "'.");
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

    public void sendPacket(Packet p) {
        con.sendPacket(p);
    }

    public SearchResultManager getSearchResultManager(SearchRequest sr) {
        SearchResultManager searchResultListener = new SearchResultManager();
        PacketFilter[] filters = new PacketFilter[3];
        filters[0] = new ThreadFilter(SearchResult.class.getCanonicalName());
        filters[1] = new NotFilter(new FromContainsFilter(getSMUserID()));
        filters[2] = new PacketIDFilter(sr.getPacketID());
        PacketFilter filterf = new AndFilter(filters);
        con.createPacketCollector(filterf);
        con.addPacketListener(searchResultListener, filterf);
        sr.setTo(getServiceManager().getServiceAddress("search"));
        sendMessage(sr);
        return searchResultListener;
    }

    public ServiceManager getServiceManager() {
        return serviceListener;
    }

    public FileTransferManager getFileTransferManager() {
        return manager;
    }

    public AuthManager getAuthManager() {
        return authListener;
    }

    public XMPPConnection getConnection() {
        return con;
    }

    public void sendMessage(P2PMessage ar) {
        ar.sendMessage(this);
    }
}
