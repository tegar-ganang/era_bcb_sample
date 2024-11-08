package be.lassi.context;

import static be.lassi.domain.Attribute.INTENSITY;
import static be.lassi.domain.Attribute.PAN;
import static be.lassi.domain.Attribute.PAN_FINE;
import static be.lassi.domain.Attribute.TILT;
import static be.lassi.domain.Attribute.TILT_FINE;
import static be.lassi.util.Util.newArrayList;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;
import javax.sound.midi.MidiDevice;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import be.lassi.base.DirtyIndicator;
import be.lassi.base.Holder;
import be.lassi.control.device.Control;
import be.lassi.control.device.ControlReader;
import be.lassi.control.midi.MidiEngine;
import be.lassi.control.midi.MidiPreferences;
import be.lassi.control.midi.MidiShowControlMessage;
import be.lassi.control.midi.MidiSystemDevices;
import be.lassi.control.midi.ShowControlMessageListener;
import be.lassi.cues.Cue;
import be.lassi.cues.Cues;
import be.lassi.cues.CuesController;
import be.lassi.cues.LightCues;
import be.lassi.cues.Timing;
import be.lassi.domain.Attribute;
import be.lassi.domain.AttributeDefinition;
import be.lassi.domain.LevelController;
import be.lassi.domain.LevelProvider;
import be.lassi.domain.Levels;
import be.lassi.domain.Show;
import be.lassi.domain.ShowBuilder;
import be.lassi.fixtures.DeviceMap;
import be.lassi.fixtures.FixtureCommand;
import be.lassi.fixtures.FixtureCommandProducer;
import be.lassi.fixtures.FixtureControl;
import be.lassi.fixtures.SetColor;
import be.lassi.kernel.ClockListener;
import be.lassi.kernel.Kernel;
import be.lassi.lanbox.ConnectionStatus;
import be.lassi.lanbox.CurrentCueChannelChanges;
import be.lassi.lanbox.Lanbox;
import be.lassi.lanbox.Runner;
import be.lassi.lanbox.commands.Command;
import be.lassi.lanbox.commands.layer.LayerGo;
import be.lassi.lanbox.commands.layer.LayerPause;
import be.lassi.lanbox.commands.layer.LayerResume;
import be.lassi.lanbox.commands.layer.LayerSetFadeTime;
import be.lassi.lanbox.domain.Time;
import be.lassi.library.Library;
import be.lassi.preferences.AllPreferences;
import be.lassi.server.WebServer;
import be.lassi.ui.audio.Audio;
import be.lassi.ui.main.FunctionKeys;
import be.lassi.util.Util;

/**
 *
 *
 *
 */
public class ShowContext implements ClockListener {

    /**
     * Destination for log messages.
     */
    private static final Logger LOGGER = LogManager.getLogger(ShowContext.class);

    /**
     * Indicates whether the show has been changed since the
     * most recent write to disk.
     */
    private final DirtyIndicator dirtyShow = new DirtyIndicator();

    /**
     * Indicates whether the cues have been changed since the
     * most recent write to the Lanbox.
     */
    private final DirtyIndicator dirtyCues = new DirtyIndicator();

    /**
     * The show !!!
     */
    private Show show = ShowBuilder.build(dirtyShow);

    private final AllPreferences preferences;

    private final Lanbox lanbox;

    private final FixtureControl fixtureControl;

    private final FunctionKeys functionKeys;

    /**
     * Collection of ShowContextListener's (that listen for new
     * Show objects).
     */
    private final List<ShowContextListener> listeners = newArrayList();

    /**
     *
     */
    private final Kernel kernel;

    /**
     *
     */
    private final List<LevelController> levelControllers = newArrayList();

    /**
     *
     */
    private final List<LevelProvider> channelLevelProviders = newArrayList();

    /**
     * Buffer that is used for cut, copy and paste operations
     * on the show CueList.
     */
    private Cues cuesPasteBuffer;

    private final CuesController cuesController;

    /**
     * The default timing values that are used when new cues are created.
     */
    private final Timing defaultTiming = new Timing(Time.TIME_2S);

