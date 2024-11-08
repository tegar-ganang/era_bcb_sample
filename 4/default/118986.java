import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import gnu.getopt.Getopt;

/**	<titleabbrev>AudioPlayer</titleabbrev>
	<title>Playing an audio file (advanced)</title>

	<formalpara><title>Purpose</title>
	<para>
	Plays a single audio file. Capable of playing some
	compressed audio formats (A-law, &mu;-law, maybe ogg vorbis, mp3,
	GSM06.10).
	Allows control over buffering
	and which mixer to use.
	</para></formalpara>

	<formalpara><title>Usage</title>
	<para>
	<cmdsynopsis>
	<command>java AudioPlayer</command>
	<arg choice="plain"><option>-l</option></arg>
	</cmdsynopsis>
	<cmdsynopsis>
	<command>java AudioPlayer</command>
	<arg><option>-M <replaceable>mixername</replaceable></option></arg>
	<arg><option>-e <replaceable>buffersize</replaceable></option></arg>
	<arg><option>-i <replaceable>buffersize</replaceable></option></arg>
	<arg choice="plain"><replaceable>audiofile</replaceable></arg>
	</cmdsynopsis>
	</para></formalpara>

	<formalpara><title>Parameters</title>
	<variablelist>
	<varlistentry>
	<term><option>-h</option></term>
	<listitem><para>print usage message</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option>-l</option></term>
	<listitem><para>lists the available mixers</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option>-M <replaceable>mixername</replaceable></option></term>
	<listitem><para>selects a mixer to play on</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option>-e <replaceable>buffersize</replaceable></option></term>
	<listitem><para>the buffer size to use in the application ("extern")</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option>-i <replaceable>buffersize</replaceable></option></term>
	<listitem><para>the buffer size to use in Java Sound ("intern")</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option>-E <replaceable>endianess</replaceable></option></term>
	<listitem><para>the endianess ("big" or "little") to use in conversions. The default is little. Specifying this option forces a conversion, even if the audio format is supported by SourceDataLines directly.</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option>-S <replaceable>sample size</replaceable></option></term>
	<listitem><para>the sample size in bits to use in conversions. The default is 16. Specifying this option forces a conversion, even if the audio format is supported by SourceDataLines directly.</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option>-D</option></term>
	<listitem><para>enable debugging output</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option>-f</option></term>
	<listitem><para>interpret filename arguments as filenames. This is the default. This option is exclusive to <option>-u</option>.</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option>-u</option></term>
	<listitem><para>interpret filename arguments as URLs. The default is to interpret them as filenames. This option is exclusive to <option>-f</option>.</para></listitem>
	</varlistentry>
	<varlistentry>
	<term><option><replaceable>audiofile</replaceable></option></term>
	<listitem><para>the file name of the audio file to play</para></listitem>
	</varlistentry>
	</variablelist>
	</formalpara>

	<formalpara><title>Bugs, limitations</title>
	<para>
	Compressed formats can be handled depending on the
	capabilities of the Java Sound implementation.
	A-law and &mu;-law can be handled in any known Java Sound implementation.
	Ogg vorbis, mp3 and GSM 06.10 can be handled by Tritonus. If you want to play these
	formats with the Sun jdk1.3/1.4, you have to install the respective plug-ins
	from <ulink url
	="http://www.tritonus.org/plugins.html">Tritonus Plug-ins</ulink>.
	</para>
	</formalpara>

	<formalpara><title>Source code</title>
	<para>
	<ulink url="AudioPlayer.java.html">AudioPlayer.java</ulink>,
	<ulink url="AudioCommon.java.html">AudioCommon.java</ulink>,
	<olink targetdocent="getopt">gnu.getopt.Getopt</olink>
	</para>
	</formalpara>

*/
public class AudioPlayer {

    /**	Flag for debugging messages.
	 *	If true, some messages are dumped to the console
	 *	during operation.	
	 */
    private static boolean DEBUG = false;

    private static int DEFAULT_EXTERNAL_BUFFER_SIZE = 128000;

