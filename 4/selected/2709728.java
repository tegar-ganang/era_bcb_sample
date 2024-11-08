package server;

import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import box.Box;
import com.sun.sgs.app.*;

/**
 * the actual server for the game
 * @author Jack
 *
 */
public class BoxWorldServer implements Serializable, AppListener {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(BoxWorldServer.class.getName());

    ManagedReference<Channel> channel = null;

    AtomicInteger owner = new AtomicInteger(0);

    public void initialize(Properties p) {
        System.out.println("initializing block world server!");
        ChannelManager cm = AppContext.getChannelManager();
        Channel c = cm.createChannel("channel 1", new BoxWorldChannelListener(), Delivery.RELIABLE);
        channel = AppContext.getDataManager().createReference(c);
        TaskManager tm = AppContext.getTaskManager();
        tm.schedulePeriodicTask(new UpdateClientGames(channel), 0, 50);
    }

    public ClientSessionListener loggedIn(ClientSession cs) {
        logger.log(Level.INFO, "a client joined the game!");
        return new BoxWorldSessionListener(cs, channel, owner);
    }
}