    /**
     * The timing parameters that are used when making changes to level
     * values in the Sheet user interface.
     */
    private final Timing changeTiming = new Timing(Time.TIME_2S);

    private final WebServer server;

    private final Holder<Control> controlHolder = new Holder<Control>();

    private final MidiEngine midiEngine;

    private final Audio audio = new Audio();

    private final ColorSelection colorSelection = new ColorSelection();

    private final Library library = new Library();

    private final ConnectionStatus connectionStatus = new ConnectionStatus();

    private final Runner runner;

    /**
     * Constructs a new instance.
     */
    public ShowContext() {
        this(new AllPreferences());
    }

    /**
     * Constructs a new instance.
     *
     * @param preferences the user preferences
     */
    public ShowContext(final AllPreferences preferences) {
        runner = new Runner(this);
        cuesController = new CuesController(this);
        this.preferences = preferences;
        kernel = new Kernel(this);
        levelControllers.add(getShow().getSubmasters());
        levelControllers.add(getShow().getCueFading());
        addChannelLevelProvider(getShow().getSubmasters());
        addChannelLevelProvider(getShow().getCueFading());
        lanbox = new Lanbox(this);
        fixtureControl = new FixtureControl(this, lanbox.getEngine());
        String path = "library/controls/BCF2000-faders-and-encoders.xml";
        try {
            Control control = new ControlReader().read(path);
            controlHolder.setValue(control);
        } catch (IOException e) {
            LOGGER.error("Could not read \"" + path + "\": " + e.getMessage());
        }
        midiEngine = new MidiEngine(controlHolder);
        midiEngine.addShowControlMessageListener(new ShowControlMessageListener() {

            public void received(final MidiShowControlMessage message) {
                int layerNumber = message.getDeviceId();
                if (layerNumber == 0) {
                    layerNumber = 1;
                }
                if (message.getCommand() == MidiShowControlMessage.GO) {
                    if ("".equals(message.getCueNumber())) {
                        runner.next(layerNumber);
                    } else {
                        if ("".equals(message.getCueList())) {
                            String[] strings = message.getCueNumber().split("\\.");
                            if (strings.length == 2) {
                                int cueListNumber = Util.toInt(strings[0]);
                                int cueStepNumber = Util.toInt(strings[1]);
                                LayerGo command = new LayerGo(layerNumber, cueListNumber, cueStepNumber);
                                lanbox.execute(command);
                            }
                        } else {
                            Cue cue = getShow().getCues().getCueWithNumber(message.getCueNumber());
                            if (cue != null) {
                                int cueIndex = getShow().getCues().indexOf(cue);
                                getShow().getCues().setCurrent(cueIndex);
                                runner.next(layerNumber);
                            }
                        }
                    }
                } else if (message.getCommand() == MidiShowControlMessage.STOP) {
                    LayerPause command = new LayerPause(layerNumber);
                    lanbox.execute(command);
                } else if (message.getCommand() == MidiShowControlMessage.RESUME) {
                    LayerResume command = new LayerResume(layerNumber);
                    lanbox.execute(command);
                }
            }
        });
        Attribute intensity = new Attribute(new AttributeDefinition(INTENSITY, ""));
        Attribute pan = new Attribute(new AttributeDefinition(PAN, ""));
        Attribute tilt = new Attribute(new AttributeDefinition(TILT, ""));
        Attribute zoom = new Attribute(new AttributeDefinition("Zoom", ""));
        Attribute iris = new Attribute(new AttributeDefinition("Iris", ""));
        Attribute focus = new Attribute(new AttributeDefinition("Focus", ""));
        Attribute shutter = new Attribute(new AttributeDefinition("Shutter", ""));
        Attribute color = new Attribute(new AttributeDefinition("Color", ""));
        Attribute speed = new Attribute(new AttributeDefinition("Speed", ""));
        Attribute panFine = new Attribute(new AttributeDefinition(PAN_FINE, ""));
        Attribute tiltFine = new Attribute(new AttributeDefinition(TILT_FINE, ""));
        Attribute gobo = new Attribute(new AttributeDefinition("Gobo", ""));
        Attribute goboRot = new Attribute(new AttributeDefinition("Gobo Rot", ""));
        Attribute goboPos = new Attribute(new AttributeDefinition("Gobo Pos", ""));
        Attribute gobo2 = new Attribute(new AttributeDefinition("Gobo2", ""));
        DeviceMap map = new DeviceMap();
        map.put(intensity.getDefinition().getName(), controlHolder.getValue().getLevelControl(0));
        map.put(pan.getDefinition().getName(), controlHolder.getValue().getLevelControl(1));
        map.put(tilt.getDefinition().getName(), controlHolder.getValue().getLevelControl(2));
        map.put(zoom.getDefinition().getName(), controlHolder.getValue().getLevelControl(3));
        map.put(iris.getDefinition().getName(), controlHolder.getValue().getLevelControl(4));
        map.put(focus.getDefinition().getName(), controlHolder.getValue().getLevelControl(5));
        map.put(shutter.getDefinition().getName(), controlHolder.getValue().getLevelControl(6));
        map.put(color.getDefinition().getName(), controlHolder.getValue().getLevelControl(7));
        map.put(speed.getDefinition().getName(), controlHolder.getValue().getLevelControl(0));
        map.put(panFine.getDefinition().getName(), controlHolder.getValue().getLevelControl(1));
        map.put(tiltFine.getDefinition().getName(), controlHolder.getValue().getLevelControl(2));
        map.put(gobo.getDefinition().getName(), controlHolder.getValue().getLevelControl(3));
        map.put(goboRot.getDefinition().getName(), controlHolder.getValue().getLevelControl(4));
        map.put(goboPos.getDefinition().getName(), controlHolder.getValue().getLevelControl(5));
        map.put(gobo2.getDefinition().getName(), controlHolder.getValue().getLevelControl(6));
        FixtureCommandProducer producer = new FixtureCommandProducer(fixtureControl);
        producer.setDeviceMap(map);
        new CurrentCueChannelChanges(this, lanbox.getEngine());
        kernel.addClockListener(this);
        server = new WebServer(this);
        functionKeys = new FunctionKeys(this);
        final MidiPreferences mp = preferences.getMidiPreferences();
        String name = mp.getInputDeviceName();
        MidiDevice midiInputDevice = new MidiSystemDevices().getInput(name);
        midiEngine.setMidiInputDevice(midiInputDevice);
        name = mp.getOutputDeviceName();
        MidiDevice midiOutputDevice = new MidiSystemDevices().getOutput(name);
        midiEngine.setMidiOutputDevice(midiOutputDevice);
        if (mp.isShowControlEnabled()) {
            name = mp.getShowControlDeviceName();
            MidiDevice showControlInputDevice = new MidiSystemDevices().getInput(name);
            midiEngine.setShowControlInputDevice(showControlInputDevice);
        }
        mp.addPropertyChangeListener(MidiPreferences.INPUT_DEVICE_NAME, new PropertyChangeListener() {

            public void propertyChange(final PropertyChangeEvent evt) {
                String n = mp.getInputDeviceName();
                MidiDevice mid = new MidiSystemDevices().getInput(n);
                midiEngine.setMidiInputDevice(mid);
            }
        });
        mp.addPropertyChangeListener(MidiPreferences.OUTPUT_DEVICE_NAME, new PropertyChangeListener() {

            public void propertyChange(final PropertyChangeEvent evt) {
                String n = mp.getOutputDeviceName();
                MidiDevice mid = new MidiSystemDevices().getOutput(n);
                midiEngine.setMidiOutputDevice(mid);
            }
        });
        mp.addPropertyChangeListener(MidiPreferences.SHOW_CONTROL_DEVICE_NAME, new PropertyChangeListener() {

            public void propertyChange(final PropertyChangeEvent evt) {
                if (mp.isShowControlEnabled()) {
                    String n = mp.getShowControlDeviceName();
                    MidiDevice mid = new MidiSystemDevices().getInput(n);
                    midiEngine.setShowControlInputDevice(mid);
                }
            }
        });
        mp.addPropertyChangeListener(MidiPreferences.SHOW_CONTROL_ENABLED, new PropertyChangeListener() {

            public void propertyChange(final PropertyChangeEvent evt) {
                if (mp.isShowControlEnabled()) {
                    String n = mp.getShowControlDeviceName();
                    MidiDevice mid = new MidiSystemDevices().getInput(n);
                    midiEngine.setShowControlInputDevice(mid);
                } else {
                    midiEngine.setShowControlInputDevice(null);
                }
            }
        });
        colorSelection.addPropertyChangeListener(ColorSelection.PROPERTY_COLOR, new PropertyChangeListener() {

            public void propertyChange(final PropertyChangeEvent evt) {
                FixtureCommand command = new SetColor(colorSelection.getColor());
                fixtureControl.process(command);
            }
        });
        LOGGER.debug("initialized");
    }