    public static void main(String[] args) throws Exception {
        boolean bInterpretFilenameAsUrl = false;
        boolean bForceConversion = false;
        boolean bBigEndian = false;
        int nSampleSizeInBits = 16;
        String strMixerName = null;
        int nExternalBufferSize = DEFAULT_EXTERNAL_BUFFER_SIZE;
        int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;
        Getopt g = new Getopt("AudioPlayer", args, "hlufM:e:i:E:S:D");
        int c;
        while ((c = g.getopt()) != -1) {
            switch(c) {
                case 'h':
                    printUsageAndExit();
                case 'l':
                    AudioCommon.listMixersAndExit(true);
                case 'u':
                    bInterpretFilenameAsUrl = true;
                    break;
                case 'f':
                    bInterpretFilenameAsUrl = false;
                    break;
                case 'M':
                    strMixerName = g.getOptarg();
                    if (DEBUG) out("AudioPlayer.main(): mixer name: " + strMixerName);
                    break;
                case 'e':
                    nExternalBufferSize = Integer.parseInt(g.getOptarg());
                    break;
                case 'i':
                    nInternalBufferSize = Integer.parseInt(g.getOptarg());
                    break;
                case 'E':
                    String strEndianess = g.getOptarg();
                    strEndianess = strEndianess.toLowerCase();
                    if (strEndianess.equals("big")) {
                        bBigEndian = true;
                    } else if (strEndianess.equals("little")) {
                        bBigEndian = false;
                    } else {
                        printUsageAndExit();
                    }
                    bForceConversion = true;
                    break;
                case 'S':
                    nSampleSizeInBits = Integer.parseInt(g.getOptarg());
                    bForceConversion = true;
                    break;
                case 'D':
                    DEBUG = true;
                    break;
                case '?':
                    printUsageAndExit();
                default:
                    out("getopt() returned " + c);
                    break;
            }
        }
        String strFilenameOrUrl = null;
        for (int i = g.getOptind(); i < args.length; i++) {
            if (strFilenameOrUrl == null) {
                strFilenameOrUrl = args[i];
            } else {
                printUsageAndExit();
            }
        }
        if (strFilenameOrUrl == null) {
            printUsageAndExit();
        }
        AudioInputStream audioInputStream = null;
        if (bInterpretFilenameAsUrl) {
            URL url = new URL(strFilenameOrUrl);
            audioInputStream = AudioSystem.getAudioInputStream(url);
        } else {
            if (strFilenameOrUrl.equals("-")) {
                InputStream inputStream = new BufferedInputStream(System.in);
                audioInputStream = AudioSystem.getAudioInputStream(inputStream);
            } else {
                File file = new File(strFilenameOrUrl);
                audioInputStream = AudioSystem.getAudioInputStream(file);
            }
        }
        if (DEBUG) out("AudioPlayer.main(): primary AIS: " + audioInputStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        if (DEBUG) out("AudioPlayer.main(): primary format: " + audioFormat);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, nInternalBufferSize);
        boolean bIsSupportedDirectly = AudioSystem.isLineSupported(info);
        if (!bIsSupportedDirectly || bForceConversion) {
            AudioFormat sourceFormat = audioFormat;
            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), nSampleSizeInBits, sourceFormat.getChannels(), sourceFormat.getChannels() * (nSampleSizeInBits / 8), sourceFormat.getSampleRate(), bBigEndian);
            if (DEBUG) {
                out("AudioPlayer.main(): source format: " + sourceFormat);
                out("AudioPlayer.main(): target format: " + targetFormat);
            }
            audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            audioFormat = audioInputStream.getFormat();
            if (DEBUG) out("AudioPlayer.main(): converted AIS: " + audioInputStream);
            if (DEBUG) out("AudioPlayer.main(): converted format: " + audioFormat);
        }
        SourceDataLine line = getSourceDataLine(strMixerName, audioFormat, nInternalBufferSize);
        if (line == null) {
            out("AudioPlayer: cannot get SourceDataLine for format " + audioFormat);
            System.exit(1);
        }
        if (DEBUG) out("AudioPlayer.main(): line: " + line);
        if (DEBUG) out("AudioPlayer.main(): line format: " + line.getFormat());
        if (DEBUG) out("AudioPlayer.main(): line buffer size: " + line.getBufferSize());
        line.start();
        int nBytesRead = 0;
        byte[] abData = new byte[nExternalBufferSize];
        if (DEBUG) out("AudioPlayer.main(): starting main loop");
        while (nBytesRead != -1) {
            try {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (DEBUG) out("AudioPlayer.main(): read from AudioInputStream (bytes): " + nBytesRead);
            if (nBytesRead >= 0) {
                int nBytesWritten = line.write(abData, 0, nBytesRead);
                if (DEBUG) out("AudioPlayer.main(): written to SourceDataLine (bytes): " + nBytesWritten);
            }
        }
        if (DEBUG) out("AudioPlayer.main(): finished main loop");
        if (DEBUG) out("AudioPlayer.main(): before drain");
        line.drain();
        if (DEBUG) out("AudioPlayer.main(): before close");
        line.close();
    }

    private static SourceDataLine getSourceDataLine(String strMixerName, AudioFormat audioFormat, int nBufferSize) {
        SourceDataLine line = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, nBufferSize);
        try {
            if (strMixerName != null) {
                Mixer.Info mixerInfo = AudioCommon.getMixerInfo(strMixerName);
                if (mixerInfo == null) {
                    out("AudioPlayer: mixer not found: " + strMixerName);
                    System.exit(1);
                }
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                line = (SourceDataLine) mixer.getLine(info);
            } else {
                line = (SourceDataLine) AudioSystem.getLine(info);
            }
            line.open(audioFormat, nBufferSize);
        } catch (LineUnavailableException e) {
            if (DEBUG) e.printStackTrace();
        } catch (Exception e) {
            if (DEBUG) e.printStackTrace();
        }
        return line;
    }

    private static void printUsageAndExit() {
        out("AudioPlayer: usage:");
        out("\tjava AudioPlayer -h");
        out("\tjava AudioPlayer -l");
        out("\tjava AudioPlayer");
        out("\t\t[-M <mixername>]");
        out("\t\t[-e <externalBuffersize>]");
        out("\t\t[-i <internalBuffersize>]");
        out("\t\t[-S <SampleSizeInBits>]");
        out("\t\t[-B (big | little)]");
        out("\t\t[-D]");
        out("\t\t[-u | -f]");
        out("\t\t<soundfileOrUrl>");
        System.exit(1);
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}
