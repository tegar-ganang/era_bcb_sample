package org.jdmp.sigmen.resources;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Sound extends Resources {

    private static Sound instance;

    private Clip clip;

    public Sound() {
        setPackage("org/jdmp/sigmen/resources/sound/");
    }

    public static Sound getSound() {
        return (instance == null ? instance = new Sound() : instance);
    }

    public void playSound(String name) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(getResource(name));
            AudioFormat af = ais.getFormat();
            AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, af.getSampleRate(), 16, af.getChannels(), af.getChannels() * 2, af.getSampleRate(), false);
            AudioInputStream dais = AudioSystem.getAudioInputStream(decodedFormat, ais);
            Clip c = AudioSystem.getClip();
            c.open(dais);
            c.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void stopSound() {
        try {
            getClip().stop();
        } catch (LineUnavailableException e) {
        }
    }

    public Clip getClip() throws LineUnavailableException {
        return (clip == null ? clip = AudioSystem.getClip() : clip);
    }
}
