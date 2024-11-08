package org.chernovia.sims.ca.hodge.sonic;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import org.chernovia.lib.misc.awt.LabeledScrollbar;
import org.chernovia.lib.music.midi.JMIDI;
import org.chernovia.lib.music.midi.JMIDIUtil;
import org.chernovia.lib.music.midi.MIDINote;
import org.chernovia.lib.music.midi.toolbox.MIDIControl;
import org.chernovia.lib.music.midi.toolbox.MIDIControlListener;
import org.chernovia.lib.music.midi.toolbox.MIDIMetaPanel;
import org.chernovia.lib.music.midi.toolbox.MIDIPanel;
import org.chernovia.lib.music.midi.toolbox.MIDIPattern;
import org.chernovia.sims.ca.hodge.basic.HodgeEngine;

/**
 * This Sonifyer implements HodgeSonifyer to enable free (polyrhythmic) and interactive
 * MIDI polyphony using the org.chernovia.lib.music.midi.MIDINote and
 * org.chernovia.lib.music.toolbox.MIDIController classes.
 * 
 * It also implements javax.sound.midi.Sequencer playback and recording.
 * 
 * @author John Chernoff (jachern@yahoo.com)
 * @version (1/31/05)
 */
public abstract class HodgeMIDISonifyer extends HodgeAbstractSonifyer implements MIDIControlListener, MetaEventListener, AdjustmentListener, ActionListener {

    public static final String PLAY_BUTT_TXT = "Playback ", STOP_BUTT_TXT = "Stop Play", REC_BUTT_TXT = "Record ", REC_STOP_BUTT_TXT = "Stop Rec", PAUSE_BUTT_TXT = "Pause ", UNPAUSE_BUTT_TXT = "Unpause";

    public boolean DEBUG_NOTE = false, DEBUG_CLICK = false;

    public int[] statDurations = { 32, 16, 4, 8, 2, 2, 8, 4 };

    public int[] hodgeDurations = { 2, 4, 6, 8, 10, 12, 14, 16 };

    public static final int MAX_POLY = 8;

    public static int[] DEF_INST = { 0, 73, 71, 68, 70, 60, 41, 49 };

    public static int PITCH_RANGE = 55, PITCH_BASE = 33, MAX_VOL = 100;

    protected Checkbox OPT_LOOPING;

    protected MIDIControl MIDIController;

    protected Button RecordButton, PlayButton, PauseButton, SaveMIDIButton;

    protected LabeledScrollbar LSB_SQR_Tempo, LSB_Beats, LSB_SubBeats;

    protected Sequencer SQR;

    protected Sequence hodgeSequence;

    private MIDINote[] CurrentNotes;

    private MIDIMetaPanel metaMIDIPanel;

    private MIDIPattern[] patterns;

    private int[] instruments;

    private int[] pitchCenters;

    private MIDINote[] notesToSonify;

    private Track[] hodgeTracks;

    private boolean PLAYBACK = false, PAUSED = false, OLD_PAUSE = false, RECORDING = false;

    long recTicks = 0, playbackTick = 0;

    public HodgeMIDISonifyer(int beats, int subbeats, HodgeSonObserver hso) {
        super(hso);
        System.out.println("Loading: " + getClass().getName());
        CurrentNotes = new MIDINote[MAX_POLY];
        resetNotes();
        PlayButton = new Button(PLAY_BUTT_TXT);
        PlayButton.addActionListener(this);
        RecordButton = new Button(REC_BUTT_TXT);
        RecordButton.addActionListener(this);
        PauseButton = new Button(PAUSE_BUTT_TXT);
        PauseButton.addActionListener(this);
        SaveMIDIButton = new Button("Save MIDI");
        SaveMIDIButton.addActionListener(this);
        LSB_SQR_Tempo = new LabeledScrollbar("Playback Tempo", Scrollbar.VERTICAL, 120, 1, 1, 499, 1);
        LSB_SQR_Tempo.getScrollbar().addAdjustmentListener(this);
        OPT_LOOPING = new Checkbox("Loop Playback");
        hso.addSonificationControl(PlayButton);
        hso.addSonificationControl(RecordButton);
        hso.addSonificationControl(PauseButton);
        hso.addSonificationControl(SaveMIDIButton);
        hso.addSonificationControl(LSB_SQR_Tempo.getLabel());
        hso.addSonificationControl(LSB_SQR_Tempo.getScrollbar());
        hso.addSonificationControl(OPT_LOOPING);
        notesToSonify = new MIDINote[MAX_POLY];
        pitchCenters = new int[MAX_POLY];
        for (int v = 0; v < MAX_POLY; v++) {
            pitchCenters[v] = (int) (Math.random() * PITCH_RANGE) + PITCH_BASE;
        }
        try {
            SQR = MidiSystem.getSequencer();
            SQR.open();
        } catch (MidiUnavailableException augh) {
            System.out.println(augh);
        }
        SQR.addMetaEventListener(this);
        setPlaybackTempo(120);
        MIDIController = new MIDIControl(MAX_POLY, DEF_INST, 1, 100, beats, subbeats);
        patterns = (MIDIController.getMIDIPanel().getPatterns());
        metaMIDIPanel = MIDIController.getMIDIMetaPanel();
        MIDIController.addMIDIListener(this);
        MIDIController.notifyUpdating();
        MIDIController.showCtrl();
    }

