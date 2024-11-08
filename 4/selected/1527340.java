package net.sourceforge.entrainer.eeg.persistence;

import java.util.LinkedList;
import java.util.List;
import net.sourceforge.entrainer.eeg.core.EEGChannelState;
import net.sourceforge.entrainer.eeg.core.EEGChannelValue;

public class EEGSessionData extends EEGPersistenceObject {

    private static final long serialVersionUID = 6199342401679539339L;

    private EEGChannelState state;

    private List<EEGValues> values = new LinkedList<EEGValues>();

    public EEGSessionData(EEGChannelState state) {
        super();
        setState(state);
    }

    public boolean isForValue(EEGChannelValue value) {
        return value.getChannelState().equals(getState());
    }

    void addState(EEGChannelValue value) {
        getValues().add(new EEGValues(value.getMillisFromStart(), value.getChannelStrength()));
    }

    public EEGChannelState getState() {
        return state;
    }

    public void setState(EEGChannelState state) {
        this.state = state;
    }

    public List<EEGValues> getValues() {
        return values;
    }

    public void setValues(List<EEGValues> values) {
        this.values = values;
    }
}
