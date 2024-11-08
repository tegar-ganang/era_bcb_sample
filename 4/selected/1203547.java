package net.sourceforge.entrainer.eeg.gui;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import net.sourceforge.entrainer.eeg.core.EEGChannelState;
import net.sourceforge.entrainer.eeg.core.EEGDevice;
import net.sourceforge.entrainer.eeg.core.EEGDeviceLoader;
import net.sourceforge.entrainer.eeg.core.EEGSignalProcessor;
import net.sourceforge.entrainer.eeg.core.EEGSignalProcessorLoader;
import net.sourceforge.entrainer.eeg.core.FrequencyType;
import net.sourceforge.entrainer.guitools.GuiUtil;

/**
 * Saves and loads {@link EEGDevice}, {@link FingBrainerState} and
 * {@link EEGChannelState} information between sessions in the properties file
 * 'fingbrainerz.settings.properties'.
 * 
 * @author burton
 */
public class FingBrainerSettings {

    public static final String SAMPLE_RATE = "sample.rate";

    public static final String FREQUENCY_TYPE = "frequency.type";

    public static final String RANGE_FROM = "range.from";

    public static final String RANGE_TO = "range.to";

    public static final String HIGH_PASS = "high.pass";

    public static final String LOW_PASS = "low.pass";

    public static final String RANDOM_COLOURS = "random.colours";

    public static final String FING_COLOUR = "fing.colour";

    public static final String TEXT_COLOUR = "text.colour";

    public static final String DEVICE = "device";

    public static final String SIGNAL_PROCESSOR = "signal.processor";

    public static final String SETTINGS_FILE = "./fingbrainerz.settings.properties";

    private static FingBrainerSettings fbs;

    private Properties settings;

    private double sampleRate;

    private EEGDevice device;

    private EEGSignalProcessor signalProcessor;

    private List<EEGChannelState> savedStates;

    private Map<FrequencyType, FingBrainerState> fingBrainerStates = new HashMap<FrequencyType, FingBrainerState>();

    private EEGDeviceLoader devLoader = EEGDeviceLoader.getInstance();

    private EEGSignalProcessorLoader sigLoader = EEGSignalProcessorLoader.getInstance();

