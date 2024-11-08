package cn.edu.wuse.musicxml.sound;

public class ChannelTouchEvent extends MusicXmlMidiEvent {

    private static final long serialVersionUID = 1L;

    private ChannelTouch channelTouch;

    public ChannelTouchEvent(Object source, ChannelTouch channelTouch) {
        super(source);
        this.channelTouch = channelTouch;
    }

    public ChannelTouch getChannelTouch() {
        return channelTouch;
    }

    public void setChannelTouch(ChannelTouch channelTouch) {
        this.channelTouch = channelTouch;
    }
}
