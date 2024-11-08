package com.dalsemi.onewire.application.tag;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.OneWireException;
import java.util.Vector;

/**
 * This class provides a default object for the Level type of a tagged 1-Wire device.
 */
public class Level extends TaggedDevice implements TaggedSensor {

    /**
    * Creates an object for the device.
    */
    public Level() {
        super();
    }

    /**
    * Creates an object for the device with the supplied address and device type connected
    * to the supplied port adapter.
    * @param adapter The adapter serving the sensor.
    * @param netAddress The 1-Wire network address of the sensor.
    *
    */
    public Level(DSPortAdapter adapter, String netAddress) {
        super(adapter, netAddress);
    }

    /**
    * The readSensor method returns the <max> or <min> string of the Sensor (in 
    * this case, a switch).  The elements <max> and <min> represent conducting 
    * and non-conducting states of the switch, respectively. 
    *
    * @param--none.
    *
    * @return String  The <max> string is associated with the conducting switch state,
    *                 and the <min> string is associated with the non-conducting state 
    *                 of the 1-Wire switch.
    */
    public String readSensor() throws OneWireException {
        String returnString = "";
        byte[] switchState;
        int switchChannel = getChannel();
        SwitchContainer Container;
        Container = (SwitchContainer) DeviceContainer;
        if (Container.hasLevelSensing()) {
            switchState = Container.readDevice();
            if (Container.getLevel(switchChannel, switchState)) {
                returnString = getMax();
            } else {
                returnString = getMin();
            }
        }
        return returnString;
    }
}
