package jasel.av.audio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.lwjgl.openal.AL;

/**
 * 
 */
public class WavAudioData extends AudioData {

    private ByteBuffer data;

    private int format;

    private int samplerate;

    private void create(AudioInputStream ais) throws AudioDataException {
        AudioFormat audioformat = ais.getFormat();
        int channels = 0;
        if (audioformat.getChannels() == 1) {
            if (audioformat.getSampleSizeInBits() == 8) {
                channels = AL.AL_FORMAT_MONO8;
            } else if (audioformat.getSampleSizeInBits() == 16) {
                channels = AL.AL_FORMAT_MONO16;
            } else {
                throw new AudioDataException("Illegal sample size");
            }
        } else if (audioformat.getChannels() == 2) {
            if (audioformat.getSampleSizeInBits() == 8) {
                channels = AL.AL_FORMAT_STEREO8;
            } else if (audioformat.getSampleSizeInBits() == 16) {
                channels = AL.AL_FORMAT_STEREO16;
            } else {
                throw new AudioDataException("Illegal sample size");
            }
        } else {
            throw new AudioDataException("Only mono or stereo supported");
        }
        byte[] buf = new byte[audioformat.getChannels() * (int) ais.getFrameLength() * audioformat.getSampleSizeInBits() / 8];
        int read = 0, total = 0;
        try {
            while ((read = ais.read(buf, total, buf.length - total)) != -1 && total < buf.length) {
                total += read;
            }
        } catch (IOException ioe) {
            throw new AudioDataException("IO Error", ioe);
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(buf.length);
        buffer.put(buf);
        buffer.rewind();
        data = buffer;
        format = channels;
        samplerate = (int) audioformat.getSampleRate();
        try {
            ais.close();
        } catch (IOException ioe) {
        }
    }

    protected WavAudioData(String file) throws AudioDataException, IOException, UnsupportedAudioFileException {
        create(AudioSystem.getAudioInputStream(new BufferedInputStream(WavAudioData.class.getClassLoader().getResourceAsStream(file))));
    }

    public void dispose() {
        data.clear();
    }

    public int getCapacity() {
        return data.capacity();
    }

    public ByteBuffer getData() {
        return data;
    }

    public int getFormat() {
        return format;
    }

    public int getSampleRate() {
        return samplerate;
    }
}
