package vivace.model;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Vector;
import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;
import vivace.exception.HistoryException;
import vivace.exception.MidiMessageNotFoundException;
import vivace.exception.NoChannelAssignedException;
import vivace.helper.ProjectHelper;
import vivace.helper.TimeSignatureHelper;
import vivace.view.DumpReceiver;

/**
 * Model of a project. Contains all MIDI information for the current sequence and offers
 * methods to edit it. 
 */
public class Project extends Observable implements MetaEventListener {

    @Override
    public void meta(MetaMessage meta) {
        if (meta.getType() == MetaMessageType.END_OF_TRACK) {
            sequencer.setMicrosecondPosition(0);
            sequencer.stop();
            metronome.stopAndReset();
            notifyObservers(Action.SEQUENCER_STOPPED);
        }
    }

    private static final int MAX_UNDO_STEPS = 5;

    private HashSet<NoteEvent> affectedNotes;

    private Sequencer sequencer = null;

    private static Synthesizer synthesizer = null;

    private Sequence sequence = null;

    private Vector<Tuple<Sequence, Action>> history;

    private int historyIndex;

    private Action action;

    private static final int RESOLUTION = 96;

    private static final long END_OF_TRACK_INCREMENT = 500;

    private Integer key;

    private String name;

    private File file;

    private boolean isSaved;

    private Track recordingTrack;

    private int recordingTrackIndex;

    private MidiEvent endOfTrackEvent;

    private Metronome metronome;

    private long currentPosition;

    private void clearHistory() {
        history.clear();
        historyIndex = 0;
        saveCurrentState(action);
    }

    /** 
	 * Returns the affected notes (if any) for the last performed action
	 * @return
	 */
    public HashSet<NoteEvent> getAffectedNotes() {
        if (affectedNotes == null) affectedNotes = new HashSet<NoteEvent>();
        return affectedNotes;
    }

    /**
	 * Performs a undo step, i.e. moves the state to the first one in the history list.
	 * @throws HistoryException
	 */
    public void undo() throws HistoryException {
        if (!canUndo()) throw new HistoryException("E_UNDO_UNAVAILABLE");
        historyIndex = historyIndex - 1;
        Tuple<Sequence, Action> t = history.get(historyIndex - 1);
        sequence = t.a();
        try {
            sequencer.setSequence(t.a());
        } catch (InvalidMidiDataException e) {
        }
        setChanged();
        super.notifyObservers(action);
        action = t.b();
    }

    /**
	 * Moves the state to a specific item in the history list
	 * @param index
	 */
    public void moveToState(int index) {
        int tmp = historyIndex - 1;
        if (index >= 0 && index != tmp) {
            historyIndex = index + 1;
            Tuple<Sequence, Action> t = history.get(index);
            try {
                sequencer.setSequence(t.a());
            } catch (InvalidMidiDataException e) {
            }
            setChanged();
            if (index > tmp) {
                action = t.b();
                super.notifyObservers(action);
            } else {
                super.notifyObservers(action);
                action = t.b();
            }
        }
    }

    /**
	 * Returns the index of the current state
	 * @return
	 */
    public int currentStateIndex() {
        return history.indexOf(sequence);
    }

    /**
	 * Performs a redo step, i.e. moves the state one step "forward" in the history list.
	 * @throws HistoryException
	 */
    public void redo() throws HistoryException {
        if (!canRedo()) throw new HistoryException("E_REDO_UNAVAILABLE");
        historyIndex = historyIndex + 1;
        Tuple<Sequence, Action> t = history.get(historyIndex - 1);
        action = t.b();
        sequence = t.a();
        try {
            sequencer.setSequence(t.a());
        } catch (InvalidMidiDataException e) {
        }
        setChanged();
        super.notifyObservers(action);
    }

    /**
	 * Returns whether an undo step can be performed, i.e. if the project has any items in the history list
	 * @return
	 */
    public boolean canUndo() {
        return historyIndex > 1;
    }

    /**
	 * Returns whether an redo step can be performed, i.e. if the project has any items in the history list
	 * @return
	 */
    public boolean canRedo() {
        return historyIndex < history.size() && history.size() > 1;
    }

    /**
	 * Returns the history of actions
	 * @return
	 */
    public Vector<Action> getActionHistory() {
        Vector<Action> actions = new Vector<Action>();
        for (Tuple<Sequence, Action> t : history) {
            actions.add(t.b());
        }
        return actions;
    }

    /**
	 * Returns the current state
	 * @return
	 */
    public Tuple<Sequence, Action> getCurrentState() {
        return history.get(historyIndex - 1);
    }

    private void saveCurrentState(Action a) {
        if (history.size() > 1) {
            for (int i = history.size() - 1; i >= historyIndex; i--) {
                Tuple<Sequence, Action> foo = history.remove(i);
                sequence = foo.a();
                try {
                    sequencer.setSequence(sequence);
                } catch (InvalidMidiDataException e) {
                }
            }
        }
        if (history.size() >= MAX_UNDO_STEPS) {
            history.remove(0);
        } else {
            historyIndex++;
        }
        history.add(new Tuple<Sequence, Action>(ProjectHelper.cloneSequence(sequence), a));
    }

