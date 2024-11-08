package com.chromamorph.notes;

import com.chromamorph.pitch.Pitch;

public class Note implements Comparable<Note> {

    private Long onset = null;

    private Long duration = null;

    private Pitch pitch = new Pitch();

    private Integer staff = null;

    private Integer channel = null;

    private Integer voice = null;

    private Notes notes = null;

    private Pitch computedPitch = new Pitch();

    public static Integer STAFF = null;

    public static Long ONSET = null;

    public static Long DURATION = null;

    public static Integer VOICE = null;

    public static String PITCH_NAME = null;

    public static Long INTER_ONSET = null;

    public static boolean CHORD = false;

    public Pitch getComputedPitch() {
        return computedPitch;
    }

    public void setPitch(Pitch pitch) {
        this.pitch = pitch;
    }

    public Note(Notes notes) {
        onset = ONSET;
        duration = DURATION;
        if (INTER_ONSET != null && CHORD == false) ONSET += INTER_ONSET; else if (CHORD == false) ONSET += DURATION;
        pitch.setPitchName(PITCH_NAME);
        staff = STAFF;
        voice = VOICE;
        setNotes(notes);
    }

    public Note(long onset, int midiNoteNumber, long duration, int channel, Notes notes) throws IllegalArgumentException {
        setOnset(onset);
        pitch.setMIDINoteNumber(midiNoteNumber);
        setChannel(channel);
        setDuration(duration);
        setNotes(notes);
    }

    /**
	 * Assumes l has form: Staff PitchName Onset Offset Voice
	 * @param l
	 */
    public Note(String l, Notes notes) {
        String[] array = l.split(" ");
        staff = Integer.parseInt(array[0]);
        String pitchName = array[1];
        pitch.setPitchName(pitchName);
        onset = Long.parseLong(array[2]);
        duration = Long.parseLong(array[3]) - onset;
        if (array.length > 4) voice = Integer.parseInt(array[4]);
        setNotes(notes);
    }

    public Pitch getPitch() {
        return pitch;
    }

    public String getPitchName() {
        return pitch.getPitchName();
    }

    public void setPitchName(String pitchName) {
        pitch.setPitchName(pitchName);
    }

    public Integer getStaff() {
        return staff;
    }

    public void setStaff(Integer staff) {
        this.staff = staff;
    }

    public Long getOnset() {
        return onset;
    }

    public Long getOffset() {
        return onset + duration;
    }

    private void setOnset(Long onset) {
        this.onset = onset;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Integer getMidiNoteNumber() {
        return pitch.getMIDINoteNumber();
    }

    public void setMidiNoteNumber(Integer midiNoteNumber) {
        pitch.setMIDINoteNumber(midiNoteNumber);
    }

    public Integer getChannel() {
        return channel;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    public Integer getVoice() {
        return voice;
    }

    public void setVoice(Integer voice) {
        this.voice = voice;
    }

    public void setNotes(Notes notes) {
        this.notes = notes;
    }

    public Notes getNotes() {
        return notes;
    }

    public Integer getMetricLevel() {
        return getNotes().getMetricLevel(getOnset());
    }

    public int compareTo(Note n) {
        if (equals(n)) return 0;
        if (getOnset() - n.getOnset() != 0l) return (getOnset() - n.getOnset()) > 0l ? 1 : -1;
        if (getPitch().compareTo(n.getPitch()) != 0) return getPitch().compareTo(n.getPitch());
        if ((getDuration() != null) && (n.getDuration() != null) && (getDuration() - n.getDuration() != 0l)) return (getDuration() - n.getDuration()) > 0l ? 1 : -1;
        if ((getChannel() != null) && (n.getChannel() != null) && (getChannel() - n.getChannel() != 0)) return getChannel() - n.getChannel();
        if (getVoice() - n.getVoice() != 0) return (getVoice() - n.getVoice()) > 0 ? 1 : -1;
        if ((getStaff() != null) && (n.getStaff() != null) && (getStaff() - n.getStaff() != 0)) return getStaff() - n.getStaff();
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Note)) return false; else {
            Note n = (Note) obj;
            return (getOnset() == n.getOnset() && getPitch().equals(n.getPitch()) && getChannel() == n.getChannel() && getStaff() == n.getStaff() && getDuration() == n.getDuration()) && getVoice() == n.getVoice();
        }
    }

    @Override
    public String toString() {
        return "Note [channel=" + channel + ", duration=" + duration + ", onset=" + onset + ", pitch=" + pitch.getPitchName() + ", staff=" + staff + ", voice=" + voice + ", metricLevel = " + getMetricLevel() + "]";
    }
}
