import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundPlayer {

    private String filename;

    public SoundPlayer(String wavfile) {
        filename = wavfile;
    }

    public void play() {
        File soundFile = new File(filename);
        if (!soundFile.exists()) {
            System.err.println("OGG file not found: " + filename);
            return;
        }
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(soundFile);
        } catch (UnsupportedAudioFileException e1) {
            e1.printStackTrace();
            return;
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
        AudioInputStream decodedInputStream = null;
        try {
            if (audioInputStream != null) {
                AudioFormat base_format = audioInputStream.getFormat();
                AudioFormat decoded_format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, base_format.getSampleRate(), 16, base_format.getChannels(), base_format.getChannels() * 2, base_format.getSampleRate(), false);
                decodedInputStream = AudioSystem.getAudioInputStream(decoded_format, audioInputStream);
                rawplay(decoded_format, decodedInputStream);
                audioInputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
        byte[] data = new byte[4096];
        SourceDataLine line = getLine(targetFormat);
        if (line != null) {
            line.start();
            int nBytesRead = 0, nBytesWritten = 0;
            while (nBytesRead != -1) {
                nBytesRead = din.read(data, 0, data.length);
                if (nBytesRead != -1) nBytesWritten = line.write(data, 0, nBytesRead);
            }
            line.drain();
            line.stop();
            line.close();
            din.close();
        }
    }

    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }
}
