package com.volantis.mcs.protocols.ticker.attributes;

/**
 * ItemsCount element attributes.
 */
public class ItemsCountAttributes extends TickerAttributes {

    private String channel;

    private String read;

    private String followed;

    /**
     * @return Returns the channel.
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @param channel The channel to set.
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * @return Returns the followed.
     */
    public String getFollowed() {
        return followed;
    }

    /**
     * @param followed The followed to set.
     */
    public void setFollowed(String followed) {
        this.followed = followed;
    }

    /**
     * @return Returns the read.
     */
    public String getRead() {
        return read;
    }

    /**
     * @param read The read to set.
     */
    public void setRead(String read) {
        this.read = read;
    }
}
