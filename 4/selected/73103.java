package com.volantis.mcs.protocols.ticker.response.attributes;

import com.volantis.mcs.protocols.MCSAttributes;

/**
 * AddItem response element attributes
 */
public class AddItemAttributes extends MCSAttributes {

    private String itemId;

    private String channel;

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
     * @return Returns the id.
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * @param id The id to set.
     */
    public void setItemId(String id) {
        this.itemId = id;
    }
}
