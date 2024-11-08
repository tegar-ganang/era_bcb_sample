package org.tritonus.sampled.mixer.esd;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import org.tritonus.share.TDebug;
import org.tritonus.lowlevel.esd.Esd;
import org.tritonus.lowlevel.esd.EsdRecordingStream;
import org.tritonus.share.sampled.TConversionTool;
import org.tritonus.share.sampled.mixer.TMixer;
import org.tritonus.share.sampled.mixer.TBaseDataLine;

public class EsdTargetDataLine extends TBaseDataLine implements TargetDataLine {

    private EsdRecordingStream m_esdStream;

    private boolean m_bSwapBytes;

    private byte[] m_abSwapBuffer;

    private int m_nBytesPerSample;

    public EsdTargetDataLine(TMixer mixer, AudioFormat format, int nBufferSize) throws LineUnavailableException {
        super(mixer, new DataLine.Info(TargetDataLine.class, format, nBufferSize));
    }

    protected void openImpl() {
        if (TDebug.TraceTargetDataLine) {
            TDebug.out("EsdTargetDataLine.openImpl(): called.");
        }
        checkOpen();
        AudioFormat format = getFormat();
        AudioFormat.Encoding encoding = format.getEncoding();
        boolean bBigEndian = format.isBigEndian();
        m_bSwapBytes = false;
        if (format.getSampleSizeInBits() == 16 && bBigEndian) {
            m_bSwapBytes = true;
            bBigEndian = false;
        } else if (format.getSampleSizeInBits() == 8 && encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            m_bSwapBytes = true;
            encoding = AudioFormat.Encoding.PCM_UNSIGNED;
        }
        if (m_bSwapBytes) {
            format = new AudioFormat(encoding, format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate(), bBigEndian);
            m_nBytesPerSample = format.getFrameSize() / format.getChannels();
        }
        int nOutFormat = Esd.ESD_STREAM | Esd.ESD_PLAY | EsdUtils.getEsdFormat(format);
        m_esdStream = new EsdRecordingStream();
        m_esdStream.open(nOutFormat, (int) format.getSampleRate());
    }

    public int available() {
        return -1;
    }

    public int read(byte[] abData, int nOffset, int nLength) {
        if (TDebug.TraceTargetDataLine) {
            TDebug.out("EsdTargetDataLine.read(): called.");
            TDebug.out("EsdTargetDataLine.read(): wanted length: " + nLength);
        }
        int nOriginalOffset = nOffset;
        if (nLength > 0 && !isActive()) {
            start();
        }
        if (!isOpen()) {
            if (TDebug.TraceTargetDataLine) {
                TDebug.out("EsdTargetDataLine.read(): stream closed");
            }
        }
        int nBytesRead = m_esdStream.read(abData, nOffset, nLength);
        if (TDebug.TraceTargetDataLine) {
            TDebug.out("EsdTargetDataLine.read(): read (bytes): " + nBytesRead);
        }
        if (m_bSwapBytes && nBytesRead > 0) {
            TConversionTool.swapOrder16(abData, nOriginalOffset, nBytesRead / 2);
        }
        return nBytesRead;
    }

    public void closeImpl() {
        m_esdStream.close();
    }

    public void drain() {
    }

    public void flush() {
    }

    public long getPosition() {
        return 0;
    }

    /**
	 *	fGain is logarithmic!!
	 */
    protected void setGain(float fGain) {
    }

    public class EsdTargetDataLineGainControl extends FloatControl {

        private final float MAX_GAIN = 90.0F;

        private final float MIN_GAIN = -96.0F;

        private final int GAIN_INCREMENTS = 1000;

        EsdTargetDataLineGainControl() {
            super(FloatControl.Type.VOLUME, -96.0F, 24.0F, 0.01F, 0, 0.0F, "dB", "-96.0", "", "+24.0");
        }

        public void setValue(float fGain) {
            fGain = Math.max(Math.min(fGain, getMaximum()), getMinimum());
            if (Math.abs(fGain - getValue()) > 1.0E9) {
                super.setValue(fGain);
                EsdTargetDataLine.this.setGain(getValue());
            }
        }
    }
}
