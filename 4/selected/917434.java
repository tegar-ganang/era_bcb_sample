package org.boticelli.plugin.dist;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.boticelli.Bot;
import org.boticelli.UserRuntimeException;
import org.boticelli.auth.UserAction;
import org.boticelli.auth.UserActionExecutor;
import org.boticelli.plugin.BotAware;
import org.boticelli.plugin.BoticelliPlugin;
import org.boticelli.plugin.PluginResult;
import f00f.net.irc.martyr.InCommand;
import f00f.net.irc.martyr.clientstate.Member;
import f00f.net.irc.martyr.commands.NoticeCommand;

public abstract class AbstractUserActionExecutor extends AbstractBotAwarePlugin {

    private static Logger log = Logger.getLogger(NickServBasedUserActionExecutor.class);

    protected Map<String, UserAction> actions = new HashMap<String, UserAction>();

    protected String nickServNick = "NickServ";

    public AbstractUserActionExecutor() {
        super();
    }

    public void setNickServNick(String nickServNick) {
        this.nickServNick = nickServNick;
    }

    public void execute(String nick, UserAction action) {
        actions.put(nick, action);
        initiateAction(nick, action);
    }

    public PluginResult handle(Bot bot, InCommand command) throws Exception {
        AuthenticationResponse response = matchResponse(bot, command);
        if (response != null) {
            String nick = response.getNick();
            UserAction action = actions.get(nick);
            boolean inChannel = false;
            for (Enumeration e = bot.getState().getChannel(bot.getChannelName()).getMembers(); e.hasMoreElements(); ) {
                Member member = (Member) e.nextElement();
                if (member.getNick().getNick().equals(nick)) {
                    inChannel = true;
                }
            }
            if (!inChannel) {
                bot.respond(nick, action.getResponseMode(), "You must be in the channel to " + action.getDescription());
            } else if (!response.isValid()) {
                bot.respond(nick, action.getResponseMode(), "You have to be registered with NickServ to " + action.getDescription());
            } else {
                try {
                    action.execute(bot, nick);
                } catch (UserRuntimeException e) {
                    bot.respond(nick, action.getResponseMode(), e.getMessage());
                }
            }
        }
        return PluginResult.NEXT;
    }

    public abstract boolean supports(Class<? extends InCommand> inCommandType);

    protected abstract AuthenticationResponse matchResponse(Bot bot, InCommand command);

    protected abstract void initiateAction(String nick, UserAction action);
}
