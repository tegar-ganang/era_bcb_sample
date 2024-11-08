package de.reichhold.jrehearsal;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Vector;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

public class MySequencer extends Observable implements MetaEventListener {

    public static final String TRACK_HAS_ENDED = "TRACK_HAS_ENDED";

    public static class Data implements Comparable<Object>, Serializable {

        private static final long serialVersionUID = 1L;

        String name;

        int channel;

        boolean active;

        int gain;

        public Data(String argName, int id) {
            name = argName;
            channel = id;
            active = true;
            gain = MidiPlayer.GAIN_RANGE;
        }

        public Data(Data argOther) {
            name = argOther.name;
            channel = argOther.channel;
            active = argOther.active;
            gain = argOther.gain;
        }

        public int compareTo(Object o) {
            int result = 0;
            if (o instanceof Data) {
                result = new Integer(channel).compareTo(new Integer(((Data) o).channel));
            }
            return result;
        }
    }

    private Sequencer delegate;

    private Synthesizer synthesizer;

    private double duration;

    private Sequence originalSequence;

    private SequenceHandler.BarTypeData barData;

    private Sequence currentSequence;

    private Sequence countIn;

    private Track countInTrack;

    private float originalSpeed;

    private float currentSpeedFactor;

    private List<Data> data = new ArrayList<Data>();

    private int gain = MidiPlayer.GAIN_RANGE;

    private long startPositionInTicks;

    private SequenceHandler.TempoData tempoData;

