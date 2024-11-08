package com.volantis.mcs.protocols.ticker.attributes;

/**
 * Feed element attributes.
 */
public class FeedAttributes extends TickerAttributes {

    private String channel;

    private String itemDisplay;

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
     * @return Returns the itemDisplay.
     */
    public String getItemDisplay() {
        return itemDisplay;
    }

    /**
     * @param itemDisplay The itemDisplay to set.
     */
    public void setItemDisplay(String itemDisplay) {
        this.itemDisplay = itemDisplay;
    }
}
