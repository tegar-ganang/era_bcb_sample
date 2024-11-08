package com.peterhi.net.msg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import com.peterhi.io.IO;

public class EChnlMsg extends AbstractMSG {

    private String channelName;

    public EChnlMsg() {
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    @Override
    protected void readData(DataInputStream in) throws IOException {
        channelName = IO.readString(in);
    }

    @Override
    protected void writeData(DataOutputStream out) throws IOException {
        IO.writeString(out, channelName);
    }

    public int getID() {
        return MSG_ECHNL;
    }
}
