package es.eucm.eadventure.engine.multimedia;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import es.eucm.eadventure.engine.resourcehandler.ResourceHandler;

public class SoundMp3 extends Sound {

    /**
     * Format to decode the sound
     */
    private AudioFormat decodedFormat;

    /**
     * Input stream with the decoded mp3
     */
    private AudioInputStream audioInputStream;

    /**
     * MP3 file
     */
    private SourceDataLine line;

    /**
     * Filename of the mp3 file
     */
    private String filename;

    /**
     * Creates a new SoundMp3
     * <p>
     * If any error happens, an error message is printed to System.out and this
     * sound is disabled
     * 
     * @param filename
     *            path to the midi file
     * @param loop
     *            defines whether or not the sound must be played in a loop
     */
    public SoundMp3(String filename, boolean loop) {
        super(loop);
        this.filename = filename;
    }

    @Override
    public void playOnce() {
        try {
            InputStream is = ResourceHandler.getInstance().getResourceAsStreamFromZip(filename);
            AudioInputStream ais = AudioSystem.getAudioInputStream(is);
            AudioFormat baseFormat = ais.getFormat();
            decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
            audioInputStream = AudioSystem.getAudioInputStream(decodedFormat, ais);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(decodedFormat);
            if (line != null) {
                try {
                    line.open(decodedFormat);
                    byte[] data = new byte[4096];
                    line.start();
                    int nBytesRead;
                    while (!stop && (nBytesRead = audioInputStream.read(data, 0, data.length)) != -1) {
                        line.write(data, 0, nBytesRead);
                    }
                } catch (IOException e) {
                    stopPlaying();
                    System.out.println("WARNING - could not open \"" + filename + "\" - sound will be disabled");
                } catch (LineUnavailableException e) {
                    stopPlaying();
                    System.out.println("WARNING - audio device is unavailable to play \"" + filename + "\" - sound will be disabled");
                }
            } else stopPlaying();
        } catch (UnsupportedAudioFileException e) {
            finalize();
            System.out.println("WARNING - \"" + filename + "\" is a no supported MP3 file - sound will be disabled");
        } catch (IOException e) {
            finalize();
            System.out.println("WARNING - could not open \"" + filename + "\" - sound will be disabled");
        } catch (LineUnavailableException e) {
            finalize();
            System.out.println("WARNING - audio device is unavailable to play \"" + filename + "\" - sound will be disabled");
        }
    }

    @Override
    public synchronized void stopPlaying() {
        super.stopPlaying();
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
        }
        if (audioInputStream != null) {
            try {
                audioInputStream.close();
            } catch (IOException e) {
                System.out.println("WARNING - could not close \"" + filename + "\" - sound will be disabled");
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void finalize() {
        line = null;
        audioInputStream = null;
    }
}