    {
        settings = new Properties();
        try {
            FileInputStream fis = new FileInputStream(SETTINGS_FILE);
            settings.load(fis);
        } catch (IOException e) {
            File file = new File(SETTINGS_FILE);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public static FingBrainerSettings getInstance() {
        if (fbs == null) {
            fbs = new FingBrainerSettings();
        }
        return fbs;
    }

    private FingBrainerSettings() {
        init();
    }

    public List<EEGChannelState> getSavedStates() {
        return new ArrayList<EEGChannelState>(savedStates);
    }

    public void saveSettings(EEGDevice device, Map<FrequencyType, FingBrainerState> fingBrainerStates) {
        setSampleRate(device.getSampleFrequencyInHertz());
        setDevice(device);
        setSignalProcessor(device.getSignalProcessor());
        this.savedStates = device.getChannelStates();
        this.fingBrainerStates = fingBrainerStates;
        saveSettings();
    }

    private void saveSettings() {
        settings.clear();
        settings.put(SAMPLE_RATE, "" + getSampleRate());
        settings.put(DEVICE, getDevice().getDeviceDescription());
        settings.put(SIGNAL_PROCESSOR, getSignalProcessor().getDescription());
        for (EEGChannelState state : savedStates) {
            setChannelProperties(state);
        }
        for (Entry<FrequencyType, FingBrainerState> entry : fingBrainerStates.entrySet()) {
            setFingBrainerState(entry);
        }
        saveProperties();
    }

    private void setFingBrainerState(Entry<FrequencyType, FingBrainerState> entry) {
        String keyPart = entry.getKey().getDescription().replaceAll(" ", "_") + ".";
        FingBrainerState fbs = entry.getValue();
        settings.put(keyPart + RANDOM_COLOURS, Boolean.toString(fbs.isRandomColours()));
        settings.put(keyPart + FING_COLOUR, parseColour(fbs.getFingColour()));
        settings.put(keyPart + TEXT_COLOUR, parseColour(fbs.getTextColour()));
    }

    private Object parseColour(Color c) {
        return c.getRed() + "," + c.getGreen() + "," + c.getBlue() + "," + c.getAlpha();
    }

    private void saveProperties() {
        try {
            settings.store(new FileOutputStream(SETTINGS_FILE), "Settings for FingBrainerz");
        } catch (IOException e) {
            GuiUtil.handleProblem(e);
        }
    }

    private void setChannelProperties(EEGChannelState state) {
        String keyPart = state.getFrequencyType().getDescription().replaceAll(" ", "_") + ".";
        settings.put(keyPart + FREQUENCY_TYPE, state.getFrequencyType().getDescription());
        settings.put(keyPart + RANGE_FROM, "" + state.getRangeFrom());
        settings.put(keyPart + RANGE_TO, "" + state.getRangeTo());
        settings.put(keyPart + HIGH_PASS, Boolean.toString(state.isHighPass()));
        settings.put(keyPart + LOW_PASS, Boolean.toString(state.isLowPass()));
    }

    private void init() {
        savedStates = new ArrayList<EEGChannelState>();
        if (settings.isEmpty()) {
            return;
        }
        setDevice(getDeviceFromSettings());
        setSignalProcessor(getSignalProcessorFromSettings());
        setSampleRate(Double.parseDouble(settings.getProperty(SAMPLE_RATE)));
        List<String> frequencyTypeKeys = getFrequencyTypeKeys();
        List<FrequencyType> frequencyTypes = getFrequencyTypeList(frequencyTypeKeys);
        for (FrequencyType type : frequencyTypes) {
            savedStates.add(getChannelState(type));
            fingBrainerStates.put(type, getFingBrainerState(type));
        }
    }

    private EEGSignalProcessor getSignalProcessorFromSettings() {
        String desc = settings.getProperty(SIGNAL_PROCESSOR);
        List<EEGSignalProcessor> sps = sigLoader.getEEGObjects();
        for (EEGSignalProcessor sp : sps) {
            if (sp.getDescription().equals(desc)) {
                return sp;
            }
        }
        return null;
    }

    private EEGDevice getDeviceFromSettings() {
        String desc = settings.getProperty(DEVICE);
        List<EEGDevice> devices = devLoader.getEEGObjects();
        for (EEGDevice device : devices) {
            if (device.getDeviceDescription().equals(desc)) {
                return device;
            }
        }
        return null;
    }

    private FingBrainerState getFingBrainerState(FrequencyType type) {
        String keyPart = type.getDescription().replaceAll(" ", "_") + ".";
        boolean randomColours = Boolean.parseBoolean(settings.getProperty(keyPart + RANDOM_COLOURS));
        Color fingColour = parseColour(settings.getProperty(keyPart + FING_COLOUR));
        Color textColour = parseColour(settings.getProperty(keyPart + TEXT_COLOUR));
        FingBrainerState fbs = new FingBrainerState();
        fbs.setFingColour(fingColour);
        fbs.setRandomColours(randomColours);
        fbs.setTextColour(textColour);
        return fbs;
    }

    private Color parseColour(String property) {
        StringTokenizer toke = new StringTokenizer(property, ",");
        int[] array = new int[4];
        int i = 0;
        while (toke.hasMoreTokens()) {
            array[i] = Integer.parseInt(toke.nextToken());
            i++;
        }
        return new Color(array[0], array[1], array[2], array[3]);
    }

    private EEGChannelState getChannelState(FrequencyType type) {
        String keyPart = type.getDescription().replaceAll(" ", "_") + ".";
        double rangeFrom = Double.parseDouble(settings.getProperty(keyPart + RANGE_FROM));
        double rangeTo = Double.parseDouble(settings.getProperty(keyPart + RANGE_TO));
        boolean highPass = Boolean.parseBoolean(settings.getProperty(keyPart + HIGH_PASS));
        boolean lowPass = Boolean.parseBoolean(settings.getProperty(keyPart + LOW_PASS));
        EEGChannelState state = new EEGChannelState(type, rangeFrom, rangeTo, getSampleRate());
        state.setHighPass(highPass);
        state.setLowPass(lowPass);
        return state;
    }

    private List<FrequencyType> getFrequencyTypeList(List<String> frequencyTypeKeys) {
        List<FrequencyType> allTypes = FrequencyType.getFrequencyTypes();
        List<FrequencyType> types = new ArrayList<FrequencyType>();
        for (FrequencyType type : allTypes) {
            if (frequencyTypeKeys.contains(type.getDescription())) {
                types.add(type);
            }
        }
        return types;
    }

    private List<String> getFrequencyTypeKeys() {
        List<String> ftKeys = new ArrayList<String>();
        Enumeration<Object> keys = settings.keys();
        String key;
        while (keys.hasMoreElements()) {
            key = keys.nextElement().toString();
            if (key.indexOf(FREQUENCY_TYPE) >= 0) {
                ftKeys.add(settings.getProperty(key));
            }
        }
        return ftKeys;
    }

    public double getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;
    }

    public Map<FrequencyType, FingBrainerState> getFingBrainerStates() {
        return fingBrainerStates;
    }

    public void setFingBrainerStates(Map<FrequencyType, FingBrainerState> fingBrainerStates) {
        this.fingBrainerStates = fingBrainerStates;
    }

    public EEGDevice getDevice() {
        return device;
    }

    public void setDevice(EEGDevice device) {
        this.device = device;
    }

    public EEGSignalProcessor getSignalProcessor() {
        return signalProcessor;
    }

    public void setSignalProcessor(EEGSignalProcessor signalProcessor) {
        this.signalProcessor = signalProcessor;
    }
}
