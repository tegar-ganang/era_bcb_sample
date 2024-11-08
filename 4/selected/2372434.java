package model.device;

import java.util.*;
import model.*;
import model.util.*;
import model.channel.set.*;

/**
 * This is a concrete implementation of the ADMXDevice framework for the USBDMX.com DMX controller.  This
 * driver required the D2xx driver for the USBDMX.com controller to be installed on the system, and utilizes
 * the USBDMX Java Driver.dll file to communicate with the driver.
 * 
 * <p>TODO: fadeValue is commented out because the program never uses it, and the code for it is out of date.
 * It should probably exist, but I havn't seen the need yet.
 */
public class USBDMXInterface extends ADMXDevice {

    private boolean[] stopTransition;

    /**
   * Native method that will open the USBDMX.com device on the computer.
   *
   * @param device The device to open a connection to.  0 opens the first device connected
   * to the computer.
   * @return The handle for the opened connection.  This handle is used to address the device.
   * Additionally, this method will return 0xFFFFFFFF if there is an error opening the device.
   */
    private native long Open(int device);

    /**
   * Native method that will close the connection to the USBDMX.com device.
   * 
   * @param handle The handle for the connection to close.
   * @return The result code of the action.  0 means no error, all other return values
   * are an error code.
   */
    private native long Close(long handle);

    /**
   * This method will write the given data to the device defined by handle.
   * 
   * @param handle The ID of the opened device.  
   * @param writeBuffer The data to write to the device.
   * @param bytesToWrite The number of bytes to write to the device.
   * @return The result code of the action.  0 means no error, all other return values 
   * are an error code.
   */
    private native long Write(long handle, byte[] writeBuffer, long bytesToWrite);

    /**
   * This code loads the DLL file.  USBDMX Java Driver.dll must be in the root directory of the program.
   */
    static {
        System.loadLibrary("USBDMX Java Driver");
    }

    /**
   * This variable stores the handle for the opened device.
   */
    private long handle;

    /**
   * This constructor will instantiate a new USBDMX.com device.  It will open the connection to
   * the device, and enable DMX transmition.
   * 
   * @throws ADMXDeviceException This exception will be thrown if there is an error trying to connect to 
   * the USBDMX.com device.
   */
    public USBDMXInterface() throws ADMXDeviceException {
        super();
        stopTransition = new boolean[] { false };
        handle = Open(0);
        if (handle == 0xFFFFFFFFL) throw new ADMXDeviceException("Error opening USBDMX.com device: No device found");
        long result = Write(handle, new byte[] { 0x44 }, 1);
        if (result != 0L) throw new ADMXDeviceException("Error writting to USBDMX.com device! Error code: " + Long.toString(result));
    }

    /**
   * This method sets the given channel, value pair on the device.
   *
   * @param channel The Channel object with the address, value pair to set on the device.
   * @return A Channel object containing the new value of the given address.
   * @throws ADMXDeviceException This exception will be thrown if there is an error while writting to the device.
   */
    public Channel setValue(Channel channel) throws ADMXDeviceException {
        byte[] data = new byte[3];
        if (channel.value < 0) return channel;
        if (channel.address <= 256) {
            data[0] = 0x48;
            data[1] = (byte) (channel.address - 1);
            data[2] = (byte) channel.value;
        } else if (channel.address <= 512) {
            data[0] = 0x49;
            data[1] = (byte) (channel.address - 1);
            data[2] = (byte) channel.value;
        }
        long result = Write(handle, data, 3);
        if (result != 0L) throw new ADMXDeviceException("Error writting to USBDMX.com device! Error code: " + Long.toString(result));
        return channel;
    }

