package org.openorb.orb.iiop;

import java.util.Map;
import java.util.HashMap;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.omg.BiDirPolicy.BidirectionalPolicy;
import org.omg.BiDirPolicy.BidirectionalPolicyHelper;
import org.omg.BiDirPolicy.BOTH;
import org.omg.BiDirPolicy.BIDIRECTIONAL_POLICY_TYPE;
import org.omg.CORBA.INITIALIZE;
import org.omg.CORBA.SystemException;
import org.omg.IOP.ServiceContext;
import org.omg.IOP.TAG_CODE_SETS;
import org.openorb.orb.net.ClientChannel;
import org.openorb.orb.net.ServerChannel;
import org.openorb.orb.net.Transport;
import org.openorb.orb.net.ClientManager;
import org.openorb.orb.net.Address;
import org.openorb.orb.net.ClientRequest;
import org.openorb.orb.net.RebindChannelException;
import org.openorb.orb.net.ServerRequest;
import org.openorb.orb.net.RequestIDAllocator;
import org.openorb.orb.io.BufferSource;
import org.openorb.orb.io.StorageBuffer;
import org.openorb.orb.io.MarshalBuffer;
import org.openorb.orb.util.Trace;
import org.openorb.util.ExceptionTool;
import org.openorb.util.NumberCache;

/**
 * Implements the {@link org.openorb.orb.net.ClientChannel} interface for IIOP.
 *
 * @author Chris Wood
 * @version $Revision: 1.17 $ $Date: 2004/08/12 12:54:23 $
 */
public class IIOPClientChannel implements ClientChannel {

    private static final int CONN_SC_TOTAL = 2;

    private static final int CONN_SC_CODESETS = 0;

    private static final int CONN_SC_BIDIR = 1;

    private static final int CONN_SC_STATUS_UNSENT = 0;

    private static final int CONN_SC_STATUS_PEND = 1;

    private static final int CONN_SC_STATUS_SENT = 2;

    private static final byte[] RESERVED = { 0, 0, 0 };

    private static int s_iiop_client_recv_timeout = 1000;

    private org.omg.CORBA.ORB m_orb;

    private IIOPClientProtocol m_client_protocol;

    private ClientManager m_client_manager;

    private Object m_sync_state;

    private int m_state = STATE_PAUSED;

    private boolean m_first_connect = true;

    private boolean m_paused = false;

    private boolean m_pending_close = false;

    private boolean m_pending_service_ctxt = false;

    private SocketQueue m_socket_queue;

    private org.omg.IIOP.Version m_version = new org.omg.IIOP.Version((byte) 1, (byte) 0);

    private org.omg.CORBA.SystemException m_close_exception = null;

    private short m_disposition = org.omg.GIOP.KeyAddr.value;

    private Logger m_logger = null;

    private Map m_active_requests = new HashMap();

    private int m_channel_age = 0;

    private int m_tcsc;

    private int m_tcsw;

    private int[] m_service_ctxts_sent = null;

    private boolean m_delegated = false;

    private IIOPServerProtocol m_server_protocol;

    private IIOPServerChannel m_server_peer;

    private java.lang.reflect.Constructor m_os_ctor;

    /**
     * Construct an initiating client channel.
     * @param protocol The client protocol.
     * @param transp Message transport.
     * @param serverProtocol non-null is a prerequisite for bidirectional use.
     * @param tcsc char codeset.
     * @param tcsw wchar codeset.
     */
    IIOPClientChannel(IIOPClientProtocol protocol, Transport transp, IIOPServerProtocol serverProtocol, int tcsc, int tcsw) {
        m_client_protocol = protocol;
        m_orb = m_client_protocol.orb();
        m_logger = ((org.openorb.orb.core.ORB) orb()).getLogger();
        m_client_manager = m_client_protocol.getClientManager();
        m_socket_queue = new SocketQueue(transp);
        m_socket_queue.enableLogging(getLogger().getChildLogger("csq"));
        m_socket_queue.setClientChannel(this);
        m_sync_state = new Object();
        m_server_protocol = serverProtocol;
        m_tcsc = tcsc;
        m_tcsw = tcsw;
        try {
            Class[] cargs = new Class[] { org.omg.CORBA.ORB.class, org.omg.GIOP.Version.class, MarshalBuffer.class };
            m_os_ctor = ((org.openorb.orb.core.ORB) m_orb).getLoader().classConstructor("iiop.CDROutputStreamClass", "org.openorb.orb.iiop.CDROutputStream", cargs);
        } catch (final Exception ex) {
            getLogger().error("Unable to initialize output stream constructor.", ex);
            throw ExceptionTool.initCause(new INITIALIZE("Unable to initialize output stream constructor (" + ex + ")"), ex);
        }
        if (getLogger().isDebugEnabled() && Trace.isHigh()) {
            getLogger().debug(this + " created");
        }
    }

