package org.eml.MMAX2.gui.sound;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class Player {

    public static void main(String[] args) {
    }

    public static byte[] getBytesFromFile(File file, long offset, long length) throws java.io.IOException {
        InputStream is = new FileInputStream(file);
        if (length > Integer.MAX_VALUE) {
            System.err.println("File too large");
            return null;
        }
        byte[] bytes = new byte[(int) length];
        try {
            is.skip(offset);
        } catch (java.io.IOException ex) {
            System.err.println(ex.getMessage());
            return null;
        }
        try {
            is.read(bytes);
        } catch (java.io.IOException ex) {
            System.err.println(ex.getMessage());
            return null;
        }
        is.close();
        return bytes;
    }

    private static SourceDataLine getLine(AudioFormat audioFormat) {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            res = (SourceDataLine) AudioSystem.getLine(info);
            res.open(audioFormat);
        } catch (javax.sound.sampled.LineUnavailableException ex) {
            System.err.println(ex.getMessage());
            res = null;
        }
        return res;
    }

    public static void playWAVSound(String WAVfileName, long offset, long length) {
        File soundFile = new File(WAVfileName);
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        } catch (javax.sound.sampled.UnsupportedAudioFileException ex) {
            System.err.println(ex.getMessage());
            return;
        } catch (java.io.IOException ex) {
            System.err.println(ex.getMessage());
            return;
        }
        byte[] data = null;
        try {
            data = Player.getBytesFromFile(soundFile, offset, length);
        } catch (java.io.IOException ex) {
            System.err.println(ex.getMessage());
            return;
        }
        if (data == null) {
            System.err.println("Data could not be played");
            return;
        }
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
        DataLine line = null;
        try {
            line = (DataLine) AudioSystem.getLine(info);
        } catch (javax.sound.sampled.LineUnavailableException ex) {
            System.err.println(ex.getMessage());
            return;
        }
        try {
            ((Clip) line).open(audioFormat, data, 0, (int) length);
        } catch (javax.sound.sampled.LineUnavailableException ex) {
            System.err.println(ex.getMessage());
            return;
        }
        ((Clip) line).start();
        int lastFramePos = 0;
        int currentFramePos = 0;
        while (true) {
            currentFramePos = ((Clip) line).getFramePosition();
            if (currentFramePos < lastFramePos) {
                break;
            }
            lastFramePos = currentFramePos;
        }
        ((Clip) line).stop();
        line = null;
    }

    private static void playMP3raw(AudioFormat targetFormat, AudioInputStream din) {
        byte[] data = new byte[4096];
        SourceDataLine line = null;
        line = getLine(targetFormat);
        if (line != null) {
            line.start();
            int nBytesRead = 0, nBytesWritten = 0;
            while (nBytesRead != -1) {
                try {
                    nBytesRead = din.read(data, 0, data.length);
                } catch (java.io.IOException ex) {
                    System.err.println("Error reading sound file: " + ex.getMessage());
                }
                if (nBytesRead != -1) {
                    nBytesWritten = line.write(data, 0, nBytesRead);
                }
            }
            line.drain();
            line.stop();
            line.close();
            try {
                din.close();
            } catch (java.io.IOException ex) {
                System.err.println("Error closing sound file: " + ex.getMessage());
            }
        }
    }

    public static void playMP3Sound(String fileName, String cpBaseDataPath) {
        File file = new File(fileName);
        if (file.isAbsolute() == false) {
            file = null;
            file = new File(cpBaseDataPath + fileName);
        }
        AudioInputStream in = null;
        try {
            in = AudioSystem.getAudioInputStream(file);
        } catch (javax.sound.sampled.UnsupportedAudioFileException ex) {
            System.err.println(ex.getMessage());
            return;
        } catch (java.io.IOException ex) {
            System.err.println(ex.getMessage());
            return;
        }
        AudioInputStream din = null;
        AudioFormat baseFormat = in.getFormat();
        AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
        din = AudioSystem.getAudioInputStream(decodedFormat, in);
        try {
            playMP3raw(decodedFormat, din);
            in.close();
        } catch (java.io.IOException ex) {
            System.err.println("Error playing file: " + ex.getMessage());
        }
    }
}
