package CADI.Server.Session;

import java.io.PrintStream;
import CADI.Common.Session.ServerSideSession;
import CADI.Server.Cache.ServerCacheModel;
import CADI.Server.LogicalTarget.JPEG2000.JP2KServerLogicalTarget;

/**
 * This class is uses to save information about a client sessions. 
 * <p>
 * For further information about JPIP sessions, see ISO/IEC 15444-9 section B.2 
 *
 * @author Group on Interactive Coding of Images (GICI)
 * @version 1.0.2 2010/12/12
 */
public class ServerClientSession extends ServerSideSession {

    /**
	 * Constructor.
	 */
    public ServerClientSession() {
        super();
    }

    /**
	 * Creates a new session.
	 * 
	 * @param returnType
	 * @param transport
	 * 
	 * @return the session identifier
	 */
    public String createSessionTarget(JP2KServerLogicalTarget logicalTarget, String returnType, String transport) {
        if (logicalTarget == null) throw new NullPointerException();
        if (returnType == null) throw new NullPointerException();
        if (transport == null) throw new NullPointerException();
        if (targets.containsKey(logicalTarget.getTID())) throw new IllegalArgumentException("There already exist a session for this target.");
        ServerClientSessionTarget sessionTarget = new ServerClientSessionTarget(logicalTarget.getTID(), logicalTarget, returnType);
        targets.put(sessionTarget.getTID(), sessionTarget);
        String cid = sessionTarget.newChannel(transport);
        updateExpirationTime();
        return cid;
    }

    /**
	 * Returns the cache which is associated with the channel.
	 * 
	 * @param cid
	 * 
	 * @return return cache object.
	 */
    public ServerCacheModel getCache(String cid) {
        if (cid == null) throw new NullPointerException();
        ServerClientSessionTarget sessionTarget = (ServerClientSessionTarget) getSessionTarget(cid);
        if (sessionTarget != null) return sessionTarget.getCache();
        return null;
    }

    /**
   * 
   * @param cid
   * @return 
   */
    public ServerJPIPChannel getJPIPChannel(String cid) {
        if (cid == null) throw new NullPointerException();
        ServerClientSessionTarget sessionTarget = (ServerClientSessionTarget) getSessionTarget(cid);
        if (sessionTarget != null) return sessionTarget.getChannel(cid);
        return null;
    }

    /**
	 * Removes the session.
	 */
    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public String toString() {
        String str = "";
        str += getClass().getName() + " [";
        str += super.toString();
        str += "]";
        return str;
    }

    /**
	 * Prints this Server Client Session out to the specified output stream. This
	 * method is useful for debugging.
	 * 
	 * @param out an output stream.
	 */
    @Override
    public void list(PrintStream out) {
        out.println("-- Server client session --");
        super.list(out);
        out.flush();
    }
}
