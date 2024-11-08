package de.iritgo.openmetix.framework.client.command;

import de.iritgo.openmetix.core.command.Command;
import de.iritgo.openmetix.core.gui.IDisplay;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import de.iritgo.openmetix.framework.client.Client;
import de.iritgo.openmetix.framework.user.action.RegisterNewUserServerAction;
import java.util.Properties;

/**
 * RegisterNewUser.
 *
 * @version $Id: RegisterNewUser.java,v 1.1 2005/04/24 18:10:45 grappendorf Exp $
 */
public class RegisterNewUser extends Command {

    private String nickname;

    private String email;

    private AppContext appContext;

    public RegisterNewUser(String nickname, String email) {
        appContext = AppContext.instance();
        this.nickname = nickname;
        this.email = email;
    }

    public void setProperties(Properties properties) {
    }

    public void perform() {
        IDisplay display = Client.instance().getClientGUI().getDesktopManager().getDisplay("main.connect");
        if (display != null) {
            display.close();
            Client.instance().getClientGUI().getDesktopManager().removeDisplay(display);
        }
        double channelNumber = appContext.getChannelNumber();
        ClientTransceiver clientTransceiver = new ClientTransceiver(channelNumber);
        clientTransceiver.addReceiver(channelNumber);
        RegisterNewUserServerAction registerNewUserServerAction = new RegisterNewUserServerAction(nickname, email);
        registerNewUserServerAction.setTransceiver(clientTransceiver);
        ActionTools.sendToServer(registerNewUserServerAction);
    }

    public boolean canPerform() {
        return appContext.isConnectedWithServer();
    }
}
