package org.freelords.util.sound;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.log4j.Logger;

public class SimpleSoundPlayer {

    /**
	 * Mixer being used if one was obtained
	 */
    protected Mixer mixer;

    protected Logger LOG = Logger.getLogger("org.freelords.ui.sound");

    protected SourceDataLine line;

    protected static final int EXTERNAL_BUFFER_SIZE = 2048;

    protected AudioFormat getDesiredFormat(AudioFormat baseFormat) {
        return new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
    }

    protected void play(InputStream is) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        AudioInputStream in = AudioSystem.getAudioInputStream(is);
        AudioFormat baseFormat = in.getFormat();
        AudioFormat decodedFormat = getDesiredFormat(baseFormat);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(decodedFormat, in);
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            mixer = null;
            if (mixers.length > 0) {
                mixer = AudioSystem.getMixer(mixers[0]);
                mixer.open();
                LOG.debug("Using sound mixer : " + mixers[0]);
            }
            if (mixer != null) {
                line = (SourceDataLine) mixer.getLine(info);
            } else {
                line = (SourceDataLine) AudioSystem.getLine(info);
            }
            try {
                line.open(decodedFormat);
                line.start();
                int nBytesRead = 0;
                byte[] abData = new byte[Math.min(EXTERNAL_BUFFER_SIZE, line.getBufferSize())];
                while (nBytesRead != -1) {
                    nBytesRead = bufferAudio(audioInputStream, abData);
                }
                line.drain();
            } finally {
                if (mixer != null) {
                    mixer.close();
                }
                line.close();
                line.stop();
            }
        } finally {
            audioInputStream.close();
            in.close();
            LOG.debug("Sound resources closed");
        }
    }

    protected int bufferAudio(AudioInputStream audioInputStream, byte[] abData) throws IOException {
        int nBytesRead;
        nBytesRead = audioInputStream.read(abData, 0, abData.length);
        if (nBytesRead >= 0) {
            line.write(abData, 0, nBytesRead);
        }
        return nBytesRead;
    }
}
