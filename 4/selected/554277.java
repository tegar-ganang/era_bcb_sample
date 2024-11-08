package rtjdds.rtps.messages;

import rtjdds.rtps.messages.elements.EntityId;
import rtjdds.rtps.messages.elements.FragmentNumber;
import rtjdds.rtps.messages.elements.KeyHashPrefix;
import rtjdds.rtps.messages.elements.KeyHashSuffix;
import rtjdds.rtps.messages.elements.LongWrapperSubmessageElement;
import rtjdds.rtps.messages.elements.ParameterList;
import rtjdds.rtps.messages.elements.SequenceNumber;
import rtjdds.rtps.messages.elements.SerializedData;
import rtjdds.rtps.messages.elements.ShortWrapperSubmessageElement;
import rtjdds.rtps.portable.CDROutputPacket;
import rtjdds.rtps.types.DATA_FRAG;

public class DataFrag extends Submessage {

    protected EntityId readerId;

    protected EntityId writerId;

    protected SequenceNumber writerSN;

    protected KeyHashPrefix khp;

    protected KeyHashSuffix khs;

    protected ParameterList inlineQoS;

    protected FragmentNumber fsn;

    protected ShortWrapperSubmessageElement fis;

    protected ShortWrapperSubmessageElement fsize;

    protected LongWrapperSubmessageElement sampleSize;

    protected SerializedData serializedData;

    public DataFrag(EntityId readerId, EntityId writerId, SequenceNumber writerSN, KeyHashPrefix khp, KeyHashSuffix khs, ParameterList inlineQoS, FragmentNumber fsn, ShortWrapperSubmessageElement fis, ShortWrapperSubmessageElement fsize, LongWrapperSubmessageElement sampleSize, SerializedData serializedData) {
        super(DATA_FRAG.value);
        this.readerId = readerId;
        this.writerId = writerId;
        this.writerSN = writerSN;
        this.khp = khp;
        this.khs = khs;
        this.inlineQoS = inlineQoS;
        this.fsn = fsn;
        this.fis = fis;
        this.fsize = fsize;
        this.sampleSize = sampleSize;
        this.serializedData = serializedData;
        if (khp != null && khs != null) {
            super.setFlagAt(2, true);
        }
        if (inlineQoS != null) {
            super.setFlagAt(1, true);
        }
    }

    protected void writeBody(CDROutputPacket os) {
        this.readerId.write(os);
        this.writerId.write(os);
        this.writerSN.write(os);
        if (super.getFlagAt(2)) {
            this.khp.write(os);
            this.khs.write(os);
        }
        if (super.getFlagAt(1)) {
            this.inlineQoS.write(os);
        }
        this.fsn.write(os);
        this.fis.write(os);
        this.fsize.write(os);
        this.sampleSize.write(os);
        this.serializedData.write(os);
    }
}
