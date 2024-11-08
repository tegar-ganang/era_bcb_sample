package model.device.input;

import model.util.*;
import model.*;

public class InputDeviceConnector {

    private LightModel model;

    private int source;

    private int maxChannels;

    public InputDeviceConnector(LightModel _model, int _source, int _maxChannels) {
        source = _source;
        model = _model;
        maxChannels = _maxChannels;
    }

    public void updateChannel(Channel channel) {
        model.setChannelValue(channel, source);
    }

    public void updateChannels(Channel[] channels) {
        model.setChannelValues(channels, source);
    }

    public void resetAllChannels() {
        model.resetAllChannels(source);
    }

    public void goToNextCue() {
        model.goToNextCue();
    }

    public Channel[] getChannels() {
        return model.getChannels();
    }

    public float[] getChannelSources(Channel[] channels) {
        return model.getChannelSources(channels);
    }

    public Channel[] getInputDeviceChannels() {
        return model.getChannels(source);
    }
}
