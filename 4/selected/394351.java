package com.okythoos.vmidi;

/**
 * Contains an array of midi events to be sequenced for playback
 * <p>
 * This class is to be used only internally by the sequencer
 *
 * * @see         VMidiSequencer
 */
public class VMidiTrack {

    /**
     * MIDI chunkid
     */
    protected byte[] chunkID;

    /**
     * Track Name
     */
    protected String trackName = null;

    /**
     * Set the track's name
     * @param trackName the track's name
     */
    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    /**
     * Track's size in bytes
     */
    protected long chunkSize;

    /**
     * Track's size in events
     */
    protected int TrackSize;

    /**
     * True if track contains NOTE_ON/OFF events
     */
    protected boolean playable = false;

    /**
     * Total duration in ticks
     */
    protected long totalTicks = 0;

    /**
     * MidiEvents
     * @see VMidiEvent
     */
    protected VMidiEvent[] midievents;

    /**
     * Program/Patch number
     */
    protected final int programNum;

    /**
     * MIDI channel number used (usually same within track)
     */
    protected final int channelNum;

    /**
     * Constructs track object(used by the parser only)
     * @see VMidiParser
     */
    VMidiTrack(byte[] chunkID, String trackName, long chunkSize, VMidiEvent[] midievents, int eventNum, boolean playable, long totalTicks, int programNum, int channelNum) {
        this.chunkID = chunkID;
        this.trackName = trackName;
        this.chunkSize = chunkSize;
        this.midievents = midievents;
        this.setTrackSize(eventNum);
        this.playable = playable;
        this.totalTicks = totalTicks;
        this.programNum = programNum;
        this.channelNum = channelNum;
    }

    /**
     * Returns the channel number
     * @return the channel number
     */
    public int getChannelNum() {
        return channelNum;
    }

    /**
     * Returns the program's number
     * @return the program/patch's number
     */
    public int getProgramNum() {
        return programNum;
    }

    /**
     * Returns true if track is playable
     * @return true if track is playable (contains NOTE_ON/OFF events)
     */
    public boolean isPlayable() {
        return playable;
    }

    /**
     * Set true for a playable track (if it contains NOTE_ON/OFF, only for parser use)
     * @param playable true for a playable track (if it contains NOTE_ON/OFF, only for parser use)
     */
    public void setPlayable(boolean playable) {
        this.playable = playable;
    }

    /**
     * Returns the total duration of the track in ticks
     * @return total duration of the track in ticks
     */
    public long getTotalTicks() {
        return totalTicks;
    }

    /**
     * Set the total duration of the track in ticks
     * @param totalTicks total duration of the track in ticks
     */
    public void setTotalTicks(long totalTicks) {
        this.totalTicks = totalTicks;
    }

    /**
     * Set the chunkID
     * @param chunkID the chunkid of the track
     */
    public void setChunkID(byte[] chunkID) {
        this.chunkID = chunkID;
    }

    /**
     * Returns the chunkID
     * @return the chunkid of the track
     */
    public byte[] getChunkID() {
        return chunkID;
    }

    /**
     * Set the midi events within a track
     * @param midievents array with MIDI events
     */
    public void setMidievents(VMidiEvent[] midievents) {
        this.midievents = midievents;
    }

    /**
     * Returns the midi events within a track
     * @return array with MIDI events
     */
    public VMidiEvent[] getMidievents() {
        return midievents;
    }

    /**
     * Set the chunk size
     * @param chunkSize the size of the track in bytes
     */
    public void setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
    }

    /**
     * Returns the chunk size
     * @return the size of the track in bytes
     */
    public long getChunkSize() {
        return chunkSize;
    }

    /**
     * Set the track's size in events
     * @param trackSize the size of the track in events
     */
    public void setTrackSize(int trackSize) {
        TrackSize = trackSize;
    }

    /**
     * Return the track's size in events
     * @return trackSize the size of the track in events
     */
    public int getTrackSize() {
        return TrackSize;
    }

    /**
     * Return the track's name
     * @return the track's name
     */
    public String getTrackName() {
        return this.trackName;
    }
}
