package server.tasks;

import java.io.Serializable;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.NameNotBoundException;
import com.sun.sgs.app.Task;

/**
 * 
 * Checks a given channel name to see if
 *  it still has clients, and removes the channel
 *  if it does not. This task should be called with
 *  at least a moderate delay.
 *
 */
public class CleanChannelTask implements Task, Serializable {

    private static final long serialVersionUID = 1L;

    private final String channelName;

    public CleanChannelTask(String channelName) {
        this.channelName = channelName;
    }

    @Override
    public void run() throws Exception {
        try {
            Channel chan = AppContext.getChannelManager().getChannel(channelName);
            if (!chan.hasSessions()) AppContext.getDataManager().removeObject(chan);
        } catch (NameNotBoundException e) {
        }
    }
}
