package com.frinika.sequencer.model;

import com.frinika.global.FrinikaConfig;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.swing.Icon;
import rasmus.midi.provider.RasmusSynthesizer;
import rasmus.midi.provider.RasmusSynthesizer.LoadedInstrument;
import com.frinika.tootX.MidiPeakMonitor;
import com.frinika.tootX.midi.MidiInDeviceManager;
import com.frinika.project.ProjectContainer;
import com.frinika.sequencer.FrinikaTrackWrapper;
import com.frinika.sequencer.MidiResource;
import com.frinika.sequencer.gui.mixer.SynthWrapper;
import com.frinika.sequencer.midi.MidiListProvider;
import com.frinika.sequencer.midi.MidiMessageListener;
import com.frinika.sequencer.midi.MonitorReceiver;
import com.frinika.sequencer.patchname.MyPatch;
import com.frinika.synth.Synth;
import com.frinika.synth.SynthRack;
import com.frinika.synth.synths.MySampler;
import com.frinika.synth.synths.sampler.settings.SampledSoundSettings;
import java.util.Collection;
import java.util.HashSet;

public class MidiLane extends Lane implements RecordableLane {

    /**
     *
     */
    private static final long serialVersionUID = 7079152267067539976L;

    static Icon iconNoteLane = new javax.swing.ImageIcon(RasmusSynthesizer.class.getResource("/icons/midilane.png"));

    static Icon iconDrumLane = new javax.swing.ImageIcon(RasmusSynthesizer.class.getResource("/icons/drumlane.png"));

    transient FrinikaTrackWrapper ftw;

    MidiPart trackHeaderPart = null;

    ProgramChangeEvent programEvent;

    String voiceName;

    /**
     * Used to store the midi device/channel used when saving
     */
    Integer midiDeviceIndex;

    int midiChannel;

    /**
     * Stuff to let the GUI know what sort of lane it is.
     *
     */
    public static final int UNKNOWN_TYPE = 0;

    public static final int MELODIC = 1;

    public static final int DRUM = 2;

    public static final int SCORE = 4;

    public static final int META = 8;

    static int nameCount = 0;

    int laneType;

    MidiPlayOptions playOptions = new MidiPlayOptions();

    String patchMapName;

    public String getPatchMapName() {
        return patchMapName;
    }

    public void setPatchMapName(String patchMapName) {
        this.patchMapName = patchMapName;
    }

    transient Collection<MidiMessageListener> midiMessageListeners = new HashSet<MidiMessageListener>();

    transient MidiPeakMonitor peakMonitor = null;

    transient String keyNames[] = null;

    transient boolean isSolo;

    protected MidiLane() {
    }

    /**
     * Constructor for deepClone
     *
     * @param cloneMe
     */
    private MidiLane(MidiLane cloneMe) {
        super("Copy of " + cloneMe.getName(), cloneMe.project);
        trackHeaderPart = (MidiPart) (cloneMe.trackHeaderPart.deepCopy(null));
        trackHeaderPart.lane = this;
        midiDeviceIndex = cloneMe.midiDeviceIndex;
        midiChannel = cloneMe.midiChannel;
        keyNames = cloneMe.keyNames;
        for (Part part : cloneMe.getParts()) {
            part.deepCopy(this);
        }
        ftw = cloneMe.ftw.getSequence().createFrinikaTrack();
        ftw.setMidiChannel(midiChannel);
        setUpKeys();
    }

    /**
     * Construct an empty lane
     *
     * @param ftw
     * @param project
     */
    public MidiLane(FrinikaTrackWrapper ftw, ProjectContainer project) {
        super("Midi " + nameCount++, project);
        this.ftw = ftw;
        trackHeaderPart = new MidiPart();
        trackHeaderPart.lane = this;
        programEvent = new ProgramChangeEvent(trackHeaderPart, 0, 0, 0, 0);
        programEvent.commitAdd();
        midiChannel = ftw.getMidiChannel();
    }

