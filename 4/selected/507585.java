package org.openremote.controller.protocol.knx.ip.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IpTunnelingAck extends IpMessage {

    public static final int STI = 0x421;

    private int channelId;

    private int seqCounter;

    private int status;

    public IpTunnelingAck(InputStream is, int length) throws IOException {
        super(STI, length);
        is.skip(1);
        this.channelId = is.read();
        this.seqCounter = is.read();
        this.status = is.read();
    }

    public IpTunnelingAck(int channelId, int seqCounter, int status) {
        super(STI, 4);
        this.channelId = channelId;
        this.seqCounter = seqCounter;
        this.status = status;
    }

    public void write(OutputStream os) throws IOException {
        super.write(os);
        os.write(4);
        os.write(this.channelId);
        os.write(this.seqCounter);
        os.write(this.status);
    }

    @Override
    public Primitive getPrimitive() {
        return Primitive.RESP;
    }

    public int getChannelId() {
        return channelId;
    }

    public int getSeqCounter() {
        return seqCounter;
    }

    public int getStatus() {
        return status;
    }
}
