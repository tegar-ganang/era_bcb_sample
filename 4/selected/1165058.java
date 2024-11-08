package org.boticelli;

import f00f.net.irc.martyr.clientstate.ClientState;

public class BotState extends ClientState {

    private Bot bot;

    public BotState(Bot bot) {
        this.bot = bot;
    }

    /**
     * We want to use our own channel object, so we create it here.
     */
    @Override
    public void addChannel(String channel) {
        addChannel(new BotChannel(channel, bot));
        bot.joinedChannel(getChannel(channel));
    }
}
