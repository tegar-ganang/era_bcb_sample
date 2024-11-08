package org.jsynthlib.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Device class defines some informations for your synthesizer, such as
 * a name of manufacturer, the model name, author(s) of drivers, etc.
 * It also manages a list of Driver classes which provides actual
 * functions.<p>
 *
 * Subclass must define two constructor, with no argument and with one
 * argument, <code>Preferences</code>.  Here is an example;
 * <pre>
 *    import java.util.prefs.Preferences;
 *    ...
 *    // Constructor for DeviceListWriter.
 *    public KawaiK4Device() {
 *        super("Kawai", "K4/K4R", "F07E..0602400000040000000000f7",
 *              INFO_TEXT, "Brian Klock & Gerrit Gehnen");
 *    }
 *
 *    // Constructor for for actual work.
 *    public KawaiK4Device(Preferences prefs) {
 *        this();
 *        this.prefs = prefs;
 *
 *      addDriver(new KawaiK4BulkConverter());
 *      ...
 *    }
 * </pre>
 * Compatibility Note: The following fields are now
 * <code>private</code>. Use setter/getter method to access them.
 * <pre>
 *        manufacturerName, modelName, inquiryID, infoText, authors,
 *        synthName, channel, inPort, port
 * </pre>
 * Created on 5. Oktober 2001, 21:59
 * @author Gerrit Gehnen
 * @see IDriver
 */
public abstract class Device implements Comparable<Device> {

    /**
     * The default inquiry id.
     */
    private static final String DEFAULT_INQUIRY_ID = "NONE";

    /**
     * The default info text.
     */
    private static final String DEFAULT_INFO_TEXT = "There is no information about this device.";

    /**
     * The name of the manufacturer of the MIDI device.
     */
    private final String manufacturerName;

    /**
     * The name of the model name of the of the MIDI device.
     */
    private final String modelName;

    /**
     * The response to the Universal Inquiry Message.  It is a
     * regular expression. It can be up to 16 bytes.<p>
     * Ex. <code>"F07E..0602413F01000000020000f7"</code>
     */
    private final String inquiryID;

    /**
     * Information about Device.
     * @see DeviceDetailsDialog
     */
    private final String infoText;

    /** Authors of the device driver. */
    private final String authors;

    /** Preferences node for storing configuration options. */
    protected Preferences prefs;

    /** set to true when initialization of MIDI output is done. */
    private boolean initPort;

    /** MIDI output Receiver */
    private Receiver rcvr;

    /** set to true when initialization of MIDI input is done. */
    private boolean initInPort;

    /**
     * MIDI message size. If zero, whole MIDI message is passed to lower MIDI
     * driver.
     */
    private int midiOutBufSize = 0;

    /** delay (msec) after every MIDI message transfer. */
    private int midiOutDelay = 0;

    /**
     * The drivers available for the MIDI device.
     */
    private List<IDriver> drivers = new ArrayList<IDriver>();

    /**
     * Creates a new <code>Device</code> instance.
     *
     * @param manufacturerName The company which made the Synthesizer.
     * @param modelName The fixed name of the model supported by
     * this driver, as stated on the type plate of the engine. eg
     * TG33/SY22
     * @param inquiryID The response to the Universal Inquiry Message.
     * It can have wildcards (*). It can be up to 16 bytes.
     * Ex. <code>"F07E**0602413F01000000020000f7"</code>
     * @param infoText Information about Device.
     * @param authors Authors of the device driver.
     */
    public Device(String manufacturerName, String modelName, String inquiryID, String infoText, String authors) {
        this.manufacturerName = manufacturerName;
        this.modelName = modelName;
        this.inquiryID = inquiryID != null ? inquiryID : DEFAULT_INQUIRY_ID;
        this.infoText = infoText != null ? infoText : DEFAULT_INFO_TEXT;
        this.authors = authors;
    }

