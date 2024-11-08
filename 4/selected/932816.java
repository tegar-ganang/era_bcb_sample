package au.vermilion.samplebank;

import java.io.File;
import java.io.FileInputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;

public class Mp3StreamHandler implements IStreamHandler {

    /**
     * The MPEG audio bitstream. 
     */
    private Bitstream bitstream;

    /**
     * The MPEG audio decoder. 
     */
    private Decoder decoder;

    private long mp3Length = 0;

    private int mp3Chan = 0;

    private int mp3Rate = 0;

    /**
     * Creates a new <code>Player</code> instance. 
     */
    public Mp3StreamHandler(File inFile) throws JavaLayerException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(inFile);
            bitstream = new Bitstream(fis);
            decoder = new Decoder();
            Header h = bitstream.readFrame();
            while (h != null) {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
                if (mp3Chan == 0) mp3Chan = decoder.getOutputChannels();
                if (mp3Rate == 0) mp3Rate = decoder.getOutputFrequency();
                mp3Length += (output.getBufferLength() / mp3Chan);
                bitstream.closeFrame();
                h = bitstream.readFrame();
            }
            fis.close();
            fis = new FileInputStream(inFile);
            bitstream = new Bitstream(fis);
            decoder = new Decoder();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void loadSample(float[][] sampleData) {
        try {
            Header h = bitstream.readFrame();
            int pos = 0;
            while (h != null) {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
                int len = output.getBufferLength();
                short[] s = output.getBuffer();
                for (int x = 0; x < len; x += mp3Chan) {
                    sampleData[0][pos] = s[x] / 32768.0f;
                    sampleData[1][pos] = s[x + 1] / 32768.0f;
                    pos++;
                }
                bitstream.closeFrame();
                h = bitstream.readFrame();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public long getFrameLength() {
        return mp3Length;
    }

    @Override
    public int getChannels() {
        return mp3Chan;
    }

    @Override
    public int getSampleRate() {
        return mp3Rate;
    }

    @Override
    public boolean isOpen() {
        return true;
    }
}
