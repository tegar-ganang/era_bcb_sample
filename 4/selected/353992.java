package de.iritgo.openmetix.framework.client.command;

import de.iritgo.openmetix.core.Engine;
import de.iritgo.openmetix.core.command.Command;
import de.iritgo.openmetix.core.iobject.IObject;
import de.iritgo.openmetix.core.iobject.IObjectProxy;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import de.iritgo.openmetix.framework.base.action.EditIObjectServerAction;

/**
 * EditPrototype.
 *
 * @version $Id: EditPrototype.java,v 1.1 2005/04/24 18:10:45 grappendorf Exp $
 */
public class EditPrototype extends Command {

    private IObject prototype;

    public EditPrototype() {
        super("editprototype");
    }

    public EditPrototype(IObject prototype) {
        this();
        this.prototype = prototype;
    }

    public void perform() {
        double channelNumber = AppContext.instance().getChannelNumber();
        ClientTransceiver clientTransceiver = new ClientTransceiver(channelNumber);
        clientTransceiver.addReceiver(channelNumber);
        IObjectProxy iObjectProxy = Engine.instance().getProxyRegistry().getProxy(prototype.getUniqueId());
        iObjectProxy.setUpToDate(false);
        EditIObjectServerAction editPrototypeServerAction = new EditIObjectServerAction(prototype);
        editPrototypeServerAction.setTransceiver(clientTransceiver);
        ActionTools.sendToServer(editPrototypeServerAction);
    }
}
