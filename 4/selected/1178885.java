package gnu.javax.sound.midi.dssi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import javax.sound.midi.MidiDevice.Info;

/**
 * DSSI soft-synth support.
 * 
 * All DSSI soft-synths are expected to be installed in /usr/lib/dssi.
 * 
 * @author Anthony Green (green@redhat.com)
 *
 */
public class DSSISynthesizer implements Synthesizer {

    /**
   * The DSSI Instrument class.
   * 
   * @author Anthony Green (green@redhat.com)
   *
   */
    class DSSIInstrument extends Instrument {

        DSSIInstrument(Soundbank soundbank, Patch patch, String name) {
            super(soundbank, patch, name, null);
        }

        public Object getData() {
            return null;
        }
    }

    /**
   * DSSISoundbank holds all instruments.
   * 
   * @author Anthony Green (green@redhat.com)
   *
   */
    class DSSISoundbank implements Soundbank {

        private String name;

        private String description;

        private List instruments = new ArrayList();

        private List resources = new ArrayList();

        private String vendor;

        private String version;

        public DSSISoundbank(String name, String description, String vendor, String version) {
            this.name = name;
            this.description = description;
            this.vendor = vendor;
            this.version = version;
        }

        void add(Instrument instrument) {
            instruments.add(instrument);
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getVendor() {
            return vendor;
        }

        public String getDescription() {
            return description;
        }

        public SoundbankResource[] getResources() {
            return (SoundbankResource[]) resources.toArray(new SoundbankResource[resources.size()]);
        }

        public Instrument[] getInstruments() {
            return (Instrument[]) instruments.toArray(new Instrument[instruments.size()]);
        }

        public Instrument getInstrument(Patch patch) {
            Iterator itr = instruments.iterator();
            while (itr.hasNext()) {
                Instrument i = (Instrument) itr.next();
                if (i.getPatch().equals(patch)) return i;
            }
            return null;
        }
    }

    /**
   * The Receiver class receives all MIDI messages from a connected
   * Transmitter.
   * 
   * @author Anthony Green (green@redhat.com)
   *
   */
    class DSSIReceiver implements Receiver {

        public void send(MidiMessage message, long timeStamp) throws IllegalStateException {
            if (message instanceof ShortMessage) {
                ShortMessage smessage = (ShortMessage) message;
                switch(message.getStatus()) {
                    case ShortMessage.NOTE_ON:
                        int velocity = smessage.getData2();
                        if (velocity > 0) channels[smessage.getChannel()].noteOn(smessage.getData1(), smessage.getData2()); else channels[smessage.getChannel()].noteOff(smessage.getData1());
                        break;
                    case ShortMessage.CONTROL_CHANGE:
                        channels[smessage.getChannel()].controlChange(smessage.getData1(), smessage.getData2());
                        break;
                    default:
                        System.out.println("Unhandled message: " + message.getStatus());
                        break;
                }
            }
        }

        public void close() {
        }
    }

    static native void noteOn_(long handle, int channel, int noteNumber, int velocity);

    static native void noteOff_(long handle, int channel, int noteNumber, int velocity);

    static native void setPolyPressure_(long handle, int channel, int noteNumber, int pressure);

    static native int getPolyPressure_(long handle, int channel, int noteNumber);

    static native void controlChange_(long handle, int channel, int control, int value);

    static native void open_(long handle);

    static native void close_(long handle);

    static native String getProgramName_(long handle, int index);

    static native int getProgramBank_(long handle, int index);

    static native int getProgramProgram_(long handle, int index);

    static native void selectProgram_(long handle, int bank, int program);

    /**
   * @author Anthony Green (green@redhat.com)
   *
   */
    public class DSSIMidiChannel implements MidiChannel {

        int channel = 0;

        /**
     * Default contructor.
     */
        public DSSIMidiChannel(int channel) {
            super();
            this.channel = channel;
        }

        public void noteOn(int noteNumber, int velocity) {
            noteOn_(sohandle, channel, noteNumber, velocity);
        }

        public void noteOff(int noteNumber, int velocity) {
            noteOff_(sohandle, channel, noteNumber, velocity);
        }

        public void noteOff(int noteNumber) {
            noteOff_(sohandle, channel, noteNumber, -1);
        }

        public void setPolyPressure(int noteNumber, int pressure) {
            setPolyPressure_(sohandle, channel, noteNumber, pressure);
        }

