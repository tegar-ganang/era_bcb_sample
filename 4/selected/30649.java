package model;

import model.device.*;
import model.cue.*;
import model.util.*;
import model.show.*;
import model.channel.*;
import model.channel.set.*;
import model.device.input.*;
import model.device.output.*;
import java.util.*;

/**
 * This is the main class for the Model part of the program.  All
 * communcations between the model and controller go through this 
 * class.  Additionally, this class contains the current Show, 
 * ADMXDevice, and ChannelValues.  
 */
public class LightModel {

    /**
   * This holds the link to the connector.  All communcation to the view
   * goes through this connector.
   */
    private ILightModelConnector connector;

    /**
   * This class holds all the current channel values for the program.  All
   * methods getting and setting channel values utilize this class.
   */
    private ChannelValues channelValues;

    /**
   * This is the pointer to the current DMX device driver.  This 
   * is used whenever channel values are changed.
   */
    private IDMXDevice dmxDevice;

    /**
   * The Show object contains the current cues and all of their data.
   * This is used to store and recall cues and their values.
   */
    private Show show;

    private IInputDevice inputDevice;

    private int maxChannels;

    /**
   * This constructor instaniates the LightModel with a link to the connector,
   * the current dmx device, and the default Show.
   */
    public LightModel(ILightModelConnector _connector) {
        maxChannels = 512;
        connector = _connector;
        dmxDevice = DummyDMXDevice.Singleton;
        show = new Show(maxChannels);
        channelValues = new ChannelValues(maxChannels);
    }

    /**
   * This method returns the value of the given channel address.
   * 
   * @param address The address whos value will be returned.
   * @return The value of the given address.
   */
    public short getChannelValue(short address) {
        return channelValues.getChannelValue(address);
    }

    /**
   * This method will return the channel values for the given addresses.
   * 
   * @param addresses The array of channel addresses.
   * @return The array of channel values.
   */
    public short[] getChannelValues(short[] addresses) {
        return channelValues.getChannelValues(addresses);
    }

    /**
   * This method will return an array of Channel objects for every channel
   * with a non-zero value.  
   * 
   * @return The array of Channel objects for every channel with a non-zero value.
   */
    public Channel[] getChannels() {
        return channelValues.getChannels();
    }

    public Channel[] getChannels(int source) {
        return channelValues.getChannels(source);
    }

    public float[] getChannelSources(Channel[] channels) {
        float[][] sources = channelValues.getChannelSources(channels);
        float[] result = new float[sources.length];
        for (int i = 0; i < result.length; i++) result[i] = sources[i][0];
        return result;
    }

    /**
   * This is a specilized form of getChannels() used when creating new cues.
   * In addition to returning all channels with a non-zero value, it will also
   * return channels with a hard-ducked fader value (-100). 
   * 
   * @return The array of Channel objects for cue creation.
   */
    public Channel[] getChannelsForCue() {
        return channelValues.getChannelsForCue();
    }

    /**
   * This method will set the channel value to the given value.  The channelValues object
   * will be updated, as will the dmxDevice, and then the view will be updated.
   * 
   * <p>TODO: Change this method so an exception won't screw up the stored values.
   * 
   * @param channel The Channel object with the new address, value pair.
   * @param source An int representing the source of this channel value. 
   */
    public void setChannelValue(Channel channel, int source) {
        if (source < 0) channelValues.updateChannel(channel, source);
        try {
            dmxDevice.setValue(channelValues.getChannel(channel));
        } catch (IDMXDeviceException e) {
            writeError(e);
            return;
        }
        connector.updateChannel(channelValues.getChannel(channel), channelValues.getChannelSource(channel));
    }

    /**
   * This method will set a series of channel values.  First, the channelValues object will
   * be updated, then the channels changed on the dmxDevice.  Finally, the view will be refreshed
   * to represent the new values.
   * 
   * @param channels The Channel objects with the new address, value pairs.
   * @param source An int value representing the source of these channel values.
   */
    public void setChannelValues(Channel[] channels, int source) {
        Channel[] oldChannels = channelValues.getChannels(channels, source);
        if (source < 0) channelValues.updateChannels(channels, source);
        Channel[] newChannels = channelValues.getChannels(channels);
        try {
            dmxDevice.setValues(newChannels);
        } catch (IDMXDeviceException e) {
            channelValues.updateChannels(oldChannels, source);
            writeError(e);
            return;
        }
        connector.updateChannels(newChannels, channelValues.getChannelSources(channels));
    }

