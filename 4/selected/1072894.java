package org.jsresources.apps.keyboard;

import java.awt.event.KeyEvent;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;

public class KeyboardModel {

    public static final int TOP_MANUAL = 0;

    public static final int BOTTOM_MANUAL = 1;

    public static final int NUM_MANUALS = 2;

    public static final int DEFAULT_VELOCITY = 64;

    public static final int NUM_KEYCODES = 256;

    private static final String MIDI_DEVICE = "midi_device_";

    public static final String SEQUENCE_CHANGED = "SEQUENCE_CHANGED";

    public static final String SEQUENCER_CHANGED = "SEQUENCER_CHANGED";

    public static final String SEQUENCER_STATE_CHANGED = "SEQUENCER_STATE_CHANGED";

    public static final String KEYMAP_FILE_CHANGED = "KEYMAP_FILE_CHANGED";

    private static MidiDevice.Info[] sm_aFilteredInfos;

    private PropertyChangeSupport m_propertyChangeSupport;

    private MidiDevice.Info[] m_aMidiDeviceInfos;

    private MidiDevice[] m_aMidiDevices;

    private Receiver[] m_aReceivers;

    private int[] m_anVelocity;

    private int[] m_anVolume;

    private Preferences m_userPreferences;

    private int[] m_anManualFromKeyCode;

    private int[] m_anNoteFromKeyCode;

    /**	Filename of the MIDI file currently loaded.
		It is maintained solely for information purposes.
		It may be null, which means that no file has been loaded
		so far.

		@see #setKeymapFile()
		@see #getKeymapFile()
	*/
    private File m_keymapFile;

    /**
	 */
    public KeyboardModel() {
        m_propertyChangeSupport = new PropertyChangeSupport(this);
        m_aMidiDeviceInfos = new MidiDevice.Info[NUM_MANUALS];
        m_aMidiDevices = new MidiDevice[NUM_MANUALS];
        m_aReceivers = new Receiver[NUM_MANUALS];
        m_anVelocity = new int[NUM_MANUALS];
        for (int i = 0; i < m_anVelocity.length; i++) {
            m_anVelocity[i] = DEFAULT_VELOCITY;
        }
        m_anVolume = new int[NUM_MANUALS];
        m_anManualFromKeyCode = new int[NUM_KEYCODES];
        m_anNoteFromKeyCode = new int[NUM_KEYCODES];
        m_userPreferences = Preferences.userNodeForPackage(this.getClass());
        loadPreferences();
        setKeymapFile(new File("keymap.txt"));
    }

    public void setKeymapFile(File keymapFile) {
        File oldKeymapFile = getKeymapFile();
        m_keymapFile = keymapFile;
        loadKeymap(m_keymapFile);
        firePropertyChange(KEYMAP_FILE_CHANGED, oldKeymapFile, keymapFile);
        if (Debug.getTraceKeyboardModel()) {
            Debug.out("KeyboardModel.setKeymapFile(): end");
        }
    }

    public File getKeymapFile() {
        return m_keymapFile;
    }