    /**
   * This method sets a series of values on the device.
   * 
   * @param channels The array of address, value pairs to set on the device.
   * @return The Channel objects containing the new values of the given addresses.
   * @throws ADMXDeviceException This exception will be thrown if there is an error while writting to the device.
   */
    public Channel[] setValues(Channel[] channels) throws ADMXDeviceException {
        byte[] data = new byte[channels.length * 3];
        for (int i = 0; i < channels.length * 3; i += 3) {
            if (channels[i / 3].address <= 256) {
                data[i] = 0x48;
                data[i + 1] = (byte) (channels[i / 3].address - 1);
                data[i + 2] = (byte) channels[i / 3].value;
            } else if (channels[i / 3].address <= 512) {
                data[i] = 0x49;
                data[i + 1] = (byte) (channels[i / 3].address - 1);
                data[i + 2] = (byte) channels[i / 3].value;
            }
        }
        long result = Write(handle, data, data.length);
        if (result != 0L) throw new ADMXDeviceException("Error writting to USBDMX.com device! Error code: " + Long.toString(result));
        return channels;
    }

    public void fadeValues(final CueValueSet cue, final short[] changedAddrs, final IChannelValueGetter valueGetter) throws ADMXDeviceException {
        final byte[] data = new byte[changedAddrs.length * 3];
        final int sends = (int) (cue.getFadeUpMillis() / 22.0 + 0.5) + 1;
        final boolean[] threadTimerDone = new boolean[1];
        final boolean[] writeError = new boolean[1];
        writeError[0] = false;
        threadTimerDone[0] = false;
        stopTransition[0] = false;
        for (int i = 0; i < changedAddrs.length * 3; i += 3) {
            if (changedAddrs[i / 3] <= 256) {
                data[i] = 0x48;
                data[i + 1] = (byte) (changedAddrs[i / 3] - 1);
            } else if (changedAddrs[i / 3] <= 512) {
                data[i] = 0x49;
                data[i + 1] = (byte) (changedAddrs[i / 3] - 1);
            }
        }
        final long[] errorResult = new long[1];
        Timer threadTimer = new Timer();
        threadTimer.scheduleAtFixedRate(new TimerTask() {

            int send = 1;

            public void run() {
                cue.setFadeLevel(clip(((float) (1.0 * send)) / sends));
                for (int i = 0; i < changedAddrs.length * 3; i += 3) {
                    data[i + 2] = (byte) (valueGetter.getChannelValue(changedAddrs[i / 3]));
                }
                long result = Write(handle, data, changedAddrs.length * 3);
                if (result != 0L) {
                    threadTimerDone[0] = true;
                    writeError[0] = true;
                    errorResult[0] = result;
                    cancel();
                    return;
                }
                if (stopTransition[0] == true) {
                    threadTimerDone[0] = true;
                    cancel();
                    return;
                }
                send++;
                if (send > sends) {
                    threadTimerDone[0] = true;
                    cancel();
                }
            }
        }, 0, 22);
        while (threadTimerDone[0] == false) Thread.yield();
        if (writeError[0] == true) throw new ADMXDeviceException("Error writting to USBDMX.com device! Error code: " + Long.toString(errorResult[0]));
    }

