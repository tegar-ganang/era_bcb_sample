package blockTest;

import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.sgs.app.*;

/**
 * the actual server for the game
 * @author Jack
 *
 */
public class BlockWorldServer implements Serializable, AppListener {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(BlockWorldServer.class.getName());

    ManagedReference<Channel> channel = null;

    public void initialize(Properties p) {
        System.out.println("initializing block world server!");
        ChannelManager cm = AppContext.getChannelManager();
        Channel c = cm.createChannel("channel 1", new BlockWorldChannelListener(), Delivery.RELIABLE);
        channel = AppContext.getDataManager().createReference(c);
    }

    public ClientSessionListener loggedIn(ClientSession cs) {
        logger.log(Level.INFO, "a user tried to log on then was NOT kicked");
        return new BlockWorldSessionListener(cs, channel);
    }
}
