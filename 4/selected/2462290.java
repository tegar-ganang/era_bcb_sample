package com.volantis.map.sti.model;

/**
 * Audio synthetic element model.
 */
public class AudioSynthetic {

    /**
     * Number of channel to use attribute.
     */
    protected int channelToUse;

    /**
     * Channels priority attribute.
     */
    protected String channelsPriority;

    /**
     * Instrument attribute.
     */
    protected int instrument;

    /**
     * Getter for channel to use attribute.
     * 
     * @return channel to use.
     */
    public int getChannelToUse() {
        return this.channelToUse;
    }

    /**
     * Setter for channel to use attribute.
     * 
     * @param channelToUse channel to use.
     */
    public void setChannelToUse(int channelToUse) {
        this.channelToUse = channelToUse;
    }

    /**
     * Getter for channels priority attribute.
     * 
     * @return channels priority.
     */
    public String getChannelsPriority() {
        return this.channelsPriority;
    }

    /**
     * Setter for channels priority attribute.
     * 
     * @param channelsPriority channels priority.
     */
    public void setChannelsPriority(String channelsPriority) {
        this.channelsPriority = channelsPriority;
    }

    /**
     * Getter for instrument attribute.
     * 
     * @return instrument.
     */
    public int getInstrument() {
        return this.instrument;
    }

    /**
     * Setter for instrument attribute.
     * 
     * @param instrument instrument attribute.
     */
    public void setInstrument(int instrument) {
        this.instrument = instrument;
    }
}
