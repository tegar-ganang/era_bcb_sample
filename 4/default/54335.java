import java.io.IOException;
import java.io.File;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**	<titleabbrev>SampleRateConverter</titleabbrev>
	<title>Converting the sample rate of audio files</title>

	<formalpara><title>Purpose</title>
	<para>Converts audio files, changing the sample rate of the
	audio data.</para>
	</formalpara>

	<formalpara><title>Usage</title>
	<para>
	<cmdsynopsis>
	<command>java SampleRateConverter</command>
	<arg choice="plain"><option>-h</option></arg>
	</cmdsynopsis>
	<cmdsynopsis>
	<command>java SampleRateConverter</command>
	<arg choice="plain"><replaceable class="parameter">targetsamplerate</replaceable></arg>
	<arg choice="plain"><replaceable class="parameter">sourcefile</replaceable></arg>
	<arg choice="plain"><replaceable class="parameter">targetfile</replaceable></arg>
	</cmdsynopsis>
	</para></formalpara>

	<formalpara><title>Parameters</title>
	<variablelist>
	<varlistentry>
	<term><option>-h</option></term>
	<listitem><para>prints usage information</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><replaceable class="parameter">targetsamplerate</replaceable></term>
	<listitem><para>the sample rate that should be converted to</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><replaceable class="parameter">sourcefile</replaceable></term>
	<listitem><para>the file name of the audio file that should be read to get the audio data to convert</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><replaceable class="parameter">targetfile</replaceable></term>
	<listitem><para>the file name of the audio file that the converted audio data should be written to</para></listitem>
	</varlistentry>
	</variablelist>
	</formalpara>

	<formalpara><title>Bugs, limitations</title>
	<para>Sample rate conversion can only be handled natively
	by <ulink url="http://www.tritonus.org/">Tritonus</ulink>.
	If you want to do sample rate conversion with the
	Sun jdk1.3/1.4, you have to install Tritonus' sample rate converter.
	It is part of the 'Tritonus miscellaneous' plug-in. See <ulink url
	="http://www.tritonus.org/plugins.html">Tritonus Plug-ins</ulink>.
	</para>
	</formalpara>

	<formalpara><title>Source code</title>
	<para>
	<ulink url="SampleRateConverter.java.html">SampleRateConverter.java</ulink>,
	<ulink url="AudioCommon.java.html">AudioCommon.java</ulink>
	</para>
	</formalpara>

*/
public class SampleRateConverter {

    /**	Flag for debugging messages.
	 *	If true, some messages are dumped to the console
	 *	during operation.	
	 */
    private static boolean DEBUG = true;

    public static void main(String[] args) throws UnsupportedAudioFileException, IOException {
        if (args.length == 1) {
            if (args[0].equals("-h")) {
                printUsageAndExit();
            } else {
                printUsageAndExit();
            }
        } else if (args.length != 3) {
            printUsageAndExit();
        }
        float fTargetSampleRate = Float.parseFloat(args[0]);
        if (DEBUG) {
            out("target sample rate: " + fTargetSampleRate);
        }
        File sourceFile = new File(args[1]);
        File targetFile = new File(args[2]);
        AudioFileFormat sourceFileFormat = AudioSystem.getAudioFileFormat(sourceFile);
        AudioFileFormat.Type targetFileType = sourceFileFormat.getType();
        AudioInputStream sourceStream = null;
        sourceStream = AudioSystem.getAudioInputStream(sourceFile);
        if (sourceStream == null) {
            out("cannot open source audio file: " + sourceFile);
            System.exit(1);
        }
        AudioFormat sourceFormat = sourceStream.getFormat();
        if (DEBUG) {
            out("source format: " + sourceFormat);
        }
        AudioFormat.Encoding encoding = sourceFormat.getEncoding();
        if (!AudioCommon.isPcm(encoding)) {
            out("encoding of source audio data is not PCM; conversion not possible");
            System.exit(1);
        }
        float fTargetFrameRate = fTargetSampleRate;
        AudioFormat targetFormat = new AudioFormat(sourceFormat.getEncoding(), fTargetSampleRate, sourceFormat.getSampleSizeInBits(), sourceFormat.getChannels(), sourceFormat.getFrameSize(), fTargetFrameRate, sourceFormat.isBigEndian());
        if (DEBUG) {
            out("desired target format: " + targetFormat);
        }
        AudioInputStream targetStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
        if (DEBUG) {
            out("targetStream: " + targetStream);
        }
        int nWrittenBytes = 0;
        nWrittenBytes = AudioSystem.write(targetStream, targetFileType, targetFile);
        if (DEBUG) {
            out("Written bytes: " + nWrittenBytes);
        }
    }

    private static void printUsageAndExit() {
        out("SampleRateConverter: usage:");
        out("\tjava SampleRateConverter -h");
        out("\tjava SampleRateConverter <targetsamplerate> <sourcefile> <targetfile>");
        System.exit(1);
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}
