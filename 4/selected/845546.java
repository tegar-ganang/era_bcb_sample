package org.tritonus.saol.engine;

import java.io.IOException;
import org.tritonus.share.TDebug;
import org.tritonus.share.sampled.TConversionTool;
import org.tritonus.share.sampled.file.AudioOutputStream;

public class AudioOutputStreamOutput extends Bus implements SystemOutput {

    private AudioOutputStream m_audioOutputStream;

    private byte[] m_abBuffer;

    public AudioOutputStreamOutput(AudioOutputStream audioOutputStream) {
        super(audioOutputStream.getFormat().getChannels());
        m_audioOutputStream = audioOutputStream;
        m_abBuffer = new byte[audioOutputStream.getFormat().getFrameSize()];
    }

    public void emit() throws IOException {
        float[] afValues = getValues();
        boolean bBigEndian = m_audioOutputStream.getFormat().isBigEndian();
        int nOffset = 0;
        for (int i = 0; i < afValues.length; i++) {
            float fOutput = Math.max(Math.min(afValues[i], 1.0F), -1.0F);
            int nOutput = (int) (fOutput * 32767.0F);
            TConversionTool.shortToBytes16((short) nOutput, m_abBuffer, nOffset, bBigEndian);
            nOffset += 2;
        }
        m_audioOutputStream.write(m_abBuffer, 0, m_abBuffer.length);
    }

    public void close() throws IOException {
        m_audioOutputStream.close();
    }
}
