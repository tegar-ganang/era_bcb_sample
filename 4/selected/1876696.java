package rtjdds.rtps.send;

import javax.realtime.LTMemory;
import javax.realtime.MemoryArea;
import javax.realtime.RealtimeThread;
import com.aicas.jamaica.util.Logger;
import rtjdds.rtps.messages.Data;
import rtjdds.rtps.messages.InfoTimestamp;
import rtjdds.rtps.messages.NoKeyData;
import rtjdds.rtps.messages.RTPSHeader;
import rtjdds.rtps.messages.Submessage;
import rtjdds.rtps.messages.elements.EntityId;
import rtjdds.rtps.messages.elements.KeyHashPrefix;
import rtjdds.rtps.messages.elements.KeyHashSuffix;
import rtjdds.rtps.messages.elements.ParameterList;
import rtjdds.rtps.messages.elements.SequenceNumber;
import rtjdds.rtps.messages.elements.SerializedData;
import rtjdds.rtps.messages.elements.StatusInfo;
import rtjdds.rtps.portable.CDROutputPacket;
import rtjdds.rtps.publication.Writer;
import rtjdds.rtps.structure.local.ParticipantImpl;
import rtjdds.rtps.types.DATA;
import rtjdds.rtps.types.INFO_TS;
import rtjdds.rtps.types.NOKEY_DATA;
import rtjdds.rtps.types.SequenceNumber_t;
import rtjdds.util.Executor;
import rtjdds.util.GlobalProperties;

/**
 * Serializes...
 * @author kerush
 *
 */
public class MessageSerializer {

    private Executor _executor = null;

    private Writer _writer = null;

    private CDROutputPacket _packet = null;

    private RTPSHeader _header = null;

    EntityId _writerId = null;

    private EntityId _readerId = null;

    private KeyHashPrefix _prefix = null;

    private KeyHashSuffix _suffix = null;

    private StatusInfo _status = null;

    private ParameterList _parms = null;

    private SerializedData _user_data = null;

    private byte _submessageKind = 0;

    public MessageSerializer(Writer w, CDROutputPacket packet, int scopeSize) {
        GlobalProperties.logger.log(rtjdds.util.Logger.INFO, getClass(), "new()", "Creating a MessageSerializer with " + (scopeSize / 1024) + " Kb buffer");
        _executor = new Executor(scopeSize, new InnerScopeSerializer());
        _writer = w;
        _packet = packet;
        _header = ((ParticipantImpl) _writer.get_publisher().get_participant()).getHeader();
        _writerId = _writer.getGuid().getEntityId();
        GlobalProperties.logger.log(rtjdds.util.Logger.INFO, getClass(), "new()", "Created a MessageSerializer with " + (scopeSize / 1024) + " Kb buffer");
    }

    /**
	 * To be called at the beginning of each serialization
	 *
	 */
    public void writeHeader() {
        _packet.setCursorPosition(0);
        _header.write(_packet);
    }

    public void writeData(EntityId readerId, KeyHashPrefix prefix, KeyHashSuffix suffix, StatusInfo status, ParameterList parms, SerializedData user_data) {
        _readerId = readerId;
        _prefix = prefix;
        _suffix = suffix;
        _status = status;
        _parms = parms;
        _user_data = user_data;
        _submessageKind = DATA.value;
        writeSubmessage();
    }

    public void writeNoKeyData(EntityId readerId, ParameterList parms, SerializedData user_data) {
        _readerId = readerId;
        _parms = parms;
        _user_data = user_data;
        _submessageKind = NOKEY_DATA.value;
        writeSubmessage();
    }

    public void writeInfoTimestamp() {
        _submessageKind = INFO_TS.value;
        writeSubmessage();
    }

    public CDROutputPacket getPacket() {
        return _packet;
    }

    private SequenceNumber getSN() {
        return new SequenceNumber(new SequenceNumber_t(_writer.getNextSequenceNumber()));
    }

    private void writeSubmessage() {
        _executor.execute();
    }

    class InnerScopeSerializer implements Runnable {

        public void run() {
            GlobalProperties.logger.log(rtjdds.util.Logger.INFO, getClass(), "writeSubmessage()", "Start serializing...");
            GlobalProperties.logger.printMemStats();
            Submessage submsg = null;
            switch(_submessageKind) {
                case DATA.value:
                    submsg = new Data(_readerId, _writerId, getSN(), _prefix, _suffix, _status, _parms, _user_data);
                    break;
                case NOKEY_DATA.value:
                    submsg = new NoKeyData(_readerId, _writerId, getSN(), _parms, _user_data);
                    break;
                case INFO_TS.value:
                    submsg = InfoTimestamp.now();
                    break;
                default:
                    return;
            }
            submsg.write(_packet);
            GlobalProperties.logger.log(rtjdds.util.Logger.INFO, getClass(), "writeSubmessage()", "Ended serializing...");
            GlobalProperties.logger.printMemStats();
        }
    }
}
