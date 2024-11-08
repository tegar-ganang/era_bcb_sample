package org.openremote.controller.protocol.knx.ip.message;

import java.io.IOException;
import java.io.InputStream;

public class IpConnectionStateResp extends IpMessage {

    public static final int STI = 0x208;

    private int channelId;

    private int status;

    public IpConnectionStateResp(InputStream is, int length) throws IOException {
        super(STI, length);
        this.channelId = is.read();
        this.status = is.read();
    }

    @Override
    public Primitive getPrimitive() {
        return Primitive.RESP;
    }

    public int getChannelId() {
        return this.channelId;
    }

    public int getStatus() {
        return this.status;
    }
}
