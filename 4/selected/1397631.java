package rtjdds.rtps.messages;

import rtjdds.rtps.messages.elements.EntityId;
import rtjdds.rtps.messages.elements.KeyHashPrefix;
import rtjdds.rtps.messages.elements.KeyHashSuffix;
import rtjdds.rtps.messages.elements.ParameterList;
import rtjdds.rtps.messages.elements.SequenceNumber;
import rtjdds.rtps.messages.elements.SerializedData;
import rtjdds.rtps.messages.elements.StatusInfo;
import rtjdds.rtps.portable.CDROutputPacket;
import rtjdds.rtps.types.DATA;

public class Data extends Submessage {

    protected EntityId readerId;

    protected EntityId writerId;

    protected SequenceNumber writerSN;

    protected KeyHashPrefix khp;

    protected KeyHashSuffix khs;

    protected StatusInfo si;

    protected ParameterList inlineQoS;

    protected SerializedData serializedData;

    public Data(EntityId readerId, EntityId writerId, SequenceNumber writerSN, KeyHashPrefix khp, KeyHashSuffix khs, StatusInfo si, ParameterList inlineQoS, SerializedData serializedData) {
        super(DATA.value);
        this.readerId = readerId;
        this.writerId = writerId;
        this.writerSN = writerSN;
        this.khp = khp;
        this.khs = khs;
        this.si = si;
        this.inlineQoS = inlineQoS;
        this.serializedData = serializedData;
        if (khp != null && khs != null) {
            super.setFlagAt(3, true);
        }
        if (si != null) {
            super.setFlagAt(4, true);
        }
        if (inlineQoS != null) {
            super.setFlagAt(1, true);
        }
        if (serializedData != null) {
            super.setFlagAt(2, true);
        }
    }

    /**
	 * @return Returns the inlineQoS.
	 */
    public ParameterList getInlineQoS() {
        return inlineQoS;
    }

    /**
	 * @return Returns the khp.
	 */
    public KeyHashPrefix getKhp() {
        return khp;
    }

    /**
	 * @return Returns the khs.
	 */
    public KeyHashSuffix getKhs() {
        return khs;
    }

    /**
	 * @return Returns the readerId.
	 */
    public EntityId getReaderId() {
        return readerId;
    }

    /**
	 * @return Returns the serializedData.
	 */
    public SerializedData getSerializedData() {
        return serializedData;
    }

    /**
	 * @return Returns the si.
	 */
    public StatusInfo getSi() {
        return si;
    }

    /**
	 * @return Returns the writerId.
	 */
    public EntityId getWriterId() {
        return writerId;
    }

    /**
	 * @return Returns the writerSN.
	 */
    public SequenceNumber getWriterSN() {
        return writerSN;
    }

    protected void writeBody(CDROutputPacket os) {
        this.readerId.write(os);
        this.writerId.write(os);
        this.writerSN.write(os);
        if (super.getFlagAt(3)) {
            this.khp.write(os);
            this.khs.write(os);
            ;
        }
        if (super.getFlagAt(4)) {
            this.si.write(os);
        }
        if (super.getFlagAt(1)) {
            this.inlineQoS.write(os);
        }
        if (super.getFlagAt(2)) {
            this.serializedData.write(os);
        }
    }
}
