package rtp.custompayload;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.format.AudioFormat;

/**
 * Implements an PCM depacketizer.  It uses a custom payload header
 * to send uncompressed PCM data over RTP.  This is not the same
 * as the standard PCMU or PCMA payloads as defined in the RTP spec.
 * Nor do we claim that this is an efficient way to transmit PCM
 * audio.  The sole purpose of this sample is to illustrate the 
 * concept to write a custom RTP depacketizer in JMF.
 */
public class PcmDepacketizer implements Codec {

    static String PLUGIN_NAME = "PCM DePacketizer";

    static String CUSTOM_PCM = "mypcm/rtp";

    static int HDR_SIZE = 8;

    static int DEFAULT_RATE = 8000;

    static int DEFAULT_SIZE = 8;

    static int DEFAULT_CHNLS = 1;

    private Format supportedInputFormats[];

    private Format supportedOutputFormats[];

    private AudioFormat inFormat;

    private AudioFormat outFormat;

    public PcmDepacketizer() {
        supportedInputFormats = new AudioFormat[] { new AudioFormat(CUSTOM_PCM) };
        supportedOutputFormats = new AudioFormat[] { new AudioFormat(AudioFormat.LINEAR, DEFAULT_RATE, DEFAULT_SIZE, DEFAULT_CHNLS) };
    }

    public String getName() {
        return PLUGIN_NAME;
    }

    public static boolean matches(Format input, Format supported[]) {
        for (int i = 0; i < supported.length; i++) {
            if (input.matches(supported[i])) return true;
        }
        return false;
    }

    public Format[] getSupportedInputFormats() {
        return supportedInputFormats;
    }

    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null || matches(input, supportedInputFormats)) {
            return supportedOutputFormats;
        }
        return new Format[0];
    }

    public Format setInputFormat(Format format) {
        if (!matches(format, supportedInputFormats)) return null;
        inFormat = (AudioFormat) format;
        return format;
    }

    public Format setOutputFormat(Format format) {
        if (!matches(format, supportedOutputFormats)) return null;
        outFormat = (AudioFormat) format;
        return format;
    }

    protected Format getInputFormat() {
        return inFormat;
    }

    protected Format getOutputFormat() {
        return outFormat;
    }

    public void open() {
    }

    public void close() {
    }

    /**
     * No controls implemented for this plugin.
     */
    public Object[] getControls() {
        return new Object[0];
    }

    /**
     * No controls implemented for this plugin.
     */
    public Object getControl(String type) {
        return null;
    }

    /**
     * The processing function that does all the work.
     */
    public int process(Buffer inBuf, Buffer outBuf) {
        if (inBuf.isEOM()) {
            outBuf.setLength(0);
            outBuf.setEOM(true);
            return BUFFER_PROCESSED_OK;
        }
        byte hdr[] = (byte[]) inBuf.getData();
        int offset = inBuf.getOffset();
        int rate = ((hdr[offset + 1] & 0xff) << 16) | ((hdr[offset + 2] & 0xff) << 8) | (hdr[offset + 3] & 0xff);
        int sizeInBits = hdr[offset + 4];
        int channels = hdr[offset + 5];
        int endian = hdr[offset + 6];
        int signed = hdr[offset + 7];
        if ((int) outFormat.getSampleRate() != rate || outFormat.getSampleSizeInBits() != sizeInBits || outFormat.getChannels() != channels || outFormat.getEndian() != endian || outFormat.getSigned() != signed) {
            outFormat = new AudioFormat(AudioFormat.LINEAR, rate, sizeInBits, channels, endian, signed);
        }
        Object outData = outBuf.getData();
        outBuf.setData(inBuf.getData());
        inBuf.setData(outData);
        outBuf.setLength(inBuf.getLength() - HDR_SIZE);
        outBuf.setOffset(inBuf.getOffset() + HDR_SIZE);
        outBuf.setFormat(outFormat);
        return BUFFER_PROCESSED_OK;
    }

    public void reset() {
    }
}
