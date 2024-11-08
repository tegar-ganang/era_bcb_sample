package org.tritonus.sampled.convert.javalayer;

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.tritonus.share.TDebug;
import org.tritonus.share.sampled.Encodings;
import org.tritonus.share.sampled.TConversionTool;
import org.tritonus.share.sampled.convert.TMatrixFormatConversionProvider;
import org.tritonus.share.sampled.convert.TAsynchronousFilteredAudioInputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.Obuffer;

/**
 * ConversionProvider for MPEG files.
 *
 * @author Matthias Pfisterer
 */
public class MpegFormatConversionProvider extends TMatrixFormatConversionProvider {

    public static final AudioFormat.Encoding MPEG1L1 = Encodings.getEncoding("MPEG1L1");

    public static final AudioFormat.Encoding MPEG1L2 = Encodings.getEncoding("MPEG1L2");

    public static final AudioFormat.Encoding MPEG1L3 = Encodings.getEncoding("MPEG1L3");

    public static final AudioFormat.Encoding MPEG2L1 = Encodings.getEncoding("MPEG2L1");

    public static final AudioFormat.Encoding MPEG2L2 = Encodings.getEncoding("MPEG2L2");

    public static final AudioFormat.Encoding MPEG2L3 = Encodings.getEncoding("MPEG2L3");

    public static final AudioFormat.Encoding MPEG2DOT5L1 = Encodings.getEncoding("MPEG2DOT5L1");

    public static final AudioFormat.Encoding MPEG2DOT5L2 = Encodings.getEncoding("MPEG2DOT5L2");

    public static final AudioFormat.Encoding MPEG2DOT5L3 = Encodings.getEncoding("MPEG2DOT5L3");

    private static final AudioFormat[] INPUT_FORMATS = { new AudioFormat(MPEG1L1, 32000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG1L1, 32000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG1L1, 44100.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG1L1, 44100.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG1L1, 48000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG1L1, 48000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG1L2, 32000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG1L2, 32000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG1L2, 44100.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG1L2, 44100.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG1L2, 48000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG1L2, 48000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG1L3, 32000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG1L3, 32000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG1L3, 44100.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG1L3, 44100.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG1L3, 48000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG1L3, 48000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2L1, 16000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2L1, 16000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2L1, 22050.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2L1, 22050.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2L1, 24000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2L1, 24000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2L2, 16000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2L2, 16000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2L2, 22050.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2L2, 22050.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2L2, 24000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2L2, 24000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2L3, 16000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2L3, 16000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2L3, 22050.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2L3, 22050.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2L3, 24000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2L3, 24000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2DOT5L1, 8000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2DOT5L1, 8000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2DOT5L1, 11025.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2DOT5L1, 11025.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2DOT5L1, 12000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2DOT5L1, 12000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2DOT5L2, 8000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2DOT5L2, 8000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2DOT5L2, 11025.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2DOT5L2, 11025.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2DOT5L2, 12000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2DOT5L2, 12000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2DOT5L3, 8000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2DOT5L3, 8000.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2DOT5L3, 11025.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2DOT5L3, 11025.0F, -1, 2, -1, -1, false), new AudioFormat(MPEG2DOT5L3, 12000.0F, -1, 1, -1, -1, false), new AudioFormat(MPEG2DOT5L3, 12000.0F, -1, 2, -1, -1, false) };

