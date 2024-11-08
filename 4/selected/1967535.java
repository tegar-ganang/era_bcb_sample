package rtjdds.rtps.messages;

import rtjdds.rtps.messages.elements.Count;
import rtjdds.rtps.messages.elements.EntityId;
import rtjdds.rtps.messages.elements.FragmentNumber;
import rtjdds.rtps.messages.elements.SequenceNumber;
import rtjdds.rtps.portable.CDROutputPacket;
import rtjdds.rtps.types.HEARTBEAT_FRAG;

public class HeartBeatFrag extends Submessage {

    protected EntityId readerId;

    protected EntityId writerId;

    protected SequenceNumber writerSN;

    protected FragmentNumber fn;

    protected Count count;

    public HeartBeatFrag(EntityId readerId, EntityId writerId, SequenceNumber writerSN, FragmentNumber fn, Count count) {
        super(HEARTBEAT_FRAG.value);
        this.readerId = readerId;
        this.writerId = writerId;
        this.writerSN = writerSN;
        this.fn = fn;
        this.count = count;
    }

    protected void writeBody(CDROutputPacket os) {
        this.readerId.write(os);
        this.writerId.write(os);
        this.writerSN.write(os);
        this.fn.write(os);
        this.count.write(os);
    }
}
