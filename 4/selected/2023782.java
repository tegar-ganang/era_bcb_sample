package com.frinika.synth;

import com.frinika.voiceserver.VoiceServer;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import com.frinika.audio.*;
import com.frinika.sequencer.ChannelListProvider;
import com.frinika.sequencer.gui.mixer.MidiDeviceIconProvider;
import com.frinika.sequencer.midi.MidiListProvider;
import com.frinika.sequencer.model.ControllerListProvider;
import com.frinika.sequencer.model.FrinikaControllerList;
import com.frinika.synth.settings.SynthSettings;
import com.frinika.synth.settings.synthsettingsversions.SynthSettings20070815;
import com.frinika.synth.soundbank.SynthRackInstrumentIF;
import com.frinika.synth.soundbank.SynthRackSoundbank;
import javax.annotation.Resource;
import javax.sound.midi.*;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.Control.Type;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import rasmus.midi.provider.RasmusSynthesizer;

/**
 * The SynthRack class can hold 16 Frinika soft synths - one for each MidiChannel. Also making it
 * ideal for connecting to your MidiKeyboard for live playing. You can then setup 16 synths
 * and shift between them using the channel switch on your master keyboard.
 * 
 * @author Peter Salomonsen
 *
 */
public class SynthRack implements Synthesizer, InstrumentNameListener, MidiListProvider, ChannelListProvider, MidiDeviceIconProvider, Mixer {

    public static final double GAIN = 0.5;

    VoiceServer voiceServer;

    @Resource(name = "Samplerate")
    int samplerate;

    private static Icon icon = new javax.swing.ImageIcon(RasmusSynthesizer.class.getResource("/icons/frinika.png"));

    public Icon getIcon() {
        if (icon.getIconHeight() > 16 || icon.getIconWidth() > 16) {
            BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = img.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            Image im = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            icon = new ImageIcon(im);
        }
        return icon;
    }

    private static FrinikaControllerList controllerList = new FrinikaControllerList();

    boolean saveReferencedData;

    public static class SynthRackInfo extends MidiDevice.Info {

        SynthRackInfo() {
            super("Frinika SynthRack", "petersalomonsen.com", "A MIDI container for Frinika soft synths", "0.4.0");
        }
    }

    MidiDevice.Info deviceInfo = new SynthRackInfo();

    Receiver receiver = new Receiver() {

        public void send(MidiMessage message, long timeStamp) {
            try {
                if (ShortMessage.class.isInstance(message)) {
                    ShortMessage shm = (ShortMessage) message;
                    int synthIndex = shm.getChannel();
                    MidiChannel channel = midiChannels[shm.getChannel()];
                    if (channel != null) {
                        if (shm.getCommand() == ShortMessage.NOTE_ON) {
                            if (shm.getData2() == 0) channel.noteOff(shm.getData1()); else if (!channel.getMute()) channel.noteOn(shm.getData1(), shm.getData2());
                        } else if (shm.getCommand() == ShortMessage.NOTE_OFF) channel.noteOff(shm.getData1()); else if (shm.getCommand() == ShortMessage.CONTROL_CHANGE) {
                            channel.controlChange(shm.getData1(), shm.getData2());
                            if (gui != null && shm.getData1() == 7) gui.synthStrips[synthIndex].setVolume(shm.getData2()); else if (gui != null && shm.getData1() == 10) gui.synthStrips[synthIndex].setPan(shm.getData2());
                        } else if (shm.getCommand() == ShortMessage.PITCH_BEND) channel.setPitchBend((0xff & shm.getData1()) + ((0xff & shm.getData2()) << 7));
                    }
                    if (shm.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                        midiChannels[shm.getChannel()] = synths[shm.getData1()];
                    }
                } else if (MetaMessage.class.isInstance(message)) {
                    byte[] msgBytes = message.getMessage();
                    if (msgBytes[0] == -1 && msgBytes[1] == 0x51 && msgBytes[2] == 3) {
                        int mpq = ((msgBytes[3] & 0xff) << 16) | ((msgBytes[4] & 0xff) << 8) | (msgBytes[5] & 0xff);
                        tempoBPM = ((60000000f / mpq));
                        java.util.logging.Logger.getLogger(this.getClass().getName()).fine("Tempo set to " + tempoBPM + " bpm");
                    }
                }
            } catch (Exception e) {
            }
        }

        public void close() {
        }
    };

