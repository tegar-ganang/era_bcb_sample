package free.jin.event;

import free.jin.ChannelsConnection;
import free.jin.event.JinEvent;

public class ChannelsEvent extends JinEvent {

    /**
     * The id specifying event fired when channel is added to user's channels list.
     */
    public static final int CHANNEL_ADDED = 1;

    /**
     * The id specifying event fired when channel is removed from user's channels
     * list.
     */
    public static final int CHANNEL_REMOVED = 2;

    /**
     * The id specifying event fired when channels list is received from the
     * server.
     */
    public static final int USER_CHANNEL_LIST_RECEIVED = 3;

    /**
     * The id of the event - it decides what kind of method in ChannelsListener
     *  will be fired.
     */
    private final int id;

    /**
     * The number of the channel.
     */
    public int channelNumber;

    /**
     * The numbers of user's channels list. Will be <code>null</code> when
     * id != USER_CHANNEL_LIST_RECEIVED
     */
    public int[] channelsNumbers;

    /** Creates a new instance of ChannelsEvent */
    public ChannelsEvent(ChannelsConnection conn, int id, int channelNumber, int[] channelsNumbers) {
        super(conn);
        switch(id) {
            case CHANNEL_ADDED:
            case CHANNEL_REMOVED:
            case USER_CHANNEL_LIST_RECEIVED:
                break;
            default:
                throw new IllegalArgumentException("Bad id: " + id);
        }
        this.id = id;
        this.channelNumber = channelNumber;
        this.channelsNumbers = channelsNumbers;
    }

    /**
     * Returns id of the event helping to determine what to do with the channel.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the channel number the event is referring to.
     */
    public int getChannelNumber() {
        return channelNumber;
    }

    /**
     * Retruns the numbers of channels contained in the channel list. 
     */
    public int[] getChannelsNumbers() {
        return channelsNumbers;
    }
}
