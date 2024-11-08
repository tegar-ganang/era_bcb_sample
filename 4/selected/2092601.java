package com.peterhi.server.realm;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.peterhi.net.ProtocolConstants;
import com.peterhi.server.LogConstants;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelListener;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.NameNotBoundException;

public class Clan implements ManagedObject, Serializable, ChannelListener, ProtocolConstants, LogConstants {

    private static final Logger logger = LoggerFactory.getLogger(Clan.class);

    /**
	 * 
	 */
    private static final long serialVersionUID = -3103391211853285398L;

    public static Clan create(String name) {
        String mulegrpBinding = clan_bind_prefix + name;
        DataManager dman = AppContext.getDataManager();
        Clan mulegrp = null;
        try {
            mulegrp = dman.getBinding(mulegrpBinding, Clan.class);
            logger.warn(String.format(err_exists, name));
            return null;
        } catch (NameNotBoundException ex) {
            mulegrp = new Clan();
            mulegrp.init(mulegrpBinding, name);
            logger.info(String.format(clan_create, name, mulegrp));
            return mulegrp;
        }
    }

    private String binding;

    private Channel channel;

    private ManagedReference worldRef;

    private final Set<String> namelist = new HashSet<String>();

    private final Set<ManagedReference> mules = new HashSet<ManagedReference>();

    private void init(String mulegroupBinding, String name) {
        DataManager dman = AppContext.getDataManager();
        dman.markForUpdate(this);
        binding = mulegroupBinding;
        dman.setBinding(binding, this);
        channel = AppContext.getChannelManager().createChannel(name, this, Delivery.RELIABLE);
    }

    public World getWorld() {
        if (worldRef == null) {
            return null;
        }
        return worldRef.get(World.class);
    }

    public void setWorld(World world) {
        DataManager dman = AppContext.getDataManager();
        dman.markForUpdate(this);
        if (world == null) {
            worldRef = null;
            return;
        }
        worldRef = dman.createReference(world);
    }

    public boolean addMule(Mule mule) {
        DataManager dman = AppContext.getDataManager();
        dman.markForUpdate(this);
        ManagedReference ref = dman.createReference(mule);
        return mules.add(ref);
    }

    public boolean addName(String name) {
        DataManager dman = AppContext.getDataManager();
        dman.markForUpdate(this);
        return namelist.add(name);
    }

    public boolean removeMule(Mule mule) {
        DataManager dman = AppContext.getDataManager();
        dman.markForUpdate(this);
        ManagedReference ref = dman.createReference(mule);
        return mules.remove(ref);
    }

    public boolean removeName(String name) {
        DataManager dman = AppContext.getDataManager();
        dman.markForUpdate(this);
        return namelist.remove(name);
    }

    @Override
    public void receivedMessage(Channel chnl, ClientSession ses, byte[] data) {
    }

    public String toString() {
        return getClass().getSimpleName() + hashCode();
    }

    public void dispose() {
        DataManager dman = AppContext.getDataManager();
        dman.markForUpdate(this);
        try {
            channel.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        String name = channel.getName();
        logger.info(String.format(clan_close, name));
        dman.removeBinding(binding);
        dman.removeObject(this);
    }
}
