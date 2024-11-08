package net.sf.fmj.media.codec;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.media.*;
import javax.media.format.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.*;
import net.sf.fmj.media.*;
import net.sf.fmj.media.renderer.audio.*;
import net.sf.fmj.utility.*;

/**
 * Converts formats that JavaSound can convert.
 * 
 * This has to do some tricks because of the delay in getting data from the
 * converted audio output stream. The streams are designed as streams, not
 * buffer-based, so all our threading and buffer queue tricks mean that we don't
 * get an output buffer right away for an input one. The missing output buffers
 * build up. And then we get EOM, and if the graph processing isn't done right,
 * most of the output buffers never make it to the renderer.
 * 
 * TODO: if this is put ahead of com.ibm.media.codec.audio.PCMToPCM in the
 * registry, there are problems playing the safexmas movie. TODO: this should
 * perhaps be in the net.sf.fmj.media.codec.audio package.
 * 
 * @author Ken Larson
 * 
 */
public class JavaSoundCodec extends AbstractCodec {

    private class AudioInputStreamThread extends Thread {

        private final BufferQueueInputStream bufferQueueInputStream;

        public AudioInputStreamThread(final BufferQueueInputStream bufferQueueInputStream) {
            super();
            this.bufferQueueInputStream = bufferQueueInputStream;
        }

