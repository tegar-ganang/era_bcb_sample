package client.communication.msgprocessor;

import java.util.Set;
import java.util.logging.Logger;
import client.communication.GameContext;
import client.game.ui.chat.ChatClient;
import com.sun.sgs.client.ClientChannel;
import common.elearning.chat.ChatCommands;
import common.messages.IMessage;
import common.messages.MsgTypes;
import common.messages.requests.MsgChat;
import common.processors.IProcessor;
import common.processors.MsgProcessorFactory;
import common.util.ChannelNameParser;

/**
 * The Class ChatMsgProcessor.
 */
public class ChatMsgProcessor implements IProcessor {

    /** The INSTANCE. */
    private static ChatMsgProcessor INSTANCE = new ChatMsgProcessor();

    private static final Logger LOGGER = Logger.getLogger(ChatMsgProcessor.class.getName());

    /**
	 * Instantiates a new chat msg processor.
	 */
    public ChatMsgProcessor() {
    }

    /**
	 * Gets the single instance of ChatMsgProcessor.
	 * 
	 * @return single instance of ChatMsgProcessor
	 */
    public static ChatMsgProcessor getInstance() {
        return INSTANCE;
    }

    /** (non-Javadoc)
	 * @see common.processors.IProcessor#factoryMethod()
	 */
    @Override
    public IProcessor factoryMethod() {
        return ChatMsgProcessor.getInstance();
    }

    /** (non-Javadoc)
	 * @see common.processors.IProcessor#getMsgType()
	 */
    @Override
    public String getMsgType() {
        return MsgTypes.MSG_CHAT;
    }

    /** (non-Javadoc)
	 * @see common.processors.IProcessor#process(common.messages.IMessage)
	 */
    @Override
    public void process(IMessage message) {
        ChatClient ui = ChatClient.getInstance();
        MsgChat msg = (MsgChat) message;
        LOGGER.info("Processing : " + msg.getCommand() + msg.getMsg());
        if (msg.getCommand().equals(ChatCommands.PONG)) {
        } else if (msg.getCommand().equals(ChatCommands.PM)) {
            ui.joinChannel(ChatCommands.PM_PREFIX + msg.getSender() + msg.getMsg());
        } else if (msg.getCommand().equals(ChatCommands.MEMBERS)) {
            ui.setVisible(true);
            ui.setTitle("Chat Client " + GameContext.getUserName());
            ui.createChannelFrame(msg.getSender());
            ui.addUserToChannel(msg.getMsg(), msg.getSender());
        } else if (msg.getCommand().equals(ChatCommands.LOGIN_FAILED)) {
            ui.showMessage("Login failed");
        } else if (msg.getCommand().equals(ChatCommands.NEW_CHANNEL)) {
            ui.addChannel(msg.getMsg());
        } else if (msg.getCommand().equals(ChatCommands.EMPTY_CHANNEL)) {
            ui.removeChannel(msg.getMsg());
        } else if (msg.getCommand().equals(ChatCommands.CHANNEL_LIST)) {
            ui.addChannel(msg.getMsg());
        } else if (msg.getCommand().equals(ChatCommands.JOINED)) {
            ClientChannel ch;
            Set<ClientChannel> channels = GameContext.getClientCommunication().getChannelConteiner().getChannelsOfType(ChannelNameParser.parseChannelType(msg.getSender()));
            if ((channels != null) && (!channels.isEmpty())) ch = (ClientChannel) channels.toArray()[0]; else ch = null;
            if (ch != null) ui.addUserToChannel(msg.getMsg(), ch.getName());
        } else if (msg.getCommand().equals(ChatCommands.LEFT)) {
            ui.removeUserFromChannel(msg.getMsg(), msg.getSender());
        } else if (msg.getCommand().equals(ChatCommands.CHANNEL_MSG)) {
            String[] args = msg.getMsg().split(" ", 2);
            ui.addChannelMessage(args[0], msg.getSender(), args[1]);
        } else {
        }
    }

    /** (non-Javadoc)
	 * @see common.processors.IProcessor#setMsgType(java.lang.String)
	 */
    @Override
    public void setMsgType(String msgType) {
        return;
    }

    /**
	 * Configure msg processor factory.
	 */
    public static void configureMsgProcessorFactory() {
        MsgProcessorFactory.getInstance().addProcessor(MsgTypes.MSG_CHAT, INSTANCE);
    }
}
