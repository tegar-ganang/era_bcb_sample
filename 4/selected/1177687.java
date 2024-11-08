package com.peterhi.net.messages;

import com.peterhi.io.IO;
import com.peterhi.net.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author YUN TAO
 */
public class EnterChannelMessage extends Message {

    private String channelName;

    public EnterChannelMessage(String channelName) {
        this.channelName = channelName;
    }

    public EnterChannelMessage(byte[] data, int offset) throws IOException {
        deserialize(data, offset);
    }

    public String getChannelName() {
        return channelName;
    }

    @Override
    public short getType() {
        return Protocol.ENTER_CHANNEL_MESSAGE;
    }

    @Override
    protected void writeData(DataOutputStream out) throws IOException {
        IO.writeString(out, channelName);
    }

    @Override
    protected void readData(DataInputStream in) throws IOException {
        channelName = IO.readString(in);
    }

    @Override
    public String toString() {
        return "enter " + channelName;
    }
}