    /** Called after Device(Preferences prefs) is called. */
    protected void setup() throws MidiUnavailableException {
        setInPort(getInPort());
        setPort(getPort());
    }

    public final Preferences getPreferences() {
        return prefs;
    }

    /**
     * Create a configuration panel for the device. Subclasses may override this
     * method to provide a custom configuration panel.
     *
     * @return a configuration panel for the device
     */
    protected JPanel config() {
        JPanel panel = new JPanel();
        panel.add(new JLabel("There is no configuration options for this device."));
        return panel;
    }

    /**
     * Getter for property getManufacturerName.
     * @return Value of property getManufacturerName.
     */
    public final String getManufacturerName() {
        return manufacturerName;
    }

    /**
     * Getter for property modelName.
     * @return Value of property modelName.
     */
    public final String getModelName() {
        return modelName;
    }

    /**
     * Getter for property inquiryID.
     * @return Value of property inquiryID.
     */
    public final String getInquiryID() {
        return inquiryID;
    }

    /**
     * Getter for property infoText.
     * @return Value of property infoText.
     */
    public final String getInfoText() {
        return infoText;
    }

    /**
     * Getter for property authors.
     * @return Value of property authors.
     */
    public final String getAuthors() {
        return authors;
    }

    /**
     * Getter for property synthName.
     * @return Value of property synthName.
     */
    public String getSynthName() {
        return prefs.get("synthName", modelName);
    }

    /**
     * Setter for property synthName.  The synthName is your personal
     * naming of the device.  A user can change it in the first column
     * of the Synth-Configuration dialog.  modelName is used as
     * default value. A synth driver should not use this.
     * @param synthName New value of property synthName.
     */
    protected final void setSynthName(String synthName) {
        prefs.put("synthName", synthName);
    }

    /**
     * Getter for property channel.
     * @return Value of property channel.
     */
    public final int getChannel() {
        return prefs.getInt("channel", 1);
    }

    /**
     * Setter for property channel which is used for playPatch, etc.
     * The value must be 1 or greater than 1, and 16 or less than 16.
     * A synth driver may use this method to set default value.<p>
     * Some old drivers use this for device ID.  Use setDeviceID
     * method to set device ID.
     * @param channel The value must be 1 or greater than 1, and 16 or
     * less than 16.
     */
    protected final void setChannel(int channel) {
        prefs.putInt("channel", channel);
    }

    /**
     * Getter for property deviceID.
     * @return Value of property deviceID.
     */
    public final int getDeviceID() {
        int deviceID = prefs.getInt("deviceID", -1);
        return deviceID == -1 ? getChannel() : deviceID;
    }

    /**
     * Setter for property deviceID.  The value must be 1 or greater
     * than 1, and 256 or less than 256.  A synth driver may use this
     * to set default device ID.<p>
     * For backward compatibility if this has the initial value (-1),
     * The value of <code>channel</code> is used as device ID.
     * @param deviceID The value must be 1 or greater than 1, and 256
     * or less than 256.
     */
    protected final void setDeviceID(int deviceID) {
        prefs.putInt("deviceID", deviceID);
    }

    /**
     * Getter for property port (MIDI output port).
     * @return Value of property port.
     */
    public final int getPort() {
        String uniqueName = prefs.get("port", "");
        List<String> outputNames = MidiUtil.getOutputDeviceNames();
        for (int i = 0; i < outputNames.size(); ++i) {
            if (uniqueName.equals(outputNames.get(i))) {
                return i;
            }
        }
        return AppConfig.getInstance().getInitPortOut();
    }

    /**
     * Setter for property port, the MIDI output port number, where
     * the cable <B>to</B> the device is connected.  A synth driver
     * should not use this.
     * @param port New value of property port.
     */
    protected final void setPort(int port) {
        if (!MidiUtil.isOutputAvailable()) {
            return;
        }
        if (!initPort || getPort() != port) {
            try {
                rcvr = MidiUtil.getReceiver(port);
            } catch (MidiUnavailableException e) {
                Logger.reportStatus(e);
            }
        }
        prefs.put("port", MidiUtil.getOutputDeviceName(port));
        initPort = true;
    }

