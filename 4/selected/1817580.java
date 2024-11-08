package org.tritonus.sampled.convert;

import java.util.Arrays;
import java.util.Iterator;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import org.tritonus.share.TDebug;
import org.tritonus.share.sampled.AudioFormats;
import org.tritonus.share.sampled.convert.TSynchronousFilteredAudioInputStream;
import org.tritonus.share.sampled.convert.TEncodingFormatConversionProvider;

/**	IMA ADPCM encoder and decoder.

	@author Matthias Pfisterer
*/
public class ImaAdpcmFormatConversionProvider extends TEncodingFormatConversionProvider {

    private static final AudioFormat.Encoding IMA_ADPCM = new AudioFormat.Encoding("IMA_ADPCM");

    private static final AudioFormat.Encoding PCM_SIGNED = new AudioFormat.Encoding("PCM_SIGNED");

    private static final AudioFormat[] INPUT_FORMATS = { new AudioFormat(IMA_ADPCM, -1.0F, 4, 1, -1, -1.0F, false), new AudioFormat(IMA_ADPCM, -1.0F, 4, 1, -1, -1.0F, true), new AudioFormat(PCM_SIGNED, -1.0F, 16, 1, 2, -1.0F, false), new AudioFormat(PCM_SIGNED, -1.0F, 16, 1, 2, -1.0F, true) };

    static final int[] indexTable = { -1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8 };

    static final int[] stepsizeTable = { 7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66, 73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899, 15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767 };

