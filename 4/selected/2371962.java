package org.red5.server.net.rtmp;

import java.util.HashSet;
import java.util.Set;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.so.SharedObjectMessage;
import org.red5.server.stream.PlaylistSubscriberStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Base class for all RTMP handlers.
 *
 * @author The Red5 Project (red5@osflash.org)
 */
public abstract class BaseRTMPHandler implements IRTMPHandler, Constants, StatusCodes, ApplicationContextAware {

    /**
     * Logger
     */
    protected static Logger log = LoggerFactory.getLogger(BaseRTMPHandler.class);

    /**
     * Application context
     */
    protected ApplicationContext appCtx;

    private static ThreadLocal<Integer> streamLocal = new ThreadLocal<Integer>();

    public static int getStreamId() {
        return streamLocal.get().intValue();
    }

    /**
     * Setter for stream Id.
     *
     * @param id  Stream id
     */
    private static void setStreamId(int id) {
        streamLocal.set(id);
    }

    /** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext appCtx) throws BeansException {
        this.appCtx = appCtx;
    }

    /** {@inheritDoc} */
    public void connectionOpened(RTMPConnection conn, RTMP state) {
        if (state.getMode() == RTMP.MODE_SERVER && appCtx != null) {
            ISchedulingService service = (ISchedulingService) appCtx.getBean(ISchedulingService.BEAN_NAME);
            conn.startWaitForHandshake(service);
        }
    }

    /** {@inheritDoc} */
    public void messageReceived(RTMPConnection conn, ProtocolState state, Object in) throws Exception {
        IRTMPEvent message = null;
        try {
            final Packet packet = (Packet) in;
            message = packet.getMessage();
            final Header header = packet.getHeader();
            final Channel channel = conn.getChannel(header.getChannelId());
            final IClientStream stream = conn.getStreamById(header.getStreamId());
            if (log.isDebugEnabled()) {
                log.debug("Message recieved");
                log.debug("Stream Id: " + header);
                log.debug("Channel: " + channel);
            }
            Red5.setConnectionLocal(conn);
            BaseRTMPHandler.setStreamId(header.getStreamId());
            conn.messageReceived();
            message.setSource(conn);
            switch(header.getDataType()) {
                case TYPE_CHUNK_SIZE:
                    onChunkSize(conn, channel, header, (ChunkSize) message);
                    break;
                case TYPE_INVOKE:
                case TYPE_FLEX_MESSAGE:
                    onInvoke(conn, channel, header, (Invoke) message, (RTMP) state);
                    if (message.getHeader().getStreamId() != 0 && ((Invoke) message).getCall().getServiceName() == null && ACTION_PUBLISH.equals(((Invoke) message).getCall().getServiceMethodName())) {
                        if (stream != null) {
                            ((IEventDispatcher) stream).dispatchEvent(message);
                        }
                    }
                    break;
                case TYPE_NOTIFY:
                    if (((Notify) message).getData() != null && stream != null) {
                        ((IEventDispatcher) stream).dispatchEvent(message);
                    } else {
                        onInvoke(conn, channel, header, (Notify) message, (RTMP) state);
                    }
                    break;
                case TYPE_FLEX_STREAM_SEND:
                    if (stream != null) {
                        ((IEventDispatcher) stream).dispatchEvent(message);
                    }
                    break;
                case TYPE_PING:
                    onPing(conn, channel, header, (Ping) message);
                    break;
                case TYPE_BYTES_READ:
                    onStreamBytesRead(conn, channel, header, (BytesRead) message);
                    break;
                case TYPE_AUDIO_DATA:
                case TYPE_VIDEO_DATA:
                    if (stream != null) ((IEventDispatcher) stream).dispatchEvent(message);
                    break;
                case TYPE_FLEX_SHARED_OBJECT:
                case TYPE_SHARED_OBJECT:
                    onSharedObject(conn, channel, header, (SharedObjectMessage) message);
                    break;
                default:
                    log.debug("Unknown type: {}", header.getDataType());
            }
            if (message instanceof Unknown) {
                log.info("{}", message);
            }
        } catch (RuntimeException e) {
            log.error("Exception", e);
        }
        if (message != null) {
            message.release();
        }
    }

