package de.iritgo.openmetix.framework.base;

import de.iritgo.openmetix.core.Engine;
import de.iritgo.openmetix.core.base.BaseObject;
import de.iritgo.openmetix.core.iobject.IObjectProxyEvent;
import de.iritgo.openmetix.core.iobject.IObjectProxyListener;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import de.iritgo.openmetix.framework.base.action.ProxyServerAction;

/**
 * NetworkProxyManager.
 *
 * @version $Id: NetworkProxyManager.java,v 1.1 2005/04/24 18:10:46 grappendorf Exp $
 */
public class NetworkProxyManager extends BaseObject implements IObjectProxyListener {

    public NetworkProxyManager() {
        super("networkproxymanager");
        Engine.instance().getEventRegistry().addListener("proxyupdate", this);
    }

    public void proxyEvent(IObjectProxyEvent event) {
        double channelNumber = AppContext.instance().getChannelNumber();
        ClientTransceiver clientTransceiver = new ClientTransceiver(channelNumber);
        clientTransceiver.addReceiver(channelNumber);
        ProxyServerAction action = new ProxyServerAction(event.getUniqueId(), -1);
        action.setTransceiver(clientTransceiver);
        ActionTools.sendToServer(action);
    }
}
