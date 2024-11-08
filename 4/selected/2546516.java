package Population.Individuals.Phenotype.IGA.Graph.RandomMidiMusic;

import Parameters.ControlParameters;
import javax.sound.midi.Instrument;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

/**
 *
 * @author 郝国生  HAO Guo-Sheng, HAO Guo Sheng, HAO GuoSheng
 */
public class SelectSound {

    Synthesizer synthesizer;

    Instrument[] instruments;

    MidiChannel[] channels;

    public static int NOTESIZE = 100;

    int channelNum = 3;

    int instr = 0;

    int[][] noteCode = new int[NOTESIZE][2];

    ;

    int track;

    public int n;

    public void setNoteCode(int[][] code, int num, int track) {
        for (int i = 0; i < num; i++) {
            for (int j = track; j <= track + 1; j++) {
                noteCode[i][j] = code[i][j];
                System.out.println(noteCode[i][j]);
            }
        }
        n = num;
        this.track = track;
    }

    public void playChannel(MidiChannel channel) {
        for (int i = 1; i < n; i++) {
            channel.noteOn(noteCode[i][track], 92);
            try {
                Thread.sleep(noteCode[i][track + 1]);
            } catch (InterruptedException e) {
            }
            channel.noteOff(noteCode[i][track], 92);
            System.out.println(noteCode[i][track]);
            System.out.println(noteCode[i][track + 1]);
        }
    }

    public SelectSound() {
    }

    public void open() {
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
        } catch (MidiUnavailableException ex) {
            Logger.getLogger(Sound.class.getName()).log(Level.SEVERE, null, ex);
        }
        instr = noteCode[0][track];
        instruments = synthesizer.getDefaultSoundbank().getInstruments();
        channels = synthesizer.getChannels();
        String name = instruments[instr].getName();
        if (name.endsWith("\n")) {
            name = name.trim();
        }
        System.out.println("Soundbank instrument " + instr + ": " + name);
        synthesizer.loadInstrument(instruments[instr]);
        channels[channelNum].programChange(instr);
        playChannel(channels[channelNum]);
        System.out.println(n);
    }
}