        public int getPolyPressure(int noteNumber) {
            return getPolyPressure_(sohandle, channel, noteNumber);
        }

        public void setChannelPressure(int pressure) {
        }

        public int getChannelPressure() {
            return 0;
        }

        public void controlChange(int controller, int value) {
            controlChange_(sohandle, channel, controller, value);
        }

        public int getController(int controller) {
            return 0;
        }

        public void programChange(int program) {
        }

        public void programChange(int bank, int program) {
        }

        public int getProgram() {
            return 0;
        }

        public void setPitchBend(int bend) {
        }

        public int getPitchBend() {
            return 0;
        }

        public void resetAllControllers() {
        }

        public void allNotesOff() {
        }

        public void allSoundOff() {
        }

        public boolean localControl(boolean on) {
            return false;
        }

        public void setMono(boolean on) {
        }

        public boolean getMono() {
            return false;
        }

        public void setOmni(boolean on) {
        }

        public boolean getOmni() {
            return false;
        }

        public void setMute(boolean mute) {
        }

        public boolean getMute() {
            return false;
        }

        public void setSolo(boolean solo) {
        }

        public boolean getSolo() {
            return false;
        }
    }

    long sohandle;

    long handle;

    private Info info;

    MidiChannel channels[] = new MidiChannel[16];

    List soundbanks = new ArrayList();

    DSSISoundbank defaultSoundbank;

    /**
   * Create a DSSI Synthesizer.
   * 
   * @param info the DSSIInfo for this soft-synth
   * @param soname the name of the .so file for this DSSI synth
   * @param index the DSSI index for this soft-synth
   */
    public DSSISynthesizer(Info info, String soname, long index) {
        super();
        this.info = info;
        sohandle = DSSIMidiDeviceProvider.dlopen_(soname);
        handle = DSSIMidiDeviceProvider.getDSSIHandle_(sohandle, index);
        channels[0] = new DSSIMidiChannel(0);
        defaultSoundbank = new DSSISoundbank("name", "description", "vendor", "version");
        soundbanks.add(defaultSoundbank);
        int i = 0;
        String name;
        do {
            name = getProgramName_(sohandle, i);
            if (name != null) {
                defaultSoundbank.add(new DSSIInstrument(defaultSoundbank, new Patch(getProgramBank_(sohandle, i), getProgramProgram_(sohandle, i)), name));
                i++;
            }
        } while (name != null);
    }

    public int getMaxPolyphony() {
        return 0;
    }

    public long getLatency() {
        return 0;
    }

    public MidiChannel[] getChannels() {
        return channels;
    }

    public VoiceStatus[] getVoiceStatus() {
        return null;
    }

    public boolean isSoundbankSupported(Soundbank soundbank) {
        return false;
    }

    public boolean loadInstrument(Instrument instrument) {
        if (instrument.getSoundbank() != defaultSoundbank) throw new IllegalArgumentException("Synthesizer doesn't support this instrument's soundbank");
        Patch patch = instrument.getPatch();
        selectProgram_(sohandle, patch.getBank(), patch.getProgram());
        return true;
    }

    public void unloadInstrument(Instrument instrument) {
    }

    public boolean remapInstrument(Instrument from, Instrument to) {
        return false;
    }

    public Soundbank getDefaultSoundbank() {
        return defaultSoundbank;
    }

    public Instrument[] getAvailableInstruments() {
        List instruments = new ArrayList();
        Iterator itr = soundbanks.iterator();
        while (itr.hasNext()) {
            Soundbank sb = (Soundbank) itr.next();
            Instrument ins[] = sb.getInstruments();
            for (int i = 0; i < ins.length; i++) instruments.add(ins[i]);
        }
        return (Instrument[]) instruments.toArray(new Instrument[instruments.size()]);
    }

    public Instrument[] getLoadedInstruments() {
        return null;
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

    public Info getDeviceInfo() {
        return info;
    }

    public void open() throws MidiUnavailableException {
        open_(sohandle);
    }

    public void close() {
        close_(sohandle);
    }

    public boolean isOpen() {
        return false;
    }

    public long getMicrosecondPosition() {
        return 0;
    }

    public int getMaxReceivers() {
        return 1;
    }

    public int getMaxTransmitters() {
        return 0;
    }

    public Receiver getReceiver() throws MidiUnavailableException {
        return new DSSIReceiver();
    }

    public Transmitter getTransmitter() throws MidiUnavailableException {
        return null;
    }
}
