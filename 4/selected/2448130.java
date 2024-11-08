package org.cdp1802.upb.impl;

import org.cdp1802.upb.UPBManager;
import org.cdp1802.upb.UPBMessage;
import org.cdp1802.upb.UPBNetworkI;
import org.cdp1802.upb.UPBProductI;

/**
 * This class allows control over the UPB IO module that 
 * provides 2 outputs and 3 inputs.  This is a very different
 * UPB device from most and as such, most control methods are
 * inapplicable.
 *
 * <P>Device events come as you'd expect, but any supplied device
 * level in the event should not be used -- consult the device
 * directly (the level in the event won't mean anything).
 *
 * <P>Internal Channel to I/O mapping
 * <BR>Channel 1 to n == Outputs #1-n
 * <BR>Channel n+1 to n+m == Inputs #1 to m 
 * <BR>(so if you have 2 output channels and 3 input channels, channels 1 and 2
 * are outputs 1 and 2, channels 3 thru 5 are inputs 1 thru 3)
 *
 * <P>Exposed from this class, we uses a different channel numbering:
 * The first batch of channels (1-MAX_OUTPUTS) is the outputs and
 * the second batch of channels (MAX_OUTPUTS + 1 - MAX_OUTPUTS + MAX_SENSORS) 
 * are inputs.
 *
 * <P>So there is a real dichotomy between what we talk to the UPB back end in 
 * and what we expose to the outside world.
 *
 * @author gerry
 */
public class UPBIODevice extends UPBDevice {

    static final int MAX_SENSORS = 3;

    static final int MAX_OUTPUTS = 2;

    boolean sensorsValid = false;

    boolean outputsValid = false;

    boolean ioState[] = null;

    UPBIODevice() {
    }

    UPBIODevice(UPBNetworkI theNetwork, UPBProductI theProduct, int deviceID) {
        super(theNetwork, theProduct, deviceID);
    }

    public void setDeviceInfo(UPBNetworkI theNetwork, UPBProductI theProduct, int deviceID) {
        super.setDeviceInfo(theNetwork, theProduct, deviceID);
        ioState = new boolean[getChannelCount()];
    }

    void debug(String theMessage) {
        deviceNetwork.getUPBManager().upbDebug("IO_DEVICE[" + deviceID + "]:: " + theMessage);
    }

    public boolean isDeviceStateValid() {
        return sensorsValid && outputsValid;
    }

    public int getChannelCount() {
        return getOutputCount() + getSensorCount();
    }

    /**
   * Determine if a sensor or output is active.
   *
   * <P>Can be used as an alternative to isOutputActive() and isSensorActive()
   * (the channel passed determining which is used
   *
   * @param forChannel channel to check to see if on/active
   * @return true if active, false if not
   */
    public boolean isDeviceOn(int forChannel) {
        if ((forChannel < 1) || (forChannel > (getOutputCount() + getSensorCount()))) return false;
        if (forChannel <= getOutputCount()) return isOutputActive(forChannel);
        return isSensorActive(forChannel - getOutputCount());
    }

    /** Not applicable to this device.  Always returns false */
    public boolean isDeviceOn() {
        return false;
    }

    private void setDeviceState(int forChannel, boolean isActive) {
        if ((forChannel < 0) || (forChannel > getOutputCount())) return;
        int lowChannel = forChannel, highChannel = forChannel;
        if (forChannel == 0) {
            lowChannel = 1;
            highChannel = getOutputCount();
        }
        for (int chanIndex = lowChannel; chanIndex <= highChannel; chanIndex++) {
            setOutputActive(chanIndex, isActive);
        }
    }

    /**
   * Can be used as an alternative to setOutputState(forChannel, true)
   *
   * @param forChannel output channel (1-n) to turn on (0 means turn all outputs on)
   */
    public void turnDeviceOn(int forChannel) {
        setDeviceState(forChannel, true);
    }

    /** Not applicable to this device.  Does Nothing. */
    public void turnDeviceOn() {
    }

