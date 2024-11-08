package com.chromamorph.notes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import com.chromamorph.maths.Maths;
import com.chromamorph.pitch.Chromagram;

/**
 * The Notes class is a container for a set of Note objects.
 * It also contains variables storing the tatums per crotchet
 * and the time signatures.
 * 
 * @author David Meredith
 *
 */
public class Notes {

    private TreeSet<Note> notes = null;

    private Long tatumsPerCrotchet = null;

    private TreeSet<TimeSignature> timeSignatures = null;

    public Notes(File notesFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(notesFile));
        notes = new TreeSet<Note>();
        for (String l = br.readLine(); l != null; l = br.readLine()) {
            if (!l.startsWith("//") && l.length() > 0) {
                if (l.startsWith("tatumsPerCrotchet")) {
                    tatumsPerCrotchet = Long.parseLong(l.split(" ")[1]);
                } else if (Character.isDigit(l.charAt(0))) {
                    notes.add(new Note(l, this));
                } else if (l.length() >= 3 && l.startsWith("set")) {
                    String[] setArray = l.split(" ");
                    String name = null;
                    for (int i = 1; i < setArray.length; i += 2) {
                        name = setArray[i];
                        if (name.equals("st")) Note.STAFF = Integer.parseInt(setArray[i + 1]);
                        if (name.equals("on")) Note.ONSET = Long.parseLong(setArray[i + 1]);
                        if (name.equals("du")) Note.DURATION = Long.parseLong(setArray[i + 1]);
                        if (name.equals("vo")) Note.VOICE = Integer.parseInt(setArray[i + 1]);
                        if (name.equals("io")) {
                            String val = setArray[i + 1];
                            if (val.equals("null")) Note.INTER_ONSET = null; else Note.INTER_ONSET = Long.parseLong(setArray[i + 1]);
                        }
                    }
                } else if (l.startsWith("ts")) {
                    String[] tsArray = l.split(" ");
                    if (tsArray.length == 5) {
                        Long onset = Long.parseLong(tsArray[1]);
                        Long offset = Long.parseLong(tsArray[2]);
                        Integer numerator = Integer.parseInt(tsArray[3]);
                        Integer denominator = Integer.parseInt(tsArray[4]);
                        if (timeSignatures == null) timeSignatures = new TreeSet<TimeSignature>();
                        timeSignatures.add(new TimeSignature(onset, offset, numerator, denominator));
                    }
                } else if (l.startsWith("ch ")) {
                    Note.CHORD = true;
                    String[] chordArray = l.split(" ");
                    for (int i = 1; i < chordArray.length; i++) {
                        if (i == chordArray.length - 1) Note.CHORD = false;
                        Note.PITCH_NAME = chordArray[i];
                        notes.add(new Note(this));
                    }
                } else {
                    String[] pnArray = l.split(" ");
                    for (String pn : pnArray) {
                        Note.PITCH_NAME = pn;
                        notes.add(new Note(this));
                    }
                }
            }
        }
        br.close();
    }

    /**
	 * Constructs a Notes object from a MIDI file whose name is given as 
	 * an argument
	 * @throws IOException 
	 * @throws InvalidMidiDataException 
	 */
    public Notes(Sequence sequence) throws InvalidMidiDataException, IOException {
        Track[] tracks = sequence.getTracks();
        for (Track track : tracks) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent midiEvent = track.get(i);
                if (midiEvent.getMessage() instanceof ShortMessage) {
                    ShortMessage shortMessage = (ShortMessage) midiEvent.getMessage();
                    if (shortMessage.getCommand() == ShortMessage.NOTE_ON && shortMessage.getData2() != 0) {
                        long onset = midiEvent.getTick();
                        int midiNoteNumber = shortMessage.getData1();
                        int channel = shortMessage.getChannel();
                        long duration = 0;
                        boolean found = false;
                        for (int j = i + 1; j < track.size() && !found; j++) {
                            MidiEvent midiEvent2 = track.get(j);
                            if (midiEvent2.getMessage() instanceof ShortMessage) {
                                ShortMessage shortMessage2 = (ShortMessage) midiEvent2.getMessage();
                                if (shortMessage2.getChannel() == channel && (shortMessage2.getData1() == midiNoteNumber) && (shortMessage2.getCommand() == ShortMessage.NOTE_OFF || (shortMessage2.getCommand() == ShortMessage.NOTE_ON && shortMessage2.getData2() == 0))) {
                                    duration = midiEvent2.getTick() - onset;
                                    found = true;
                                }
                            }
                        }
                        if (!found) System.out.println("WARNING! Failed to find duration of MIDI event: onset = " + onset + ", note number = " + midiNoteNumber + ", channel = " + channel);
                        addNote(new Note(onset, midiNoteNumber, duration, channel, this));
                    }
                }
            }
        }
    }

    public void addNote(Note note) {
        if (notes == null) notes = new TreeSet<Note>();
        notes.add(note);
    }

    public TreeSet<Note> getNotes() {
        return notes;
    }

    public Long getTatumsPerCrotchet() {
        return tatumsPerCrotchet;
    }

    public void setTatumsPerCrotchet(Long tatumsPerCrotchet) {
        this.tatumsPerCrotchet = tatumsPerCrotchet;
    }

    public TreeSet<TimeSignature> getTimeSignatures() {
        return timeSignatures;
    }

    public void setTimeSignatures(TreeSet<TimeSignature> timeSignatures) {
        this.timeSignatures = timeSignatures;
    }

    public void play(int tatumsPerBeat, float beatsPerMinute) throws InvalidMidiDataException, MidiUnavailableException {
        this.play(tatumsPerBeat, beatsPerMinute, 0L);
    }

    public void play(Integer tatumsPerBeat, Float beatsPerMinute, Long segmentStart) throws InvalidMidiDataException, MidiUnavailableException {
        this.play(tatumsPerBeat, beatsPerMinute, segmentStart, null, null);
    }

    public void play(Integer tatumsPerBeat, Float beatsPerMinute, Long segmentStart, Long segmentEnd) throws InvalidMidiDataException, MidiUnavailableException {
        this.play(tatumsPerBeat, beatsPerMinute, segmentStart, segmentEnd, null);
    }

    public void play(Integer tatumsPerBeat, Float beatsPerMinute, Integer lowestMetricLevel) throws InvalidMidiDataException, MidiUnavailableException {
        this.play(tatumsPerBeat, beatsPerMinute, null, null, lowestMetricLevel);
    }

    public void play(Integer tatumsPerBeat, Float beatsPerMinute, Long segmentStart, Long segmentEnd, Integer lowestMetricLevel) throws InvalidMidiDataException, MidiUnavailableException {
        if (segmentStart == null) segmentStart = 0l;
        int ticksPerBeat = 144;
        int ticksPerTatum = ticksPerBeat / tatumsPerBeat;
        Sequence sequence = new Sequence(Sequence.PPQ, ticksPerBeat, 1);
        Track track = sequence.getTracks()[0];
        long finalOffsetTime = 0;
        for (Note note : notes) {
            if ((segmentStart == null || note.getOnset() >= segmentStart) && (segmentEnd == null || note.getOnset() < segmentEnd) && (lowestMetricLevel == null || note.getMetricLevel().compareTo(lowestMetricLevel) <= 0)) {
                ShortMessage onMessage = new ShortMessage();
                onMessage.setMessage(ShortMessage.NOTE_ON, note.getMidiNoteNumber(), 96);
                ShortMessage offMessage = new ShortMessage();
                offMessage.setMessage(ShortMessage.NOTE_ON, note.getMidiNoteNumber(), 0);
                long onsetTime = (note.getOnset() - segmentStart) * ticksPerTatum;
                long offsetTime = (note.getOffset() - segmentStart) * ticksPerTatum;
                track.add(new MidiEvent(onMessage, onsetTime));
                track.add(new MidiEvent(offMessage, offsetTime));
                if (offsetTime > finalOffsetTime) finalOffsetTime = offsetTime;
            }
        }
        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequencer.setTempoInBPM(beatsPerMinute);
        Synthesizer synth = MidiSystem.getSynthesizer();
        synth.open();
        sequencer.setSequence(sequence);
        sequencer.start();
        try {
            double ticksPerMinute = ticksPerBeat * beatsPerMinute;
            long msPerTick = 1 + (long) (60000.0 / ticksPerMinute);
            long finalOffsetTimeInMS = finalOffsetTime * msPerTick;
            Thread.sleep(finalOffsetTimeInMS + 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sequencer.stop();
        synth.close();
        sequencer.close();
    }

    public TimeSignature getTimeSignature(Long timePoint) {
        if (timePoint.equals(getMaxTimePoint())) return getTimeSignatures().last();
        for (TimeSignature ts : getTimeSignatures()) {
            if (ts.getOnset().compareTo(timePoint) <= 0 && ts.getOffset().compareTo(timePoint) > 0) return ts;
        }
        return null;
    }

    public Long getMaxTimePoint() {
        Long maxTimePoint = 0l;
        for (Note note : notes) {
            if (note.getOffset().compareTo(maxTimePoint) > 0) maxTimePoint = note.getOffset();
        }
        return maxTimePoint;
    }

    public Integer getMetricLevel(Long timePoint) {
        TimeSignature ts = getTimeSignature(timePoint);
        Long barLength = (long) (getTatumsPerCrotchet() * ts.getCrotchetsPerBar());
        Long posWithinBar = Maths.mod(timePoint - ts.getOnset(), barLength);
        ArrayList<Integer> divisionArray = Maths.factorize(ts.getNumerator());
        int divisionArrayIndex = divisionArray.size();
        Long thisPos = 0l;
        Integer metricLevel = 0;
        Long interBeatIntervalInTatums = barLength;
        int divisor;
        while (!interBeatIntervalInTatums.equals(0l)) {
            metricLevel++;
            divisionArrayIndex--;
            if (divisionArrayIndex < 0) divisor = 2; else divisor = divisionArray.get(divisionArrayIndex);
            interBeatIntervalInTatums /= divisor;
            if (!interBeatIntervalInTatums.equals(0l)) {
                thisPos = 0l;
                while (thisPos.compareTo(barLength) <= 0) {
                    if (thisPos.equals(posWithinBar)) return metricLevel;
                    thisPos += interBeatIntervalInTatums;
                }
            }
        }
        return null;
    }

    public Integer getMaxMetricLevel() {
        Integer thisMetricLevel = 0;
        Integer maxMetricLevel = 0;
        for (Note note : notes) {
            thisMetricLevel = getMetricLevel(note.getOnset());
            if (thisMetricLevel > maxMetricLevel) maxMetricLevel = thisMetricLevel;
            thisMetricLevel = getMetricLevel(note.getOffset());
            if (thisMetricLevel > maxMetricLevel) maxMetricLevel = thisMetricLevel;
        }
        return maxMetricLevel;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("");
        for (TimeSignature ts : getTimeSignatures()) sb.append("\n    " + ts);
        String timeSignaturesString = sb.toString();
        sb = new StringBuilder("");
        for (Note note : getNotes()) sb.append("\n    " + note);
        String notesString = sb.toString();
        return "Notes" + "\n  tatumsPerCrotchet\n    " + tatumsPerCrotchet + "\n  timeSignatures" + timeSignaturesString + "\n  notes" + notesString;
    }

    public ArrayList<Long> getNoteOnsets() {
        TreeSet<Long> onsets = new TreeSet<Long>();
        for (Note note : notes) {
            onsets.add(note.getOnset());
        }
        return new ArrayList<Long>(onsets);
    }

    /**
	 * Computes a pitch name for each note in this Notes object. Uses
	 * the PS13s1 algorithm with the precontext and postcontext set to 
	 * kPre and kPost, respectively.
	 * 
	 * @param kPre
	 * @param kPost
	 */
    public void pitchSpell(int kPre, int kPost) {
        ArrayList<Note> noteArray = new ArrayList<Note>(notes);
        ArrayList<Chromagram> chromagramList = new ArrayList<Chromagram>();
        for (int i = 0; i < noteArray.size(); i++) {
            Chromagram chromagram = new Chromagram();
            for (int j = i - kPre; j < i + kPost; j++) {
                if (j >= 0 && j < noteArray.size()) {
                    int chroma = noteArray.get(j).getPitch().getChroma();
                    chromagram.addOneToChromaFrequency(chroma);
                }
            }
            chromagramList.add(chromagram);
        }
        int[] morphInt = { 0, 1, 1, 2, 2, 3, 3, 4, 5, 5, 6, 6 };
        int[] initMorph = { 0, 1, 1, 2, 2, 3, 4, 4, 5, 5, 6, 6 };
        int c0 = noteArray.get(0).getPitch().getChroma();
        int m0 = initMorph[c0];
        Integer[] tonicMorphForTonicChroma = { null, null, null, null, null, null, null, null, null, null, null, null };
        for (int ct = 0; ct < 12; ct++) tonicMorphForTonicChroma[ct] = Maths.mod(m0 - morphInt[Maths.mod(c0 - ct, 12)], 7);
        Integer[] morphForTonicChroma = { null, null, null, null, null, null, null, null, null, null, null, null };
        ArrayList<TreeSet<Integer>> tonicChromaSetForMorph = null;
        Integer[] morphStrength = { null, null, null, null, null, null, null };
        for (int j = 0; j < noteArray.size(); j++) {
            for (int ct = 0; ct < 12; ct++) {
                int c = noteArray.get(j).getPitch().getChroma();
                morphForTonicChroma[ct] = Maths.mod(morphInt[Maths.mod(c - ct, 12)] + tonicMorphForTonicChroma[ct], 7);
            }
            tonicChromaSetForMorph = new ArrayList<TreeSet<Integer>>();
            for (int m = 0; m < 7; m++) tonicChromaSetForMorph.add(new TreeSet<Integer>());
            for (int m = 0; m < 7; m++) for (int ct = 0; ct < 12; ct++) if (morphForTonicChroma[ct] == m) tonicChromaSetForMorph.get(m).add(ct);
            for (int m = 0; m < 7; m++) {
                int thisMorphStrength = 0;
                for (Integer tonicChroma : tonicChromaSetForMorph.get(m)) thisMorphStrength += chromagramList.get(j).get(tonicChroma);
                morphStrength[m] = thisMorphStrength;
            }
            int maxStrengthMorph = 0;
            int maxMorphStrength = morphStrength[0];
            for (int m = 1; m < 7; m++) if (morphStrength[m] > maxMorphStrength) {
                maxStrengthMorph = m;
                maxMorphStrength = morphStrength[m];
            }
            noteArray.get(j).getComputedPitch().setMorph(maxStrengthMorph);
        }
        for (int i = 0; i < noteArray.size(); i++) {
            int chromaticPitch = noteArray.get(i).getPitch().getChromaticPitch();
            int morph = noteArray.get(i).getComputedPitch().getMorph();
            int morphOct1 = Maths.floor(chromaticPitch, 12);
            int morphOct2 = morphOct1 + 1;
            int morphOct3 = morphOct1 - 1;
            float mp1 = morphOct1 + morph / 7.0f;
            float mp2 = morphOct2 + morph / 7.0f;
            float mp3 = morphOct3 + morph / 7.0f;
            int chroma = Maths.mod(chromaticPitch, 12);
            float cp = morphOct1 + chroma / 12.0f;
            float[] diffList = { Math.abs(cp - mp1), Math.abs(cp - mp2), Math.abs(cp - mp3) };
            int[] morphOctList = { morphOct1, morphOct2, morphOct3 };
            int bestMorphOctIndex = 0;
            for (int j = 1; j < 3; j++) if (diffList[j] < diffList[bestMorphOctIndex]) bestMorphOctIndex = j;
            int bestMorphOct = morphOctList[bestMorphOctIndex];
            int bestMorpheticPitch = morph + 7 * bestMorphOct;
            noteArray.get(i).getComputedPitch().setMorpheticPitch(bestMorpheticPitch);
        }
        for (int i = 0; i < noteArray.size(); i++) {
            int chromaticPitch = noteArray.get(i).getPitch().getChromaticPitch();
            int morpheticPitch = noteArray.get(i).getComputedPitch().getMorpheticPitch();
            noteArray.get(i).getComputedPitch().setChromamorpheticPitch(chromaticPitch, morpheticPitch);
        }
        int i = 0;
        for (Note note : notes) {
            note.getComputedPitch().setPitchName(noteArray.get(i).getComputedPitch().getPitchName());
            i++;
        }
    }

    public int getPitchSpellingNoteErrorCount() {
        int errorCount = 0;
        for (Note note : getNotes()) {
            if (!note.getComputedPitch().getPitchName().equals(note.getPitchName())) errorCount++;
        }
        return errorCount;
    }

    public double getPitchSpellingNoteAccuracy() {
        return 100.0 * (1.0 - 1.0 * getPitchSpellingNoteErrorCount() / notes.size());
    }
}
