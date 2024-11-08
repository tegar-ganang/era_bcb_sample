package server.gameObjects;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import server.ChatChannelListener;
import server.factions.Faction;
import server.factions.PlayableFaction;
import server.tasks.SerialLocalSpaceLinkageAndEntryTask;
import shared.network.SerialUniversePacket;
import shared.network.SerialUniversePacket.LocalSpaceUniverseInfo;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.util.ScalableDeque;
import com.sun.sgs.app.util.ScalableList;

public final class GameUniverse implements ManagedObject, Serializable {

    private static final long serialVersionUID = 1L;

    private static final String generalChatName = "chat-general";

    public static final String tradeChatName = "chat-trade";

    public static final String bindingName = "GameUniverse";

    private ManagedReference<ScalableList<ServerLocalSpace>> localSpaceListRef = null;

    private ManagedReference<ScalableDeque<ServerLocalSpace>> addQueue = null;

    private boolean updatingLocalSpaceList = false;

    private ArrayList<ManagedReference<? extends PlayableFaction>> playableFactions = new ArrayList<ManagedReference<? extends PlayableFaction>>();

    public GameUniverse() {
    }

    public ScalableList<ServerLocalSpace> getLocalSpaces() {
        return localSpaceListRef.get();
    }

    public ArrayList<ManagedReference<? extends PlayableFaction>> getPlayableFactions() {
        return new ArrayList<ManagedReference<? extends PlayableFaction>>(playableFactions);
    }

    public ManagedReference<GameUniverse> register() {
        AppContext.getChannelManager().createChannel("chat-general", new ChatChannelListener(), Delivery.UNRELIABLE);
        AppContext.getChannelManager().createChannel("chat-trade", new ChatChannelListener(), Delivery.UNRELIABLE);
        ScalableList<ServerLocalSpace> localSpaceList = new ScalableList<ServerLocalSpace>();
        ScalableDeque<ServerLocalSpace> queue = new ScalableDeque<ServerLocalSpace>();
        localSpaceListRef = AppContext.getDataManager().createReference(localSpaceList);
        addQueue = AppContext.getDataManager().createReference(queue);
        ManagedReference<GameUniverse> ret = AppContext.getDataManager().createReference(this);
        AppContext.getDataManager().setBinding(bindingName, this);
        return ret;
    }

    public void addAndRegisterFaction(Faction f) {
        if (f instanceof PlayableFaction) {
            playableFactions.add(((PlayableFaction) f).register());
        } else f.register();
    }

    public boolean isUpdatingLocalSpaces() {
        return updatingLocalSpaceList;
    }

    public void finishUpdatingLocalSpaces() {
        updatingLocalSpaceList = false;
    }

    public void queueLocalSpaceForAdditionAndRegistration(ServerLocalSpace ls) {
        ls.register();
        addQueue.getForUpdate().add(ls);
        System.out.println("Queueing LS " + ls.getName() + " up");
        if (!updatingLocalSpaceList) {
            System.out.println("Activating connection task");
            updatingLocalSpaceList = true;
            AppContext.getTaskManager().scheduleTask(new SerialLocalSpaceLinkageAndEntryTask(addQueue));
        }
    }

    public static Channel getGeneralChat() {
        return AppContext.getChannelManager().getChannel(generalChatName);
    }

    public static Channel getTradeChat() {
        return AppContext.getChannelManager().getChannel(tradeChatName);
    }

    public static GameUniverse getReadOnly() {
        return (GameUniverse) AppContext.getDataManager().getBinding(bindingName);
    }

    public static GameUniverse getForUpdate() {
        return (GameUniverse) AppContext.getDataManager().getBindingForUpdate(bindingName);
    }

    public ArrayList<ByteBuffer> makeUniverseInformationPackets() {
        ScalableList<ServerLocalSpace> lsList = localSpaceListRef.get();
        LocalSpaceUniverseInfo[] infos = new LocalSpaceUniverseInfo[lsList.size()];
        for (int i = 0; i < infos.length; i++) {
            infos[i] = new LocalSpaceUniverseInfo();
            ServerLocalSpace ls = lsList.get(i);
            infos[i].id = ls.getId();
            infos[i].location.x = ls.location_x;
            infos[i].location.y = ls.location_y;
            infos[i].radius = ls.getRadius();
            infos[i].name = ls.getName();
            infos[i].connections.addAll(ls.getWarpDestinations());
        }
        return SerialUniversePacket.create(infos);
    }
}
