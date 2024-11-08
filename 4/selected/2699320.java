package decoder;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Port;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;

public class Test {

    int sampleRate = 44100;

    int channels = 2;

    float fadeTimeSeconds = 10.0f;

    public void test() throws IOException {
        FileInputStream in = new FileInputStream("/home/marc/tmp/test1.raw");
        FileOutputStream out = new FileOutputStream("/home/marc/tmp/javatest.raw");
        try {
            int sample = 0;
            while (true) {
                short left = EndianUtils.readSwappedShort(in);
                short right = EndianUtils.readSwappedShort(in);
                int zeroSample = Math.round((float) sampleRate * fadeTimeSeconds);
                if (sample > zeroSample) break;
                float x = 1 - (float) sample / (float) zeroSample;
                short newLeft = (short) Math.round((float) left * x);
                short newRight = (short) Math.round((float) right * x);
                EndianUtils.writeSwappedShort(out, newLeft);
                EndianUtils.writeSwappedShort(out, newRight);
                sample++;
            }
        } catch (EOFException e) {
        } finally {
            in.close();
            out.close();
        }
    }

    public void test2() throws UnsupportedAudioFileException, IOException {
        System.out.println(Arrays.toString(AudioSystem.getAudioFileTypes()));
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("/home/marc/tmp/test1.ogg"));
        System.out.println(audioInputStream);
        audioInputStream = AudioSystem.getAudioInputStream(new File("/home/marc/tmp/test1.mp3"));
        System.out.println(audioInputStream);
    }

    public void test3() throws FileNotFoundException, IOException {
        Decoder decoder1 = new MP3Decoder(new FileInputStream("/home/marc/tmp/test1.mp3"));
        Decoder decoder2 = new OggDecoder(new FileInputStream("/home/marc/tmp/test1.ogg"));
        FileOutputStream out = new FileOutputStream("/home/marc/tmp/test.pipe");
        IOUtils.copy(decoder1, out);
        IOUtils.copy(decoder2, out);
    }

    public static void main(String[] args) throws Exception {
        new Test().test3();
    }
}
