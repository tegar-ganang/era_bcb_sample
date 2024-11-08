package net.sf.opendf.cal.interpreter;

/**
 * This implementation of the InputPort interface represents ports that are not multiports.
 *
 * @author Jorn W. Janneck <janneck@eecs.berkeley.edu>
 * @see InputPort
 */
public class SingleInputPort implements InputPort {

    public InputChannel getChannel(int n) {
        return channel;
    }

    public String getName() {
        return name;
    }

    public boolean isMultiport() {
        return false;
    }

    public int width() {
        return 1;
    }

    public SingleInputPort(String name, InputChannel channel) {
        this.name = name;
        this.channel = channel;
    }

    private String name;

    private InputChannel channel;

    public String toString() {
        return "(SingleInputPort '" + name + "', " + channel.toString() + ")";
    }
}