    /**
   * Can be used as an alternative to setOutputState(forChannel, false)
   *
   * @param forChannel output channel (1-n) to turn off (0 means turn all outputs off)
   */
    public void turnDeviceOff(int forChannel) {
        setDeviceState(forChannel, false);
    }

    /** Not applicable to this device.  Does Nothing. */
    public void turnDeviceOff() {
    }

    /**
   * Get the number of sensors on this IO device
   *
   * @return number of sensors, from 0 on up
   */
    public int getSensorCount() {
        return MAX_SENSORS;
    }

    /**
   * Get the number of outputs on this IO device
   *
   * @return number of outputs, from 0 on up
   */
    public int getOutputCount() {
        return MAX_OUTPUTS;
    }

    /**
   * Return the current state of the sensor.  There are
   * three input sensors, so sensorNum can be 1 to getSensorCount().  If
   * the sensor is closed (light on sensor is ON), true
   * is returned.  If the sensor is open (light on sensor
   * is off, false.
   *
   * @param sensorNum sensor number, 1 to getOutputCount()
   * @return true if specified sensor input is closed, false is open or invalid sensor #
   */
    public boolean isSensorActive(int sensorNum) {
        if ((sensorNum < 1) || (sensorNum > getSensorCount())) return false;
        if (!sensorsValid) return false;
        if (UPBManager.DEBUG_MODE) debug("IS_SENSOR_ACTIVE: SENSOR=" + sensorNum + ", IS_ACTIVE=" + ioState[(sensorNum - 1) + getOutputCount()]);
        return ioState[(sensorNum - 1) + getOutputCount()];
    }

    /**
   * Return the current state of the output.  There are
   * two outputs, so outputNum can be 1 to getOutputCount().  If
   * the output is closed/active (light on sensor is ON), true
   * is returned.  If the output is open (light on sensor
   * is off, false.
   *
   * @param outputNum output number, 1 to getOutputCount()
   * @return true if specified output closed/active, false is open or invalid output #
   */
    public boolean isOutputActive(int outputNum) {
        if ((outputNum < 1) || (outputNum > getOutputCount())) return false;
        if (!outputsValid) return false;
        if (UPBManager.DEBUG_MODE) debug("IS_OUTPUT_ACTIVE: OUTPUT=" + outputNum + ", IS_ACTIVE=" + ioState[outputNum - 1]);
        return ioState[outputNum - 1];
    }

    /** 
   * Change the status of the output passed to match the passed state.
   *
   * @param outputNum output to change state of, 1 to getOutputCount() or 0 for to change all outputs
   * @param outputActive true to close/activate the output, false to open it
   */
    public void setOutputActive(int outputNum, boolean outputActive) {
        if ((outputNum < 0) || (outputNum > getOutputCount())) return;
        int lowChannel = outputNum, highChannel = outputNum;
        if (outputNum == 0) {
            lowChannel = 1;
            highChannel = getOutputCount();
        }
        for (int chanIndex = lowChannel; chanIndex <= highChannel; chanIndex++) {
            if (UPBManager.DEBUG_MODE) debug("SET_OUTPUT_ACTIVE: OUTPUT=" + chanIndex + ", IS_ACTIVE=" + isOutputActive(chanIndex) + ", SET_TO_ACTIVE=" + outputActive);
            transmitNewDeviceLevel((outputActive ? 1 : 0), 0, chanIndex);
        }
        return;
    }

    boolean isReallyNewDeviceLevel(int newDeviceLevel, int atFadeRate, int toChannel) {
        if (!outputsValid) return true;
        if (isOutputActive(toChannel) == (newDeviceLevel != 0)) return false;
        return true;
    }

