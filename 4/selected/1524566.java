package com.musparke.midi.event;

import com.musparke.midi.model.ChannelPressure;

public class ChannelTouchEvent extends MusicXmlMidiEvent {

    private static final long serialVersionUID = 1L;

    private ChannelPressure channelTouch;

    public ChannelTouchEvent(Object source, ChannelPressure channelTouch) {
        super(source);
        this.channelTouch = channelTouch;
    }

    public ChannelPressure getChannelTouch() {
        return channelTouch;
    }

    public void setChannelTouch(ChannelPressure channelTouch) {
        this.channelTouch = channelTouch;
    }
}
