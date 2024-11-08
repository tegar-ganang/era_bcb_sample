package demo;

import com.sun.media.sound.AudioSynthesizer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;
import uk.org.toot.audio.server.AudioClient;
import uk.org.toot.audio.server.AudioServer;

/**
 *
 * @author pjl
 */
public class TootAdapter {

    static void connect(AudioSynthesizer audioSynth, AudioServer server, final AudioProcess out) throws MidiUnavailableException {
        final AudioProcess synthVoice = createSynthAudioProcess(audioSynth);
        final AudioBuffer buff = server.createAudioBuffer("BUFF");
        AudioClient client = new AudioClient() {

            public void setEnabled(boolean arg0) {
            }

            public void work(int arg0) {
                synthVoice.processAudio(buff);
                out.processAudio(buff);
            }
        };
        server.setClient(client);
    }

    static AudioProcess createSynthAudioProcess(AudioSynthesizer audosynth) throws MidiUnavailableException {
        AudioFormat.Encoding PCM_FLOAT = new AudioFormat.Encoding("PCM_FLOAT");
        AudioFormat format = new AudioFormat(PCM_FLOAT, 44100, 32, 2, 4 * 2, 44100, ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN));
        final AudioInputStream ais = audosynth.openStream(format, null);
        System.out.println("PCM_FLOAT Encoding used!");
        final AudioProcess synthVoice = new AudioProcess() {

            byte[] streamBuffer = null;

            float[] floatArray = null;

            FloatBuffer floatBuffer = null;

            public void close() {
            }

            public void open() {
            }

            public int processAudio(AudioBuffer buffer) {
                if (buffer == null) {
                    return 0;
                }
                if (streamBuffer == null || streamBuffer.length != buffer.getSampleCount() * 8) {
                    ByteBuffer bytebuffer = ByteBuffer.allocate(buffer.getSampleCount() * 8).order(ByteOrder.nativeOrder());
                    streamBuffer = bytebuffer.array();
                    floatArray = new float[buffer.getSampleCount() * 2];
                    floatBuffer = bytebuffer.asFloatBuffer();
                }
                try {
                    ais.read(streamBuffer, 0, buffer.getSampleCount() * 8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                floatBuffer.position(0);
                floatBuffer.get(floatArray);
                float[] left = buffer.getChannel(0);
                float[] right = buffer.getChannel(1);
                for (int n = 0; n < buffer.getSampleCount() * 2; n += 2) {
                    left[n / 2] = floatArray[n];
                    right[n / 2] = floatArray[n + 1];
                }
                return AUDIO_OK;
            }
        };
        return synthVoice;
    }
}
