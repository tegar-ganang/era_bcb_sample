package com.googlecode.lawu.net.irc.cmd;

import static com.googlecode.lawu.util.Iterators.iterator;
import com.googlecode.lawu.net.irc.Entity;
import com.googlecode.lawu.net.irc.IrcClient;
import com.googlecode.lawu.net.irc.event.AbstractIrcEvent;
import com.googlecode.lawu.net.irc.event.IrcEvent;
import com.googlecode.lawu.net.irc.event.IrcEventListener;
import com.googlecode.lawu.util.iterators.UniversalIterator;

/**
 * A command to join a channel.
 * 
 * @author Miorel-Lucian Palii
 */
public class JoinCommand extends AbstractIrcCommand implements IncomingIrcCommand {

    private final String channel;

    private final String key;

    /**
	 * Builds a command to join the channel using the specified key.
	 * 
	 * @param channel
	 *            the channel to join
	 * @param key
	 *            the key to use for admittance, may be <code>null</code> if
	 *            there is no key
	 */
    public JoinCommand(String channel, String key) {
        validateChannel(channel);
        validateString("channel key", key, true, false);
        this.channel = channel;
        this.key = key;
    }

    /**
	 * Builds a command to join the channel with no key.
	 * 
	 * @param channel
	 *            the channel to join
	 */
    public JoinCommand(String channel) {
        this(channel, null);
    }

    /**
	 * Gets the channel to join.
	 * 
	 * @return the channel to join
	 */
    public String getChannel() {
        return this.channel;
    }

    /**
	 * Gets the channel key, or <code>null</code> if there isn't one.
	 * 
	 * @return the channel key
	 */
    public String getKey() {
        return this.key;
    }

    /**
	 * Checks whether or not this command includes a channel key.
	 * 
	 * @return whether there is a channel key
	 */
    public boolean hasKey() {
        return this.key != null;
    }

    @Override
    public UniversalIterator<String> getArguments() {
        return hasKey() ? iterator(this.channel, this.key) : iterator(this.channel);
    }

    @Override
    public String getCommand() {
        return "JOIN";
    }

    /**
	 * Builds a channel join command using the specified parameters.
	 * 
	 * @param param
	 *            the command parameters
	 * @return a channel join command
	 */
    public static JoinCommand build(String[] param) {
        validateParam(param, 1);
        return new JoinCommand(param[0]);
    }

    @Override
    public IrcEvent<JoinCommand> getEvent(final IrcClient client, final Entity origin) {
        return new AbstractIrcEvent<JoinCommand>(client, origin, this) {

            @Override
            protected void doTrigger(IrcEventListener listener) {
                listener.joinEvent(this);
            }
        };
    }
}
