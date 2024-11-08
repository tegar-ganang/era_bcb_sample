package de.iritgo.openmetix.comm.chat.command;

import de.iritgo.openmetix.comm.chat.action.ChatMessageServerAction;
import de.iritgo.openmetix.core.Engine;
import de.iritgo.openmetix.core.command.Command;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import de.iritgo.openmetix.framework.console.ConsoleManager;
import java.util.Properties;

public class UserChatCommand extends Command {

    private String userName;

    private int channelId;

    private String message;

    private AppContext appContext;

    public UserChatCommand() {
    }

    public UserChatCommand(String message, int channelId, String userName) {
        appContext = AppContext.instance();
        this.userName = userName;
        this.channelId = channelId;
        this.message = message;
    }

    public void setProperties(Properties properties) {
    }

    public void perform() {
        double channelNumber = appContext.getChannelNumber();
        ClientTransceiver clientTransceiver = new ClientTransceiver(channelNumber);
        clientTransceiver.addReceiver(channelNumber);
        if (message.startsWith("/")) {
            ConsoleManager consoleManager = (ConsoleManager) Engine.instance().getManagerRegistry().getManager("console");
            try {
                consoleManager.doConsoleCommand(message.substring(1, message.length()));
            } catch (Exception x) {
            }
        } else {
            ChatMessageServerAction chatMessageServerAction = new ChatMessageServerAction(message, channelId, userName);
            chatMessageServerAction.setTransceiver(clientTransceiver);
            ActionTools.sendToServer(chatMessageServerAction);
        }
    }

    public boolean canPerform() {
        return appContext.isConnectedWithServer();
    }
}