    /**
     *
     *
     */
    public void fade() {
        if (getShow().getCues().getCurrentIndex() != -1) {
            lanbox.fade();
        }
    }

    /**
     *
     *
     */
    public void go() {
        Cues cues = getShow().getCues();
        if (cues.getCurrentIndex() != -1) {
            int index = cues.getCurrentIndex();
            fade();
            if (++index >= cues.size()) {
                index = -1;
            }
            cues.setCurrent(index);
        }
    }

    public void record() {
        Cues cues = getShow().getCues();
        LightCues lightCues = cues.getLightCues();
        Levels stage = lanbox.getMixer().getLevels();
        for (Cue cue : cues) {
            if (cue.isSelected()) {
                if (cue.isLightCue()) {
                    int cueIndex = cue.getLightCueIndex();
                    for (int channelIndex = 0; channelIndex < getShow().getNumberOfChannels(); channelIndex++) {
                        float levelValue = stage.get(channelIndex).getValue();
                        lightCues.setChannel(cueIndex, channelIndex, levelValue);
                    }
                }
            }
        }
    }

    /**
     * Adds a level controller.
     *
     * @param levelController the level controller to be added
     */
    public void add(final LevelController levelController) {
        levelControllers.add(levelController);
    }

    /**
     * Adds a show context listener.
     *
     * @param listener the show context listener to be added
     */
    public void addShowContextListener(final ShowContextListener listener) {
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void clockTick(final long now) {
    }

    /**
     * Copy the currently selected cues in the <code>CueList</code>
     * into the cue paste buffer.
     *
     */
    public void cuesCopy() {
        cuesPasteBuffer = getShow().getCues().copySelectedCues();
    }

    /**
     * Remove the currently selected cues in the CueList after copying them
     * into the cue paste buffer.
     *
     */
    public void cuesCut() {
        cuesCopy();
        getShow().getCues().removeSelectedCues();
    }

    /**
     * Insert the <code>Cue</code>s in the cue paste buffer into the
     * <code>CueList</code> at given index.
     *
     * @param cueIndex the position in the cue list at which to insert
     */
    public void cuesPasteInsert(final int cueIndex) {
        if (cuesPasteBuffer != null) {
            getShow().getCues().insert(cueIndex, cuesPasteBuffer.copy());
        }
    }

    /**
     * Answer the level for the channel with given index, using
     * the HTP (Highest Takes Precidence) principle.
     *
     * @param now the current system time
     * @param channelIndex the channel index
     * @return the level value
     */
    public float getLevel(final long now, final int channelIndex) {
        float max = 0;
        for (LevelController controller : levelControllers) {
            float level = controller.getLevelValue(now, channelIndex);
            if (level > max) {
                max = level;
            }
        }
        return max;
    }

    /**
     * Gets the current show.
     *
     * @return Show the current show
     */
    public Show getShow() {
        return show;
    }

    /**
     * Notifies listeners that the Show is about to change.  The listeners
     * should perform any finialization work on the old show now.
     */
    public void notifyListenersPreChange() {
        for (ShowContextListener listener : listeners) {
            listener.preShowChange();
        }
    }

    /**
     * Notifies listeners that the Show is has changed.  The listeners
     * should perform any initialization work for the new Show now.
     */
    public void notifyListenersPostChange() {
        for (ShowContextListener listener : listeners) {
            listener.postShowChange();
        }
    }

    /**
     * Removes a show context listener.
     *
     * @param listener the listener to be remove
     */
    public void removeShowContextListener(final ShowContextListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sets a new show.
     *
     * @param newShow the show to be set
     */
    public void setShow(final Show newShow) {
        if (show != newShow) {
            notifyListenersPreChange();
            show = newShow;
            notifyListenersPostChange();
        }
    }

    /**
     * Gets the number of channel level providers.
     *
     * @return the number of channel level providers
     */
    public int getNumberOfChannelLevelProviders() {
        return channelLevelProviders.size();
    }

    /**
     * Adds a channel level provider.
     *
     * @param levelProvider the channel level provider to be added
     */
    public void addChannelLevelProvider(final LevelProvider levelProvider) {
        channelLevelProviders.add(levelProvider);
    }

    /**
     * Removes given channel level provider.
     *
     * @param levelProvider the channel level provider to be removed
     */
    public void removeChannelLevelProvider(final LevelProvider levelProvider) {
        channelLevelProviders.remove(levelProvider);
    }

    /**
     * Gets the channel level provider at given index.
     *
     * @param index the channel level provider index
     * @return the channel level provider at given index
     */
    public LevelProvider getChannelLevelProvider(final int index) {
        return channelLevelProviders.get(index);
    }

    /**
     * Gets the kernel.
     *
     * @return the kernel
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * Gets the change timing.
     *
     * @return the change timing
     */
    public Timing getChangeTiming() {
        return changeTiming;
    }

    /**
     * Gets the default timing.
     *
     * @return the default timing
     */
    public Timing getDefaultTiming() {
        return defaultTiming;
    }

    /**
     * Set the timing parameters for changes in the Sheet user interface.
     * Note that the variable itself is not changed, but rather the timing
     * values within the timing object are overwritten.
     *
     * @param timing the new timing parameters
     */
    public void setChangeTiming(final Timing timing) {
        changeTiming.set(timing);
        Time time = timing.getFadeInTime();
        Command command = new LayerSetFadeTime(Lanbox.ENGINE_SHEET, time);
        lanbox.execute(command);
    }

    /**
     * Set the default timing parameters that will be used when creating
     * new cues.
     * Note that the variable itself is not changed, but rather the timing
     * values within the timing object are overwritten.
     *
     * @param timing the new timing parameters
     */
    public void setDefaultTiming(final Timing timing) {
        defaultTiming.set(timing);
    }

    /**
     * Executes given lanbox command.
     *
     * @param command the lanbox command to be executed
     */
    public void execute(final Command command) {
        lanbox.execute(command);
    }

    /**
     * Gets the lanbox.
     *
     * @return the lanbox
     */
    public Lanbox getLanbox() {
        return lanbox;
    }

    public FixtureControl getFixtureControl() {
        return fixtureControl;
    }

    /**
     * Closes the lanbox.
     */
    public void close() {
        kernel.close();
        lanbox.close();
        midiEngine.close();
    }

    /**
     * Gets all preferences.
     *
     * @return all preferences
     */
    public AllPreferences getPreferences() {
        return preferences;
    }

    public WebServer getServer() {
        return server;
    }

    public FunctionKeys getFunctionKeys() {
        return functionKeys;
    }

    public CuesController getCuesController() {
        return cuesController;
    }

    public DirtyIndicator getDirtyShow() {
        return dirtyShow;
    }

    public DirtyIndicator getDirtyCues() {
        return dirtyCues;
    }

    public Holder<Control> getControlHolder() {
        return controlHolder;
    }

    public MidiEngine getMidiEngine() {
        return midiEngine;
    }

    public Audio getAudio() {
        return audio;
    }

    public ColorSelection getColorSelection() {
        return colorSelection;
    }

    public Library getLibrary() {
        return library;
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public Runner getRunner() {
        return runner;
    }
}
