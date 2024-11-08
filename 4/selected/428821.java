package ircam.jmax.editors.explode;

/**
 * a mapper into the time value of the ScrEvents
 */
public class ChannelMapper extends Mapper {

    /**
   * set the given channel in the given event
   */
    public void set(ScrEvent e, int value) {
        e.setChannel(value);
    }

    /**
   * get the channel of the given event
   */
    public int get(ScrEvent e) {
        return e.getChannel();
    }

    /**
   * access the static instance
   */
    public static Mapper getMapper() {
        if (itsChannelMapper == null) itsChannelMapper = new ChannelMapper();
        return itsChannelMapper;
    }

    static ChannelMapper itsChannelMapper;
}
