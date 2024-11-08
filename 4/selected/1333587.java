package org.gs.game.gostop.sound;

import java.io.InputStream;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class GameSoundPlayer {

    private static final String OGG_AUDIO_TYPE = "OGG";

    private static final int DEFAULT_BUFFER_SIZE = 4 * 1024;

    private int bufferSize;

    private boolean stopRequested;

    public GameSoundPlayer() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public GameSoundPlayer(int bufferSize) {
        this.bufferSize = bufferSize;
        stopRequested = false;
    }

    public void play(InputStream isSound) {
        try {
            AudioFileFormat aff = AudioSystem.getAudioFileFormat(isSound);
            AudioInputStream ais = AudioSystem.getAudioInputStream(isSound);
            AudioFormat audioFormat;
            AudioInputStream oldAis = null;
            if (OGG_AUDIO_TYPE.equalsIgnoreCase(aff.getType().toString())) {
                AudioFormat baseFormat = ais.getFormat();
                audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                oldAis = ais;
                ais = AudioSystem.getAudioInputStream(audioFormat, ais);
            } else audioFormat = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();
            int nBytesRead = 0;
            byte[] abData = new byte[bufferSize];
            do {
                nBytesRead = ais.read(abData);
                if (nBytesRead > 0) line.write(abData, 0, nBytesRead);
            } while (nBytesRead >= 0 && isStopRequested() == false);
            line.drain();
            line.close();
            ais.close();
            if (oldAis != null) oldAis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void stop() {
        stopRequested = true;
    }

    private synchronized boolean isStopRequested() {
        return stopRequested;
    }

    public static void main(String[] args) throws Exception {
        String dir = "D:/Work/java/projects/gostop/lib/resources/sounds/";
        java.io.FileInputStream fis = new java.io.FileInputStream(dir + "Male1/ddadak1.ogg");
        java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fis);
        new GameSoundPlayer().play(bis);
        bis.close();
    }
}
