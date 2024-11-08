package ru.caffeineim.protocols.icq.setting.enumerations;

/**
 * <p>Created by
 *   @author Fabrice Michellonet
 */
public class MessageChannelEnum {

    public static final int MESSAGE_CHANNEL_1 = 1;

    public static final int MESSAGE_CHANNEL_2 = 2;

    public static final int MESSAGE_CHANNEL_4 = 4;

    private int channelNumber;

    public MessageChannelEnum(int channelNumber) {
        this.channelNumber = channelNumber;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public String toString() {
        String ret = "";
        switch(channelNumber) {
            case MESSAGE_CHANNEL_1:
                ret = "Channel 1";
                break;
            case MESSAGE_CHANNEL_2:
                ret = "Channel 2";
                break;
            case MESSAGE_CHANNEL_4:
                ret = "Channel 4";
                break;
        }
        return ret;
    }
}