    public void resetAllChannels(int source) {
        Channel[] channels = channelValues.getChannels(source);
        Channel[] result = new Channel[channels.length];
        for (int i = 0; i < channels.length; i++) result[i] = new Channel(channels[i].address, (short) -100);
        setChannelValues(result, source);
    }

    /**
   * This method takes in two arrays of Channel objects and starts a fade between the two.  This fade 
   * occurs in a separate thread from the view.  
   * <p>
   * First, the method loops through the values to find out how many of the values have actually changed.
   * With this information, changedOldCues and changedNewCues are created.  This two Channel arrays will 
   * contain on the channels that actually change values between the two sets of values.  The channel values
   * which do not change are eliminated to increase the efficiency of this method.
   * <p>
   * The sources array is configured for all the channels changing value, and will hold either FADE_DOWN_MODE
   * or FADE_UP_MODE depending on which way that channel is changing value.
   * <p>
   * The ViewUpdater is a private class which updates the channels that are changing value on the view at a
   * fixed rate.
   * <p>
   * The IUpdateChannel class is anonymously defined inside the method call to the dmxDevice.  This class 
   * updates the values in channelValues as the cues fade.  
   * <p>
   * After the fade is finished, all channel values are set to their final values, the view is updated, and 
   * the thread exits.
   * 
   * @param oldCue The Channel array of the starting values for the fade.  This must be the same length and
   * contain the same channel addresses in the same order as newCue.
   * @param newCue The Channel array of the ending values for the fade.  This must be the same length and
   * constain the same channel addresses in the same order as oldCue.
   * @param fadeUpMillis The time over which the newCue values will be faded in in milliseconds.
   * @param fadeDownMillis The time over which the oldCue values will be faded out in milliseconds.
   * @param cueSource The number of the new cue to use for updating channel values.
   */
    private void fadeChannelValues(final CueValueSet startCue, final CueValueSet endCue) {
        connector.toggleDataEntry(false);
        new Thread() {

            public void run() {
                Channel[] startChannels = startCue.getAllChannels();
                Channel[] endChannels = endCue.getAllChannels();
                ArrayList<Channel> changedAddrs = new ArrayList<Channel>();
                ArrayList<Integer> changedSources = new ArrayList<Integer>();
                for (int i = 0; i < startChannels.length; i++) {
                    if (startChannels[i].value != endChannels[i].value) {
                        changedAddrs.add(startChannels[i]);
                        if (startChannels[i].value > endChannels[i].value) changedSources.add(ChannelValues.FADE_DOWN_SOURCE); else changedSources.add(ChannelValues.FADE_UP_SOURCE);
                    }
                }
                float[][] sources = new float[changedSources.size()][];
                for (int i = 0; i < changedSources.size(); i++) sources[i] = new float[] { changedSources.get(i) };
                connector.setChannelSources(changedAddrs.toArray(new Channel[0]), sources);
                ViewUpdater viewUpdater = new ViewUpdater(150, changedAddrs.toArray(new Channel[0]), endCue, startCue);
                endCue.setFadeLevel(0);
                endCue.setCombineMethod(IValueSet.ADD);
                channelValues.addSet(endCue);
                startCue.setCombineMethod(IValueSet.ADD);
                try {
                    dmxDevice.fadeValues(startCue, endCue, Channel.getAddresses(changedAddrs.toArray(new Channel[0])), new IChannelValueGetter() {

                        public short getChannelValue(short address) {
                            return channelValues.getChannelValue(address);
                        }
                    });
                } catch (IDMXDeviceException e) {
                    viewUpdater.cancel();
                    endCue.setCombineMethod(IValueSet.AVERAGE);
                    startCue.setCombineMethod(IValueSet.AVERAGE);
                    writeError(e);
                    return;
                }
                viewUpdater.cancel();
                endCue.setCombineMethod(IValueSet.AVERAGE);
                startCue.setCombineMethod(IValueSet.AVERAGE);
                connector.updateFadeUpProgress((int) (100 * endCue.getFadeLevel() + 0.5));
                connector.updateFadeDownProgress((int) (100 * startCue.getFadeLevel() + 0.5));
                channelValues.removeSet(startCue);
                connector.updateChannels(channelValues.getChannels(changedAddrs.toArray(new Channel[0])), channelValues.getChannelSources(changedAddrs.toArray(new Channel[0])));
                connector.toggleDataEntry(true);
            }
        }.start();
    }

