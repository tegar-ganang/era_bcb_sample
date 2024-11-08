package org.stellarium;

import javax.sound.sampled.*;
import java.io.File;

/**
 * manage an audio track (SDL mixer music track)
 */
public class Audio {

    public Audio(String filename, String name) throws StellariumException {
        trackName = name;
        try {
            AudioInputStream stream = AudioSystem.getAudioInputStream(new File(filename));
            AudioFormat format = stream.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
            track = (Clip) AudioSystem.getLine(info);
            track.open(stream);
        } catch (Exception e) {
            throw new StellariumException("Could not read audio clip", e);
        }
    }

    public Audio(String s, String s1, long l) {
    }

    protected void finalize() {
        stop();
        track.close();
    }

    public void play(boolean loop) {
        System.out.println("now playing audio");
        if (loop) {
            track.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            track.start();
        }
    }

    public void pause() {
        track.stop();
    }

    public void resume() {
        track.start();
    }

    void stop() {
        track.stop();
        track.setFramePosition(0);
    }

    public void update(long deltaTime) {
    }

    private Clip track;

    private String trackName;

    public void sync() {
    }

    public void close() {
    }

    public void incrementVolume() {
    }

    public void decrementVolume() {
    }

    public void setVolume(float v) {
    }
}
