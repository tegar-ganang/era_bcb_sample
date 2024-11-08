package com.xenoage.zong.io.midi.out;

import java.util.LinkedList;
import java.util.List;
import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import com.xenoage.pdlib.IVector;
import com.xenoage.zong.core.Score;
import com.xenoage.zong.core.music.MP;

/**
 * This class offers the interface for MIDI playback in
 * the program to play a given {@link Score}.
 * 
 * @author Uli Teschemacher
 * @author Andreas Wenger
 */
public class MidiScorePlayer implements ControllerEventListener {

    private SequenceContainer sequenceContainer = null;

    private List<PlaybackListener> listeners = new LinkedList<PlaybackListener>();

    private boolean metronomeEnabled;

    private float volume = 0.7f;

    private int currentPosition;

    private static final int NOTE_ON_EVENT = 119;

    private static final int PLAYBACK_END_EVENT = 117;

    /**
	 * Creates a new {@link MidiScorePlayer}.
	 */
    public MidiScorePlayer() throws MidiUnavailableException {
        setVolume(volume);
        SynthManager.removeAllControllerEventListeners();
        int controllers[] = { NOTE_ON_EVENT };
        SynthManager.addControllerEventListener(this, controllers);
        int controllersplaybackAtEnd[] = { PLAYBACK_END_EVENT };
        SynthManager.addControllerEventListener(this, controllersplaybackAtEnd);
    }

    /**
	 * Opens the given {@link Score} for playback.
	 */
    public void openScore(Score score) {
        stop();
        SequenceContainer container = MidiConverter.convertToSequence(score, true, true);
        this.sequenceContainer = container;
        try {
            SynthManager.getSequencer().setSequence(container.sequence);
        } catch (InvalidMidiDataException ex) {
        }
    }

    /**
	 * Registers the given {@link PlaybackListener} which will be
	 * informed about playback events like the current position.
	 */
    public void addPlaybackListener(PlaybackListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    /**
	 * Unregisters the given {@link PlaybackListener}. 
	 */
    public void removePlaybackListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    /**
	 * Changes the position of the playback cursor to the given
	 * time in microseconds.
	 */
    public void setMicrosecondPosition(long ms) {
        SynthManager.getSequencer().setMicrosecondPosition(ms);
        currentPosition = 0;
    }

    /**
	 * Changes the position of the playback cursor to the given
	 * {@link MP}.
	 */
    public void setMP(MP mp) {
        long tickPosition = calculateTickFromMP(mp, sequenceContainer.measureStartTicks, sequenceContainer.sequence.getResolution());
        SynthManager.getSequencer().setTickPosition(tickPosition);
        currentPosition = 0;
    }

    /**
	 * Starts playback at the current position.
	 */
    public void play() {
        Sequencer sequencer = SynthManager.getSequencer();
        if (sequencer.getSequence() != null) sequencer.start();
    }

    /**
	 * Stops the playback without resetting the
	 * current position.
	 */
    public void pause() {
        Sequencer sequencer = SynthManager.getSequencer();
        if (sequencer.isRunning()) {
            sequencer.stop();
            for (PlaybackListener listener : listeners) {
                listener.playbackStopped(sequenceContainer.timePool.getFirst().mp);
            }
        }
    }

    /**
	 * Stops the playback and sets the cursor to the start position.
	 */
    public void stop() {
        pause();
        setMicrosecondPosition(0);
        currentPosition = 0;
    }

    public boolean getMetronomeEnabled() {
        return metronomeEnabled;
    }

    public void setMetronomeEnabled(boolean metronomeEnabled) {
        this.metronomeEnabled = metronomeEnabled;
        Integer metronomeBeatTrackNumber = sequenceContainer.metronomeBeatTrackNumber;
        if (metronomeBeatTrackNumber != null) SynthManager.getSequencer().setTrackMute(metronomeBeatTrackNumber, !metronomeEnabled);
    }

    private long calculateTickFromMP(MP pos, IVector<Long> measureTicks, int resolution) {
        if (pos == null) {
            return 0;
        } else {
            return measureTicks.get(pos.getMeasure()) + MidiConverter.calculateTickFromFraction(pos.getBeat(), resolution);
        }
    }

    /**
	 * This method catches the ControllerChangedEvent from the sequencer.
	 * For MP-specific events, the method decides, which {@link MP} is the
	 * right one and notifies the listener.
	 */
    @Override
    public void controlChange(ShortMessage message) {
        IVector<MidiTime> timePool = sequenceContainer.timePool;
        if (message.getData1() == NOTE_ON_EVENT) {
            long currentTick = SynthManager.getSequencer().getTickPosition();
            if (timePool.getFirst().tick > currentTick) {
                return;
            }
            while (timePool.get(currentPosition + 1).tick <= currentTick) currentPosition++;
            MP pos = timePool.get(currentPosition).mp;
            for (PlaybackListener listener : listeners) {
                listener.playbackAtMP(pos);
            }
        } else if (message.getData1() == PLAYBACK_END_EVENT) {
            stop();
            for (PlaybackListener listener : listeners) {
                listener.playbackAtEnd();
            }
        }
    }

    /**
	 * Gets the volume, which is a value between 0 (silent) and 1 (loud).
	 */
    public float getVolume() {
        return volume;
    }

    /**
	 * Sets the volume.
	 * @param volume  value between 0 (silent) and 1 (loud)
	 */
    public void setVolume(float volume) {
        this.volume = volume;
        MidiChannel[] channels = SynthManager.getSynthesizer().getChannels();
        int max = 255;
        for (int i = 0; i < channels.length; i++) {
            channels[i].controlChange(7, Math.round(volume * max));
        }
    }

    /**
	 * Returns true, if the playback cursor is at the end of the
	 * score, otherwise false.
	 */
    public boolean isPlaybackFinished() {
        Sequencer sequencer = SynthManager.getSequencer();
        return sequencer.getMicrosecondPosition() >= sequencer.getMicrosecondLength();
    }

    /**
	 * Gets the length of the current sequence in microseconds,
	 * or 0 if no score is loaded.
	 */
    public long getMicrosecondLength() {
        if (sequenceContainer == null) return 0;
        return sequenceContainer.sequence.getMicrosecondLength();
    }

    /**
	 * Gets the current position within the sequence in microseconds,
	 * or 0 if no score is loaded.
	 */
    public long getMicrosecondPosition() {
        if (sequenceContainer == null) return 0;
        return SynthManager.getSequencer().getMicrosecondPosition();
    }

    public Sequence getSequence() {
        if (sequenceContainer != null) return sequenceContainer.sequence; else return null;
    }
}
