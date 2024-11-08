package org.openremote.controller.protocol.knx.ip.message;

import java.io.IOException;
import java.io.InputStream;

public class IpDisconnectResp extends IpMessage {

    public static final int STI = 0x20A;

    private int channelId;

    private int status;

    public IpDisconnectResp(InputStream is, int length) throws IOException {
        super(STI, length);
        this.channelId = is.read();
        this.status = is.read();
    }

    public int getChannelId() {
        return this.channelId;
    }

    public int getStatus() {
        return this.status;
    }

    @Override
    public Primitive getPrimitive() {
        return Primitive.RESP;
    }
}
