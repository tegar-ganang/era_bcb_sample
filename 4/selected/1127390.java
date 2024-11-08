package org.jogodeluta.tests.sounds;

import java.io.IOException;
import java.text.DecimalFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundTests extends Thread implements LineListener {

    public Clip clip = null;

    public DecimalFormat df = null;

    public String soundFile = null;

    public SoundTests(String fnm) {
        this.soundFile = fnm;
    }

    public void start() {
        df = new DecimalFormat("0.#");
        demoSound(soundFile);
        play();
        System.out.println("Waiting");
    }

    public void demoSound(String fnm) {
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(SoundTests.class.getResourceAsStream(fnm));
            AudioFormat format = stream.getFormat();
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat newFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(newFormat, stream);
                System.out.println("Converted Audio format: " + newFormat);
                format = newFormat;
            }
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Unsupported Clip File: " + fnm);
                System.exit(0);
            }
            clip = AudioSystem.getClip();
            clip.addLineListener(this);
            clip.open(stream);
            stream.close();
            double duration = clip.getMicrosecondLength() / 1000000.0;
            System.out.println("Duration: " + df.format(duration) + " secs");
        } catch (UnsupportedAudioFileException audioException) {
            audioException.printStackTrace();
            System.out.println("Unsupported audio file: " + fnm);
            System.exit(0);
        } catch (LineUnavailableException noLineException) {
            noLineException.printStackTrace();
            System.out.println("No audio line available for : " + fnm);
            System.exit(0);
        } catch (IOException ioException) {
            ioException.printStackTrace();
            System.out.println("Could not read: " + fnm);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Problem with " + fnm);
            System.exit(0);
        }
    }

    @Override
    public void update(LineEvent lineEvent) {
        if (lineEvent.getType() == LineEvent.Type.STOP) {
            System.out.println("Exiting...");
            clip.stop();
        }
    }

    public void play() {
        if (clip != null) {
            clip.start();
        }
    }

    @Override
    public void run() {
        this.start();
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        new SoundTests("/sounds/clean00.wav").run();
        new SoundTests("/sounds/clean01.wav").run();
        new SoundTests("/sounds/clean02.wav").run();
        new SoundTests("/sounds/clean03.wav").run();
        new SoundTests("/sounds/clean04.wav").run();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            System.out.println("Sleep Interrupted");
        }
    }
}
