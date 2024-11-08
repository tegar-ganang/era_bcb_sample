package com.petersalomonsen.pjsynth;

import com.petersalomonsen.pjsynth.control.ChannelControlMaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
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
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 *
 * @author Peter Johan Salomonsen
 */
public class PJSynth implements Synthesizer, Mixer {

    static final long serialVersionUID = 1L;

    private transient PJSynthMidiChannel[] midiChannels = new PJSynthMidiChannel[16];

    private transient boolean isOpen = false;

    transient Receiver receiver;

    transient AudioFormat format;

    private transient HashMap<PJSynthPatch, Instrument> patchInstrumentMap = new HashMap<PJSynthPatch, Instrument>();

    private transient HashMap<PJSynthPatch, ChannelControlMaster> channelControlMasterMap = new HashMap<PJSynthPatch, ChannelControlMaster>();

    transient TargetDataLine targetDataLine;

    public int getMaxPolyphony() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getLatency() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public MidiChannel[] getChannels() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public VoiceStatus[] getVoiceStatus() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isSoundbankSupported(Soundbank soundbank) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean loadInstrument(Instrument instrument) {
        patchInstrumentMap.put(new PJSynthPatch(instrument.getPatch()), instrument);
        for (SoundbankResource sbr : instrument.getSoundbank().getResources()) {
            if (ChannelControlMaster.class.isAssignableFrom(sbr.getDataClass())) try {
                channelControlMasterMap.put(new PJSynthPatch(instrument.getPatch()), (ChannelControlMaster) sbr.getDataClass().newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(PJSynth.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(PJSynth.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return true;
    }

    public void unloadInstrument(Instrument instrument) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean remapInstrument(Instrument from, Instrument to) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Soundbank getDefaultSoundbank() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Instrument[] getAvailableInstruments() {
        return new Instrument[] {};
    }

    public Instrument[] getLoadedInstruments() {
        Instrument[] instrArr = new Instrument[patchInstrumentMap.size()];
        return patchInstrumentMap.values().toArray(instrArr);
    }

    public boolean loadAllInstruments(Soundbank soundbank) {
        for (Instrument instrument : soundbank.getInstruments()) loadInstrument(instrument);
        return true;
    }

    public void unloadAllInstruments(Soundbank soundbank) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public MidiDevice.Info getDeviceInfo() {
        return new PJSynthProvider.PJSynthProviderInfo();
    }

    public void open() {
        receiver = new Receiver() {

            public void send(MidiMessage message, long timeStamp) {
                if (targetDataLine.isOpen() && ShortMessage.class.isInstance(message)) {
                    ShortMessage shm = (ShortMessage) message;
                    int channelNo = shm.getChannel();
                    MidiChannel channel = channelNo >= 0 && channelNo <= 15 ? midiChannels[channelNo] : null;
                    switch(shm.getCommand()) {
                        case ShortMessage.PROGRAM_CHANGE:
                            channel.programChange(shm.getData1());
                            break;
                        case ShortMessage.NOTE_ON:
                            channel.noteOn(shm.getData1(), shm.getData2());
                            break;
                        case ShortMessage.NOTE_OFF:
                            channel.noteOff(shm.getData1());
                            break;
                        case ShortMessage.PITCH_BEND:
                            channel.setPitchBend((0xff & shm.getData1()) + ((0xff & shm.getData2()) << 7));
                            break;
                    }
                }
            }

            public void close() {
            }
        };
        targetDataLine = new TargetDataLine() {

            ByteBuffer buf = null;

            float[] floatBuffer = null;

            FloatBuffer flView = null;

            boolean isOpen = false;

            public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
                PJSynth.this.format = format;
                buf = ByteBuffer.allocate(bufferSize).order(format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
                flView = buf.asFloatBuffer();
                floatBuffer = new float[flView.capacity()];
                for (int n = 0; n < midiChannels.length; n++) midiChannels[n] = new PJSynthMidiChannel(PJSynth.this, flView.capacity());
                isOpen = true;
            }

            public void open(AudioFormat format) throws LineUnavailableException {
                open(format, 16384);
            }

            public int read(byte[] b, int off, int len) {
                if (!isOpen) return len;
                int bytesLeft = len;
                while (bytesLeft > 0) {
                    int templen = bytesLeft > buf.capacity() ? buf.capacity() : bytesLeft;
                    for (int n = 0; n < floatBuffer.length; n++) floatBuffer[n] = 0;
                    fillBuffer(floatBuffer, templen / format.getFrameSize(), format.getChannels());
                    flView.position(0);
                    flView.put(floatBuffer);
                    buf.position(0);
                    buf.get(b, off, templen);
                    off += buf.capacity();
                    bytesLeft -= templen;
                }
                return len;
            }

            public void drain() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void flush() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void start() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void stop() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean isRunning() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean isActive() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public AudioFormat getFormat() {
                return format;
            }

            public int getBufferSize() {
                return buf.limit();
            }

            public int available() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public int getFramePosition() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public long getLongFramePosition() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public long getMicrosecondPosition() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public float getLevel() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public Info getLineInfo() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void open() throws LineUnavailableException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void close() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean isOpen() {
                return isOpen;
            }

            public Control[] getControls() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public boolean isControlSupported(Type control) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public Control getControl(Type control) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void addLineListener(LineListener listener) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void removeLineListener(LineListener listener) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
        isOpen = true;
    }

    public void close() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isOpen() {
        return isOpen;
    }

    public long getMicrosecondPosition() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getMaxReceivers() {
        return 1;
    }

    public int getMaxTransmitters() {
        return 0;
    }

    public Receiver getReceiver() throws MidiUnavailableException {
        return receiver;
    }

    public List<Receiver> getReceivers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Transmitter getTransmitter() throws MidiUnavailableException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<Transmitter> getTransmitters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Mixer.Info getMixerInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Line.Info[] getSourceLineInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Line.Info[] getTargetLineInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Line.Info[] getSourceLineInfo(Line.Info info) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Line.Info[] getTargetLineInfo(Line.Info info) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isLineSupported(Line.Info info) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Line getLine(Line.Info info) throws LineUnavailableException {
        return targetDataLine;
    }

    public int getMaxLines(Line.Info info) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Line[] getSourceLines() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Line[] getTargetLines() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void synchronize(Line[] lines, boolean maintainSync) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void unsynchronize(Line[] lines) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isSynchronizationSupported(Line[] lines, boolean maintainSync) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Line.Info getLineInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Control[] getControls() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isControlSupported(Type control) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Control getControl(Type control) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addLineListener(LineListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void removeLineListener(LineListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void fillBuffer(float[] floatBuffer, int numberOfFrames, int channels) {
        for (PJSynthMidiChannel mc : midiChannels) mc.fillBuffer(floatBuffer, numberOfFrames, channels);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    public AudioFormat getFormat() {
        return format;
    }

    final Instrument getInstrumentByPatch(PJSynthPatch patch) {
        return patchInstrumentMap.get(patch);
    }

    final ChannelControlMaster getChannelControlMasterByPatch(PJSynthPatch patch) {
        return channelControlMasterMap.get(patch);
    }
}
