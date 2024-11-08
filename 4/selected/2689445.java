package rtjdds.rtps.messages;

import rtjdds.rtps.messages.elements.EntityId;
import rtjdds.rtps.messages.elements.FragmentNumber;
import rtjdds.rtps.messages.elements.ParameterList;
import rtjdds.rtps.messages.elements.SequenceNumber;
import rtjdds.rtps.messages.elements.SerializedData;
import rtjdds.rtps.portable.CDROutputPacket;
import rtjdds.rtps.types.NOKEY_DATA_FRAG;

public class NoKeyDataFrag extends Submessage {

    private EntityId readerId;

    private EntityId writerId;

    private SequenceNumber writerSN;

    private ParameterList inlineQoS;

    private FragmentNumber fragmentStartingNum;

    private short fragmentsInSubmessage;

    private short fragmentSize;

    private int sampleSize;

    private SerializedData serializedData;

    public NoKeyDataFrag(EntityId readerId, EntityId writerId, SequenceNumber writerSN, ParameterList inlineQoS, FragmentNumber fragmentStartingNum, short fragmentsInSubmessage, short fragmentSize, int sampleSize, SerializedData serializedData) {
        super(NOKEY_DATA_FRAG.value);
        this.readerId = readerId;
        this.writerId = writerId;
        this.writerSN = writerSN;
        this.inlineQoS = inlineQoS;
        this.fragmentStartingNum = fragmentStartingNum;
        this.fragmentsInSubmessage = fragmentsInSubmessage;
        this.fragmentSize = fragmentSize;
        this.sampleSize = sampleSize;
        this.serializedData = serializedData;
        if (inlineQoS != null) {
            super.setFlagAt(1, true);
        }
    }

    protected void writeBody(CDROutputPacket os) {
        readerId.write(os);
        writerId.write(os);
        writerSN.write(os);
        if (super.getFlagAt(1)) {
            inlineQoS.write(os);
        }
        fragmentStartingNum.write(os);
        os.write_short(fragmentsInSubmessage);
        os.write_short(fragmentSize);
        os.write_long(sampleSize);
        serializedData.write(os);
    }
}