    List<Receiver> receivers = new ArrayList<Receiver>();

    {
        receivers.add(receiver);
    }

    List<Transmitter> transmitters = new ArrayList<Transmitter>();

    SynthRackGUI gui;

    Synth[] synths = new Synth[256];

    MidiChannel[] midiChannels = new MidiChannel[16];

    private Vector<GlobalInstrumentNameListener> globalInstrumentNameListeners = new Vector<GlobalInstrumentNameListener>();

    private SynthRackSoundbank soundbank = new SynthRackSoundbank();

    private float tempoBPM = 100f;

    /**
     * Construct a Frinika Synth 
     * @param voiceServer
     * @throws Exception
     */
    public SynthRack(VoiceServer voiceServer) {
        this.voiceServer = voiceServer;
        if (voiceServer != null) MasterVoice.getDefaultInstance().initialize(voiceServer);
    }

    public void setSynth(int index, Synth synth) {
        if (synths[index] != null) synths[index].close();
        if (synth != null) {
            synth.addInstrumentNameListener(this);
            Patch patch = new Patch(0, index);
            soundbank.createAndRegisterInstrument(patch, synth);
        }
        synths[index] = synth;
        if (gui != null) gui.synthStrips[index].setSynth(synth);
    }

    public Synth getSynth(int index) {
        return (synths[index]);
    }

    public SynthSettings getSynthSetup() {
        SynthSettings setup = new SynthSettings20070815();
        String[] synthClassNames = setup.getSynthClassNames();
        Serializable[] synthSettings = setup.getSynthSettings();
        for (int n = 0; n < synths.length; n++) {
            try {
                synthClassNames[n] = synths[n].getClass().getName();
                synthSettings[n] = synths[n].getSettings();
            } catch (NullPointerException nex) {
            }
        }
        return (setup);
    }

    public void clearSynths() {
        for (int n = 0; n < synths.length; n++) {
            try {
                setSynth(n, null);
            } catch (Exception e) {
            }
        }
    }

    public void loadSynthSetup(final SynthSettings setup) {
        if (gui != null) {
            gui.initLoadSynthSetupProgress(new Thread() {

                public void run() {
                    loadSynthSetupThread(setup);
                }
            });
        } else loadSynthSetupThread(setup);
    }

