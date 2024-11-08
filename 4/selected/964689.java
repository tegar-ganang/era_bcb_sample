package silence.format.xm;

import silence.format.xm.data.*;

/**
 * Structure for holding a channels update data
 *
 * @author Fredrik Ehnbom
 */
class ChannelUpdateData {

    private int note;

    private int effect;

    private int effectParameter;

    private int volume;

    private Instrument instrument;

    private final Channel channel;

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public int getEffect() {
        return effect;
    }

    public void setEffect(int effect) {
        this.effect = effect;
    }

    public int getEffectParameter() {
        return effectParameter;
    }

    public void setEffectParameter(int e) {
        this.effectParameter = e;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument i) {
        this.instrument = i;
    }

    public Channel getChannel() {
        return channel;
    }

    public void reset() {
        setNote(-1);
        setInstrument(null);
        setVolume(-1);
        setEffect(-1);
        setEffectParameter(-1);
    }

    public ChannelUpdateData(Channel c) {
        this.channel = c;
        reset();
    }
}