    /**
     * If the target MIDI device cannot handle a whole Sysex message and
     * requires to divide the Sysex Message into several small messages, use
     * this method.
     *
     * @param bufSize
     *            MIDI message size. If zero, whole MIDI message is passed to
     *            lower MIDI driver.
     * @param delay
     *            delay (msec) after every MIDI message transfer.
     */
    protected final void setMidiBufSize(int bufSize, int delay) {
        midiOutBufSize = bufSize;
        midiOutDelay = delay;
    }

    /**
     * send MidiMessage to MIDI output. Called by Driver.send().
     *
     * @param message the message to send
     */
    public final void send(final MidiMessage message) {
        if (rcvr == null) {
            return;
        }
        try {
            AppConfig appConfig = AppConfig.getInstance();
            MidiUtil.send(rcvr, message, Math.min(midiOutBufSize, appConfig.getMidiOutBufSize()), Math.max(midiOutDelay, appConfig.getMidiOutDelay()));
        } catch (Exception e) {
            Logger.reportStatus(e);
        }
    }

    /**
     * Getter for property inPort.
     * @return Value of property inPort.
     */
    public final int getInPort() {
        String uniqueName = prefs.get("inPort", "");
        int i = 0;
        for (String inputName : MidiUtil.getInputDeviceNames()) {
            if (inputName.equals(uniqueName)) {
                return i;
            }
            ++i;
        }
        return AppConfig.getInstance().getInitPortIn();
    }

    /**
     * Setter for property inPort, the MIDI input port number, where
     * the cable <B>to</B> the device is connected.  A synth driver
     * should not use this.
     * @param inPort New value of property inPort.
     */
    protected final void setInPort(final int inPort) throws MidiUnavailableException {
        if (!MidiUtil.isInputAvailable()) {
            return;
        }
        if (!initInPort || getInPort() != inPort) {
            MidiUtil.setSysexInputQueue(inPort);
        }
        prefs.put("inPort", MidiUtil.getInputDeviceName(inPort));
        initInPort = true;
    }

    /**
     * Get the number of drivers available for the MIDI device.
     *
     * @return the number of drivers available for the MIDI device
     */
    public final int getDriverCount() {
        return drivers.size();
    }

    /**
     * Get the drivers available for the MIDI device.
     *
     * @return the drivers available for the MIDI device
     */
    public final List<IDriver> getDrivers() {
        return Collections.unmodifiableList(drivers);
    }

    /**
     * Get a driver available for the MIDI device.
     *
     * @param index the index of the driver available for the MIDI device
     * @return the driver available for the MIDI device
     */
    public final IDriver getDriver(final int index) {
        return drivers.get(index);
    }

    /**
     * Add a driver available for the MIDI device.
     *
     * @param driver the driver available for the MIDI device
     */
    public final void addDriver(final IDriver driver) {
        drivers.add(driver);
    }

    /** getter for device number. */
    final int getDeviceNum() {
        return AppConfig.getInstance().getDeviceIndex(this);
    }

    /**
     * Getter for DeviceName.
     * @return String of Device Name with inPort and Channel.
     */
    public String getDeviceName() {
        String di = MidiUtil.getOutputDeviceName(getPort());
        return getManufacturerName() + " " + getModelName() + " <" + getSynthName() + ">  -  MIDI Out Port: " + ((di == "") ? "None" : di) + "  -  MIDI Channel: " + getChannel();
    }

    /**
     * Same as <code>getDeviceName()</code>.
     * See #getDeviceName
     */
    @Override
    public String toString() {
        return getDeviceName();
    }

    @Override
    public int compareTo(final Device d) {
        return getDeviceName().compareTo(d.getDeviceName());
    }
}