    public MidiPart getHeadPart() {
        return trackHeaderPart;
    }

    public FrinikaTrackWrapper getTrack() {
        return ftw;
    }

    public MidiDevice getMidiDevice() {
        return ftw.getMidiDevice();
    }

    /**
     *
     * @return The reciever associated with this lane
     */
    public Receiver getReceiver() {
        MidiDevice dev = ftw.getMidiDevice();
        if (dev == null) {
            return null;
        }
        try {
            Receiver r = ftw.getMidiDevice().getReceiver();
            if (r != null) {
                return new MonitorReceiver(midiMessageListeners, r);
            } else {
                return null;
            }
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
            return null;
        }
    }

    public int getMidiChannel() {
        return ftw.getMidiChannel();
    }

    /**
     * The midi channel stored in the saved project
     * @return
     */
    public int getStoredMidiChannel() {
        return midiChannel;
    }

    public void restoreFromClone(EditHistoryRecordable object) {
    }

    public ControllerListProvider getControllerList() {
        MidiDevice dev = getMidiDevice();
        if (dev == null) {
            return MidiResource.getDefaultControllerList();
        }
        if (dev instanceof MidiListProvider) {
            return ((MidiListProvider) dev).getControllerList();
        }
        return MidiResource.getDefaultControllerList();
    }

    public void attachFTW() {
        ftw.attachToSequence();
    }

    public void detachFTW() {
        ftw.detachFromSequence();
    }

    /**
     * This is used to find the entry in Voice To Patch map.
     *
     * @return name associated with the midipatch (prog,bank) pair
     */
    public String getVoiceName() {
        return voiceName;
    }

