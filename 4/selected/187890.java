package cn.edu.wuse.musicxml.sound;

/**
 * 抽象的转换器中的Midi事件
 * @author Mao
 *
 */
public class ChannelProgramEvent extends MusicXmlMidiEvent {

    private static final long serialVersionUID = 1L;

    private ChannelProgram channel;

    public ChannelProgramEvent(Object source, ChannelProgram channel) {
        super(source);
        this.channel = channel;
    }

    public ChannelProgram getChannel() {
        return channel;
    }

    public void setChannel(ChannelProgram channel) {
        this.channel = channel;
    }
}
