package com.jpianobar.main.player;

import com.jpianobar.main.Pandora;
import com.jpianobar.main.PandoraPlayer;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.*;
import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: vincent
 * Date: 7/17/11
 * Time: 7:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class WavPlayer {

    private static Thread musicPlayerThread;

    private boolean alive = true;

    private boolean paused = false;

    private float available;

    private float total;

    private double duration;

    private JProgressBar musicPlayerBar;

    private static final Logger LOG = Logger.getLogger(WavPlayer.class.getName());

    public WavPlayer(JProgressBar musicProgressBar) {
        this.musicPlayerBar = musicProgressBar;
    }

    public void beginPlaying() {
        if (musicPlayerThread != null) {
            musicPlayerThread.stop();
        }
        musicPlayerThread = new Thread() {

            @Override
            public void run() {
                alive = true;
                AudioInputStream audioInputStream = null;
                try {
                    audioInputStream = AudioSystem.getAudioInputStream(new File("tempSongFileOut.aac"));
                    try {
                        total = audioInputStream.available();
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, e.getMessage());
                        if (alive) {
                            Pandora.pandoraPlayer.setPlayerState(PandoraPlayer.PlayerState.WAITING_FOR_NEXT_SONG);
                        }
                        return;
                    }
                } catch (UnsupportedAudioFileException e) {
                    LOG.log(Level.SEVERE, e.getMessage());
                    if (alive) {
                        Pandora.pandoraPlayer.setPlayerState(PandoraPlayer.PlayerState.WAITING_FOR_NEXT_SONG);
                    }
                    return;
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, e.getMessage());
                    if (alive) {
                        Pandora.pandoraPlayer.setPlayerState(PandoraPlayer.PlayerState.WAITING_FOR_NEXT_SONG);
                    }
                    return;
                }
                SourceDataLine sourceDataLine = null;
                try {
                    AudioFormat audioFormat = audioInputStream.getFormat();
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                    sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
                    sourceDataLine.open(audioFormat);
                    duration = audioInputStream.available() / audioFormat.getSampleRate() / (audioFormat.getSampleSizeInBits() / 8.0) / audioFormat.getChannels();
                } catch (LineUnavailableException e) {
                    LOG.log(Level.WARNING, e.getMessage());
                    if (alive) {
                        Pandora.pandoraPlayer.setPlayerState(PandoraPlayer.PlayerState.WAITING_FOR_NEXT_SONG);
                    }
                    return;
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, e.getMessage());
                    if (alive) {
                        Pandora.pandoraPlayer.setPlayerState(PandoraPlayer.PlayerState.WAITING_FOR_NEXT_SONG);
                    }
                }
                sourceDataLine.start();
                byte[] data = new byte[256];
                try {
                    int bytesRead = 0;
                    while (bytesRead != -1 && alive) {
                        while (paused) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                LOG.log(Level.SEVERE, e.getMessage());
                                if (alive) {
                                    Pandora.pandoraPlayer.setPlayerState(PandoraPlayer.PlayerState.WAITING_FOR_NEXT_SONG);
                                }
                                return;
                            }
                        }
                        bytesRead = audioInputStream.read(data, 0, data.length);
                        updateProgressInformation(audioInputStream);
                        adjustVolumeIfNeeded(sourceDataLine);
                        if (bytesRead >= 0) sourceDataLine.write(data, 0, bytesRead);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (alive) {
                        Pandora.pandoraPlayer.setPlayerState(PandoraPlayer.PlayerState.WAITING_FOR_NEXT_SONG);
                    }
                    alive = false;
                    return;
                } finally {
                    sourceDataLine.drain();
                    sourceDataLine.close();
                    if (alive) {
                        Pandora.pandoraPlayer.setPlayerState(PandoraPlayer.PlayerState.WAITING_FOR_NEXT_SONG);
                    }
                }
                if (alive) {
                    Pandora.pandoraPlayer.setPlayerState(PandoraPlayer.PlayerState.WAITING_FOR_NEXT_SONG);
                }
            }
        };
        musicPlayerThread.start();
    }

    private void updateProgressInformation(AudioInputStream audioInputStream) throws IOException {
        available = audioInputStream.available();
        musicPlayerBar.setValue((int) getProgress());
        String duration = getFormattedSecondString((int) (getDuration() * getProgress() / 100)) + " / " + getFormattedSecondString((int) getDuration());
        musicPlayerBar.setString(duration);
    }

    private void adjustVolumeIfNeeded(SourceDataLine sourceDataLine) {
        if (sourceDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl volume = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            volume.setValue(volume.getMinimum() + ((volume.getMaximum() - volume.getMinimum()) * Pandora.volume / 100));
        }
        if (sourceDataLine.isControlSupported(FloatControl.Type.VOLUME)) {
            FloatControl volume = (FloatControl) sourceDataLine.getControl(FloatControl.Type.VOLUME);
            volume.setValue(volume.getMinimum() + ((volume.getMaximum() - volume.getMinimum()) * Pandora.volume / 100));
        }
    }

    public void play() throws FileNotFoundException {
        if (paused) {
            paused = false;
        } else {
            beginPlaying();
        }
    }

    private String getFormattedSecondString(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds - (minutes * 60);
        return "" + minutes + ":" + ((remainingSeconds / 10 == 0) ? "0" : "") + remainingSeconds;
    }

    public void stop() {
        alive = false;
    }

    public boolean isAlive() {
        if (musicPlayerThread == null) {
            return false;
        }
        return musicPlayerThread.isAlive();
    }

    public void pause() {
        paused = true;
    }

    public boolean isPaused() {
        return paused;
    }

    public double getProgress() {
        return ((1.0 - available / total) * 100.0);
    }

    public double getDuration() {
        return duration;
    }
}
