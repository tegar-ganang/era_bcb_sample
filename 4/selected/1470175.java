package info.wisl;

import java.io.*;
import java.util.*;
import javax.sound.sampled.*;
import javazoom.spi.vorbis.sampled.convert.*;
import javazoom.spi.vorbis.sampled.file.*;

/**
 * Container for SourceDataLine for each SoundModule.  Used to ensure that
 * related Sounds are played sequentially and mixed.
 *
 * @author Thomas Hildebrandt
 */
public class SoundPlayer implements Runnable {

    public static final String[] supportedFiles = { "ogg", "wav", "au", "aiff", "rmf" };

    public static boolean isMuted = false;

    private boolean die = false;

    private static final int EXTERNAL_BUFFER_SIZE = 4096;

    private byte[] audioBuffer;

    private Vector toBePlayed;

    private Runnable onDone = null;

    /**
     * Constructor.
     */
    public SoundPlayer() {
        audioBuffer = new byte[EXTERNAL_BUFFER_SIZE];
        toBePlayed = new Vector();
    }

    /**
     * Constructor for testing the play of a sound
     */
    public SoundPlayer(String filename, Runnable onDone) throws SoundException {
        this();
        this.onDone = onDone;
        Sound sound = new Sound(filename);
        toBePlayed.add(sound);
    }

    /**
     * From VorbisSPI sample code
     */
    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    private void closeAudioStreams(AudioInputStream ais1, AudioInputStream ais2) {
        if (ais1 != null) {
            try {
                ais1.close();
            } catch (IOException ioe) {
            }
        }
        if (ais2 != null) {
            try {
                ais2.close();
            } catch (IOException ioe) {
            }
        }
    }

    /**
     * Plays a sound according to repetitions and volume. Should be called
     * through the synchronized methods.
     *
     * @param sound the Sound to be played
     */
    private void play(Sound sound) {
        AudioInputStream sourceAIS = null;
        try {
            sourceAIS = AudioSystem.getAudioInputStream(sound.soundFile);
        } catch (UnsupportedAudioFileException e) {
            Debugger.print(Wisl.DEBUG_CRITICAL, "Sound file is not valid audio data recognized by " + "the system:\n " + sound.soundFile.getAbsolutePath());
            return;
        } catch (IOException e) {
            Debugger.print(Wisl.DEBUG_CRITICAL, "An I/O exception occurred in opening the file:\n" + sound.soundFile.getAbsolutePath());
            Debugger.print(Wisl.DEBUG_CRITICAL, e.toString());
            return;
        }
        AudioFormat sourceFormat = sourceAIS.getFormat();
        AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
        AudioInputStream decodedAIS = null;
        try {
            decodedAIS = AudioSystem.getAudioInputStream(targetFormat, sourceAIS);
        } catch (IllegalArgumentException iae) {
            Debugger.print(Wisl.DEBUG_CRITICAL, "The playback of the sound file is not supported:\n" + sound.soundFile.getAbsolutePath());
            closeAudioStreams(decodedAIS, sourceAIS);
            return;
        }
        SourceDataLine line = null;
        try {
            line = getLine(targetFormat);
        } catch (LineUnavailableException e) {
            Debugger.print(Wisl.DEBUG_CRITICAL, "Cannot open a SourceDataLine: \n" + e.getMessage());
            closeAudioStreams(decodedAIS, sourceAIS);
            return;
        }
        line.start();
        playloop: for (int repeat = 0; repeat < sound.repetitions; repeat++) {
            if (repeat != 0) {
                closeAudioStreams(decodedAIS, sourceAIS);
                try {
                    sourceAIS = AudioSystem.getAudioInputStream(sound.soundFile);
                } catch (UnsupportedAudioFileException e) {
                    Debugger.print(Wisl.DEBUG_CRITICAL, "Sound file is not valid audio data " + "recognized by the system:\n " + sound.soundFile.getAbsolutePath());
                    return;
                } catch (IOException e) {
                    Debugger.print(Wisl.DEBUG_CRITICAL, "An I/O exception occurred in opening the " + "file:\n" + sound.soundFile.getAbsolutePath());
                    Debugger.print(Wisl.DEBUG_CRITICAL, e.toString());
                    return;
                }
                decodedAIS = AudioSystem.getAudioInputStream(targetFormat, sourceAIS);
            }
            int nBytesRead = 0;
            while (nBytesRead != -1) {
                try {
                    nBytesRead = decodedAIS.read(audioBuffer, 0, audioBuffer.length);
                } catch (IOException e) {
                    Debugger.print(Wisl.DEBUG_CRITICAL, "An I/O exception occurred in reading the " + "file:\n" + sound.soundFile.getAbsolutePath());
                    Debugger.print(Wisl.DEBUG_CRITICAL, e.toString());
                    break playloop;
                }
                if (die) {
                    break playloop;
                }
                if (nBytesRead >= 0) {
                    if ((sound.volume != 100) && !SoundPlayer.isMuted) {
                        convertVolume(audioBuffer, sound.volume, targetFormat.isBigEndian());
                    }
                    if (SoundPlayer.isMuted) {
                        convertVolume(audioBuffer, 0, targetFormat.isBigEndian());
                    }
                    line.write(audioBuffer, 0, nBytesRead);
                }
            }
            line.drain();
        }
        closeAudioStreams(decodedAIS, sourceAIS);
        line.stop();
        line.close();
        die = false;
    }

