package org.openorb.orb.iiop;

import java.util.Map;
import java.util.HashMap;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.omg.BiDirPolicy.BOTH;
import org.omg.BiDirPolicy.BIDIRECTIONAL_POLICY_TYPE;
import org.omg.BiDirPolicy.BidirectionalPolicy;
import org.omg.BiDirPolicy.BidirectionalPolicyHelper;
import org.openorb.orb.io.BufferSource;
import org.openorb.orb.io.StorageBuffer;
import org.openorb.orb.io.MarshalBuffer;
import org.openorb.orb.net.ServerChannel;
import org.openorb.orb.net.ClientChannel;
import org.openorb.orb.net.RequestIDAllocator;
import org.openorb.orb.net.ClientRequest;
import org.openorb.orb.net.ServerRequest;
import org.openorb.orb.net.ServerManager;
import org.openorb.orb.net.Transport;
import org.openorb.orb.util.Trace;
import org.openorb.util.ExceptionTool;
import org.openorb.util.NumberCache;
import org.omg.CORBA.INITIALIZE;

/**
 * IIOP implementation of {@link org.openorb.orb.net.ServerChannel}.
 *
 * @author Chris Wood
 * @version $Revision: 1.18 $ $Date: 2004/08/12 12:54:24 $
 */
public class IIOPServerChannel implements ServerChannel {

    public static final int CONN_SC_TOTAL = 2;

    public static final int CONN_SC_CODESETS = 0;

    public static final int CONN_SC_BIDIR = 1;

    private static int s_iiop_server_recv_timeout = 1000;

    private org.omg.CORBA.ORB m_orb;

    private ServerManager m_server_manager;

    private SocketQueue m_socket_queue;

    private Object m_sync_state;

    private int m_state = STATE_CONNECTED;

    private boolean m_pending_close = false;

    private boolean m_codesets = false;

    private int m_tcsc = 0;

    private int m_tcsw = 0;

    private IIOPClientProtocol m_client_protocol = null;

    private IIOPClientChannel m_client_peer = null;

    private boolean m_delegated = false;

    private Map m_active_requests = new HashMap();

    private int m_channel_age;

    private Object m_sync_recv = new Object();

    private boolean m_in_recv = false;

    private java.lang.reflect.Constructor m_os_ctor;

    private org.omg.IOP.Codec m_codec;

    private org.omg.IIOP.ListenPoint[] m_pending_bidir_endpoints;

    private Logger m_logger = null;

    private boolean[] m_recv_connection_service_contexts = new boolean[CONN_SC_TOTAL];