    protected void resetNotes() {
        for (int i = 0; i < CurrentNotes.length; i++) CurrentNotes[i] = new MIDINote(0, 0, 0);
    }

    public void clearNotes() {
        if (!JMIDI.isReady()) return;
        for (int c = 0; c < getPolyphony(); c++) stopNote(CurrentNotes[c], c);
    }

    public void stopNote(MIDINote note, int channel) {
        if (!JMIDI.isReady()) return;
        JMIDI.getChannel(channel).noteOff(note.getPitch());
    }

    public void playNote(MIDINote note, int channel) {
        if (!JMIDI.isReady()) return;
        if (note.getPitch() != MIDINote.REST) {
            JMIDI.getChannel(channel).noteOn(note.getPitch(), note.getVolume());
        }
        CurrentNotes[channel] = note;
    }

    public MIDINote getNote(int c) {
        return CurrentNotes[c];
    }

    public int mapValueToPitch(int val, int max_val) {
        if (val <= 0 || val >= max_val) return MIDINote.REST;
        return (int) ((val / (float) max_val) * PITCH_RANGE) + PITCH_BASE;
    }

    public int mapValueToDuration(int val, int max_val, int dur_range) {
        return (int) ((val / (float) max_val) * dur_range);
    }

    private void initSequencer() {
        instruments = new int[MAX_POLY];
        recTicks = 0;
        playbackTick = 0;
        try {
            hodgeSequence = new Sequence(Sequence.PPQ, MIDIController.getMIDIPanel().getSubBeats(), MAX_POLY);
        } catch (InvalidMidiDataException augh) {
            System.out.println(augh);
        }
        hodgeTracks = hodgeSequence.getTracks();
        for (int v = 0; v < hodgeTracks.length; v++) {
            instruments[v] = -1;
        }
    }

    public void setPlaybackTempo(int t) {
        SQR.setTempoInBPM(t);
        System.out.println("Set Tempo: " + t);
    }

    public int getPitchCenter(int v) {
        return pitchCenters[v];
    }

    public void setPitchCenter(int v, int center) {
        pitchCenters[v] = center;
    }

    private synchronized void playNotes() {
        if (RECORDING && !PAUSED) recTicks++;
        int numVoices = getPolyphony();
        for (int v = 0; v < numVoices; v++) {
            if (notesToSonify[v] != null) {
                if (RECORDING && !PAUSED) manualRecord(v);
                stopNote(getNote(v), v);
                playNote(notesToSonify[v], v);
            }
        }
    }

    private synchronized void manualRecord(int v) {
        int newPitch = notesToSonify[v].getPitch();
        int oldPitch = getNote(v).getPitch();
        if (oldPitch != MIDINote.REST) {
            JMIDIUtil.addMessageToTrack(hodgeTracks[v], ShortMessage.NOTE_OFF, v, oldPitch, getNote(v).getVolume(), recTicks);
        }
        if (newPitch != MIDINote.REST) {
            JMIDIUtil.addMessageToTrack(hodgeTracks[v], ShortMessage.NOTE_ON, v, newPitch, notesToSonify[v].getVolume(), recTicks);
        }
        if (instruments[v] != patterns[v].getInstrument()) {
            instruments[v] = patterns[v].getInstrument();
            int bank = JMIDI.getInstrument(instruments[v]).getPatch().getBank();
            JMIDIUtil.addMessageToTrack(hodgeTracks[v], ShortMessage.PROGRAM_CHANGE, v, instruments[v], bank, recTicks);
        }
    }

    public synchronized void startRecording() {
        initSequencer();
        RECORDING = true;
    }

    public synchronized void stopRecording() {
        RECORDING = false;
    }

    public synchronized void startPlayback() {
        if (hodgeSequence == null) return;
        try {
            SQR.setSequence(hodgeSequence);
        } catch (InvalidMidiDataException augh) {
            System.out.println(augh);
        }
        JMIDI.silence();
        OLD_PAUSE = PAUSED;
        SQR.start();
        PLAYBACK = true;
        startingPlayback();
    }

    public synchronized void stopPlayback() {
        if (PAUSED) unpauseSonification();
        OPT_LOOPING.setState(false);
        SQR.setTickPosition(SQR.getSequence().getTickLength());
    }

    public void pauseSonification() {
        PAUSED = true;
        JMIDI.silence();
        if (PLAYBACK) {
            playbackTick = SQR.getTickPosition();
            SQR.stop();
        }
    }

