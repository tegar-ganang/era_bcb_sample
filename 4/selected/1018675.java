package com.net.minaimpl;

import com.cell.net.io.MessageHeader;

public final class NetPackageProtocol {

    public static final int PACKAGE_DEFAULT_SIZE = 2 * 1024;

    public static final String CHAR_SET = "UTF-8";

    public static final short PROTOCOL_HART_BEAT = 0x3000;

    public static final short PROTOCOL_SYSTEM_MESSAGE = 0x3001;

    public static final short PROTOCOL_SESSION_MESSAGE = 0x3030;

    public static final short PROTOCOL_CHANNEL_JOIN_S2C = 0x3050;

    public static final short PROTOCOL_CHANNEL_LEAVE_S2C = 0x3051;

    public static final short PROTOCOL_CHANNEL_MESSAGE = 0x3052;

    public static final int FixedHeaderSize = 4 + 4 + 2 + 2 + 8 + 4 + 2 + 2 + 4 + 2;

    static byte[] MagicStart = new byte[] { 2, 0, 0, 6 };

    protected int Flags;

    protected short Dummy;

    protected int Size;

    protected long SesseionID;

    protected int PacketID = 0;

    protected short TotalPieces = 1;

    protected short PieceIndex = 0;

    protected int ChannelID;

    protected short Protocol;

    protected MessageHeader Message;

    public static void setMagicStart(byte[] start) {
        if (start.length == MagicStart.length) {
            MagicStart = start;
        }
    }

    static long HartBeatInterval = 10000;

    public static void setHartBeatInterval(long interval) {
        HartBeatInterval = interval;
    }

    NetPackageProtocol() {
    }

    public int getChannelID() {
        return ChannelID;
    }

    public int getMessageSize() {
        return Size - FixedHeaderSize;
    }

    public MessageHeader getMessage() {
        return Message;
    }

    public short getProtocol() {
        return Protocol;
    }

    public String toString() {
        return "NetPackageProtocol" + " Size=" + Size + " Protocol=0x" + Integer.toString(Protocol, 16) + " Message=\"" + Message + "\"" + "";
    }

    public static final NetPackageProtocol createHartBeat() {
        NetPackageProtocol ret = new NetPackageProtocol();
        ret.Protocol = PROTOCOL_HART_BEAT;
        return ret;
    }

    public static final NetPackageProtocol createChannelJoin(int channelID) {
        NetPackageProtocol ret = new NetPackageProtocol();
        ret.Protocol = PROTOCOL_CHANNEL_JOIN_S2C;
        ret.ChannelID = channelID;
        ret.Message = null;
        return ret;
    }

    public static final NetPackageProtocol createChannelLeave(int channelID) {
        NetPackageProtocol ret = new NetPackageProtocol();
        ret.Protocol = PROTOCOL_CHANNEL_LEAVE_S2C;
        ret.ChannelID = channelID;
        ret.Message = null;
        return ret;
    }

    public static final NetPackageProtocol createChannelMessage(int channelID, MessageHeader message) {
        NetPackageProtocol ret = new NetPackageProtocol();
        ret.Protocol = PROTOCOL_CHANNEL_MESSAGE;
        ret.ChannelID = channelID;
        ret.Message = message;
        return ret;
    }

    public static final NetPackageProtocol createSessionMessage(MessageHeader message) {
        NetPackageProtocol ret = new NetPackageProtocol();
        ret.Protocol = PROTOCOL_SESSION_MESSAGE;
        ret.Message = message;
        return ret;
    }

    public static final NetPackageProtocol createSystemMessage(MessageHeader message) {
        NetPackageProtocol ret = new NetPackageProtocol();
        ret.Protocol = PROTOCOL_SYSTEM_MESSAGE;
        ret.Message = message;
        return ret;
    }
}
