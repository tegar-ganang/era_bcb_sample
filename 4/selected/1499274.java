package server;

import java.io.Serializable;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.Task;

public class RemoveFromLobbyTask implements Task, Serializable {

    private static final long serialVersionUID = 5081420764449502812L;

    private ManagedReference<ClientSession> sessRef;

    public RemoveFromLobbyTask(ClientSession sess) {
        DataManager dataManager = AppContext.getDataManager();
        this.sessRef = dataManager.createReference(sess);
    }

    @Override
    public void run() throws Exception {
        System.out.println("RemoveFromLobbyTask.run(" + sessRef.get().getName() + ")");
        Lobby lobby = (Lobby) AppContext.getDataManager().getBinding(DarkstrisServer.LOBBY);
        AppContext.getDataManager().markForUpdate(lobby);
        lobby.remove(sessRef.get().getName());
        Channel lobbyChannel = AppContext.getChannelManager().getChannel(DarkstrisServer.LOBBY_CHANNEL);
        lobbyChannel.leave(sessRef.get());
    }
}
