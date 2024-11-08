package ar.edu.unicen.exa.server.communication.tasks;

import ar.edu.unicen.exa.server.communication.processors.PChat;
import com.sun.sgs.app.AppContext;
import com.sun.sgs.app.Channel;
import common.elearning.chat.ChatCommands;
import common.messages.IMessage;
import common.messages.requests.MsgChat;

/**
 * Esta clase procesa los mensajes de chat que van a los canales
 */
public class CleanupChannelTask extends TaskCommunication {

    /** The version of the serialized form. */
    private static final long serialVersionUID = 1;

    /** The name of the channel that we'll check. */
    private final String channelName;

    /**
	 * Instantiates a new cleanup channel task.
	 * 
	 * @param msg
	 *            the msg
	 * @param channelName
	 *            the channel name
	 */
    public CleanupChannelTask(IMessage msg, String channelName) {
        super(msg);
        this.channelName = channelName;
    }

    /**
	 * Check to see if the channel is empty; if so, close it and remove its
	 * binding.
	 */
    public void run() {
        Channel channel = AppContext.getChannelManager().getChannel(channelName);
        if (!channel.hasSessions()) {
            sendEmptyChannelNotification(channelName);
            AppContext.getDataManager().removeObject(channel);
            PChat.getINSTANCE().removeChannelfromlist(channelName);
        }
    }

    /**
	 * Send empty channel notification.
	 * 
	 * @param empty
	 *            the empty
	 */
    private void sendEmptyChannelNotification(String empty) {
        Channel global = AppContext.getChannelManager().getChannel(ChatCommands.GLOBAL_CHANNEL_NAME);
        MsgChat newChannelmsg = new MsgChat(ChatCommands.EMPTY_CHANNEL, "", empty);
        global.send(null, newChannelmsg.toByteBuffer());
    }

    /**
	 * (non-Javadoc)
	 * 
	 * @see ar.edu.unicen.exa.server.communication.tasks.TaskCommunication#factoryMethod(common.messages.IMessage)
	 */
    @Override
    public TaskCommunication factoryMethod(IMessage msg) {
        return new CleanupChannelTask(msg, channelName);
    }
}
