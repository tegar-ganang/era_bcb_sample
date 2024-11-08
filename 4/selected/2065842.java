package com.petersalomonsen.pjsynth;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Line;
import javax.sound.midi.MidiSystem;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author peter
 */
public class PJSynthTest {

    public PJSynthTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testOpenMidiDevice() throws Exception {
        MidiDevice dev = null;
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            if (info.toString().startsWith("UM1")) {
                dev = MidiSystem.getMidiDevice(info);
                break;
            }
        }
        PJSynth synth = (PJSynth) MidiSystem.getMidiDevice(new PJSynthProvider.PJSynthProviderInfo());
        final TargetDataLine line = (TargetDataLine) ((Mixer) synth).getLine(new Line.Info(TargetDataLine.class));
        AudioFormat.Encoding PCM_FLOAT = new AudioFormat.Encoding("PCM_FLOAT");
        AudioFormat format = new AudioFormat(PCM_FLOAT, 44100, 32, 2, 4 * 2, 44100, ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN));
        line.open(format);
        dev.open();
        dev.getTransmitter().setReceiver(synth.getReceiver());
        AudioInputStream ais = new AudioInputStream(line);
        assertTrue(AudioSystem.isConversionSupported(Encoding.PCM_SIGNED, ais.getFormat()));
        AudioInputStream convertedAis = AudioSystem.getAudioInputStream(Encoding.PCM_SIGNED, ais);
        System.out.println(AudioSystem.getMixerInfo()[2]);
        SourceDataLine sdl = AudioSystem.getSourceDataLine(convertedAis.getFormat(), AudioSystem.getMixerInfo()[2]);
        sdl.open();
        sdl.start();
        byte[] buf = new byte[512];
        for (int n = 0; n < 20000; n++) {
            int read = convertedAis.read(buf);
            sdl.write(buf, 0, read);
        }
    }
}
