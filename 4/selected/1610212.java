package jelb.commands.chat;

import jelb.Locale;
import jelb.commands.Command;
import jelb.common.IChatter;
import jelb.common.ParseException;
import jelb.common.Parser;
import jelb.netio.Protocol.Emote;
import jelb.struct.ChatableUser;

public class JoinChannelCommand extends Command<IChatter> {

    public JoinChannelCommand(String name, String commandHelp) {
        super(name, commandHelp);
    }

    @Override
    public void invoke(IChatter bot, String playerName, String[] params) {
        try {
            String channel = Parser.parseString(params, 0);
            int channelId = bot.getChannelId(channel);
            if (channelId <= 0) {
                bot.appendAnserw(playerName, Locale.MSG_AVAILABLE_CHANNELS);
                bot.sendEmote(Emote.ScratchHead);
            } else {
                ChatableUser user = bot.getChatableUser(playerName);
                if (user != null) {
                    user.joinChannel(channelId);
                    bot.appendAnserw(playerName, String.format(Locale.MSG_JOIN_CHANNEL, channel));
                }
            }
        } catch (ParseException pe) {
            this.onSyntaxError(bot, playerName);
        }
    }

    @Override
    public boolean isGlobalTradeLock() {
        return false;
    }

    @Override
    public boolean isPersonalTradeLock() {
        return false;
    }
}
