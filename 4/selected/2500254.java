package de.iritgo.openmetix.framework.action;

import de.iritgo.openmetix.core.Engine;
import de.iritgo.openmetix.core.action.Action;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.appcontext.AppContext;

/**
 * Utility methods for easier action handling.
 *
 * @version $Id: ActionTools.java,v 1.1 2005/04/24 18:10:46 grappendorf Exp $
 */
public class ActionTools {

    /**
	 * Perform an action via the processor that sends actions from the client
	 * to the server.
	 *
	 * @param action The action to execute.
	 */
    public static void sendToServer(Action action) {
        if (action.getTransceiver() == null) {
            ClientTransceiver ct = new ClientTransceiver(AppContext.instance().getChannelNumber());
            ct.addReceiver(AppContext.instance().getChannelNumber());
            action.setTransceiver(ct);
        }
        Engine.instance().getActionProcessorRegistry().get("Client.SendEntryNetworkActionProcessor").perform(action);
    }

    /**
	 * Perform an action via the processor that sends actions from the server
	 * to the client.
	 *
	 * @param action The action to execute.
	 */
    public static void sendToClient(Action action) {
        Engine.instance().getActionProcessorRegistry().get("Server.SendEntryNetworkActionProcessor").perform(action);
    }
}
