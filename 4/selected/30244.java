package com.frinika.synth;

import com.frinika.voiceserver.VoiceServer;
import com.frinika.voiceserver.VoiceInterrupt;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;
import javax.sound.midi.*;
import com.frinika.audio.*;
import com.frinika.synth.envelope.MidiVolume;

/**
 * @author Peter Johan Salomonsen
 *
 */
public abstract class Synth implements MidiChannel {

    protected boolean sustain = false;

    protected HashMap<Integer, Oscillator> keys = new HashMap<Integer, Oscillator>();

    protected HashMap<Integer, Oscillator> sustainedKeys = new HashMap<Integer, Oscillator>();

    protected LinkedList<Oscillator> oscillators = new LinkedList<Oscillator>();

    protected PreOscillator preOscillator;

    protected PostOscillator postOscillator;

    private String instrumentName = "New Synth";

    private Vector<InstrumentNameListener> instrumentNameListeners = new Vector<InstrumentNameListener>();

    private boolean mute;

    SynthRack frinikaSynth;

    public Synth(SynthRack synth) {
        this.frinikaSynth = synth;
        preOscillator = new PreOscillator(this);
        postOscillator = new PostOscillator(this);
        preOscillator.nextVoice = postOscillator;
        postOscillator.nextVoice = MasterVoice.getDefaultInstance();
        synth.getVoiceServer().addTransmitter(postOscillator);
        synth.getVoiceServer().addTransmitter(preOscillator);
    }

    protected synchronized void addOscillator(int noteNumber, Oscillator osc) {
        try {
            if (sustain) {
                sustainedKeys.get(new Integer(noteNumber)).release();
                sustainedKeys.remove(new Integer(noteNumber));
            } else {
                keys.get(new Integer(noteNumber)).release();
                keys.remove(new Integer(noteNumber));
            }
        } catch (NullPointerException e) {
        }
        osc.nextVoice = postOscillator;
        frinikaSynth.getVoiceServer().addTransmitter(osc);
        keys.put(new Integer(noteNumber), osc);
        oscillators.add(osc);
    }

    public void noteOff(int noteNumber, int velocity) {
        noteOff(noteNumber);
    }

    public synchronized void noteOff(int noteNumber) {
        if (sustain) sustainedKeys.put(new Integer(noteNumber), keys.get(new Integer(noteNumber))); else {
            Oscillator voice = keys.get(new Integer(noteNumber));
            if (voice != null) voice.release();
        }
        keys.remove(new Integer(noteNumber));
    }

    public abstract void loadSettings(Serializable settings);

    public abstract Serializable getSettings();

    public void setPolyPressure(int noteNumber, int pressure) {
    }

    public int getPolyPressure(int noteNumber) {
        return 0;
    }

    public void setChannelPressure(int pressure) {
    }

    public int getChannelPressure() {
        return 0;
    }

    public void controlChange(int controller, final int value) {
        switch(controller) {
            case 1:
                preOscillator.setVibratoAmount(value);
                break;
            case 2:
                preOscillator.setVibratoFrequency((float) value);
                break;
            case 10:
                getAudioOutput().interruptTransmitter(postOscillator, new VoiceInterrupt() {

                    public void doInterrupt() {
                        postOscillator.setPan(value);
                    }
                });
                break;
            case 7:
                getAudioOutput().interruptTransmitter(postOscillator, new VoiceInterrupt() {

                    public void doInterrupt() {
                        postOscillator.setVolume(MidiVolume.midiVolumeToAmplitudeRatio(value));
                    }
                });
                break;
            case 20:
                getAudioOutput().interruptTransmitter(postOscillator, new VoiceInterrupt() {

                    public void doInterrupt() {
                        postOscillator.setOverDriveAmount(value);
                    }
                });
                break;
            case 22:
                getAudioOutput().interruptTransmitter(postOscillator, new VoiceInterrupt() {

                    public void doInterrupt() {
                        postOscillator.setEchoAmount(value);
                    }
                });
                break;
            case 23:
                getAudioOutput().interruptTransmitter(postOscillator, new VoiceInterrupt() {

                    public void doInterrupt() {
                        postOscillator.setEchoLength(value);
                    }
                });
                break;
            case 64:
                if (value > 63) enableSustain(); else disableSustain();
                break;
            case 91:
                getAudioOutput().interruptTransmitter(postOscillator, new VoiceInterrupt() {

                    public void doInterrupt() {
                        postOscillator.setReverb(MidiVolume.midiVolumeToAmplitudeRatio(value));
                    }
                });
                break;
        }
    }

    void enableSustain() {
        getAudioOutput().interruptTransmitter(preOscillator, new VoiceInterrupt() {

            public void doInterrupt() {
                sustain = true;
            }
        });
    }

    synchronized void disableSustain() {
        sustain = false;
        for (Oscillator osc : oscillators) if (!keys.containsValue(osc)) osc.release();
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

    public void setPitchBend(final int bend) {
        getAudioOutput().interruptTransmitter(preOscillator, new VoiceInterrupt() {

            public void doInterrupt() {
                preOscillator.pitchBend = bend;
                preOscillator.pitchBendFactor = (float) Math.pow(2.0, (((double) (bend - 0x2000) / (double) 0x1000) / 12.0));
            }
        });
    }

    public int getPitchBend() {
        return preOscillator.pitchBend;
    }

    public void resetAllControllers() {
    }

    public void allNotesOff() {
        for (Oscillator osc : oscillators) osc.release();
    }

    public void allSoundOff() {
        for (Oscillator osc : oscillators) frinikaSynth.getVoiceServer().removeTransmitter(osc);
        frinikaSynth.getVoiceServer().removeTransmitter(postOscillator);
        frinikaSynth.getVoiceServer().removeTransmitter(preOscillator);
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
        this.mute = mute;
    }

    public boolean getMute() {
        return mute;
    }

    public void setSolo(boolean soloState) {
    }

    public boolean getSolo() {
        return false;
    }

    public VoiceServer getAudioOutput() {
        return frinikaSynth.getVoiceServer();
    }

    public void close() {
        allSoundOff();
    }

    /**
	 * 
	 */
    public void showGUI() {
        System.out.println("Sorry, no GUI...");
    }

    /**
     * @return
     */
    public String getInstrumentName() {
        return instrumentName;
    }

    public void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
        for (InstrumentNameListener instrumentNameListener : instrumentNameListeners) instrumentNameListener.instrumentNameChange(this, instrumentName);
    }

    /**
     * @param strip
     */
    public void addInstrumentNameListener(InstrumentNameListener instrumentNameListener) {
        instrumentNameListeners.add(instrumentNameListener);
    }

    /**
     * @param adapter
     */
    public void removeInstrumentNameListener(InstrumentNameListener instrumentNameListener) {
        instrumentNameListeners.remove(instrumentNameListener);
    }

    /**
     * @return Returns the postOscillator.
     */
    public final PostOscillator getPostOscillator() {
        return postOscillator;
    }

    /**
     * @return Returns the preOscillator.
     */
    public final PreOscillator getPreOscillator() {
        return preOscillator;
    }

    /**
     * @return Returns the frinikaSynth.
     */
    public SynthRack getFrinikaSynth() {
        return frinikaSynth;
    }

    @Override
    public String toString() {
        return getInstrumentName();
    }
}
