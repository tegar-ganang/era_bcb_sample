package de.joergjahnke.common.jme;

import de.joergjahnke.common.emulation.WaveDataProducer;
import de.joergjahnke.common.util.Observer;

/**
 * Observes and plays the wave data delivered by a wave data producer.
 * This implementation works for J2ME.
 *
 * @author  Joerg Jahnke (joergjahnke@users.sourceforge.net)
 */
public class NokiaWavePlayer implements Observer {

    /**
     * heaver for the generated .wav format streams
     */
    private static final short[] WAV_HEADER = { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E', 'f', 'm', 't', ' ', 16, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 16, 0, 'd', 'a', 't', 'a', 0, 0, 0, 0 };

    /**
     * number of buffers we use
     */
    private static final int NUM_BUFFERS = 2;

    /**
     * current position in the buf
     */
    private int currentBufferPos = WAV_HEADER.length;

    /**
     * wave data producer
     */
    private final WaveDataProducer producer;

    /**
     * buffers to fill with wave data
     */
    private byte[][] buffers = new byte[NUM_BUFFERS][];

    /**
     * plays the generated sound
     */
    private com.nokia.mid.sound.Sound[] players = new com.nokia.mid.sound.Sound[NUM_BUFFERS];

    /**
     * buffer we currently fill
     */
    private int currentBuffer = 0;

    /**
     * Creates a new instance of WavePlayer
     */
    public NokiaWavePlayer(final WaveDataProducer producer) {
        boolean isWavSupported = false;
        final int[] formats = com.nokia.mid.sound.Sound.getSupportedFormats();
        for (int i = 0; !isWavSupported && i < formats.length; ++i) {
            isWavSupported |= formats[i] == com.nokia.mid.sound.Sound.FORMAT_WAV;
        }
        if (!isWavSupported) {
            throw new RuntimeException("Wav format not supported by this player!");
        }
        this.producer = producer;
        WAV_HEADER[22] = (short) producer.getChannels();
        WAV_HEADER[32] = (short) producer.getChannels();
        WAV_HEADER[34] = (short) producer.getBitsPerSample();
        final int defaultBufferSize = producer.getSampleRate() / 10;
        final int bs = defaultBufferSize + WAV_HEADER.length;
        for (int i = 0, shift = 0; i < 4; ++i, shift += 8) {
            WAV_HEADER[4 + i] = (short) ((bs >> shift) & 0xff);
            WAV_HEADER[40 + i] = (short) ((defaultBufferSize >> shift) & 0xff);
            WAV_HEADER[24 + i] = (short) ((producer.getSampleRate() >> shift) & 0xff);
            WAV_HEADER[28 + i] = (short) (((producer.getSampleRate() * producer.getChannels() * producer.getBitsPerSample() / 8) >> shift) & 0xff);
        }
        for (int i = 0; i < this.buffers.length; ++i) {
            this.buffers[i] = new byte[bs];
            initBuffer(this.buffers[i]);
            this.players[i] = new com.nokia.mid.sound.Sound(this.buffers[i], com.nokia.mid.sound.Sound.FORMAT_WAV);
        }
    }

    /**
     * Copy wave header to buffer
     */
    private final void initBuffer(final byte[] buffer) {
        for (int i = 0; i < WAV_HEADER.length; ++i) {
            buffer[i] = (byte) (WAV_HEADER[i] & 0xff);
        }
    }

    public void update(final Object observed, final Object obj) {
        if (observed instanceof WaveDataProducer) {
            final byte[] buf = (byte[]) obj;
            update(buf, 0, buf.length);
        }
    }

    /**
     * fill current buf with bytes delivered
     *
     * @param   buf  buf with data to copy
     * @param   offset  offset where to start copying
     * @param   numBytes    number of bytes to copy
     */
    public void update(final byte[] buffer, final int offset, final int numBytes) {
        final byte[] cb = this.buffers[this.currentBuffer];
        final int n = Math.min(numBytes, cb.length - this.currentBufferPos);
        synchronized (this) {
            System.arraycopy(buffer, offset, cb, this.currentBufferPos, n);
            this.currentBufferPos += n;
            if (this.currentBufferPos >= cb.length) {
                final com.nokia.mid.sound.Sound player = this.players[this.currentBuffer];
                player.init(cb, com.nokia.mid.sound.Sound.FORMAT_WAV);
                player.play(1);
                ++this.currentBuffer;
                this.currentBuffer %= this.buffers.length;
                this.currentBufferPos = WAV_HEADER.length;
            }
        }
        if (n < numBytes) {
            update(buffer, offset + n, numBytes - n);
        }
    }
}
