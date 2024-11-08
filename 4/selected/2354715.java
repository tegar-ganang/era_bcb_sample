package org.tritonus.sampled.file;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

/** Common constants and methods for handling au files.
 *
 * @author Florian Bomers
 */
public class AuTool {

    public static final int AU_HEADER_MAGIC = 0x2e736e64;

    public static final int AUDIO_UNKNOWN_SIZE = -1;

    public static final int DATA_OFFSET = 24;

    public static final int SND_FORMAT_UNSPECIFIED = 0;

    public static final int SND_FORMAT_MULAW_8 = 1;

    public static final int SND_FORMAT_LINEAR_8 = 2;

    public static final int SND_FORMAT_LINEAR_16 = 3;

    public static final int SND_FORMAT_LINEAR_24 = 4;

    public static final int SND_FORMAT_LINEAR_32 = 5;

    public static final int SND_FORMAT_FLOAT = 6;

    public static final int SND_FORMAT_DOUBLE = 7;

    public static final int SND_FORMAT_ADPCM_G721 = 23;

    public static final int SND_FORMAT_ADPCM_G722 = 24;

    public static final int SND_FORMAT_ADPCM_G723_3 = 25;

    public static final int SND_FORMAT_ADPCM_G723_5 = 26;

    public static final int SND_FORMAT_ALAW_8 = 27;

    public static int getFormatCode(AudioFormat format) {
        AudioFormat.Encoding encoding = format.getEncoding();
        int nSampleSize = format.getSampleSizeInBits();
        boolean bigEndian = format.isBigEndian();
        boolean frameSizeOK = (format.getFrameSize() == AudioSystem.NOT_SPECIFIED || format.getChannels() != AudioSystem.NOT_SPECIFIED || format.getFrameSize() == nSampleSize / 8 * format.getChannels());
        if (encoding.equals(AudioFormat.Encoding.ULAW) && nSampleSize == 8 && frameSizeOK) {
            return SND_FORMAT_MULAW_8;
        } else if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED) && frameSizeOK) {
            if (nSampleSize == 8) {
                return SND_FORMAT_LINEAR_8;
            } else if (nSampleSize == 16 && bigEndian) {
                return SND_FORMAT_LINEAR_16;
            } else if (nSampleSize == 24 && bigEndian) {
                return SND_FORMAT_LINEAR_24;
            } else if (nSampleSize == 32 && bigEndian) {
                return SND_FORMAT_LINEAR_32;
            }
        } else if (encoding.equals(AudioFormat.Encoding.ALAW) && nSampleSize == 8 && frameSizeOK) {
            return SND_FORMAT_ALAW_8;
        }
        return SND_FORMAT_UNSPECIFIED;
    }
}