    public void setVoiceName(String name) {
        voiceName = name;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        this.midiChannel = ftw.getMidiChannel();
        if (ftw.getMidiDevice() != null) {
            this.midiDeviceIndex = project.getMidiDeviceIndex(ftw.getMidiDevice());
        }
        out.defaultWriteObject();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ftw.setMidiChannel(midiChannel);
        if (midiDeviceIndex != null) {
            try {
                ftw.setMidiDevice(project.getSequencer().listMidiOutDevices().get(midiDeviceIndex));
            } catch (Exception e) {
                System.out.println("WARNING: Was unable to connect to external midi device");
            }
        }
        setUpKeys();
    }

    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        if (this.getProject().getSequence() == null) {
            this.getProject().createSequence();
        }
        this.ftw = this.getProject().getSequence().createFrinikaTrack();
        in.defaultReadObject();
        if (programEvent == null) {
            trackHeaderPart = new MidiPart();
            trackHeaderPart.lane = this;
            programEvent = new ProgramChangeEvent(trackHeaderPart, 0, 0, 0, 0);
        }
        midiMessageListeners = new HashSet<MidiMessageListener>();
        programEvent.commitAdd();
    }

    public void setProgram(int prog, int msb, int lsb) {
        programEvent.commitRemove();
        programEvent.setProgram(prog, msb, lsb);
        programEvent.commitAdd();
        setUpKeys();
    }

    public void setProgram(MyPatch patch) {
        programEvent.commitRemove();
        programEvent.setProgram(patch.prog, patch.msb, patch.lsb);
        programEvent.commitAdd();
        setUpKeys();
    }

    public MyPatch getProgram() {
        MyPatch patch = programEvent.getPatch();
        if (patch != null) {
            return patch;
        }
        FrinikaTrackWrapper track = ftw;
        int count = track.size();
        for (int i = 0; i < count; i++) {
            MidiEvent event = track.get(i);
            if (event.getTick() != 0) {
                return patch;
            }
            MidiMessage msg = event.getMessage();
            if (msg instanceof ShortMessage) {
                ShortMessage sms = (ShortMessage) msg;
                if (sms.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                    if (patch == null) {
                        patch = new MyPatch(0, 0, 0);
                    }
                    patch.prog = sms.getData1();
                }
                if (sms.getCommand() == ShortMessage.CONTROL_CHANGE) {
                    if (sms.getData1() == 0) {
                        if (patch == null) {
                            patch = new MyPatch(0, 0, 0);
                        }
                        patch.msb = sms.getData2();
                    }
                    if (sms.getData1() == 0x20) {
                        if (patch == null) {
                            patch = new MyPatch(0, 0, 0);
                        }
                        patch.lsb = sms.getData2();
                    }
                }
            }
        }
        return patch;
    }

    public Selectable deepCopy(Selectable parent) {
        MidiLane clone = new MidiLane(this);
        return clone;
    }

    /**
     * Lanes just move down the list. SIlly generic interface.
     */
    public void deepMove(long tick) {
    }

    public void addToModel() {
        super.addToModel();
        onLoad();
    }

    public boolean isRecording() {
        return project.getSequencer().isRecording(this);
    }

    public boolean isMute() {
        return playOptions.muted;
    }

    public boolean isSolo() {
        return isSolo;
    }

    public void setRecording(boolean b) {
        MidiInDeviceManager.open(FrinikaConfig.getMidiInDeviceList());
        if (b) {
            project.getSequencer().recordEnable(this);
        } else {
            project.getSequencer().recordDisable(this);
        }
    }

    public void setMute(boolean b) {
        playOptions.muted = b;
    }

    public void setSolo(boolean b) {
        isSolo = b;
    }

    public double getMonitorValue() {
        if (peakMonitor == null) {
            peakMonitor = new MidiPeakMonitor();
            addMidiMessageListener(peakMonitor);
        }
        return peakMonitor.getPeak();
    }

    public MidiPlayOptions getPlayOptions() {
        if (playOptions == null) {
            playOptions = new MidiPlayOptions();
        }
        return playOptions;
    }

    public void addMidiMessageListener(MidiMessageListener l) {
        midiMessageListeners.add(l);
    }

    public void removeMidiMessageListener(MidiMessageListener l) {
        midiMessageListeners.remove(l);
    }

    public void setMidiChannel(int channel) {
        midiChannel = channel;
        ftw.setMidiChannel(midiChannel);
        setUpKeys();
    }

    public boolean isDrumLane() {
        return (laneType == DRUM) || (laneType == UNKNOWN_TYPE && midiChannel == 9);
    }

    public void setMidiDevice(MidiDevice dev) {
        ftw.setMidiDevice(dev);
        setUpKeys();
    }

    public void setType(int type) {
        laneType = type;
        setUpKeys();
    }

    public int getType() {
        return laneType;
    }

    public void setKeyNames(String[] keyNames) {
        if (keyNames == null) {
            keyNames = null;
        } else {
            if (keyNames.length != 128) {
                String[] nn = new String[128];
                for (int i = 0; i < keyNames.length; i++) {
                    nn[i] = keyNames[i];
                }
                for (int i = keyNames.length; i < 128; i++) {
                    nn[i] = "";
                }
                System.err.println("Strange keyNames.length =" + keyNames.length);
                keyNames = nn;
            }
            this.keyNames = keyNames;
        }
        notifyFocusListeners();
    }

    @Deprecated
    protected void setUpKeys() {
        getPlayOptions();
        if (!isDrumLane()) {
            getPlayOptions().drumMapped = false;
        } else {
            if (playOptions.noteMap != null) {
                playOptions.drumMapped = true;
            }
        }
        notifyFocusListeners();
        if (true) {
            return;
        }
        keyNames = null;
        if (!isDrumLane()) {
            playOptions.drumMapped = false;
        } else {
            if (playOptions.noteMap != null) {
                playOptions.drumMapped = true;
            }
            Instrument inst = null;
            MidiDevice dev = getMidiDevice();
            if (dev instanceof SynthRack) {
                Synth syn = ((SynthRack) dev).getSynth(midiChannel);
                if (syn instanceof MySampler) {
                    keyNames = new String[128];
                    MySampler mys = (MySampler) syn;
                    SampledSoundSettings[][] ssss = mys.sampledSounds;
                    for (int i = 0; i < 128; i++) {
                        if (ssss[i][0] != null) {
                            keyNames[i] = ssss[i][0].toString();
                        }
                    }
                }
            } else if (dev instanceof SynthWrapper) {
                dev = ((SynthWrapper) dev).getRealDevice();
                if (dev instanceof Synthesizer) {
                    Synthesizer synth = (Synthesizer) dev;
                    MyPatch patch = getProgram();
                    Method getChannels = null;
                    Instrument insts[] = synth.getLoadedInstruments();
                    for (Instrument ins : insts) {
                        Instrument li = (Instrument) ins;
                        boolean[] channels = null;
                        try {
                            if (getChannels != null) {
                                if (getChannels.getDeclaringClass() != li.getClass()) {
                                    getChannels = null;
                                }
                            }
                            if (getChannels == null) {
                                getChannels = li.getClass().getMethod("getChannels");
                            }
                            if (getChannels != null) {
                                channels = (boolean[]) getChannels.invoke(li, (Object[]) null);
                            }
                        } catch (Exception e) {
                        }
                        if (channels != null) {
                            if ((ins.getPatch().getProgram() == patch.prog) && channels[midiChannel]) {
                                inst = ins;
                                break;
                            }
                        }
                    }
                    if (inst == null) {
                        insts = synth.getAvailableInstruments();
                        for (Instrument ins : insts) {
                            Instrument li = (Instrument) ins;
                            boolean[] channels = null;
                            try {
                                if (getChannels != null) {
                                    if (getChannels.getDeclaringClass() != li.getClass()) {
                                        getChannels = null;
                                    }
                                }
                                if (getChannels == null) {
                                    getChannels = li.getClass().getMethod("getChannels");
                                }
                                if (getChannels != null) {
                                    channels = (boolean[]) getChannels.invoke(li, (Object[]) null);
                                }
                            } catch (Exception e) {
                            }
                            if (channels != null) {
                                if ((ins.getPatch().getProgram() == patch.prog) && channels[midiChannel]) {
                                    inst = ins;
                                    break;
                                }
                            }
                        }
                    }
                    if (inst != null) {
                        try {
                            Method getKeys = inst.getClass().getMethod("getKeys");
                            if (getKeys != null) {
                                keyNames = (String[]) getKeys.invoke(inst, (Object[]) null);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        notifyFocusListeners();
    }

    private void notifyFocusListeners() {
        Part f = project.getPartSelection().getFocus();
        if (f == null) {
            return;
        }
        if (f.getLane() == this) {
            project.getPartSelection().setDirty();
            project.getPartSelection().notifyListeners();
        }
    }

    public String[] getKeyNames() {
        return keyNames;
    }

    public void setDrumMapping(int k, int index) {
        playOptions.drumMapped = true;
        if (playOptions.noteMap == null) {
            playOptions.noteMap = new int[128];
            for (int i = 0; i < 128; i++) {
                playOptions.noteMap[i] = i;
            }
        }
        playOptions.noteMap[k] = index;
        System.out.println(k + "--->" + index);
        notifyFocusListeners();
    }

    public int mapNote(int num) {
        if (playOptions.drumMapped) {
            return playOptions.noteMap[num];
        } else {
            return num;
        }
    }

    @Override
    public Part createPart() {
        return new MidiPart(this);
    }

    @Override
    public Icon getIcon() {
        if (isDrumLane()) {
            return iconDrumLane;
        } else {
            return iconNoteLane;
        }
    }

    /**
     * The index of the midiDevice according to the stored order in the saved project
     * @return
     */
    public Integer getMidiDeviceIndex() {
        if (midiDeviceIndex == null) {
            this.midiChannel = ftw.getMidiChannel();
            if (ftw.getMidiDevice() != null) {
                this.midiDeviceIndex = project.getMidiDeviceIndex(ftw.getMidiDevice());
            }
        }
        return midiDeviceIndex;
    }
}
