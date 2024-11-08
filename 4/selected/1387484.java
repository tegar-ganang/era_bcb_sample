package com.frinika.sequencer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;
import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;
import com.frinika.project.FrinikaAudioSystem;
import com.frinika.sequencer.gui.RecordingDialog;
import com.frinika.sequencer.midi.MidiMessageListener;
import com.frinika.sequencer.midi.MonitorReceiver;
import com.frinika.sequencer.model.AudioLane;
import com.frinika.sequencer.model.ChannelEvent;
import com.frinika.sequencer.model.ControllerEvent;
import com.frinika.sequencer.model.MidiLane;
import com.frinika.sequencer.model.MidiPart;
import com.frinika.sequencer.model.MidiPlayOptions;
import com.frinika.sequencer.model.MultiEvent;
import com.frinika.sequencer.model.NoteEvent;
import com.frinika.sequencer.model.Part;
import com.frinika.sequencer.model.PitchBendEvent;
import com.frinika.sequencer.model.RecordableLane;
import com.frinika.synth.SynthRack;
import com.frinika.sequencer.model.tempo.TempoList;

/**
 * The purpose of the Frinika sequencer implementation is to solve
 * the following issues of the current implementation in Sun J2SE5.0:
 * - Smooth looping
 * - Don't skip notes / events on the first tick when starting to play
 * - Able to insert / remove events from the sequence while playing (not same as
 * 	 recording)
 * - Better abilities for song position monitoring
 *   NOTE: The Frinika sequencer is not a complete implementation of the Java sequencer. Its
 *         primary goals is to solve the needs of Frinika, and the implementation subset
 *         is thereafter.
 *
 * To be able to insert and remove events while playing the sequence has to be reorganized 
 * from a vector, to a hashtable structure. In the default sequencer pointers to the next 
 * midi event to be played is based on the vector index. When inserting or removing notes 
 * before the current index the sequencer pointer will break. By using a hashtable with 
 * keyword tick and value set to an array of events occuring at that tick, one is able to
 * lookup notes to be played for a given tick position - thus not vulnerable to changes in
 * the ordering of events.
 * 
 * The skipping of notes on the start tick, is caused by a bug in the binary search routine
 * of the default sequencer. This binary search is searching for the first event to play
 * after the given starttick. By using the mentioned hashtable above, this is not an issue
 * anymore.
 * 
 * Issues with smooth looping is suspected though not verified, to be caused by chasing of
 * controller values when resetting the loop. The symptom is that the longer out in the song
 * you are, the loop gap is longer and longer. 
 * 
 * @author Peter Johan Salomonsen
 *
 * TODO PJL repair notification for recording.
 */
public class FrinikaSequencer implements Sequencer {

    private FrinikaSequence sequence;

    private FrinikaSequencerPlayer player = new FrinikaSequencerPlayer(this);

    private int loopCount;

    private long loopStartPoint;

    private long loopEndPoint;

    private float bpm = 100;

    private boolean recording;

    private final List<Transmitter> transmitters = new ArrayList<Transmitter>();

    private final List<Receiver> receivers = new ArrayList<Receiver>();

    private final List<SongPositionListener> songPositionListeners = new ArrayList<SongPositionListener>();

    private final ArrayList<SequencerListener> sequencerListeners = new ArrayList<SequencerListener>();

    private final Collection<MidiMessageListener> midiMessageListeners = new HashSet<MidiMessageListener>();

    /**
     * List of Midi Out Devices mapped to the transmitters
     */
    private final HashMap<MidiDevice, Transmitter> midiOutDeviceTransmitters = new HashMap<MidiDevice, Transmitter>();

    private final List<MidiDevice> midiOutDevices = new ArrayList<MidiDevice>();

    /** 
     * Songposition listeners that requires to be notified on each tick  
	 */
    private final ArrayList<SongPositionListener> intenseSongPositionListeners = new ArrayList<SongPositionListener>();

    /** 
     * Tempochange listeners   
	 */
    private final ArrayList<TempoChangeListener> tempoChangeListeners = new ArrayList<TempoChangeListener>();

    /**
     * MidiLanes that are armed for recording
     */
    private final HashSet<RecordableLane> recordingLanes = new HashSet<RecordableLane>();

    private Vector<Vector<MultiEvent>> recordingTakes = new Vector<Vector<MultiEvent>>();

    private Vector<MultiEvent> currentRecordingTake = new Vector<MultiEvent>();

    private RecordingDialog recordingTakeDialog = null;

