package system.io;

import javax.sound.midi.MidiUnavailableException;
import system.Memory;

/**
 * @author dom
 */
public class MidiSoundCard extends SoundCard {

    private javax.sound.midi.Synthesizer synthesizer;

    javax.sound.midi.MidiChannel[] channels;

    /**
     * @throws MidiUnavailableException 
     * 
     */
    public MidiSoundCard(Memory mem, int memBegin) throws MidiUnavailableException {
        super(mem, memBegin);
        this.synthesizer = javax.sound.midi.MidiSystem.getSynthesizer();
        this.synthesizer.open();
        this.channels = synthesizer.getChannels();
    }

    public void play(int note) {
        this.channels[0].noteOn(note, 70);
    }

    public void stop() {
        this.channels[0].allNotesOff();
    }
}
