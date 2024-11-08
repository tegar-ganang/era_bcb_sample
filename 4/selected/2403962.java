package org.openremote.controller.protocol.knx.ip.message;

import java.io.IOException;
import java.io.InputStream;

public class IpConnectResp extends IpMessage {

    public static final int STI = 0x206;

    private int channelId;

    private int status;

    private Hpai dataEndpoint;

    public IpConnectResp(InputStream is, int length) throws IOException {
        super(STI, length);
        this.channelId = is.read();
        this.status = is.read();
        this.dataEndpoint = new Hpai(is);
        is.skip(4);
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

    public Hpai getDataEndpoint() {
        return this.dataEndpoint;
    }
}
