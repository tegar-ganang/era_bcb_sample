package com.sun.jini.discovery;

import com.sun.jini.collection.WeakIdentityMap;
import com.sun.jini.logging.Levels;
import com.sun.jini.resource.Service;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.Reference;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.MessageDigest;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.jini.core.constraint.InvocationConstraints;
import net.jini.discovery.Constants;
import net.jini.io.UnsupportedConstraintException;

/**
 * Class providing methods for implementing discovery protocol version 2.
 */
class DiscoveryV2 extends Discovery {

    private static final byte MULTICAST_ANNOUNCEMENT = 0;

    private static final byte MULTICAST_REQUEST = 1;

    private static final long NULL_FORMAT_ID = 0;

    private static final int FORMAT_ID_LEN = 8;

    private static final int MULTICAST_HEADER_LEN = 4 + 1 + 8;

    private static final int UNICAST_REQUEST_HEADER_LEN = 4 + 2;

    private static final int UNICAST_RESPONSE_HEADER_LEN = 4 + 8;

    private static final int MULTICAST_REQUEST_ENCODER = 0;

    private static final int MULTICAST_REQUEST_DECODER = 1;

    private static final int MULTICAST_ANNOUNCEMENT_ENCODER = 2;

    private static final int MULTICAST_ANNOUNCEMENT_DECODER = 3;

    private static final int UNICAST_DISCOVERY_CLIENT = 4;

    private static final int UNICAST_DISCOVERY_SERVER = 5;

    private static final int NUM_PROVIDER_TYPES = 6;

    private static final Class[] providerTypes;

    static {
        Class[] t = new Class[NUM_PROVIDER_TYPES];
        t[MULTICAST_REQUEST_ENCODER] = MulticastRequestEncoder.class;
        t[MULTICAST_REQUEST_DECODER] = MulticastRequestDecoder.class;
        t[MULTICAST_ANNOUNCEMENT_ENCODER] = MulticastAnnouncementEncoder.class;
        t[MULTICAST_ANNOUNCEMENT_DECODER] = MulticastAnnouncementDecoder.class;
        t[UNICAST_DISCOVERY_CLIENT] = UnicastDiscoveryClient.class;
        t[UNICAST_DISCOVERY_SERVER] = UnicastDiscoveryServer.class;
        providerTypes = t;
    }

    private static final WeakIdentityMap instances = new WeakIdentityMap();

    private static final Logger logger = Logger.getLogger(DiscoveryV2.class.getName());

    private final Map[] formatIdMaps;

