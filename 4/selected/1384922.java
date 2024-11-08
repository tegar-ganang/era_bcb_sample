package de.iritgo.openmetix.comm.chat.command;

import de.iritgo.openmetix.comm.chat.action.UserJoinServerAction;
import de.iritgo.openmetix.core.command.Command;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import java.util.Properties;

public class UserJoinCommand extends Command {

    private String userName;

    private String channel;

    private AppContext appContext;

    public UserJoinCommand() {
    }

    public UserJoinCommand(String channel, String userName) {
        appContext = AppContext.instance();
        this.userName = userName;
        this.channel = channel;
    }

    public UserJoinCommand(String channel) {
        appContext = AppContext.instance();
        this.userName = appContext.getUser().getName();
        this.channel = channel;
    }

    public void setProperties(Properties properties) {
    }

    public void perform() {
        double channelNumber = appContext.getChannelNumber();
        ClientTransceiver clientTransceiver = new ClientTransceiver(channelNumber);
        clientTransceiver.addReceiver(channelNumber);
        UserJoinServerAction userJoinServerAction = new UserJoinServerAction(userName, channel);
        userJoinServerAction.setTransceiver(clientTransceiver);
        ActionTools.sendToServer(userJoinServerAction);
    }

    public boolean canPerform() {
        return appContext.isConnectedWithServer();
    }
}
