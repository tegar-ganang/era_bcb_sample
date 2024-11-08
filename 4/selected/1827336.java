package CADI.Server.Session;

import java.io.PrintStream;
import CADI.Common.Session.ServerSideSessionTarget;
import CADI.Server.Cache.ServerCacheModel;
import CADI.Server.LogicalTarget.JPEG2000.JP2KServerLogicalTarget;

/**
 * This class is used to save information about a logical target that
 * belongs to a session.
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0.2 2010/12/13
 */
public class ServerClientSessionTarget extends ServerSideSessionTarget {

    /**
	 * Constructor.
	 * 
	 * @param tid
	 */
    public ServerClientSessionTarget(String tid, JP2KServerLogicalTarget logicalTarget) {
        this(tid, logicalTarget, "jpp-stream");
    }

    /**
	 * Constructor.
	 * 
	 * @param tid definition in {@link #tid}.
	 * @param returnType definition in {@link #returnType}
	 */
    public ServerClientSessionTarget(String tid, JP2KServerLogicalTarget logicalTarget, String returnType) {
        super(tid, returnType);
        cache = new ServerCacheModel(logicalTarget);
    }

    /**
	 * 
	 * @return
	 */
    public ServerCacheModel getCache() {
        return (ServerCacheModel) cache;
    }

    /**
	 * 
	 * @return
	 */
    @Override
    public String newChannel() {
        return newChannel("http");
    }

    /**
	 * 
	 * @param transport
	 * @return
	 */
    @Override
    public String newChannel(String transport) {
        ServerJPIPChannel channel = new ServerJPIPChannel(transport);
        channels.put(channel.getCid(), channel);
        return channel.getCid();
    }

    /**
   * 
   * @param cid
   * @return 
   */
    @Override
    public ServerJPIPChannel getChannel(String cid) {
        assert (cid != null);
        if (!contains(cid)) return null;
        return (ServerJPIPChannel) channels.get(cid);
    }

    /**
	 * Removes all the attributes.
	 */
    @Override
    public void remove() {
        super.remove();
        cache.reset();
    }

    @Override
    public String toString() {
        String str = "";
        str += getClass().getName() + " [";
        str += super.toString();
        str += " cache=" + "<<< Not displayed >>>";
        str += "]";
        return str;
    }

    /**
	 * Prints this Session out to the specified output stream. This method
	 * is useful for debugging.
	 * 
	 * @param out an output stream.
	 */
    @Override
    public void list(PrintStream out) {
        out.println("-- Server session target --");
        super.list(out);
        out.println("cache: " + "<<< Not displayed >>>");
        out.flush();
    }
}
