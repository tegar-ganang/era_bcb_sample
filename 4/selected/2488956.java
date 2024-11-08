package net.sourceforge.entrainer.eeg;

import java.util.EventObject;
import java.util.List;

/**
 * Used to notify {@link EEGReadListener}s that a read of the device
 * has been completed.
 * 
 * @author burton
 *
 */
public class EEGReadEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private List<EEGChannelValue> channels;

    public EEGReadEvent(Object source, List<EEGChannelValue> channels) {
        super(source);
        setChannels(channels);
    }

    /**
	 * returns the current values of the channels being monitored.
	 */
    public List<EEGChannelValue> getChannels() {
        return channels;
    }

    public void setChannels(List<EEGChannelValue> channels) {
        this.channels = channels;
    }
}
