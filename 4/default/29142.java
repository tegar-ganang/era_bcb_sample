import javax.sound.midi.Instrument;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;

public class MidiSound {

    private static final int MAX_CHANNELS = 16;

    private static MidiChannel[] channel = new MidiChannel[MAX_CHANNELS];

    private static boolean[] playing = new boolean[MAX_CHANNELS];

    private static long[] startTime = new long[MAX_CHANNELS];

    private static int[] duration = new int[MAX_CHANNELS];

    private static int[] note = new int[MAX_CHANNELS];

    private static Synthesizer synthesizer = null;

    private static int channelIndex = 0;

    private static int numChannels = 0;

    private static int numInstruments = 0;

    private static long currentTime = 0;

    public MidiSound() {
        try {
            synthesizer = MidiSystem.getSynthesizer();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        }
        try {
            synthesizer.open();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        }
        Soundbank soundBank = synthesizer.getDefaultSoundbank();
        if (soundBank != null) {
            Instrument instrumentArray[] = soundBank.getInstruments();
            numInstruments = instrumentArray.length;
            System.out.println("Number of Instruments = " + numInstruments);
        }
        channel = synthesizer.getChannels();
        numChannels = channel.length;
        System.out.println("Number of Channels = " + numChannels);
    }

    public void assignInstrumentToChannel(int i, int c) {
        channel[c].programChange(i);
    }

    public void play(int c, int n, int v, int d) {
        if (playing[c]) {
            channel[c].noteOff(note[c]);
        }
        playing[c] = true;
        duration[c] = d;
        note[c] = n;
        startTime[c] = System.currentTimeMillis();
        channel[c].noteOn(note[c], v);
    }

    public static void update() {
        currentTime = System.currentTimeMillis();
        for (int c = 0; c < numChannels; c++) {
            if (playing[c]) {
                if (currentTime - startTime[c] > duration[c]) {
                    playing[c] = false;
                    channel[c].noteOff(note[c]);
                }
            }
        }
    }

    public static int getNumInstruments() {
        return numInstruments;
    }

    public static int getNumChannels() {
        return numChannels;
    }

    public static void close() {
        synthesizer.close();
    }
}