    public MySequencer() {
        try {
            delegate = MidiSystem.getSequencer();
            if (delegate instanceof Synthesizer) {
                synthesizer = (Synthesizer) delegate;
            } else {
                synthesizer = MidiSystem.getSynthesizer();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
        delegate.addMetaEventListener(this);
    }

    public synchronized void loadFile(URL midiFile) throws InvalidMidiDataException, MidiUnavailableException {
        stop();
        try {
            originalSequence = MidiSystem.getSequence(midiFile);
            SequenceHandler handler = new SequenceHandler();
            handler.parse(originalSequence);
            currentSequence = handler.copy(originalSequence);
            Map<Integer, SequenceHandler.ChannelMetaData> channels = handler.getChannelData();
            barData = handler.getFrameData();
            countIn = new Sequence(Sequence.PPQ, originalSequence.getResolution());
            tempoData = handler.getTempoData();
            originalSpeed = tempoData.getBPM(0);
            delegate.setTempoInBPM(originalSpeed);
            notifyObservers(originalSpeed);
            data = new Vector<Data>();
            for (SequenceHandler.ChannelMetaData channel : channels.values()) {
                String name = "";
                for (Iterator<Integer> j = channel.programs.iterator(); j.hasNext(); ) {
                    int instrumentNumber = j.next().intValue();
                    if (name.length() > 0) {
                        name += " | ";
                    }
                    if (synthesizer.getDefaultSoundbank() != null) {
                        name += synthesizer.getDefaultSoundbank().getInstrument(new Patch(0, instrumentNumber)).getName();
                    } else {
                        name += MidiEventInterpreter.DEFAULT_INSTRUMENT_NAMES[instrumentNumber];
                    }
                }
                if (name.length() == 0) {
                    name = Messages.getString("MySequencer.Metronome");
                }
                data.add(new Data(name, channel.channel));
            }
            Collections.sort(data);
        } catch (InvalidMidiDataException imde) {
            System.out.println("Unsupported audio file.");
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
            currentSequence = null;
            return;
        }
        if (!delegate.isOpen()) {
            delegate.open();
        }
        delegate.setSequence(currentSequence);
        duration = getDuration();
        return;
    }

    public void meta(MetaMessage message) {
        try {
            new MidiEventInterpreter() {

                protected void endOfTrack() {
                    if (delegate.getSequence() == countIn) {
                        try {
                            if (!delegate.isOpen()) {
                                delegate.open();
                            }
                            countIn.deleteTrack(countInTrack);
                            startAt(startPositionInTicks, false);
                        } catch (InvalidMidiDataException e) {
                            e.printStackTrace();
                        } catch (MidiUnavailableException e) {
                            e.printStackTrace();
                        } catch (MidiParserException e) {
                            e.printStackTrace();
                        }
                    } else {
                        delegate.stop();
                        setChanged();
                        notifyObservers(TRACK_HAS_ENDED);
                    }
                }

                protected void setTempo(int microSecondsPerQuarterNote) {
                    setChanged();
                    delegate.setTempoInMPQ(microSecondsPerQuarterNote);
                    originalSpeed = delegate.getTempoInBPM();
                    float currentSpeed = originalSpeed * currentSpeedFactor;
                    delegate.setTempoInBPM(currentSpeed);
                    notifyObservers(currentSpeed);
                    clearChanged();
                }
            }.processMetaMessage(message);
        } catch (MidiParserException e) {
            e.printStackTrace();
        }
    }

    public void notifySettingsChange() throws MidiParserException, InvalidMidiDataException {
        if (originalSequence != null) {
            if (delegate.isRunning()) {
                delegate.stop();
                startAt(delegate.getTickPosition(), false);
            }
        }
    }

    private void updateSequence() throws MidiParserException, InvalidMidiDataException {
        currentSequence = new SequenceHandler().getModifiedSequence(originalSequence, data, gain);
        delegate.setSequence(currentSequence);
    }

    public List<Data> getTrackDescriptions() {
        return data;
    }

    public List<Data> getClonedTrackDescriptions() {
        List<Data> result = new ArrayList<Data>();
        for (Data datum : data) {
            result.add(new Data(datum));
        }
        return result;
    }

    public void setTrackDescriptions(List<Data> argData) throws MidiParserException, InvalidMidiDataException {
        for (int i = 0; i < data.size(); i++) {
            data.set(i, argData.get(i));
        }
        updateSequence();
    }

    public void startAt(long argStartPositionInTicks, boolean argUseCount) throws InvalidMidiDataException, MidiParserException {
        startPositionInTicks = argStartPositionInTicks;
        updateSequence();
        if (argUseCount) {
            activateCountIn(argStartPositionInTicks);
        }
        delegate.start();
        if (!argUseCount) {
            setTickPosition(argStartPositionInTicks);
        }
    }

    public double getDuration() {
        double result = 0.0;
        if (delegate.getSequence() != null) {
            result = delegate.getSequence().getMicrosecondLength() / 1000000.0;
        }
        return result;
    }

    public double getPositionInSequenceInSeconds() {
        double seconds = 0.0;
        try {
            seconds = delegate.getTickPosition() * duration / currentSequence.getTickLength();
        } catch (IllegalStateException e) {
            System.out.println("TEMP: IllegalStateException " + "on sequencer.getTickPosition(): " + e);
        }
        return seconds;
    }

    public double getMicrosecondPosition() {
        return delegate.getMicrosecondPosition();
    }

    public boolean isRunning() {
        return delegate.isRunning();
    }

    private void activateCountIn(long argTicks) throws InvalidMidiDataException {
        List<MidiEvent> events = barData.getCountInTrack(argTicks);
        countInTrack = countIn.createTrack();
        for (int i = 0; i < events.size(); i++) {
            countInTrack.add((MidiEvent) events.get(i));
        }
        delegate.setSequence(countIn);
        float currentSpeed = tempoData.getBPM(argTicks) * currentSpeedFactor;
        delegate.setTempoInBPM(currentSpeed);
        notifyObservers(currentSpeed);
    }

    public void stop() {
        if (delegate.isRunning()) {
            delegate.stop();
        }
    }

    public long getTickPosition() {
        return delegate.getTickPosition();
    }

    public long getTickLength() {
        return delegate.getTickLength();
    }

    public long getMicrosecondLength() {
        return delegate.getMicrosecondLength();
    }

    public void setTickPosition(long argStartPositionInTicks) {
        delegate.setTickPosition(argStartPositionInTicks);
        originalSpeed = tempoData.getBPM(argStartPositionInTicks);
        float currentSpeed = originalSpeed * currentSpeedFactor;
        delegate.setTempoInBPM(currentSpeed);
        notifyObservers(currentSpeed);
    }

    public int getTempoInBPM() {
        return (int) delegate.getTempoInBPM();
    }

    public void setTempoInBPM(float argBPM) {
        currentSpeedFactor = argBPM / originalSpeed;
        delegate.setTempoInBPM(argBPM);
        setChanged();
        notifyObservers(argBPM);
    }

    public void setSecondPosition(int argSeconds) {
        delegate.setMicrosecondPosition(argSeconds * 1000);
    }

    public SequenceHandler.BarTypeData getBarData() {
        return barData;
    }

    public int getInitialTempoInBPM() {
        return (int) originalSpeed;
    }

    public void setGain(int value) throws MidiParserException, InvalidMidiDataException {
        gain = value;
        notifySettingsChange();
    }

    public SequenceHandler.BarTypeData.PositionInBar getPositionInSequenceAsBars() {
        long ticks = delegate.getTickPosition();
        if (delegate.getSequence() == countIn) {
            ticks += startPositionInTicks;
        }
        return barData.getBarNumber(ticks);
    }
}
