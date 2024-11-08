package musicsequencer.designer;

import java.io.Serializable;

/**
 * Stores the state of an instrument, i.e. the <code>AudioData</code> and <code>RhythmTrack</code>.
 *
 *  @author Music Sequencer Group
 */
public class InstrumentState implements Serializable {

    private static final long serialVersionUID = InstrumentState.class.getName().hashCode();

    private AudioData audioData;

    private RhythmTrack rhythmTrack;

    /**
     * Creates a blank/empty InstrumentState
     */
    public InstrumentState() {
        audioData = new AudioData(null);
        rhythmTrack = new RhythmTrack();
    }

    /**
     * gets the name of the instrument state
     * @return string of the instrument state name
     */
    public String getName() {
        return audioData.getName();
    }

    /**
     * gets the AudioData for the instrument state
     * @return the AudioData object of a instrument state
     */
    public AudioData getAudioData() {
        return audioData;
    }

    /**
     * sets the AudioData for the instrument state
     * @param inputAudioData the AudioData for the instrument state
     */
    public void setAudioData(AudioData inputAudioData) {
        audioData = inputAudioData;
    }

    /**
     * @return the RhythmTrack of the the InstrumentState
     */
    public RhythmTrack getRhythmTrack() {
        return rhythmTrack;
    }

    /**
     * @param rhythmTrack the RhythmTrack you want the InstrumentState to use
     */
    public void setRhythmTrack(RhythmTrack rhythmTrack) {
        this.rhythmTrack = rhythmTrack;
    }

    /**
     * gets the desired RyhthmEvent for the instrument state
     * @param i an index for the RhythmEvent
     * @return a RhythmEvent object
     */
    public RhythmEvent getRhythmEvent(int i) {
        return rhythmTrack.getEvent(i);
    }

    private boolean outOfRange(int position, int rhythmplace, boolean loop, int num16ths) {
        if (rhythmTrack.getEvent(position).isReverseCued()) {
            boolean result = (rhythmplace >= position) || (rhythmplace < position - num16ths);
            return result;
        }
        return (rhythmplace < position && !loop) || (rhythmplace >= position + num16ths);
    }

    private boolean nothingWorthPlaying(int index) {
        return (rhythmTrack.getEvent(index).getVolume() == 0.0f) || (audioData == null);
    }

    private int correctForWrapAround(int position, int rhythmplace, boolean loop) {
        if (rhythmTrack.getEvent(position).isReverseCued()) {
            if (rhythmplace >= position) {
                rhythmplace -= RiffEngine.BEAT_COUNT;
            }
        } else if (rhythmplace < position && loop) {
            rhythmplace += RiffEngine.BEAT_COUNT;
        }
        return rhythmplace;
    }

    /**
     * Calculates a 16th of audio data, and adds it to the output
     *  When the <code>loop</code> parameter is false, the wraparound of samples
     * starting after the specified position is not included.
     *
     * @param rhythmposition    the beat position of the data to return
     * @param loop              whether wraparound should be included
     * @param output            The output array to add to
     */
    public void add16thOfFloatData(int rhythmposition, boolean loop, float[] output) {
        add16thOfFloatData(rhythmposition, 1, loop, output);
    }

    private void addData(int position, int rhythmplace, float volume, boolean loop, float[] output) {
        if (nothingWorthPlaying(position)) return;
        if (rhythmTrack.getEvent(position).isReverseCued()) rhythmplace -= 1;
        int num16ths = (int) Math.ceil(((double) audioData.getNumberOfSamples()) / output.length);
        rhythmplace = correctForWrapAround(position, rhythmplace, loop);
        if (outOfRange(position, rhythmplace, loop, num16ths)) return;
        if (rhythmTrack.getEvent(position).isReverseCued()) addReverseCuedData(position, rhythmplace, volume, output); else addForwardCuedData(position, rhythmplace, volume, output);
    }

    private void addForwardCuedData(int position, int rhythmplace, float volume, float[] output) {
        int start = (rhythmplace - position) * output.length;
        audioData.addData(start, volume * rhythmTrack.getEvent(position).getVolume(), output);
    }

    private void addReverseCuedData(int position, int rhythmplace, float volume, float[] output) {
        int start = (rhythmplace - position) * output.length + 1;
        audioData.addReverseCuedData(start, volume * rhythmTrack.getEvent(position).getVolume(), output);
    }

    /**
     * similar to RiffEngine.bytesIn16th, except here we are worried about
     * the amount of float data rather than raw data
     *
     * @param tempo integer number of beats per minute
     */
    public static int samplesIn16th(int tempo) {
        int channels = AudioData.format.getChannels();
        float frameRate = AudioData.format.getFrameRate();
        int output = (int) (15 * channels * frameRate / (float) tempo);
        output = (output / channels) * channels;
        return output;
    }

    public void add16thOfFloatData(int rhythmposition, float volume, boolean loop, float[] output) {
        for (int j = 0; j < RiffEngine.BEAT_COUNT; j++) {
            addData(j, rhythmposition, volume, loop, output);
        }
    }
}
