package com.peterhi.net.messages;

import com.peterhi.StatusCode;
import com.peterhi.net.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author YUN TAO
 */
public class EnterChannelResponse extends Message {

    private int channelId;

    private StatusCode statusCode;

    public EnterChannelResponse(int channelId, StatusCode statusCode) {
        this.channelId = channelId;
        this.statusCode = statusCode;
    }

    public EnterChannelResponse(byte[] data, int offset) throws IOException {
        deserialize(data, offset);
    }

    public int getChannelId() {
        return channelId;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    @Override
    public short getType() {
        return Protocol.ENTER_CHANNEL_RESPONSE;
    }

    @Override
    protected void writeData(DataOutputStream out) throws IOException {
        out.writeInt(channelId);
        if (statusCode == null) {
            out.writeInt(StatusCode.Unknown.ordinal());
        } else {
            out.writeInt(statusCode.ordinal());
        }
    }

    @Override
    protected void readData(DataInputStream in) throws IOException {
        channelId = in.readInt();
        statusCode = StatusCode.toStatusCode(in.readInt());
    }

    @Override
    public String toString() {
        return statusCode + "[" + channelId + "]";
    }
}