    /** {@inheritDoc} */
    public void messageSent(RTMPConnection conn, Object message) {
        if (log.isDebugEnabled()) {
            log.debug("Message sent");
        }
        if (message instanceof ByteBuffer) {
            return;
        }
        conn.messageSent((Packet) message);
        Packet sent = (Packet) message;
        final int channelId = sent.getHeader().getChannelId();
        final IClientStream stream = conn.getStreamByChannelId(channelId);
        if (stream != null && (stream instanceof PlaylistSubscriberStream)) {
            ((PlaylistSubscriberStream) stream).written(sent.getMessage());
        }
    }

    /** {@inheritDoc} */
    public void connectionClosed(RTMPConnection conn, RTMP state) {
        state.setState(RTMP.STATE_DISCONNECTED);
        conn.close();
    }

    /**
     * Return hostname for URL.
	 *
     * @param url          URL
     * @return             Hostname from that URL
     */
    protected String getHostname(String url) {
        log.debug("url: {}", url);
        String[] parts = url.split("/");
        if (parts.length == 2) {
            return "";
        } else {
            return parts[2];
        }
    }

    /**
     * Chunk size change event handler. Abstract, to be implemented in subclasses.
	 *
     * @param conn         Connection
     * @param channel      Channel
     * @param source       Header
     * @param chunkSize    New chunk size
     */
    protected abstract void onChunkSize(RTMPConnection conn, Channel channel, Header source, ChunkSize chunkSize);

    /**
     * Handler for pending call result. Dispatches results to all pending call handlers.
	 *
     * @param conn         Connection
     * @param invoke       Pending call result event context
     */
    protected void handlePendingCallResult(RTMPConnection conn, Notify invoke) {
        final IServiceCall call = invoke.getCall();
        final IPendingServiceCall pendingCall = conn.getPendingCall(invoke.getInvokeId());
        if (pendingCall != null) {
            Object[] args = call.getArguments();
            if ((args != null) && (args.length > 0)) {
                pendingCall.setResult(args[0]);
            }
            Set<IPendingServiceCallback> callbacks = pendingCall.getCallbacks();
            if (!callbacks.isEmpty()) {
                HashSet<IPendingServiceCallback> tmp = new HashSet<IPendingServiceCallback>();
                tmp.addAll(callbacks);
                for (IPendingServiceCallback callback : tmp) {
                    try {
                        callback.resultReceived(pendingCall);
                    } catch (Exception e) {
                        log.error("Error while executing callback {} {}", callback, e);
                    }
                }
            }
        }
    }

    /**
     * Invocation event handler.
	 *
     * @param conn         Connection
     * @param channel      Channel
     * @param source       Header
     * @param invoke       Invocation event context
     * @param rtmp		   RTMP connection state
     */
    protected abstract void onInvoke(RTMPConnection conn, Channel channel, Header source, Notify invoke, RTMP rtmp);

    /**
     * Ping event handler.
	 *
     * @param conn         Connection
     * @param channel      Channel
     * @param source       Header
     * @param ping         Ping event context
     */
    protected abstract void onPing(RTMPConnection conn, Channel channel, Header source, Ping ping);

    /**
     * Stream bytes read event handler.
	 *
     * @param conn              Connection
     * @param channel           Channel
     * @param source            Header
     * @param streamBytesRead   Bytes read event context
     */
    protected void onStreamBytesRead(RTMPConnection conn, Channel channel, Header source, BytesRead streamBytesRead) {
        conn.receivedBytesRead(streamBytesRead.getBytesRead());
    }

    /**
     * Shared object event handler.
	 *
     * @param conn              Connection
     * @param channel           Channel
     * @param source            Header
     * @param object            Shared object event context
     */
    protected abstract void onSharedObject(RTMPConnection conn, Channel channel, Header source, SharedObjectMessage object);
}
