package net.jsrb.runtime.impl.endpoint.socket;

import java.io.*;
import java.nio.ByteOrder;
import net.jsrb.client.socket.SocketProtocol;
import net.jsrb.rtl.nbuf;
import net.jsrb.runtime.impl.endpoint.socket.SocketEndpointProtocol;
import net.jsrb.runtime.impl.endpoint.socket.SocketEndpointReadWriter;
import net.jsrb.util.ByteArrayBuffer;
import static net.jsrb.runtime.impl.endpoint.socket.SocketEndpointProtocol.*;
import junit.framework.TestCase;

public class TestSocketEndppintProtocol extends TestCase {

    public void testRequestSessInit() throws Exception {
        RequestSessInit req = new RequestSessInit();
        req.cntrole = SocketEndpointProtocol.CONN_TYPE_CNT;
        req.userid = "test";
        req.passwd = "test";
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        SocketProtocol.write(baos, req);
        byte[] data = baos.toByteArray();
        nbuf buf = nbuf.mallocNative(4096);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putBytes(data);
        buf.flip();
        SocketEndpointReadWriter readWriter = new SocketEndpointReadWriter(0);
        CommonHeader header = (CommonHeader) readWriter.readMessage(buf);
        readWriter.cleanup();
        assertTrue(header.msgtype == (MSGTYPE_REQUEST | MSGTYPE_SESSINIT));
        buf.free();
    }

    public void testRequestSvcCall() throws Exception {
        String str1 = "01234576789afdsadsfasdfk;lsadf";
        RequestSvcCall req = new RequestSvcCall();
        req.service = "TOUPPER";
        req.callMode = CALLMODE_SYNC;
        req.requestData = new ByteArrayBuffer(4096);
        req.requestData.putBytes(str1.getBytes()).flip();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        SocketProtocol.write(baos, req);
        byte[] rawdata = baos.toByteArray();
        nbuf buf = nbuf.mallocNative(4096);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putBytes(rawdata);
        buf.flip();
        SocketEndpointReadWriter readWriter = new SocketEndpointReadWriter(0);
        CommonHeader header = (CommonHeader) readWriter.readMessage(buf);
        readWriter.cleanup();
        assertTrue(header.msgtype == (MSGTYPE_REQUEST | MSGTYPE_SVCCALL));
        buf.free();
    }

    public void testRequestFetchReply() throws Exception {
        RequestFetchReply request = new RequestFetchReply();
        request.asyncKey = 10000;
        request.timeout = 50000;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        SocketProtocol.write(baos, request);
        byte[] rawdata = baos.toByteArray();
        nbuf buf = nbuf.mallocNative(4096);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putBytes(rawdata);
        buf.flip();
        SocketEndpointReadWriter readWriter = new SocketEndpointReadWriter(0);
        CommonHeader header = (CommonHeader) readWriter.readMessage(buf);
        readWriter.cleanup();
        assertTrue(header.msgtype == (MSGTYPE_REQUEST | MSGTYPE_FETCHREPLY));
        buf.free();
    }

    public void testResponseSvcCall() throws Exception {
        ResponseSvcCall response = new ResponseSvcCall();
        response.asyncKey = 50000;
        response.statusCode = 0;
        response.serviceCode = 1000;
        response.responseData = (new ByteArrayBuffer(4096)).putBytes("AAAXXXVVVVDDDD".getBytes()).flip();
        ;
        nbuf buf = nbuf.mallocNative(4096);
        buf.order(ByteOrder.BIG_ENDIAN);
        SocketEndpointReadWriter readWriter = new SocketEndpointReadWriter(0);
        readWriter.writeMessage(buf, response);
        readWriter.cleanup();
        buf.flip();
        byte[] rawdata = new byte[buf.remaining()];
        buf.getBytes(rawdata, 0, rawdata.length);
        ByteArrayInputStream bais = new ByteArrayInputStream(rawdata);
        CommonHeader header = SocketProtocol.read(new DataInputStream(bais));
        assertTrue(header.msgtype == (MSGTYPE_RESPONSE | MSGTYPE_SVCCALL));
        buf.free();
    }

    public void testResponseFetchReply() throws Exception {
        ResponseFetchReply response = new ResponseFetchReply();
        response.asyncKey = 50000;
        response.statusCode = 1000;
        response.responseData = (new ByteArrayBuffer(4096)).putBytes("AAAXXXVVVVDDDD".getBytes()).flip();
        ;
        nbuf buf = nbuf.mallocNative(4096);
        buf.order(ByteOrder.BIG_ENDIAN);
        SocketEndpointReadWriter readWriter = new SocketEndpointReadWriter(0);
        readWriter.writeMessage(buf, response);
        readWriter.cleanup();
        buf.flip();
        byte[] rawdata = new byte[buf.remaining()];
        buf.getBytes(rawdata, 0, rawdata.length);
        ByteArrayInputStream bais = new ByteArrayInputStream(rawdata);
        CommonHeader header = SocketProtocol.read(new DataInputStream(bais));
        assertTrue(header.msgtype == (MSGTYPE_RESPONSE | MSGTYPE_FETCHREPLY));
        buf.free();
    }
}
