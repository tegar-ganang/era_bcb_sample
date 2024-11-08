package net.java.sip.communicator.impl.media.codec.audio.alaw;

import javax.media.*;
import javax.media.format.*;

/**
 * The ALAW Encoder
 *
 * @author Damian Minkov
 */
public class JavaEncoder extends com.ibm.media.codec.audio.AudioCodec {

    private Format lastFormat = null;

    private int numberOfInputChannels;

    private int numberOfOutputChannels = 1;

    private boolean downmix = false;

    private int inputSampleSize;

    private boolean bigEndian = false;

    public JavaEncoder() {
        supportedInputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 16, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED), new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 16, 2, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED), new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 8, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED), new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 8, 2, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED) };
        defaultOutputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.ALAW, 8000, 8, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED) };
        PLUGIN_NAME = "pcm to alaw converter";
    }

    protected Format[] getMatchingOutputFormats(Format in) {
        AudioFormat inFormat = (AudioFormat) in;
        int channels = inFormat.getChannels();
        int sampleRate = (int) (inFormat.getSampleRate());
        if (channels == 2) {
            supportedOutputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.ALAW, sampleRate, 8, 2, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED), new AudioFormat(AudioFormat.ALAW, sampleRate, 8, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED) };
        } else {
            supportedOutputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.ALAW, sampleRate, 8, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED) };
        }
        return supportedOutputFormats;
    }

    public void open() throws ResourceUnavailableException {
    }

    public void close() {
    }

    private int calculateOutputSize(int inputLength) {
        if (inputSampleSize == 16) {
            inputLength /= 2;
        }
        if (downmix) {
            inputLength /= 2;
        }
        return inputLength;
    }

    /**
     * Init the converter to the new format
     * @param inFormat AudioFormat
     */
    private void initConverter(AudioFormat inFormat) {
        lastFormat = inFormat;
        numberOfInputChannels = inFormat.getChannels();
        if (outputFormat != null) {
            numberOfOutputChannels = outputFormat.getChannels();
        }
        inputSampleSize = inFormat.getSampleSizeInBits();
        bigEndian = inFormat.getEndian() == AudioFormat.BIG_ENDIAN;
        if ((numberOfInputChannels == 2) && (numberOfOutputChannels == 1)) {
            downmix = true;
        } else {
            downmix = false;
        }
    }

    /**
     * Encodes the input buffer passing it to the output one
     * @param inputBuffer Buffer
     * @param outputBuffer Buffer
     * @return int
     */
    public int process(Buffer inputBuffer, Buffer outputBuffer) {
        if (!checkInputBuffer(inputBuffer)) {
            return BUFFER_PROCESSED_FAILED;
        }
        if (isEOM(inputBuffer)) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }
        Format newFormat = inputBuffer.getFormat();
        if (lastFormat != newFormat) {
            initConverter((AudioFormat) newFormat);
        }
        int inpLength = inputBuffer.getLength();
        int outLength = calculateOutputSize(inputBuffer.getLength());
        byte[] inpData = (byte[]) inputBuffer.getData();
        byte[] outData = validateByteArraySize(outputBuffer, outLength);
        pcm162alaw(inpData, inputBuffer.getOffset(), outData, 0, outData.length, bigEndian);
        updateOutput(outputBuffer, outputFormat, outLength, 0);
        return BUFFER_PROCESSED_OK;
    }

    private static final byte QUANT_MASK = 0xf;

    private static final byte SEG_SHIFT = 4;

    private static final short[] seg_end = { 0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF, 0x3FFF, 0x7FFF };

    private static byte linear2alaw(short pcm_val) {
        byte mask;
        byte seg = 8;
        byte aval;
        if (pcm_val >= 0) {
            mask = (byte) 0xD5;
        } else {
            mask = 0x55;
            pcm_val = (short) (-pcm_val - 8);
        }
        for (int i = 0; i < 8; i++) {
            if (pcm_val <= seg_end[i]) {
                seg = (byte) i;
                break;
            }
        }
        if (seg >= 8) {
            return (byte) ((0x7F ^ mask) & 0xFF);
        } else {
            aval = (byte) (seg << SEG_SHIFT);
            if (seg < 2) {
                aval |= (pcm_val >> 4) & QUANT_MASK;
            } else {
                aval |= (pcm_val >> (seg + 3)) & QUANT_MASK;
            }
            return (byte) ((aval ^ mask) & 0xFF);
        }
    }

    /**
     * Converts the input buffer to the otput one using the alaw codec
     * @param inBuffer byte[]
     * @param inByteOffset int
     * @param outBuffer byte[]
     * @param outByteOffset int
     * @param sampleCount int
     * @param bigEndian boolean
     */
    private static void pcm162alaw(byte[] inBuffer, int inByteOffset, byte[] outBuffer, int outByteOffset, int sampleCount, boolean bigEndian) {
        int shortIndex = inByteOffset;
        int alawIndex = outByteOffset;
        if (bigEndian) {
            while (sampleCount > 0) {
                outBuffer[alawIndex++] = linear2alaw(bytesToShort16(inBuffer[shortIndex], inBuffer[shortIndex + 1]));
                shortIndex++;
                shortIndex++;
                sampleCount--;
            }
        } else {
            while (sampleCount > 0) {
                outBuffer[alawIndex++] = linear2alaw(bytesToShort16(inBuffer[shortIndex + 1], inBuffer[shortIndex]));
                shortIndex++;
                shortIndex++;
                sampleCount--;
            }
        }
    }

    /**
     * Converts the 2 bytes to the corresponding short value
     * @param highByte byte
     * @param lowByte byte
     * @return short
     */
    private static short bytesToShort16(byte highByte, byte lowByte) {
        return (short) ((highByte << 8) | (lowByte & 0xFF));
    }
}
