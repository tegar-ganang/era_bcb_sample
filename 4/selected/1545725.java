package net.sf.opendub.xplayer;

import java.net.URL;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.tritonus.dsp.ais.AmplitudeAudioInputStream;
import org.tritonus.share.sampled.file.TAudioFileFormat;

public class XPlayerSource {

    private File file = null;

    private URL url = null;

    private String name;

    private AmplitudeAudioInputStream stream;

    public XPlayerSource() {
    }

    public XPlayerSource(File file) {
        this.file = file;
        setName(file.getName());
    }

    public XPlayerSource(URL url) {
        this.url = url;
        setName(url.getFile());
    }

    public void setStream(AmplitudeAudioInputStream stream) {
        this.stream = stream;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLength(Float frameRate) throws IOException, UnsupportedAudioFileException {
        if (getStream().getFrameLength() > 0) {
            return getStream().getFrameLength();
        }
        Long duration;
        AudioFileFormat audioFileFormat;
        if (file != null) {
            audioFileFormat = AudioSystem.getAudioFileFormat(file);
        } else if (url != null) {
            audioFileFormat = AudioSystem.getAudioFileFormat(url);
            return 0;
        } else {
            throw new IOException("url/source is missing!");
        }
        if (audioFileFormat instanceof TAudioFileFormat) {
            duration = (Long) ((TAudioFileFormat) audioFileFormat).properties().get("duration");
            duration = (duration * frameRate.intValue()) / 1000000;
            return duration;
        }
        return 0;
    }

    public AmplitudeAudioInputStream getStream() throws IOException, UnsupportedAudioFileException {
        if (stream == null) {
            AudioInputStream streamInput = null;
            if (file != null) {
                streamInput = AudioSystem.getAudioInputStream(file);
            } else if (url != null) {
                streamInput = AudioSystem.getAudioInputStream(url);
            } else {
                throw new IOException("url/source is missing!");
            }
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, streamInput.getFormat().getSampleRate(), 16, streamInput.getFormat().getChannels(), streamInput.getFormat().getChannels() * 2, streamInput.getFormat().getSampleRate(), false);
            stream = new AmplitudeAudioInputStream(AudioSystem.getAudioInputStream(decodedFormat, streamInput));
        }
        return stream;
    }

    public String toString() {
        return getName();
    }
}
