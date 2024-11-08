package listeners;

import java.util.LinkedList;
import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.DisconnectEvent;
import shared.Message;
import shared.RoomManager;
import ui.room.Room;
import connection.Connection;
import connection.KEllyBot;

/**
 * The listener interface for receiving connection events.
 * The class that is interested in processing a connection
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addConnectionListener<code> method. When
 * the connection event occurs, that object's appropriate
 * method is invoked.
 *
 * @see ConnectionEvent
 */
public abstract class ConnectionListener extends ListenerAdapter<KEllyBot> {

    @Override
    public void onDisconnect(DisconnectEvent<KEllyBot> event) throws Exception {
        super.onDisconnect(event);
        LinkedList<Room> rooms = event.getBot().getConnection().getRooms();
        for (Room r : rooms) {
            RoomManager.enQueue(new Message(event.getBot(), "Disconnected.", KEllyBot.systemName, r.getChannelName(), Message.CONSOLE));
        }
    }

    /** The connection this listener works with. */
    Connection nc;

    /**
	 * Instantiates a new connection listener.
	 *
	 * @param nc the nc
	 */
    public ConnectionListener(Connection nc) {
        this.nc = nc;
    }

    /**
	 * Update who.
	 *
	 * @param c the c
	 */
    protected void updateWho(Channel c) {
        nc.updateWho(c.getName());
    }

    /**
	 * Bot equals user.
	 *
	 * @param u the u
	 * @return true, if successful
	 */
    protected boolean botEqualsUser(User u) {
        return u.equals(nc.getBot().getUserBot());
    }

    /**
	 * Queue message.
	 *
	 * @param m the m
	 */
    protected void queueMessage(Message m) {
        RoomManager.enQueue(m);
    }

    /**
	 * Manage message.
	 *
	 * @param m the m
	 */
    protected void manageMessage(Message m) {
        RoomManager.enQueue(m);
    }
}
