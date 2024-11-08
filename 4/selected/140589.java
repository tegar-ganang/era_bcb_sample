package espresso3d.engine.lowlevel.sound;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.sound.sampled.AudioInputStream;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import espresso3d.engine.exceptions.E3DInvalidSoundFormatException;

/**
 * @author Curt
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class E3DWaveFile {

    private AudioInputStream audioStream = null;

    private ByteBuffer dataBuffer = null;

    public E3DWaveFile(AudioInputStream audioInputStream) {
        audioStream = audioInputStream;
    }

    /**
     * Returns one of AL10.AL_FORMAT_MONO8, AL10.AL_FORMAT_MONO16, AL10.AL_FORMAT_STEREO8, AL10.AL_FORMAT_STEREO16, or -1 
     * @return
     * @throws E3DInvalidSoundFormatException
     */
    public int getALFormat() throws E3DInvalidSoundFormatException {
        if (audioStream == null || audioStream.getFormat() == null) return -1;
        int format = -1;
        if (audioStream.getFormat().getChannels() == 1) {
            if (audioStream.getFormat().getSampleSizeInBits() == 8) format = AL10.AL_FORMAT_MONO8; else if (audioStream.getFormat().getSampleSizeInBits() == 16) format = AL10.AL_FORMAT_MONO16;
        } else if (audioStream.getFormat().getChannels() == 2) {
            if (audioStream.getFormat().getSampleSizeInBits() == 8) format = AL10.AL_FORMAT_STEREO8; else if (audioStream.getFormat().getSampleSizeInBits() == 16) format = AL10.AL_FORMAT_STEREO16;
        }
        if (format == -1) throw new E3DInvalidSoundFormatException("Channels: " + audioStream.getFormat().getChannels() + "and SampleSize: " + audioStream.getFormat().getSampleSizeInBits() + " bits " + "is not a support wav format.  Only mono and stereo of 8 and 16 bit sample size are supported.");
        return format;
    }

    public long getDataSize() {
        if (audioStream == null || audioStream.getFormat() == null) return -1;
        return audioStream.getFrameLength() * audioStream.getFormat().getFrameSize();
    }

    public ByteBuffer getDataBuffer() throws IOException {
        if (dataBuffer == null) {
            dataBuffer = BufferUtils.createByteBuffer((int) getDataSize());
            byte[] buffer = null;
            byte[] actualBuffer = null;
            int retCode = 0;
            int read = 0;
            while (retCode != -1 && audioStream.available() > 0) {
                buffer = new byte[audioStream.available()];
                retCode = audioStream.read(buffer, 0, audioStream.available());
                read += retCode;
                if (retCode != -1) {
                    actualBuffer = new byte[retCode];
                    for (int i = 0; i < retCode; i++) actualBuffer[i] = buffer[i];
                    dataBuffer.put(actualBuffer);
                }
            }
            dataBuffer.flip();
        }
        return dataBuffer;
    }

    public float getSampleRate() {
        if (audioStream == null || audioStream.getFormat() == null) return -1;
        return audioStream.getFormat().getSampleRate();
    }

    public AudioInputStream getAudioStream() {
        return audioStream;
    }

    public void setAudioStream(AudioInputStream audioStream) {
        this.audioStream = audioStream;
    }
}
