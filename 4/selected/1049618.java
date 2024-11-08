package f00f.net.irc.martyr.errors;

import f00f.net.irc.martyr.InCommand;
import f00f.net.irc.martyr.State;
import f00f.net.irc.martyr.util.ParameterIterator;

/**
 * @author <a href="mailto:martyr@mog.se">Morgan Christiansson</a>
 * @version $Id: GenericJoinError.java 31 2004-04-01 22:02:33Z bdamm $
 */
public abstract class GenericJoinError extends GenericError {

    private String channel;

    private String comment;

    public GenericJoinError() {
    }

    protected GenericJoinError(String chan, String comment) {
        this.channel = chan;
        this.comment = comment;
    }

    protected abstract InCommand create(String channel, String comment);

    public String getChannel() {
        return channel;
    }

    public String getComment() {
        return comment;
    }

    public State getState() {
        return State.UNKNOWN;
    }

    public InCommand parse(String prefix, String identifier, String params) {
        ParameterIterator pI = new ParameterIterator(params);
        pI.next();
        String channel = (String) pI.next();
        String comment = (String) pI.next();
        return create(channel, comment);
    }
}