    /**
	 * Returns DiscoveryV2 instance which uses providers loaded from the given
	 * class loader, or the current context class loader if the given loader is
	 * null.
	 */
    static DiscoveryV2 getInstance(ClassLoader loader) {
        if (loader == null) {
            loader = getContextClassLoader();
        }
        DiscoveryV2 disco;
        synchronized (instances) {
            disco = null;
            Reference softDisco = (Reference) instances.get(loader);
            if (softDisco != null) {
                disco = (DiscoveryV2) softDisco.get();
            }
        }
        if (disco == null) {
            disco = new DiscoveryV2(getProviders(loader));
            synchronized (instances) {
                instances.put(loader, new SoftReference(disco));
            }
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "returning {0}", new Object[] { disco });
        }
        return disco;
    }

    /**
	 * Returns DiscoveryV2 instance which uses the given providers.
	 */
    static DiscoveryV2 getInstance(MulticastRequestEncoder[] mre, MulticastRequestDecoder[] mrd, MulticastAnnouncementEncoder[] mae, MulticastAnnouncementDecoder[] mad, UnicastDiscoveryClient[] udc, UnicastDiscoveryServer[] uds) {
        List[] providers = new List[NUM_PROVIDER_TYPES];
        providers[MULTICAST_REQUEST_ENCODER] = asList(mre);
        providers[MULTICAST_REQUEST_DECODER] = asList(mrd);
        providers[MULTICAST_ANNOUNCEMENT_ENCODER] = asList(mae);
        providers[MULTICAST_ANNOUNCEMENT_DECODER] = asList(mad);
        providers[UNICAST_DISCOVERY_CLIENT] = asList(udc);
        providers[UNICAST_DISCOVERY_SERVER] = asList(uds);
        DiscoveryV2 disco = new DiscoveryV2(providers);
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "returning {0}", new Object[] { disco });
        }
        return disco;
    }

    private DiscoveryV2(List[] providers) {
        formatIdMaps = new Map[NUM_PROVIDER_TYPES];
        for (int i = 0; i < formatIdMaps.length; i++) {
            formatIdMaps[i] = makeFormatIdMap(providers[i]);
        }
    }

    public EncodeIterator encodeMulticastRequest(final MulticastRequest request, final int maxPacketSize, InvocationConstraints constraints) {
        if (maxPacketSize < MIN_MAX_PACKET_SIZE) {
            throw new IllegalArgumentException("maxPacketSize too small");
        }
        final InvocationConstraints absc = (constraints != null) ? constraints.makeAbsolute() : null;
        return new EncodeIterator() {

            private final Iterator entries = formatIdMaps[MULTICAST_REQUEST_ENCODER].entrySet().iterator();

            public DatagramPacket[] next() throws IOException {
                Map.Entry ent = (Map.Entry) entries.next();
                long fid = ((Long) ent.getKey()).longValue();
                MulticastRequestEncoder mre = (MulticastRequestEncoder) ent.getValue();
                DatagramBuffers db = new DatagramBuffers(Constants.getRequestAddress(), maxPacketSize, MULTICAST_REQUEST, fid);
                mre.encodeMulticastRequest(request, db, absc);
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "encoded {0} using {1}, {2}", new Object[] { request, mre, absc });
                }
                return db.getDatagrams();
            }

            public boolean hasNext() {
                return entries.hasNext();
            }
        };
    }

    public MulticastRequest decodeMulticastRequest(DatagramPacket packet, InvocationConstraints constraints, ClientSubjectChecker checker, boolean delayConstraintCheck) throws IOException {
        if (constraints != null) {
            constraints = constraints.makeAbsolute();
        }
        ByteBuffer buf = ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength()).slice();
        if (buf.remaining() < MULTICAST_HEADER_LEN) {
            throw new DiscoveryProtocolException("incomplete header");
        }
        int pv = buf.getInt();
        if (pv != PROTOCOL_VERSION_2) {
            throw new DiscoveryProtocolException("wrong protocol version: " + pv);
        }
        byte pt = buf.get();
        if (pt != MULTICAST_REQUEST) {
            throw new DiscoveryProtocolException("wrong packet type: " + pt);
        }
        long fid = buf.getLong();
        MulticastRequestDecoder mrd = (MulticastRequestDecoder) formatIdMaps[MULTICAST_REQUEST_DECODER].get(new Long(fid));
        if (mrd == null) {
            throw new DiscoveryProtocolException("unsupported format ID: " + fid);
        }
        MulticastRequest req;
        if (mrd instanceof DelayedMulticastRequestDecoder) {
            DelayedMulticastRequestDecoder dmrd = (DelayedMulticastRequestDecoder) mrd;
            req = dmrd.decodeMulticastRequest(buf, constraints, checker, delayConstraintCheck);
        } else {
            req = mrd.decodeMulticastRequest(buf, constraints, checker);
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "decoded {0} using {1}, {2}, {3}", new Object[] { req, mrd, constraints, checker });
        }
        return req;
    }

    public MulticastRequest decodeMulticastRequest(DatagramPacket packet, InvocationConstraints constraints, ClientSubjectChecker checker) throws IOException {
        return decodeMulticastRequest(packet, constraints, checker, false);
    }

    public EncodeIterator encodeMulticastAnnouncement(final MulticastAnnouncement announcement, final int maxPacketSize, InvocationConstraints constraints) {
        if (maxPacketSize < MIN_MAX_PACKET_SIZE) {
            throw new IllegalArgumentException("maxPacketSize too small");
        }
        final InvocationConstraints absc = (constraints != null) ? constraints.makeAbsolute() : null;
        return new EncodeIterator() {

            private final Iterator entries = formatIdMaps[MULTICAST_ANNOUNCEMENT_ENCODER].entrySet().iterator();

            public DatagramPacket[] next() throws IOException {
                Map.Entry ent = (Map.Entry) entries.next();
                long fid = ((Long) ent.getKey()).longValue();
                MulticastAnnouncementEncoder mae = (MulticastAnnouncementEncoder) ent.getValue();
                DatagramBuffers db = new DatagramBuffers(Constants.getAnnouncementAddress(), maxPacketSize, MULTICAST_ANNOUNCEMENT, fid);
                mae.encodeMulticastAnnouncement(announcement, db, absc);
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "encoded {0} using {1}, {2}", new Object[] { announcement, mae, absc });
                }
                return db.getDatagrams();
            }

            public boolean hasNext() {
                return entries.hasNext();
            }
        };
    }

    public MulticastAnnouncement decodeMulticastAnnouncement(DatagramPacket packet, InvocationConstraints constraints, boolean delayConstraintCheck) throws IOException {
        if (constraints != null) {
            constraints = constraints.makeAbsolute();
        }
        ByteBuffer buf = ByteBuffer.wrap(packet.getData(), packet.getOffset(), packet.getLength()).slice();
        if (buf.remaining() < MULTICAST_HEADER_LEN) {
            throw new DiscoveryProtocolException("incomplete header");
        }
        int pv = buf.getInt();
        if (pv != PROTOCOL_VERSION_2) {
            throw new DiscoveryProtocolException("wrong protocol version: " + pv);
        }
        byte pt = buf.get();
        if (pt != MULTICAST_ANNOUNCEMENT) {
            throw new DiscoveryProtocolException("wrong packet type: " + pt);
        }
        long fid = buf.getLong();
        MulticastAnnouncementDecoder mad = (MulticastAnnouncementDecoder) formatIdMaps[MULTICAST_ANNOUNCEMENT_DECODER].get(new Long(fid));
        if (mad == null) {
            throw new DiscoveryProtocolException("unsupported format ID: " + fid);
        }
        MulticastAnnouncement ann;
        if (mad instanceof DelayedMulticastAnnouncementDecoder) {
            DelayedMulticastAnnouncementDecoder dmad = (DelayedMulticastAnnouncementDecoder) mad;
            ann = dmad.decodeMulticastAnnouncement(buf, constraints, delayConstraintCheck);
        } else {
            ann = mad.decodeMulticastAnnouncement(buf, constraints);
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "decoded {0} using {1}, {2}", new Object[] { ann, mad, constraints });
        }
        return ann;
    }

    public MulticastAnnouncement decodeMulticastAnnouncement(DatagramPacket packet, InvocationConstraints constraints) throws IOException {
        return decodeMulticastAnnouncement(packet, constraints, false);
    }

    public UnicastResponse doUnicastDiscovery(Socket socket, InvocationConstraints constraints, ClassLoader defaultLoader, ClassLoader verifierLoader, Collection context) throws IOException, ClassNotFoundException {
        final int MAX_FORMATS = 0xFFFF;
        if (constraints != null) {
            constraints = constraints.makeAbsolute();
        }
        Map udcMap = formatIdMaps[UNICAST_DISCOVERY_CLIENT];
        Set fids = new HashSet();
        Exception ex = null;
        for (Iterator i = udcMap.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry ent = (Map.Entry) i.next();
            UnicastDiscoveryClient udc = (UnicastDiscoveryClient) ent.getValue();
            try {
                udc.checkUnicastDiscoveryConstraints(constraints);
                fids.add(ent.getKey());
                if (fids.size() == MAX_FORMATS) {
                    logger.log(Level.WARNING, "truncating format ID list");
                    break;
                }
            } catch (Exception e) {
                if (e instanceof UnsupportedConstraintException || e instanceof SecurityException) {
                    ex = e;
                    logger.log(Levels.HANDLED, "constraint check failed", e);
                } else {
                    throw (RuntimeException) e;
                }
            }
        }
        if (fids.isEmpty()) {
            if (ex == null) {
                throw new DiscoveryProtocolException("no supported formats");
            } else if (ex instanceof UnsupportedConstraintException) {
                throw (UnsupportedConstraintException) ex;
            } else {
                throw (SecurityException) ex;
            }
        }
        ByteBuffer outBuf = ByteBuffer.allocate(UNICAST_REQUEST_HEADER_LEN + (FORMAT_ID_LEN * fids.size()));
        outBuf.putInt(PROTOCOL_VERSION_2);
        outBuf.putShort((short) fids.size());
        for (Iterator i = fids.iterator(); i.hasNext(); ) {
            outBuf.putLong(((Long) i.next()).longValue());
        }
        OutputStream out = socket.getOutputStream();
        out.write(outBuf.array(), outBuf.arrayOffset(), outBuf.position());
        out.flush();
        ByteBuffer inBuf = ByteBuffer.allocate(UNICAST_RESPONSE_HEADER_LEN);
        new DataInputStream(socket.getInputStream()).readFully(inBuf.array(), inBuf.arrayOffset() + inBuf.position(), inBuf.remaining());
        int pv = inBuf.getInt();
        if (pv != PROTOCOL_VERSION_2) {
            throw new DiscoveryProtocolException("wrong protocol version: " + pv);
        }
        Long fid = new Long(inBuf.getLong());
        if (fid.longValue() == NULL_FORMAT_ID) {
            throw new DiscoveryProtocolException("format negotiation failed");
        }
        if (!fids.contains(fid)) {
            throw new DiscoveryProtocolException("response format ID not proposed: " + fid);
        }
        UnicastDiscoveryClient udc = (UnicastDiscoveryClient) udcMap.get(fid);
        UnicastResponse resp = udc.doUnicastDiscovery(socket, constraints, defaultLoader, verifierLoader, context, (ByteBuffer) outBuf.flip(), (ByteBuffer) inBuf.flip());
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "received {0} using {1}, {2}", new Object[] { resp, udc, constraints });
        }
        return resp;
    }

    public void handleUnicastDiscovery(UnicastResponse response, Socket socket, InvocationConstraints constraints, ClientSubjectChecker checker, Collection context) throws IOException {
        if (constraints != null) {
            constraints = constraints.makeAbsolute();
        }
        DataInputStream din = new DataInputStream(socket.getInputStream());
        int nfids = din.readUnsignedShort();
        if (nfids < 0) {
            throw new DiscoveryProtocolException("invalid format ID count: " + nfids);
        }
        ByteBuffer inBuf = ByteBuffer.allocate(UNICAST_REQUEST_HEADER_LEN + (FORMAT_ID_LEN * nfids));
        inBuf.putInt(PROTOCOL_VERSION_2);
        inBuf.putShort((short) nfids);
        din.readFully(inBuf.array(), inBuf.arrayOffset() + inBuf.position(), inBuf.remaining());
        UnicastDiscoveryServer uds = null;
        long fid = NULL_FORMAT_ID;
        Map udsMap = formatIdMaps[UNICAST_DISCOVERY_SERVER];
        while (inBuf.hasRemaining()) {
            fid = inBuf.getLong();
            UnicastDiscoveryServer s = (UnicastDiscoveryServer) udsMap.get(new Long(fid));
            if (s != null) {
                try {
                    s.checkUnicastDiscoveryConstraints(constraints);
                    uds = s;
                    break;
                } catch (Exception e) {
                    logger.log(Levels.HANDLED, "constraint check failed", e);
                }
            }
        }
        ByteBuffer outBuf = ByteBuffer.allocate(UNICAST_RESPONSE_HEADER_LEN);
        outBuf.putInt(PROTOCOL_VERSION_2);
        outBuf.putLong((uds != null) ? fid : NULL_FORMAT_ID);
        OutputStream out = socket.getOutputStream();
        out.write(outBuf.array(), outBuf.arrayOffset(), outBuf.position());
        out.flush();
        if (uds == null) {
            throw new DiscoveryProtocolException("format negotiation failed");
        }
        uds.handleUnicastDiscovery(response, socket, constraints, checker, context, (ByteBuffer) inBuf.flip(), (ByteBuffer) outBuf.flip());
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "sent {0} using {1}, {2}, {3}", new Object[] { response, uds, constraints, checker });
        }
    }

    public String toString() {
        List l = new ArrayList(NUM_PROVIDER_TYPES);
        for (int i = 0; i < NUM_PROVIDER_TYPES; i++) {
            l.add(formatIdMaps[i].values());
        }
        return "DiscoveryV2" + l;
    }

    private static ClassLoader getContextClassLoader() {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    private static List[] getProviders(final ClassLoader ldr) {
        return (List[]) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                List[] providers = new List[NUM_PROVIDER_TYPES];
                for (int i = 0; i < providers.length; i++) {
                    providers[i] = new ArrayList();
                }
                Iterator iter = Service.providers(DiscoveryFormatProvider.class, ldr);
                while (iter.hasNext()) {
                    Object obj = iter.next();
                    boolean used = false;
                    for (int i = 0; i < providerTypes.length; i++) {
                        if (providerTypes[i].isInstance(obj)) {
                            providers[i].add(obj);
                            used = true;
                        }
                    }
                    if (!used) {
                        logger.log(Level.WARNING, "unusable format provider {0}", new Object[] { obj });
                    }
                }
                return providers;
            }
        });
    }

    private static Map makeFormatIdMap(List providers) {
        Map map = new HashMap();
        for (Iterator i = providers.iterator(); i.hasNext(); ) {
            DiscoveryFormatProvider p = (DiscoveryFormatProvider) i.next();
            Long fid = new Long(computeFormatID(p.getFormatName()));
            if (map.keySet().contains(fid)) {
                logger.log(Level.WARNING, "ignoring provider {0} ({1}) with " + "conflicting format ID {2}", new Object[] { p, p.getFormatName(), fid });
                continue;
            }
            map.put(fid, p);
        }
        return map;
    }

    private static long computeFormatID(String format) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] b = md.digest(format.getBytes("UTF-8"));
            return ((b[7] & 0xFFL) << 0) + ((b[6] & 0xFFL) << 8) + ((b[5] & 0xFFL) << 16) + ((b[4] & 0xFFL) << 24) + ((b[3] & 0xFFL) << 32) + ((b[2] & 0xFFL) << 40) + ((b[1] & 0xFFL) << 48) + ((b[0] & 0xFFL) << 56);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static List asList(Object[] a) {
        return (a != null) ? Arrays.asList(a) : Collections.EMPTY_LIST;
    }

    /**
	 * Buffer factory passed to multicast request and announcement encoders.
	 */
    private static class DatagramBuffers implements DatagramBufferFactory {

        private static final int TRIM_THRESHOLD = 512;

        private final List datagrams = new ArrayList();

        private final InetAddress addr;

        private final int maxPacketSize;

        private final byte packetType;

        private final long formatId;

        DatagramBuffers(InetAddress addr, int maxPacketSize, byte packetType, long formatId) {
            this.addr = addr;
            this.maxPacketSize = maxPacketSize;
            this.packetType = packetType;
            this.formatId = formatId;
        }

        public ByteBuffer newBuffer() {
            DatagramInfo di = new DatagramInfo();
            datagrams.add(di);
            return di.getBuffer();
        }

        DatagramPacket[] getDatagrams() {
            DatagramPacket[] dp = new DatagramPacket[datagrams.size()];
            for (int i = 0; i < dp.length; i++) {
                dp[i] = ((DatagramInfo) datagrams.get(i)).getDatagram();
            }
            return dp;
        }

        private class DatagramInfo {

            private final DatagramPacket datagram;

            private final ByteBuffer buf;

            DatagramInfo() {
                datagram = new DatagramPacket(new byte[maxPacketSize], 0, addr, Constants.discoveryPort);
                buf = ByteBuffer.wrap(datagram.getData());
                buf.putInt(PROTOCOL_VERSION_2);
                buf.put(packetType);
                buf.putLong(formatId);
            }

            ByteBuffer getBuffer() {
                return buf;
            }

            DatagramPacket getDatagram() {
                int len = buf.position();
                if (buf.remaining() > TRIM_THRESHOLD) {
                    byte[] b = new byte[len];
                    System.arraycopy(datagram.getData(), 0, b, 0, len);
                    datagram.setData(b);
                }
                datagram.setLength(len);
                return datagram;
            }
        }
    }
}