        @Override
        public void run() {
            try {
                audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(bufferQueueInputStream));
            } catch (UnsupportedAudioFileException e) {
                logger.log(Level.WARNING, "" + e, e);
                return;
            } catch (IOException e) {
                logger.log(Level.WARNING, "" + e, e);
                return;
            }
            final javax.sound.sampled.AudioFormat javaSoundAudioFormat = JavaSoundUtils.convertFormat((AudioFormat) outputFormat);
            logger.fine("javaSoundAudioFormat converted (out)=" + javaSoundAudioFormat);
            audioInputStreamConverted = AudioSystem.getAudioInputStream(javaSoundAudioFormat, audioInputStream);
        }
    }

    private static final Logger logger = LoggerSingleton.logger;

    private BufferQueueInputStream bufferQueueInputStream;

    private volatile AudioInputStream audioInputStream;

    private volatile AudioInputStream audioInputStreamConverted;

    private AudioInputStreamThread audioInputStreamThread;

    private boolean trace;

    private int totalIn;

    private int totalOut;

    private static final int SIZEOF_INT = 4;

    private static final int SIZEOF_LONG = 8;

    private static final int SIZEOF_SHORT = 2;

    private static final int BITS_PER_BYTE = 8;

    private static final int MAX_SIGNED_BYTE = 127;

    private static final int MAX_BYTE = 0xFF;

    private static final int MAX_BYTE_PLUS1 = 256;

    /**
     * Used to create a "fake" AU header for fakeHeader. See
     * http://en.wikipedia.org/wiki/Au_file_format. The Au file format is a
     * simple audio file format that consists of a header of 6 32-bit words and
     * then the data (high-order byte comes first). The format was introduced by
     * Sun Microsystems.
     */
    public static byte[] createAuHeader(javax.sound.sampled.AudioFormat f) {
        byte[] result = new byte[4 * 6];
        encodeIntBE(0x2e736e64, result, 0);
        encodeIntBE(result.length, result, 4);
        encodeIntBE(0xffffffff, result, 8);
        final int encoding;
        if (f.getEncoding() == Encoding.ALAW) {
            if (f.getSampleSizeInBits() == 8) encoding = 27; else return null;
        } else if (f.getEncoding() == Encoding.ULAW) {
            if (f.getSampleSizeInBits() == 8) encoding = 1; else return null;
        } else if (f.getEncoding() == Encoding.PCM_SIGNED) {
            if (f.getSampleSizeInBits() == 8) encoding = 2; else if (f.getSampleSizeInBits() == 16) encoding = 3; else if (f.getSampleSizeInBits() == 24) encoding = 4; else if (f.getSampleSizeInBits() == 32) encoding = 5; else return null;
            if (f.getSampleSizeInBits() > 8 && !f.isBigEndian()) return null;
        } else if (f.getEncoding() == Encoding.PCM_UNSIGNED) {
            return null;
        } else {
            return null;
        }
        encodeIntBE(encoding, result, 12);
        if (f.getSampleRate() < 0) return null;
        encodeIntBE((int) f.getSampleRate(), result, 16);
        if (f.getChannels() < 0) return null;
        encodeIntBE(f.getChannels(), result, 20);
        return result;
    }

    /**
     * See http://ccrma.stanford.edu/courses/422/projects/WaveFormat/.
     * 
     * @param f
     * @return the header for the wav
     */
    public static byte[] createWavHeader(javax.sound.sampled.AudioFormat f) {
        if (f.getEncoding() != Encoding.PCM_SIGNED && f.getEncoding() != Encoding.PCM_UNSIGNED) return null;
        if (f.getSampleSizeInBits() == 8 && f.getEncoding() != Encoding.PCM_UNSIGNED) return null;
        if (f.getSampleSizeInBits() == 16 && f.getEncoding() != Encoding.PCM_SIGNED) return null;
        byte[] result = new byte[44];
        if (f.getSampleSizeInBits() > 8 && f.isBigEndian()) encodeIntBE(0x52494658, result, 0); else encodeIntBE(0x52494646, result, 0);
        int len = Integer.MAX_VALUE;
        encodeIntLE(len + result.length - 8, result, 4);
        encodeIntBE(0x57415645, result, 8);
        encodeIntBE(0x666d7420, result, 12);
        encodeIntLE(16, result, 16);
        encodeShortLE((short) 1, result, 20);
        encodeShortLE((short) f.getChannels(), result, 22);
        encodeIntLE((int) f.getSampleRate(), result, 24);
        encodeIntLE((((int) f.getSampleRate()) * f.getChannels() * f.getSampleSizeInBits()) / 8, result, 28);
        encodeShortLE((short) ((f.getChannels() * f.getSampleSizeInBits()) / 8), result, 32);
        encodeShortLE((short) f.getSampleSizeInBits(), result, 34);
        encodeIntBE(0x64617461, result, 36);
        encodeIntLE(len, result, 40);
        return result;
    }

    private static void encodeIntBE(int value, byte[] ba, int offset) {
        int length = SIZEOF_INT;
        for (int i = 0; i < length; ++i) {
            int byteValue = value & MAX_BYTE;
            if (byteValue > MAX_SIGNED_BYTE) byteValue = byteValue - MAX_BYTE_PLUS1;
            ba[offset + (length - i - 1)] = (byte) byteValue;
            value = value >> BITS_PER_BYTE;
        }
    }

    private static void encodeIntLE(int value, byte[] ba, int offset) {
        int length = SIZEOF_INT;
        for (int i = 0; i < length; ++i) {
            int byteValue = value & MAX_BYTE;
            if (byteValue > MAX_SIGNED_BYTE) byteValue = byteValue - MAX_BYTE_PLUS1;
            ba[offset + i] = (byte) byteValue;
            value = value >> BITS_PER_BYTE;
        }
    }

    public static void encodeShortBE(short value, byte[] ba, int offset) {
        int length = SIZEOF_SHORT;
        for (int i = 0; i < length; ++i) {
            int byteValue = value & MAX_BYTE;
            if (byteValue > MAX_SIGNED_BYTE) byteValue = byteValue - MAX_BYTE_PLUS1;
            ba[offset + (length - i - 1)] = (byte) byteValue;
            value = (short) (value >> BITS_PER_BYTE);
        }
    }

    public static void encodeShortLE(short value, byte[] ba, int offset) {
        int length = SIZEOF_SHORT;
        for (int i = 0; i < length; ++i) {
            int byteValue = value & MAX_BYTE;
            if (byteValue > MAX_SIGNED_BYTE) byteValue = byteValue - MAX_BYTE_PLUS1;
            ba[offset + i] = (byte) byteValue;
            value = (short) (value >> BITS_PER_BYTE);
        }
    }

    /**
     * See the comments in JavaSoundParser for more info.
     * AudioSystem.getAudioInputStream lets us specify the output format, but it
     * does not let us specify the input format. It gets the input format by
     * reading the stream. However, the parser has already stripped off the
     * headers, and the data we are getting is pure. To use JavaSound, we have
     * to create a fake header based on the input format information. It does
     * not matter whether this header matches the original header, it just has
     * to get AudioSystem.getAudioInputStream To figure out what kind of format
     * it is.
     * 
     */
    private static byte[] fakeHeader(javax.sound.sampled.AudioFormat f) {
        Class classVorbisAudioFormat = null;
        Class classMpegAudioFormatt = null;
        if (!JavaSoundUtils.onlyStandardFormats) {
            try {
                classMpegAudioFormatt = Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFormat");
                classVorbisAudioFormat = Class.forName("javazoom.spi.vorbis.sampled.file.VorbisAudioFormat");
            } catch (Exception dontcare) {
            }
        }
        if ((null != classMpegAudioFormatt) && classMpegAudioFormatt.isInstance(f)) {
            return new byte[0];
        }
        if ((null != classVorbisAudioFormat) && classVorbisAudioFormat.isInstance(f)) {
            return new byte[0];
        }
        byte[] result = createAuHeader(f);
        if (result != null) return result;
        result = createWavHeader(f);
        if (result != null) return result;
        return null;
    }

    public JavaSoundCodec() {
        Vector<Format> formats = new Vector<Format>();
        formats.add(new AudioFormat(AudioFormat.ULAW));
        formats.add(new AudioFormat(AudioFormat.ALAW));
        formats.add(new AudioFormat(AudioFormat.LINEAR));
        if (!JavaSoundUtils.onlyStandardFormats) {
            try {
                Class classMpegEncoding = Class.forName("javazoom.spi.mpeg.sampled.file.MpegEncoding");
                final String[] mpegEncodingStrings = { "MPEG1L1", "MPEG1L2", "MPEG1L3", "MPEG2DOT5L1", "MPEG2DOT5L2", "MPEG2DOT5L3", "MPEG2L1", "MPEG2L2", "MPEG2L3" };
                for (int i = 0; i < mpegEncodingStrings.length; i++) {
                    formats.add(new AudioFormat(mpegEncodingStrings[i]));
                }
            } catch (Exception dontcare) {
            }
            try {
                Class classVorbisEncoding = Class.forName("javazoom.spi.vorbis.sampled.file.VorbisEncoding");
                final String[] vorbisEncodingStrings = { "VORBISENC" };
                for (int i = 0; i < vorbisEncodingStrings.length; i++) {
                    formats.add(new AudioFormat(vorbisEncodingStrings[i]));
                }
            } catch (Exception dontcare) {
            }
        }
        inputFormats = new Format[formats.size()];
        formats.toArray(inputFormats);
    }

    @Override
    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) {
            return new Format[] { new AudioFormat(AudioFormat.LINEAR) };
        }
        final javax.sound.sampled.AudioFormat javaSoundFormat = JavaSoundUtils.convertFormat((AudioFormat) input);
        final javax.sound.sampled.AudioFormat[] targets1 = AudioSystem.getTargetFormats(Encoding.PCM_UNSIGNED, javaSoundFormat);
        final javax.sound.sampled.AudioFormat[] targets2 = AudioSystem.getTargetFormats(Encoding.PCM_SIGNED, javaSoundFormat);
        final javax.sound.sampled.AudioFormat[] targetsSpecial;
        Class classVorbisAudioFormat = null;
        Class classMpegAudioFormatt = null;
        if (!JavaSoundUtils.onlyStandardFormats) {
            try {
                classMpegAudioFormatt = Class.forName("javazoom.spi.mpeg.sampled.file.MpegAudioFormat");
                classVorbisAudioFormat = Class.forName("javazoom.spi.vorbis.sampled.file.VorbisAudioFormat");
            } catch (Exception dontcare) {
            }
        }
        if ((null != classMpegAudioFormatt) && classMpegAudioFormatt.isInstance(javaSoundFormat)) {
            javax.sound.sampled.AudioFormat decodedFormat = new javax.sound.sampled.AudioFormat(javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, javaSoundFormat.getSampleRate(), 16, javaSoundFormat.getChannels(), javaSoundFormat.getChannels() * 2, javaSoundFormat.getSampleRate(), false);
            targetsSpecial = new javax.sound.sampled.AudioFormat[] { decodedFormat };
        } else if ((null != classVorbisAudioFormat) && classVorbisAudioFormat.isInstance(javaSoundFormat)) {
            javax.sound.sampled.AudioFormat decodedFormat = new javax.sound.sampled.AudioFormat(javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED, javaSoundFormat.getSampleRate(), 16, javaSoundFormat.getChannels(), javaSoundFormat.getChannels() * 2, javaSoundFormat.getSampleRate(), false);
            targetsSpecial = new javax.sound.sampled.AudioFormat[] { decodedFormat };
        } else {
            targetsSpecial = new javax.sound.sampled.AudioFormat[0];
        }
        final Format[] result = new Format[targets1.length + targets2.length + targetsSpecial.length];
        for (int i = 0; i < targets1.length; ++i) {
            result[i] = JavaSoundUtils.convertFormat(targets1[i]);
            logger.finer("getSupportedOutputFormats: " + result[i]);
        }
        for (int i = 0; i < targets2.length; ++i) {
            result[targets1.length + i] = JavaSoundUtils.convertFormat(targets2[i]);
            logger.finer("getSupportedOutputFormats: " + result[targets1.length + i]);
        }
        for (int i = 0; i < targetsSpecial.length; ++i) {
            result[targets1.length + targets2.length + i] = JavaSoundUtils.convertFormat(targetsSpecial[i]);
            logger.finer("getSupportedOutputFormats: " + result[targets1.length + targets2.length + i]);
        }
        for (int i = 0; i < result.length; ++i) {
            AudioFormat a = ((AudioFormat) result[i]);
            AudioFormat inputAudioFormat = (AudioFormat) input;
            if (FormatUtils.specified(inputAudioFormat.getSampleRate()) && !FormatUtils.specified(a.getSampleRate())) result[i] = null;
        }
        return result;
    }

    @Override
    public void open() throws ResourceUnavailableException {
        super.open();
        bufferQueueInputStream = new BufferQueueInputStream();
        final javax.sound.sampled.AudioFormat javaSoundAudioFormat = JavaSoundUtils.convertFormat((AudioFormat) inputFormat);
        logger.fine("javaSoundAudioFormat converted (in)=" + javaSoundAudioFormat);
        final byte[] header = fakeHeader(javaSoundAudioFormat);
        if (header == null) throw new ResourceUnavailableException("Unable to reconstruct header for format: " + inputFormat);
        if (header.length > 0) {
            Buffer headerBuffer = new Buffer();
            headerBuffer.setData(header);
            headerBuffer.setLength(header.length);
            bufferQueueInputStream.put(headerBuffer);
        }
        audioInputStreamThread = new AudioInputStreamThread(bufferQueueInputStream);
        audioInputStreamThread.start();
    }

    @Override
    public int process(Buffer input, Buffer output) {
        if (!checkInputBuffer(input)) {
            return BUFFER_PROCESSED_FAILED;
        }
        try {
            if (trace) logger.fine("process: " + LoggingStringUtils.bufferToStr(input));
            totalIn += input.getLength();
            final boolean noRoomInBufferQueue;
            noRoomInBufferQueue = !bufferQueueInputStream.put(input);
            if (audioInputStreamConverted == null) {
                if (noRoomInBufferQueue) {
                    logger.fine("JavaSoundCodec: audioInputStreamConverted == null, blocking until not null");
                    try {
                        while (audioInputStreamConverted == null) Thread.sleep(100);
                    } catch (InterruptedException e) {
                        return BUFFER_PROCESSED_FAILED;
                    }
                } else {
                    logger.fine("JavaSoundCodec: audioInputStreamConverted == null, returning OUTPUT_BUFFER_NOT_FILLED");
                    output.setLength(0);
                    return OUTPUT_BUFFER_NOT_FILLED;
                }
            }
            final int avail = audioInputStreamConverted.available();
            if (trace) logger.fine("audioInputStreamConverted.available() == " + avail + ", bufferQueueInputStream.available() = " + bufferQueueInputStream.available());
            if (output.getData() == null) output.setData(new byte[10000]);
            output.setFormat(getOutputFormat());
            final byte[] data = (byte[]) output.getData();
            int lenToRead;
            if (noRoomInBufferQueue || input.isEOM()) lenToRead = data.length; else lenToRead = avail > data.length ? data.length : avail;
            if (lenToRead == 0) {
                logger.finer("JavaSoundCodec: lenToRead == 0, returning OUTPUT_BUFFER_NOT_FILLED.  input.isEOM()=" + input.isEOM());
                output.setLength(0);
                return OUTPUT_BUFFER_NOT_FILLED;
            }
            final int lenRead = audioInputStreamConverted.read(data, 0, lenToRead);
            logger.finer("JavaSoundCodec: Read from audioInputStreamConverted: " + lenRead);
            if (lenRead == -1) {
                logger.fine("total in: " + totalIn + " total out: " + totalOut);
                output.setEOM(true);
                output.setLength(0);
                return BUFFER_PROCESSED_OK;
            }
            output.setLength(lenRead);
            totalOut += lenRead;
            return (noRoomInBufferQueue || input.isEOM()) ? INPUT_BUFFER_NOT_CONSUMED : BUFFER_PROCESSED_OK;
        } catch (IOException e) {
            output.setLength(0);
            return BUFFER_PROCESSED_FAILED;
        }
    }

    void setTrace(boolean value) {
        this.trace = value;
    }
}
