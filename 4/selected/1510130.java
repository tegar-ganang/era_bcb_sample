package eu.easyedu.jnetwalk.utils;

import eu.easyedu.jnetwalk.Settings;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioPlayer extends Thread {

    private String fileName;

    private Settings settings;

    private static final transient Map<String, Clip> cache = new HashMap<String, Clip>();

    public AudioPlayer(String wavfile, Settings settings) {
        fileName = wavfile;
        this.settings = settings;
    }

    @Override
    public void run() {
        if (settings.isPlaySound()) {
            playSound();
        }
    }

    private void playSound() {
        Clip clip = cache.get(fileName);
        try {
            if (clip == null) {
                AudioInputStream stream = AudioSystem.getAudioInputStream(getClass().getResourceAsStream(fileName));
                AudioFormat format = stream.getFormat();
                if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                    format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                    stream = AudioSystem.getAudioInputStream(format, stream);
                }
                DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), (int) stream.getFrameLength() * format.getFrameSize());
                clip = (Clip) AudioSystem.getLine(info);
                clip.open(stream);
                cache.put(fileName, clip);
            }
            clip.setFramePosition(0);
            clip.start();
        } catch (IOException e) {
        } catch (LineUnavailableException e) {
        } catch (UnsupportedAudioFileException e) {
        }
    }
}
