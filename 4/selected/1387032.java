package net.sf.opendf.cal.interpreter;

/**
 * This implementation of the OutputPort interface represents ports that are not multiports.
 *
 * @author Jorn W. Janneck <janneck@eecs.berkeley.edu>
 * @see OutputPort
 */
public class SingleOutputPort implements OutputPort {

    public OutputChannel getChannel(int n) {
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

    public SingleOutputPort(String name, OutputChannel channel) {
        this.name = name;
        this.channel = channel;
    }

    private String name;

    private OutputChannel channel;

    public String toString() {
        return "(SingleOutputPort '" + name + "', " + channel.toString() + ")";
    }
}