    public void fadeChannelValues(final CueValueSet cue) {
        connector.toggleDataEntry(false);
        new Thread() {

            public void run() {
                Channel[] startChannels = channelValues.getAllChannels();
                Channel[] endChannels = cue.getAllChannels();
                ArrayList<Channel> changedAddrs = new ArrayList<Channel>();
                ArrayList<Integer> changedSources = new ArrayList<Integer>();
                for (int i = 0; i < startChannels.length; i++) {
                    Channel c1 = startChannels[i];
                    Channel c2 = endChannels[i];
                    if (startChannels[i].value != endChannels[i].value) {
                        changedAddrs.add(startChannels[i]);
                        if (startChannels[i].value > endChannels[i].value) changedSources.add(ChannelValues.FADE_DOWN_SOURCE); else changedSources.add(ChannelValues.FADE_UP_SOURCE);
                    }
                }
                float[][] sources = new float[changedSources.size()][];
                for (int i = 0; i < changedSources.size(); i++) sources[i] = new float[] { changedSources.get(i) };
                connector.setChannelSources(changedAddrs.toArray(new Channel[0]), sources);
                ViewUpdater viewUpdater = new ViewUpdater(150, changedAddrs.toArray(new Channel[0]), cue);
                cue.setFadeLevel(0);
                channelValues.addSet(cue);
                try {
                    dmxDevice.fadeValues(cue, Channel.getAddresses(changedAddrs.toArray(new Channel[0])), new IChannelValueGetter() {

                        public short getChannelValue(short address) {
                            return channelValues.getChannelValue(address);
                        }
                    });
                } catch (IDMXDeviceException e) {
                    viewUpdater.cancel();
                    writeError(e);
                    return;
                }
                viewUpdater.cancel();
                connector.updateFadeUpProgress((int) (100 * cue.getFadeLevel() + 0.5));
                connector.updateChannels(channelValues.getChannels(changedAddrs.toArray(new Channel[0])), channelValues.getChannelSources(changedAddrs.toArray(new Channel[0])));
                connector.toggleDataEntry(true);
            }
        }.start();
    }

    /**
   * This method adds a new cue to the current show.  It will also
   * update the view to reflect this change, and select this cue
   * as the current cue.
   */
    public void addCue(ACue cue) {
        show.addCue(cue);
        connector.updateCueList();
        goToCue(cue.getNumber());
        connector.selectCueWithoutTransition(cue.getSummary());
    }

    /**
   * This method will return an array containing the summary of every cue in
   * the show.
   * 
   * <p>TODO: Rename this method from getCueNames() to getCueSummaries()
   * 
   * @return The string array constaining the summary of every cue in the show.
   */
    public String[] getCueNames() {
        return show.getCueNames();
    }

    /**
   * This method will return the highest cue number in the show.  This is used 
   * to select the default cue value when someone creates a new cue.
   * 
   * @return The highest cue number in the show.
   */
    public float getHighestCueNumber() {
        return show.getHighestCueNumber();
    }

    public void goToNextCue() {
        float oldCue = connector.getSelectedCueNumber();
        float newCue = show.getNextCueNumber(oldCue);
        if (oldCue == newCue) return;
        cueTransition(oldCue, newCue);
    }

