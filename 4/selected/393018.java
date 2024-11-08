package eu.popeye.middleware.dataSharing.distributedDataManagement.coherenceScheme;

import java.util.Hashtable;
import eu.popeye.middleware.dataSharing.distributedDataManagement.dataSharingMessages.coherenceMessages.*;
import eu.popeye.middleware.dataSharing.distributedDataManagement.dataSharingMessages.DataSharingMsg;
import eu.popeye.middleware.groupmanagement.membership.Member;
import eu.popeye.networkabstraction.communication.ApplicationMessageListener;
import eu.popeye.networkabstraction.communication.CommunicationChannel;
import eu.popeye.networkabstraction.communication.message.PopeyeMessage;

public class CoherenceCommunication implements ApplicationMessageListener {

    private Member myself;

    private Hashtable<String, Coherence> coherenceObjects;

    private CommunicationChannel chan;

    private static Hashtable<String, CoherenceCommunication> CoherenceCommunicationList = new Hashtable<String, CoherenceCommunication>();

    ;

    protected CoherenceCommunication(CommunicationChannel chan, String name, Member myself) {
        chan.addApplicationMessageListener(this);
        this.chan = chan;
        this.coherenceObjects = new Hashtable<String, Coherence>();
        this.myself = myself;
    }

    public static CoherenceCommunication instance(CommunicationChannel chan, String name, Member myself) {
        CoherenceCommunication newCom;
        synchronized (CoherenceCommunicationList) {
            newCom = (CoherenceCommunication) CoherenceCommunicationList.get(name);
            if (newCom == null) {
                newCom = new CoherenceCommunication(chan, name, myself);
                CoherenceCommunicationList.put(name, newCom);
            }
            return newCom;
        }
    }

    public static CoherenceCommunication instance(String name) {
        return (CoherenceCommunication) CoherenceCommunicationList.get(name);
    }

    public void register(Coherence c, String dataPath) {
        coherenceObjects.put(dataPath, c);
    }

    public void onMessage(PopeyeMessage msg) {
        if (!(msg instanceof CoherenceMsg) || ((CoherenceMsg) msg).getSender().equals(myself)) return;
        CoherenceMsg cMsg = (CoherenceMsg) msg;
        Coherence destination = (Coherence) coherenceObjects.get(cMsg.dataPath);
        if (destination != null) destination.onMessage(cMsg);
    }

    public CommunicationChannel getChannel() {
        return this.chan;
    }

    public Member getMyself() {
        return this.myself;
    }
}
