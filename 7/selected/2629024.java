package net.lunglet.sound.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import net.lunglet.sound.sampled.SphereAudioFileReaderTest;
import net.lunglet.util.ArrayMath;
import org.junit.Test;

public final class SoundUtilsTest {

    private static short[] convertBuffer(final byte[] buf) throws IOException {
        assertTrue(buf.length % 2 == 0);
        byte[] bigEndianBuf = new byte[buf.length];
        for (int i = 0; i < buf.length - 1; i += 2) {
            bigEndianBuf[i] = buf[i + 1];
            bigEndianBuf[i + 1] = buf[i];
        }
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bigEndianBuf));
        short[] samples = new short[buf.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = dis.readShort();
        }
        return samples;
    }

    private static short[] getEx5Channel(final int channel) throws IOException {
        InputStream stream = SphereAudioFileReaderTest.class.getResourceAsStream("ex5.wav");
        assertNotNull(stream);
        byte[] buf = SoundUtils.readChannel(stream, channel);
        stream.close();
        return convertBuffer(buf);
    }

    @Test
    public void testReadChannel() throws IOException {
        short[] channel0 = getEx5Channel(0);
        assertEquals(-72, channel0[0]);
        assertEquals(-72, channel0[1]);
        assertEquals(-12412, ArrayMath.min(channel0));
        assertEquals(11900, ArrayMath.max(channel0));
        short[] channel1 = getEx5Channel(1);
        assertEquals(-24956, ArrayMath.min(channel1));
        assertEquals(19836, ArrayMath.max(channel1));
    }

    @Test
    public void testReadChannels() throws IOException, UnsupportedAudioFileException {
        AudioInputStream ais = SphereAudioFileReaderTest.getAudioInputStream("ex5.wav");
        byte[][] channelsData = SoundUtils.readChannels(ais);
        for (int i = 0; i < channelsData.length; i++) {
            assertEquals(32000, channelsData[i].length);
            short[] expectedSamples = getEx5Channel(i);
            short[] actualSamples = convertBuffer(channelsData[i]);
            assertEquals(expectedSamples.length, actualSamples.length);
            for (int j = 0; j < expectedSamples.length; j++) {
                assertEquals(expectedSamples[j], actualSamples[j]);
            }
        }
        ais.close();
    }
}
