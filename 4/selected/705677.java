package edu.mit.csail.sls.wami.applet.sound;

import java.io.IOException;
import javax.sound.sampled.*;

/** Makes an AudioSource from an AudioInputStream.
 *
 **/
public class AudioInputStreamSource implements SpeechDetector.AudioSource {

    AudioInputStream ais;

    public AudioInputStreamSource(AudioInputStream aisin) {
        AudioFormat aisf = aisin.getFormat();
        ais = (aisf.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && aisf.getSampleSizeInBits() == 16) ? aisin : AudioSystem.getAudioInputStream(new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, aisf.getSampleRate(), 16, aisf.getChannels(), 2 * aisf.getFrameSize() * aisf.getChannels(), aisf.getFrameRate(), aisf.isBigEndian()), aisin);
    }

    public int read(byte[] dest, int off, int len) throws IOException {
        return ais.read(dest, off, len);
    }

    public void close() {
        try {
            ais.close();
        } catch (IOException e) {
        }
    }

    public AudioFormat getFormat() {
        return ais.getFormat();
    }
}