    /**
   * This method will start a fade from the startValues to the endValues.  The two Channel object arrays must be the same length
   * and contain the same channel addresses, in the same order.  IUpdateChannel and IUpdateFadeProgress are used to update the
   * view during the fade.
   * 
   * @param startValues The Channel object array of the starting values for the fade.  These values will be faded down in fadeDownMillis milliseconds.  This
   * array must be the same length as endValues, and contain the same addresses in the same order.
   * @param endValues The Channel object array of the ending values for the fade.  These values will be faded up in fadeUpMillis milliseconds.  This
   * array must be the same length as startValues, and contain the same addresses in the same order.
   * @param fadeUpMillis The time in milliseconds that it will take to fade in the endValues.
   * @param fadeDownMillis The time in milliseconds that it will take to fade out the startValues.
   * @param channelUpdater This interface contains two methods that will be called every time a new value is written to the device.  These
   * methods tell the model the current values of the channels during the fade.  Using these values, the view can be updated as the channels
   * fade.
   * @param fadeUpdater This class contains two methods that are used to update the fade progress bars during the fade.  They should be called
   * every time a value is written to the device.
   * @return An array of Channel objects with the final values of the faded channels.
   * @throws ADMXDeviceException This exception will be thrown if there is an error while writting to the device.
   */
    public void fadeValues(final CueValueSet startCue, final CueValueSet endCue, final short[] changedAddrs, final IChannelValueGetter valueGetter) throws ADMXDeviceException {
        final byte[] data = new byte[changedAddrs.length * 3];
        final int fadeUpSends = (int) (endCue.getFadeUpMillis() / 22.0 + 0.5) + 1;
        final int fadeDownSends = (int) (endCue.getFadeDownMillis() / 22.0 + 0.5) + 1;
        final int totalSends = Math.max(fadeUpSends, fadeDownSends);
        final boolean[] threadTimerDone = new boolean[1];
        final boolean[] writeError = new boolean[1];
        writeError[0] = false;
        threadTimerDone[0] = false;
        stopTransition[0] = false;
        for (int i = 0; i < changedAddrs.length * 3; i += 3) {
            if (changedAddrs[i / 3] <= 256) {
                data[i] = 0x48;
                data[i + 1] = (byte) (changedAddrs[i / 3] - 1);
            } else if (changedAddrs[i / 3] <= 512) {
                data[i] = 0x49;
                data[i + 1] = (byte) (changedAddrs[i / 3] - 1);
            }
        }
        final long[] errorResult = new long[1];
        Timer threadTimer = new Timer();
        threadTimer.scheduleAtFixedRate(new TimerTask() {

            int send = 1;

            public void run() {
                startCue.setFadeLevel(clip((float) (1.0 * (fadeDownSends - send) / fadeDownSends)));
                endCue.setFadeLevel(clip(((float) (1.0 * send)) / fadeUpSends));
                for (int i = 0; i < changedAddrs.length * 3; i += 3) {
                    data[i + 2] = (byte) (valueGetter.getChannelValue(changedAddrs[i / 3]));
                }
                long result = Write(handle, data, changedAddrs.length * 3);
                if (result != 0L) {
                    threadTimerDone[0] = true;
                    writeError[0] = true;
                    errorResult[0] = result;
                    cancel();
                    return;
                }
                if (stopTransition[0] == true) {
                    threadTimerDone[0] = true;
                    cancel();
                    return;
                }
                send++;
                if (send > totalSends) {
                    threadTimerDone[0] = true;
                    cancel();
                }
            }
        }, 0, 22);
        while (threadTimerDone[0] == false) Thread.yield();
        if (writeError[0] == true) throw new ADMXDeviceException("Error writting to USBDMX.com device! Error code: " + Long.toString(errorResult[0]));
    }

    /**
   * This method will clip values to a range of 0 to 1.
   * 
   * @param value The value to be clipped.
   * @return The clipped value.
   */
    private float clip(float value) {
        if (value > 1) return 1; else if (value < 0) return 0; else return value;
    }

    /**
   * This method allows you to set a maximum address on the device, if the device supports it.
   * 
   * <p>TODO: This method should be renamed to setMaxChannels().
   * 
   * @param maxChannels The maximum number of channels.
   * @throws ADMXDeviceException This exception will be thrown if there is an error while writting to the device.
   */
    public void setMaxAddress(short address) throws ADMXDeviceException {
        byte[] data = new byte[2];
        if (address <= 256) {
            data[0] = 0x4E;
            data[1] = (byte) (address - 1);
        } else if (address <= 512) {
            data[0] = 0x4F;
            data[1] = (byte) (address - 1);
        }
        long result = Write(handle, data, 2);
        if (result != 0L) throw new ADMXDeviceException("Error writting to USBDMX.com device! Error code: " + Long.toString(result));
    }

    /**
   * This method will be called before the program closes, or when the program is disconnecting from the device.  If
   * the device does not need to be 'closed' in any way, this method can be left blank.
   * 
   * @throws ADMXDeviceException This exception will be thrown if there is an error while closing the device.
   */
    public void closeDevice() throws ADMXDeviceException {
        long result = Write(handle, new byte[] { 0x46 }, 1);
        if (result != 0L) throw new ADMXDeviceException("Error closing USBDMX.com device! Error code: " + Long.toString(result));
        result = Close(handle);
        if (result != 0L) throw new ADMXDeviceException("Error closing USBDMX.com device! Error code: " + Long.toString(result));
    }

    public void stopTransition() {
        stopTransition[0] = true;
    }
}
