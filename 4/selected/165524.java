package percussiongenerator.sound;

import com.sun.media.sound.AudioSynthesizer;
import com.sun.media.sound.SF2Soundbank;
import com.sun.media.sound.SF2SoundbankReader;
import percussiongenerator.model.ITrackCollectionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import org.jfonia.model.PerformanceNode;
import org.jfonia.model.ToneSequence;
import percussiongenerator.model.Track;
import percussiongenerator.model.TrackCollection;

/**
 *
 * @author Jannes Plyson
 */
public class MidiPlayerTimer implements IMidiPlayer, ITrackCollectionListener {

    protected Receiver receiver;

    protected boolean playerLoaded;

    protected Synthesizer synth;

    protected TrackCollection trackCollection;

    protected Timer timer, timerViews;

    protected long lastStarted, lastAdded, timeOffset;

    protected long lastStartedMicro, timeOffsetMicro;

    protected TimerTask timerTask;

    protected HashMap<Instrument, Integer> channelMapping;

    protected long interval;

    protected ArrayList<ITimeListener> listeners;

    protected static long microSecondsOffset = 20000;

    protected SF2Soundbank soundbank;

    protected int mappedChannels;

    protected Instrument defaultInstrument;

    protected long soundDelay;

    protected ShortMessage keepAlive;

