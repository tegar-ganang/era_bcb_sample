package org.paquitosoft.namtia.session.actions.player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javazoom.jl.decoder.JavaLayerException;
import org.paquitosoft.namtia.session.SessionController;
import org.paquitosoft.namtia.vo.SongVO;

/**
 *
 * @author paquitosoft
 */
public class NmPlayer {

    private static boolean playing;

    private boolean paused;

    private int currentFrame;

    private Thread thread;

    private static NmAdvancedPlayer player;

    private AudioInputStream audioStream;

    /**
     * Creates a new instance of NmPlayer
     */
    public NmPlayer() {
        playing = false;
        paused = false;
        currentFrame = 0;
    }

    protected InputStream getAudioStream(String filePath) {
        InputStream in = null;
        try {
            in = new FileInputStream(new File(filePath));
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return in;
    }

    public void pause() {
        if (!paused) {
            paused = true;
            playing = false;
            this.player.pause();
            System.out.println("    Frame en el que se pauso la reproduccion -> " + this.currentFrame);
        }
    }

    public void resume() {
        synchronized (this.player.getFriendlyObject()) {
            this.player.getFriendlyObject().notify();
        }
    }

    public void stopPlayer() {
        if (player != null && (playing || paused)) {
            player.close();
            playing = false;
        }
    }

    public void play(SongVO song) throws JavaLayerException {
        int startFrame = 0;
        if (playing) {
            player.close();
        } else if (paused) {
            paused = false;
            playing = true;
            this.resume();
            return;
        }
        if (song == null) {
            song = SessionController.getInstance().getCurrentSong();
        }
        playing = true;
        paused = false;
        File songFile = new File(song.getPath());
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(songFile));
            audioStream = AudioSystem.getAudioInputStream(songFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new JavaLayerException(e.getMessage());
        } catch (UnsupportedAudioFileException a) {
            a.printStackTrace();
            throw new JavaLayerException(a.getMessage());
        } catch (IOException q) {
            q.printStackTrace();
            throw new JavaLayerException(q.getMessage());
        }
        this.play(is, startFrame, Integer.MAX_VALUE, new NmPlayerListener());
    }

    private synchronized void play(final InputStream is, final int startFrame, final int endFrame, NmPlayerListener listener) throws JavaLayerException {
        if (player != null) {
            player.close();
        }
        player = new NmAdvancedPlayer(is);
        player.setPlayBackListener(listener);
        thread = new Thread() {

            public void run() {
                try {
                    player.play(startFrame, endFrame);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("ERROR: Could not play song (NmPlayer)");
                }
            }
        };
        thread.start();
    }

    public void setVolume(Double value) {
        try {
            AudioFormat format = audioStream.getFormat();
            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                audioStream = AudioSystem.getAudioInputStream(format, audioStream);
            }
            DataLine.Info info = new DataLine.Info(DataLine.class, audioStream.getFormat(), ((int) audioStream.getFrameLength() * format.getFrameSize()));
            DataLine dataLine = (DataLine) AudioSystem.getLine(info);
            dataLine.open();
            dataLine.start();
            FloatControl volume = (FloatControl) dataLine.getControl(FloatControl.Type.VOLUME);
            float dB = (float) (Math.log(value.doubleValue()) / Math.log(10.0) * 20.0);
            volume.setValue(dB);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
