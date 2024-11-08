package com.dalsemi.onewire.application.tag;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.OneWireException;
import java.util.Vector;

/**
 * This class provides a default object for the D2A type of a tagged 1-Wire device.
 */
public class D2A extends TaggedDevice implements TaggedActuator {

    /**
    * Creates an object for the device.
    */
    public D2A() {
        super();
        ActuatorSelections = new Vector();
    }

    /**
    * Creates an object for the device with the supplied address connected
    * to the supplied port adapter.
    * @param adapter The adapter serving the actuator.
    * @param netAddress The 1-Wire network address of the actuator.
    */
    public D2A(DSPortAdapter adapter, String netAddress) {
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
        PotentiometerContainer pc = (PotentiometerContainer) getDeviceContainer();
        int Index = 0;
        Index = ActuatorSelections.indexOf(selection);
        byte[] state = pc.readDevice();
        pc.setCurrentWiperNumber(getChannel(), state);
        pc.writeDevice(state);
        if (Index > -1) {
            state = pc.readDevice();
            pc.setWiperPosition(Index);
            pc.writeDevice(state);
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
        PotentiometerContainer pc = (PotentiometerContainer) getDeviceContainer();
        int numOfWiperSettings;
        int resistance;
        double offset = 0.6;
        double wiperResistance;
        String selectionString;
        byte[] state = pc.readDevice();
        pc.setCurrentWiperNumber(getChannel(), state);
        pc.writeDevice(state);
        numOfWiperSettings = pc.numberOfWiperSettings(state);
        resistance = pc.potentiometerResistance(state);
        wiperResistance = (double) ((double) (resistance - offset) / (double) numOfWiperSettings);
        selectionString = resistance + " k-Ohms";
        ActuatorSelections.addElement(selectionString);
        for (int i = (numOfWiperSettings - 2); i > -1; i--) {
            double newWiperResistance = (double) (wiperResistance * (double) i);
            int roundedWiperResistance = (int) ((newWiperResistance + offset) * 10000);
            selectionString = (double) ((double) roundedWiperResistance / 10000.0) + " k-Ohms";
            ActuatorSelections.addElement(selectionString);
        }
    }

    /**
    * Keeps the selections of this actuator
    */
    private Vector ActuatorSelections;
}
