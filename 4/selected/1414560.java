package es.eucm.eadventure.editor.gui.audio;

import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import es.eucm.eadventure.common.auxiliar.ReportDialog;
import es.eucm.eadventure.editor.control.controllers.AssetsController;

/**
 * This class is an implementation for Sound that is able to play mp3 sounds.
 */
public class SoundMp3 extends Sound {

    /**
     * Filename of the mp3 file.
     */
    private String filename;

    /**
     * Format to decode the sound.
     */
    private AudioFormat decodedFormat;

    /**
     * Input stream with the decoded mp3.
     */
    private AudioInputStream audioInputStream;

    /**
     * Source of the MP3 data.
     */
    private SourceDataLine line;

    /**
     * Tells if the sound must stop or not.
     */
    private boolean stop;

    /**
     * Creates a new SoundMp3
     * <p>
     * If any error happens, an error message is printed to System.out and this
     * sound is disabled
     * 
     * @param filename
     *            path to the midi file
     */
    public SoundMp3(String filename) {
        super();
        this.filename = filename;
        stop = false;
        try {
            InputStream is = AssetsController.getInputStream(filename);
            AudioInputStream ais = AudioSystem.getAudioInputStream(is);
            AudioFormat baseFormat = ais.getFormat();
            decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
            audioInputStream = AudioSystem.getAudioInputStream(decodedFormat, ais);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(decodedFormat);
        } catch (UnsupportedAudioFileException e) {
            System.err.println("WARNING - \"" + filename + "\" is a no supported MP3 file - sound will be disabled");
        } catch (IOException e) {
            System.err.println("WARNING - could not open \"" + filename + "\" - sound will be disabled");
        } catch (LineUnavailableException e) {
            System.err.println("WARNING - audio device is unavailable to play \"" + filename + "\" - sound will be disabled");
        }
    }

    @Override
    public void playOnce() {
        if (line != null) {
            try {
                line.open(decodedFormat);
                byte[] data = new byte[4096];
                line.start();
                int nBytesRead;
                while (!stop && (nBytesRead = audioInputStream.read(data, 0, data.length)) != -1) line.write(data, 0, nBytesRead);
            } catch (IOException e) {
                stopPlaying();
                System.out.println("WARNING - could not open \"" + filename + "\" - sound will be disabled");
            } catch (LineUnavailableException e) {
                stopPlaying();
                System.out.println("WARNING - audio device is unavailable to play \"" + filename + "\" - sound will be disabled");
            }
        } else stopPlaying();
    }

    @Override
    public void stopPlaying() {
        stop = true;
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
        }
        if (audioInputStream != null) {
            try {
                audioInputStream.close();
            } catch (IOException e) {
                ReportDialog.GenerateErrorReport(e, false, "WARNING - could not close \"" + filename + "\" - sound will be disabled");
            }
        }
    }
}
