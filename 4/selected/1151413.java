package rtjdds.rtps.receive;

import rtjdds.rtps.messages.AckNack;
import rtjdds.rtps.messages.Data;
import rtjdds.rtps.messages.DataFrag;
import rtjdds.rtps.messages.Gap;
import rtjdds.rtps.messages.HeartBeat;
import rtjdds.rtps.messages.HeartBeatFrag;
import rtjdds.rtps.messages.InfoTimestamp;
import rtjdds.rtps.messages.MalformedSubmessageException;
import rtjdds.rtps.messages.NackFrag;
import rtjdds.rtps.messages.NoKeyData;
import rtjdds.rtps.messages.NoKeyDataFrag;
import rtjdds.rtps.messages.Pad;
import rtjdds.rtps.messages.RTPSHeader;
import rtjdds.rtps.messages.Submessage;
import rtjdds.rtps.messages.elements.Count;
import rtjdds.rtps.messages.elements.EntityId;
import rtjdds.rtps.messages.elements.FragmentNumber;
import rtjdds.rtps.messages.elements.FragmentNumberSet;
import rtjdds.rtps.messages.elements.GuidPrefix;
import rtjdds.rtps.messages.elements.KeyHashPrefix;
import rtjdds.rtps.messages.elements.KeyHashSuffix;
import rtjdds.rtps.messages.elements.LongWrapperSubmessageElement;
import rtjdds.rtps.messages.elements.ParameterList;
import rtjdds.rtps.messages.elements.ProtocolId;
import rtjdds.rtps.messages.elements.ProtocolVersion;
import rtjdds.rtps.messages.elements.SequenceNumber;
import rtjdds.rtps.messages.elements.SequenceNumberSet;
import rtjdds.rtps.messages.elements.SerializedData;
import rtjdds.rtps.messages.elements.ShortWrapperSubmessageElement;
import rtjdds.rtps.messages.elements.StatusInfo;
import rtjdds.rtps.messages.elements.SubmessageElement;
import rtjdds.rtps.messages.elements.Timestamp;
import rtjdds.rtps.messages.elements.VendorId;
import rtjdds.rtps.portable.InputPacket;
import rtjdds.rtps.types.ACKNACK;
import rtjdds.rtps.types.Count_tHelper;
import rtjdds.rtps.types.DATA;
import rtjdds.rtps.types.DATA_FRAG;
import rtjdds.rtps.types.EntityId_tHelper;
import rtjdds.rtps.types.FragmentNumber_tHelper;
import rtjdds.rtps.types.GAP;
import rtjdds.rtps.types.GuidPrefix_tHelper;
import rtjdds.rtps.types.HEARTBEAT;
import rtjdds.rtps.types.HEARTBEAT_FRAG;
import rtjdds.rtps.types.INFO_DST;
import rtjdds.rtps.types.INFO_REPLY;
import rtjdds.rtps.types.INFO_SRC;
import rtjdds.rtps.types.INFO_TS;
import rtjdds.rtps.types.KeyHashPrefix_tHelper;
import rtjdds.rtps.types.KeyHashSuffix_tHelper;
import rtjdds.rtps.types.NACK_FRAG;
import rtjdds.rtps.types.NOKEY_DATA;
import rtjdds.rtps.types.NOKEY_DATA_FRAG;
import rtjdds.rtps.types.PAD;
import rtjdds.rtps.types.ProtocolId_tHelper;
import rtjdds.rtps.types.ProtocolVersion_tHelper;
import rtjdds.rtps.types.SequenceNumber_tHelper;
import rtjdds.rtps.types.Time_tHelper;
import rtjdds.rtps.types.VendorId_tHelper;
import rtjdds.util.BitUtility;
import rtjdds.util.GlobalProperties;
import rtjdds.util.Logger;

/**
 * Concrete message decoding class according to CDR encapsulation.
 * 
 * @author kerush
 *
 */
public class CDRMessageProcessor extends MessageProcessor {

    private InputPacket packet = null;

    private byte kind;

    private byte flags;

    private short submessageLength;

