package org.tritonus.sampled.file;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

/**
 * Common constants and methods for handling aiff and aiff-c files.
 *
 * @author Florian Bomers
 */
public class AiffTool {

    public static final int AIFF_FORM_MAGIC = 0x464F524D;

    public static final int AIFF_AIFF_MAGIC = 0x41494646;

    public static final int AIFF_AIFC_MAGIC = 0x41494643;

    public static final int AIFF_COMM_MAGIC = 0x434F4D4D;

    public static final int AIFF_SSND_MAGIC = 0x53534E44;

    public static final int AIFF_FVER_MAGIC = 0x46564552;

    public static final int AIFF_COMM_UNSPECIFIED = 0x00000000;

    public static final int AIFF_COMM_PCM = 0x4E4F4E45;

    public static final int AIFF_COMM_ULAW = 0x756C6177;

    public static final int AIFF_COMM_IMA_ADPCM = 0x696D6134;

    public static final int AIFF_FVER_TIME_STAMP = 0xA2805140;

    public static int getFormatCode(AudioFormat format) {
        AudioFormat.Encoding encoding = format.getEncoding();
        int nSampleSize = format.getSampleSizeInBits();
        boolean bigEndian = format.isBigEndian();
        boolean frameSizeOK = format.getFrameSize() == AudioSystem.NOT_SPECIFIED || format.getChannels() != AudioSystem.NOT_SPECIFIED || format.getFrameSize() == nSampleSize / 8 * format.getChannels();
        if ((encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) && ((bigEndian && nSampleSize >= 16 && nSampleSize <= 32) || (nSampleSize == 8)) && frameSizeOK) {
            return AIFF_COMM_PCM;
        } else if (encoding.equals(AudioFormat.Encoding.ULAW) && nSampleSize == 8 && frameSizeOK) {
            return AIFF_COMM_ULAW;
        } else if (encoding.equals(new AudioFormat.Encoding("IMA_ADPCM")) && nSampleSize == 4) {
            return AIFF_COMM_IMA_ADPCM;
        } else {
            return AIFF_COMM_UNSPECIFIED;
        }
    }
}
