package de.iritgo.openmetix.framework.base;

import de.iritgo.openmetix.core.Engine;
import de.iritgo.openmetix.core.action.ActionProcessorRegistry;
import de.iritgo.openmetix.core.base.BaseObject;
import de.iritgo.openmetix.core.iobject.IObject;
import de.iritgo.openmetix.core.iobject.IObjectListEvent;
import de.iritgo.openmetix.core.iobject.IObjectListListener;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import de.iritgo.openmetix.framework.base.action.ProxyLinkedListAddServerAction;
import de.iritgo.openmetix.framework.base.action.ProxyLinkedListRemoveServerAction;
import de.iritgo.openmetix.framework.user.User;

/**
 * NetworkProxyLinkedListManager.
 *
 * @version $Id: NetworkProxyLinkedListManager.java,v 1.1 2005/04/24 18:10:46 grappendorf Exp $
 */
public class NetworkProxyLinkedListManager extends BaseObject implements IObjectListListener {

    public NetworkProxyLinkedListManager() {
        super("networkproxylinkedlistmanager");
        Engine.instance().getEventRegistry().addListener("proxylinkedlistupdate", this);
    }

    public void iObjectListEvent(IObjectListEvent event) {
        IObject newProt = event.getObject();
        double channelNumber = AppContext.instance().getChannelNumber();
        ClientTransceiver clientTransceiver = new ClientTransceiver(channelNumber);
        if (event.getType() == IObjectListEvent.REMOVE) {
            ProxyLinkedListRemoveServerAction action = new ProxyLinkedListRemoveServerAction(event.getOwnerObject().getUniqueId(), event.getListAttribute(), newProt);
            clientTransceiver.addReceiver(clientTransceiver.getSender());
            action.setTransceiver(clientTransceiver);
            ActionTools.sendToServer(action);
            return;
        }
        if (newProt.getUniqueId() > 0) {
            return;
        }
        long newUniqueId = newProt.getUniqueId();
        ((User) AppContext.instance().getUser()).putNewObjectsMapping(new Long(newUniqueId), new Long(newUniqueId));
        clientTransceiver.addReceiver(channelNumber);
        ProxyLinkedListAddServerAction action = new ProxyLinkedListAddServerAction(event.getOwnerObject().getUniqueId(), event.getListAttribute(), newProt);
        action.setTransceiver(clientTransceiver);
        ActionProcessorRegistry actionProcessorRegistry = Engine.instance().getActionProcessorRegistry();
        ActionTools.sendToServer(action);
    }
}