    /**
     * Solo / mute
     */
    private final HashSet<FrinikaTrackWrapper> soloFrinikaTrackWrappers = new HashSet<FrinikaTrackWrapper>();

    private final HashMap<FrinikaTrackWrapper, MidiPlayOptions> frinikaTrackWrappersMidiPlayOptions = new HashMap<FrinikaTrackWrapper, MidiPlayOptions>();

    private Transmitter transmitter;

    HashMap<Integer, NoteEvent> pendingNoteEvents = new HashMap<Integer, NoteEvent>();

    int lastLoopCount;

    private Receiver receiver = new MonitorReceiver(getMidiMessageListeners(), new Receiver() {

        void addEventToRecordingTracks(ChannelEvent event) {
            currentRecordingTake.add(event);
        }

        public void send(MidiMessage message, long timeStamp) {
            long tick = player.getRealTimeTickPosition();
            for (RecordableLane rlane : recordingLanes) {
                if (!(rlane instanceof MidiLane)) continue;
                MidiLane lane = (MidiLane) rlane;
                MidiDevice dev = lane.getMidiDevice();
                MidiPlayOptions po = lane.getPlayOptions();
                if (po.muted) continue;
                if (dev == null) continue;
                if (message instanceof ShortMessage) {
                    ShortMessage shm = (ShortMessage) message;
                    try {
                        ShortMessage throughShm;
                        int cmd = shm.getCommand();
                        int chn = lane.getMidiChannel();
                        int dat1 = shm.getData1();
                        int dat2 = shm.getData2();
                        throughShm = new ShortMessage();
                        if ((cmd == ShortMessage.NOTE_ON) || (cmd == ShortMessage.NOTE_OFF)) {
                            dat1 = FrinikaSequencerPlayer.applyPlayOptionsNote(po, dat1);
                            dat2 = FrinikaSequencerPlayer.applyPlayOptionsVelocity(po, dat2);
                            throughShm.setMessage(cmd, chn, dat1, dat2);
                            dev.getReceiver().send(throughShm, -1);
                        } else {
                            throughShm.setMessage(cmd, chn, dat1, dat2);
                            dev.getReceiver().send(throughShm, -1);
                            System.out.println(shm.getData1() + " " + shm.getData2() + "  " + throughShm.getData1() + " " + throughShm.getData2());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (true) return;
            if (recording) {
                if (player.getLoopCount() > lastLoopCount) {
                    newRecordingTake();
                    lastLoopCount = player.getLoopCount();
                }
                if (message instanceof ShortMessage) {
                    ShortMessage shm = (ShortMessage) message;
                    if (shm.getCommand() == ShortMessage.NOTE_ON || shm.getCommand() == ShortMessage.NOTE_OFF) {
                        if (shm.getCommand() == ShortMessage.NOTE_OFF || shm.getData2() == 0) {
                            NoteEvent noteEvent = pendingNoteEvents.get(shm.getChannel() << 8 | shm.getData1());
                            if (noteEvent != null) {
                                noteEvent.setDuration(tick - noteEvent.getStartTick());
                                pendingNoteEvents.remove(shm.getChannel() << 8 | shm.getData1());
                                addEventToRecordingTracks(noteEvent);
                            }
                        } else {
                            pendingNoteEvents.put(shm.getChannel() << 8 | shm.getData1(), new NoteEvent((FrinikaTrackWrapper) null, tick, shm.getData1(), shm.getData2(), shm.getChannel(), 0));
                        }
                    } else if (shm.getCommand() == ShortMessage.CONTROL_CHANGE) {
                        addEventToRecordingTracks(new ControllerEvent((FrinikaTrackWrapper) null, tick, shm.getData1(), shm.getData2()));
                    } else if (shm.getCommand() == ShortMessage.PITCH_BEND) {
                        addEventToRecordingTracks(new PitchBendEvent((FrinikaTrackWrapper) null, tick, ((shm.getData1()) | (shm.getData2() << 7)) & 0x7fff));
                    }
                }
            }
        }

        public void close() {
        }
    });

    public FrinikaSequencer() {
        receivers.add(receiver);
    }

    /**
         * PJL added for Recording Manager
         * 
         * @return
         */
    public long getRealTimeTickPosition() {
        return player.getRealTimeTickPosition();
    }

    public void setLoopEndPointInBeats(double d) {
        long ticks = (long) (d * sequence.getResolution());
        setLoopEndPoint(ticks);
    }

    public void setLoopStartPointInBeats(double d) {
        long ticks = (long) (d * sequence.getResolution());
        setLoopStartPoint(ticks);
    }

    public void setSequence(Sequence sequence) throws InvalidMidiDataException {
        this.sequence = (FrinikaSequence) sequence;
    }

    public void setSequence(InputStream stream) throws IOException, InvalidMidiDataException {
    }

    public Sequence getSequence() {
        return sequence;
    }

    public void start() {
        player.setLatencyCompensationInMillis(FrinikaAudioSystem.getAudioServer().getOutputLatencyMillis());
        for (SequencerListener listener : sequencerListeners) listener.beforeStart();
        player.start();
        for (SequencerListener listener : sequencerListeners) listener.start();
    }

    public void stop() {
        if (recording) {
            newRecordingTake();
            recording = false;
        }
        player.stop();
        ArrayList<SequencerListener> list = (ArrayList) sequencerListeners.clone();
        for (SequencerListener listener : list) listener.stop();
    }

    public boolean isRunning() {
        return player.isRunning();
    }

    /**
     * Add a new take for recording
     *
	 */
    final void newRecordingTake() {
        if (currentRecordingTake.size() > 0) {
            recordingTakes.add(currentRecordingTake);
            if (recordingTakeDialog != null) {
                recordingTakeDialog.notifyNewTake(recordingTakes.size() - 1);
                if (recordingTakes.size() > 1) recordingTakeDialog.setVisible(true);
            }
        }
        currentRecordingTake = new Vector<MultiEvent>();
    }

    /**
     * Get number of takes from the last recording
     * @return
     */
    public int getNumberOfTakes() {
        return recordingTakes.size();
    }

    /**
     * Deploy one of the takes from the last recording to recording lanes
     * @param takeNo - the take to deploy
     */
    public void deployTake(int[] takeNumbers) {
        Vector<MidiPart> parts = new Vector<MidiPart>();
        for (RecordableLane lane : recordingLanes) {
            Part part;
            if (lane instanceof MidiLane) {
                part = new MidiPart((MidiLane) lane);
                parts.add((MidiPart) part);
            }
            if (lane instanceof AudioLane) {
                try {
                    throw new Exception(" CAN NOT DEPLOY AUDIO YET");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        for (Integer takeNo : takeNumbers) {
            for (MultiEvent event : recordingTakes.get(takeNo)) {
                for (MidiPart part : parts) {
                    try {
                        part.add((MultiEvent) event.clone());
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        for (MidiPart part : parts) part.setBoundsFromEvents();
        recordingTakes.clear();
        if (recordingTakeDialog != null) {
            recordingTakeDialog.dispose();
            recordingTakeDialog = null;
        }
    }

    /**
     * Return the MultiEvents of a recording take
     * @param takeNo
     * @return
     */
    public Vector<MultiEvent> getRecordingTake(int takeNo) {
        return recordingTakes.get(takeNo);
    }

    public void startRecording() {
        pendingNoteEvents.clear();
        lastLoopCount = 0;
        recording = true;
        start();
    }

    public void stopRecording() {
        stop();
    }

    public boolean isRecording() {
        return recording;
    }

    /**
     * @deprecated Use recordEnable on MidiLane instead
     */
    public void recordEnable(Track track, int channel) {
    }

    public void recordEnable(RecordableLane lane) {
        recordingLanes.add(lane);
    }

    /**
     * @deprecated Use recordEnable on MidiLane instead
     */
    public void recordDisable(Track track) {
    }

    public boolean isRecording(RecordableLane lane) {
        return recordingLanes.contains(lane);
    }

    public void recordDisable(RecordableLane lane) {
        recordingLanes.remove(lane);
    }

    public float getTempoInBPM() {
        return bpm;
    }

    public void setTempoInBPM(float bpm) {
        this.bpm = bpm;
        notifyTempoChangeListeners();
    }

    public float getTempoInMPQ() {
        return 0;
    }

    public void setTempoInMPQ(float mpq) {
    }

    public void setTempoFactor(float factor) {
    }

    public float getTempoFactor() {
        return 0;
    }

    public long getTickLength() {
        return 0;
    }

    public long getTicksLooped() {
        return player.getTicksLooped();
    }

    public long getTickPosition() {
        return player.getTickPosition();
    }

    public void setTickPosition(long tick) {
        player.setTickPosition(tick);
    }

    public long getMicrosecondLength() {
        return 0;
    }

    public long getMicrosecondPosition() {
        return player.getMicroSecondPosition();
    }

    public void setMicrosecondPosition(long microseconds) {
    }

    public void setMasterSyncMode(SyncMode sync) {
    }

    public SyncMode getMasterSyncMode() {
        return null;
    }

    public SyncMode[] getMasterSyncModes() {
        return null;
    }

    public void setSlaveSyncMode(SyncMode sync) {
    }

    public SyncMode getSlaveSyncMode() {
        return null;
    }

    public SyncMode[] getSlaveSyncModes() {
        return null;
    }

    public void setTrackMute(int track, boolean mute) {
    }

    public boolean getTrackMute(int track) {
        return false;
    }

    public void setTrackSolo(int track, boolean solo) {
    }

    public boolean getTrackSolo(int track) {
        return false;
    }

    public boolean addMetaEventListener(MetaEventListener listener) {
        return false;
    }

    public void removeMetaEventListener(MetaEventListener listener) {
    }

    public int[] addControllerEventListener(ControllerEventListener listener, int[] controllers) {
        return null;
    }

    public int[] removeControllerEventListener(ControllerEventListener listener, int[] controllers) {
        return null;
    }

    public void setLoopStartPoint(long tick) {
        this.loopStartPoint = tick;
        notifySongPositionListeners(player.getTickPosition());
    }

    public long getLoopStartPoint() {
        return loopStartPoint;
    }

    public void setLoopEndPoint(long tick) {
        this.loopEndPoint = tick;
        notifySongPositionListeners(player.getTickPosition());
    }

    public long getLoopEndPoint() {
        return loopEndPoint;
    }

    public void setLoopCount(int count) {
        this.loopCount = count;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public Info getDeviceInfo() {
        return null;
    }

    public void open() throws MidiUnavailableException {
    }

    public void close() {
        stop();
    }

    public boolean isOpen() {
        return false;
    }

    public int getMaxReceivers() {
        return 0;
    }

    public int getMaxTransmitters() {
        return 0;
    }

    public Receiver getReceiver() throws MidiUnavailableException {
        return receiver;
    }

    public List<Receiver> getReceivers() {
        return receivers;
    }

    public Transmitter getTransmitter() throws MidiUnavailableException {
        return transmitter;
    }

    public List<Transmitter> getTransmitters() {
        return transmitters;
    }

    /**
     * Register a MidiOutDevice to this sequencer. It's also added to the list of transmitters.
     * @param midiDevice
     * @throws MidiUnavailableException
     */
    public void addMidiOutDevice(final MidiDevice midiDevice) throws MidiUnavailableException {
        Transmitter transmitter = new Transmitter() {

            Receiver receiver = midiDevice.getReceiver();

            public void setReceiver(Receiver receiver) {
                this.receiver = receiver;
            }

            public Receiver getReceiver() {
                return receiver;
            }

            public void close() {
            }
        };
        midiOutDeviceTransmitters.put(midiDevice, transmitter);
        midiOutDevices.add(midiDevice);
        transmitters.add(transmitter);
    }

    /**
     * Deregister a midi out device from this sequencer. Also removed from the list of transmitters
     * @param midiDevice
     */
    public void removeMidiOutDevice(MidiDevice midiDevice) {
        transmitters.remove(midiOutDeviceTransmitters.get(midiDevice));
        midiOutDeviceTransmitters.remove(midiDevice);
        midiOutDevices.remove(midiDevice);
    }

    public List<MidiDevice> listMidiOutDevices() {
        return midiOutDevices;
    }

    public void addSequencerListener(SequencerListener sequencerListener) {
        sequencerListeners.add(sequencerListener);
    }

    public void removeSequencerListener(SequencerListener sequencerListener) {
        sequencerListeners.remove(sequencerListener);
    }

    /**
     * Add a song position listener to the sequencer. 
     * See the SongPositionListener javadoc.
     * @param songPositionListener
     */
    public void addSongPositionListener(SongPositionListener songPositionListener) {
        if (songPositionListener.requiresNotificationOnEachTick()) intenseSongPositionListeners.add(songPositionListener); else songPositionListeners.add(songPositionListener);
    }

    /**
     * Remove a song position listener from the sequencer
     * @param songPositionListener
     */
    public void removeSongPositionListener(SongPositionListener songPositionListener) {
        if (songPositionListener.requiresNotificationOnEachTick()) intenseSongPositionListeners.remove(songPositionListener); else songPositionListeners.remove(songPositionListener);
    }

    public final void notifySongPositionListeners() {
        notifySongPositionListeners(getTickPosition());
    }

    /**
     * Add a tempo change listener to the sequencer. 
     * This is notified when we play a tempo change event.
     * @param listener
     */
    public void addTempoChangeListener(TempoChangeListener listener) {
        tempoChangeListeners.add(listener);
    }

    /**
     * Remove a tempo change position listener from the sequencer
     * @param listener
     */
    public void removeTempoChangeListener(TempoChangeListener listener) {
        tempoChangeListeners.remove(listener);
    }

    final void notifyTempoChangeListeners() {
        for (TempoChangeListener listener : tempoChangeListeners) listener.notifyTempoChange(bpm);
    }

    /**
     * Notify songPositionListeners that requires to be notified on each tick
     * @param tick
     */
    final void notifyIntenseSongPositionListeners(long tick) {
        for (SongPositionListener listener : intenseSongPositionListeners) listener.notifyTickPosition(tick);
    }

    final void notifyAllSongPositionListeners(long tick) {
        notifyIntenseSongPositionListeners(tick);
        notifySongPositionListeners(tick);
    }

    final void notifySongPositionListeners(long tick) {
        for (SongPositionListener listener : songPositionListeners) listener.notifyTickPosition(tick);
    }

    public Collection<FrinikaTrackWrapper> getSoloFrinikaTrackWrappers() {
        return soloFrinikaTrackWrappers;
    }

    public void setSolo(MidiLane lane, boolean solo) {
        if (solo) soloFrinikaTrackWrappers.add(lane.getTrack()); else soloFrinikaTrackWrappers.remove(lane.getTrack());
    }

    public void setRecordingTakeDialog(RecordingDialog dialog) {
        this.recordingTakeDialog = dialog;
    }

    /**
     * Send noteOff and reset all controllers to zero
     *
     */
    public void panic() {
        player.notesOff(true);
        byte[] data = { (byte) 0xF0, 0x43, 0x10, 0x4C, 0x00, 0x00, 0x7F, 0x00, (byte) 0xF7 };
        SysexMessage mess = new SysexMessage();
        try {
            mess.setMessage(data, data.length);
        } catch (InvalidMidiDataException e1) {
            e1.printStackTrace();
        }
        for (MidiDevice midiDev : listMidiOutDevices()) {
            if (midiDev instanceof SynthRack) {
            } else {
                try {
                    midiDev.getReceiver().send(mess, -1);
                } catch (MidiUnavailableException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public boolean isSolo(RecordableLane lane) {
        return soloFrinikaTrackWrappers.contains(((MidiLane) lane).getTrack());
    }

    public MidiPlayOptions getPlayOptions(FrinikaTrackWrapper track) {
        MidiPlayOptions opt = frinikaTrackWrappersMidiPlayOptions.get(track);
        if (opt == null) return new MidiPlayOptions();
        return opt;
    }

    public void setPlayOptions(FrinikaTrackWrapper track, MidiPlayOptions opt) {
        frinikaTrackWrappersMidiPlayOptions.put(track, opt);
    }

    public void addMidiMessageListener(MidiMessageListener l) {
        getMidiMessageListeners().add(l);
    }

    public void removeMidiMessageListener(MidiMessageListener l) {
        getMidiMessageListeners().remove(l);
    }

    /**
     * Manually send a midi message using the channel/device settings of a FrinikaTrackWrapper
     * This is also used by wavexport
     * @param msg
     * @param trackWrapper
     * @throws InvalidMidiDataException
     * @throws MidiUnavailableException
     */
    public void sendMidiMessage(MidiMessage msg, FrinikaTrackWrapper trackWrapper) throws InvalidMidiDataException, MidiUnavailableException {
        player.sendMidiMessage(msg, trackWrapper);
    }

    /**
	 * Set whether to play in realtime or if rendering (e.g. export wav)
	 * @param realtime
	 */
    public void setRealtime(boolean realtime) {
        player.setRealtime(realtime);
    }

    /**
	 * Returns whether to play in realtime or if rendering (e.g. export wav)
	 */
    public boolean getRealtime() {
        return player.getRealtime();
    }

    /**
	 * If the player is not in realtime mode you can manually trigger the next tick here
	 *
	 */
    public void nonRealtimeNextTick() {
        if (!player.getRealtime()) player.timerEvent();
    }

    /**
	 * set the tempolist 
	 * 
	 * @param tl
	 */
    public void setTempoList(TempoList tl) {
        player.setTempoList(tl);
    }

    /**
	 * 
	 * set priority of the player
	 * 
	 */
    public void setPlayerPriority(int prio) {
        player.priorityRequested = prio;
    }

    public Collection<MidiMessageListener> getMidiMessageListeners() {
        return midiMessageListeners;
    }
}