    /**	Constructor.
	 */
    public ImaAdpcmFormatConversionProvider() {
        super(Arrays.asList(INPUT_FORMATS), Arrays.asList(INPUT_FORMATS));
    }

    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream) {
        AudioInputStream convertedAudioInputStream = null;
        if (TDebug.TraceAudioConverter) {
            TDebug.out(">ImaAdpcmFormatConversionProvider.getAudioInputStream(): begin");
            TDebug.out("checking if conversion supported");
            TDebug.out("from: " + audioInputStream.getFormat());
            TDebug.out("to: " + targetFormat);
        }
        targetFormat = getDefaultTargetFormat(targetFormat, audioInputStream.getFormat());
        if (isConversionSupported(targetFormat, audioInputStream.getFormat())) {
            if (targetFormat.getEncoding().equals(IMA_ADPCM)) {
                if (TDebug.TraceAudioConverter) {
                    TDebug.out("conversion supported; trying to create EncodedImaAdpcmAudioInputStream");
                }
                convertedAudioInputStream = new EncodedImaAdpcmAudioInputStream(audioInputStream, targetFormat);
            } else {
                if (TDebug.TraceAudioConverter) {
                    TDebug.out("conversion supported; trying to create DecodedImaAdpcmAudioInputStream");
                }
                convertedAudioInputStream = new DecodedImaAdpcmAudioInputStream(audioInputStream, targetFormat);
            }
        } else {
            if (TDebug.TraceAudioConverter) {
                TDebug.out("<conversion not supported; throwing IllegalArgumentException");
            }
            throw new IllegalArgumentException("conversion not supported");
        }
        if (TDebug.TraceAudioConverter) {
            TDebug.out("<ImaAdpcmFormatConversionProvider.getAudioInputStream(): end");
        }
        return convertedAudioInputStream;
    }

    protected AudioFormat getDefaultTargetFormat(AudioFormat targetFormat, AudioFormat sourceFormat) {
        if (TDebug.TraceAudioConverter) {
            TDebug.out("ImaAdpcmFormatConversionProvider.getDefaultTargetFormat(): target format: " + targetFormat);
        }
        if (TDebug.TraceAudioConverter) {
            TDebug.out("ImaAdpcmFormatConversionProvider.getDefaultTargetFormat(): source format: " + sourceFormat);
        }
        AudioFormat newTargetFormat = null;
        Iterator iterator = getCollectionTargetFormats().iterator();
        while (iterator.hasNext()) {
            AudioFormat format = (AudioFormat) iterator.next();
            if (AudioFormats.matches(targetFormat, format)) {
                newTargetFormat = format;
            }
        }
        if (newTargetFormat == null) {
            throw new IllegalArgumentException("conversion not supported");
        }
        if (TDebug.TraceAudioConverter) {
            TDebug.out("ImaAdpcmFormatConversionProvider.getDefaultTargetFormat(): new target format: " + newTargetFormat);
        }
        newTargetFormat = new AudioFormat(targetFormat.getEncoding(), sourceFormat.getSampleRate(), newTargetFormat.getSampleSizeInBits(), newTargetFormat.getChannels(), newTargetFormat.getFrameSize(), sourceFormat.getSampleRate(), newTargetFormat.isBigEndian());
        if (TDebug.TraceAudioConverter) {
            TDebug.out("ImaAdpcmFormatConversionProvider.getDefaultTargetFormat(): really new target format: " + newTargetFormat);
        }
        return newTargetFormat;
    }

    public static class DecodedImaAdpcmAudioInputStream extends TSynchronousFilteredAudioInputStream {

        private ImaAdpcmState m_state;

        /**
		 * Constructor.
		 */
        public DecodedImaAdpcmAudioInputStream(AudioInputStream encodedStream, AudioFormat outputFormat) {
            super(encodedStream, outputFormat);
            if (TDebug.TraceAudioConverter) {
                TDebug.out("DecodedImaAdpcmAudioInputStream.<init>(): begin");
            }
            m_state = new ImaAdpcmState();
            if (TDebug.TraceAudioConverter) {
                TDebug.out("DecodedImaAdpcmAudioInputStream.<init>(): end");
            }
        }

        protected int convert(byte[] inBuffer, byte[] outBuffer, int outByteOffset, int inFrameCount) {
            if (TDebug.TraceAudioConverter) {
                TDebug.out("DecodedImaAdpcmAudioInputStream.convert(): begin");
            }
            int inp;
            int outp;
            int sign;
            int delta;
            int step;
            int valpred;
            int vpdiff;
            int index;
            int inputbuffer = 0;
            boolean bufferstep;
            int len = inFrameCount;
            inp = 0;
            outp = outByteOffset;
            valpred = m_state.valprev;
            index = m_state.index;
            step = stepsizeTable[index];
            bufferstep = false;
            for (; len > 0; len--) {
                if (bufferstep) {
                    delta = inputbuffer & 0xf;
                } else {
                    inputbuffer = inBuffer[inp];
                    inp++;
                    delta = (inputbuffer >> 4) & 0xf;
                }
                bufferstep = !bufferstep;
                index += indexTable[delta];
                if (index < 0) index = 0;
                if (index > 88) index = 88;
                sign = delta & 8;
                delta = delta & 7;
                vpdiff = step >> 3;
                if ((delta & 4) != 0) vpdiff += step;
                if ((delta & 2) != 0) vpdiff += step >> 1;
                if ((delta & 1) != 0) vpdiff += step >> 2;
                if (sign != 0) valpred -= vpdiff; else valpred += vpdiff;
                if (valpred > 32767) valpred = 32767; else if (valpred < -32768) valpred = -32768;
                step = stepsizeTable[index];
                if (isBigEndian()) {
                    outBuffer[outp++] = (byte) (valpred >> 8);
                    outBuffer[outp++] = (byte) (valpred & 0xFF);
                } else {
                    outBuffer[outp++] = (byte) (valpred & 0xFF);
                    outBuffer[outp++] = (byte) (valpred >> 8);
                }
            }
            m_state.valprev = valpred;
            m_state.index = index;
            if (TDebug.TraceAudioConverter) {
                TDebug.out("DecodedImaAdpcmAudioInputStream.convert(): end");
            }
            return inFrameCount;
        }

        /**
		 */
        protected int getSampleSizeInBytes() {
            return getFormat().getFrameSize() / getFormat().getChannels();
        }

        /** .
		    @return .
		*/
        protected int getFrameSize() {
            return getFormat().getFrameSize();
        }

        /** Returns if this stream (the decoded one) is big endian.
		    @return true if this stream is big endian.
		*/
        private boolean isBigEndian() {
            return getFormat().isBigEndian();
        }
    }

    public static class EncodedImaAdpcmAudioInputStream extends TSynchronousFilteredAudioInputStream {

        private ImaAdpcmState m_state;

        /**
		 * Constructor.
		 */
        public EncodedImaAdpcmAudioInputStream(AudioInputStream decodedStream, AudioFormat outputFormat) {
            super(decodedStream, outputFormat);
            if (TDebug.TraceAudioConverter) {
                TDebug.out("EncodedImaAdpcmAudioInputStream.<init>(): begin");
            }
            m_state = new ImaAdpcmState();
            if (TDebug.TraceAudioConverter) {
                TDebug.out("EncodedImaAdpcmAudioInputStream.<init>(): end");
            }
        }

        protected int convert(byte[] inBuffer, byte[] outBuffer, int outByteOffset, int inFrameCount) {
            if (TDebug.TraceAudioConverter) {
                TDebug.out("EncodedImaAdpcmAudioInputStream.convert(): begin");
            }
            int inp;
            int outp;
            int val;
            int sign;
            int delta;
            int diff;
            int step;
            int valpred;
            int vpdiff;
            int index;
            int outputbuffer = 0;
            boolean bufferstep;
            int len = inFrameCount;
            inp = 0;
            outp = outByteOffset;
            valpred = m_state.valprev;
            index = m_state.index;
            step = stepsizeTable[index];
            bufferstep = true;
            for (; len > 0; len--) {
                val = isBigEndian() ? ((inBuffer[inp] << 8) | (inBuffer[inp + 1] & 0xFF)) : ((inBuffer[inp + 1] << 8) | (inBuffer[inp] & 0xFF));
                inp += 2;
                diff = val - valpred;
                sign = (diff < 0) ? 8 : 0;
                if (sign != 0) diff = (-diff);
                delta = 0;
                vpdiff = (step >> 3);
                if (diff >= step) {
                    delta = 4;
                    diff -= step;
                    vpdiff += step;
                }
                step >>= 1;
                if (diff >= step) {
                    delta |= 2;
                    diff -= step;
                    vpdiff += step;
                }
                step >>= 1;
                if (diff >= step) {
                    delta |= 1;
                    vpdiff += step;
                }
                if (sign != 0) valpred -= vpdiff; else valpred += vpdiff;
                if (valpred > 32767) valpred = 32767; else if (valpred < -32768) valpred = -32768;
                delta |= sign;
                index += indexTable[delta];
                if (index < 0) index = 0;
                if (index > 88) index = 88;
                step = stepsizeTable[index];
                if (bufferstep) {
                    outputbuffer = (delta << 4) & 0xf0;
                } else {
                    outBuffer[outp++] = (byte) ((delta & 0x0f) | outputbuffer);
                }
                bufferstep = !bufferstep;
            }
            if (!bufferstep) outBuffer[outp++] = (byte) outputbuffer;
            m_state.valprev = valpred;
            m_state.index = index;
            if (TDebug.TraceAudioConverter) {
                TDebug.out("EncodedImaAdpcmAudioInputStream.convert(): end");
            }
            return inFrameCount;
        }

        /**
		 */
        protected int getSampleSizeInBytes() {
            return getFormat().getFrameSize() / getFormat().getChannels();
        }

        /** .
		    @return .
		*/
        protected int getFrameSize() {
            return getFormat().getFrameSize();
        }

        /** Returns if this stream (the decoded one) is big endian.
		    @return true if this stream is big endian.
		*/
        private boolean isBigEndian() {
            return getFormat().isBigEndian();
        }
    }

    /** persistent state of a IMA ADPCM decoder.
	    This state class contains the information that
	    has to be passed between two blocks that are encoded or
	    decoded.
	*/
    private static class ImaAdpcmState {

        public int valprev;

        public int index;
    }
}
