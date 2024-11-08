package tfri;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

public class SoundPlayer {

    public static void main(String[] args) throws IOException, SAPIException, UnsupportedAudioFileException, LineUnavailableException {
        Sink sink = new Sink();
        sink.OpenRBNBConnection("localhost:3333", "Listener");
        ChannelMap cmap = new ChannelMap();
        cmap.Add("AUDIO/audioX");
        sink.Subscribe(cmap, 0, 1, "newest");
        ChannelMap lmap = sink.Fetch(1000000);
        byte voiceData[] = lmap.GetDataAsByteArray(0)[0];
        System.out.println(voiceData.length);
        play(voiceData);
    }

    protected static void writeToFile(String filename, byte[] voiceData) throws IOException {
        File audioFile = new File(filename);
        ByteArrayInputStream baiStream = new ByteArrayInputStream(voiceData);
        AudioInputStream aiStream = new AudioInputStream(baiStream, GenericSoundSource.BLOCK_FORMAT, voiceData.length);
        AudioSystem.write(aiStream, AudioFileFormat.Type.AU, audioFile);
        aiStream.close();
        baiStream.close();
    }

    protected static void play(final byte[] audio) {
        new Thread() {

            public void run() {
                try {
                    AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(audio), GenericSoundSource.BLOCK_FORMAT, GenericSoundSource.NUM_SAMPLES);
                    AudioFormat audioFormat = in.getFormat();
                    AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioFormat.getSampleRate(), 16, audioFormat.getChannels(), audioFormat.getChannels() * 2, audioFormat.getSampleRate(), false);
                    AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in);
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(decodedFormat);
                    rawplay(din, line);
                    in.close();
                } catch (Exception e) {
                }
            }
        }.start();
    }

    private static void rawplay(AudioInputStream din, SourceDataLine line) throws IOException {
        line.start();
        byte[] data = new byte[4096];
        int nBytesRead = 0;
        while (nBytesRead != -1) {
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
