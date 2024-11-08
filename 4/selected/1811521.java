package org.boticelli.plugin.chat;

import java.util.List;
import org.apache.log4j.Logger;
import org.boticelli.Bot;
import org.boticelli.plugin.HelpfulBoticelliPlugin;
import org.boticelli.plugin.PluginResult;
import f00f.net.irc.martyr.InCommand;
import f00f.net.irc.martyr.commands.MessageCommand;

public class BoticelliChatPlugin implements HelpfulBoticelliPlugin {

    protected static Logger log = Logger.getLogger(BoticelliChatPlugin.class);

    private ChatMessageHub chatMessageHub;

    private String secret;

    public BoticelliChatPlugin(ChatMessageHub chatMessageHub, String secret) {
        this.chatMessageHub = chatMessageHub;
        this.secret = secret;
    }

    public String getHelpName() {
        return "chat";
    }

    public String helpText(Bot bot, List<String> args) {
        return "You can use my web application as chat client.";
    }

    public PluginResult handle(Bot bot, InCommand command) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("enter handle(" + bot + ", " + command + ")");
        }
        boolean isPrivateToUs = false;
        if (command instanceof MessageCommand) {
            MessageCommand msg = (MessageCommand) command;
            isPrivateToUs = msg.isPrivateToUs(bot.getState());
        }
        ChatMessage chatMessage = ChatMessage.create(command, bot.getChannelName(), isPrivateToUs);
        if (chatMessage != null) {
            chatMessageHub.queueMessage(secret, chatMessage);
        }
        if (log.isDebugEnabled()) {
            log.debug("exit handle(" + bot + ", " + command + ")");
        }
        return PluginResult.NEXT;
    }

    public boolean supports(Class<? extends InCommand> inCommandType) {
        return true;
    }
}
