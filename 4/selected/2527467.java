package mybridge.core.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import mybridge.core.packet.*;
import xnet.core.server.Session;
import xnet.core.util.IOBuffer;

/**
 * 实现mysql协议的session
 * @author quanwei
 *
 */
public class MyBridgeSession extends Session {

    static Log logger = LogFactory.getLog(MyBridgeSession.class);

    static int READ_HEADER = 0;

    static int READ_BODY = 1;

    MyBridgeProtocal protocal;

    int currentState = READ_HEADER;

    @Override
    public void complateRead(IOBuffer readBuf, IOBuffer writeBuf) throws Exception {
        if (currentState == READ_HEADER) {
            PacketHeader header = new PacketHeader();
            header.putBytes(readBuf.readBytes(0, readBuf.limit()));
            protocal.packetNum = (byte) (header.packetNum + 1);
            readBuf.position(0);
            readBuf.limit(header.packetLen);
            currentState = READ_BODY;
        } else {
            currentState = READ_HEADER;
            protocal.onPacketReceived(readBuf, writeBuf);
        }
    }

    @Override
    public void complateWrite(IOBuffer readBuf, IOBuffer writeBuf) throws Exception {
        protocal.onPacketSended(readBuf, writeBuf);
    }

    @Override
    public void open(IOBuffer readBuf, IOBuffer writeBuf) throws Exception {
        protocal = new MyBridgeProtocal(this);
        protocal.onSessionOpen(readBuf, writeBuf);
    }

    public void close() {
        if (protocal != null) {
            protocal.onSessionClose();
        }
    }
}
