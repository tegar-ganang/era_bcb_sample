package org.openremote.controller.protocol.knx.ip.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IpTunnelingReq extends IpMessage {

    public static final int STI = 0x420;

    private int channelId;

    private int seqCounter;

    private byte[] cEmiFrame;

    public IpTunnelingReq(int channelId, int seqCounter, byte[] cEmiFrame) {
        super(STI, 4 + cEmiFrame.length);
        this.channelId = channelId;
        this.seqCounter = seqCounter;
        this.cEmiFrame = cEmiFrame;
    }

    public IpTunnelingReq(InputStream is, int length) throws IOException {
        super(STI, length);
        is.skip(1);
        this.channelId = is.read();
        this.seqCounter = is.read();
        is.skip(1);
        this.cEmiFrame = new byte[length - 4];
        is.read(this.cEmiFrame);
    }

    @Override
    public Primitive getPrimitive() {
        return Primitive.REQ;
    }

    @Override
    public int getSyncSendTimeout() {
        return 10000;
    }

    @Override
    public void write(OutputStream os) throws IOException {
        super.write(os);
        os.write(4);
        os.write(this.channelId);
        os.write(this.seqCounter);
        os.write(0);
        os.write(this.cEmiFrame);
    }

    public int getChannelId() {
        return channelId;
    }

    public int getSeqCounter() {
        return seqCounter;
    }

    public byte[] getcEmiFrame() {
        return cEmiFrame;
    }
}
