import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Properties;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javazoom.spi.PropertiesContainer;
import junit.framework.TestCase;

/**
 * Simple player (based on Vorbis SPI) unit test.
 * It takes around 3%-5% of CPU and 10MB RAM under Win2K/P4/2.4GHz/JDK1.4.1
 * It takes around 4% of CPU and 10MB RAM under Win2K/Athlon/1GHz/JDK1.3.1
 */
public class PlayerTest extends TestCase {

    private String basefile = null;

    private String filename = null;

    private String name = null;

    private String baseurl = null;

    private String fileurl = null;

    private Properties props = null;

    private PrintStream out = null;

    /**
	 * Constructor for PlayerTest.
	 * @param arg0
	 */
    public PlayerTest(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        super.setUp();
        props = new Properties();
        InputStream pin = getClass().getClassLoader().getResourceAsStream("test.ogg.properties");
        props.load(pin);
        basefile = (String) props.getProperty("basefile");
        baseurl = (String) props.getProperty("baseurl");
        name = (String) props.getProperty("filename");
        filename = basefile + name;
        String stream = (String) props.getProperty("stream");
        if (stream != null) fileurl = stream; else fileurl = baseurl + name;
        out = System.out;
    }

    public void testPlayFile() {
        try {
            if (out != null) out.println("---  Start : " + filename + "  ---");
            File file = new File(filename);
            AudioFileFormat aff = AudioSystem.getAudioFileFormat(file);
            if (out != null) out.println("Audio Type : " + aff.getType());
            AudioInputStream in = AudioSystem.getAudioInputStream(file);
            AudioInputStream din = null;
            if (in != null) {
                AudioFormat baseFormat = in.getFormat();
                if (out != null) out.println("Source Format : " + baseFormat.toString());
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                if (out != null) out.println("Target Format : " + decodedFormat.toString());
                din = AudioSystem.getAudioInputStream(decodedFormat, in);
                if (din instanceof PropertiesContainer) {
                    assertTrue("PropertiesContainer : OK", true);
                } else {
                    assertTrue("Wrong PropertiesContainer instance", false);
                }
                rawplay(decodedFormat, din);
                in.close();
                if (out != null) out.println("---  Stop : " + filename + "  ---");
                assertTrue("testPlay : OK", true);
            }
        } catch (Exception e) {
            assertTrue("testPlay : " + e.getMessage(), false);
        }
    }

    public void _testPlayURL() {
        try {
            if (out != null) out.println("---  Start : " + fileurl + "  ---");
            URL url = new URL(fileurl);
            AudioFileFormat aff = AudioSystem.getAudioFileFormat(url);
            if (out != null) out.println("Audio Type : " + aff.getType());
            AudioInputStream in = AudioSystem.getAudioInputStream(url);
            AudioInputStream din = null;
            if (in != null) {
                AudioFormat baseFormat = in.getFormat();
                if (out != null) out.println("Source Format : " + baseFormat.toString());
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                if (out != null) out.println("Target Format : " + decodedFormat.toString());
                din = AudioSystem.getAudioInputStream(decodedFormat, in);
                if (din instanceof PropertiesContainer) {
                    assertTrue("PropertiesContainer : OK", true);
                } else {
                    assertTrue("Wrong PropertiesContainer instance", false);
                }
                rawplay(decodedFormat, din);
                in.close();
                if (out != null) out.println("---  Stop : " + filename + "  ---");
                assertTrue("testPlay : OK", true);
            }
        } catch (Exception e) {
            assertTrue("testPlay : " + e.getMessage(), false);
        }
    }

    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    private void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
        byte[] data = new byte[4096];
        SourceDataLine line = getLine(targetFormat);
        if (line != null) {
            line.start();
            int nBytesRead = 0, nBytesWritten = 0;
            while (nBytesRead != -1) {
                nBytesRead = din.read(data, 0, data.length);
                if (nBytesRead != -1) nBytesWritten = line.write(data, 0, nBytesRead);
            }
            line.drain();
            line.stop();
            line.close();
            din.close();
        }
    }
}
