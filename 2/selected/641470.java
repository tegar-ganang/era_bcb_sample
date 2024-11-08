package com.flagstone.transform.util.sound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.DataFormatException;
import com.flagstone.transform.MovieTag;
import com.flagstone.transform.coder.LittleDecoder;
import com.flagstone.transform.sound.DefineSound;
import com.flagstone.transform.sound.SoundFormat;
import com.flagstone.transform.sound.SoundStreamBlock;
import com.flagstone.transform.sound.SoundStreamHead2;

/**
 * Decoder for WAV sounds so they can be added to a flash file.
 */
public final class WAVDecoder implements SoundProvider, SoundDecoder {

    /** The binary signature for xIFF files. */
    private static final int[] RIFF = { 82, 73, 70, 70 };

    /** The binary signature for WAV files. */
    private static final int[] WAV = { 87, 65, 86, 69 };

    /** The identifier of a format block. */
    private static final int FMT = 0x20746d66;

    /** The identifier of a data block. */
    private static final int DATA = 0x61746164;

    /** The sound format. */
    private transient SoundFormat format;

    /** The number of sound channels: 1 - mono, 2 - stereo. */
    private transient int numberOfChannels;

    /** The number of sound samples for each channel. */
    private transient int samplesPerChannel;

    /** The rate at which the sound will be played. */
    private transient int sampleRate;

    /** The number of bytes in each sample. */
    private transient int sampleSize;

    /** The sound samples. */
    private transient byte[] sound = null;

    /** The frame rate for the movie. */
    private transient float movieRate;

    /** The number of bytes already streamed. */
    private transient int bytesSent;

    /** {@inheritDoc} */
    public SoundDecoder newDecoder() {
        return new WAVDecoder();
    }

    /** {@inheritDoc} */
    public void read(final File file) throws IOException, DataFormatException {
        read(new FileInputStream(file));
    }

    /** {@inheritDoc} */
    public void read(final URL url) throws IOException, DataFormatException {
        final URLConnection connection = url.openConnection();
        final int fileSize = connection.getContentLength();
        if (fileSize < 0) {
            throw new FileNotFoundException(url.getFile());
        }
        read(url.openStream());
    }

    /** {@inheritDoc} */
    public DefineSound defineSound(final int identifier) {
        return new DefineSound(identifier, format, sampleRate, numberOfChannels, sampleSize, samplesPerChannel, sound);
    }

    /** {@inheritDoc} */
    public DefineSound defineSound(final int identifier, final float duration) {
        return new DefineSound(identifier, format, sampleRate, numberOfChannels, sampleSize, samplesPerChannel, sound);
    }

    /** {@inheritDoc} */
    public MovieTag streamHeader(final float frameRate) {
        movieRate = frameRate;
        return new SoundStreamHead2(format, sampleRate, numberOfChannels, sampleSize, sampleRate, numberOfChannels, sampleSize, (int) (sampleRate / frameRate));
    }

    /** {@inheritDoc} */
    public MovieTag streamSound() {
        final int samplesPerBlock = (int) (sampleRate / movieRate);
        final int bytesPerBlock = samplesPerBlock * sampleSize * numberOfChannels;
        SoundStreamBlock block = null;
        if (bytesSent < sound.length) {
            final int bytesRemaining = sound.length - bytesSent;
            final int numberOfBytes = (bytesRemaining < bytesPerBlock) ? bytesRemaining : bytesPerBlock;
            final byte[] bytes = new byte[numberOfBytes];
            System.arraycopy(sound, bytesSent, bytes, 0, numberOfBytes);
            block = new SoundStreamBlock(bytes);
            bytesSent += numberOfBytes;
        }
        return block;
    }

    /** {@inheritDoc} */
    public void read(final InputStream stream) throws IOException, DataFormatException {
        final LittleDecoder coder = new LittleDecoder(stream);
        for (int i = 0; i < RIFF.length; i++) {
            if (coder.readByte() != RIFF[i]) {
                throw new DataFormatException("Unsupported format");
            }
        }
        coder.readInt();
        for (int i = 0; i < WAV.length; i++) {
            if (coder.readByte() != WAV[i]) {
                throw new DataFormatException("Unsupported format");
            }
        }
        int chunkType;
        int length;
        boolean readFMT = false;
        boolean readDATA = false;
        do {
            chunkType = coder.readInt();
            length = coder.readInt();
            switch(chunkType) {
                case FMT:
                    decodeFMT(coder);
                    readFMT = true;
                    break;
                case DATA:
                    decodeDATA(coder, length);
                    readDATA = true;
                    break;
                default:
                    coder.skip(length);
                    break;
            }
        } while (!(readFMT && readDATA));
    }

    /**
     * Decode the FMT block.
     *
     * @param coder an SWFDecoder containing the bytes to be decoded.
     *
     * @throws IOException if there is an error decoding the data.
     * @throws DataFormatException if the block is in a format not supported
     * by this decoder.
     */
    private void decodeFMT(final LittleDecoder coder) throws IOException, DataFormatException {
        format = SoundFormat.PCM;
        if (coder.readUnsignedShort() != 1) {
            throw new DataFormatException("Unsupported format");
        }
        numberOfChannels = coder.readUnsignedShort();
        sampleRate = coder.readInt();
        coder.readInt();
        coder.readUnsignedShort();
        sampleSize = coder.readUnsignedShort() >> 3;
    }

    /**
     * Decode the Data block containing the sound samples.
     *
     * @param coder an SWFDecoder containing the bytes to be decoded.
     * @param length the length of the block in bytes.
     * @throws IOException if there is an error decoding the data.
     */
    private void decodeDATA(final LittleDecoder coder, final int length) throws IOException {
        samplesPerChannel = length / (sampleSize * numberOfChannels);
        sound = coder.readBytes(new byte[length]);
    }
}
