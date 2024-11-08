package org.tritonus.sampled.file;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;

/**
 * Common constants and methods for handling wave files.
 *
 * @author Florian Bomers
 */
public class WaveTool {

    public static final int WAVE_RIFF_MAGIC = 0x52494646;

    public static final int WAVE_WAVE_MAGIC = 0x57415645;

    public static final int WAVE_FMT_MAGIC = 0x666D7420;

    public static final int WAVE_DATA_MAGIC = 0x64617461;

    public static final int WAVE_FACT_MAGIC = 0x66616374;

    public static final short WAVE_FORMAT_UNSPECIFIED = 0;

    public static final short WAVE_FORMAT_PCM = 1;

    public static final short WAVE_FORMAT_MS_ADPCM = 2;

    public static final short WAVE_FORMAT_ALAW = 6;

    public static final short WAVE_FORMAT_ULAW = 7;

    public static final short WAVE_FORMAT_IMA_ADPCM = 17;

    public static final short WAVE_FORMAT_G723_ADPCM = 20;

    public static final short WAVE_FORMAT_GSM610 = 49;

    public static final short WAVE_FORMAT_G721_ADPCM = 64;

    public static final short WAVE_FORMAT_MPEG = 80;

    public static final int MIN_FMT_CHUNK_LENGTH = 14;

    public static final int MIN_DATA_OFFSET = 12 + 8 + MIN_FMT_CHUNK_LENGTH + 8;

    public static final int MIN_FACT_CHUNK_LENGTH = 4;

    public static final int FMT_CHUNK_SIZE = 18;

    public static final int RIFF_CONTAINER_CHUNK_SIZE = 12;

    public static final int CHUNK_HEADER_SIZE = 8;

    public static final int DATA_OFFSET = RIFF_CONTAINER_CHUNK_SIZE + CHUNK_HEADER_SIZE + FMT_CHUNK_SIZE + CHUNK_HEADER_SIZE;

    public static AudioFormat.Encoding GSM0610 = new AudioFormat.Encoding("GSM0610");

    public static AudioFormat.Encoding IMA_ADPCM = new AudioFormat.Encoding("IMA_ADPCM");

    public static short getFormatCode(AudioFormat format) {
        AudioFormat.Encoding encoding = format.getEncoding();
        int nSampleSize = format.getSampleSizeInBits();
        boolean littleEndian = !format.isBigEndian();
        boolean frameSizeOK = format.getFrameSize() == AudioSystem.NOT_SPECIFIED || format.getChannels() != AudioSystem.NOT_SPECIFIED || format.getFrameSize() == nSampleSize / 8 * format.getChannels();
        if (nSampleSize == 8 && frameSizeOK && (encoding.equals(AudioFormat.Encoding.PCM_SIGNED) || encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED))) {
            return WAVE_FORMAT_PCM;
        } else if (nSampleSize > 8 && frameSizeOK && littleEndian && encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            return WAVE_FORMAT_PCM;
        } else if (encoding.equals(AudioFormat.Encoding.ULAW) && (nSampleSize == AudioSystem.NOT_SPECIFIED || nSampleSize == 8) && frameSizeOK) {
            return WAVE_FORMAT_ULAW;
        } else if (encoding.equals(AudioFormat.Encoding.ALAW) && (nSampleSize == AudioSystem.NOT_SPECIFIED || nSampleSize == 8) && frameSizeOK) {
            return WAVE_FORMAT_ALAW;
        } else if (encoding.equals(new AudioFormat.Encoding("IMA_ADPCM")) && nSampleSize == 4) {
            return WAVE_FORMAT_IMA_ADPCM;
        } else if (encoding.equals(GSM0610)) {
            return WAVE_FORMAT_GSM610;
        }
        return WAVE_FORMAT_UNSPECIFIED;
    }
}
