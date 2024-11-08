package net.jsrb.runtime.impl.gateway;

import static net.jsrb.runtime.impl.endpoint.socket.SocketEndpointProtocol.*;
import java.nio.ByteOrder;
import junit.framework.TestCase;
import net.jsrb.rtl.nbuf;
import static net.jsrb.runtime.impl.gateway.GatewayProtocol.*;

public class TestGatewayProtocol extends TestCase {

    public void testGWCloseTopreq() {
        RequestGWCloseTopreq request = new RequestGWCloseTopreq();
        request.topreqid = 0X12345678;
        nbuf writebuf = nbuf.mallocNative(4096);
        writebuf.order(ByteOrder.BIG_ENDIAN);
        GatewayReadWriter readWriter = new GatewayReadWriter(0);
        readWriter.writeMessage(writebuf, request);
        writebuf.flip();
        nbuf readbuf = nbuf.mallocNative(4096);
        readbuf.order(ByteOrder.BIG_ENDIAN);
        writebuf.copyTo(0, readbuf, 0, writebuf.remaining());
        readbuf.limit(writebuf.limit());
        CommonHeader header = (CommonHeader) readWriter.readMessage(readbuf);
        readWriter.cleanup();
        assertTrue(header.msgtype == (MSGTYPE_REQUEST | MSGTYPE_GWCLOSETOPREQ));
        assertTrue(((RequestGWCloseTopreq) header).topreqid == request.topreqid);
        readbuf.free();
        writebuf.free();
    }

    public void testRequestGWInit() {
        RequestGWInit response = new RequestGWInit();
        response.jsrbId = "JSRB2";
        nbuf writebuf = nbuf.mallocNative(4096);
        writebuf.order(ByteOrder.BIG_ENDIAN);
        GatewayReadWriter readWriter = new GatewayReadWriter(0);
        readWriter.writeMessage(writebuf, response);
        writebuf.flip();
        nbuf readbuf = nbuf.mallocNative(4096);
        readbuf.order(ByteOrder.BIG_ENDIAN);
        writebuf.copyTo(0, readbuf, 0, writebuf.remaining());
        readbuf.limit(writebuf.limit());
        CommonHeader header = (CommonHeader) readWriter.readMessage(readbuf);
        readWriter.cleanup();
        assertTrue(header.msgtype == (MSGTYPE_REQUEST | MSGTYPE_GWINIT));
        assertTrue(((RequestGWInit) header).jsrbId.equals(response.jsrbId));
        readbuf.free();
        writebuf.free();
    }

    public void testResponseGWInit() {
        ResponseGWInit response = new ResponseGWInit();
        response.jsrbId = "JSRB1";
        nbuf writebuf = nbuf.mallocNative(4096);
        writebuf.order(ByteOrder.BIG_ENDIAN);
        GatewayReadWriter readWriter = new GatewayReadWriter(0);
        readWriter.writeMessage(writebuf, response);
        writebuf.flip();
        nbuf readbuf = nbuf.mallocNative(4096);
        readbuf.order(ByteOrder.BIG_ENDIAN);
        writebuf.copyTo(0, readbuf, 0, writebuf.remaining());
        readbuf.limit(writebuf.limit());
        CommonHeader header = (CommonHeader) readWriter.readMessage(readbuf);
        readWriter.cleanup();
        assertTrue(header.msgtype == (MSGTYPE_RESPONSE | MSGTYPE_GWINIT));
        assertTrue(((ResponseGWInit) header).jsrbId.equals(response.jsrbId));
        readbuf.free();
        writebuf.free();
    }
}
