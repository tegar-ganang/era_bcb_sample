package jade.core.event;

import jade.core.ContainerID;
import jade.core.Channel;
import jade.domain.FIPAAgentManagement.Envelope;

/**
   This class represents an event related to the MTP configuration.

   @author Giovanni Rimassa - Universita` di Parma
   @version $Date: 2002-08-28 17:14:13 +0200 (mer, 28 ago 2002) $ $Revision: 3354 $
 */
public class MTPEvent extends JADEEvent {

    public static final int ADDED_MTP = 1;

    public static final int REMOVED_MTP = 2;

    public static final int MESSAGE_IN = 3;

    public static final int MESSAGE_OUT = 4;

    private Channel chan;

    private Envelope env;

    private byte[] payload;

    public MTPEvent(int id, ContainerID cid, Channel ch) {
        super(id, cid);
        if (!isInstall()) {
            throw new InternalError("Bad event kind: it must be an MTP installation related kind.");
        }
        chan = ch;
        env = null;
        payload = null;
    }

    public MTPEvent(int id, ContainerID cid, Envelope e, byte[] pl) {
        super(id, cid);
        if (!isCommunication()) {
            throw new InternalError("Bad event kind: it must be a communication related kind.");
        }
        chan = null;
        env = e;
        payload = pl;
    }

    public Channel getChannel() {
        return chan;
    }

    public Envelope getEnvelope() {
        return env;
    }

    public byte[] getPayload() {
        return payload;
    }

    public boolean isInstall() {
        return (type == ADDED_MTP) || (type == REMOVED_MTP);
    }

    public boolean isCommunication() {
        return (type == MESSAGE_IN) || (type == MESSAGE_OUT);
    }
}
