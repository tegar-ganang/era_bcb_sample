package com.peterhi.server.realm;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelListener;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.ManagedObject;
import com.sun.sgs.app.ManagedReference;
import com.sun.sgs.app.NameNotBoundException;

public class Classroom implements ManagedObject, Serializable, ChannelListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = -3103391211853285398L;

    public static Classroom create(String name) {
        ChannelManager cman = AppContext.getChannelManager();
        Classroom classroom;
        try {
            cman.getChannel(name);
            return null;
        } catch (NameNotBoundException ex) {
            classroom = new Classroom();
            classroom.setBinding(name);
            Channel chnl = cman.createChannel(name, classroom, Delivery.RELIABLE);
            classroom.setChannel(chnl);
            return classroom;
        }
    }

    private String binding;

    private Channel channel;

    private ManagedReference worldRef;

    private final Set<ManagedReference> mules = new HashSet<ManagedReference>();

    protected void setChannel(Channel chnl) {
        AppContext.getDataManager().markForUpdate(this);
        channel = chnl;
    }

    protected void setBinding(String name) {
        DataManager dman = AppContext.getDataManager();
        dman.markForUpdate(this);
        binding = name;
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

    public boolean removeMule(Mule mule) {
        DataManager dman = AppContext.getDataManager();
        dman.markForUpdate(this);
        ManagedReference ref = dman.createReference(mule);
        return mules.remove(ref);
    }

    @Override
    public void receivedMessage(Channel chnl, ClientSession ses, byte[] data) {
    }

    public String toString() {
        return getClass().getSimpleName() + hashCode();
    }
}