    /**
     * Plays a sound, with thread serialization.
     *
     * @param sound to be played
     */
    public synchronized void playSound(Sound sound) {
        toBePlayed.add(sound);
    }

    public synchronized void flushQueue() {
        toBePlayed.removeAllElements();
    }

    /**
     * Internal method to get a sound to be played
     */
    private synchronized Sound getSoundToPlay() {
        if (toBePlayed.size() > 0) {
            return (Sound) toBePlayed.remove(0);
        } else {
            return null;
        }
    }

    /**
     * Converts the PCM audio data by the specified volume.
     *
     * @param audioBuffer PCM audio data
     * @param volume volume to be applied, 0-100
     * @param bigEndian indicates whether this audio data is big endian
     */
    private static void convertVolume(byte[] audioBuffer, int volume, boolean bigEndian) {
        short first;
        short second;
        short result;
        double volume_xform = volume / 100.0;
        for (int i = 0; (i + 1) < audioBuffer.length; i += 2) {
            if (bigEndian) {
                first = (short) (audioBuffer[i] << 8);
                second = audioBuffer[i + 1];
            } else {
                first = (short) (audioBuffer[i + 1] << 8);
                second = audioBuffer[i];
            }
            result = (short) (first + second);
            result = (short) (result * volume_xform);
            first = (short) (result >> 8);
            second = (short) (result & 255);
            if (bigEndian) {
                audioBuffer[i] = (byte) first;
                audioBuffer[i + 1] = (byte) second;
            } else {
                audioBuffer[i + 1] = (byte) first;
                audioBuffer[i] = (byte) second;
            }
        }
    }

    public void start() {
        run();
    }

    public void run() {
        SoundPlayerThread spt = new SoundPlayerThread(this);
        spt.start();
    }

    public void kill() {
        die = true;
    }

    public static boolean isFileSupported(String filename) {
        for (int i = 0; i < supportedFiles.length; i++) {
            if (filename.endsWith("." + supportedFiles[i])) return true;
        }
        return false;
    }

    class SoundPlayerThread extends Thread {

        SoundPlayer sp;

        SoundPlayerThread(SoundPlayer sp) {
            this.sp = sp;
        }

        public void run() {
            Sound sound;
            while ((sound = sp.getSoundToPlay()) != null) {
                try {
                    sp.play(sound);
                } catch (Exception e) {
                    Debugger.print(Wisl.DEBUG_CRITICAL, "Error playing sound, Sound device unavailable");
                }
            }
            if (onDone != null) javax.swing.SwingUtilities.invokeLater(onDone);
        }
    }
}
