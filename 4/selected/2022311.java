package rtjdds.rtps.messages;

import rtjdds.rtps.messages.elements.Count;
import rtjdds.rtps.messages.elements.EntityId;
import rtjdds.rtps.messages.elements.SequenceNumber;
import rtjdds.rtps.portable.CDROutputPacket;
import rtjdds.rtps.types.HEARTBEAT;

public class HeartBeat extends Submessage {

    protected EntityId readerId;

    protected EntityId writerId;

    protected SequenceNumber firstSN;

    protected SequenceNumber lastSN;

    protected Count count;

    public HeartBeat(EntityId readerId, EntityId writerId, SequenceNumber firstSN, SequenceNumber lastSN, Count count) {
        super(HEARTBEAT.value);
        this.readerId = readerId;
        this.writerId = writerId;
        this.firstSN = firstSN;
        this.lastSN = lastSN;
        this.count = count;
    }

    public void setFinal(boolean f) {
        super.setFlagAt(1, f);
    }

    public void setLiveliness(boolean l) {
        super.setFlagAt(2, l);
    }

    protected void writeBody(CDROutputPacket os) {
        this.readerId.write(os);
        this.writerId.write(os);
        this.firstSN.write(os);
        this.lastSN.write(os);
        this.count.write(os);
    }
}
