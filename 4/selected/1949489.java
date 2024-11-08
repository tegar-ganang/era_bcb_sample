package network.server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import network.IOConstants;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import com.sun.sgs.app.Task;

/**
 * a tasks that sends a message to all the clients signalling that the server
 * has been closed, then closes the server after the message was sent
 * @author Jack
 *
 */
public class ServerCloseTask implements Task, Serializable {

    private static final long serialVersionUID = 1L;

    public void run() throws Exception {
        Channel c = AppContext.getChannelManager().getChannel(Server.allChannelName);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
            dos.writeByte(IOConstants.serverClose);
            c.send(null, buffer);
        } catch (IOException e) {
        }
        System.exit(0);
    }
}
