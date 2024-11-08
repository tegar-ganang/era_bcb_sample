package model.device.output;

import java.util.*;
import model.*;
import model.util.*;
import model.channel.set.*;

/**
 * This abstract class defines the device framework for DMX devices.
 * 
 * <p>TODO: fadeValue is commented out because the program never uses it, and the code for it is out of date.
 * It should probably exist, but I havn't seen the need yet.
 */
public abstract class NonFadingDMXDevice implements IDMXDevice {

    private boolean[] stopTransition;

    public NonFadingDMXDevice() {
        stopTransition = new boolean[] { false };
    }

    /**
   * This method sets the given channel, value pair on the device.
   *
   * @param channel The Channel object with the address, value pair to set on the device.
   * @return A Channel object containing the new value of the given address.
   * @throws ADMXDeviceException This exception will be thrown if there is an error while writting to the device.
   */
    public abstract Channel setValue(Channel channel) throws IDMXDeviceException;

    /**
   * This method sets a series of values on the device.
   * 
   * @param channels The array of address, value pairs to set on the device.
   * @return The Channel objects containing the new values of the given addresses.
   * @throws ADMXDeviceException This exception will be thrown if there is an error while writing to the device.
   */
    public abstract Channel[] setValues(Channel[] channels) throws IDMXDeviceException;

    /**
   * This method allows you to set a maximum address on the device, if the device supports it.
   * 
   * <p>TODO: This method should be renamed to setMaxChannels().
   * 
   * @param maxChannels The maximum number of channels.
   * @throws ADMXDeviceException This exception will be thrown if there is an error while writing to the device.
   */
    public abstract void setMaxAddress(short maxChannels) throws IDMXDeviceException;

    /**
   * This method will be called before the program closes, or when the program is disconnecting from the device.  If
   * the device does not need to be 'closed' in any way, this method can be left blank.
   * 
   * @throws ADMXDeviceException This exception will be thrown if there is an error while closing the device.
   */
    public abstract void closeDevice() throws IDMXDeviceException;

    public void stopTransition() {
        stopTransition[0] = true;
    }

    public void fadeValues(final CueValueSet cue, final short[] changedAddrs, final IChannelValueGetter valueGetter) throws IDMXDeviceException {
        final int sends = (int) (cue.getFadeUpMillis() / 22.0 + 0.5) + 1;
        final boolean[] threadTimerDone = new boolean[1];
        final String[] errorResult = new String[1];
        final boolean[] writeError = new boolean[1];
        threadTimerDone[0] = false;
        stopTransition[0] = false;
        writeError[0] = false;
        Timer threadTimer = new Timer();
        threadTimer.scheduleAtFixedRate(new TimerTask() {

            int send = 1;

            public void run() {
                cue.setFadeLevel(Utilities.clip(((float) (1.0 * send)) / sends));
                Channel[] channels = new Channel[changedAddrs.length];
                for (int i = 0; i < changedAddrs.length; i++) {
                    channels[i] = new Channel(changedAddrs[i], valueGetter.getChannelValue(changedAddrs[i]));
                }
                try {
                    setValues(channels);
                } catch (Exception e) {
                    threadTimerDone[0] = true;
                    writeError[0] = true;
                    errorResult[0] = e.getMessage();
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
        if (writeError[0] == true) throw new IDMXDeviceException("Error writting to the device! Error: " + errorResult[0]);
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
   * @throws ADMXDeviceException This exception will be thrown if there is an error while writing to the device.
   */
    public void fadeValues(final CueValueSet startCue, final CueValueSet endCue, final short[] changedAddrs, final IChannelValueGetter valueGetter) throws IDMXDeviceException {
        final int fadeUpSends = (int) (endCue.getFadeUpMillis() / 22.0 + 0.5) + 1;
        final int fadeDownSends = (int) (endCue.getFadeDownMillis() / 22.0 + 0.5) + 1;
        final int totalSends = Math.max(fadeUpSends, fadeDownSends);
        final boolean[] threadTimerDone = new boolean[1];
        final boolean[] writeError = new boolean[1];
        final String[] errorResult = new String[1];
        writeError[0] = false;
        threadTimerDone[0] = false;
        stopTransition[0] = false;
        Timer threadTimer = new Timer();
        threadTimer.scheduleAtFixedRate(new TimerTask() {

            int send = 1;

            public void run() {
                startCue.setFadeLevel(Utilities.clip((float) (1.0 * (fadeDownSends - send) / fadeDownSends)));
                endCue.setFadeLevel(Utilities.clip(((float) (1.0 * send)) / fadeUpSends));
                Channel[] channels = new Channel[changedAddrs.length];
                for (int i = 0; i < changedAddrs.length; i++) {
                    channels[i] = new Channel(changedAddrs[i], valueGetter.getChannelValue(changedAddrs[i]));
                }
                try {
                    setValues(channels);
                } catch (Exception e) {
                    threadTimerDone[0] = true;
                    writeError[0] = true;
                    errorResult[0] = e.getMessage();
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
        if (writeError[0] == true) throw new IDMXDeviceException("Error writting to the device! Error: " + errorResult[0]);
    }
}
