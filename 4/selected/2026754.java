package org.n3rd.carajox.libraries.directoryLibrary.fileAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.FrameListener;
import org.kc7bfi.jflac.frame.Frame;
import org.kc7bfi.jflac.metadata.Metadata;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.n3rd.carajo.library.Codec;

/**
 * 
 *
 * @version @RELEASE@
 */
public class FlacAdapter extends AbstractAdapter {

    private class Listener implements FrameListener {

        @Override
        public void processError(String msg) {
        }

        @Override
        public void processFrame(Frame frame) {
        }

        @Override
        public void processMetadata(Metadata meta) {
            if (meta instanceof StreamInfo) {
                StreamInfo si = (StreamInfo) meta;
                duration = si.getTotalSamples() / si.getSampleRate();
                channels = si.getChannels();
                bitrate = si.getBitsPerSample();
            }
        }
    }

    public FlacAdapter(File file) throws AdapterException {
        super(Codec.FLAC, file);
        FileInputStream is;
        try {
            is = new FileInputStream(file);
            FLACDecoder decoder = new FLACDecoder(is);
            decoder.addFrameListener(new Listener());
            decoder.decode();
        } catch (FileNotFoundException e) {
            throw new AdapterException(e);
        } catch (IOException e) {
            throw new AdapterException(e);
        }
    }
}
