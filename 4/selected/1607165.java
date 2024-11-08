package CADI.Common.Session;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class is used to save information about a logical target that
 * belongs to a session.
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0.1 2010/12/12
 */
public class SessionTarget {

    /**
	 * Definition in {@link CADI.Common.Network.JPIP.TargetField#tid}
	 */
    protected String tid = null;

    /**
	 * Definition in {@link CADI.Common.Network.JPIP.ServerControlField#type}
	 */
    protected String returnType = null;

    /**
	 * Contains the channels which are associated to this logical target.
	 */
    protected Map<String, JPIPChannel> channels = null;

    /**
	 * Constructor.
	 */
    public SessionTarget() {
        channels = new HashMap<String, JPIPChannel>();
    }

    /**
	 * Constructor.
	 * 
	 * @param tid
	 */
    public SessionTarget(String tid) {
        this(tid, "jpp-stream");
    }

    /**
	 * Constructor.
	 * 
	 * @param tid definition in {@link #tid}.
	 * @param returnType definition in {@link #returnType}
	 * @param transport definition in {@link CADI.Common.Network.JPIP.JPIPResponseFields#transport}.
	 */
    public SessionTarget(String tid, String returnType) {
        if (!returnType.equalsIgnoreCase("jpp-stream")) throw new IllegalArgumentException();
        this.tid = tid;
        this.returnType = returnType;
        channels = new HashMap<String, JPIPChannel>();
    }

    /**
	 * 
	 * @return
	 */
    public String newChannel() {
        return newChannel("http");
    }

    /**
	 * 
	 * @param transport
	 * @return
	 */
    public String newChannel(String transport) {
        JPIPChannel channel = new JPIPChannel(transport);
        channels.put(channel.getCid(), channel);
        return channel.getCid();
    }

    /**
	 * 
	 * @param cid
	 * @return
	 */
    public JPIPChannel getChannel(String cid) {
        if (channels.containsKey(cid)) return channels.get(cid);
        return null;
    }

    /**
	 * Returns a CID belonging to the session target.
	 * 
	 * @return
	 */
    public String getCid() {
        if (channels.isEmpty()) return null;
        Iterator<String> it = getCids().iterator();
        return it.next();
    }

    /**
	 * Returns an one-dimensional array containing the available CIDs.
	 * 
	 * @return
	 */
    public Set<String> getCids() {
        if (channels.isEmpty()) return null;
        return channels.keySet();
    }

    /**
	 * Returns the {@link #tid}.
   *
	 * @return the {@link #tid}.
   *
   * @deprecated this method has been replaced by the {@link #getTID()}.
	 */
    public final String getTid() {
        return tid;
    }

    /**
	 * Returns the {@link #tid}.
	 * @return the {@link #tid}.
	 */
    public final String getTID() {
        return tid;
    }

    public String getReturnType() {
        return returnType;
    }

    /**
	 * Checks if the channel <code>cid</code> belongs to this session target.  
	 * 
	 * @param cid the unique channel identifier.
	 * @return <code>true</code> if the channel belongs to this session target.
	 * 			Otherwise, returns <code>false</code>.
	 */
    public boolean contains(String cid) {
        return channels.containsKey(cid);
    }

    /**
	 * Closes a channel.
	 * 
	 * @param cid the unique channel identifier.
	 */
    public void closeChannel(String cid) {
        channels.remove(cid);
    }

    /**
	 * Returns the number of opened channels.
	 * 
	 * @return the number of opened channels.
	 */
    public int numOfChannels() {
        return channels.size();
    }

    /**
	 * Removes all the attributes.
	 */
    public void remove() {
        tid = null;
        channels.clear();
    }

    @Override
    public String toString() {
        String str = "";
        str += getClass().getName() + " [";
        str += " tid=" + tid;
        str += " Image Return Type=" + returnType;
        for (Map.Entry<String, JPIPChannel> entry : channels.entrySet()) {
            str += entry.getValue().toString();
        }
        str += "]";
        return str;
    }

    /**
	 * Prints this Session out to the specified output stream. This method
	 * is useful for debugging.
	 * 
	 * @param out an output stream.
	 */
    public void list(PrintStream out) {
        out.println("-- Session target --");
        out.println("tid: " + tid);
        out.println("Image return type: " + returnType);
        for (Map.Entry<String, JPIPChannel> entry : channels.entrySet()) {
            entry.getValue().list(out);
        }
        out.flush();
    }

    /**
	 * 
	 * @param tid
	 */
    protected final void setTID(String tid) {
        if (tid == null) throw new NullPointerException();
        if (this.tid != null) if (!tid.equals(this.tid)) throw new IllegalArgumentException();
        this.tid = tid;
    }
}
