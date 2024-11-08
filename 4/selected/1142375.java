package net.java.sip.communicator.impl.media.codec.audio.speex;

import java.io.*;
import javax.media.*;
import javax.media.format.*;
import org.xiph.speex.*;
import net.java.sip.communicator.impl.media.codec.*;

/**
 * Speex to PCM java decoder
 * @author Damian Minkov
 **/
public class JavaDecoder extends com.ibm.media.codec.audio.AudioCodec {

    private Format lastFormat = null;

    private int numberOfChannels = 0;

    private SpeexDecoder decoder = null;

    public JavaDecoder() {
        inputFormats = new Format[] { new AudioFormat(Constants.SPEEX_RTP, 8000, 8, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED) };
        supportedInputFormats = new AudioFormat[] { new AudioFormat(Constants.SPEEX_RTP, 8000, 8, 1, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED) };
        defaultOutputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR) };
        PLUGIN_NAME = "Speex Decoder";
    }

    protected Format[] getMatchingOutputFormats(Format in) {
        AudioFormat af = (AudioFormat) in;
        supportedOutputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR, af.getSampleRate(), 16, af.getChannels(), AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED) };
        return supportedOutputFormats;
    }

    public void open() {
    }

    public void close() {
    }

    private void initConverter(AudioFormat inFormat) {
        lastFormat = inFormat;
        numberOfChannels = inFormat.getChannels();
        decoder = new SpeexDecoder();
        decoder.init(0, (int) ((AudioFormat) inFormat).getSampleRate(), inFormat.getChannels(), false);
    }

    public int process(Buffer inputBuffer, Buffer outputBuffer) {
        if (!checkInputBuffer(inputBuffer)) {
            return BUFFER_PROCESSED_FAILED;
        }
        if (isEOM(inputBuffer)) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }
        int channels = ((AudioFormat) outputFormat).getChannels();
        byte[] inData = (byte[]) inputBuffer.getData();
        int inpLength = inputBuffer.getLength();
        int outLength = 0;
        int inOffset = inputBuffer.getOffset();
        int outOffset = outputBuffer.getOffset();
        Format newFormat = inputBuffer.getFormat();
        if (lastFormat != newFormat) {
            initConverter((AudioFormat) newFormat);
        }
        try {
            decoder.processData(inData, inOffset, inpLength);
            outLength = decoder.getProcessedDataByteSize();
            byte[] outData = validateByteArraySize(outputBuffer, outLength);
            decoder.getProcessedData(outData, outOffset);
        } catch (StreamCorruptedException ex) {
            ex.printStackTrace();
        }
        updateOutput(outputBuffer, outputFormat, outLength, 0);
        return BUFFER_PROCESSED_OK;
    }

    static int SpeexSubModeSz[] = { 0, 43, 119, 160, 220, 300, 364, 492, 79, 0, 0, 0, 0, 0, 0, 0 };

    static int SpeexInBandSz[] = { 1, 1, 4, 4, 4, 4, 4, 4, 8, 8, 16, 16, 32, 32, 64, 64 };

    /**
     * Counts the samples in given data
     * @param data byte[]
     * @param len int
     * @return int
     */
    private static int speex_get_samples(byte[] data, int len) {
        int bit = 0;
        int cnt = 0;
        int off = 0;
        int c;
        while ((len * 8 - bit) >= 5) {
            off = speex_get_wb_sz_at(data, len, bit);
            if (off < 0) {
                break;
            }
            bit += off;
            if ((len * 8 - bit) < 5) {
                break;
            }
            c = get_n_bits_at(data, 5, bit);
            bit += 5;
            if (c == 15) {
                break;
            } else if (c == 14) {
                c = get_n_bits_at(data, 4, bit);
                bit += 4;
                bit += SpeexInBandSz[c];
            } else if (c == 13) {
                c = get_n_bits_at(data, 5, bit);
                bit += 5;
                bit += c * 8;
            } else if (c > 8) {
                break;
            } else {
                bit += SpeexSubModeSz[c] - 5;
                cnt += 160;
            }
        }
        return cnt;
    }

    private static int get_n_bits_at(byte[] data, int n, int bit) {
        int byteInt = bit / 8;
        int rem = 8 - (bit % 8);
        int ret = 0;
        if (n <= 0 || n > 8) return 0;
        if (rem < n) {
            ret = (data[byteInt] << (n - rem));
            ret |= (data[byteInt + 1] >> (8 - n + rem));
        } else {
            ret = (data[byteInt] >> (rem - n));
        }
        return (ret & (0xff >> (8 - n)));
    }

    static int SpeexWBSubModeSz[] = { 0, 36, 112, 192, 352, 0, 0, 0 };

    private static int speex_get_wb_sz_at(byte[] data, int len, int bit) {
        int off = bit;
        int c;
        if (((len * 8 - off) >= 5) && get_n_bits_at(data, 1, off) != 0) {
            c = get_n_bits_at(data, 3, off + 1);
            off += SpeexWBSubModeSz[c];
            if (((len * 8 - off) >= 5) && get_n_bits_at(data, 1, off) != 0) {
                c = get_n_bits_at(data, 3, off + 1);
                off += SpeexWBSubModeSz[c];
                if (((len * 8 - off) >= 5) && get_n_bits_at(data, 1, off) != 0) {
                    return -1;
                }
            }
        }
        return off - bit;
    }
}
