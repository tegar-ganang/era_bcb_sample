package eu.popeye.middleware.dataSharing.distributedDataManagement.coherenceScheme;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import eu.popeye.middleware.dataSharing.Metadata;
import eu.popeye.middleware.dataSharing.distributedDataManagement.SharedDataContainer;
import eu.popeye.middleware.dataSharing.distributedDataManagement.dataSharingMessages.coherenceMessages.*;
import eu.popeye.middleware.groupmanagement.membership.Member;
import eu.popeye.networkabstraction.communication.CommunicationChannel;

public abstract class Coherence {

    protected CommunicationChannel chan;

    protected ArrayList<Member> neighbours;

    protected Member myself;

    protected SharedDataContainer SDC;

    protected String dataPath;

    protected String owner;

    protected boolean isOwner;

    protected boolean monitor = false;

    public Coherence(SharedDataContainer SDC, String sharedSpaceName) {
        this.dataPath = (String) SDC.getMetadata().get(Metadata.PATH);
        this.SDC = SDC;
        this.neighbours = new ArrayList<Member>();
        System.out.println("Coherence = " + sharedSpaceName + "Data path = " + this.dataPath);
        CoherenceCommunication cc = CoherenceCommunication.instance(sharedSpaceName);
        cc.register(this, dataPath);
        this.chan = CoherenceCommunication.instance(sharedSpaceName).getChannel();
        this.myself = CoherenceCommunication.instance(sharedSpaceName).getMyself();
        this.owner = (String) SDC.getMetadata().get(Metadata.OWNER);
        this.isOwner = owner.equals(this.myself.getKey().toString());
    }

    public void setOwner(String owner) {
        this.owner = owner;
        this.isOwner = owner.equals(this.myself.getKey().toString());
    }

    public boolean hasMonitor() {
        return this.monitor;
    }

    ;

    public abstract void acquire();

    public abstract void release();

    public abstract void onMessage(CoherenceMsg msg);

    public synchronized void updateMetadata() {
        UpdateMetadataMsg msg = new UpdateMetadataMsg(myself, dataPath, SDC.getMetadata());
        for (int i = 0; i < this.neighbours.size(); i++) this.chan.send((Member) neighbours.get(i), msg);
    }

    ;

    public void quitCoherenceTree() {
        Random r = new Random();
        int index = r.nextInt(this.neighbours.size());
        Member replacement = this.neighbours.get(index);
        ArrayList<Member> fwdTo = this.getFwdList(replacement);
        this.chan.send(replacement, new QuitCoherenceTreeMsg(myself, this.dataPath, fwdTo));
        ArrayList<Member> unique = new ArrayList<Member>();
        unique.add(this.neighbours.get(index));
        for (int i = 0; i < fwdTo.size(); i++) this.chan.send((Member) neighbours.get(i), new QuitCoherenceTreeMsg(myself, this.dataPath, unique));
    }

    ;

    public void addNeighbour(Member newNeighbour) {
        synchronized (neighbours) {
            if (!neighbours.contains(newNeighbour)) {
                neighbours.add(newNeighbour);
            }
        }
    }

    public void removeNeighbour(Member neighbour) {
        synchronized (neighbours) {
            if (neighbours.contains(neighbour)) neighbours.remove(neighbour);
        }
    }

    protected ArrayList<Member> getFwdList(Member origin) {
        ArrayList<Member> fwdTo = new ArrayList<Member>();
        for (int i = 0; i < this.neighbours.size(); i++) {
            Member m = (Member) this.neighbours.get(i);
            if (m != origin) fwdTo.add(m);
        }
        return fwdTo;
    }
}
