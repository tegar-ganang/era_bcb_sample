package com.frinika.sequencer.model;

import com.frinika.sequencer.FrinikaTrackWrapper;

/**
 * @author Peter Johan Salomonsen
 */
public abstract class ChannelEvent extends MultiEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    int channel;

    /**
     * 
     * @param track
     * @param startTick
     * @deprecated
     */
    public ChannelEvent(FrinikaTrackWrapper track, long startTick) {
        super(track, startTick);
    }

    public ChannelEvent(MidiPart part, long startTick) {
        super(part, startTick);
    }

    /**
     * @return Returns the channel.
     */
    public int getChannel() {
        return channel;
    }

    /**
     * @param channel The channel to set.
     */
    public void setChannel(int channel) {
        this.channel = channel;
    }
}
