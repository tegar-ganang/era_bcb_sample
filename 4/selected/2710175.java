package de.iritgo.openmetix.framework.client.command;

import de.iritgo.openmetix.core.command.Command;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import de.iritgo.openmetix.framework.user.User;
import de.iritgo.openmetix.framework.user.action.UserLoginServerAction;
import java.util.Properties;

/**
 * UserLogin.
 *
 * @version $Id: UserLogin.java,v 1.1 2005/04/24 18:10:45 grappendorf Exp $
 */
public class UserLogin extends Command {

    private String username;

    private String password;

    private AppContext appContext;

    public UserLogin() {
        appContext = AppContext.instance();
        User user = appContext.getUser();
        if (user == null) {
            return;
        }
        this.username = user.getName();
        this.password = user.getPassword();
    }

    public UserLogin(String username, String password) {
        appContext = AppContext.instance();
        this.username = username;
        this.password = password;
    }

    public void setProperties(Properties properties) {
    }

    public void perform() {
        double channelNumber = appContext.getChannelNumber();
        ClientTransceiver clientTransceiver = new ClientTransceiver(channelNumber);
        clientTransceiver.addReceiver(channelNumber);
        UserLoginServerAction userLoginServerAction = new UserLoginServerAction(username, password);
        userLoginServerAction.setTransceiver(clientTransceiver);
        ActionTools.sendToServer(userLoginServerAction);
    }

    public boolean canPerform() {
        return appContext.isConnectedWithServer();
    }
}
