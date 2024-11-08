package rtjdds.rtps.messages;

import rtjdds.rtps.messages.elements.EntityId;
import rtjdds.rtps.messages.elements.SequenceNumber;
import rtjdds.rtps.messages.elements.SequenceNumberSet;
import rtjdds.rtps.portable.CDROutputPacket;
import rtjdds.rtps.types.GAP;

public class Gap extends Submessage {

    protected EntityId readerId;

    protected EntityId writerId;

    protected SequenceNumber gapStart;

    protected SequenceNumberSet gapList;

    public Gap(EntityId readerId, EntityId writerId, SequenceNumber gapStart, SequenceNumberSet gapList) {
        super(GAP.value);
        this.readerId = readerId;
        this.writerId = writerId;
        this.gapStart = gapStart;
        this.gapList = gapList;
    }

    protected void writeBody(CDROutputPacket os) {
        this.readerId.write(os);
        this.writerId.write(os);
        this.gapStart.write(os);
        this.gapList.write(os);
    }
}
