package rtjdds.rtps.messages;

import rtjdds.rtps.messages.elements.Count;
import rtjdds.rtps.messages.elements.EntityId;
import rtjdds.rtps.messages.elements.SequenceNumberSet;
import rtjdds.rtps.portable.CDROutputPacket;
import rtjdds.rtps.types.ACKNACK;

public class AckNack extends Submessage {

    private EntityId readerId = null;

    private EntityId writerId = null;

    private SequenceNumberSet sns = null;

    private Count count = null;

    public AckNack(EntityId readerId, EntityId writerId, SequenceNumberSet sns, Count count) {
        super(ACKNACK.value);
        this.readerId = readerId;
        this.writerId = writerId;
        this.sns = sns;
        this.count = count;
    }

    /**
	 * Checks if the Reader requires or not a response from the Writer 
	 * @return
	 */
    public boolean isFinal() {
        return getFlagAt(1);
    }

    public void setFinal(boolean value) {
        super.setFlagAt(1, value);
    }

    protected void writeBody(CDROutputPacket os) {
        readerId.write(os);
        writerId.write(os);
        sns.write(os);
        count.write(os);
    }
}