    private int nextSubmessageHeader;

    /**
	 * Constructs a new <code>Receiver</code> of <code>Messages</code>.<br/>
	 * It operates following these rules:
	 * <ol>
	 * 	<li>If the FULL Submessage Header cannot be read, the packet is dropped</li>
	 *	<li>If the <code>submessageLength</code> is invalid, the rest of the message is invalid</li>
	 *	<li>A <code>Submessage</code> with unknown <code>SubmessageKind</code> MUST be ignored and
	 *	parsing should continue with the next Submessage. This point enables vendor-specific
	 * 	SubmessageKinds</li>
	 * <li>Unknown flags should be ignored</li>
	 * <li>A valid <code>submessageLength</code> field MUST always be used to find the next message,
	 * even for Submessages with known Kind</li>
	 * <li>A known but invalid Submessage invalidates the rest of the message</li>
	 * </ol>
	 * 
	 * 
	 */
    public CDRMessageProcessor(SubmessageDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
	 * Decodes the packet following the CDR representation.<br/>
	 * Once a submessage is decoded, it is passed to the dispatcher that notifies the
	 * respective <code>Reader</code> or <code>Writer</code>. 
	 * <ol>
	 * 	<li>If the FULL Submessage Header cannot be read, the packet is dropped</li>
	 *	<li>If the <code>submessageLength</code> is invalid, the rest of the message is invalid</li>
	 *	<li>A <code>Submessage</code> with unknown <code>SubmessageKind</code> MUST be ignored and
	 *	parsing should continue with the next Submessage. This point enables vendor-specific
	 * 	SubmessageKinds</li>
	 * <li>Unknown flags should be ignored</li>
	 * <li>A valid <code>submessageLength</code> field MUST always be used to find the next message,
	 * even for Submessages with known Kind</li>
	 * <li>A known but invalid Submessage invalidates the rest of the message</li>
	 * </ol>
	 * <!--
	 * <b>performance info: this method is SYNCHRONIZED, IT PUTS A LOCK ON THE ENTIRE OBJECT</b>
	 * -->
	 * 
	 */
    public void process(InputPacket packet) {
        this.packet = packet;
        ProtocolVersion sourceVersion = null;
        VendorId sourceVendorId = null;
        GuidPrefix sourceGuidPrefix = null;
        GuidPrefix destGuidPrefix = null;
        Timestamp ts = null;
        logInfo("decode()", "Decoding packet of " + packet.getLength() + " bytes");
        RTPSHeader header = null;
        try {
            header = decodeRTPSHeader();
        } catch (MalformedSubmessageException e1) {
            logWarn("decode()", e1.getMessage());
            return;
        }
        if (header.getVersion().compareTo(GlobalProperties.protocolVersion) > 0) {
            logWarn("decode()", "received a message with incompatible version, message will be dropped...");
            return;
        }
        if (!header.getVendorId().equals(GlobalProperties.vendorId)) {
            logWarn("decode()", "messages from other vendors are not supported, message will be dropped...");
            return;
        }
        sourceVersion = header.getVersion();
        sourceVendorId = header.getVendorId();
        sourceGuidPrefix = header.getGuidPrefix();
        do {
            int submessageHeaderStart = packet.getCursorPosition();
            int submessageHeaderStop = submessageHeaderStart + Submessage.HEADER_SIZE;
            if (packet.getCursorPosition() + Submessage.HEADER_SIZE > packet.getLength()) {
                logWarn("decode()", "PACKET TOO SHORT, cannot read the submessage header");
                return;
            }
            kind = packet.read_octet();
            flags = packet.read_octet();
            packet.setEndianess(!BitUtility.getFlagAt(flags, 0));
            submessageLength = packet.read_short();
            nextSubmessageHeader = submessageHeaderStop + submessageLength;
            if ((submessageLength % 4) != 0) {
                logWarn("decode()", "Submessage starting at " + packet.getCursorPosition() + ": Length not aligned: value=" + submessageLength + ", dropping the rest of the message");
                return;
            }
            if (submessageLength <= 0) {
                logWarn("decode()", "Submessage Length cannot be a non-positive value (" + submessageLength + "), " + "dropping the rest of the message");
                return;
            }
            if (nextSubmessageHeader > packet.getLength()) {
                logWarn("decode()", "Packet too short, cannot read the next submessage, dropping the rest of the message");
                return;
            }
            try {
                switch(kind) {
                    case DATA.value:
                        {
                            logInfo("decode()", "Starting decoding a DATA submessage at byte " + (packet.getCursorPosition() - 1));
                            Data data = decodeData();
                            data.setTimestamp(ts);
                            data.setSrcGuidPrefix(sourceGuidPrefix);
                            _dispatcher.dispatch(data);
                            break;
                        }
                    case DATA_FRAG.value:
                        {
                            logInfo("decode()", "Starting decoding a DATA_FRAG submessage at byte " + (packet.getCursorPosition() - 1));
                            DataFrag dataFrag = decodeDataFrag();
                            dataFrag.setTimestamp(ts);
                            dataFrag.setSrcGuidPrefix(sourceGuidPrefix);
                            _dispatcher.dispatch(dataFrag);
                            break;
                        }
                    case NOKEY_DATA.value:
                        {
                            logInfo("decode()", "Starting decoding a NOKEY_DATA submessage at byte " + (packet.getCursorPosition() - 1));
                            NoKeyData data = decodeNoKeyData();
                            data.setTimestamp(ts);
                            data.setSrcGuidPrefix(sourceGuidPrefix);
                            _dispatcher.dispatch(data);
                            break;
                        }
                    case NOKEY_DATA_FRAG.value:
                        {
                            logInfo("decode()", "Starting decoding a NOKEY_DATA_FRAG submessage at byte " + (packet.getCursorPosition() - 1));
                            NoKeyDataFrag dataFrag = decodeNoKeyDataFrag();
                            dataFrag.setTimestamp(ts);
                            dataFrag.setSrcGuidPrefix(sourceGuidPrefix);
                            _dispatcher.dispatch(dataFrag);
                            break;
                        }
                    case HEARTBEAT.value:
                        {
                            logInfo("decode()", "Starting decoding a HEARTBEAT submessage at byte " + (packet.getCursorPosition() - 1));
                            HeartBeat h = decodeHeartBeat();
                            h.setTimestamp(ts);
                            h.setSrcGuidPrefix(sourceGuidPrefix);
                            _dispatcher.dispatch(h);
                            break;
                        }
                    case ACKNACK.value:
                        {
                            logInfo("decode()", "Starting decoding a ACKNACK submessage at byte " + (packet.getCursorPosition() - 1));
                            AckNack a = decodeAckNack();
                            a.setTimestamp(ts);
                            a.setSrcGuidPrefix(sourceGuidPrefix);
                            _dispatcher.dispatch(a);
                            break;
                        }
                    case HEARTBEAT_FRAG.value:
                        {
                            logInfo("decode()", "Starting decoding a HEARTBEAT_FRAG submessage at byte " + (packet.getCursorPosition() - 1));
                            HeartBeatFrag h = decodeHeartBeatFrag();
                            h.setTimestamp(ts);
                            h.setSrcGuidPrefix(sourceGuidPrefix);
                            _dispatcher.dispatch(h);
                            break;
                        }
                    case NACK_FRAG.value:
                        {
                            logInfo("decode()", "Starting decoding a NACK_FRAG submessage at byte " + (packet.getCursorPosition() - 1));
                            NackFrag n = decodeNackFrag();
                            n.setTimestamp(ts);
                            n.setSrcGuidPrefix(sourceGuidPrefix);
                            _dispatcher.dispatch(n);
                            break;
                        }
                    case GAP.value:
                        {
                            logInfo("decode()", "Starting decoding a GAP submessage at byte " + (packet.getCursorPosition() - 1));
                            Gap g = decodeGap();
                            g.setSrcGuidPrefix(sourceGuidPrefix);
                            g.setTimestamp(ts);
                            _dispatcher.dispatch(g);
                            break;
                        }
                    case PAD.value:
                        {
                            logInfo("decode()", "Starting decoding a PAD submessage at byte " + (packet.getCursorPosition() - 1));
                            break;
                        }
                    case INFO_TS.value:
                        {
                            logInfo("decode()", "Starting decoding a INFO_TS submessage at byte " + (packet.getCursorPosition() - 1));
                            ts = decodeInfoTimestamp().getT();
                            break;
                        }
                    case INFO_REPLY.value:
                        {
                            logInfo("decode()", "Starting decoding a INFO_REPLY submessage at byte " + (packet.getCursorPosition() - 1));
                            logInfo("decode()", "Submessage not supported, skipping...");
                            break;
                        }
                    case INFO_DST.value:
                        {
                            logInfo("decode()", "Starting decoding a INFO_DST submessage at byte " + (packet.getCursorPosition() - 1));
                            logInfo("decode()", "Submessage not supported, skipping...");
                            break;
                        }
                    case INFO_SRC.value:
                        {
                            logInfo("decode()", "Starting decoding a INFO_SRC submessage at byte " + (packet.getCursorPosition() - 1));
                            logInfo("decode()", "Submessage not supported, skipping...");
                            break;
                        }
                    default:
                        {
                            logWarn("decode()", "Unknown Submessage Kind=" + kind + " at byte " + (packet.getCursorPosition() - 1) + ", skipping the submessage");
                            break;
                        }
                }
            } catch (MalformedSubmessageException e) {
                logWarn("decode()", "Malformed Submessage Error:" + e.toString() + ", dropping the rest of the message");
                return;
            }
            packet.setCursorPosition(nextSubmessageHeader);
        } while (packet.getCursorPosition() < packet.getLength());
        logInfo("decode()", "Decoding process terminated correctly");
    }

    private RTPSHeader decodeRTPSHeader() throws MalformedSubmessageException {
        ProtocolId protocol = null;
        ProtocolVersion version = null;
        VendorId vendorId = null;
        GuidPrefix guidPrefix = null;
        if (packet.getLength() >= SubmessageElement.RTPS_HEADER_SIZE) {
            protocol = new ProtocolId(ProtocolId_tHelper.read(packet));
            if (protocol.equals(GlobalProperties.protocolId)) {
                version = new ProtocolVersion(ProtocolVersion_tHelper.read(packet));
                vendorId = new VendorId(VendorId_tHelper.read(packet));
                guidPrefix = new GuidPrefix(GuidPrefix_tHelper.read(packet));
            } else {
                throw new MalformedSubmessageException("RTPS Protocol string not correct");
            }
        } else {
            throw new MalformedSubmessageException("RTPS Header non found in the message");
        }
        logInfo("decodeRTPSHeader()", "" + "Received RTPS message: " + "GUID_PREFIX=" + guidPrefix + " " + "VENDOR_ID=" + vendorId + " " + "VERSION=" + version);
        return new RTPSHeader(protocol, version, vendorId, guidPrefix);
    }

    private Data decodeData() throws MalformedSubmessageException {
        EntityId readerId = new EntityId(EntityId_tHelper.read(packet));
        EntityId writerId = new EntityId(EntityId_tHelper.read(packet));
        SequenceNumber sn = new SequenceNumber(SequenceNumber_tHelper.read(packet));
        KeyHashPrefix khp = null;
        KeyHashSuffix khs = null;
        byte[] bytes = null;
        if (BitUtility.getFlagAt(flags, 3)) {
            khp = new KeyHashPrefix(KeyHashPrefix_tHelper.read(packet));
            khs = new KeyHashSuffix(KeyHashSuffix_tHelper.read(packet));
        }
        StatusInfo si = null;
        if (BitUtility.getFlagAt(flags, 4)) {
            si = new StatusInfo(packet.read_long());
        }
        ParameterList qos = null;
        if (BitUtility.getFlagAt(flags, 1)) {
            qos = new ParameterList(packet);
        }
        SerializedData serializedData = null;
        if (BitUtility.getFlagAt(flags, 2)) {
            int dataLength = nextSubmessageHeader - packet.getCursorPosition();
            serializedData = new SerializedData(packet, dataLength);
        }
        logInfo("decodeData()", "" + "Decoded DATA submessage SN=" + sn.getLongValue() + " CONTENT=" + serializedData);
        return new Data(readerId, writerId, sn, khp, khs, si, qos, serializedData);
    }

    private DataFrag decodeDataFrag() throws MalformedSubmessageException {
        EntityId readerId = new EntityId(EntityId_tHelper.read(packet));
        EntityId writerId = new EntityId(EntityId_tHelper.read(packet));
        SequenceNumber sn = new SequenceNumber(SequenceNumber_tHelper.read(packet));
        KeyHashPrefix khp = null;
        KeyHashSuffix khs = null;
        if (BitUtility.getFlagAt(flags, 2)) {
            khp = new KeyHashPrefix(KeyHashPrefix_tHelper.read(packet));
            khs = new KeyHashSuffix(KeyHashSuffix_tHelper.read(packet));
        }
        ParameterList qos = null;
        if (BitUtility.getFlagAt(flags, 1)) {
            qos = new ParameterList(packet);
        }
        FragmentNumber fsn = new FragmentNumber(FragmentNumber_tHelper.read(packet));
        ShortWrapperSubmessageElement fis = new ShortWrapperSubmessageElement(packet.read_short());
        ShortWrapperSubmessageElement fsize = new ShortWrapperSubmessageElement(packet.read_short());
        LongWrapperSubmessageElement sampleSize = new LongWrapperSubmessageElement(packet.read_long());
        SerializedData serializedData = new SerializedData(packet, nextSubmessageHeader - packet.getCursorPosition());
        logInfo("decodeDataFrag()", "" + "Received DataFrag submessage SN=" + sn.getLongValue());
        return new DataFrag(readerId, writerId, sn, khp, khs, qos, fsn, fis, fsize, sampleSize, serializedData);
    }

    private NoKeyData decodeNoKeyData() throws MalformedSubmessageException {
        EntityId readerId = new EntityId(EntityId_tHelper.read(packet));
        EntityId writerId = new EntityId(EntityId_tHelper.read(packet));
        SequenceNumber sn = new SequenceNumber(SequenceNumber_tHelper.read(packet));
        ParameterList inlineQoS = null;
        if (BitUtility.getFlagAt(flags, 1)) {
            inlineQoS = new ParameterList(packet);
        }
        SerializedData serializedData = null;
        if (BitUtility.getFlagAt(flags, 2)) {
            serializedData = new SerializedData(packet, nextSubmessageHeader - packet.getCursorPosition());
        }
        logInfo("decodeNoKeyData()", "" + "Received NoKeyData submessage SN=" + sn.getLongValue());
        return new NoKeyData(readerId, writerId, sn, inlineQoS, serializedData);
    }

    private NoKeyDataFrag decodeNoKeyDataFrag() throws MalformedSubmessageException {
        EntityId readerId = new EntityId(EntityId_tHelper.read(packet));
        EntityId writerId = new EntityId(EntityId_tHelper.read(packet));
        SequenceNumber sn = new SequenceNumber(SequenceNumber_tHelper.read(packet));
        ParameterList inlineQoS = null;
        if (BitUtility.getFlagAt(flags, 1)) {
            inlineQoS = new ParameterList(packet);
        }
        FragmentNumber fragmentStartingNum = new FragmentNumber(FragmentNumber_tHelper.read(packet));
        short fragmentsInSubmessage = packet.read_short();
        short fragmentSize = packet.read_short();
        int sampleSize = packet.read_long();
        SerializedData serializedData = new SerializedData(packet, nextSubmessageHeader - packet.getCursorPosition());
        logInfo("decodeNoKeyDataFrag()", "" + "Received NoKeyDataFrag submessage SN=" + sn.getLongValue());
        return new NoKeyDataFrag(readerId, writerId, sn, inlineQoS, fragmentStartingNum, fragmentsInSubmessage, fragmentSize, sampleSize, serializedData);
    }

    private HeartBeat decodeHeartBeat() throws MalformedSubmessageException {
        EntityId readerId = new EntityId(EntityId_tHelper.read(packet));
        EntityId writerId = new EntityId(EntityId_tHelper.read(packet));
        SequenceNumber firstSN = new SequenceNumber(SequenceNumber_tHelper.read(packet));
        SequenceNumber lastSN = new SequenceNumber(SequenceNumber_tHelper.read(packet));
        Count count = new Count(Count_tHelper.read(packet));
        logInfo("decodeHeartBeat()", "" + "Received HeartBeat submessage SN=" + firstSN.getLongValue());
        return new HeartBeat(readerId, writerId, firstSN, lastSN, count);
    }

    private AckNack decodeAckNack() throws MalformedSubmessageException {
        EntityId readerId = new EntityId(EntityId_tHelper.read(packet));
        EntityId writerId = new EntityId(EntityId_tHelper.read(packet));
        SequenceNumberSet sns = SequenceNumberSet.read(packet);
        Count count = new Count(Count_tHelper.read(packet));
        logInfo("decodeAckNack()", "" + "Received AckNack submessage");
        return new AckNack(readerId, writerId, sns, count);
    }

    private HeartBeatFrag decodeHeartBeatFrag() throws MalformedSubmessageException {
        EntityId readerId = new EntityId(EntityId_tHelper.read(packet));
        EntityId writerId = new EntityId(EntityId_tHelper.read(packet));
        SequenceNumber writerSN = new SequenceNumber(SequenceNumber_tHelper.read(packet));
        FragmentNumber fn = new FragmentNumber(FragmentNumber_tHelper.read(packet));
        Count count = new Count(Count_tHelper.read(packet));
        logInfo("decodeHeartBeatFrag()", "" + "Received HeartBeatFrag submessage writerSN=" + writerSN.getLongValue());
        return new HeartBeatFrag(readerId, writerId, writerSN, fn, count);
    }

    private NackFrag decodeNackFrag() throws MalformedSubmessageException {
        EntityId readerId = new EntityId(EntityId_tHelper.read(packet));
        EntityId writerId = new EntityId(EntityId_tHelper.read(packet));
        SequenceNumber writerSN = new SequenceNumber(SequenceNumber_tHelper.read(packet));
        FragmentNumberSet fragmentNumberState = FragmentNumberSet.read(packet);
        Count count = new Count(Count_tHelper.read(packet));
        logInfo("decodeNackFrag()", "" + "Received NackFrag submessage writerSN=" + writerSN.getLongValue());
        return new NackFrag(readerId, writerId, writerSN, fragmentNumberState, count);
    }

    private Gap decodeGap() throws MalformedSubmessageException {
        EntityId readerId = new EntityId(EntityId_tHelper.read(packet));
        EntityId writerId = new EntityId(EntityId_tHelper.read(packet));
        SequenceNumber gapStart = new SequenceNumber(SequenceNumber_tHelper.read(packet));
        SequenceNumberSet gapList = SequenceNumberSet.read(packet);
        logInfo("decodeGap()", "" + "Received Gap submessage gapStart=" + gapStart.getLongValue());
        return new Gap(readerId, writerId, gapStart, gapList);
    }

    private Pad decodePad() throws MalformedSubmessageException {
        logInfo("decodePad()", "" + "Received Pad submessage");
        return new Pad();
    }

    private InfoTimestamp decodeInfoTimestamp() throws MalformedSubmessageException {
        Timestamp ts = null;
        if (BitUtility.getFlagAt(flags, 1)) {
            ts = new Timestamp(Time_tHelper.read(packet));
        }
        return new InfoTimestamp(ts);
    }

    private static void logInfo(String method, String msg) {
        GlobalProperties.logger.log(Logger.INFO, CDRMessageProcessor.class, method, msg);
    }

    private static void logWarn(String method, String msg) {
        GlobalProperties.logger.log(Logger.WARN, CDRMessageProcessor.class, method, msg);
    }
}
