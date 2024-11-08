package rtjdds.rtps.messages;

import rtjdds.rtps.messages.elements.Count;
import rtjdds.rtps.messages.elements.EntityId;
import rtjdds.rtps.messages.elements.FragmentNumberSet;
import rtjdds.rtps.messages.elements.SequenceNumber;
import rtjdds.rtps.portable.CDROutputPacket;
import rtjdds.rtps.types.NACK_FRAG;

public class NackFrag extends Submessage {

    protected EntityId readerId;

    protected EntityId writerId;

    protected SequenceNumber writerSN;

    protected FragmentNumberSet fragmentNumberState;

    protected Count count;

    public NackFrag(EntityId readerId, EntityId writerId, SequenceNumber writerSN, FragmentNumberSet fragmentNumberState, Count count) {
        super(NACK_FRAG.value);
        this.readerId = readerId;
        this.writerId = writerId;
        this.writerSN = writerSN;
        this.fragmentNumberState = fragmentNumberState;
        this.count = count;
    }

    protected void writeBody(CDROutputPacket os) {
        this.readerId.write(os);
        this.writerId.write(os);
        this.writerSN.write(os);
        this.fragmentNumberState.write(os);
        this.count.write(os);
    }
}
