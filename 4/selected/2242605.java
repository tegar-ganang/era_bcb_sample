package eu.jacquet80.rds.input;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioFileBitReader extends BitReader {

    private final AudioInputStream ais;

    private final int frameSize;

    private int prevClock = 0;

    public AudioFileBitReader(File file) throws IOException {
        try {
            ais = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException e) {
            throw new IOException(e.toString());
        }
        AudioFormat format = ais.getFormat();
        format = new AudioFormat(format.getEncoding(), format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate(), true);
        System.out.println("length = " + ais.getFrameLength() + " samples, format: " + format);
        frameSize = format.getFrameSize();
    }

    public boolean getBit() throws IOException {
        byte[] smpl = new byte[frameSize];
        int data, clock = -1;
        do {
            prevClock = clock;
            ais.read(smpl, 0, frameSize);
            data = (0xFF & (int) smpl[0]) + ((int) smpl[1]) * 256;
            clock = (0xFF & (int) smpl[2]) + ((int) smpl[3]) * 256;
        } while (!(prevClock >= 0 && clock < 0));
        return data > 0;
    }
}
