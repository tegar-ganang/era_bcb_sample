package sun.rmi.transport;

import java.io.*;
import java.util.*;
import java.rmi.RemoteException;
import java.rmi.server.UID;
import sun.rmi.server.MarshalInputStream;
import sun.rmi.runtime.Log;

/**
 * Special stream to keep track of refs being unmarshaled so that
 * refs can be ref-counted locally.
 *
 * @author Ann Wollrath
 */
class ConnectionInputStream extends MarshalInputStream {

    /** indicates whether ack is required for DGC */
    private boolean dgcAckNeeded = false;

    /** Hashtable mapping Endpoints to lists of LiveRefs to register */
    private Map incomingRefTable = new HashMap(5);

    /** identifier for gc ack*/
    private UID ackID;

    /**
     * Constructs a marshal input stream using the underlying
     * stream "in".
     */
    ConnectionInputStream(InputStream in) throws IOException {
        super(in);
    }

    void readID() throws IOException {
        ackID = UID.read((DataInput) this);
    }

    /**
     * Save reference in order to send "dirty" call after all args/returns
     * have been unmarshaled.  Save in hashtable incomingRefTable.  This
     * table is keyed on endpoints, and holds objects of type
     * IncomingRefTableEntry.
     */
    void saveRef(LiveRef ref) {
        Endpoint ep = ref.getEndpoint();
        List refList = (List) incomingRefTable.get(ep);
        if (refList == null) {
            refList = new ArrayList();
            incomingRefTable.put(ep, refList);
        }
        refList.add(ref);
    }

    /**
     * Add references to DGC table (and possibly send dirty call).
     * RegisterRefs now calls DGCClient.referenced on all
     * refs with the same endpoint at once to achieve batching of
     * calls to the DGC
     */
    void registerRefs() throws IOException {
        if (!incomingRefTable.isEmpty()) {
            Set entrySet = incomingRefTable.entrySet();
            Iterator iter = entrySet.iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Endpoint ep = (Endpoint) entry.getKey();
                List refList = (List) entry.getValue();
                DGCClient.registerRefs(ep, refList);
            }
        }
    }

    /**
     * Indicate that an ack is required to the distributed
     * collector.
     */
    void setAckNeeded() {
        dgcAckNeeded = true;
    }

    /**
     * Done with input stream for remote call. Send DGC ack if necessary.
     * Allow sending of ack to fail without flagging an error.
     */
    void done(Connection c) {
        if (dgcAckNeeded) {
            Connection conn = null;
            Channel ch = null;
            boolean reuse = true;
            DGCImpl.dgcLog.log(Log.VERBOSE, "send ack");
            try {
                ch = c.getChannel();
                conn = ch.newConnection();
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeByte(TransportConstants.DGCAck);
                if (ackID == null) {
                    ackID = new UID();
                }
                ackID.write((DataOutput) out);
                conn.releaseOutputStream();
                conn.getInputStream().available();
                conn.releaseInputStream();
            } catch (RemoteException e) {
                reuse = false;
            } catch (IOException e) {
                reuse = false;
            }
            try {
                if (conn != null) ch.free(conn, reuse);
            } catch (RemoteException e) {
            }
        }
    }
}
