import java.io.File;
import java.io.IOException;
import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

/**	<titleabbrev>LoadSoundbank</titleabbrev>
	<title>Using custom Soundbanks</title>

	<formalpara><title>Purpose</title>
	<para>Loads a custom soundbank and uses its instruments. One note is
	played to verify successful loading.</para>
	</formalpara>

	<formalpara><title>Usage</title>
	<para>
	<cmdsynopsis><command>java LoadSoundbank</command>
	<arg choice="opt">
		<replaceable class="parameter">soundbank</replaceable>
	</arg>
	</cmdsynopsis>
	</para></formalpara>

	<formalpara><title>Parameters</title>
	<variablelist>
	<varlistentry>
	<term><replaceable class="parameter">soundbank</replaceable></term>
	<listitem><para>the filename of a custom soundbank to be loaded. If no
	soundbank is specified, the default soundbank of the synthesizer is used.
	If there is no default soundbank, no sound will be produced (without an
	error message).</para></listitem>
	</varlistentry>
	</variablelist>
	</formalpara>

	<formalpara><title>Bugs, limitations</title>
	<para>Using a custom soundbank even if no default soundbank is
	available only works with JDK 1.5.0 and later.</para>
	</formalpara>

	<formalpara><title>Source code</title>
	<para>
	<ulink url="LoadSoundbank.java.html">LoadSoundbank.java</ulink>
	</para>
	</formalpara>

*/
public class LoadSoundbank {

    private static final boolean DEBUG = true;

    public static void main(String[] args) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        int nNoteNumber = 66;
        int nVelocity = 100;
        int nDuration = 2000;
        Soundbank soundbank = null;
        if (args.length == 1) {
            File file = new File(args[0]);
            soundbank = MidiSystem.getSoundbank(file);
            if (DEBUG) out("Soundbank: " + soundbank);
        } else if (args.length > 1) {
            printUsageAndExit();
        }
        Synthesizer synth = null;
        synth = MidiSystem.getSynthesizer();
        if (DEBUG) out("Synthesizer: " + synth);
        synth.open();
        if (DEBUG) out("Defaut soundbank: " + synth.getDefaultSoundbank());
        if (soundbank != null) {
            out("soundbank supported: " + synth.isSoundbankSupported(soundbank));
            boolean bInstrumentsLoaded = synth.loadAllInstruments(soundbank);
            if (DEBUG) out("Instruments loaded: " + bInstrumentsLoaded);
        }
        MidiChannel[] channels = synth.getChannels();
        channels[0].noteOn(nNoteNumber, nVelocity);
        try {
            Thread.sleep(nDuration);
        } catch (InterruptedException e) {
        }
        channels[0].noteOff(nNoteNumber);
    }

    private static void printUsageAndExit() {
        out("LoadSoundbank: usage:");
        out("java LoadSoundbank [<soundbankfilename>]");
        System.exit(1);
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}
