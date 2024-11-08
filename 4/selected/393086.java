package server.factions;

import java.util.ArrayList;
import server.ChatChannelListener;
import server.gameObjects.ServerPlayerShip;
import server.player.Player;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.NameNotBoundException;

public abstract class PlayableFaction extends Faction {

    private static final long serialVersionUID = 1L;

    private static final String chatPrefix = "factionChat-";

    public PlayableFaction(String name) {
        super(name);
    }

    @Override
    public ManagedReference<? extends PlayableFaction> register() {
        super.register();
        AppContext.getChannelManager().createChannel(chatPrefix + getName(), new ChatChannelListener(), Delivery.UNRELIABLE);
        return AppContext.getDataManager().createReference(this);
    }

    @Override
    public void unRegister() {
        AppContext.getDataManager().removeObject(AppContext.getChannelManager().getChannel(chatPrefix + getName()));
        super.unRegister();
    }

    public Channel getChatChannel() {
        return AppContext.getChannelManager().getChannel(chatPrefix + getName());
    }

    public static Channel getFactionChatChannel(String name) {
        try {
            return AppContext.getChannelManager().getChannel(chatPrefix + name);
        } catch (NameNotBoundException e) {
            return null;
        }
    }

    public abstract ArrayList<String> getUsableShipNames();

    public abstract ServerPlayerShip makeShipBySelection(byte sel, ManagedReference<Player> playerRef);
}
