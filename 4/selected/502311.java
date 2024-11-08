package apps;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/** 
 * Play.java
 * <p>
 * A simple class that plays audio from given file names.
 * <p>
 * Uses the Java Sound SourceDataLine interface to stream the sound. 
 * Converts compressed encodings (ALAW, ULAW, MP3) to PCM.
 * @author Dan Becker, beckerdo@io.com
 */
public class SndPlayer {

    /** 
     * Plays audio from given file names.
     * @param args Command line parameters
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("SndPlayer usage:");
            System.out.println("\tjava SndPlayer <sound file names>*");
            System.exit(0);
        }
        for (int i = 0; i < args.length; i++) playAudioFile(args[i]);
        System.exit(0);
    }

    /** 
     * Play audio from the given file name. 
     * @param fileName  The file to play
     */
    public static void playAudioFile(String fileName) {
        File soundFile = new File(fileName);
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            playAudioStream(audioInputStream);
        } catch (Exception e) {
            System.out.println("Problem with file " + fileName + ":");
            e.printStackTrace();
        }
    }

    /** 
     * Plays audio from the given audio input stream. 
     * @param audioInputStream  The audio stream to play
     */
    public static void playAudioStream(AudioInputStream audioInputStream) {
        AudioFormat audioFormat = audioInputStream.getFormat();
        System.out.println("Play input audio format=" + audioFormat);
        if (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            AudioFormat newFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioFormat.getSampleRate(), 16, audioFormat.getChannels(), audioFormat.getChannels() * 2, audioFormat.getSampleRate(), false);
            System.out.println("Converting audio format to " + newFormat);
            AudioInputStream newStream = AudioSystem.getAudioInputStream(newFormat, audioInputStream);
            audioFormat = newFormat;
            audioInputStream = newStream;
        }
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Play.playAudioStream does not handle this type of audio on this system.");
            return;
        }
        try {
            SourceDataLine dataLine = (SourceDataLine) AudioSystem.getLine(info);
            dataLine.open(audioFormat);
            if (dataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl volume = (FloatControl) dataLine.getControl(FloatControl.Type.MASTER_GAIN);
                volume.setValue(100.0F);
            }
            dataLine.start();
            int bufferSize = (int) audioFormat.getSampleRate() * audioFormat.getFrameSize();
            byte[] buffer = new byte[bufferSize];
            try {
                int bytesRead = 0;
                while (bytesRead >= 0) {
                    bytesRead = audioInputStream.read(buffer, 0, buffer.length);
                    if (bytesRead >= 0) {
                        dataLine.write(buffer, 0, bytesRead);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Play.playAudioStream draining line.");
            dataLine.drain();
            System.out.println("Play.playAudioStream closing line.");
            dataLine.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
