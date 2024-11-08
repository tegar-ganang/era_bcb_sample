package de.iritgo.openmetix.comm.chat.command;

import de.iritgo.openmetix.comm.chat.action.UserLeaveServerAction;
import de.iritgo.openmetix.comm.chat.chatter.ChatClientManager;
import de.iritgo.openmetix.core.Engine;
import de.iritgo.openmetix.core.command.Command;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;

public class UserLeaveCommand extends Command {

    private String userName;

    private int channel;

    private AppContext appContext;

    public UserLeaveCommand() {
        channel = -1;
    }

    public UserLeaveCommand(int channel) {
        this.channel = channel;
    }

    public void perform() {
        appContext = AppContext.instance();
        this.userName = appContext.getUser().getName();
        double channelNumber = appContext.getChannelNumber();
        ClientTransceiver clientTransceiver = new ClientTransceiver(channelNumber);
        clientTransceiver.addReceiver(channelNumber);
        ChatClientManager chatManager = (ChatClientManager) Engine.instance().getManagerRegistry().getManager("chat.client");
        if (channel == -1) {
            channel = chatManager.getCurrentChannel().intValue();
        }
        UserLeaveServerAction userLeaveServerAction = new UserLeaveServerAction(userName, channel);
        userLeaveServerAction.setTransceiver(clientTransceiver);
        ActionTools.sendToServer(userLeaveServerAction);
    }
}
