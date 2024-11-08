import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**	Converting a mono stream to single-sided a stereo stream.


	Note: skip(), available() and mark()/reset() are not tested!

	@author Matthias Pfisterer
 */
public class SingleChannelStereoAudioInputStream extends AudioInputStream {

    private static final boolean DEBUG = false;

    /** Flag for writing silence samples.  If this flag is true, zero
		bytes for silence samples are not written to the array passed
		to read(). This assumes that the array has been filled with
		0's outside of this class, which is often the case. Non-zero
		bytes (e.g. 128 for unsigned 8 bit) are always written.
	*/
    private static boolean sm_bOptimizeSilenceWriting = true;

    /** The AudioInputStream for this one, already converted to mono.
	 */
    private AudioInputStream m_sourceStream;

    /** Stream should appear left or right?  If true, the signal is
		put on the left channel and silence on the right, otherwise
		vice versa.
	*/
    private boolean m_bSignalOnLeftChannel;

    private byte[] m_abSilenceSample;

    /** Intermediate buffer for read().  This reference to this buffer
		is an instance variable so that there is no need to allocate
		the buffer on each invocation of read(). We initialize it here
		with an array of length 0 because this saves the handling of
		null references.
	*/
    private byte[] m_abSourceBuffer = new byte[0];

    /** Constructor.

	@param sourceStream the stream this one should be based on. The
	stream has to be in a PCM encoding. It may be stereo or mono if
	Tritonus' PCM2PCM converter is available in the system. If not,
	only mono is allowed.

	@param bSignalOnLeftChannel determines on which of the stereo
	channels (left or right) the sourceStream should appear. Passing
	true puts the stream on the left side and silence on the right
	side, passing false does it the other way round.

	@throws IllegalArgumentException if the encoding of sourceStream
	is neither PCM_SIGNED nor PCM_UNSIGNED, or if the encoding is
	PCM_UNSIGNED and the sample size in bits is different from 8.
	*/
    public SingleChannelStereoAudioInputStream(AudioInputStream sourceStream, boolean bSignalOnLeftChannel) {
        super(new ByteArrayInputStream(new byte[0]), new AudioFormat(sourceStream.getFormat().getSampleRate(), sourceStream.getFormat().getSampleSizeInBits(), 2, sourceStream.getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED), sourceStream.getFormat().isBigEndian()), sourceStream.getFrameLength());
        if (DEBUG) {
            out("SingleChannelStereoAudioInputStream.<init>(): begin");
        }
        AudioFormat sourceFormat = sourceStream.getFormat();
        if (!AudioCommon.isPcm(sourceFormat.getEncoding())) {
            throw new IllegalArgumentException("source stream has to be PCM");
        }
        if (sourceFormat.getChannels() != 1) {
            AudioFormat monoFormat = new AudioFormat(sourceFormat.getEncoding(), sourceFormat.getSampleRate(), sourceFormat.getSampleSizeInBits(), 1, (sourceFormat.getSampleSizeInBits() + 7) / 8, sourceFormat.getFrameRate(), sourceFormat.isBigEndian());
            sourceStream = AudioSystem.getAudioInputStream(monoFormat, sourceStream);
        }
        m_sourceStream = sourceStream;
        m_bSignalOnLeftChannel = bSignalOnLeftChannel;
        int nSampleSizeInBytes = getFormat().getFrameSize() / 2;
        m_abSilenceSample = new byte[nSampleSizeInBytes];
        if (getFormat().getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
            if (getFormat().getSampleSizeInBits() == 8) m_abSilenceSample[0] = (byte) 128; else throw new IllegalArgumentException("unsigned formats are only supported for 8 bit");
        }
        if (DEBUG) {
            out("SingleChannelStereoAudioInputStream.<init>(): end");
        }
    }

    public int read() throws IOException {
        if (DEBUG) {
            out("SingleChannelStereoAudioInputStream.read(): begin");
        }
        throw new IOException("cannot read fraction of a frame");
    }

    public int read(byte[] abData, int nOffset, int nLength) throws IOException {
        if (DEBUG) {
            out("SingleChannelStereoAudioInputStream.read(byte[], int, int): begin");
            out("SingleChannelStereoAudioInputStream.read(byte[], int, int): requested length: " + nLength);
        }
        int nThisFrameSize = getFormat().getFrameSize();
        int nThisSampleSizeInBytes = nThisFrameSize / 2;
        int nSourceFrameSize = m_sourceStream.getFormat().getFrameSize();
        if ((nLength % nThisFrameSize) != 0) {
            throw new IOException("cannot read fraction of a frame");
        }
        AudioFormat.Encoding encoding = getFormat().getEncoding();
        if (DEBUG) {
            out("SingleChannelStereoAudioInputStream.read(byte[], int, int): frame size: " + nThisFrameSize);
            out("SingleChannelStereoAudioInputStream.read(byte[], int, int): encoding: " + encoding);
        }
        int nFrames = nLength / nThisFrameSize;
        int nUsedSourceBufferLength = nFrames * nSourceFrameSize;
        if (m_abSourceBuffer.length < nUsedSourceBufferLength) {
            m_abSourceBuffer = new byte[nUsedSourceBufferLength];
        }
        byte[] abSourceBuffer = m_abSourceBuffer;
        int nBytesRead = m_sourceStream.read(abSourceBuffer, 0, nUsedSourceBufferLength);
        if (DEBUG) {
            out("SingleChannelStereoAudioInputStream.read(byte[], int, int): bytes read: " + nBytesRead);
        }
        if (nBytesRead == -1) {
            return -1;
        }
        nFrames = nBytesRead / m_sourceStream.getFormat().getFrameSize();
        int nThisFrameBoundry = nOffset;
        int nSourceFrameBoundry = 0;
        int n;
        if (nThisSampleSizeInBytes == 2) {
            for (int i = 0; i < nFrames; i++) {
                int nSignalDestIndex;
                int nSilenceDestIndex;
                if (m_bSignalOnLeftChannel) {
                    nSignalDestIndex = nThisFrameBoundry;
                    nSilenceDestIndex = nThisFrameBoundry + nThisSampleSizeInBytes;
                } else {
                    nSilenceDestIndex = nThisFrameBoundry;
                    nSignalDestIndex = nThisFrameBoundry + nThisSampleSizeInBytes;
                }
                abData[nSignalDestIndex] = abSourceBuffer[nSourceFrameBoundry];
                abData[nSignalDestIndex + 1] = abSourceBuffer[nSourceFrameBoundry + 1];
                if (!sm_bOptimizeSilenceWriting) {
                    abData[nSilenceDestIndex] = 0;
                    abData[nSilenceDestIndex + 1] = 0;
                }
                nThisFrameBoundry += nThisFrameSize;
                nSourceFrameBoundry += nSourceFrameSize;
            }
        } else {
            for (int i = 0; i < nFrames; i++) {
                int nSignalDestIndex;
                int nSilenceDestIndex;
                if (m_bSignalOnLeftChannel) {
                    nSignalDestIndex = nThisFrameBoundry;
                    nSilenceDestIndex = nThisFrameBoundry + nThisSampleSizeInBytes;
                } else {
                    nSilenceDestIndex = nThisFrameBoundry;
                    nSignalDestIndex = nThisFrameBoundry + nThisSampleSizeInBytes;
                }
                n = 0;
                while (n < nThisSampleSizeInBytes) {
                    abData[nSignalDestIndex + n] = abSourceBuffer[nSourceFrameBoundry + n];
                    n++;
                }
                if (!sm_bOptimizeSilenceWriting) {
                    n = 0;
                    while (n < nThisSampleSizeInBytes) {
                        abData[nSilenceDestIndex + n] = 0;
                        n++;
                    }
                }
                nThisFrameBoundry += nThisFrameSize;
                nSourceFrameBoundry += nSourceFrameSize;
            }
        }
        if (DEBUG) {
            out("SingleChannelStereoAudioInputStream.read(byte[], int, int): end");
        }
        return nFrames * nThisFrameSize;
    }

    /**
	   Calls skip() on the source stream. Since the source stream may
	   have a different frame size, The number of bytes is
	   recalculated, so that the number of skipped frames is the same
	   as requested.
	*/
    public long skip(long lLength) throws IOException {
        int nThisFrameSize = getFormat().getFrameSize();
        int nSourceFrameSize = m_sourceStream.getFormat().getFrameSize();
        long lBytesInSource = (lLength / nThisFrameSize) * nSourceFrameSize;
        long lBytesSkippedInSource = m_sourceStream.skip(lBytesInSource);
        return (lBytesSkippedInSource / nSourceFrameSize) * nThisFrameSize;
    }

    /**
	   The minimum of available() of all input stream is calculated and returned.
	*/
    public int available() throws IOException {
        int nThisFrameSize = getFormat().getFrameSize();
        int nSourceFrameSize = m_sourceStream.getFormat().getFrameSize();
        int nAvailableInSource = m_sourceStream.available();
        return (nAvailableInSource / nSourceFrameSize) * nThisFrameSize;
    }

    public void close() throws IOException {
        m_sourceStream.close();
    }

    /**
	   Recalculates nReadLimit to an equivalent number (same number of
	   frames) for the source stream and calls mark() on it.
	*/
    public void mark(int nReadLimit) {
        int nThisFrameSize = getFormat().getFrameSize();
        int nSourceFrameSize = m_sourceStream.getFormat().getFrameSize();
        int nSourceReadLimit = (nReadLimit / nThisFrameSize) * nSourceFrameSize;
        m_sourceStream.mark(nSourceReadLimit);
    }

    /**
	   Calls reset() on the source stream.
	*/
    public void reset() throws IOException {
        m_sourceStream.reset();
    }

    /**
	   returns true if the source stream return true for
	   markSupported().
	*/
    public boolean markSupported() {
        return m_sourceStream.markSupported();
    }

    /** Print a message to standard output.
		@param strMessage the string that should be printed
	*/
    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}
