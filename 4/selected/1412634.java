package com.dalsemi.onewire.application.tag;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.OneWireException;
import java.util.Vector;

/**
 * This class provides a default object for the Event type of a tagged 1-Wire device.
 */
public class Event extends TaggedDevice implements TaggedSensor {

    /**
    * Creates an object for the device.
    */
    public Event() {
        super();
    }

    /**
    * Creates an object for the device with the supplied address and device type connected
    * to the supplied port adapter.
    * @param adapter The adapter serving the sensor.
    * @param netAddress The 1-Wire network address of the sensor.
    *
    */
    public Event(DSPortAdapter adapter, String netAddress) {
        super(adapter, netAddress);
    }

    /**
    * The readSensor method returns the "max" string if the Sensor (a 
    * switch) has had activity since last time it was checked for activity.
    * @param--none.
    *
    * @return String  The "max" string associated with this Sensor.
    */
    public String readSensor() throws OneWireException {
        String returnString = "";
        byte[] switchState;
        SwitchContainer Container;
        Container = (SwitchContainer) DeviceContainer;
        if (Container.hasActivitySensing()) {
            switchState = Container.readDevice();
            if (Container.getSensedActivity(getChannel(), switchState)) {
                returnString = getMax();
                Container.clearActivity();
                switchState = Container.readDevice();
            }
        } else {
            returnString = "";
        }
        return returnString;
    }
}