    public void unpauseSonification() {
        PAUSED = false;
        if (PLAYBACK) {
            SQR.setTickPosition(playbackTick);
            SQR.start();
        }
    }

    public boolean isRecording() {
        return RECORDING;
    }

    public boolean isPlayingBack() {
        return PLAYBACK;
    }

    public boolean isPaused() {
        return PAUSED;
    }

    public boolean wasPaused() {
        return OLD_PAUSE;
    }

    public Sequence getCurrentSequence() {
        return hodgeSequence;
    }

    public MIDIPattern getMIDIPattern(int v) {
        return patterns[v];
    }

    public MIDIMetaPanel getMIDIMetaPanel() {
        return metaMIDIPanel;
    }

    public void closingMIDICtrl() {
        resetAllVoices();
    }

    protected void resetAllVoices() {
        JMIDI.silence();
        resetNotes();
        resetVoicePositions();
        for (int i = 0; i < MAX_POLY; i++) patterns[i].resetPosition();
        getObs().updateStatusTxt("Reset all voices.");
    }

    public void updatingMIDIMetaPanel(MIDIPanel panel, MIDIMetaPanel metaPanel) {
        setSonifying(!metaMIDIPanel.getMuteAll());
        setPolyphony(metaMIDIPanel.getPolyphony());
    }

    public void meta(MetaMessage metaMsg) {
        if (metaMsg.getType() == JMIDIUtil.END_OF_TRACK) {
            if (OPT_LOOPING.getState()) {
                float t = SQR.getTempoInBPM();
                SQR.start();
                SQR.setTempoInBPM(t);
            } else {
                JMIDI.silence();
                PAUSED = OLD_PAUSE;
                PLAYBACK = false;
                finishedPlayback();
            }
        }
    }

    @Override
    public synchronized void sonify(HodgeEngine hodge, int tick) {
        if (PLAYBACK || (PAUSED && !RECORDING)) return;
        playNotes();
        int numVoices = getPolyphony();
        for (int v = 0; v < numVoices; v++) {
            boolean newTick = getNote(v).nextTick();
            boolean newNote = newTick;
            if (newTick) {
                notesToSonify[v] = getNextNote(v, hodge);
                if (notesToSonify[v] != null) {
                    if (getMIDIPattern(v).isMuted()) notesToSonify[v].setVolume(0);
                    if (notesToSonify[v].getDuration() < 0) {
                        getNote(v).setDuration(-notesToSonify[v].getDuration());
                        notesToSonify[v] = null;
                        newNote = false;
                    } else if (notesToSonify[v].getPitch() == MIDINote.REST) newNote = false;
                }
                patterns[v].nextSubbeat();
            } else notesToSonify[v] = null;
            nextCurrentCell(v, newTick, newNote, hodge);
        }
    }

    public void adjustmentValueChanged(AdjustmentEvent e) {
        if (e.getSource() == LSB_SQR_Tempo.getScrollbar()) {
            setPlaybackTempo(LSB_SQR_Tempo.getValue());
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == PlayButton) {
            if (PlayButton.getLabel().equals(PLAY_BUTT_TXT)) startPlayback(); else stopPlayback();
        } else if (e.getSource() == RecordButton) {
            if (RecordButton.getLabel().equals(REC_BUTT_TXT)) {
                startRecording();
                RecordButton.setLabel(REC_STOP_BUTT_TXT);
            } else {
                stopRecording();
                RecordButton.setLabel(REC_BUTT_TXT);
            }
        } else if (e.getSource() == PauseButton) {
            if (PauseButton.getLabel().equals(PAUSE_BUTT_TXT)) {
                pauseSonification();
                PauseButton.setLabel(UNPAUSE_BUTT_TXT);
            } else {
                unpauseSonification();
                PauseButton.setLabel(PAUSE_BUTT_TXT);
            }
        } else if (e.getSource() == SaveMIDIButton) {
            getObs().saveSonicData(getCurrentSequence());
        }
        getObs().refreshControls();
    }

    public void startingPlayback() {
        getObs().updateStatusTxt("Starting Playback...");
        setPlaybackTempo(LSB_SQR_Tempo.getValue());
        PlayButton.setLabel(STOP_BUTT_TXT);
        PauseButton.setLabel(PAUSE_BUTT_TXT);
        getObs().refreshControls();
    }

    public void finishedPlayback() {
        getObs().updateStatusTxt("Finished Playback.");
        PlayButton.setLabel(PLAY_BUTT_TXT);
        if (wasPaused()) PauseButton.setLabel(UNPAUSE_BUTT_TXT); else PauseButton.setLabel(PAUSE_BUTT_TXT);
        getObs().refreshControls();
    }

    @Override
    public void cleanup() {
        JMIDI.silence();
        MIDIController.dispose();
        System.gc();
    }

    public abstract MIDINote getNextNote(int v, HodgeEngine hodge);

    public abstract void nextCurrentCell(int v, boolean newTick, boolean newNote, HodgeEngine hodge);
}
