import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**	

	@author Matthias Pfisterer
 */
public class SilenceAudioInputStream extends AudioInputStream {

    public SilenceAudioInputStream(AudioFormat audioFormat, long lLengthInMilliseconds) {
        super(new SilenceInputStream(audioFormat), audioFormat, calculateFrameLengthFromDuration(lLengthInMilliseconds, audioFormat.getFrameRate()));
    }

    private static long calculateFrameLengthFromDuration(long lLengthInMilliseconds, float fFrameRate) {
        return (long) (lLengthInMilliseconds * fFrameRate / 1000);
    }

    private static class SilenceInputStream extends InputStream {

        private byte[] m_abOneFrameBuffer;

        public SilenceInputStream(AudioFormat audioFormat) {
            m_abOneFrameBuffer = new byte[audioFormat.getFrameSize()];
            if (audioFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            } else if (audioFormat.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
                int nSampleSizeInBits = audioFormat.getSampleSizeInBits();
                int nSampleSizeInBytes = audioFormat.getFrameSize() / audioFormat.getChannels();
                switch(nSampleSizeInBits) {
                    case 8:
                        m_abOneFrameBuffer[0] = (byte) 0x80;
                        break;
                    case 16:
                        if (audioFormat.isBigEndian()) {
                            m_abOneFrameBuffer[0] = (byte) 0x80;
                            m_abOneFrameBuffer[1] = (byte) 0x00;
                        } else {
                            m_abOneFrameBuffer[0] = (byte) 0x00;
                            m_abOneFrameBuffer[1] = (byte) 0x80;
                        }
                        break;
                    case 24:
                        if (audioFormat.isBigEndian()) {
                            m_abOneFrameBuffer[0] = (byte) 0x80;
                            m_abOneFrameBuffer[1] = (byte) 0x00;
                            m_abOneFrameBuffer[2] = (byte) 0x00;
                        } else {
                            m_abOneFrameBuffer[0] = (byte) 0x00;
                            m_abOneFrameBuffer[1] = (byte) 0x00;
                            m_abOneFrameBuffer[2] = (byte) 0x80;
                        }
                        break;
                    case 32:
                        if (audioFormat.isBigEndian()) {
                            m_abOneFrameBuffer[0] = (byte) 0x80;
                            m_abOneFrameBuffer[1] = (byte) 0x00;
                            m_abOneFrameBuffer[2] = (byte) 0x00;
                            m_abOneFrameBuffer[3] = (byte) 0x00;
                        } else {
                            m_abOneFrameBuffer[0] = (byte) 0x00;
                            m_abOneFrameBuffer[1] = (byte) 0x00;
                            m_abOneFrameBuffer[2] = (byte) 0x00;
                            m_abOneFrameBuffer[3] = (byte) 0x80;
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("sample size not supported");
                }
                for (int i = 1; i < audioFormat.getChannels(); i++) {
                    System.arraycopy(m_abOneFrameBuffer, 0, m_abOneFrameBuffer, i * nSampleSizeInBytes, nSampleSizeInBytes);
                }
            } else {
                throw new IllegalArgumentException("encoding is not PCM_SIGNED or PCM_UNSIGNED");
            }
        }

        public int read() throws IOException {
            return m_abOneFrameBuffer[0];
        }

        public int read(byte[] abBuffer, int nOffset, int nLength) {
            int nFrameSize = m_abOneFrameBuffer.length;
            for (int nBufferPosition = 0; nBufferPosition < nLength; nBufferPosition += nFrameSize) {
                System.arraycopy(m_abOneFrameBuffer, 0, abBuffer, nOffset + nBufferPosition, nFrameSize);
            }
            return nLength;
        }

        public int available() {
            int nFrameSize = m_abOneFrameBuffer.length;
            return (Integer.MAX_VALUE / nFrameSize) * nFrameSize;
        }
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}
