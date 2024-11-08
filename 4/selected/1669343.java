package com.cookeroo.io;

import java.io.IOException;
import java.io.InputStream;
import com.cookeroo.media.DecoderInterface;
import com.cookeroo.threads.ThreadState;
import com.cookeroo.util.CircularBuffer;

/**
 * The <code>DecoderInputStream</code> class wraps around decoders
 * to provide an <code>InputStream</code> from the decoder output.
 * @author Thomas Quintana
 */
public class DecoderInputStream extends InputStream {

    private CircularBuffer<Byte> buffer = null;

    private DecoderInterface decoder = null;

    private boolean endOfStream = false;

    /**
	 * Creates a new <code>DecoderInputStream</code>
	 * @param decoder
	 */
    public DecoderInputStream(DecoderInterface decoder) {
        this.decoder = decoder;
        this.buffer = new CircularBuffer<Byte>(new Byte[2 * decoder.getSampleRate() * (decoder.getSampleSize() / 8) * decoder.getChannels()]);
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public synchronized int read() throws IOException {
        while (this.buffer.getElementCount() == 0 && !this.endOfStream) {
            if (this.decoder.getAvailableByteLength() > 0) {
                byte data[] = this.decoder.getBytes();
                for (int counter = 0; counter < data.length; counter++) this.buffer.put(new Byte(data[counter]));
            } else if (!this.decoder.isRunning()) {
                this.endOfStream = true;
                return -1;
            } else ThreadState.sleep(100);
        }
        return this.buffer.get().byteValue();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead;
        if (len == 0) return 0;
        for (bytesRead = 0; bytesRead < len; bytesRead++) if (!this.endOfStream) b[off + bytesRead] = (byte) this.read(); else break;
        if (this.endOfStream && bytesRead == 0) return -1;
        return bytesRead;
    }
}
