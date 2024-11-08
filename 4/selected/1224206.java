package org.tritonus.test.tritonus.sampled.file;

import javax.sound.sampled.AudioFormat;
import org.tritonus.share.sampled.file.AudioOutputStream;
import org.tritonus.share.sampled.file.TDataOutputStream;
import org.tritonus.sampled.file.WaveAudioOutputStream;

public class WaveAudioOutputStreamTestCase extends BaseAudioOutputStreamTestCase {

    private static final int EXPECTED_ADDITIONAL_HEADER_LENGTH = 0;

    public WaveAudioOutputStreamTestCase(String strName) {
        super(strName);
    }

    protected AudioOutputStream createAudioOutputStreamImpl(AudioFormat audioFormat, long nLength, TDataOutputStream dataOutputStream) throws Exception {
        return new WaveAudioOutputStream(audioFormat, nLength, dataOutputStream);
    }

    protected byte[] getExpectedHeaderData(AudioFormat audioFormat, int nLength, boolean bSeekable, boolean bLengthGiven) {
        int nTotalLength = 38 + nLength;
        int nSampleRate = (int) audioFormat.getSampleRate();
        int nBytesPerSecond = nSampleRate * audioFormat.getFrameSize();
        byte[] abExpectedHeaderData = new byte[] { 0x52, 0x49, 0x46, 0x46, (byte) nTotalLength, 0, 0, 0, 0x57, 0x41, 0x56, 0x45, 0x66, 0x6d, 0x74, 0x20, 18, 0, 0, 0, 1, 0, (byte) audioFormat.getChannels(), 0, (byte) nSampleRate, (byte) (nSampleRate / 256), (byte) (nSampleRate / 65536), 0, (byte) nBytesPerSecond, (byte) (nBytesPerSecond / 256), (byte) (nBytesPerSecond / 65536), 0, (byte) audioFormat.getFrameSize(), 0, (byte) audioFormat.getSampleSizeInBits(), 0, 0, 0, 0x64, 0x61, 0x74, 0x61, (byte) nLength, (byte) (nLength / 256), (byte) (nLength / 65536), 0 };
        return abExpectedHeaderData;
    }

    private byte getEncoding(AudioFormat format) {
        return (byte) (format.getSampleSizeInBits() / 8 + 1);
    }

    protected int getExpectedAdditionalHeaderLength() {
        return EXPECTED_ADDITIONAL_HEADER_LENGTH;
    }

    protected boolean getBigEndian() {
        return false;
    }

    protected boolean is8bitUnsigned() {
        return true;
    }
}
