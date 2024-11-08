package com.dukesoftware.utils.musiccontroller;

import java.io.File;
import java.io.IOException;
import javax.media.Time;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.google.common.io.Closeables;

public class SoundUtils {

    public static final Time getDuration(File file) {
        try {
            return new Time((Long) AudioSystem.getAudioFileFormat(file).properties().get("duration") * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return MediaUtils.START;
    }

    public static final void play(String path) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        File file = new File(path);
        if (file.exists()) {
            AudioInputStream in = null;
            try {
                in = AudioSystem.getAudioInputStream(file);
                AudioInputStream din = null;
                AudioFormat baseFormat = in.getFormat();
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                din = AudioSystem.getAudioInputStream(decodedFormat, in);
                rawplay(decodedFormat, din);
            } finally {
                Closeables.closeQuietly(in);
            }
        }
    }

    public static void rawplay(AudioFormat targetFormat, AudioInputStream din) throws LineUnavailableException, IOException {
        SourceDataLine line = null;
        try {
            line = getLine(targetFormat);
            if (line != null) {
                line.start();
                int nBytesRead = 0;
                byte[] data = new byte[4096];
                while (nBytesRead != -1) {
                    nBytesRead = din.read(data, 0, data.length);
                    if (nBytesRead != -1) {
                        line.write(data, 0, nBytesRead);
                    }
                }
            }
        } finally {
            if (line != null) {
                line.drain();
                line.stop();
                line.close();
            }
            Closeables.closeQuietly(din);
        }
    }

    private static SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }
}