    private void loadKeymap(File file) {
        for (int i = 0; i < m_anManualFromKeyCode.length; i++) {
            m_anManualFromKeyCode[i] = -1;
            m_anNoteFromKeyCode[i] = -1;
        }
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader reader = new BufferedReader(fileReader);
            String strLine = reader.readLine();
            int nManual = -1;
            while (strLine != null) {
                if (strLine.equals("first")) {
                    nManual = 0;
                } else if (strLine.equals("second")) {
                    nManual = 1;
                } else if (strLine.length() != 0) {
                    String[] str = strLine.split(" ");
                    String strKeyName = str[0].toLowerCase();
                    int nNote = Integer.parseInt(str[1]);
                    int nKeyCode = -1;
                    for (int i = 0; i < 400; i++) {
                        if (KeyEvent.getKeyText(i).toLowerCase().equals(strKeyName)) {
                            nKeyCode = i;
                            break;
                        }
                    }
                    if (nKeyCode != -1) {
                        m_anManualFromKeyCode[nKeyCode] = nManual;
                        m_anNoteFromKeyCode[nKeyCode] = nNote;
                    }
                }
                strLine = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMidiDeviceInfo(int nManual, MidiDevice.Info info) {
        MidiDevice device = null;
        try {
            device = MidiSystem.getMidiDevice(info);
        } catch (MidiUnavailableException e) {
            Debug.out(e);
        }
        if (m_aMidiDevices[nManual] != null) {
            allNotesOff(nManual);
            m_aMidiDevices[nManual].close();
        }
        m_aMidiDeviceInfos[nManual] = info;
        m_aMidiDevices[nManual] = device;
        try {
            m_aMidiDevices[nManual].open();
            m_aReceivers[nManual] = m_aMidiDevices[nManual].getReceiver();
        } catch (MidiUnavailableException e) {
            Debug.out(e);
        }
    }

    public MidiDevice.Info getMidiDeviceInfo(int nManual) {
        return m_aMidiDeviceInfos[nManual];
    }

    private Receiver getReceiver(int nManual) {
        return m_aReceivers[nManual];
    }

    public void closeAllMidiDevices() {
        for (int i = 0; i < m_aMidiDevices.length; i++) {
            m_aMidiDevices[i].close();
        }
    }

    public void noteOn(int nKeyCode) {
        note(ShortMessage.NOTE_ON, nKeyCode);
    }

    public void noteOff(int nKeyCode) {
        note(ShortMessage.NOTE_OFF, nKeyCode);
    }

    public boolean isRecognizedKeyCode(int nKeyCode) {
        if (nKeyCode <= m_anManualFromKeyCode.length) {
            return (m_anManualFromKeyCode[nKeyCode] != -1);
        } else {
            return false;
        }
    }

    private void note(int nCommand, int nKeyCode) {
        if (nKeyCode <= m_anManualFromKeyCode.length) {
            int nManual = m_anManualFromKeyCode[nKeyCode];
            if (nManual != -1) {
                ShortMessage sm = new ShortMessage();
                try {
                    sm.setMessage(nCommand, getChannel(nManual), getNote(nManual, nKeyCode), getVelocity(nManual));
                } catch (InvalidMidiDataException e) {
                    Debug.out(e);
                }
                getReceiver(nManual).send(sm, -1);
            } else {
                Debug.out("Unrecognized keyCode " + nKeyCode);
            }
        } else {
            Debug.out("Unrecognized keyCode " + nKeyCode);
        }
    }

    private int getNote(int nManual, int nKeyCode) {
        return m_anNoteFromKeyCode[nKeyCode] + getStartEighth(nManual) * 12;
    }

    private int getStartEighth(int nManual) {
        return 3;
    }

    private int getChannel(int nManual) {
        return 0;
    }

    public int getVelocity(int nManual) {
        return m_anVelocity[nManual];
    }

    public void setVelocity(int nManual, int nVelocity) {
        m_anVelocity[nManual] = nVelocity;
    }

    public int getVolume(int nManual) {
        return m_anVolume[nManual];
    }

    public void setVolume(int nManual, int nVolume) {
        m_anVolume[nManual] = nVolume;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        m_propertyChangeSupport.addPropertyChangeListener(listener);
    }

    private void allNotesOff(int nManual) {
        controlChange(nManual, 0x7B, 0);
    }

    private void controlChange(int nManual, int nController, int nValue) {
        ShortMessage sm = new ShortMessage();
        try {
            sm.setMessage(ShortMessage.CONTROL_CHANGE, getChannel(nManual), nController, nValue);
        } catch (InvalidMidiDataException e) {
            Debug.out(e);
        }
        getReceiver(nManual).send(sm, -1);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        m_propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String strPropertyName, PropertyChangeListener listener) {
        m_propertyChangeSupport.addPropertyChangeListener(strPropertyName, listener);
    }

    public void removePropertyChangeListener(String strPropertyName, PropertyChangeListener listener) {
        m_propertyChangeSupport.removePropertyChangeListener(strPropertyName, listener);
    }

    private void firePropertyChange(String strPropertyName, Object oldValue, Object newValue) {
        m_propertyChangeSupport.firePropertyChange(strPropertyName, oldValue, newValue);
    }

    public void savePreferences() {
        for (int nManual = 0; nManual < m_aMidiDevices.length; nManual++) {
            String strDeviceName = getMidiDeviceInfo(nManual).getName();
            m_userPreferences.put(getMidiDevicePropertyName(nManual), strDeviceName);
        }
        try {
            m_userPreferences.flush();
        } catch (BackingStoreException e) {
            if (Debug.getTraceKeyboardModel() || Debug.getTraceAllExceptions()) {
                Debug.out(e);
            }
        }
    }

    private void loadPreferences() {
        for (int nManual = 0; nManual < m_aMidiDevices.length; nManual++) {
            String strMidiDeviceName = m_userPreferences.get(getMidiDevicePropertyName(nManual), null);
            MidiDevice.Info info = null;
            if (strMidiDeviceName != null) {
                info = getMidiDeviceInfo(strMidiDeviceName, false, false);
            }
            MidiDevice midiDevice = null;
            try {
                if (info == null) {
                    info = MidiSystem.getSynthesizer().getDeviceInfo();
                }
                setMidiDeviceInfo(nManual, info);
            } catch (MidiUnavailableException e) {
                Debug.out(e);
            }
        }
    }

    private static String getMidiDevicePropertyName(int nManual) {
        return MIDI_DEVICE + nManual;
    }

    private static MidiDevice.Info getMidiDeviceInfo(String strDeviceName, boolean receiverRequired, boolean transmitterRequired) {
        MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < aInfos.length; i++) {
            if (aInfos[i].getName().equals(strDeviceName)) {
                if (receiverRequired || transmitterRequired) {
                    try {
                        MidiDevice dev = MidiSystem.getMidiDevice(aInfos[i]);
                        if ((!receiverRequired || (dev.getMaxReceivers() != 0)) && (!transmitterRequired || (dev.getMaxTransmitters() != 0))) {
                            return aInfos[i];
                        }
                    } catch (Exception e) {
                    }
                } else {
                    return aInfos[i];
                }
            }
        }
        return null;
    }

    public static MidiDevice.Info[] getMidiDeviceInfos() {
        if (sm_aFilteredInfos == null) {
            MidiDevice.Info[] aInfos = MidiSystem.getMidiDeviceInfo();
            List<MidiDevice.Info> filteredInfos = new ArrayList<MidiDevice.Info>();
            for (int i = 0; i < aInfos.length; i++) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(aInfos[i]);
                    if ((device.getMaxReceivers() != 0) && !(device instanceof Sequencer)) {
                        filteredInfos.add(aInfos[i]);
                    }
                } catch (Exception e) {
                }
            }
            sm_aFilteredInfos = filteredInfos.toArray(new MidiDevice.Info[filteredInfos.size()]);
        }
        return sm_aFilteredInfos;
    }
}
