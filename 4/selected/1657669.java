package cubeworld.command;

import java.util.HashMap;
import java.util.StringTokenizer;
import cubeworld.Channel;
import cubeworld.Command;
import cubeworld.Session;

/**
 * @author Garg Oyle (garg_oyle@users.sourceforge.net)
 * 
 */
public class ChannelTalk implements Command {

    /**
     * Sub command.
     *
     * @author Garg Oyle (garg_oyle@sourceforge.net)
     *
     */
    protected interface SubCommand {

        /**
         * Process a sub command on a given channel for a given session.
         * @param channel to process on
         * @param session to process for
         */
        void process(Channel channel, Session session);
    }

    ;

    /**
     * Turning on a channel.
     *
     * @author Garg Oyle (garg_oyle@sourceforge.net)
     */
    private class On implements SubCommand {

        /**
         * Process a sub command on a given channel for a given session.
         * @param channel to process on
         * @param session to process for
         */
        public void process(final Channel channel, final Session session) {
            if (channel.isRegistered(session)) {
                session.addOutput("You are already listening to channel [" + channel.getName() + "].");
            } else {
                channel.register(session);
                session.addChannel(channel.getName());
                session.addOutput("You have turned on channel [" + channel.getName() + "].");
            }
        }
    }

    /**
     * Turning off a given channel.
     *
     * @author Garg Oyle (garg_oyle@sourceforge.net)
     */
    private class Off implements SubCommand {

        /**
         * Process a sub command on a given channel for a given session.
         * @param channel to process on
         * @param session to process for
         */
        public void process(final Channel channel, final Session session) {
            channel.remove(session);
            session.removeChannel(channel.getName());
            session.addOutput("You've turned off channel [" + channel.getName() + "].");
        }
    }

    /**
     * Who listens on a given channel?
     *
     * @author Garg Oyle (garg_oyle@sourceforge.net)
     */
    private class Who implements SubCommand {

        /**
         * Process a sub command on a given channel for a given session.
         * @param channel to process on
         * @param session to process for
         */
        public void process(final Channel channel, final Session session) {
            session.addOutput("Listening on channel [" + channel.getName() + "]:");
            for (Session listener : channel.getListeners()) {
                session.addOutput("    " + listener.getName());
            }
        }
    }

    /** Known channels. */
    private HashMap<String, Channel> mChannels;

    /** Sub commands. */
    private HashMap<String, SubCommand> mCommands = new HashMap<String, SubCommand>();

    /**
     * @param channels
     *            known channels
     */
    public ChannelTalk(final HashMap<String, Channel> channels) {
        mChannels = channels;
        mCommands.put("on", new On());
        mCommands.put("off", new Off());
        mCommands.put("who", new Who());
    }

    /**
     * Process command on session if line matches.
     *
     * @param line
     *            client input
     * @param session
     *            actual session
     * @return if line matched and command was processed.
     */
    public final boolean process(final String line, final Session session) {
        StringTokenizer token = new StringTokenizer(line);
        if (!token.hasMoreTokens()) {
            return false;
        }
        String channel = token.nextToken();
        if (!isKnown(channel)) {
            return false;
        }
        if (!token.hasMoreTokens()) {
            return false;
        }
        String parameter = token.nextToken();
        if (!token.hasMoreTokens()) {
            Channel targetChannel = mChannels.get(channel);
            SubCommand command = mCommands.get(parameter);
            if (null != command) {
                command.process(targetChannel, session);
                return true;
            }
        }
        String message = line.substring(channel.length() + 1);
        channelSay(channel, message, session);
        return true;
    }

    /**
     * Register a channel to make it known to the world.
     *
     * @param channel
     *            to register.
     */
    public final void registerChannel(final Channel channel) {
        if (!mChannels.containsKey(channel.getName())) {
            mChannels.put(channel.getName(), channel);
        }
    }

    /**
     * Check if channel with given name is known.
     *
     * @param channel
     *            name to check
     * @return true when channel is known
     */
    private boolean isKnown(final String channel) {
        return mChannels.containsKey(channel);
    }

    /**
     * Send message to given channel.
     *
     * @param channel
     *            destination channel
     * @param message
     *            message to send
     * @param session
     *            the message comes from
     */
    private void channelSay(final String channel, final String message, final Session session) {
        mChannels.get(channel).send(message, session);
    }
}
