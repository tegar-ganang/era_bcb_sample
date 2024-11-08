package org.mhpbox.rcontrol;

import org.mhpbox.infra.AbstractSTB;
import org.mhpbox.infra.TVControls;

public class ChannelControlState extends RemoteControlHandlerState {

    protected TVControls tvControls;

    public ChannelControlState(TVControls controls) {
        this.tvControls = controls;
    }

    private ChannelControlState() {
        this(AbstractSTB.getInstance().getTVControls());
    }

    public void buttonPressed(int code) {
        if (code >= ButtonPressedEvent.NUM0 && code <= ButtonPressedEvent.NUM9) {
            int dig = code - ButtonPressedEvent.NUM0;
            tvControls.getChannelSelector().appendDigit(dig);
        }
    }
}
