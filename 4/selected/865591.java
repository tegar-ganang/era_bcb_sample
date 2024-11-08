package server;

import java.io.Serializable;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.Task;

public class JoinRoomTask implements Task, Serializable {

    private static final long serialVersionUID = 1L;

    private long id;

    private ManagedReference<Player> playerRef;

    public JoinRoomTask(long roomId, Player ply) {
        this.id = roomId;
        DataManager dataManager = AppContext.getDataManager();
        this.playerRef = dataManager.createReference(ply);
    }

    @Override
    public void run() throws Exception {
        System.out.println("JoinRoomTask.run(" + playerRef.get().getName() + ", room" + id + ")");
        DataManager dataManager = AppContext.getDataManager();
        ServerRoom room = (ServerRoom) dataManager.getBinding("room" + id);
        dataManager.markForUpdate(room);
        if (room.isFull()) {
            System.out.println(playerRef.get().getName() + " - room" + room.getId() + " is full");
            playerRef.get().getClientSession().send(Protocol.roomFull(id));
        } else if (room.hasPlayer(playerRef.get())) {
            System.out.println(playerRef.get().getName() + " is already in room" + room.getId());
        } else {
            AppContext.getTaskManager().scheduleTask(new RemoveFromLobbyTask(playerRef.get().getClientSession()));
            room.addPlayer(playerRef.getForUpdate());
            AppContext.getChannelManager().getChannel(DarkstrisServer.LOBBY_CHANNEL).send(null, Protocol.roomJoined(id, playerRef.get().getColor(), playerRef.get().getName()));
            room.getChannel().send(null, Protocol.roomJoined(id, playerRef.get().getColor(), playerRef.get().getName()));
            playerRef.get().getClientSession().send(Protocol.roomJoined(id, playerRef.get().getColor(), room.getPlayerInfos()));
            System.out.println(playerRef.get().getName() + " - joined room" + room.getId());
        }
    }
}
