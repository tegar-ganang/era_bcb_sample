package game;

import java.io.IOException;
import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class audio {

    boolean loadAudio = true;

    Clip click;

    Clip error;

    Clip ocean;

    Clip smallCannon;

    Clip threeSmallCannons;

    Clip threeLargeCannons;

    Clip twoSmallCannons;

    Clip twoLargeCannons;

    Clip acknowledgeSelection[] = new Clip[10];

    Clip sink;

    audio() {
        if (loadAudio) {
            click = LoadAudio("Sound/click.wav");
            error = LoadAudio("Sound/doh.wav");
            ocean = LoadAudio("Sound/ocean1.wav");
            smallCannon = LoadAudio("Sound/smallCannon.wav");
            threeSmallCannons = LoadAudio("Sound/3smallCannons.wav");
            threeLargeCannons = LoadAudio("Sound/3LargeCannons.wav");
            twoSmallCannons = LoadAudio("Sound/twoSmallCannons.wav");
            twoLargeCannons = LoadAudio("Sound/twoLargeCannons.wav");
            sink = LoadAudio("Sound/sink.wav");
            for (int i = 1; i <= acknowledgeSelection.length; i++) {
                acknowledgeSelection[i - 1] = LoadAudio("Sound/hello" + i + ".wav");
            }
        }
    }

    Clip LoadAudio(String file) {
        Clip tempClip = null;
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(new File(file));
            AudioFormat format = stream.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            tempClip = (Clip) AudioSystem.getLine(info);
            tempClip.open(stream);
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (LineUnavailableException e) {
        } catch (UnsupportedAudioFileException e) {
        }
        return tempClip;
    }

    void playClick() {
        if (click != null) {
            click.setFramePosition(0);
            click.start();
        }
    }

    void playError() {
        if (error != null) {
            error.setFramePosition(0);
            error.start();
        }
    }

    void playOcean() {
        if (ocean != null) {
            ocean.setFramePosition(0);
            ocean.start();
        }
    }

    void playSmallCannon() {
        if (smallCannon != null) {
            smallCannon.setFramePosition(0);
            smallCannon.start();
        }
    }

    void playThreeSmallCannons() {
        if (threeSmallCannons != null) {
            threeSmallCannons.setFramePosition(0);
            threeSmallCannons.start();
        }
    }

    void playThreeLargeCannons() {
        if (threeLargeCannons != null) {
            threeLargeCannons.setFramePosition(0);
            threeLargeCannons.start();
        }
    }

    void playTwoSmallCannons() {
        if (twoSmallCannons != null) {
            twoSmallCannons.setFramePosition(0);
            twoSmallCannons.start();
        }
    }

    void playTwoLargeCannons() {
        if (twoLargeCannons != null) {
            twoLargeCannons.setFramePosition(0);
            twoLargeCannons.start();
        }
    }

    void playHello() {
        int temp = (int) (Math.random() * (acknowledgeSelection.length - 1));
        if (acknowledgeSelection[temp] != null) {
            acknowledgeSelection[temp].setFramePosition(0);
            acknowledgeSelection[temp].start();
        }
    }

    void playSink() {
        if (sink != null) {
            sink.setFramePosition(0);
            sink.start();
        }
    }
}
