package be.castanea.server;

import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.AppListener;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;
import com.sun.sgs.app.Delivery;
import java.io.Serializable;
import java.util.Properties;

/**
 * Castanea
 * 2009
 * @author Geert van Leemputten, Steven Rymenans, Bart Van Hooydonck
 */
public class CastaneaServer implements AppListener, Serializable {

    public static final String WORLD = "WORLDCHANNEL";

    private static final long serialVersionUID = 1L;

    public void initialize(Properties arg0) {
        AppContext.getChannelManager().createChannel(WORLD, null, Delivery.RELIABLE);
    }

    public ClientSessionListener loggedIn(ClientSession session) {
        return new CastaneaPlayer(session);
    }
}
