package consciouscode.bonsai.channels;

import java.util.EventObject;

/**
    Information regarding a change to the data in a {@link Channel}.
*/
public class ChannelEvent extends EventObject {

    /**
       Constructs a new event indicating a value change in a source channel.

       @param source indicates the channel that has been updated.
       It must not be <code>null</code>.
    */
    public ChannelEvent(@SuppressWarnings("hiding") Channel source, Object oldValue, Object newValue) {
        super(source);
        myOldValue = oldValue;
        myNewValue = newValue;
    }

    /**
       Returns the channel that is the source of this event.  This is the same
       value returned from {@link #getSource()} cast to a {@link Channel}.
    */
    public final Channel getChannel() {
        return (Channel) source;
    }

    public final Object getOldValue() {
        return myOldValue;
    }

    public final Object getNewValue() {
        return myNewValue;
    }

    private Object myOldValue;

    private Object myNewValue;
}