    /**
     * Construct a delegate client channel.
     */
    IIOPClientChannel(IIOPClientProtocol protocol, IIOPServerChannel peer) {
        m_client_protocol = protocol;
        m_orb = m_client_protocol.orb();
        m_logger = ((org.openorb.orb.core.ORB) orb()).getLogger();
        m_client_manager = m_client_protocol.getClientManager();
        m_server_peer = peer;
        m_socket_queue = m_server_peer.getSocketQueue();
        m_socket_queue.setClientChannel(this);
        m_sync_state = m_server_peer.getSyncState();
        m_state = STATE_CONNECTED;
        m_delegated = true;
        m_tcsc = m_server_peer.getTCSC();
        m_tcsw = m_server_peer.getTCSW();
        try {
            Class[] cargs = new Class[] { org.omg.CORBA.ORB.class, org.omg.GIOP.Version.class, MarshalBuffer.class };
            m_os_ctor = ((org.openorb.orb.core.ORB) m_orb).getLoader().classConstructor("iiop.CDROutputStreamClass", "org.openorb.orb.iiop.CDROutputStream", cargs);
        } catch (final Exception ex) {
            getLogger().error("Unable to initialize output stream constructor.", ex);
            throw ExceptionTool.initCause(new INITIALIZE("Unable to initialize output stream constructor (" + ex + ")"), ex);
        }
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
     * Set the IIOP client receive timeout.
     *
     * @param timeout The new timeout value.
     */
    public static void setIIOPClientRecvTimeout(int timeout) {
        s_iiop_client_recv_timeout = timeout;
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
     * Set this client's peer, which is usually the server.
     *
     * @param srvchan The server channel.
     */
    public void setServerPeer(IIOPServerChannel srvchan) {
        m_server_peer = srvchan;
    }

    /**
     * Set the closing exception.
     *
     * @param closeex
     */
    public void setCloseException(org.omg.CORBA.SystemException closeex) {
        m_close_exception = closeex;
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

    public org.omg.CORBA.ORB orb() {
        return m_orb;
    }

    /**
     * Connection state. Note that state values are 0x1000000 apart,
     * Allowing this value to be ORed with the binding priority.
     */
    public int state() {
        if (m_delegated) {
            synchronized (m_sync_state) {
                if (m_server_peer != null) {
                    return STATE_CONNECTED;
                }
                return STATE_PAUSED;
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
     * Returns whether the channel is delegated.
     * @return True when the channel is delegated, false otherwise.
     */
    boolean isDelegated() {
        return m_delegated;
    }

    /**
     * Returns a stringified representation of this class.
     *
     * @return A string representation of useful information of this class.
     */
    public String toString() {
        return "ClientChannel: " + m_socket_queue.toString();
    }

    /**
     * Return the GIOP/IIOP version
     */
    public org.omg.IIOP.Version version() {
        return m_version;
    }

    /**
     * Active request count. This is the number of requests which have not
     * yet been sent or are still expecting a reply.
     */
    public int active_requests() {
        synchronized (m_sync_state) {
            return m_active_requests.size() + ((m_server_peer != null) ? m_server_peer.getActiveRequestMap().size() : 0);
        }
    }

    /**
     * Indication of channel age. This will result in a call to peek_request_id
     * if active_requests would be non-zero, otherwise it returns whatever
     * peek_request_id returned the last time active_requests dropped to zero.
     */
    public int channel_age() {
        synchronized (m_sync_state) {
            if (!m_active_requests.isEmpty() || (m_server_peer != null && !m_server_peer.getActiveRequestMap().isEmpty())) {
                return RequestIDAllocator.peek_request_id();
            }
            if (m_server_peer != null && m_channel_age < m_server_peer.getChannelAge()) {
                return m_server_peer.getChannelAge();
            }
            return m_channel_age;
        }
    }

    /**
      * Open channel for request. m_syncState should be owned.
      */
    private void request_open(org.omg.CORBA.Object target, Address address) {
        switch(m_state) {
            case STATE_CONNECTED:
                return;
            case STATE_CLOSED:
                throw m_close_exception;
        }
        if (m_delegated && m_server_peer == null) {
            throw new org.omg.CORBA.TRANSIENT(0, org.omg.CORBA.CompletionStatus.COMPLETED_NO);
        }
        m_socket_queue.open();
        m_state = STATE_CONNECTED;
        m_first_connect = false;
        m_pending_close = false;
        m_close_exception = null;
        connectionSCReset();
        m_server_peer = null;
        m_client_manager.register_channel(this);
        if (getLogger().isDebugEnabled() && Trace.isHigh()) {
            getLogger().debug(this + " opened");
        }
        return;
    }

    /**
     * Begin the pausing sequence, enter the PAUSED state. As soon as
     * work_pending returns false this will close the connection.
     */
    public void pause() {
        if (m_delegated) {
            m_server_peer.soft_close(false);
            return;
        }
        synchronized (m_sync_state) {
            if (active_requests() > 0) {
                return;
            }
            if (m_state != STATE_CONNECTED) {
                return;
            }
            if (getLogger().isDebugEnabled() && Trace.isHigh()) {
                getLogger().debug(this + " paused");
            }
            m_state = STATE_PAUSED;
            if (m_server_peer != null) {
                m_server_peer.setState(ServerChannel.STATE_CLOSED);
                m_server_peer.setClientPeer(null);
                m_server_peer = null;
            }
            m_paused = true;
        }
        m_client_manager.unregister_channel(this);
        m_socket_queue.close();
        synchronized (m_sync_state) {
            m_paused = false;
            m_sync_state.notifyAll();
        }
    }

    /**
     * Respond to a server CLOSE_CONNECTION message. This will cancel all active
     * requests but allow them to be resent on a new connection. This will also
     * be called by a server with this as a delegate.
     */
    void server_pause(org.omg.CORBA.SystemException ex) {
        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
            getLogger().debug("server_pause invocation caused by", ex);
        }
        IIOPServerChannel peer = null;
        ServerRequest[] serverRequests = null;
        synchronized (m_sync_state) {
            if (m_state != STATE_CONNECTED) {
                if (getLogger().isDebugEnabled() && Trace.isHigh()) {
                    getLogger().debug(this + " already closed");
                }
                return;
            }
            m_state = STATE_PAUSED;
            if (m_server_peer != null) {
                Map actreq = m_server_peer.getActiveRequestMap();
                if (!actreq.isEmpty()) {
                    serverRequests = new ServerRequest[actreq.size()];
                    actreq.values().toArray(serverRequests);
                    actreq.clear();
                }
                peer = m_server_peer;
                m_server_peer.setState(ServerChannel.STATE_CLOSED);
                m_server_peer.setClientPeer(null);
                m_server_peer = null;
            }
            m_paused = true;
        }
        Object name;
        if (m_delegated) {
            name = peer;
            peer.getServerManager().unregister_channel(peer);
        } else {
            name = this;
            m_client_manager.unregister_channel(this);
        }
        m_socket_queue.close();
        IIOPClientRequest[] clientRequests = null;
        synchronized (m_sync_state) {
            m_paused = false;
            m_sync_state.notifyAll();
            if (!m_active_requests.isEmpty()) {
                clientRequests = new IIOPClientRequest[m_active_requests.size()];
                m_active_requests.values().toArray(clientRequests);
                m_active_requests.clear();
            }
            if (getLogger().isDebugEnabled() && Trace.isHigh()) {
                getLogger().debug(name.toString() + (m_delegated ? "paused by client" : "paused by server"));
            }
        }
        if (clientRequests != null) {
            for (int i = 0; i < clientRequests.length; ++i) {
                clientRequests[i].cancel(ex);
            }
        }
        if (serverRequests != null) {
            for (int i = 0; i < serverRequests.length; ++i) {
                serverRequests[i].client_cancel();
            }
        }
    }

    /**
     * Change to the CLOSED state and reject new requests
     * by throwing a system exception with status COMPLETED_NO.
     * If kill_requests is true, call cancel on any active
     * ClientRequests, otherwise wait until they complete before closing.
     */
    public void close(boolean kill_requests, org.omg.CORBA.SystemException ex) {
        if (m_delegated) {
            if (kill_requests) {
                m_server_peer.close();
            } else {
                m_server_peer.soft_close(m_pending_close);
            }
            return;
        }
        IIOPClientRequest[] clientRequests = null;
        ServerRequest[] serverRequests = null;
        synchronized (m_sync_state) {
            if (m_state == STATE_PAUSED) {
                boolean interrupted = false;
                while (m_paused) {
                    try {
                        m_sync_state.wait();
                    } catch (InterruptedException ir) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                m_close_exception = ex;
                m_state = STATE_CLOSED;
                return;
            } else if (m_state == STATE_CLOSED) {
                return;
            }
            m_state = STATE_CLOSED;
            m_close_exception = ex;
            if (!kill_requests && active_requests() > 0) {
                m_pending_close = true;
                if (m_server_peer != null) {
                    m_server_peer.setPendingClose();
                }
                return;
            }
            clientRequests = new IIOPClientRequest[m_active_requests.size()];
            m_active_requests.values().toArray(clientRequests);
            m_active_requests.clear();
            if (m_server_peer != null) {
                Map actreq = m_server_peer.getActiveRequestMap();
                serverRequests = new ServerRequest[actreq.size()];
                actreq.values().toArray(serverRequests);
                actreq.clear();
                m_server_peer.setState(ServerChannel.STATE_CLOSED);
                m_server_peer.setClientPeer(null);
                m_server_peer = null;
            }
        }
        m_client_manager.unregister_channel(this);
        m_socket_queue.close();
        if (getLogger().isDebugEnabled() && Trace.isHigh()) {
            getLogger().debug(this + " closed");
        }
        if (clientRequests != null) {
            for (int i = 0; i < clientRequests.length; ++i) {
                clientRequests[i].cancel(ex);
            }
        }
        if (serverRequests != null) {
            for (int i = 0; i < serverRequests.length; ++i) {
                serverRequests[i].client_cancel();
            }
        }
    }

    /**
     * Create a request. If this is the first request on this channel
     * then client_connect will be called on all ChannelInterceptor
     * before returning the request. This may throw a system exception if the
     * channel cannot establish a connection for some reason, for example
     * INV_POLICY if client side policies prevent a successfull invocation,
     * COMM_FAILURE if a communication problem occours, or REBIND if channel
     * is temporaraly closed and a NO_REBIND policy is in effect.
     *
     * @param target The target of the request.
     * @param address The target address. If the target has been redirected
     *            this may not correspond to the target's ior.
     */
    public ClientRequest create_request(org.omg.CORBA.Object target, Address address, String operation, boolean response_expected) throws RebindChannelException {
        int request_id = RequestIDAllocator.get_request_id() << 1;
        if (m_delegated) {
            request_id--;
        }
        IIOPClientRequest request = new IIOPClientRequest(request_id, target, address, this, operation, response_expected);
        synchronized (m_sync_state) {
            check_rebind(address);
            m_active_requests.put(NumberCache.getInteger(request_id), request);
        }
        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
            getLogger().debug(this + " request #" + request_id + " created");
        }
        return request;
    }

    /**
     * Create a locate request. This may throw a system exception if the
     * channel cannot establish a connection for some reason, for example
     * INV_POLICY if client side policies prevent a successfull invocation,
     * COMM_FAILURE if a communication problem occours, or REBIND if channel
     * is temporaraly closed and a NO_REBIND policy is in effect.
     *
     * @param target The target of the request.
     * @param address The target address. If the target has been redirected
     *            this may not correspond to the target's ior.
     */
    public ClientRequest create_locate_request(org.omg.CORBA.Object target, Address address) throws RebindChannelException {
        int request_id = RequestIDAllocator.get_request_id() << 1;
        if (m_delegated) {
            request_id--;
        }
        IIOPClientRequest request = new IIOPClientRequest(request_id, target, address, this);
        synchronized (m_sync_state) {
            check_rebind(address);
            m_active_requests.put(NumberCache.getInteger(request_id), request);
        }
        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
            getLogger().debug(this + " locate request #" + request_id + " created");
        }
        return request;
    }

    /**
     * Check for channel rebind. m_syncState should be owned.
     */
    private void check_rebind(Address address) throws RebindChannelException {
        if (m_delegated && m_server_peer == null) {
            m_client_protocol.rebindBidirDelegate(this, (IIOPAddress) address);
            return;
        }
    }

    /**
     * Send a cancel message for the given request or just discard it.
     */
    boolean cancel_request(IIOPClientRequest request, boolean sendMesg) {
        int req_id = request.request_id();
        boolean closeChan = false;
        synchronized (m_sync_state) {
            if (m_state == STATE_CLOSED) {
                return false;
            }
            if (m_pending_service_ctxt && !request.is_locate()) {
                connectionSCCanceled();
                m_pending_service_ctxt = false;
                m_sync_state.notifyAll();
            }
            if (m_active_requests.isEmpty() || m_active_requests.remove(NumberCache.getInteger(req_id)) == null) {
                return false;
            }
            if (m_active_requests.isEmpty()) {
                m_channel_age = RequestIDAllocator.peek_request_id();
                closeChan = m_pending_close && (m_server_peer == null || m_server_peer.getActiveRequestMap().isEmpty());
            }
        }
        if (getLogger().isDebugEnabled() && Trace.isMedium()) {
            getLogger().debug(this + " request #" + req_id + " canceled");
        }
        if (closeChan) {
            if (m_delegated) {
                m_server_peer.soft_close(false);
            } else {
                close(false, m_close_exception);
            }
            return true;
        }
        if (sendMesg) {
            long size = 4L;
            byte[] cancel = new byte[] { (byte) 'G', (byte) 'I', (byte) 'O', (byte) 'P', m_version.major, m_version.minor, (byte) 0, (byte) org.omg.GIOP.MsgType_1_1._CancelRequest, (byte) (size >>> 24), (byte) (size >>> 16), (byte) (size >>> 8), (byte) (size), (byte) (req_id >>> 24), (byte) (req_id >>> 16), (byte) (req_id >>> 8), (byte) req_id };
            StorageBuffer buf = new StorageBuffer(cancel, 0, cancel.length);
            m_socket_queue.send(buf);
        }
        return true;
    }

    /**
     * Create output stream for request or throw a system exception
     */
    org.omg.CORBA_2_3.portable.OutputStream begin_marshal(IIOPClientRequest req) {
        org.omg.IIOP.Version iiopvers = ((IIOPAddress) req.address()).get_version();
        org.omg.GIOP.Version req_vers = new org.omg.GIOP.Version((byte) 1, (byte) 0);
        short req_disposition;
        synchronized (m_sync_state) {
            if (m_version.minor < iiopvers.minor) {
                m_version.minor = iiopvers.minor;
            }
            req_vers.minor = m_version.minor;
            boolean isRequest = !req.is_locate();
            if (isRequest) {
                while (m_paused || m_pending_service_ctxt) {
                    try {
                        m_sync_state.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
            request_open(req.target(), req.address());
            if (isRequest) {
                m_pending_service_ctxt = connectionSCAdd(req);
            }
            req_disposition = m_disposition;
        }
        MarshalBuffer buf = new MarshalBuffer(new MarshalListener(), req);
        buf.enableLogging(getLogger().getChildLogger("mb"));
        CDROutputStream os;
        try {
            os = (CDROutputStream) m_os_ctor.newInstance(new Object[] { req.orb(), req_vers, buf });
            if (LogEnabled.class.isAssignableFrom(os.getClass())) {
                ((LogEnabled) os).enableLogging(getLogger().getChildLogger("os"));
            }
        } catch (final Exception ex) {
            getLogger().error("Unable to create CDROutputStream class.", ex);
            throw ExceptionTool.initCause(new INITIALIZE("Unable to create CDROutputStream class (" + ex + ")"), ex);
        }
        os.setCodesets(m_tcsc, m_tcsw);
        int req_id = req.request_id();
        if (req.is_request()) {
            new HeaderBlock((byte) org.omg.GIOP.MsgType_1_1._Request, req_id, os);
            switch(req_vers.minor) {
                case 0:
                case 1:
                    org.omg.IOP.ServiceContextListHelper.write(os, req.get_request_service_contexts());
                    os.write_long(req_id);
                    os.write_boolean(req.response_expected());
                    org.omg.CORBA.OctetSeqHelper.write(os, req.address().getTargetAddress(org.omg.GIOP.KeyAddr.value).object_key());
                    os.write_string(req.operation());
                    os.write_long(0);
                    break;
                case 2:
                    os.write_long(req_id);
                    os.allowFragment();
                    switch(req.sync_scope()) {
                        case org.omg.Messaging.SYNC_NONE.value:
                        case org.omg.Messaging.SYNC_WITH_TRANSPORT.value:
                            os.write_octet((byte) 0);
                            break;
                        case org.omg.Messaging.SYNC_WITH_SERVER.value:
                            os.write_octet((byte) 1);
                            break;
                        case org.omg.Messaging.SYNC_WITH_TARGET.value:
                            os.write_octet((byte) 3);
                            break;
                    }
                    os.write_octet_array(RESERVED, 0, 3);
                    org.omg.GIOP.TargetAddressHelper.write(os, req.address().getTargetAddress(req_disposition));
                    os.write_string(req.operation());
                    org.omg.IOP.ServiceContextListHelper.write(os, req.get_request_service_contexts());
                    os.pending_alignment(8);
                    break;
            }
        } else {
            new HeaderBlock((byte) org.omg.GIOP.MsgType_1_1._LocateRequest, req_id, os);
            os.write_long(req_id);
            switch(req_vers.minor) {
                case 0:
                case 1:
                    {
                        byte[] oid = req.address().getTargetAddress(org.omg.GIOP.KeyAddr.value).object_key();
                        org.omg.CORBA.OctetSeqHelper.write(os, oid);
                    }
                    break;
                case 2:
                    os.allowFragment();
                    org.omg.GIOP.TargetAddressHelper.write(os, req.address().getTargetAddress(req_disposition));
                    break;
            }
        }
        return os;
    }

    /**
     * Add any neccicary conneciton SCs, and set the status to pending.
     * @return true if a connection SC is now pending
     */
    protected boolean connectionSCAdd(IIOPClientRequest req) {
        if (m_service_ctxts_sent == null) {
            return false;
        }
        boolean addSent = false;
        boolean addPend = false;
        if (m_service_ctxts_sent[CONN_SC_CODESETS] == CONN_SC_STATUS_UNSENT && m_version.minor > 0) {
            if (m_tcsc != 0 || m_tcsw != 0) {
                byte[] data = new byte[] { 0, 0, 0, 0, (byte) (m_tcsc >>> 24), (byte) (m_tcsc >>> 16), (byte) (m_tcsc >>> 8), (byte) m_tcsc, (byte) (m_tcsw >>> 24), (byte) (m_tcsw >>> 16), (byte) (m_tcsw >>> 8), (byte) m_tcsw };
                ServiceContext codesetSC = new ServiceContext(TAG_CODE_SETS.value, data);
                req.add_request_service_context(codesetSC, true);
                m_service_ctxts_sent[CONN_SC_CODESETS] = CONN_SC_STATUS_PEND;
                addPend = true;
            } else {
                m_service_ctxts_sent[CONN_SC_CODESETS] = CONN_SC_STATUS_SENT;
                addSent = true;
            }
        }
        if (m_service_ctxts_sent[CONN_SC_BIDIR] == CONN_SC_STATUS_UNSENT) {
            org.omg.IOP.ServiceContext sc = m_client_protocol.getBiDirSC();
            if (sc != null) {
                BidirectionalPolicy pol = null;
                try {
                    pol = BidirectionalPolicyHelper.narrow(req.get_request_policy(BIDIRECTIONAL_POLICY_TYPE.value));
                } catch (org.omg.CORBA.INV_POLICY ex) {
                }
                if (pol != null && pol.value() == BOTH.value) {
                    m_server_peer = new IIOPServerChannel(m_server_protocol.getServerManager(), this, m_client_protocol.getCodec());
                    if (getLogger().isDebugEnabled() && Trace.isHigh()) {
                        getLogger().debug(this + " became bidirectional with peer " + m_server_peer.toString());
                    }
                    req.add_request_service_context(sc, true);
                    m_service_ctxts_sent[CONN_SC_BIDIR] = CONN_SC_STATUS_PEND;
                    addPend = true;
                }
            } else {
                m_service_ctxts_sent[CONN_SC_BIDIR] = CONN_SC_STATUS_SENT;
                addSent = true;
            }
        }
        if (addPend) {
            return true;
        }
        if (addSent) {
            boolean allSent = true;
            for (int i = 0; i < m_service_ctxts_sent.length; ++i) {
                if (!(allSent = m_service_ctxts_sent[i] == CONN_SC_STATUS_SENT)) {
                    break;
                }
            }
            if (allSent) {
                m_service_ctxts_sent = null;
            }
        }
        return false;
    }

    /**
     * Change the status of any pending connection SCs to sent.
     */
    protected void connectionSCSent() {
        if (m_service_ctxts_sent != null) {
            boolean allSent = true;
            for (int i = 0; i < m_service_ctxts_sent.length; ++i) {
                if (m_service_ctxts_sent[i] != CONN_SC_STATUS_UNSENT) {
                    m_service_ctxts_sent[i] = CONN_SC_STATUS_SENT;
                } else {
                    allSent = false;
                }
            }
            if (allSent) {
                m_service_ctxts_sent = null;
            }
        }
    }

    /**
     * Change the status of any pending connection SCs to unsent.
     */
    protected void connectionSCCanceled() {
        if (m_service_ctxts_sent != null) {
            for (int i = 0; i < m_service_ctxts_sent.length; ++i) {
                if (m_service_ctxts_sent[i] == CONN_SC_STATUS_PEND) {
                    m_service_ctxts_sent[i] = CONN_SC_STATUS_UNSENT;
                }
            }
        }
    }

    /**
     * Change the status of all connection SCs to unsent.
     */
    protected void connectionSCReset() {
        m_service_ctxts_sent = new int[CONN_SC_TOTAL];
    }

    private class MarshalListener implements MarshalBuffer.Listener {

        private boolean m_fragment_sent = false;

        public void availIncreaced(MarshalBuffer buffer, int available, Object cookie) {
            while (available > SocketQueue.MAX_FRAG_SIZE) {
                m_fragment_sent = true;
                IIOPClientRequest request = (IIOPClientRequest) cookie;
                if (request.state() != ClientRequest.STATE_MARSHAL) {
                    return;
                }
                StorageBuffer buf = buffer.fragment(SocketQueue.MAX_FRAG_SIZE);
                if (m_socket_queue.send(buf)) {
                    if (IIOPClientChannel.this.getLogger().isDebugEnabled() && Trace.isMedium()) {
                        IIOPClientChannel.this.getLogger().info(IIOPClientChannel.this + "request #" + request.request_id() + " fragment sent");
                    }
                }
                available -= SocketQueue.MAX_FRAG_SIZE;
            }
        }

        public void bufferClosed(MarshalBuffer buffer, int available, Object cookie) {
            IIOPClientRequest request = (IIOPClientRequest) cookie;
            if (request.state() != ClientRequest.STATE_MARSHAL) {
                return;
            }
            StorageBuffer buf = buffer.lastFragment();
            if (m_socket_queue.send(buf)) {
                if (IIOPClientChannel.this.getLogger().isDebugEnabled() && Trace.isMedium()) {
                    IIOPClientChannel.this.getLogger().debug(IIOPClientChannel.this + " request #" + request.request_id() + " last fragment sent");
                }
            }
            if (m_pending_service_ctxt && !request.is_locate()) {
                synchronized (m_sync_state) {
                    connectionSCSent();
                    m_pending_service_ctxt = false;
                    m_sync_state.notifyAll();
                }
            }
        }

        public void bufferCanceled(MarshalBuffer buffer, SystemException ex, Object cookie) {
            IIOPClientRequest request = (IIOPClientRequest) cookie;
            cancel_request(request, m_fragment_sent);
            if (request.state() != ClientRequest.STATE_MARSHAL) {
                return;
            }
            ex.completed = org.omg.CORBA.CompletionStatus.COMPLETED_NO;
            request.cancel(ex);
            throw ex;
        }
    }

    /**
     * Wait the specified amount of time for an incoming message.
     *
     * @return false if the channel is closed.
     */
    public boolean recv(int timeout) {
        if (m_delegated) {
            return false;
        }
        return m_socket_queue.receive(timeout);
    }

    /**
     * Donate a thread for recieving messages. This function returns when interrupt
     * is called on the thread or the channel is closed.
     */
    public void run_recv() {
        if (m_delegated) {
            return;
        }
        while (!Thread.interrupted() && m_socket_queue.receive(s_iiop_client_recv_timeout)) {
        }
    }

    /**
     * ???
     */
    int process_reply(byte minor, CDRInputStream is, byte msg_type, boolean fragFollows, BufferSource source) {
        int reply_status = -1;
        org.omg.IOP.ServiceContext[] reply_service_contexts = null;
        is.setCodesets(m_tcsc, m_tcsw);
        int req_id = 0;
        IIOPClientRequest request = null;
        boolean closeChan = false;
        try {
            if (minor < 2 && msg_type == org.omg.GIOP.MsgType_1_1._Reply) {
                reply_service_contexts = org.omg.IOP.ServiceContextListHelper.read(is);
            }
            req_id = is.read_long();
            synchronized (m_sync_state) {
                if (fragFollows) {
                    request = (IIOPClientRequest) m_active_requests.get(NumberCache.getInteger(req_id));
                } else {
                    request = (IIOPClientRequest) m_active_requests.remove(NumberCache.getInteger(req_id));
                    if (m_active_requests.isEmpty()) {
                        m_channel_age = RequestIDAllocator.peek_request_id();
                        closeChan = m_pending_close && (m_server_peer == null || m_server_peer.getActiveRequestMap().isEmpty());
                    }
                }
            }
            if (request == null) {
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug(this + "request #" + req_id + (fragFollows ? "" : " last") + " reply fragment discarded, no corresponding request.");
                }
            } else {
                if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                    getLogger().debug(this + "request #" + req_id + (fragFollows ? "" : " last") + " reply fragment received");
                }
                request.setReplySource(source);
                reply_status = is.read_long();
                if (minor >= 2) {
                    if (msg_type == org.omg.GIOP.MsgType_1_1._Reply) {
                        reply_service_contexts = org.omg.IOP.ServiceContextListHelper.read(is);
                    }
                    try {
                        is.alignment(8);
                    } catch (org.omg.CORBA.MARSHAL ex1) {
                    }
                }
            }
        } catch (org.omg.CORBA.SystemException ex) {
            if (request == null) {
                close(true, m_close_exception);
            } else {
                request.cancel(ex);
            }
            return req_id;
        } finally {
            source.removeWaitingForBufferListener(null);
        }
        if (request != null) {
            request.handle_reply(reply_status, reply_service_contexts, is);
        }
        if (closeChan) {
            if (m_delegated) {
                m_server_peer.soft_close(false);
            } else {
                close(false, m_close_exception);
            }
        }
        return req_id;
    }

    void process_fragment(int req_id, StorageBuffer buf, boolean fragFollows) {
        IIOPClientRequest request;
        boolean closeChan = false;
        synchronized (m_sync_state) {
            if (fragFollows) {
                request = (IIOPClientRequest) m_active_requests.get(NumberCache.getInteger(req_id));
            } else {
                request = (IIOPClientRequest) m_active_requests.remove(NumberCache.getInteger(req_id));
                if (m_active_requests.isEmpty()) {
                    m_channel_age = RequestIDAllocator.peek_request_id();
                    closeChan = m_pending_close && (m_server_peer == null || m_server_peer.getActiveRequestMap().isEmpty());
                }
            }
        }
        if (request == null) {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug(this + "request #" + req_id + (fragFollows ? "" : " last") + " reply fragment discarded, no corresponding request.");
            }
        } else {
            if (getLogger().isDebugEnabled() && Trace.isMedium()) {
                getLogger().debug(this + " request #" + req_id + (fragFollows ? "" : " last") + " reply fragment.");
            }
            request.getReplySource().addLast(buf, !fragFollows);
        }
        if (closeChan) {
            if (m_delegated) {
                m_server_peer.soft_close(false);
            } else {
                close(false, m_close_exception);
            }
        }
    }

    /**
     * Finalize method closes channel.
     */
    protected void finalize() throws Throwable {
        close(false, new org.omg.CORBA.BAD_INV_ORDER(org.omg.CORBA.OMGVMCID.value | 4, org.omg.CORBA.CompletionStatus.COMPLETED_NO));
    }

    private Logger getLogger() {
        return m_logger;
    }
}
