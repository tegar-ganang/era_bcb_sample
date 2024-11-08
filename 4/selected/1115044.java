package org.apache.harmony.sound.tests.javax.sound.sampled;

import java.io.File;
import java.net.URL;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import junit.framework.TestCase;

/**
 * 
 * Dummy sound provider located in soundProvider.jar is used for testing.
 * Provider sources are provided at the comments at the end of this file.
 * 
 */
public class AudioSystemTest extends TestCase {

    public void testAudioFile() throws Exception {
        boolean ok;
        assertTrue(AudioSystem.getAudioFileFormat(new URL("file:./myFile.txt")) != null);
        AudioFileFormat.Type[] types = AudioSystem.getAudioFileTypes();
        ok = false;
        for (int i = 0; i < types.length; i++) {
            if (types[i].getExtension().equals("txt")) {
                ok = true;
                break;
            }
        }
        assertTrue(ok);
    }

    public void testMixer() throws Exception {
        boolean ok;
        Mixer.Info[] minfos = AudioSystem.getMixerInfo();
        assertTrue(minfos.length > 0);
        assertEquals("NAME", minfos[0].getName());
        assertEquals("VERSION", minfos[0].getVersion());
        assertTrue(AudioSystem.getMixer(null) != null);
        Mixer mix = AudioSystem.getMixer(minfos[0]);
        assertEquals("org.apache.harmony.sound.testProvider.MyMixer", mix.getClass().getName());
        Line.Info[] mli = mix.getSourceLineInfo();
        assertEquals(4, mli.length);
        Line.Info[] infos = AudioSystem.getSourceLineInfo(mli[0]);
        ok = false;
        for (int i = 0; i < infos.length; i++) {
            if (infos[i].getLineClass().getName().equals("org.apache.harmony.sound.testProvider.myClip")) {
                ok = true;
                break;
            }
        }
        assertTrue(ok);
        infos = AudioSystem.getTargetLineInfo(mli[0]);
        ok = false;
        for (int i = 0; i < infos.length; i++) {
            if (infos[i].getLineClass().getName().equals("org.apache.harmony.sound.testProvider.myClip")) {
                ok = true;
                break;
            }
        }
        assertTrue(ok);
    }

    public void testAudioInputStream() throws Exception {
        AudioInputStream stream = AudioSystem.getAudioInputStream(new File("myFile.txt"));
        assertTrue(stream != null);
        AudioSystem.write(stream, new AudioFileFormat.Type("TXT", "txt"), System.out);
        assertEquals(AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_UNSIGNED, stream), stream);
    }

    public void testFormatConversion() throws Exception {
        boolean ok;
        AudioFormat af_source = new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, 1f, 2, 3, 4, 5f, true);
        AudioFormat.Encoding[] aafe = AudioSystem.getTargetEncodings(AudioFormat.Encoding.PCM_UNSIGNED);
        ok = false;
        for (int i = 0; i < aafe.length; i++) {
            if (aafe[i].equals(AudioFormat.Encoding.PCM_SIGNED)) {
                ok = true;
                break;
            }
        }
        assertTrue(ok);
        assertTrue(AudioSystem.isConversionSupported(AudioFormat.Encoding.PCM_SIGNED, af_source));
        AudioFormat[] aaf = AudioSystem.getTargetFormats(AudioFormat.Encoding.PCM_UNSIGNED, af_source);
        ok = false;
        for (int i = 0; i < aaf.length; i++) {
            if (aaf[i].getSampleRate() == 10f && aaf[i].getSampleSizeInBits() == 2 && aaf[i].getChannels() == 30 && aaf[i].getFrameSize() == 40 && aaf[i].getFrameRate() == 50f) {
                ok = true;
                break;
            }
        }
        assertTrue(ok);
    }

    public void testGetLine() throws Exception {
        assertEquals("org.apache.harmony.sound.testProvider.myClip", AudioSystem.getLine(new Line.Info(javax.sound.sampled.Clip.class)).getClass().getName());
        assertEquals("org.apache.harmony.sound.testProvider.mySourceDataLine", AudioSystem.getLine(new Line.Info(javax.sound.sampled.SourceDataLine.class)).getClass().getName());
        assertEquals("org.apache.harmony.sound.testProvider.myTargetDataLine", AudioSystem.getLine(new Line.Info(javax.sound.sampled.TargetDataLine.class)).getClass().getName());
        assertEquals("org.apache.harmony.sound.testProvider.myPort", AudioSystem.getLine(new Line.Info(javax.sound.sampled.Port.class)).getClass().getName());
        assertEquals("org.apache.harmony.sound.testProvider.myClip", AudioSystem.getClip().getClass().getName());
    }
}
