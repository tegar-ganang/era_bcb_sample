package au.vermilion.samplebank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.FrameListener;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.StreamInfo;

/**
 *
 */
public class FlacStreamHandler implements IStreamHandler, FrameListener {

    private StreamInfo streamInfo;

    private FLACDecoder decoder;

    public FlacStreamHandler(File inFile) {
        try {
            decoder = new FLACDecoder(new FileInputStream(inFile));
            streamInfo = decoder.readStreamInfo();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void loadSample(float[][] sampleData) {
        decoder.addFrameListener(this);
        try {
            decoder.decode();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public long getFrameLength() {
        return streamInfo.getTotalSamples();
    }

    @Override
    public int getChannels() {
        return streamInfo.getChannels();
    }

    @Override
    public int getSampleRate() {
        return streamInfo.getSampleRate();
    }

    @Override
    public boolean isOpen() {
        return (streamInfo != null);
    }

    @Override
    public void processMetadata(Metadata mtdt) {
        System.out.println(mtdt);
    }

    @Override
    public void processFrame(Frame frame) {
        System.out.println(frame);
    }

    @Override
    public void processError(String string) {
        System.out.println(string);
    }
}
