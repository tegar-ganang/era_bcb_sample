package com.cookeroo.media;

import java.io.StreamCorruptedException;
import java.nio.BufferOverflowException;
import java.util.Vector;
import com.cookeroo.threads.ThreadState;

/**
 * An audio decoder for the Speex codec.
 * @author Thomas Quintana
 */
public class SpeexDecoder implements DecoderInterface, Runnable {

    private Vector<byte[]> speexPackets = null;

    private boolean running = false;

    private boolean decoding = false;

    org.xiph.speex.SpeexDecoder decoder = null;

    /**
	 * Creates a new Speex decoder.
	 * @param mode The mode of the decoder (0=NB, 1=WB, 2=UWB).
	 * @param sampleRate The number of samples per second.
	 * @param channels The number of audio channels (1=mono, 2=stereo, ...).
	 */
    public SpeexDecoder(int mode, int sampleRate, int channels) {
        this.speexPackets = new Vector<byte[]>(100);
        this.decoder = new org.xiph.speex.SpeexDecoder();
        decoder.init(mode, sampleRate, channels, true);
        this.running = true;
        Thread decoderThread = new Thread(this);
        decoderThread.setName("Speex Decoder");
        decoderThread.start();
    }

    /**
	 * @see com.cookeroo.media.CodecInterface#close()
	 */
    @Override
    public void close() {
        this.running = false;
    }

    /**
	 * @see com.cookeroo.media.DecoderInterface#decode(byte[])
	 */
    @Override
    public void decode(byte frame[]) throws BufferOverflowException {
        this.speexPackets.addElement(frame);
    }

    /**
	 * @see com.cookeroo.media.DecoderInterface#getAvailableSampleLength()
	 */
    @Override
    public int getAvailableSampleLength() {
        return this.decoder.getProcessedDataByteSize() / 2;
    }

    /**
	 * @see com.cookeroo.media.CodecInterface#getAvailableByteLength()
	 */
    @Override
    public int getAvailableByteLength() {
        return this.decoder.getProcessedDataByteSize();
    }

    /**
	 * @see com.cookeroo.media.CodecInterface#getBytes()
	 */
    @Override
    public byte[] getBytes() {
        byte availableSamples[];
        synchronized (this.decoder) {
            availableSamples = new byte[this.decoder.getProcessedDataByteSize()];
            this.decoder.getProcessedData(availableSamples, 0);
        }
        return availableSamples;
    }

    /**
	 * @see com.cookeroo.media.CodecInterface#getChannels()
	 */
    @Override
    public int getChannels() {
        return this.decoder.getChannels();
    }

    /**
	 * Not implemented always returns null.
	 */
    @Override
    public int[] getInts() {
        return null;
    }

    /**
	 * @see com.cookeroo.media.CodecInterface#getSampleRate()
	 */
    @Override
    public int getSampleRate() {
        return this.decoder.getSampleRate();
    }

    /**
	 * @see com.cookeroo.media.CodecInterface#getSampleSize()
	 */
    @Override
    public int getSampleSize() {
        return 16;
    }

    /**
	 * @see com.cookeroo.media.CodecInterface#getShorts()
	 */
    @Override
    public short[] getShorts() {
        short availableSamples[];
        synchronized (this.decoder) {
            availableSamples = new short[this.decoder.getProcessedDataByteSize() / 2];
            this.decoder.getProcessedData(availableSamples, 0);
        }
        return availableSamples;
    }

    /**
	 * @see com.cookeroo.media.DecoderInterface#isDecoding()
	 */
    public boolean isDecoding() {
        return this.decoding;
    }

    /**
	 * @see com.cookeroo.media.CodecInterface#isRunning()
	 */
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public void run() {
        while (this.running) {
            while (this.decoding) {
                if (this.speexPackets.size() > 0) {
                    try {
                        synchronized (this.decoder) {
                            int packetCount = this.speexPackets.size();
                            for (int counter = 0; counter < packetCount; counter++) {
                                this.decoder.processData(this.speexPackets.firstElement(), 0, this.speexPackets.firstElement().length);
                                this.speexPackets.remove(0);
                            }
                        }
                    } catch (StreamCorruptedException exception) {
                        this.decoding = false;
                        this.running = false;
                        break;
                    }
                }
            }
            ThreadState.sleep(100);
        }
    }

    /**
	 * @see com.cookeroo.media.CodecInterface#start()
	 */
    @Override
    public void start() {
        this.decoding = true;
    }

    /**
	 * @see com.cookeroo.media.CodecInterface#stop()
	 */
    @Override
    public void stop() {
        this.decoding = false;
    }
}
