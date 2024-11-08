package de.iritgo.openmetix.framework.base.action;

import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import java.io.IOException;

/**
 * AliveCheckAction.
 *
 * @version $Id: AliveCheckAction.java,v 1.1 2005/04/24 18:10:43 grappendorf Exp $
 */
public class AliveCheckAction extends FrameworkAction {

    public static final int SERVER = 1;

    public static final int CLIENT = 2;

    private int source;

    public AliveCheckAction() {
    }

    public AliveCheckAction(int source) {
        this.source = source;
    }

    public void readObject(FrameworkInputStream stream) throws IOException, ClassNotFoundException {
        source = stream.readInt();
    }

    public void writeObject(FrameworkOutputStream stream) throws IOException {
        stream.writeInt(source);
    }

    public void perform() {
        if (source == CLIENT) {
            ((ClientTransceiver) getTransceiver()).getConnectedChannel().setAliveCheckSent(false);
            return;
        }
        double channelNumber = AppContext.instance().getChannelNumber();
        ClientTransceiver clientTransceiver = new ClientTransceiver(channelNumber);
        clientTransceiver.addReceiver(channelNumber);
        AliveCheckServerAction aliveCheckServerAction = new AliveCheckServerAction(source);
        aliveCheckServerAction.setTransceiver(clientTransceiver);
        ActionTools.sendToServer(aliveCheckServerAction);
    }
}
