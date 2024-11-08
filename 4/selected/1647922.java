package net.peddn.typebattle.lib.remote;

import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import net.peddn.typebattle.client.TypeBattleClient;

public class ClientServiceImpl extends UnicastRemoteObject implements ClientService {

    private static final long serialVersionUID = 1L;

    private TypeBattleClient client;

    private ClientSession clientSession;

    public ClientServiceImpl(TypeBattleClient client) throws RemoteException {
        super();
        this.client = client;
        this.clientSession = new ClientSession();
    }

    public ClientServiceImpl(int port) throws RemoteException {
        super(port);
    }

    public ClientServiceImpl(int port, RMIClientSocketFactory csf, RMIServerSocketFactory ssf) throws RemoteException {
        super(port, csf, ssf);
    }

    public UUID getId() throws RemoteException {
        return (this.clientSession.getId());
    }

    public void setId(UUID id) throws RemoteException {
        this.clientSession.setId(id);
    }

    public String getClientVersion() throws RemoteException {
        return (this.clientSession.getClientVersion());
    }

    public void setClientVersion(String version) throws RemoteException {
        this.clientSession.setClientVersion(version);
    }

    public short getChatChannel() throws RemoteException {
        return (this.clientSession.getChatChannel());
    }

    public void setChatChannel(short chatChannel) throws RemoteException {
        this.clientSession.setChatChannel(chatChannel);
    }

    public void receiveChatMessage(ChatMessage message) throws RemoteException {
        System.out.println(message.getChannel() + ": " + message.getMessage());
    }

    public ClientSession getClientSession() {
        return clientSession;
    }

    public void setClientSession(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    public TypeBattleClient getClient() {
        return client;
    }

    public void setClient(TypeBattleClient client) {
        this.client = client;
    }

    public void isReachable() throws RemoteException {
    }
}
