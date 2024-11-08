package src.engine;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import src.eleconics.Utilities;

public class AudioPlayer implements Runnable {

    protected int category;

    protected SourceDataLine line = null;

    protected FloatControl gainControl;

    public AudioPlayer(int category) {
        this.category = category;
    }

    public void run() {
        AudioEngine.players.add(this);
        playOgg(((String) AudioEngine.sounds[category].get(Utilities.getRandom(0, AudioEngine.sounds[category].size()))));
        AudioEngine.players.remove(this);
    }

    public void playOgg(String filename) {
        try {
            File file = new File(filename);
            AudioInputStream in = AudioSystem.getAudioInputStream(file);
            AudioInputStream din = null;
            if (in != null) {
                AudioFormat baseFormat = in.getFormat();
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                din = AudioSystem.getAudioInputStream(decodedFormat, in);
                rawplay(decodedFormat, din);
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
        byte[] data = new byte[4096];
        line = getLine(targetFormat);
        if (line != null) {
            gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            if (category == AudioEngine.MUSIC) {
                gainControl.setValue(AudioEngine.musicGain);
            } else {
                gainControl.setValue(AudioEngine.fxGain);
            }
            line.start();
            int nBytesRead = 0;
            while (nBytesRead != -1 && !AudioEngine.stopPlaying) {
                nBytesRead = din.read(data, 0, data.length);
                if (nBytesRead != -1) {
                    line.write(data, 0, nBytesRead);
                }
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