    public void loadSynthSetupThread(SynthSettings setup) {
        if (voiceServer == null) setVoiceServer(new VoiceServer() {

            @Override
            public void configureAudioOutput(JFrame frame) {
            }
        });
        clearSynths();
        for (int n = 0; n < setup.getSynthClassNames().length; n++) {
            try {
                String synthName = setup.getSynthClassNames()[n];
                if (synthName == null) break;
                if (synthName.equals("com.petersalomonsen.mystudio.mysynth.synths.SoundFont") || synthName.equals("com.petersalomonsen.mystudio.mysynth.synths.MySampler")) synthName = com.frinika.synth.synths.MySampler.class.getName();
                setSynth(n, (Synth) Class.forName(synthName).getConstructors()[0].newInstance(new Object[] { this }));
                synths[n].loadSettings(setup.getSynthSettings()[n]);
                if (gui != null) gui.notifyLoadSynthSetupProgress((n * 100) / setup.getSynthClassNames().length, synths[n].getInstrumentName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (gui != null) gui.notifyLoadSynthSetupProgress(100, "Completed");
    }

    public void save(File file) {
        try {
            SynthSettings setup = getSynthSetup();
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(setup);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load(File file) {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            SynthSettings setup = (SynthSettings) in.readObject();
            loadSynthSetup(setup);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getMaxPolyphony() {
        return 0;
    }

    public long getLatency() {
        return voiceServer.getLatency();
    }

    public MidiChannel[] getChannels() {
        return null;
    }

    public VoiceStatus[] getVoiceStatus() {
        return null;
    }

    public boolean isSoundbankSupported(Soundbank soundbank) {
        return false;
    }

    public boolean loadInstrument(Instrument instrument) {
        return false;
    }

    public void unloadInstrument(Instrument instrument) {
    }

    public boolean remapInstrument(Instrument from, Instrument to) {
        return false;
    }

    public Soundbank getDefaultSoundbank() {
        return null;
    }

    public Instrument[] getAvailableInstruments() {
        return new Instrument[0];
    }

    public Instrument[] getLoadedInstruments() {
        return soundbank.getInstruments();
    }

    public boolean loadAllInstruments(Soundbank soundbank) {
        return false;
    }

    public void unloadAllInstruments(Soundbank soundbank) {
    }

    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        return false;
    }

    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
    }

    public MidiDevice.Info getDeviceInfo() {
        return deviceInfo;
    }

    public void open() {
    }

    public void close() {
    }

    public boolean isOpen() {
        return false;
    }

    public long getMicrosecondPosition() {
        return 0;
    }

    public int getMaxReceivers() {
        return -1;
    }

    public int getMaxTransmitters() {
        return 0;
    }

    public Receiver getReceiver() throws MidiUnavailableException {
        return receiver;
    }

    @SuppressWarnings("unchecked")
    public List getReceivers() {
        return receivers;
    }

    public Transmitter getTransmitter() throws MidiUnavailableException {
        return null;
    }

    @SuppressWarnings("unchecked")
    public List getTransmitters() {
        return transmitters;
    }

    /**
     * @param box
     */
    public void addGlobalInstrumentNameListener(GlobalInstrumentNameListener globalInstrumentNameListener) {
        globalInstrumentNameListeners.add(globalInstrumentNameListener);
    }

    public void instrumentNameChange(Synth synth, String instrumentName) {
        for (int n = 0; n < synths.length; n++) if (synth.equals(synths[n])) for (GlobalInstrumentNameListener globalInstrumentNameListener : globalInstrumentNameListeners) globalInstrumentNameListener.instrumentNameChange(n, instrumentName);
    }

    public int getNumberOfSynths() {
        return synths.length;
    }

    /**
     * @return Returns the voiceServer.
     */
    public VoiceServer getVoiceServer() {
        return voiceServer;
    }

    /**
     * over to provide easier GUI manufactoring
     */
    public String toString() {
        return getDeviceInfo().toString();
    }

    public Object[] getList() {
        return synths;
    }

    /**
     * @return Returns the saveReferencedData.
     */
    public boolean isSaveReferencedData() {
        return saveReferencedData;
    }

    /**
     * @param saveReferencedData The saveReferencedData to set.
     */
    public void setSaveReferencedData(boolean saveReferencedData) {
        this.saveReferencedData = saveReferencedData;
    }

    public ControllerListProvider getControllerList() {
        return controllerList;
    }

    public void setVoiceServer(VoiceServer voiceServer) {
        this.voiceServer = voiceServer;
        if (voiceServer != null) MasterVoice.getDefaultInstance().initialize(voiceServer);
    }

    public Line getLine(javax.sound.sampled.Line.Info info) throws LineUnavailableException {
        return new TargetDataLine() {

            ByteBuffer buf = null;

            float[] floatBuffer = null;

            FloatBuffer flView = null;

            public void open(AudioFormat format) throws LineUnavailableException {
            }

            public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
            }

            public int read(byte[] b, int off, int len) {
                if (buf == null || buf.capacity() != len) {
                    buf = ByteBuffer.allocate(b.length).order(ByteOrder.nativeOrder());
                    flView = buf.asFloatBuffer();
                    floatBuffer = new float[flView.capacity()];
                }
                for (int n = 0; n < floatBuffer.length; n++) floatBuffer[n] = 0;
                voiceServer.read(floatBuffer);
                flView.position(0);
                flView.put(floatBuffer);
                buf.position(0);
                buf.get(b);
                return len;
            }

            public int available() {
                return 0;
            }

            public void drain() {
            }

            public void flush() {
            }

            public int getBufferSize() {
                return 0;
            }

            public AudioFormat getFormat() {
                return null;
            }

            public int getFramePosition() {
                return 0;
            }

            public float getLevel() {
                return 0;
            }

            public long getLongFramePosition() {
                return 0;
            }

            public long getMicrosecondPosition() {
                return 0;
            }

            public boolean isActive() {
                return false;
            }

            public boolean isRunning() {
                return false;
            }

            public void start() {
            }

            public void stop() {
            }

            public void addLineListener(LineListener listener) {
            }

            public void close() {
            }

            public Control getControl(Type control) {
                return null;
            }

            public Control[] getControls() {
                return null;
            }

            public javax.sound.sampled.Line.Info getLineInfo() {
                return null;
            }

            public boolean isControlSupported(Type control) {
                return false;
            }

            public boolean isOpen() {
                return false;
            }

            public void open() throws LineUnavailableException {
            }

            public void removeLineListener(LineListener listener) {
            }
        };
    }

    public int getMaxLines(javax.sound.sampled.Line.Info info) {
        return 0;
    }

    public javax.sound.sampled.Mixer.Info getMixerInfo() {
        return null;
    }

    public javax.sound.sampled.Line.Info[] getSourceLineInfo() {
        return null;
    }

    public javax.sound.sampled.Line.Info[] getSourceLineInfo(javax.sound.sampled.Line.Info info) {
        return null;
    }

    public Line[] getSourceLines() {
        return null;
    }

    public javax.sound.sampled.Line.Info[] getTargetLineInfo() {
        return null;
    }

    public javax.sound.sampled.Line.Info[] getTargetLineInfo(javax.sound.sampled.Line.Info info) {
        return null;
    }

    public Line[] getTargetLines() {
        return null;
    }

    public boolean isLineSupported(javax.sound.sampled.Line.Info info) {
        return false;
    }

    public boolean isSynchronizationSupported(Line[] lines, boolean maintainSync) {
        return false;
    }

    public void synchronize(Line[] lines, boolean maintainSync) {
    }

    public void unsynchronize(Line[] lines) {
    }

    public void addLineListener(LineListener listener) {
    }

    public Control getControl(Type control) {
        return null;
    }

    public Control[] getControls() {
        return null;
    }

    public javax.sound.sampled.Line.Info getLineInfo() {
        return null;
    }

    public boolean isControlSupported(Type control) {
        return false;
    }

    public void removeLineListener(LineListener listener) {
    }

    public float getTempoBPM() {
        return tempoBPM;
    }

    /**
	 * Frinika specific method to show gui of this synth
	 *
	 */
    public void show() {
        JFrame f = new JFrame();
        JScrollPane s = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        if (gui == null) gui = new SynthRackGUI(f, this);
        f.add(s);
        f.setVisible(true);
        f.setSize(600, 400);
        s.getViewport().add(gui);
    }

    /**
     * Local test program for SynthRack with GUI - opens a SynthRack midi device and creates the GUI dialog
     * @param args
     * @throws java.lang.Exception
     */
    public static void main(String args[]) throws Exception {
        MidiDevice.Info info = null;
        for (MidiDevice.Info inf : MidiSystem.getMidiDeviceInfo()) {
            if (inf.getClass().equals(SynthRack.SynthRackInfo.class)) info = inf;
        }
        SynthRack s = ((SynthRack) MidiSystem.getMidiDevice(info));
        s.show();
        s.gui.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
