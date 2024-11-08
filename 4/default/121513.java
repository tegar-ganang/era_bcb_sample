import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;

/**	<titleabbrev>SynthNote</titleabbrev>
	<title>Playing a note on the synthesizer</title>

	<formalpara><title>Purpose</title>
	<para>Plays a single note on the synthesizer.</para>
	</formalpara>

	<formalpara><title>Usage</title>
	<para>
	<cmdsynopsis><command>java SynthNote</command>
	<arg choice="plain"><replaceable class="parameter">keynumber</replaceable></arg>
	<arg choice="plain"><replaceable class="parameter">velocity</replaceable></arg>
	<arg choice="plain"><replaceable class="parameter">duration</replaceable></arg>
	</cmdsynopsis>
	</para></formalpara>

	<formalpara><title>Parameters</title>
	<variablelist>
	<varlistentry>
	<term><replaceable class="parameter">keynumber</replaceable></term>
	<listitem><para>the MIDI key number</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><replaceable class="parameter">velocity</replaceable></term>
	<listitem><para>the velocity</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><replaceable class="parameter">duration</replaceable></term>
	<listitem><para>the duration in milliseconds</para></listitem>
	</varlistentry>
	</variablelist>
	</formalpara>

	<formalpara><title>Bugs, limitations</title>
	<para>The precision of the duration depends on the precision
	of <function>Thread.sleep()</function>, which in turn depends on
	the precision of the system time and the latency of th
	thread scheduling of the Java VM. For many VMs, this
	means about 20 ms. When playing multiple notes, it is
	recommended to use a <classname>Sequence</classname> and the
	<classname>Sequencer</classname>, which is supposed to give better
	timing.</para>
	</formalpara>

	<formalpara><title>Source code</title>
	<para>
	<ulink url="SynthNote.java.html">SynthNote.java</ulink>
	</para>
	</formalpara>

*/
public class SynthNote {

    private static boolean DEBUG = true;

    public static void main(String[] args) {
        int nChannelNumber = 0;
        int nNoteNumber = 0;
        int nVelocity = 0;
        int nDuration = 0;
        int nNoteNumberArgIndex = 0;
        switch(args.length) {
            case 4:
                nChannelNumber = Integer.parseInt(args[0]) - 1;
                nChannelNumber = Math.min(15, Math.max(0, nChannelNumber));
                nNoteNumberArgIndex = 1;
            case 3:
                nNoteNumber = Integer.parseInt(args[nNoteNumberArgIndex]);
                nNoteNumber = Math.min(127, Math.max(0, nNoteNumber));
                nVelocity = Integer.parseInt(args[nNoteNumberArgIndex + 1]);
                nVelocity = Math.min(127, Math.max(0, nVelocity));
                nDuration = Integer.parseInt(args[nNoteNumberArgIndex + 2]);
                nDuration = Math.max(0, nDuration);
                break;
            default:
                printUsageAndExit();
        }
        Synthesizer synth = null;
        try {
            synth = MidiSystem.getSynthesizer();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        }
        if (DEBUG) out("Synthesizer: " + synth);
        try {
            synth.open();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
            System.exit(1);
        }
        MidiChannel[] channels = synth.getChannels();
        MidiChannel channel = channels[nChannelNumber];
        if (DEBUG) out("MidiChannel: " + channel);
        channel.noteOn(nNoteNumber, nVelocity);
        try {
            Thread.sleep(nDuration);
        } catch (InterruptedException e) {
        }
        channel.noteOff(nNoteNumber);
        synth.close();
    }

    private static void printUsageAndExit() {
        out("SynthNote: usage:");
        out("java SynthNote [<channel>] <note_number> <velocity> <duration>");
        System.exit(1);
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}
