package ch.unifr.nio.framework;

import ch.unifr.nio.framework.transform.ChannelReader;
import ch.unifr.nio.framework.transform.ChannelWriter;

/**
 * An abstract ChannelHandler with a ChannelReader, a ChannelWriter and a
 * HandlerAdapter
 *
 * @author Ronny Standtke <Ronny.Standtke@gmx.net>
 */
public abstract class AbstractChannelHandler implements ChannelHandler {

    /**
     * the ChannelReader for this ChannelHandler
     */
    protected final ChannelReader channelReader;

    /**
     * the ChannelWriter for this ChannelHandler
     */
    protected final ChannelWriter channelWriter;

    /**
     * the reference to the HandlerAdapter
     */
    protected HandlerAdapter handlerAdapter;

    /**
     * creates a new AbstractChannelHandler with the following properties:<br>
     * <ul> <li> channelReader: non-direct {@link ChannelReader} (initial
     * capacity = 1 kbyte, max capacity = 10 kbyte) </li> <li> channelWriter:
     * non-direct {@link ChannelWriter} </li> <li> stores a reference to its {@link HandlerAdapter}
     * for changing interest ops </li> </ul>
     */
    public AbstractChannelHandler() {
        channelReader = new ChannelReader(false, 1024, 10240);
        channelWriter = new ChannelWriter(false);
    }

    /**
     * creates a new AbstractChannelHandler
     *
     * @param directReading if
     * <code>true</code>, a direct buffer is used for reading, otherwise a
     * non-direct buffer
     * @param initialReadingCapacity the initial capacity of the reading buffer
     * in byte
     * @param maxReadingCapacity the maximum capacity of the reading buffer in
     * byte
     * @param directWriting if
     * <code>true</code>, a direct buffer is used for buffering data from
     * incomplete write operations, otherwise a non-direct buffer
     */
    public AbstractChannelHandler(boolean directReading, int initialReadingCapacity, int maxReadingCapacity, boolean directWriting) {
        channelReader = new ChannelReader(directReading, initialReadingCapacity, maxReadingCapacity);
        channelWriter = new ChannelWriter(directWriting);
    }

    @Override
    public void channelRegistered(HandlerAdapter handlerAdapter) {
        this.handlerAdapter = handlerAdapter;
    }

    @Override
    public ChannelReader getChannelReader() {
        return channelReader;
    }

    @Override
    public ChannelWriter getChannelWriter() {
        return channelWriter;
    }
}