    private static final AudioFormat[] OUTPUT_FORMATS = { new AudioFormat(8000.0F, 16, 1, true, false), new AudioFormat(8000.0F, 16, 1, true, true), new AudioFormat(8000.0F, 16, 2, true, false), new AudioFormat(8000.0F, 16, 2, true, true), new AudioFormat(11025.0F, 16, 1, true, false), new AudioFormat(11025.0F, 16, 1, true, true), new AudioFormat(11025.0F, 16, 2, true, false), new AudioFormat(11025.0F, 16, 2, true, true), new AudioFormat(12000.0F, 16, 1, true, false), new AudioFormat(12000.0F, 16, 1, true, true), new AudioFormat(12000.0F, 16, 2, true, false), new AudioFormat(12000.0F, 16, 2, true, true), new AudioFormat(16000.0F, 16, 1, true, false), new AudioFormat(16000.0F, 16, 1, true, true), new AudioFormat(16000.0F, 16, 2, true, false), new AudioFormat(16000.0F, 16, 2, true, true), new AudioFormat(22050.0F, 16, 1, true, false), new AudioFormat(22050.0F, 16, 1, true, true), new AudioFormat(22050.0F, 16, 2, true, false), new AudioFormat(22050.0F, 16, 2, true, true), new AudioFormat(24000.0F, 16, 1, true, false), new AudioFormat(24000.0F, 16, 1, true, true), new AudioFormat(24000.0F, 16, 2, true, false), new AudioFormat(24000.0F, 16, 2, true, true), new AudioFormat(32000.0F, 16, 1, true, false), new AudioFormat(32000.0F, 16, 1, true, true), new AudioFormat(32000.0F, 16, 2, true, false), new AudioFormat(32000.0F, 16, 2, true, true), new AudioFormat(44100.0F, 16, 1, true, false), new AudioFormat(44100.0F, 16, 1, true, true), new AudioFormat(44100.0F, 16, 2, true, false), new AudioFormat(44100.0F, 16, 2, true, true), new AudioFormat(48000.0F, 16, 1, true, false), new AudioFormat(48000.0F, 16, 1, true, true), new AudioFormat(48000.0F, 16, 2, true, false), new AudioFormat(48000.0F, 16, 2, true, true) };

    private static final boolean t = true;

    private static final boolean f = false;

    private static final boolean[][] CONVERSIONS = { { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t }, { f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f }, { t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f }, { f, f, f, f, f, f, f, f, f, f, t, t, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f } };

