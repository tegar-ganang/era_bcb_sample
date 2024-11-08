package server.managed;

import java.io.Serializable;
import java.util.Set;
import javolution.util.FastSet;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.NameNotBoundException;
import com.sun.sgs.app.ObjectNotFoundException;
import common.Constants;

public class ChannelData implements ManagedObject, Serializable {

    private static final long serialVersionUID = -7621806321224828487L;

    private Channel channel;

    private Set<Presence> presences;

    public ChannelData(Channel channel) {
        this.channel = channel;
        presences = new FastSet<Presence>();
    }

    public Channel getChannel() {
        return channel;
    }

    public void addPresences(Set<Presence> coll) {
        presences.addAll(coll);
    }

    public int presenceSize() {
        return presences.size();
    }

    public Set<Presence> getPresences() {
        return presences;
    }

    public boolean addPresence(Presence p) {
        return presences.add(p);
    }

    public boolean contains(Presence p) {
        return presences.contains(p);
    }

    public void removePresence(Presence p) {
        presences.remove(p);
    }

    public static ChannelData getChannelData(String channelName) {
        try {
            DataManager dman = AppContext.getDataManager();
            return dman.getBinding(Constants.CHANNEL_DATA + channelName, ChannelData.class);
        } catch (NameNotBoundException ex) {
            return null;
        } catch (ObjectNotFoundException ex) {
            return null;
        }
    }

    public static ChannelData getChannelData(Channel channel) {
        return getChannelData(channel.getName());
    }
}
