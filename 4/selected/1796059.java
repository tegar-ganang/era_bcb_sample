package net.sf.peervibes.rendezvous.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import net.sf.peervibes.protocols.membership.OverlayNetwork;
import net.sf.peervibes.rendezvous.events.RendezVousHeartbeatEvent;
import net.sf.peervibes.utils.Peer;

/**
 * This is used to maintain information about peers and the overlays where they are
 * by the RendezVousServerSession.
 * <br>
 * It has a set of specialized public methods that manipulate, in a consistent way,
 * data structures which are usually kept by the RendezVousServer.
 * 
 * @version 0.1
 * @author Joao Leitao
 */
public class PeerRegister {

    private Peer peer;

    private long timeLastContact;

    private Collection<Collection<PeerRegister>> helpers;

    private Collection<OverlayNetwork> overlaysPresent;

    public PeerRegister(Peer peer, long time) {
        this.peer = peer;
        this.timeLastContact = time;
        this.helpers = new ArrayList<Collection<PeerRegister>>();
        this.overlaysPresent = new ArrayList<OverlayNetwork>();
    }

    public PeerRegister(RendezVousHeartbeatEvent event, HashMap<OverlayNetwork, Collection<PeerRegister>> control) {
        this.peer = event.getPeerID();
        this.timeLastContact = event.getChannel().getTimeProvider().currentTimeMillis();
        this.helpers = new ArrayList<Collection<PeerRegister>>();
        this.overlaysPresent = new ArrayList<OverlayNetwork>();
        for (OverlayNetwork on : event.getOverlayNetworksArray()) {
            Collection<PeerRegister> help = null;
            if (control.containsKey(on)) help = control.get(on); else {
                help = new ArrayList<PeerRegister>();
                control.put(on, help);
            }
            this.helpers.add(help);
            help.add(this);
            this.overlaysPresent.add(on);
        }
    }

    public void updateState(RendezVousHeartbeatEvent event, HashMap<OverlayNetwork, Collection<PeerRegister>> control) {
        this.timeLastContact = event.getChannel().getTimeProvider().currentTimeMillis();
        Collection<OverlayNetwork> temp = event.getOverlayNetworks();
        for (OverlayNetwork on : this.overlaysPresent) {
            if (!temp.contains((on))) {
                Collection<PeerRegister> list = control.get(on);
                list.remove(this);
                if (list.size() == 0) control.remove(on);
            }
        }
        for (OverlayNetwork on : temp) {
            if (!this.overlaysPresent.contains(on)) {
                Collection<PeerRegister> list = null;
                if (control.containsKey(on)) list = control.get(on); else {
                    list = new ArrayList<PeerRegister>();
                    control.put(on, list);
                }
                list.add(this);
                this.helpers.add(list);
                this.overlaysPresent.add(on);
            }
        }
    }

    public void timeExpired(HashMap<OverlayNetwork, Collection<PeerRegister>> control) {
        boolean additionalCleanUpRequired = false;
        for (Collection<PeerRegister> c : this.helpers) {
            c.remove(this);
            if (c.size() == 0) additionalCleanUpRequired = true;
        }
        if (additionalCleanUpRequired) {
            for (OverlayNetwork on : this.overlaysPresent) {
                if (control.get(on).size() == 0) control.remove(on);
            }
        }
    }

    public Peer getPeer() {
        return this.peer;
    }

    public long getTimeLastContact() {
        return this.timeLastContact;
    }
}
