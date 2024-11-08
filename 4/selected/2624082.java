package Population.Individuals.Phenotype.IGA.Sound;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.*;

/**
 *
 * @author 郝国生  HAO Guo-Sheng, HAO Guo Sheng, HAO GuoSheng
 */
public class Sound {

    Synthesizer synthesizer;

    Instrument[] instruments;

    MidiChannel[] channels;

    public static int NOTESIZE = 100;

    int channelNum = 3;

    int instr = 9;

    int[][] noteCode = new int[NOTESIZE][3];

    public void setNoteCode(int[][] code) {
        for (int i = 0; i < noteCode.length; i++) {
            for (int j = 0; j < noteCode[0].length; j++) {
                noteCode[i][j] = code[i][j];
            }
        }
    }

    public void playChannel(MidiChannel channel) {
        for (int i = 0; i < noteCode.length; i++) {
            channel.noteOn(noteCode[i][0], noteCode[i][1]);
            try {
                Thread.sleep(noteCode[i][2]);
            } catch (InterruptedException e) {
            }
        }
        for (int i = 0; i < noteCode.length; i++) {
            channel.noteOff(noteCode[i][0]);
        }
    }

    public Sound() {
    }

    public void open() {
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
        } catch (MidiUnavailableException ex) {
            Logger.getLogger(Sound.class.getName()).log(Level.SEVERE, null, ex);
        }
        instruments = synthesizer.getDefaultSoundbank().getInstruments();
        channels = synthesizer.getChannels();
        String name = instruments[instr].getName();
        if (name.endsWith("\n")) {
            name = name.trim();
        }
        synthesizer.loadInstrument(instruments[instr]);
        channels[channelNum].programChange(instr);
        playChannel(channels[channelNum]);
    }
}