    /** Creates new IIOPServerChannel */
    public IIOPServerChannel(ServerManager serverManager, Transport transport, IIOPClientProtocol clientProtocol, org.omg.IOP.Codec codec) {
        m_server_manager = serverManager;
        m_orb = m_server_manager.orb();
        m_logger = ((org.openorb.orb.core.ORB) orb()).getLogger();
        try {
            Class[] cargs = new Class[] { org.omg.CORBA.ORB.class, org.omg.GIOP.Version.class, MarshalBuffer.class };
            m_os_ctor = ((org.openorb.orb.core.ORB) m_orb).getLoader().classConstructor("iiop.CDROutputStreamClass", "org.openorb.orb.iiop.CDROutputStream", cargs);
        } catch (Exception ex) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("Unable to initialize output stream constructor.", ex);
            }
            throw ExceptionTool.initCause(new INITIALIZE("Unable to initialize output stream constructor (" + ex + ")"), ex);
        }
        m_socket_queue = new SocketQueue(transport);
        m_socket_queue.enableLogging(getLogger().getChildLogger("ssq"));
        m_socket_queue.setServerChannel(this);
        m_client_protocol = clientProtocol;
        m_codec = codec;
        m_sync_state = new Object();
        if (getLogger().isDebugEnabled() && Trace.isHigh()) {
            getLogger().debug(this + " created");
        }
        m_server_manager.register_channel(this);
    }

    /**
     * Constructor for bidir delegates.
     */
    public IIOPServerChannel(ServerManager serverManager, IIOPClientChannel peer, org.omg.IOP.Codec codec) {
        m_server_manager = serverManager;
        m_orb = m_server_manager.orb();
        m_logger = ((org.openorb.orb.core.ORB) orb()).getLogger();
        try {
            Class[] cargs = new Class[] { org.omg.CORBA.ORB.class, org.omg.GIOP.Version.class, MarshalBuffer.class };
            m_os_ctor = ((org.openorb.orb.core.ORB) m_orb).getLoader().classConstructor("iiop.CDROutputStreamClass", "org.openorb.orb.iiop.CDROutputStream", cargs);
        } catch (final Exception ex) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("Unable to initialize output stream constructor.", ex);
            }
            throw ExceptionTool.initCause(new INITIALIZE("Unable to initialize output stream constructor (" + ex + ")"), ex);
        }
        m_client_peer = peer;
        m_socket_queue = m_client_peer.getSocketQueue();
        m_socket_queue.setServerChannel(this);
        m_delegated = true;
        m_codec = codec;
        m_sync_state = m_client_peer.getSyncState();
        m_codesets = true;
        m_tcsc = m_client_peer.getTCSC();
        m_tcsw = m_client_peer.getTCSW();
    }

    /**
     * Obtain the transport which created this channel.
     *
     * @return The Transport which created this channel.
     */
    public Transport transport() {
        return m_socket_queue.getTransport();
    }

    /**
     * Set the IIOP server receive timeout.
     */
    public static void setIIOPServerRecvTimeout(int timeout) {
        s_iiop_server_recv_timeout = timeout;
    }

    /**
     * Return the array with active requests.
     *
     * @return The active requests.
     */
    public Map getActiveRequestMap() {
        return m_active_requests;
    }

    /**
     * Return the socket queue.
     *
     * @return The socket queue.
     */
    public SocketQueue getSocketQueue() {
        return m_socket_queue;
    }

    /**
     * Return the server manager of this channel.
     *
     * @return The server manager.
     */
    public ServerManager getServerManager() {
        return m_server_manager;
    }

    /**
     * Return the synchronization state of the client channel.
     *
     * @return The synchronization state.
     */
    public Object getSyncState() {
        return m_sync_state;
    }

    /**
     * Return the age of the channel.
     *
     * @return The channel's age.
     */
    public int getChannelAge() {
        return m_channel_age;
    }

    /**
     * Set this server's peer, which is usually the client.
     *
     * @param cltchan The client channel.
     */
    public void setClientPeer(IIOPClientChannel cltchan) {
        m_client_peer = cltchan;
    }

    /**
     * returns the selected char codeset.
     *
     * @return The char codeset.
     */
    public int getTCSC() {
        return m_tcsc;
    }

    /**
     * Sets the selected char codeset.
     *
     * @param tcsc The char codest.
     */
    public void setTCSC(int tcsc) {
        m_tcsc = tcsc;
    }

    /**
     * Returns the selected wide char codeset.
     *
     * @return The wide char codeset.
     */
    public int getTCSW() {
        return m_tcsw;
    }

    /**
     * Sets the selected wide char codeset.
     *
     * @param tcsw The wide char codeset.
     */
    public void setTCSW(int tcsw) {
        m_tcsw = tcsw;
    }

    /**
     * Set the channel to a closing state..
     */
    public void setPendingClose() {
        m_pending_close = true;
    }

    /**
     * Creates a stringified representation of this class.
     *
     * @return A string showing information about this class.
     */
    public String toString() {
        return "ServerChannel: " + m_socket_queue.toString();
    }

    /**
     * Return the orb instance.
     *
     * @return The orb.
     */
    public org.omg.CORBA.ORB orb() {
        return m_orb;
    }

    public int state() {
        if (m_delegated) {
            synchronized (m_sync_state) {
                if (m_client_peer != null) {
                    return STATE_CONNECTED;
                }
                return STATE_CLOSED;
            }
        }
        return m_state;
    }

    /**
     * Sets the state of the channel.
     *
     * @param state The state to switch this channel into.
     */
    public void setState(int state) {
        m_state = state;
    }

    /**
     * Active request count. This is the number of requests which have not
     * yet been sent or are still expecting a reply. This will return -1 if the
     * channel has been perminently closed.
     */
    public int active_requests() {
        synchronized (m_sync_state) {
            return m_active_requests.size() + ((m_client_peer != null) ? m_client_peer.getActiveRequestMap().size() : 0);
        }
    }

    /**
     * Indication of channel age. On the client side this is the request ID of
     * the oldest active request, Since request IDs rise sequentialy this gives
     * an indication of the activity on the channel. On server channels this
     * will result in a call to peek_request_id if active_requests would be
     * non-zero, otherwise it returns whatever peek_request_id returned the
     * last time active_requests dropped to zero.
     */
    public int channel_age() {
        synchronized (m_sync_state) {
            if (!m_active_requests.isEmpty() || (m_client_peer != null && !m_client_peer.getActiveRequestMap().isEmpty())) {
                return RequestIDAllocator.peek_request_id();
            }
            if (m_client_peer != null && m_channel_age < m_client_peer.getChannelAge()) {
                return m_client_peer.getChannelAge();
            }
            return m_channel_age;
        }
    }

    /**
     * Close the connection. This closes the channel immediatly, all replys to
     * current requests will be discarded. This is a disorderly shutdown.
     */
    public void close() {
        if (getLogger().isDebugEnabled() && Trace.isHigh()) {
            getLogger().debug(this + " disorderly shutdown");
        }
        if (m_delegated) {
            m_client_peer.close(true, new org.omg.CORBA.BAD_INV_ORDER(0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE));
            return;
        }
        ServerRequest[] serverRequests;
        ClientRequest[] clientRequests;
        synchronized (m_sync_state) {
            if (m_state == STATE_CLOSED) {
                return;
            }
            m_state = STATE_CLOSED;
            m_pending_close = false;
            serverRequests = new ServerRequest[m_active_requests.values().size()];
            m_active_requests.values().toArray(serverRequests);
            m_active_requests.clear();
            if (m_client_peer != null) {
                Map actreq = m_client_peer.getActiveRequestMap();
                clientRequests = new ClientRequest[actreq.values().size()];
                actreq.values().toArray(clientRequests);
                actreq.clear();
                m_client_peer.setState(ClientChannel.STATE_CLOSED);
                m_client_peer.setCloseException(new org.omg.CORBA.BAD_INV_ORDER(0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE));
                m_client_peer.setServerPeer(null);
                m_client_peer = null;
            } else {
                clientRequests = null;
            }
        }
        m_server_manager.unregister_channel(this);
        m_socket_queue.close();
        for (int i = 0; i < serverRequests.length; ++i) {
            serverRequests[i].client_cancel();
        }
        if (clientRequests != null) {
            for (int i = 0; i < clientRequests.length; ++i) {
                clientRequests[i].cancel(new org.omg.CORBA.BAD_INV_ORDER("ORB shutdown", org.omg.CORBA.OMGVMCID.value | 4, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE));
            }
        }
        if (getLogger().isDebugEnabled() && Trace.isHigh()) {
            getLogger().debug(this + " closed");
        }
    }

    /**
     * Close the connection after all currently processing requests are complete.
     * If there are no currently active requests the channel will close immediatly
     * otherwise if ifActive is true then the channel will close once all
     * currently active requests have completed, and in the mean time new
     * incomming requests will be silently discarded. This call returns
     * immediatly, use the state function to interrogate the state while a close
     * is pending.
     */
    public void soft_close(boolean ifActive) {
        if (Trace.isHigh() && getLogger().isDebugEnabled()) {
            getLogger().debug(this + " soft close [" + ifActive + "]");
        }
        if (m_delegated) {
            if (!ifActive) {
                m_client_peer.pause();
            } else {
                m_client_peer.close(false, new org.omg.CORBA.BAD_INV_ORDER(org.omg.CORBA.OMGVMCID.value | 4, org.omg.CORBA.CompletionStatus.COMPLETED_NO));
            }
            return;
        }
        synchronized (m_sync_state) {
            if (m_state == STATE_CLOSED) {
                return;
            }
            if (active_requests() > 0) {
                if (ifActive) {
                    m_pending_close = true;
                    if (m_client_peer != null) {
                        m_client_peer.setPendingClose();
                    }
                }
                return;
            }
            m_state = STATE_CLOSED;
            m_pending_close = false;
            if (m_client_peer != null) {
                m_client_peer.setState(ClientChannel.STATE_PAUSED);
                m_client_peer.setServerPeer(null);
                m_client_peer = null;
            }
        }
        m_server_manager.unregister_channel(this);
        m_socket_queue.close();
        if (getLogger().isDebugEnabled() && Trace.isHigh()) {
            getLogger().debug(this + " closed");
        }
    }

    /**
     * Wait the specified amount of time for an incoming message.
     * @return false if the channel is closed.
     */
    public boolean recv(int timeout) {
        if (m_delegated) {
            return false;
        }
        synchronized (m_sync_recv) {
            if (m_in_recv) {
                return true;
            }
            m_in_recv = true;
        }
        try {
            if (m_socket_queue.receive(timeout)) {
                return true;
            }
            close();
            return false;
        } finally {
            synchronized (m_sync_recv) {
                m_in_recv = false;
            }
        }
    }

    /**
     * Donate a thread for recieving messages. This function returns when interrupt
     * is called on the thread or the channel is closed.
     */
    public void run_recv() {
        if (m_delegated) {
            return;
        }
        synchronized (m_sync_recv) {
            if (m_in_recv) {
                return;
            }
            m_in_recv = true;
        }
        try {
            while (!Thread.interrupted()) {
                if (!m_socket_queue.receive(s_iiop_server_recv_timeout)) {
                    close();
                    break;
                }
            }
        } finally {
            synchronized (m_sync_recv) {
                m_in_recv = false;
            }
        }
    }

    /**
     * Called by SocketQueue. Read off the request header, construct a
     * server request object, add it to the active request list and return.
     */
    int process_request(byte minor, CDRInputStream is, byte msg_type, BufferSource source) {
        IIOPServerRequest request = null;
        int request_id = 0;
        byte[] object_id = null;
        switch(msg_type) {
            case org.omg.GIOP.MsgType_1_1._Request:
                String operation;
                byte sync_scope;
                org.omg.IOP.ServiceContext[] request_service_contexts;
                switch(minor) {
                    case 0:
                    case 1:
                        request_service_contexts = org.omg.IOP.ServiceContextListHelper.read(is);
                        request_id = is.read_ulong();
                        sync_scope = (is.read_boolean()) ? (byte) org.omg.Messaging.SYNC_WITH_TARGET.value : (byte) org.omg.Messaging.SYNC_WITH_SERVER.value;
                        object_id = org.omg.CORBA.OctetSeqHelper.read(is);
                        operation = is.read_string();
                        org.omg.CORBA.OctetSeqHelper.read(is);
                        request = new IIOPServerRequest(m_server_manager, this, request_id, is, object_id, operation, sync_scope, request_service_contexts, new org.omg.GIOP.Version((byte) 1, minor));
                        checkConnectionSCs(request_service_contexts);
                        if (m_recv_connection_service_contexts == null || m_recv_connection_service_contexts[CONN_SC_CODESETS]) {
                            is.setCodesets(m_tcsc, m_tcsw);
                        }
                        if (!addRequest(request)) {
                            return 0;
                        }
                        break;
                    case 2:
                        request_id = is.read_ulong();
                        request = new IIOPServerRequest(m_server_manager, this, request_id, is, new org.omg.GIOP.Version((byte) 1, minor));
                        request.setRequestSource(source);
                        if (!addRequest(request)) {
                            return 0;
                        }
                        sync_scope = is.read_octet();
                        is.skip(3);
                        object_id = read_target_address(is.read_short(), is);
                        operation = is.read_string();
                        request_service_contexts = org.omg.IOP.ServiceContextListHelper.read(is);
                        try {
                            is.alignment(8);
                        } catch (org.omg.CORBA.SystemException ex) {
                        }
                        if (!m_delegated) {
                            checkConnectionSCs(request_service_contexts);
                        }
                        if (m_recv_connection_service_contexts == null || m_recv_connection_service_contexts[CONN_SC_CODESETS]) {
                            is.setCodesets(m_tcsc, m_tcsw);
                        }
                        request.init(object_id, operation, sync_scope, request_service_contexts);
                }
                source.removeWaitingForBufferListener(null);
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug(this + " incoming request #" + request_id);
                }
                m_server_manager.enqueue_request(request);
                break;
            case org.omg.GIOP.MsgType_1_1._LocateRequest:
                switch(minor) {
                    case 0:
                    case 1:
                        request_id = is.read_ulong();
                        object_id = org.omg.CORBA.OctetSeqHelper.read(is);
                        request = new IIOPServerRequest(m_server_manager, this, request_id, object_id, new org.omg.GIOP.Version((byte) 1, minor));
                        if (!addRequest(request)) {
                            return 0;
                        }
                        break;
                    case 2:
                        request_id = is.read_ulong();
                        request = new IIOPServerRequest(m_server_manager, this, request_id, new org.omg.GIOP.Version((byte) 1, minor));
                        request.setRequestSource(source);
                        if (!addRequest(request)) {
                            return 0;
                        }
                        object_id = read_target_address(is.read_short(), is);
                        request.init(object_id);
                        break;
                }
                source.removeWaitingForBufferListener(null);
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug(this + " incoming locate request #" + request_id);
                }
                m_server_manager.enqueue_request(request);
                break;
            case org.omg.GIOP.MsgType_1_1._CancelRequest:
                request_id = is.read_ulong();
                synchronized (m_sync_state) {
                    request = (IIOPServerRequest) m_active_requests.remove(NumberCache.getInteger(request_id));
                }
                if (request != null) {
                    if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                        getLogger().debug(this + " request #" + request_id + " canceled by client");
                    }
                    request.client_cancel();
                }
                break;
        }
        return request_id;
    }

    void process_fragment(int req_id, StorageBuffer buf, boolean fragFollows) {
        IIOPServerRequest request;
        synchronized (m_sync_state) {
            request = (IIOPServerRequest) m_active_requests.get(NumberCache.getInteger(req_id));
        }
        if (request != null) {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                if (fragFollows) {
                    getLogger().debug(this + " request #" + req_id + " request fragment");
                } else {
                    getLogger().debug(this + " request #" + req_id + " last request fragment");
                }
            }
            request.getRequestSource().addLast(buf, !fragFollows);
        }
    }

    protected void checkConnectionSCs(org.omg.IOP.ServiceContext[] request_service_contexts) {
        if (m_recv_connection_service_contexts == null) {
            return;
        }
        boolean recv = false;
        boolean readCSFirst = false;
        int i = 0;
        for (int j = 0; j < request_service_contexts.length; ++j) {
            if (request_service_contexts[j].context_id == org.omg.IOP.TAG_CODE_SETS.value) {
                readCSFirst = true;
                i = j;
                break;
            }
        }
        for (; i < request_service_contexts.length; ++i) {
            switch(request_service_contexts[i].context_id) {
                case org.omg.IOP.TAG_CODE_SETS.value:
                    if (m_recv_connection_service_contexts[CONN_SC_CODESETS]) {
                        break;
                    }
                    byte[] cd = request_service_contexts[i].context_data;
                    if (cd[0] == 0) {
                        m_tcsc = ((cd[4] & 0xFF) << 24) | ((cd[5] & 0xFF) << 16) | ((cd[6] & 0xFF) << 8) | (cd[7] & 0xFF);
                        m_tcsw = ((cd[8] & 0xFF) << 24) | ((cd[9] & 0xFF) << 16) | ((cd[10] & 0xFF) << 8) | (cd[11] & 0xFF);
                    } else {
                        m_tcsc = ((cd[7] & 0xFF) << 24) | ((cd[6] & 0xFF) << 16) | ((cd[5] & 0xFF) << 8) | (cd[4] & 0xFF);
                        m_tcsw = ((cd[11] & 0xFF) << 24) | ((cd[10] & 0xFF) << 16) | ((cd[9] & 0xFF) << 8) | (cd[8] & 0xFF);
                    }
                    if (m_client_peer != null) {
                        m_client_peer.setTCSC(m_tcsc);
                        m_client_peer.setTCSW(m_tcsw);
                    }
                    m_recv_connection_service_contexts[CONN_SC_CODESETS] = true;
                    recv = true;
                    break;
                case org.omg.IOP.BI_DIR_IIOP.value:
                    if (m_recv_connection_service_contexts[CONN_SC_BIDIR]) {
                        break;
                    }
                    try {
                        if (m_client_protocol != null) {
                            org.omg.CORBA.Any any = m_codec.decode_value(request_service_contexts[i].context_data, org.omg.IIOP.BiDirIIOPServiceContextHelper.type());
                            m_pending_bidir_endpoints = org.omg.IIOP.BiDirIIOPServiceContextHelper.extract(any).listen_points;
                        }
                        m_recv_connection_service_contexts[CONN_SC_BIDIR] = true;
                        recv = true;
                    } catch (org.omg.CORBA.UserException ex) {
                        if (getLogger().isErrorEnabled()) {
                            getLogger().error("Unexpected UserException.", ex);
                        }
                    }
                    break;
            }
            if (readCSFirst) {
                i = -1;
                readCSFirst = false;
            }
        }
        if (recv) {
            for (int j = 0; j < m_recv_connection_service_contexts.length; ++j) {
                recv = recv && m_recv_connection_service_contexts[j];
            }
            if (recv) {
                m_recv_connection_service_contexts = null;
            }
        }
    }

    /**
     * Completes the bidir activation. This is called by the server interceptor.
     */
    public void checkBiDirActivation(IIOPServerRequest request) {
        if (m_pending_bidir_endpoints != null && m_client_peer == null) {
            BidirectionalPolicy pol = null;
            try {
                pol = BidirectionalPolicyHelper.narrow(request.get_server_policy(BIDIRECTIONAL_POLICY_TYPE.value));
            } catch (org.omg.CORBA.INV_POLICY ex) {
            }
            if (pol != null && pol.value() == BOTH.value) {
                synchronized (m_sync_state) {
                    if (m_state == STATE_CONNECTED && m_client_peer == null) {
                        m_client_peer = m_client_protocol.createBidirDelegate(this, m_pending_bidir_endpoints);
                        m_pending_bidir_endpoints = null;
                        if (m_client_peer != null) {
                            if (getLogger().isDebugEnabled() && Trace.isHigh()) {
                                getLogger().debug(this + " became bidirectional with peer " + m_client_peer.toString());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds request. Returns false if state is closed or will be closed soon
     */
    private boolean addRequest(IIOPServerRequest request) {
        synchronized (m_sync_state) {
            if (m_state == STATE_CLOSED || m_pending_close) {
                return false;
            }
            m_active_requests.put(NumberCache.getInteger(request.request_id()), request);
        }
        return true;
    }

    private byte[] read_target_address(short disposition, CDRInputStream is) {
        byte[] object_id = null;
        switch(disposition) {
            case org.omg.GIOP.KeyAddr.value:
                object_id = org.omg.CORBA.OctetSeqHelper.read(is);
                break;
            case org.omg.GIOP.ProfileAddr.value:
                if (is.read_ulong() != org.omg.IOP.TAG_INTERNET_IOP.value) {
                    return null;
                }
                is.begin_encapsulation();
                is.skip(2);
                is.skip(is.read_long());
                is.skip(2);
                object_id = org.omg.CORBA.OctetSeqHelper.read(is);
                is.end_encapsulation();
                break;
            case org.omg.GIOP.ReferenceAddr.value:
                int offset = is.read_ulong();
                int len = is.read_ulong();
                for (int i = 0; i < len; ++i) {
                    if (i == offset) {
                        object_id = read_target_address(org.omg.GIOP.ProfileAddr.value, is);
                    } else {
                        is.skip(4);
                        is.skip(is.read_ulong());
                    }
                }
                break;
        }
        return object_id;
    }

    CDROutputStream create_reply_stream(IIOPServerRequest req, int status) {
        if (!m_socket_queue.isOpen()) {
            throw new org.omg.CORBA.COMM_FAILURE("Client has shutdown", IIOPMinorCodes.COMM_FAILURE_CLIENT_DIED, org.omg.CORBA.CompletionStatus.COMPLETED_YES);
        }
        org.omg.GIOP.Version vers = req.version();
        int req_id = req.request_id();
        org.omg.IOP.ServiceContext[] reply_service_contexts = req.get_reply_service_contexts();
        MarshalBuffer buf = new MarshalBuffer(m_marshal_listener, req);
        buf.enableLogging(getLogger().getChildLogger("mb"));
        CDROutputStream os;
        try {
            os = (CDROutputStream) m_os_ctor.newInstance(new Object[] { m_orb, vers, buf });
            if (LogEnabled.class.isAssignableFrom(os.getClass())) {
                ((LogEnabled) os).enableLogging(getLogger().getChildLogger("os"));
            }
        } catch (final Exception ex) {
            getLogger().error("Unable to create CDROutputStream class.", ex);
            throw ExceptionTool.initCause(new INITIALIZE("Unable to create CDROutputStream class (" + ex + ")"), ex);
        }
        if (m_recv_connection_service_contexts == null || m_recv_connection_service_contexts[CONN_SC_CODESETS]) {
            os.setCodesets(m_tcsc, m_tcsw);
        }
        new HeaderBlock((byte) org.omg.GIOP.MsgType_1_1._Reply, req_id, os);
        switch(vers.minor) {
            case 0:
            case 1:
                org.omg.IOP.ServiceContextListHelper.write(os, reply_service_contexts);
                os.write_ulong(req_id);
                os.write_ulong(status);
                break;
            case 2:
                os.write_ulong(req_id);
                os.allowFragment();
                os.write_ulong(status);
                org.omg.IOP.ServiceContextListHelper.write(os, reply_service_contexts);
                os.pending_alignment(8);
                break;
        }
        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
            getLogger().debug(this + " request #" + req_id + " reply stream created. Reply status: " + status);
        }
        return os;
    }

    CDROutputStream create_locate_reply_stream(IIOPServerRequest req, int status) {
        if (!m_socket_queue.isOpen()) {
            throw new org.omg.CORBA.COMM_FAILURE("Client has shutdown", IIOPMinorCodes.COMM_FAILURE_CLIENT_DIED, org.omg.CORBA.CompletionStatus.COMPLETED_YES);
        }
        org.omg.GIOP.Version vers = req.version();
        int req_id = req.request_id();
        MarshalBuffer buf = new MarshalBuffer(m_marshal_listener, req);
        buf.enableLogging(getLogger().getChildLogger("mb"));
        CDROutputStream os;
        try {
            os = (CDROutputStream) m_os_ctor.newInstance(new Object[] { m_orb, vers, buf });
        } catch (final Exception ex) {
            getLogger().error("Unable to create CDROutputStream class.", ex);
            throw ExceptionTool.initCause(new INITIALIZE("Unable to create CDROutputStream class (" + ex + ")"), ex);
        }
        new HeaderBlock((byte) org.omg.GIOP.MsgType_1_1._LocateReply, req_id, os);
        os.write_ulong(req_id);
        if (vers.minor == (byte) 2) {
            os.allowFragment();
        }
        os.write_ulong(status);
        if (vers.minor == (byte) 2) {
            os.pending_alignment(8);
        }
        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
            getLogger().debug(this + " request #" + req_id + " locate reply stream created. Reply status: " + status);
        }
        return os;
    }

    /**
     * Any call to process_request always completes with a call to this
     * function. Should close the channel if a close is pending.
     */
    void release_request(IIOPServerRequest request) {
        synchronized (m_sync_state) {
            int req_id = request.request_id();
            m_active_requests.remove(NumberCache.getInteger(req_id));
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug(this + " request #" + req_id + " completed");
            }
            if (!m_active_requests.isEmpty()) {
                return;
            }
            m_channel_age = RequestIDAllocator.peek_request_id();
        }
        if (m_pending_close) {
            soft_close(true);
        }
    }

    private MarshalBuffer.Listener m_marshal_listener = new MarshalListener();

    /**
     * This class implements the Marshal.Listener interface which is used by
     * the MarshalBuffer to send important messages to interested listeners.
     *
     * @see MarhslBuffer.Listener
     */
    private class MarshalListener implements MarshalBuffer.Listener {

        /**
         * The availIncreased method is called when the size of the marshal buffer
         * increases.
         * The implementation of this method
         */
        public void availIncreaced(MarshalBuffer buffer, int available, Object cookie) {
            if (available > SocketQueue.MAX_FRAG_SIZE) {
                IIOPServerRequest req = (IIOPServerRequest) cookie;
                while (buffer.available() > SocketQueue.MAX_FRAG_SIZE) {
                    if (!m_socket_queue.send(buffer.fragment(SocketQueue.MAX_FRAG_SIZE))) {
                        throw new org.omg.CORBA.COMM_FAILURE("Client has shutdown", IIOPMinorCodes.COMM_FAILURE_CLIENT_DIED, org.omg.CORBA.CompletionStatus.COMPLETED_YES);
                    }
                }
                if (IIOPServerChannel.this.getLogger().isDebugEnabled() && Trace.isMedium()) {
                    IIOPServerChannel.this.getLogger().debug(IIOPServerChannel.this + "request #" + req.request_id() + " last fragment sent");
                }
            }
        }

        public void bufferClosed(MarshalBuffer buffer, int available, Object cookie) {
            org.openorb.orb.io.StorageBuffer buf = buffer.lastFragment();
            IIOPServerRequest req = (IIOPServerRequest) cookie;
            if (!m_socket_queue.send(buf)) {
                throw new org.omg.CORBA.COMM_FAILURE("Client has shutdown", IIOPMinorCodes.COMM_FAILURE_CLIENT_DIED, org.omg.CORBA.CompletionStatus.COMPLETED_YES);
            }
            if (IIOPServerChannel.this.getLogger().isDebugEnabled() && Trace.isMedium()) {
                IIOPServerChannel.this.getLogger().debug(IIOPServerChannel.this + " request #" + req.request_id() + " last fragment sent after buffer close");
            }
        }

        public void bufferCanceled(MarshalBuffer buffer, org.omg.CORBA.SystemException ex, Object cookie) {
            ex.completed = org.omg.CORBA.CompletionStatus.COMPLETED_YES;
            throw ex;
        }
    }

    private Logger getLogger() {
        return m_logger;
    }
}
