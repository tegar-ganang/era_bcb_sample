import java.io.Serializable;
import java.util.Properties;
import java.util.logging.Logger;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.AppListener;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.ManagedReference;

/**
 * Darkstar basic server for the MundoJava project.
 *
 * @author Claudio Horvilleur.
 */
public class MundojavaServer implements AppListener, Serializable {

    /**
     * The version of the serialized form of this class.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The {@link Logger} for this class.
     */
    private static final Logger log = Logger.getLogger(MundojavaServer.class.getName());

    /**
     * The name of the base channel used to send information to all the
     * players.
     */
    public static final String BASE_CHANNEL_NAME = "MJ_BASE_CHANNEL";

    /**
     * The name of the players list in the non volatil memory.
     */
    public static final String PLAYERS_LIST_NAME = "MJ_PLAYERS_LIST";

    /**
     * {@inheritDoc}
     * <P>
     * Just print a log.
     */
    public void initialize(Properties props) {
        PlayersList playersList = new PlayersList();
        DataManager dataMgr = AppContext.getDataManager();
        dataMgr.setBinding(PLAYERS_LIST_NAME, playersList);
        ChannelManager channelMgr = AppContext.getChannelManager();
        Channel c1 = channelMgr.createChannel(BASE_CHANNEL_NAME, new BaseChannelListener(), Delivery.RELIABLE);
        log.info("Mundojava Server initialiazed");
    }

    /**
     * {@inheritDoc}
     * <P>
     * We return a player.
     */
    public ClientSessionListener loggedIn(ClientSession session) {
        return ServerPlayer.loggedIn(session);
    }
}
