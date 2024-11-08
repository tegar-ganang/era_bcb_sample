package javaclient3.structures.mcom;

import javaclient3.structures.*;

/**
 * Configuration request to the device.
 * @author Radu Bogdan Rusu
 * @version
 * <ul>
 *      <li>v3.0 - Player 3.0 supported
 * </ul>
 */
public class PlayerMcomConfig implements PlayerConstants {

    private int command;

    private int type;

    private int channel_count;

    private int[] channel = new int[MCOM_CHANNEL_LEN];

    private PlayerMcomData data;

    /**
     * @return  Which request.  Should be one of the defined request ids.
     */
    public synchronized int getCommand() {
        return this.command;
    }

    /**
     * @param newCommand  Which request.  Should be one of the defined request ids.
     */
    public synchronized void setCommand(int newCommand) {
        this.command = newCommand;
    }

    /**
     * @return  The "type" of the data.
     */
    public synchronized int getType() {
        return this.type;
    }

    /**
     * @param newType  The "type" of the data.
     */
    public synchronized void setType(int newType) {
        this.type = newType;
    }

    /**
     * @return  length of channel name
     */
    public synchronized int getChannel_count() {
        return this.channel_count;
    }

    /**
     * @param newChannel_count  length of channel name
     */
    public synchronized void setChannel_count(int newChannel_count) {
        this.channel_count = newChannel_count;
    }

    /**
     * @return  The name of the channel.
     */
    public synchronized int[] getChannel() {
        return this.channel;
    }

    /**
     * @param newChannel  The name of the channel.
     */
    public synchronized void setChannel(int[] newChannel) {
        this.channel = newChannel;
    }

    /**
     * @return  The data.
     */
    public synchronized PlayerMcomData getData() {
        return this.data;
    }

    /**
     * @param newData  The data.
     */
    public synchronized void setData(PlayerMcomData newData) {
        this.data = newData;
    }
}