    /**
   * This method will start a transition from the old cue to the new cue.
   * 
   * <P>TODO: It appears this method is no longer necessary.
   * 
   * @param oldCueNumber The number of the cue to transition away from.
   * @param newCueNumber The number of the cue to transition to.
   */
    public void cueTransition(float oldCueNumber, float newCueNumber) {
        stopTransition();
        fadeChannelValues((CueValueSet) channelValues.getValueSet(show.getCueIndex(oldCueNumber)), new CueValueSet(show.getCue(newCueNumber)));
    }

    public void cueTransition(float cueNumber) {
        stopTransition();
        fadeChannelValues(new CueValueSet(show.getCue(cueNumber)));
    }

    /**
   * This method will move to a cue without a transition.
   * 
   * @param cueNumber The cue number to go to.
   */
    public void goToCue(float cueNumber) {
        stopTransition();
        channelValues.removeAllCues();
        channelValues.addSet(new CueValueSet(show.getCue(cueNumber)));
        Channel[] channels = show.getCue(cueNumber).getAllChannels();
        try {
            dmxDevice.setValues(channels);
        } catch (IDMXDeviceException e) {
            writeError(e);
            return;
        }
        connector.updateChannels(channels, channelValues.getChannelSources(channels));
    }

    public void createCueFromLive(float oldCueNumber, float newCueNumber, String name, String desc, long fadeUpMillis, long fadeDownMillis) {
        stopTransition();
        Hashtable<Short, Channel> channels = new Hashtable<Short, Channel>();
        Channel[] channelArray = channelValues.getChannelsForCue();
        for (Channel c : channelArray) channels.put(c.address, c);
        addCue(new ACue(newCueNumber, name, desc, fadeUpMillis, fadeDownMillis, show.getNewCueIndex(), maxChannels, channels));
        Channel[] changedChannels = channelValues.resetValues(ChannelValues.FADER_SOURCE);
        connector.updateChannels(channelValues.getChannels(changedChannels), channelValues.getChannelSources(changedChannels));
    }

    public void addCueSet(String name, String desc, int priority) {
        show.addCueSet(new CueSet(name, desc, priority, maxChannels, show.getNewCueIndex()));
        connector.updateCueSetList(show.getCueSetNames());
    }

    public void selectCueSet(String cueSetName) {
        show.selectCueSet(cueSetName);
        connector.updateCueSetList(show.getCueSetNames());
        connector.updateCueList();
    }

    public String[] getCueSetNames() {
        return show.getCueSetNames();
    }

    /**
   * This method will tell the view to the connect to the USBDMX.com device.
   * 
   * <p>TODO: This method needs to be redone so that the user can select what device they want to connect to.
   * 
   * @return This method will return true if the model sucsessfully connected to the device, and false otherwise.
   */
    public boolean connect(String deviceName) {
        try {
            if (deviceName.equals("Test Output Device")) {
                dmxDevice = new TestOutputDevice(maxChannels);
            } else if (deviceName.equals("USBDMX.com Device")) {
                dmxDevice = new USBDMXInterface();
            } else if (deviceName.equals("Art-Net Device")) {
                dmxDevice = new ArtNetDmxInterface();
            } else return false;
        } catch (Throwable e) {
            dmxDevice = DummyDMXDevice.Singleton;
            connector.deviceDisconnected();
            connector.raiseError(e, "There was an error connecting to the " + deviceName + " device.  If you do not have a " + deviceName + " device installed, you can ignore this message.  If you do have the device connected, please email this error to me at sinorm@gmail.com.\n\nThe dummy DMX driver has been loaded, the program will continue to operate in disconnected mode");
            e.printStackTrace();
            return false;
        }
        try {
            dmxDevice.setValues(channelValues.getChannels());
        } catch (IDMXDeviceException e) {
            writeError(e);
            return false;
        }
        return true;
    }

    /**
   * This method will tell the model to disconnect from the current device, and switch to the
   * dummy DMX device.
   */
    public void disconnect() {
        try {
            dmxDevice.closeDevice();
        } catch (Throwable e) {
            e.printStackTrace();
            connector.raiseError(e, "There was an error disconnecting from the device.  If the device was disconnected from the computer before telling the program to disconnect, you can ignore this message.  Otherwise, please email this error message to sinorm@gmail.com");
        }
        dmxDevice = DummyDMXDevice.Singleton;
    }

