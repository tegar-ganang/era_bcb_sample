package net.peddn.typebattle.lib.remote;

import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import net.peddn.typebattle.server.TypeBattleServer;

public class ServerServiceImpl implements ServerService {

    private static final long serialVersionUID = 1L;

    private TypeBattleServer server;

    public ServerServiceImpl(TypeBattleServer server) throws RemoteException {
        super();
        this.server = server;
    }

    public void login(ClientService clientService) throws RemoteException {
        UUID clientId = clientService.getId();
        String clientVersion = clientService.getClientVersion();
        this.server.getClients().put(clientId, clientService);
        System.out.println("Client " + clientId.toString() + " version " + clientVersion + " logged in.");
        System.out.println("Client connected.");
    }

    public void logout(UUID clientId) throws RemoteException {
        this.server.getClients().remove(clientId);
        System.out.println("Client " + clientId.toString() + " successfully logged out.");
    }

    public void chat(UUID clientId, ChatMessage message) throws RemoteException {
        Set<UUID> keys = this.server.getClients().keySet();
        for (Iterator<UUID> i = keys.iterator(); i.hasNext(); ) {
            ClientService actClient = this.server.getClients().get(i.next());
            if (actClient.getChatChannel() == message.getChannel()) {
                actClient.receiveChatMessage(message);
            }
        }
    }

    public Game createGame(UUID clientId) throws RemoteException {
        UUID gameId = UUID.randomUUID();
        Game game = new Game();
        this.server.getGames().put(gameId, game);
        return (game);
    }
}
