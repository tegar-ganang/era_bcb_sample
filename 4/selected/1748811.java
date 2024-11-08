package org.tritonus.sampled.mixer.esd;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.tritonus.share.TDebug;
import org.tritonus.lowlevel.esd.Esd;
import org.tritonus.lowlevel.esd.EsdStream;
import org.tritonus.share.sampled.TConversionTool;
import org.tritonus.share.sampled.mixer.TMixer;
import org.tritonus.share.sampled.mixer.TBaseDataLine;

public class EsdSourceDataLine extends TBaseDataLine implements SourceDataLine {

    private EsdStream m_esdStream;

    private boolean m_bSwapBytes;

    private byte[] m_abSwapBuffer;

    private int m_nBytesPerSample;

    private boolean m_bMuted;

    private float m_fGain;

    private float m_fPan;

    public EsdSourceDataLine(TMixer mixer, AudioFormat format, int nBufferSize) throws LineUnavailableException {
        super(mixer, new DataLine.Info(SourceDataLine.class, format, nBufferSize));
        addControl(new EsdSourceDataLineGainControl());
        addControl(new EsdSourceDataLinePanControl());
        addControl(new EsdSourceDataLineMuteControl());
    }

    protected void openImpl() {
        if (TDebug.TraceSourceDataLine) {
            TDebug.out("EsdSourceDataLine.openImpl(): called.");
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
        if (System.getProperty("os.arch").equals("ppc") && format.getSampleSizeInBits() == 16) {
            m_bSwapBytes ^= true;
        }
        if (m_bSwapBytes) {
            format = new AudioFormat(encoding, format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate(), bBigEndian);
            m_nBytesPerSample = format.getFrameSize() / format.getChannels();
        }
        int nOutFormat = Esd.ESD_STREAM | Esd.ESD_PLAY | EsdUtils.getEsdFormat(format);
        m_esdStream = new EsdStream();
        m_esdStream.open(nOutFormat, (int) format.getSampleRate());
    }

    public int available() {
        return -1;
    }

    public int write(byte[] abData, int nOffset, int nLength) {
        if (TDebug.TraceSourceDataLine) {
            TDebug.out("EsdSourceDataLine.write(): called.");
        }
        if (m_bSwapBytes) {
            if (m_abSwapBuffer == null || m_abSwapBuffer.length < nOffset + nLength) {
                m_abSwapBuffer = new byte[nOffset + nLength];
            }
            TConversionTool.changeOrderOrSign(abData, nOffset, m_abSwapBuffer, nOffset, nLength, m_nBytesPerSample);
            abData = m_abSwapBuffer;
        }
        if (nLength > 0 && !isActive()) {
            start();
        }
        int nRemaining = nLength;
        while (nRemaining > 0 && isOpen()) {
            synchronized (this) {
                if (!isOpen()) {
                    return nLength - nRemaining;
                }
                int nWritten = m_esdStream.write(abData, nOffset, nRemaining);
                nOffset += nWritten;
                nRemaining -= nWritten;
            }
        }
        return nLength;
    }

    protected void closeImpl() {
        if (TDebug.TraceSourceDataLine) {
            TDebug.out("EsdSourceDataLine.closeImpl(): called.");
        }
        m_esdStream.close();
    }

    public void drain() {
        if (TDebug.TraceSourceDataLine) {
            TDebug.out("EsdSourceDataLine.drain(): called.");
        }
    }

    public void flush() {
        if (TDebug.TraceSourceDataLine) {
            TDebug.out("EsdSourceDataLine.flush(): called.");
        }
    }

    /**
	 *	fGain is logarithmic!!
	 */
    protected void setGain(float fGain) {
        if (TDebug.TraceSourceDataLine) {
            TDebug.out("EsdSourceDataLine.setGain(): gain: " + fGain);
        }
        m_fGain = fGain;
        if (!m_bMuted) {
            setGainImpl();
        }
    }

    /**
	 */
    protected void setPan(float fPan) {
        if (TDebug.TraceSourceDataLine) {
            TDebug.out("EsdSourceDataLine.setPan(): pan: " + fPan);
        }
        m_fPan = fPan;
        if (!m_bMuted) {
            setGainImpl();
        }
    }

    /**
	 */
    protected void setMuted(boolean bMuted) {
        if (TDebug.TraceSourceDataLine) {
            TDebug.out("EsdSourceDataLine.setMuted(): muted: " + bMuted);
        }
        m_bMuted = bMuted;
        if (m_bMuted) {
        } else {
            setGainImpl();
        }
    }

    /**
	 */
    private void setGainImpl() {
        if (TDebug.TraceSourceDataLine) {
            TDebug.out("EsdSourceDataLine.setGainImpl(): called: ");
        }
    }

    public class EsdSourceDataLineGainControl extends FloatControl {

        private final float MAX_GAIN = 24.0F;

        private final float MIN_GAIN = -96.0F;

        EsdSourceDataLineGainControl() {
            super(FloatControl.Type.MASTER_GAIN, -96.0F, 24.0F, 0.01F, 0, 0.0F, "dB", "-96.0", "", "+24.0");
        }

        public void setValue(float fGain) {
            if (TDebug.TraceSourceDataLine) {
                TDebug.out("EsdSourceDataLineGainControl.setValue(): gain: " + fGain);
            }
            float fOldGain = getValue();
            super.setValue(fGain);
            if (Math.abs(fOldGain - getValue()) > 1.0E-9) {
                if (TDebug.TraceSourceDataLine) {
                    TDebug.out("EsdSourceDataLineGainControl.setValue(): really changing gain");
                }
                EsdSourceDataLine.this.setGain(getValue());
            }
        }
    }

    public class EsdSourceDataLinePanControl extends FloatControl {

        EsdSourceDataLinePanControl() {
            super(FloatControl.Type.PAN, -1.0F, 1.0F, 0.01F, 0, 0.0F, "??", "left", "center", "right");
        }

        public void setValue(float fPan) {
            if (TDebug.TraceSourceDataLine) {
                TDebug.out("EsdSourceDataLinePanControl.setValue(): pan: " + fPan);
            }
            float fOldPan = getValue();
            super.setValue(fPan);
            if (Math.abs(fOldPan - getValue()) > 1.0E-9) {
                if (TDebug.TraceSourceDataLine) {
                    TDebug.out("EsdSourceDataLinePanControl.setValue(): really changing pan");
                }
                EsdSourceDataLine.this.setPan(getValue());
            }
        }
    }

    public class EsdSourceDataLineMuteControl extends BooleanControl {

        EsdSourceDataLineMuteControl() {
            super(BooleanControl.Type.MUTE, false, "muted", "unmuted");
        }

        public void setValue(boolean bMuted) {
            if (TDebug.TraceSourceDataLine) {
                TDebug.out("EsdSourceDataLineMuteControl.setValue(): muted: " + bMuted);
            }
            if (bMuted != getValue()) {
                if (TDebug.TraceSourceDataLine) {
                    TDebug.out("EsdSourceDataLineMuteControl.setValue(): really changing mute status");
                }
                super.setValue(bMuted);
                EsdSourceDataLine.this.setMuted(getValue());
            }
        }
    }
}