    public boolean enableNetworkInput() {
        channelValues.addSet(new NetworkValueSet());
        inputDevice = new NetworkServer(new InputDeviceConnector(this, channelValues.NETWORK_SOURCE, maxChannels));
        return true;
    }

    public boolean disableNetworkInput() {
        inputDevice.stop();
        inputDevice = null;
        Channel[] channels = channelValues.getChannels(ChannelValues.NETWORK_SOURCE);
        channelValues.removeSet(ChannelValues.NETWORK_SOURCE);
        connector.updateChannels(channelValues.getChannels(channels), channelValues.getChannelSources(channels));
        return true;
    }

    public String[] getDevices() {
        return new String[] { "USBDMX.com Device", "Art-Net Device", "Test Output Device" };
    }

    public void stopTransition() {
        dmxDevice.stopTransition();
    }

    /**
   * This method will save the current show to a file.
   * 
   * @param filename The file name and path to save the show to.
   */
    public void saveShow(String filename) {
        show.saveShow(filename);
    }

    public void saveShow() {
        show.saveShow();
    }

    public void newShow() {
        show = new Show(maxChannels);
        channelValues = new ChannelValues(maxChannels);
        connector.updateCueList();
        Channel[] channels = channelValues.getAllChannels();
        String[] cueNames = getCueNames();
        if (cueNames.length > 0) {
            goToCue(getCueNumber(cueNames[0]));
            connector.selectCueWithoutTransition(cueNames[0]);
        }
    }

    public String getFileName() {
        return show.getFileName();
    }

    /**
   * This method will load the saved show back into the program.  After
   * loading the show, it will update the view and channel values to 
   * reflect this change.
   *
   * @param filename The file name and path to load the show from.
   */
    public void loadShow(String filename) {
        show = ShowFactory.openShow(filename);
        maxChannels = show.getMaxChannels();
        channelValues = new ChannelValues(maxChannels);
        connector.updateCueList();
        Channel[] channels = channelValues.getAllChannels();
        String[] cueNames = getCueNames();
        if (cueNames.length > 0) {
            goToCue(getCueNumber(cueNames[0]));
            connector.selectCueWithoutTransition(cueNames[0]);
        }
    }

    /**
   * This method is called whenever there is a write error to the DMX device.
   * 
   * @param e The exception thrown by the ADMXDevice driver.
   */
    private void writeError(IDMXDeviceException e) {
        dmxDevice = DummyDMXDevice.Singleton;
        e.printStackTrace();
        connector.deviceDisconnected();
        connector.raiseError(e, "There was an error writting to the DMX device.  The program is now using the Dummy DMX device driver and will no longer be outputting to the actual DMX device.");
    }

    private Float getCueNumber(String cueSummary) {
        if (cueSummary == null) return null;
        return Float.parseFloat(cueSummary.substring(0, cueSummary.indexOf(":")));
    }

    /**
   * This private class is used to update the view at fixed intervals
   * during a fade.  It only updates the specified channels for efficiency.
   */
    private class ViewUpdater {

        Timer threadTimer;

        public ViewUpdater(final long period, final Channel[] channels, final CueValueSet cue) {
            threadTimer = new Timer();
            threadTimer.scheduleAtFixedRate(new TimerTask() {

                public void run() {
                    connector.updateChannels(channelValues.getChannels(channels));
                    connector.updateFadeUpProgress((int) (100 * cue.getFadeLevel() + 0.5));
                }
            }, 0, period);
        }

        public ViewUpdater(final long period, final Channel[] channels, final CueValueSet fadeUpCue, final CueValueSet fadeDownCue) {
            threadTimer = new Timer();
            threadTimer.scheduleAtFixedRate(new TimerTask() {

                public void run() {
                    connector.updateChannels(channelValues.getChannels(channels));
                    connector.updateFadeUpProgress((int) (100 * fadeUpCue.getFadeLevel() + 0.5));
                    connector.updateFadeDownProgress((int) (100 * fadeDownCue.getFadeLevel() + 0.5));
                }
            }, 0, period);
        }

        public void cancel() {
            threadTimer.cancel();
        }
    }
}
