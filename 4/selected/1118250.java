package jtinymediav1.jtinymedia.core.decoder;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Administrator
 */
public class JTM_Mp3Player {

    private static final Logger LOGGER = LoggerFactory.getLogger(JTM_Mp3Player.class);

    private AudioInputStream in;

    private AudioInputStream decodedStream;

    private AudioFormat baseFormat;

    private AudioFormat decodedFormat;

    private SourceDataLine line;

    private byte[] data;

    private boolean complete;

    private float vol;

    private File srcFile;

    public JTM_Mp3Player(File f, float gain) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        srcFile = f;
        in = AudioSystem.getAudioInputStream(f);
        baseFormat = in.getFormat();
        decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
        decodedStream = AudioSystem.getAudioInputStream(decodedFormat, in);
        data = new byte[4096];
        line = getLine(decodedFormat);
        setVolume(gain);
        setComplete(false);
    }

    public void play() throws IOException {
        if (line != null) {
            line.start();
            LOGGER.debug("Playing music with volume " + vol);
            int nBytesRead = 0, nBytesWritten = 0;
            while (nBytesRead != -1) {
                nBytesRead = decodedStream.read(data, 0, data.length);
                if (nBytesRead != -1) {
                    nBytesWritten = line.write(data, 0, nBytesRead);
                }
            }
            line.drain();
            line.stop();
            line.close();
            decodedStream.close();
            setComplete(true);
        }
    }

    public void play(int skipSec) throws IOException {
        long bytesToSkip = 0;
        try {
            float percent = (((float) (skipSec)) / ((float) (this.calculateLength())));
            bytesToSkip = (long) (percent * srcFile.length());
            if (line != null) {
                LOGGER.debug("Skipping " + bytesToSkip + " bytes");
                decodedStream.skip(bytesToSkip);
                line.start();
                LOGGER.debug("Playing music with volume " + vol);
                int nBytesRead = 0, nBytesWritten = 0;
                while (nBytesRead != -1) {
                    nBytesRead = decodedStream.read(data, 0, data.length);
                    if (nBytesRead != -1) {
                        nBytesWritten = line.write(data, 0, nBytesRead);
                    }
                }
                line.drain();
                line.stop();
                line.close();
                decodedStream.close();
                setComplete(true);
            }
        } catch (Exception e) {
            LOGGER.debug("File length[sec] couldn´t be obtained, play from beginning");
            play();
        }
    }

    public SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    public Control[] getControls() {
        return line.getControls();
    }

    public void setVolume(float value) {
        FloatControl gainCrtl = (FloatControl) getControls()[0];
        float newval = (gainCrtl.getMinimum() + ((float) (86.0 / 100.0) * value));
        gainCrtl.setValue(newval);
        this.vol = newval;
    }

    /**
     * @return the complete
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * @param complete the complete to set
     */
    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    private int calculateLength() throws UnsupportedAudioFileException, IOException {
        double seconds = 0;
        Long lenMicroseconds = new Long(0);
        AudioFileFormat mp3Format = null;
        mp3Format = AudioSystem.getAudioFileFormat(srcFile);
        lenMicroseconds = (Long) mp3Format.properties().get("duration");
        seconds = ((double) (lenMicroseconds) / 1000000);
        return ((int) (seconds));
    }
}
