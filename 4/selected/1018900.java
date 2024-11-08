package com.net.sfsimpl.server;

import java.io.Serializable;
import com.cell.net.io.ExternalizableMessage;
import com.cell.net.io.MessageHeader;

public final class SFSProtocol implements com.net.Protocol {

    /**消息类型*/
    byte Protocol;

    /**匹配Request和Response的值，如果为0，则代表为Notify*/
    int PacketNumber = 0;

    /**频道ID<br>
	 * 仅PROTOCOL_CHANNEL_*类型的消息有效*/
    int ChannelID;

    MessageHeader Message;

    transient long DynamicSendTime;

    /**接收时间*/
    transient long DynamicReceiveTime;

    public SFSProtocol() {
    }

    @Override
    public byte getProtocol() {
        return Protocol;
    }

    @Override
    public int getPacketNumber() {
        return PacketNumber;
    }

    @Override
    public int getChannelID() {
        return ChannelID;
    }

    @Override
    public long getSentTime() {
        return DynamicSendTime;
    }

    @Override
    public long getReceivedTime() {
        return DynamicReceiveTime;
    }

    @Override
    public MessageHeader getMessage() {
        return Message;
    }

    @Override
    public String toString() {
        return "[0x" + Integer.toHexString(Protocol) + "] : " + Message;
    }
}