    public MidiPlayerTimer() {
        playerLoaded = false;
        receiver = null;
        try {
            if (isMac()) synth = MidiSystem.getSynthesizer(); else synth = findAudioSynthesizer();
            if (synth != null) {
                SF2SoundbankReader reader = new SF2SoundbankReader();
                soundbank = (SF2Soundbank) reader.getSoundbank(getClass().getResourceAsStream("/percussiongenerator/soundbank/TimGM6mb.sf2"));
                synth.open();
                if (isMac()) synth.loadAllInstruments(synth.getDefaultSoundbank()); else synth.loadAllInstruments(soundbank);
                receiver = synth.getReceiver();
                playerLoaded = true;
                System.out.println("midi player loaded");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        soundDelay = 0;
        timer = null;
        listeners = new ArrayList<ITimeListener>();
        lastStarted = 0;
        lastAdded = -1;
        timeOffset = 0;
        lastStartedMicro = 0;
        timeOffsetMicro = 0;
        interval = 50;
        channelMapping = new HashMap<Instrument, Integer>();
        keepAlive = new ShortMessage();
        try {
            keepAlive.setMessage(241, 0, 0);
        } catch (Exception exc) {
            System.err.println(exc.getMessage());
        }
        loadDefaultInstrument();
    }

    private boolean isMac() {
        return System.getProperty("os.name").toUpperCase().contains("MAC");
    }

    private void loadDefaultInstrument() {
        Instrument[] instruments = getInstruments();
        int i = 0;
        while (i < instruments.length && !instruments[i].toString().contains("Drumkit:")) {
            i++;
        }
        if (i < instruments.length) {
            defaultInstrument = instruments[i];
        } else {
            defaultInstrument = instruments[0];
        }
    }

    private void resetTimer() {
        if (timer != null) {
            timer.cancel();
        }
        if (receiver != null) {
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {

                public void run() {
                    addTimerEvents();
                }
            }, 0, interval);
        }
    }

    public TrackCollection getTrackCollection() {
        return trackCollection;
    }

    public void setTrackCollection(final TrackCollection trackCollection) {
        if (trackCollection != null) {
            trackCollection.removeListener(this);
            removeTimeListener(trackCollection);
        }
        this.trackCollection = trackCollection;
        addTimeListener(trackCollection);
        trackCollection.addListener(this);
        mapChannels();
        if (timerViews != null) {
            timerViews.cancel();
        }
        timerViews = new Timer();
        timerViews.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                if (timer != null && trackCollection != null) {
                    updateListeners(System.nanoTime() - lastStarted);
                }
            }
        }, 0, 25);
    }

    private void mapChannels() {
        if (synth != null) {
            ArrayList<Track> tracks = trackCollection.getTracks();
            channelMapping = new HashMap<Instrument, Integer>();
            mappedChannels = 0;
            for (int i = 0; i < tracks.size(); i++) {
                Track track = tracks.get(i);
                SimpleInstrument instrument = track.getInstrument();
                Integer channel = channelMapping.get(instrument.instrument);
                if (channel == null) {
                    if (instrument.instrument != null && instrument.instrument.toString().contains("Drumkit")) {
                        channelMapping.put(instrument.instrument, 9);
                        synth.getChannels()[9].programChange(instrument.instrument.getPatch().getBank(), instrument.instrument.getPatch().getProgram());
                    } else if (instrument.instrument == null) {
                        trackCollection.removeListener(this);
                        track.setInstrument(new SimpleInstrument(defaultInstrument, DrumkitHelp.getPitch("High Tom 1")));
                        instrument = track.getInstrument();
                        channelMapping.put(instrument.instrument, 9);
                        synth.getChannels()[9].programChange(instrument.instrument.getPatch().getBank(), instrument.instrument.getPatch().getProgram());
                        trackCollection.addListener(this);
                    } else {
                        int channelNumber = mappedChannels;
                        if (channelNumber == 9) {
                            channelNumber++;
                        }
                        channelMapping.put(instrument.instrument, channelNumber);
                        mappedChannels++;
                        synth.getChannels()[channelNumber].programChange(instrument.instrument.getPatch().getBank(), instrument.instrument.getPatch().getProgram());
                    }
                }
            }
        }
    }

    public boolean isPlayerLoaded() {
        return playerLoaded;
    }

    public static AudioSynthesizer findAudioSynthesizer() throws MidiUnavailableException {
        Synthesizer synth = MidiSystem.getSynthesizer();
        if (synth instanceof AudioSynthesizer) return (AudioSynthesizer) synth;
        Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            MidiDevice dev = MidiSystem.getMidiDevice(infos[i]);
            if (dev instanceof AudioSynthesizer) return (AudioSynthesizer) dev;
        }
        return null;
    }

    public Instrument[] getInstruments() {
        if (synth != null) {
            return synth.getLoadedInstruments();
        } else {
            return new Instrument[0];
        }
    }

    public void play() {
        mapChannels();
        lastStarted = System.nanoTime() - timeOffset;
        lastStartedMicro = synth.getMicrosecondPosition() - timeOffsetMicro;
        resetTimer();
    }

    public void pause() {
        timeOffset = (System.nanoTime() - lastStarted);
        timeOffsetMicro = synth.getMicrosecondPosition() - lastStartedMicro;
        lastAdded = timeOffset;
        if (timer != null) {
            timer.cancel();
        }
        timer = null;
        try {
            receiver.close();
            receiver = synth.getReceiver();
        } catch (Exception exc) {
            System.err.append(exc.getMessage());
        }
    }

    public void stop() {
        lastAdded = -1;
        timeOffset = 0;
        timeOffsetMicro = 0;
        if (timer != null) {
            timer.cancel();
        }
        timer = null;
        updateListeners(0l);
        try {
            receiver.close();
            receiver = synth.getReceiver();
        } catch (Exception exc) {
            System.err.append(exc.getMessage());
        }
    }

    protected void addTimerEvents() {
        try {
            ArrayList<Track> tracks = trackCollection.getTracks();
            long newLastAdded = (System.nanoTime() - lastStarted) + 2000000 * interval;
            for (int i = 0; i < tracks.size(); i++) {
                Track track = tracks.get(i);
                if (!track.isMuted()) {
                    long time = lastAdded % track.getPerformanceLength();
                    long newTime = newLastAdded % track.getPerformanceLength();
                    ToneSequence seq = track.getStaff().getToneSequence();
                    if (seq != null) {
                        int pos = seq.indexOf(seq.getPerformanceNode(time + 1));
                        if (seq.getPerformanceNodeFromIndex(pos).getBegin() <= lastAdded) {
                            pos++;
                        }
                        if (newTime < time) {
                            while (pos < seq.size()) {
                                scheduleNote(seq.getPerformanceNodeFromIndex(pos), seq, track.getInstrument());
                                pos++;
                            }
                            pos = 0;
                        }
                        while (pos < seq.size() && seq.getPerformanceNodeFromIndex(pos).getBegin() < newTime) {
                            scheduleNote(seq.getPerformanceNodeFromIndex(pos), seq, track.getInstrument());
                            pos++;
                        }
                    }
                }
            }
            lastAdded = newLastAdded;
            receiver.send(keepAlive, -1);
        } catch (Exception exc) {
            System.err.println(exc.getMessage());
        }
    }

    protected void scheduleNote(final PerformanceNode node, ToneSequence sequence, SimpleInstrument instrument) {
        try {
            if (node.getPitch() != null && node.getVelocity() > 0) {
                long elapsed = (System.nanoTime() - lastStarted);
                long delay = elapsed % sequence.getPerformanceLength();
                long elapsedLoopsMicro = (elapsed - delay) / 1000;
                long microPosition = microSecondsOffset + lastStartedMicro + elapsedLoopsMicro + node.getBegin() / 1000;
                final ShortMessage messageNoteOn = new ShortMessage();
                final ShortMessage messageNoteOff = new ShortMessage();
                if (instrument.instrument.toString().contains("Drumkit")) {
                    messageNoteOn.setMessage(ShortMessage.NOTE_ON, getChannel(instrument.instrument), instrument.drumkitInstrument, node.getVelocity());
                    messageNoteOff.setMessage(ShortMessage.NOTE_ON, getChannel(instrument.instrument), instrument.drumkitInstrument, 0);
                } else {
                    messageNoteOn.setMessage(ShortMessage.NOTE_ON, getChannel(instrument.instrument), node.getPitch(), node.getVelocity());
                    messageNoteOff.setMessage(ShortMessage.NOTE_OFF, getChannel(instrument.instrument), node.getPitch(), 0);
                }
                if (lastAdded == -1 && node.getBegin() == 0) {
                    receiver.send(messageNoteOn, microPosition);
                    receiver.send(messageNoteOff, microPosition + node.getDifference() / 1000);
                } else if (node.getBegin() < delay) {
                    delay = (node.getBegin() + sequence.getPerformanceLength() - delay) / 1000000;
                    microPosition += sequence.getPerformanceLength() / 1000;
                } else {
                    delay = (node.getBegin() - delay) / 1000000;
                }
                final long microPos = microPosition;
                timer.schedule(new TimerTask() {

                    public void run() {
                        receiver.send(messageNoteOn, microPos);
                        receiver.send(messageNoteOff, microPos + node.getDifference() / 1000);
                    }
                }, delay);
            }
        } catch (Exception exc) {
            System.err.println("error shedule note: " + exc.getMessage());
        }
    }

    private int getChannel(Instrument instrument) {
        int channel = 0;
        try {
            channel = channelMapping.get(instrument);
            return channel;
        } catch (Exception exc) {
            mapChannels();
            return getChannel(instrument);
        }
    }

    public boolean isPlaying() {
        return timer != null;
    }

    public void addTimeListener(ITimeListener listener) {
        listeners.add(listener);
    }

    public void removeTimeListener(ITimeListener listener) {
        listeners.remove(listener);
    }

    public void updateListeners(long time) {
        long offset;
        if (soundDelay != 0) {
            offset = soundDelay;
        } else {
            offset = microSecondsOffset * 1000;
        }
        for (ITimeListener listener : listeners) {
            listener.timeUpdated(time - offset);
        }
    }

    public void trackAdded(Track track) {
        if (timer != null) {
            resetTimer();
        }
        mapChannels();
    }

    public void trackRemoved(Track track) {
        if (timer != null) {
            resetTimer();
        }
        mapChannels();
    }

    public void cleared() {
        if (timer != null) {
            resetTimer();
        }
        mapChannels();
    }

    public void trackUpdated(Track track) {
        if (timer != null) {
            resetTimer();
        }
        mapChannels();
    }

    public long getTime() {
        return System.nanoTime() - lastStarted;
    }

    public void setSoundDelay(long delay) {
        soundDelay = delay;
    }

    public long getSoundDelay() {
        if (soundDelay != 0) {
            return soundDelay;
        } else {
            return microSecondsOffset;
        }
    }
}
