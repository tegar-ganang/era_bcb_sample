package org.red5.server.net.mrtmp;

import java.util.Map;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.red5.server.api.IConnection.Encoding;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandler;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.message.StreamAction;
import org.red5.server.service.Call;

public class EdgeRTMPHandler extends RTMPHandler {

    private IMRTMPManager mrtmpManager;

    public void setMRTMPManager(IMRTMPManager mrtmpManager) {
        this.mrtmpManager = mrtmpManager;
    }

    @Override
    public void messageReceived(Object in, IoSession session) throws Exception {
        RTMPConnection conn = (RTMPConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY);
        RTMP state = (RTMP) session.getAttribute(ProtocolState.SESSION_KEY);
        IRTMPEvent message = null;
        final Packet packet = (Packet) in;
        message = packet.getMessage();
        final Header header = packet.getHeader();
        final Channel channel = conn.getChannel(header.getChannelId());
        conn.messageReceived();
        if (header.getDataType() == TYPE_BYTES_READ) {
            onStreamBytesRead(conn, channel, header, (BytesRead) message);
        }
        if (header.getDataType() == TYPE_INVOKE) {
            final IServiceCall call = ((Invoke) message).getCall();
            final String action = call.getServiceMethodName();
            if (call.getServiceName() == null && !conn.isConnected() && StreamAction.valueOf(action).equals(StreamAction.CONNECT)) {
                handleConnect(conn, channel, header, (Invoke) message, (RTMP) state);
                return;
            }
        }
        switch(header.getDataType()) {
            case TYPE_CHUNK_SIZE:
            case TYPE_INVOKE:
            case TYPE_FLEX_MESSAGE:
            case TYPE_NOTIFY:
            case TYPE_AUDIO_DATA:
            case TYPE_VIDEO_DATA:
            case TYPE_FLEX_SHARED_OBJECT:
            case TYPE_FLEX_STREAM_SEND:
            case TYPE_SHARED_OBJECT:
            case TYPE_BYTES_READ:
                forwardPacket(conn, packet);
                break;
            case TYPE_PING:
                onPing(conn, channel, header, (Ping) message);
                break;
            default:
                if (log.isDebugEnabled()) {
                    log.debug("Unknown type: {}", header.getDataType());
                }
        }
        if (message instanceof Unknown) {
            log.info(message.toString());
        }
        if (message != null) {
            message.release();
        }
    }

    public void messageSent(RTMPConnection conn, Object message) {
        log.debug("Message sent");
        if (message instanceof IoBuffer) {
            return;
        }
        conn.messageSent((Packet) message);
    }

    /**
	 * Pass through all Ping events to origin except ping/pong
	 */
    protected void onPing(RTMPConnection conn, Channel channel, Header source, Ping ping) {
        switch(ping.getEventType()) {
            case Ping.PONG_SERVER:
                conn.pingReceived(ping);
                break;
            default:
                Packet p = new Packet(source);
                p.setMessage(ping);
                forwardPacket(conn, p);
        }
    }

    protected void handleConnect(RTMPConnection conn, Channel channel, Header header, Invoke invoke, RTMP rtmp) {
        final IPendingServiceCall call = invoke.getCall();
        final Map<String, Object> params = invoke.getConnectionParams();
        String host = getHostname((String) params.get("tcUrl"));
        if (host.endsWith(":1935")) {
            host = host.substring(0, host.length() - 5);
        }
        String path = (String) params.get("app");
        if (path.indexOf("?") != -1) {
            int idx = path.indexOf("?");
            params.put("queryString", path.substring(idx));
            path = path.substring(0, idx);
        }
        params.put("path", path);
        final String sessionId = null;
        conn.setup(host, path, sessionId, params);
        if (!checkPermission(conn)) {
            call.setStatus(Call.STATUS_ACCESS_DENIED);
            call.setResult(getStatus(NC_CONNECT_REJECTED));
            Invoke reply = new Invoke();
            reply.setCall(call);
            reply.setInvokeId(invoke.getInvokeId());
            channel.write(reply);
            conn.close();
        } else {
            synchronized (rtmp) {
                sendConnectMessage(conn);
                rtmp.setState(RTMP.STATE_EDGE_CONNECT_ORIGIN_SENT);
                Packet packet = new Packet(header);
                packet.setMessage(invoke);
                forwardPacket(conn, packet);
                rtmp.setState(RTMP.STATE_ORIGIN_CONNECT_FORWARDED);
                if (Integer.valueOf(3).equals(params.get("objectEncoding"))) {
                    rtmp.setEncoding(Encoding.AMF3);
                }
            }
        }
    }

    protected boolean checkPermission(RTMPConnection conn) {
        return true;
    }

    protected void sendConnectMessage(RTMPConnection conn) {
        IMRTMPConnection mrtmpConn = mrtmpManager.lookupMRTMPConnection(conn);
        if (mrtmpConn != null) {
            mrtmpConn.connect(conn.getId());
        }
    }

    protected void forwardPacket(RTMPConnection conn, Packet packet) {
        IMRTMPConnection mrtmpConn = mrtmpManager.lookupMRTMPConnection(conn);
        if (mrtmpConn != null) {
            mrtmpManager.lookupMRTMPConnection(conn).write(conn.getId(), packet);
        }
    }

    @Override
    public void connectionClosed(RTMPConnection conn, RTMP state) {
        conn.close();
    }
}
