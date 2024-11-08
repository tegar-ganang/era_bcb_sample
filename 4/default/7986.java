import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;

/**	<titleabbrev>ProgramChange</titleabbrev>

Shows how to do a program change on a Synthesizer.
    Just a code fragment...
 */
public class ProgramChange {

    public static void main(String[] args) throws MidiUnavailableException {
        int nNoteNumber = 66;
        int nVelocity = 100;
        int nChannel = 0;
        int nDuration = 2000;
        int nBank = Integer.parseInt(args[0]);
        int nProgram = Integer.parseInt(args[1]);
        Synthesizer synthesizer = MidiSystem.getSynthesizer();
        out("Synthsizer: " + synthesizer);
        synthesizer.open();
        MidiChannel[] channels = synthesizer.getChannels();
        out("Program before: " + channels[0].getProgram() + ", " + channels[0].getController(0) + ", " + channels[0].getController(20));
        channels[0].programChange(nBank, nProgram);
        out("Program after: " + channels[0].getProgram() + ", " + channels[0].getController(0) + ", " + channels[0].getController(20));
        channels[0].noteOn(nNoteNumber, nVelocity);
        sleep(nDuration);
        channels[nChannel].noteOff(nNoteNumber);
        sleep(200);
        synthesizer.close();
    }

    private static void sleep(int nDuration) {
        try {
            Thread.sleep(nDuration);
        } catch (InterruptedException e) {
        }
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}
