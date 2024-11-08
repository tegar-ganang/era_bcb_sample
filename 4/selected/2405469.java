package com.dalsemi.onewire.application.tag;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.OneWireException;
import java.util.Vector;

/**
 * This class provides a default object for the Switch type of a tagged 1-Wire device.
 */
public class Switch extends TaggedDevice implements TaggedActuator {

    /**
    * Creates an object for the device.
    */
    public Switch() {
        super();
        ActuatorSelections = new Vector();
    }

    /**
    * Creates an object for the device with the supplied address connected
    * to the supplied port adapter.
    * @param adapter The adapter serving the actuator.
    * @param netAddress The 1-Wire network address of the actuator.
    */
    public Switch(DSPortAdapter adapter, String netAddress) {
        super(adapter, netAddress);
        ActuatorSelections = new Vector();
    }

    /**
    * Get the possible selection states of this actuator
    *
    * @return Vector of Strings representing selection states.
    */
    public Vector getSelections() {
        return ActuatorSelections;
    }

    /**
    * Set the selection of this actuator
    *
    * @param The selection string.
    *
    * @throws OneWireException
    *
    */
    public void setSelection(String selection) throws OneWireException {
        SwitchContainer switchcontainer = (SwitchContainer) getDeviceContainer();
        int Index = 0;
        int channelValue = getChannel();
        Index = ActuatorSelections.indexOf(selection);
        boolean switch_state = false;
        if (Index > -1) {
            if (Index > 0) switch_state = true;
            byte[] state = switchcontainer.readDevice();
            switchcontainer.setLatchState(channelValue, switch_state, false, state);
            switchcontainer.writeDevice(state);
        }
    }

    /**
    * Initializes the actuator
    * @param Init The initialization string.
    *
    * @throws OneWireException
    * 
    */
    public void initActuator() throws OneWireException {
        SwitchContainer switchcontainer = (SwitchContainer) getDeviceContainer();
        ActuatorSelections.addElement(getMin());
        ActuatorSelections.addElement(getMax());
        int initValue;
        int channelValue;
        int switchStateIntValue = 0;
        Integer init = new Integer(getInit());
        initValue = init.intValue();
        channelValue = getChannel();
        byte[] state = switchcontainer.readDevice();
        boolean switch_state = switchcontainer.getLatchState(channelValue, state);
        if (switch_state) switchStateIntValue = 1; else switchStateIntValue = 0;
        if (initValue != switchStateIntValue) {
            switchcontainer.setLatchState(channelValue, !switch_state, false, state);
            switchcontainer.writeDevice(state);
        }
    }

    /**
    * Keeps the selections of this actuator
    */
    private Vector ActuatorSelections;
}
