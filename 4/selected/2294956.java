package server;

import java.io.Serializable;
import java.util.Properties;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.AppListener;
import com.sun.sgs.app.ClientSession;
import com.sun.sgs.app.ClientSessionListener;
import com.sun.sgs.app.Delivery;
import com.sun.sgs.app.NameNotBoundException;

public class DarkstrisServer implements AppListener, Serializable {

    public static final String LOBBY_CHANNEL = "lobbyChannel";

    public static final String LOBBY = "lobby";

    public static final String USER_PREFIX = "user_";

    private static final long serialVersionUID = -6106607881393546480L;

    @Override
    public void initialize(Properties props) {
        AppContext.getChannelManager().createChannel(LOBBY_CHANNEL, null, Delivery.RELIABLE);
        AppContext.getDataManager().setBinding(LOBBY, new Lobby());
    }

    @Override
    public ClientSessionListener loggedIn(ClientSession sess) {
        String username = USER_PREFIX + sess.getName();
        try {
            AppContext.getDataManager().getBinding(username);
            System.out.println("ServerSessionHandler.loggedIn(" + username + ") -> NameBound");
            return null;
        } catch (NameNotBoundException nnbe) {
            System.out.println("ServerSessionHandler.loggedIn(" + username + ") -> NameNotBound");
            return new Player(sess);
        }
    }
}