    void installNewDeviceLevel(int theLevel, int theFadeRate, int theChannel) {
        if (theChannel == 0) {
            if (UPBManager.DEBUG_MODE) debug("Got a newDeviceLevel with channel 0 -- ignoring");
            return;
        }
        boolean isActive = (theLevel != 0);
        boolean forceUpdate = false;
        int chanIndex = theChannel - 1;
        if (theChannel <= getOutputCount()) {
            forceUpdate = !outputsValid;
            if ((ioState[chanIndex] == isActive) && !forceUpdate) return;
            ioState[chanIndex] = isActive;
            outputsValid = true;
            if (UPBManager.DEBUG_MODE) debug("Updated Output #" + theChannel + " state to " + (ioState[chanIndex] ? "high" : "low"));
            deviceStateChanged(theChannel);
            return;
        }
        forceUpdate = !sensorsValid;
        if ((ioState[chanIndex] == isActive) && !forceUpdate) return;
        ioState[chanIndex] = isActive;
        sensorsValid = true;
        if (UPBManager.DEBUG_MODE) debug("Updated Sensor #" + ((chanIndex - getOutputCount()) + 1) + " state to " + (ioState[chanIndex] ? "high" : "low"));
        deviceStateChanged(theChannel);
        return;
    }

    /**
   * Unify the update model
   */
    public void updateInternalDeviceLevel(int theLevel, int atFadeRate, int forChannel) {
        installNewDeviceLevel(theLevel, atFadeRate, forChannel);
    }

    /**
   * State reports, when dealing with low-level messages, are a little wierd in 
   * that they come across with all outputs in channel 1 and all sensors in
   * channel 2.  This is the only time the "unnormalized" channel numbers
   * are used, so deal with it.
   */
    void receiveDeviceStateReport(UPBMessage theMessage) {
        deviceNetwork.getUPBManager().cancelStateRequest(this);
        if (!deviceNetwork.getUPBManager().getMediaAdapter().isLowLevelAdapter()) {
            if (UPBManager.DEBUG_MODE) debug("Got STATUS report from POST-TRANSFORMED source (not PIM) -- LEVEL=" + theMessage.getLevel() + ", CHAN=" + theMessage.getChannel() + ") -- handing to internal dispatch");
            installNewDeviceLevel(theMessage.getLevel(), theMessage.getFadeRate(), theMessage.getChannel());
            return;
        }
        int forChannel = theMessage.getChannel(), theLevel = theMessage.getLevel();
        boolean isActive = false;
        boolean forceSensors = false, forceOutputs = false;
        boolean changedMap[] = new boolean[ioState.length];
        if (UPBManager.DEBUG_MODE) debug("Got PIM based STATUS report -- LEVEL=" + theLevel + ", CHANNEL=" + forChannel);
        if (forChannel == 1) {
            forceSensors = !sensorsValid;
            for (int sensorIndex = 0; sensorIndex < getSensorCount(); sensorIndex++) {
                isActive = (theLevel & (1 << sensorIndex)) != 0;
                if (ioState[sensorIndex + getOutputCount()] == isActive) continue;
                ioState[sensorIndex + getOutputCount()] = isActive;
                changedMap[sensorIndex + getOutputCount()] = true;
            }
            sensorsValid = true;
        } else if (forChannel == 2) {
            forceOutputs = !outputsValid;
            for (int outputIndex = 0; outputIndex < getOutputCount(); outputIndex++) {
                isActive = (theLevel & (1 << outputIndex)) != 0;
                if (ioState[outputIndex] == isActive) continue;
                ioState[outputIndex] = isActive;
                changedMap[outputIndex] = true;
            }
            outputsValid = true;
        } else if (forChannel == 0) {
            return;
        } else {
            error("Unknown channel report for device (channel " + forChannel + ") -- ignored");
            return;
        }
        for (int ioIndex = 0; ioIndex < getChannelCount(); ioIndex++) {
            if (changedMap[ioIndex] || ((ioIndex < getOutputCount()) && forceOutputs) || ((ioIndex >= getOutputCount() && forceSensors))) {
                deviceStateChanged(ioIndex + 1);
            }
        }
    }
}