    /**
	 * Returns the last performed operation for the current instance. This value can be used
	 * in the observer's update-method to see what kind of operation that was performed.
	 * @return 
	 */
    public Integer getKey() {
        return key;
    }

    /**
	 * The name of the project
	 * @return 
	 */
    public String getName() {
        return name;
    }

    /**
	 * Sets the name of the project
	 * @param name
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * The file of the project
	 * @return 
	 */
    public File getFile() {
        return file;
    }

    /**
	 * Whether the project has unsaved changes or not
	 * @return 
	 */
    public boolean getIsSaved() {
        return isSaved;
    }

    private void init(Sequence seq) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequencer.addMetaEventListener(this);
        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        if (synthesizer.getDefaultSoundbank() == null) {
            synthesizer.loadAllInstruments(MidiSystem.getSoundbank(new File("resources/soundbank.gm")));
        }
        for (Transmitter t : sequencer.getTransmitters()) {
            t.close();
        }
        Receiver synthReceiver = synthesizer.getReceiver();
        Transmitter seqTransmitter = sequencer.getTransmitter();
        seqTransmitter.setReceiver(synthReceiver);
        sequencer.setSequence(seq);
        action = Action.PROJECT_OPENED;
        history = new Vector<Tuple<Sequence, Action>>(MAX_UNDO_STEPS);
        recordingTrack = null;
        clearHistory();
        metronome = new Metronome(this);
    }

    /**
	 * Constructor. Creates a new project with an "empty" sequence.
	 */
    public Project() throws MidiUnavailableException {
        isSaved = false;
        try {
            sequence = new Sequence(Sequence.PPQ, RESOLUTION);
            Track t = sequence.createTrack();
            MetaMessage timeSignature = new MetaMessage();
            byte[] data = { 4, 2 };
            timeSignature.setMessage(MetaMessageType.TRACK_TIMESIGNATURE, data, data.length);
            t.add(new MidiEvent(timeSignature, 0));
            setEndOfTrack(t, 1000);
            init(sequence);
            ShortMessage m = new ShortMessage();
            m.setMessage(ShortMessage.PROGRAM_CHANGE, 0, 0, 0);
            t.add(new MidiEvent(m, 0));
        } catch (IOException e) {
        } catch (InvalidMidiDataException e) {
        }
    }

    /**
	 * Constructor. Creates a new project from the specified midi file
	 */
    public Project(File midiFile) throws IOException, InvalidMidiDataException, MidiUnavailableException {
        file = midiFile;
        isSaved = true;
        sequence = MidiSystem.getSequence(midiFile);
        init(sequence);
    }

    /**
	 * Returns the sequencer
	 */
    public Sequencer getSequencer() {
        return sequencer;
    }

    private long latencyInTicks = -1;

    /**
	 * Returns the synthesizer's latency in ticks
	 * @return
	 */
    public long getLatencyInTicks() {
        long realTick = App.Project.getSequencer().getTickPosition();
        if (latencyInTicks == -1) {
            long latency = App.Project.getSynthesizer().getLatency();
            double factor = App.Project.getSequencer().getSequence().getMicrosecondLength() / App.Project.getSequencer().getSequence().getTickLength();
            latencyInTicks = (long) Math.ceil((double) latency / factor);
        }
        return latencyInTicks;
    }

    /**
	 * Creates a new track to the sequence 
	 */
    public void createTrack() {
        sequence.createTrack();
        reloadSequence();
        notifyObservers(Action.TRACK_ADDED);
    }

    /**
	 * Removes the tracks with indices in the given set
	 */
    public void removeTracks(HashSet<Integer> tracks) {
        Track t;
        for (Integer i : tracks) {
            t = getTrack(i);
            sequence.deleteTrack(t);
        }
        reloadSequence();
        notifyObservers(Action.TRACK_REMOVED);
    }

    /**
	 * Plays the current sequence, saving the current play position for later use by stop().
	 */
    public void play() throws NoChannelAssignedException {
        if (!sequencer.isRunning()) {
            if (sequencer.getMicrosecondPosition() == sequencer.getMicrosecondLength()) {
                currentPosition = 0;
                setMicrosecondPosition(0);
            } else {
                currentPosition = sequencer.getMicrosecondPosition();
            }
            if (isRecordEnabled()) {
                startRecording(recordingTrack);
            }
            if (!metronome.isRunning()) {
                metronome.start();
            }
            sequencer.start();
            notifyObservers(Action.SEQUENCER_STARTED);
        }
    }

    /**
	 * Stops/plays the sequencer, without saving/restoring the sequencer position.
	 */
    public void pause() {
        if (sequencer.isRunning()) {
            sequencer.stop();
            if (metronome.isRunning()) {
                metronome.stop();
            }
            notifyObservers(Action.SEQUENCER_STOPPED);
        }
    }

    /**
	 * Stops the sequencer, restoring the play position to where it was when play() was last invoked.
	 * Calling stop when not playing resets the play position to the beginning of the file.
	 */
    public void stop() {
        setMicrosecondPosition(currentPosition);
        currentPosition = 0;
        if (isRecording() || isRecordEnabled()) {
            stopRecording();
        }
        sequencer.stop();
        metronome.stopAndReset();
        notifyObservers(Action.SEQUENCER_STOPPED);
    }

    /**
	 * Starts recording on a specific track
	 * @param track The track that should be recorderd
	 * @throws NoChannelAssignedException
	 */
    public void startRecording(int track) throws NoChannelAssignedException {
        recordingTrack = getTrack(track);
        recordingTrackIndex = track;
        startRecording(recordingTrack);
    }

    private void startRecording(Track t) throws NoChannelAssignedException {
        try {
            endOfTrackEvent = ProjectHelper.filterFirstMetaMessage(t, MetaMessageType.END_OF_TRACK);
        } catch (MidiMessageNotFoundException e) {
        }
        sequencer.recordEnable(recordingTrack, getTrackChannel(recordingTrack));
        sequencer.startRecording();
        if (!metronome.isRunning()) {
            metronome.start();
        }
        sequencer.setTrackMute(recordingTrackIndex, true);
        new RecordingThread().start();
        notifyObservers(Action.SEQUENCER_STARTED);
    }

    /**
	 * Stops recording.
	 */
    public void stopRecording() {
        sequencer.stopRecording();
        sequencer.recordDisable(recordingTrack);
        sequencer.setTrackMute(recordingTrackIndex, false);
        recordingTrack = null;
        recordingTrackIndex = -1;
        setChanged();
        notifyObservers(Action.RECORDING_FINISHED);
    }

    /**
	 * Moves the sequencer to the specified tick
	 * @param tick
	 */
    public void setTickPosition(long tick) {
        sequencer.setTickPosition(tick);
        notifyObservers(Action.SEQUENCER_POSITION_CHANGED);
    }

    /**
	 * @return True if project is currently playing, false otherwise
	 */
    public boolean isPlaying() {
        return sequencer.isRunning();
    }

    /**
	 * @return True if project is currently recording, false otherwise
	 */
    public boolean isRecording() {
        return sequencer.isRecording();
    }

    /**
	 * @return True if any track is record enabled, false otherwise
	 */
    public boolean isRecordEnabled() {
        return recordingTrack != null;
    }

    /**
	 * Sets the position of the play marker
	 * @param	microseconds	the offset from the beginning of the project (in microseconds) to which the play marker will be moved
	 */
    public void setMicrosecondPosition(long microseconds) {
        sequencer.setMicrosecondPosition(microseconds);
        notifyObservers(Action.SEQUENCER_POSITION_CHANGED);
    }

    /**
	 * Sets the position of the play marker
	 * @param	ppm	the offset from the beginning of the project (in parts per million of the project's total length, 0 - 100)
	 */
    public void setPPMPosition(int ppm) {
        long microsecondLength = sequencer.getMicrosecondLength();
        long position = Math.round(microsecondLength * (double) ppm / 1000000);
        setMicrosecondPosition(position);
    }

    /**
	 * @return The play marker position represented in parts per million (PPM) of the project's length
	 */
    public int getPPMPosition() {
        long microsecondLength = sequencer.getMicrosecondLength();
        long microsecondPosition = sequencer.getMicrosecondPosition();
        int percentage = (int) Math.round((double) microsecondPosition / microsecondLength * 1000000);
        return percentage;
    }

    /**
	 * Returns the name of the track with the given track index
	 * @param track The index of the track
	 * @return
	 */
    public String getTrackName(int track) {
        Track t = getTrack(track);
        try {
            MetaMessage mm = (MetaMessage) ProjectHelper.filterFirstMetaMessage(t, 0x03).getMessage();
            return new String(mm.getData());
        } catch (MidiMessageNotFoundException e) {
            return "";
        }
    }

    /**
	 * Sets the name of the given track
	 * @param track Track index
	 * @param name New track name
	 */
    public void setTrackName(int track, String name) {
        Track t = getTrack(track);
        try {
            MetaMessage tn = (MetaMessage) ProjectHelper.filterFirstMetaMessage(t, 0x03).getMessage();
            tn.setMessage(MetaMessageType.TRACK_NAME, name.getBytes(), name.length());
        } catch (MidiMessageNotFoundException e) {
            try {
                MetaMessage tn = new MetaMessage();
                tn.setMessage(MetaMessageType.TRACK_NAME, name.getBytes(), name.length());
                t.add(new MidiEvent(tn, 0));
            } catch (InvalidMidiDataException ex) {
            }
        } catch (InvalidMidiDataException e) {
        }
        isSaved = false;
        notifyObservers(Action.TRACK_PARAMETERS_EDITED);
    }

    /**
	 * Set the playback channel of the track
	 * @param track Track index
	 * @param channel New playback channel
	 */
    public void setTrackChannel(int track, int channel) {
        if (channel < 0 || channel > 15) {
            throw new IllegalArgumentException("E_INVALID_CHANNEL");
        }
        Track t = getTrack(track);
        Vector<MidiEvent> shortMessageEvents = ProjectHelper.allShortMessages(t);
        if (shortMessageEvents.size() > 0) {
            for (MidiEvent e : shortMessageEvents) {
                ShortMessage sm = (ShortMessage) e.getMessage();
                try {
                    sm.setMessage(sm.getCommand(), channel, sm.getData1(), sm.getData2());
                } catch (InvalidMidiDataException ex) {
                }
            }
        } else {
            ShortMessage m = new ShortMessage();
            try {
                m.setMessage(ShortMessage.PROGRAM_CHANGE, channel, 0, 0);
                t.add(new MidiEvent(m, 0));
            } catch (InvalidMidiDataException ex) {
            }
        }
        reloadSequence();
        this.notifyObservers(Action.TRACK_PARAMETERS_EDITED);
    }

    /**
	 * Returns the playback channel of the given track
	 * @param track The track index
	 * @throws NoChannelAssignedException if track had no channel assigned to it
	 * were found, i.e. no way to fetch the track channel.
	 * @return
	 */
    public Integer getTrackChannel(int track) throws NoChannelAssignedException {
        Track t = getTrack(track);
        return getTrackChannel(t);
    }

    /**
	 * Returns the playback channel of the given track
	 * @param track The track
	 * @throws NoChannelAssignedException if track had no channel assigned to it
	 * were found, i.e. no way to fetch the track channel.
	 * @return
	 */
    public Integer getTrackChannel(Track t) throws NoChannelAssignedException {
        try {
            ShortMessage sm = ProjectHelper.firstShortMessage(t);
            return sm.getChannel();
        } catch (MidiMessageNotFoundException e) {
            throw new NoChannelAssignedException("E_NO_CHANNEL_ASSIGNED");
        }
    }

    /**
	 * Returns the instrument assigned to the given track
	 * @param track The track index
	 * @return
	 * @throws MidiMessageNotFoundException if no program change messages
	 * were found, i.e. no way to fetch the track instrument.
	 */
    public Instrument getTrackInstrument(int track) throws MidiMessageNotFoundException {
        Track t = getTrack(track);
        ShortMessage sm = ProjectHelper.filterFirstShortMessage(t, ShortMessage.PROGRAM_CHANGE);
        Instrument[] instruments = getInstruments();
        int program = sm.getData1();
        if (program > 0 && program < instruments.length) {
            return instruments[program];
        } else {
            return null;
        }
    }

    /**
	 * Sets the instrument assigned to the given track
	 * @param track The track
	 * @return
	 * @throws NoChannelFoundException if no channel is assigned to the track
	 */
    public void setTrackInstrument(int track, Instrument instr) throws NoChannelAssignedException {
        int program = instr.getPatch().getProgram();
        int bank = instr.getPatch().getProgram();
        int channel = getTrackChannel(track);
        Track t = getTrack(track);
        Vector<MidiEvent> events = ProjectHelper.filterShortMessages(t, ShortMessage.PROGRAM_CHANGE);
        ShortMessage sm = new ShortMessage();
        try {
            sm.setMessage(ShortMessage.PROGRAM_CHANGE, channel, program, bank);
        } catch (InvalidMidiDataException e) {
        }
        if (events.size() == 0) {
            MidiEvent e = new MidiEvent(sm, 0);
            t.add(e);
        } else {
            for (MidiEvent e : events) {
                t.remove(e);
                t.add(new MidiEvent(sm, e.getTick()));
            }
        }
        synthesizer.getChannels()[channel].programChange(bank, program);
        reloadSequence();
        this.notifyObservers(Action.TRACK_PARAMETERS_EDITED);
    }

    /**
	 * Returns the volume assigned to the given track
	 * @param track The track index
	 * @return
	 * @throws NoChannelAssignedException if no channel is assigned to the track
	 */
    public Integer getTrackVolume(int track) throws NoChannelAssignedException {
        Track t = getTrack(track);
        Vector<MidiEvent> volControlEvents = ProjectHelper.filterControlChanges(t, ControlChangeNumber.CHANNEL_VOLUME_MSB);
        if (volControlEvents.size() > 0) {
            ShortMessage firstVolControl = (ShortMessage) volControlEvents.firstElement().getMessage();
            return firstVolControl.getData2();
        } else {
            return null;
        }
    }

    /**
	 * Sets the volume assigned to the given track
	 * @param track The track
	 * @return
	 * @throws NoChannelAssignedException if no channel is assigned to the track
	 */
    public void setTrackVolume(int track, int volume) throws NoChannelAssignedException {
        int channel = getTrackChannel(track);
        Track t = getTrack(track);
        Vector<MidiEvent> events = ProjectHelper.filterControlChanges(t, ControlChangeNumber.CHANNEL_VOLUME_MSB);
        ShortMessage m;
        if (events.size() > 0) {
            for (MidiEvent e : events) {
                m = (ShortMessage) e.getMessage();
                try {
                    m.setMessage(m.getCommand(), m.getChannel(), m.getData1(), volume);
                } catch (InvalidMidiDataException ex) {
                }
            }
        } else {
            m = new ShortMessage();
            try {
                m.setMessage(ShortMessage.CONTROL_CHANGE, channel, ControlChangeNumber.CHANNEL_VOLUME_MSB, volume);
            } catch (InvalidMidiDataException ex) {
            }
            t.add(new MidiEvent(m, 0));
        }
        setChannelVolume(channel, volume);
        reloadSequence();
        this.notifyObservers(Action.TRACK_PARAMETERS_EDITED);
    }

    private void setChannelVolume(int channel, int volume) {
        MidiChannel ch = synthesizer.getChannels()[channel];
        ch.controlChange(ControlChangeNumber.CHANNEL_VOLUME_MSB, volume);
    }

    /**
	 * Sets the balance for the given track
	 * @param track The track index
	 * @param balance The balance
	 * @return
	 * @throws NoChannelAssignedException if no channel is assigned to the track
	 */
    public void setTrackBalance(int track, int balance) throws NoChannelAssignedException {
        Track t = getTrack(track);
        int channel = getTrackChannel(track);
        Vector<MidiEvent> events = ProjectHelper.filterControlChanges(t, ControlChangeNumber.CHANNEL_BALANCE_MSB);
        ShortMessage m;
        if (events.size() > 0) {
            for (MidiEvent e : events) {
                m = (ShortMessage) e.getMessage();
                try {
                    m.setMessage(m.getCommand(), m.getChannel(), m.getData1(), balance);
                } catch (InvalidMidiDataException ex) {
                }
            }
        } else {
            m = new ShortMessage();
            try {
                m.setMessage(ShortMessage.CONTROL_CHANGE, channel, ControlChangeNumber.CHANNEL_BALANCE_MSB, balance);
            } catch (InvalidMidiDataException ex) {
            }
            t.add(new MidiEvent(m, 0));
        }
        MidiChannel ch = synthesizer.getChannels()[channel];
        ch.controlChange(ControlChangeNumber.CHANNEL_BALANCE_MSB, balance);
        reloadSequence();
        this.notifyObservers(Action.TRACK_PARAMETERS_EDITED);
    }

    /**
	 * Get all the timeSignatures in the midifile
	 * @return	int[][]	signatureList	A 2D list containing a reference to the ceratain wanted timesignature
	 * and a reference to the Timesignature specifics for that event. Ex: signatureList[1][1] is saying that we
	 * want to look at the second time signature event in the midi-file and the Divisor for that event.
	 * The values for the timesignatures are: [0] - The Numerator [1] - The Divisor [2] - Where the event accured in ticks
	 */
    public TimeSignatureHelper[] getTimeSignatures() {
        Track[] tracks = sequence.getTracks();
        TimeSignatureHelper[] signatureList;
        Vector<MidiEvent> events;
        Track t = tracks[0];
        events = ProjectHelper.filterMetaMessages(t, MetaMessageType.TRACK_TIMESIGNATURE);
        if (events.size() == 0) {
            byte[] bites = new byte[2];
            bites[0] = 4;
            bites[1] = 2;
            MetaMessage quartMeta = new MetaMessage();
            try {
                quartMeta.setMessage(MetaMessageType.TRACK_TIMESIGNATURE, bites, 2);
            } catch (InvalidMidiDataException e) {
            }
            MidiMessage quartMessage = quartMeta;
            MidiEvent quarterEvent = new MidiEvent(quartMessage, 0);
            tracks[0].add(quarterEvent);
            signatureList = new TimeSignatureHelper[2];
        } else {
            signatureList = new TimeSignatureHelper[events.size() + 1];
        }
        int position = 0;
        for (MidiEvent event : events) {
            MetaMessage meta = (MetaMessage) events.get(position).getMessage();
            byte[] bite = meta.getData();
            int z = 1;
            for (int i = 0; i < bite[1]; i++) {
                z = z * 2;
            }
            signatureList[position] = new TimeSignatureHelper(bite[0], z, (int) events.get(position).getTick(), event);
            position++;
        }
        signatureList[signatureList.length - 1] = new TimeSignatureHelper(0, 0, 0, null);
        return signatureList;
    }

    /**
	 * Removes the specified note from the sequence
	 * @param note
	 * @param notify Whether or not to notify the observers
	 */
    public void removeNote(NoteEvent note, boolean notify) {
        getAffectedNotes().add(note);
        sequence.getTracks()[note.getTrackIndex()].remove(note.getNoteOn());
        sequence.getTracks()[note.getTrackIndex()].remove(note.getNoteOff());
        if (notify) {
            notifyObservers(Action.NOTE_REMOVED);
            getAffectedNotes().clear();
        }
    }

    /**
	 * Sets the notes start and end tick.
	 * @param note
	 * @param startTick
	 * @param stopTick
	 * @param notify Whether or not to notify the observers
	 */
    public void resizeNote(NoteEvent note, long startTick, long stopTick, boolean notify) {
        getAffectedNotes().add(note);
        moveEvent(note.getNoteOn(), startTick, false);
        moveEvent(note.getNoteOff(), stopTick, false);
        if (notify) {
            notifyObservers(Action.NOTE_RESIZED);
            getAffectedNotes().clear();
        }
    }

    /**
	 * Moves a note a certain number of ticks. The tick parameter can be negative.
	 * @param note
	 * @param ticks
	 * @param noteValue
	 * @param notify Whether or not to notify the observers
	 */
    public void moveNote(NoteEvent note, long ticks, int noteValue, boolean notify) {
        try {
            getAffectedNotes().add(note);
            ShortMessage smNoteOn = (ShortMessage) note.getNoteOn().getMessage();
            ShortMessage smNoteOff = (ShortMessage) note.getNoteOff().getMessage();
            smNoteOn.setMessage(smNoteOn.getStatus(), noteValue, smNoteOn.getData2());
            smNoteOff.setMessage(smNoteOff.getStatus(), noteValue, smNoteOff.getData2());
        } catch (InvalidMidiDataException ex) {
        }
        moveEvent(note.getNoteOn(), note.getNoteOn().getTick() + ticks, false);
        moveEvent(note.getNoteOff(), note.getNoteOff().getTick() + ticks, false);
        if (notify) {
            notifyObservers(Action.NOTE_MOVED);
            getAffectedNotes().clear();
        }
    }

    /**
	 * Moves a track a certain number of ticks
	 * The parameter can be negative
	 * @param track
	 * @param long
	 * @param notify Whether or not to notify the observers
	 */
    public void moveTrack(int track, long ticks, boolean notify) {
        Vector<MidiEvent> es = ProjectHelper.allMidiEvents(getTrack(track));
        for (MidiEvent e : es) e.setTick(e.getTick() + ticks);
        if (notify) notifyObservers(Action.TRACK_MOVED);
    }

    /**
	 * Moves an event to the specified tick
	 * @param e
	 * @param tick
	 * @param notify Whether or not to notify the observers
	 */
    public void moveEvent(MidiEvent e, long tick, boolean notify) {
        e.setTick(tick);
        if (notify) notifyObservers(Action.NOTE_MOVED);
    }

    /**
	 * Adds the specified note to the sequence 
	 * @param note
	 * @param notify Whether or not to notify the observers
	 */
    public void addNote(NoteEvent note, boolean notify) {
        getAffectedNotes().add(note);
        sequence.getTracks()[note.getTrackIndex()].add(note.getNoteOn());
        sequence.getTracks()[note.getTrackIndex()].add(note.getNoteOff());
        if (notify) {
            notifyObservers(Action.NOTE_ADDED);
            getAffectedNotes().clear();
        }
    }

    /**
	 * Sets the velocity for the specified note.
	 * @param sm
	 * @param value
	 * @param notify Whether or not to notify the observers
	 */
    public void setNoteVelocity(NoteEvent note, int value, boolean notify) {
        ShortMessage sm = (ShortMessage) note.getNoteOn().getMessage();
        if (sm.getData2() != value || notify) {
            try {
                getAffectedNotes().add(note);
                sm.setMessage(sm.getStatus(), sm.getData1(), value);
            } catch (InvalidMidiDataException ex) {
                ex.printStackTrace();
            }
            if (notify) {
                notifyObservers(Action.EVENT_PARAMETER_CHANGED);
                getAffectedNotes().clear();
            }
        }
    }

    /**
	 * Adds an event on a specific position in track 0
	 * @param numerator
	 * @param denominator
	 * @param tickPosition
	 */
    public void addTimesignatureEventTrack0(int numerator, int denominator, long tickPosition) {
        byte[] bites = new byte[2];
        bites[0] = (byte) numerator;
        int z = 2;
        int denominatorPow = 1;
        while (z != denominator) {
            denominatorPow++;
            z = z * 2;
        }
        bites[1] = (byte) denominatorPow;
        MetaMessage newMeta = new MetaMessage();
        try {
            newMeta.setMessage(MetaMessageType.TRACK_TIMESIGNATURE, bites, 2);
        } catch (InvalidMidiDataException e) {
        }
        Track[] tracks = sequence.getTracks();
        Track t = tracks[0];
        MidiEvent newEvent = new MidiEvent(newMeta, tickPosition);
        t.add(newEvent);
        this.notifyObservers(Action.TIMESIGNATURE_ADDED);
    }

    /**
 	* Adds a new tempochange metaevent into track 0 at position tickPosition
 	* If there are no temposetting metaevents in the file, then yet another metaevent will be created 
 	* at tickPosition 0 with the same bpm
 	* @param cpqn
 	* @param tickPosition
	*/
    public void newTempoChange(double cpqn, long tickPosition) {
        MetaMessage firstMeta, newMeta = new MetaMessage();
        try {
            if (ProjectHelper.filterMetaMessages(App.Project.getTrack(0), 0x51).size() == 0) {
                firstMeta = new MetaMessage();
                firstMeta.setMessage(0x51, cpqnToBytes(cpqn), 3);
                sequence.getTracks()[0].add(new MidiEvent(firstMeta, tickPosition));
            }
            newMeta.setMessage(0x51, cpqnToBytes(cpqn), 3);
        } catch (InvalidMidiDataException e) {
            System.out.println("Fel i mididatat");
        }
        sequence.getTracks()[0].add(new MidiEvent(newMeta, tickPosition));
        this.notifyObservers(Action.TEMPOCHANGE_CREATED);
    }

    /**
 	* Removes a tempo change number event
 	* @param event
	*/
    public void removeTempoChange(int event) {
        if (ProjectHelper.filterMetaMessages(App.Project.getTrack(0), 0x51).size() != 0 && event == 0) newTempoChange(500000, 0);
        if (ProjectHelper.filterMetaMessages(App.Project.getTrack(0), 0x51).size() != 0) sequence.getTracks()[0].remove(ProjectHelper.filterMetaMessages(App.Project.getTrack(0), 0x51).elementAt(event));
        this.notifyObservers(Action.TEMPOCHANGE_DELETED);
    }

    /**
 	* Changes the timestamp of tempochange event into tickPosition
 	* @param event
 	* @param tickPosition
	*/
    public void editTempoChangeTime(int event, long tickPosition) {
        Vector<MidiEvent> tc = ProjectHelper.filterMetaMessages(App.Project.getTrack(0), 0x51);
        MidiEvent newEvent, oldEvent;
        oldEvent = tc.elementAt(event);
        newEvent = new MidiEvent(oldEvent.getMessage(), tickPosition);
        sequence.getTracks()[0].remove(oldEvent);
        sequence.getTracks()[0].add(newEvent);
        this.notifyObservers(Action.TEMPOTIME_CHANGED);
    }

    /**
 	* Changes the tempo of a tempo change number event into cpqn 
 	* @param event
 	* @param cpqn
	*/
    public void editTempoChangeTempo(int event, double cpqn) {
        byte[] bites = new byte[3];
        MidiEvent newEvent;
        MetaMessage newMeta;
        bites = cpqnToBytes(cpqn);
        newMeta = new MetaMessage();
        try {
            newMeta.setMessage(0x51, bites, 3);
        } catch (InvalidMidiDataException e) {
        }
        newEvent = new MidiEvent(newMeta, ProjectHelper.filterMetaMessages(App.Project.getTrack(0), 0x51).elementAt(event).getTick());
        sequence.getTracks()[0].remove(ProjectHelper.filterMetaMessages(App.Project.getTrack(0), 0x51).elementAt(event));
        sequence.getTracks()[0].add(newEvent);
        this.notifyObservers(Action.TEMPO_CHANGED);
    }

    /**
 	* Takes clocks per quarternote and converts it into three bytes 
 	* (this is the way it is stored in a tempochange metaevent)
 	* @param cpqn
	*/
    private byte[] cpqnToBytes(double cpqn) {
        byte[] bites = new byte[3];
        int temp;
        for (int index = 2; index >= 0; index--) {
            temp = (int) (Math.floor(cpqn / (Math.pow(256, index))));
            bites[2 - index] = (byte) temp;
            cpqn = cpqn - (temp * (Math.pow(256, index)));
        }
        return bites;
    }

    /**
	 * Takes an timesignature event and edits the values of it from track 0
	 * @param event
	 * @param numerator
	 * @param denominator
	 */
    public void editTimeSignatureEventTrack0(MidiEvent event, int numerator, int denominator) {
        byte[] bites = new byte[2];
        bites[0] = (byte) numerator;
        int z = 2;
        int denominatorPow = 1;
        while (z != denominator) {
            denominatorPow++;
            z = z * 2;
        }
        bites[1] = (byte) denominatorPow;
        MetaMessage newMeta = new MetaMessage();
        try {
            newMeta.setMessage(MetaMessageType.TRACK_TIMESIGNATURE, bites, 2);
        } catch (InvalidMidiDataException e) {
        }
        MidiEvent newEvent = new MidiEvent(newMeta, event.getTick());
        Track[] tracks = sequence.getTracks();
        Track t = tracks[0];
        t.remove(event);
        t.add(newEvent);
        this.notifyObservers(Action.TIMESIGNATRUE_EDIT);
    }

    /**
	 * Removes a MidiEvent from track 0
	 * @param event
	 */
    public void removeEventTrack0(MidiEvent event) {
        Track[] tracks = sequence.getTracks();
        Track t = tracks[0];
        t.remove(event);
        this.notifyObservers(Action.TIMESIGNATURE_REMOVED);
    }

    /**
	 * Returns the balance for the specified track
	 * @param track
	 * @return
	 * @throws NoChannelAssignedException
	 */
    public Integer getTrackBalance(int track) throws NoChannelAssignedException {
        Track t = getTrack(track);
        Vector<MidiEvent> balControlEvents = ProjectHelper.filterControlChanges(t, ControlChangeNumber.CHANNEL_BALANCE_MSB);
        if (balControlEvents.size() > 0) {
            ShortMessage balControl = (ShortMessage) balControlEvents.firstElement().getMessage();
            return balControl.getData2();
        } else {
            return null;
        }
    }

    /**
	 * Returns an array of currently available instruments
	 * for the current synthesizer
	 * @return
	 */
    public Instrument[] getInstruments() {
        Instrument[] instr = synthesizer.getLoadedInstruments();
        if (instr.length == 0) {
            instr = synthesizer.getAvailableInstruments();
        }
        return instr;
    }

    /**
	 * Gets a reference to the track with the specified index
	 * @param track Track index
	 */
    public Track getTrack(int track) {
        Track[] tracks = getTracks();
        return tracks[track];
    }

    /**
	 * Returns an array of all tracks in the sequence
	 * @return
	 */
    public Track[] getTracks() {
        return sequencer.getSequence().getTracks();
    }

    /**
	 * Saves the project. Requires that the file path is set.
	 */
    public void save() throws IOException {
        clearHistory();
        MidiSystem.write(sequencer.getSequence(), 1, file);
        name = file.getName();
        this.notifyObservers(Action.SAVED);
    }

    /**
	 * Saves the project to the file
	 * @param file
	 */
    public void save(File file) throws IOException {
        this.file = file;
        save();
    }

    /**
	 * Get the number of ticks in the sequence
	 * @return	Int		The returned value is the number of ticks in the sequence round off and + 1
	 */
    public int getSequenceLength() {
        return (int) sequence.getTickLength();
    }

    /**
	 * Get the PPQ for the sequence
	 * @return	Int		The PPQ (a quarter note of a time signature)
	 */
    public int getPPQ() {
        return sequence.getResolution();
    }

    /**
	 * Get the number of timesignatures in the sequence
	 * @param Int	signature	The chose timesignature divider
	 * @return		the number of timesignatures in the sequence
	 */
    public int getNumberOfSignatures(int signature) {
        long tracklength = sequence.getTickLength();
        int ppq = sequence.getResolution();
        return (int) (tracklength / (signature * ppq) + 1);
    }

    /**
	 * Returns the currently used synthesizer
	 * @return
	 */
    public Synthesizer getSynthesizer() {
        return synthesizer;
    }

    /**
	 * Sets the current synthesizer
	 * @param s
	 */
    public void setSynthesizer(Synthesizer s) {
        synthesizer = s;
    }

    /**
	 * Turns off sound on all channels
	 * Useful for stuck notes on the synthesizer
	 */
    public void panic() {
        for (MidiChannel ch : synthesizer.getChannels()) {
            ch.allSoundOff();
        }
    }

    /**
	 * Update the device getting MIDI-data from
	 * @param device
	 * @throws MidiUnavailableException
	 */
    public void updateInDevice(MidiDevice device) throws MidiUnavailableException {
        for (Receiver r : App.Project.getSequencer().getReceivers()) {
            r.close();
        }
        device.open();
        Receiver r1 = DumpReceiver.createInstance(System.out);
        Transmitter t1 = device.getTransmitter();
        t1.setReceiver(r1);
        Receiver r2 = synthesizer.getReceiver();
        Transmitter t2 = device.getTransmitter();
        t2.setReceiver(r2);
        latencyInTicks = -1;
        notifyObservers(Action.DEVICE_CHANGED);
    }

    /**
	 * Update the MIDI-out device transmitting data to
	 */
    public void updateOutDevice(MidiDevice device) throws MidiUnavailableException {
        for (Transmitter f : sequencer.getTransmitters()) {
            f.close();
        }
        for (Receiver r : synthesizer.getReceivers()) {
            r.close();
        }
        device.open();
        Transmitter transmitter = sequencer.getTransmitter();
        Receiver receiver = device.getReceiver();
        transmitter.setReceiver(receiver);
        latencyInTicks = -1;
        notifyObservers(Action.DEVICE_CHANGED);
    }

    private void notifyObservers(Action a) {
        if (a == Action.SAVED) {
            isSaved = true;
        } else if (a.isEdit()) {
            saveCurrentState(a);
            action = a;
            isSaved = false;
        }
        App.setNotifying(true);
        setChanged();
        super.notifyObservers(a);
        App.setNotifying(false);
    }

    private void printHistory(String s) {
        for (int j = 0; j < history.size(); j++) {
            Tuple<Sequence, Action> t = history.get(j);
            System.out.println(j + " - " + t.a() + ", " + t.b() + ": " + t.a().getTracks().length);
        }
    }

    /**
	 * Returns the project's resolution in ticks per beat.
	 * @return the resolution in ticks per beat
	 */
    public long getPPQResolution() {
        if (sequence.getDivisionType() == Sequence.PPQ) {
            return sequence.getResolution();
        } else {
            return -1;
        }
    }

    /**
	 * Sets the specified track to record enabled
	 * @param t
	 */
    public void setRecordingTrack(Track t) {
        recordingTrack = t;
    }

    private void setEndOfTrack(Track t, long tick) {
        try {
            MidiEvent e = ProjectHelper.filterFirstMetaMessage(t, MetaMessageType.END_OF_TRACK);
            e.setTick(tick);
        } catch (MidiMessageNotFoundException ex) {
        }
    }

    /**
	 * Reloads the sequence. Useful if, for instance, tracks have been added, since the sequencer
	 * "caches" the sequence.
	 */
    private void reloadSequence() {
        try {
            sequencer.setSequence(sequence);
        } catch (InvalidMidiDataException e) {
        }
    }

    /**
	 * Runnable class that is executed during recording to prolong the "End of track" message for the 
	 * recorded track.
	 */
    private class RecordingThread extends Thread {

        @Override
        public void run() {
            long eotTick;
            while (sequencer.isRecording()) {
                eotTick = endOfTrackEvent.getTick();
                if (eotTick - sequencer.getTickPosition() < END_OF_TRACK_INCREMENT) {
                    moveEvent(endOfTrackEvent, eotTick + END_OF_TRACK_INCREMENT, true);
                    try {
                        long sleep = Math.round((double) (sequencer.getTempoInMPQ() / (RESOLUTION * 1000)) * END_OF_TRACK_INCREMENT);
                        sleep(sleep);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }
}