    /**	Constructor.
	 */
    public MpegFormatConversionProvider() {
        super(Arrays.asList(INPUT_FORMATS), Arrays.asList(OUTPUT_FORMATS), CONVERSIONS);
        if (TDebug.TraceAudioConverter) {
            TDebug.out("MpegFormatConversionProvider.<init>(): begin");
        }
        if (TDebug.TraceAudioConverter) {
            TDebug.out("MpegFormatConversionProvider.<init>(): end");
        }
    }

    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream) {
        if (TDebug.TraceAudioConverter) {
            TDebug.out("MpegFormatConversionProvider.getAudioInputStream(AudioFormat, AudioInputStream):");
            TDebug.out("trying to convert");
            TDebug.out("\tfrom: " + audioInputStream.getFormat());
            TDebug.out("\tto: " + targetFormat);
        }
        AudioFormat matchingFormat = getMatchingFormat(targetFormat, audioInputStream.getFormat());
        if (matchingFormat != null) {
            if (TDebug.TraceAudioConverter) {
                TDebug.out("MpegFormatConversionProvider.getAudioInputStream(AudioFormat, AudioInputStream):");
                TDebug.out("\tisConversionSupported() accepted it; now setting up the conversion");
            }
            targetFormat = setUnspecifiedFieldsFromProto(targetFormat, matchingFormat);
            if (TDebug.TraceAudioConverter) {
                TDebug.out("MpegFormatConversionProvider.getAudioInputStream(AudioFormat, AudioInputStream):");
                TDebug.out("\tcompleted target format (1. stage): " + targetFormat);
            }
            targetFormat = setUnspecifiedFieldsFromProto(targetFormat, audioInputStream.getFormat());
            if (TDebug.TraceAudioConverter) {
                TDebug.out("MpegFormatConversionProvider.getAudioInputStream(AudioFormat, AudioInputStream):");
                TDebug.out("\tcompleted target format (2. stage): " + targetFormat);
            }
            return new DecodedMpegAudioInputStream(targetFormat, audioInputStream);
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }

    private static AudioFormat setUnspecifiedFieldsFromProto(AudioFormat incomplete, AudioFormat prototype) {
        AudioFormat format = new AudioFormat(incomplete.getEncoding(), getSpecificValue(incomplete.getSampleRate(), prototype.getSampleRate()), getSpecificValue(incomplete.getSampleSizeInBits(), prototype.getSampleSizeInBits()), getSpecificValue(incomplete.getChannels(), prototype.getChannels()), getSpecificValue(incomplete.getFrameSize(), prototype.getFrameSize()), getSpecificValue(incomplete.getFrameRate(), prototype.getFrameRate()), incomplete.isBigEndian());
        return format;
    }

    private static float getSpecificValue(float fIncomplete, float fProto) {
        return (fIncomplete == AudioSystem.NOT_SPECIFIED) ? fProto : fIncomplete;
    }

    private static int getSpecificValue(int nIncomplete, int nProto) {
        return (nIncomplete == AudioSystem.NOT_SPECIFIED) ? nProto : nIncomplete;
    }

    public static class DecodedMpegAudioInputStream extends TAsynchronousFilteredAudioInputStream {

        private InputStream m_encodedStream;

        private Bitstream m_bitstream;

        private Decoder m_decoder;

        private DMAISObuffer m_oBuffer;

        public DecodedMpegAudioInputStream(AudioFormat outputFormat, AudioInputStream inputStream) {
            super(outputFormat, AudioSystem.NOT_SPECIFIED);
            m_encodedStream = inputStream;
            m_bitstream = new Bitstream(inputStream);
            m_decoder = new Decoder(null);
            m_oBuffer = new DMAISObuffer(outputFormat.getChannels());
            m_decoder.setOutputBuffer(m_oBuffer);
        }

        public void execute() {
            try {
                Header header = m_bitstream.readFrame();
                if (header == null) {
                    if (TDebug.TraceAudioConverter) {
                        TDebug.out("header is null (end of mpeg stream)");
                    }
                    getCircularBuffer().close();
                    return;
                }
                Obuffer decoderOutput = m_decoder.decodeFrame(header, m_bitstream);
                m_bitstream.closeFrame();
                getCircularBuffer().write(m_oBuffer.getBuffer(), 0, m_oBuffer.getCurrentBufferSize());
                m_oBuffer.reset();
            } catch (BitstreamException e) {
                if (TDebug.TraceAudioConverter || TDebug.TraceAllExceptions) {
                    TDebug.out(e);
                }
            } catch (DecoderException e) {
                if (TDebug.TraceAudioConverter || TDebug.TraceAllExceptions) {
                    TDebug.out(e);
                }
            }
        }

        private boolean isBigEndian() {
            return getFormat().isBigEndian();
        }

        public void close() throws IOException {
            super.close();
            m_encodedStream.close();
        }

        private class DMAISObuffer extends Obuffer {

            private int m_nChannels;

            private byte[] m_abBuffer;

            private int[] m_anBufferPointers;

            private boolean m_bIsBigEndian;

            public DMAISObuffer(int nChannels) {
                m_nChannels = nChannels;
                m_abBuffer = new byte[OBUFFERSIZE * nChannels];
                m_anBufferPointers = new int[nChannels];
                reset();
                m_bIsBigEndian = DecodedMpegAudioInputStream.this.isBigEndian();
            }

            public void append(int nChannel, short sValue) {
                TConversionTool.shortToBytes16(sValue, m_abBuffer, m_anBufferPointers[nChannel], m_bIsBigEndian);
                m_anBufferPointers[nChannel] += m_nChannels * 2;
            }

            public void set_stop_flag() {
            }

            public void close() {
            }

            public void write_buffer(int nValue) {
            }

            public void clear_buffer() {
            }

            public byte[] getBuffer() {
                return m_abBuffer;
            }

            public int getCurrentBufferSize() {
                return m_anBufferPointers[0];
            }

            public void reset() {
                for (int i = 0; i < m_nChannels; i++) {
                    m_anBufferPointers[i] = i * 2;
                }
            }
        }
    }
}
