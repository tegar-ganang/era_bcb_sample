package net.sf.fmj.media.codec.audio.ulaw;

import java.util.logging.*;
import javax.media.*;
import javax.media.format.*;
import net.sf.fmj.media.*;
import net.sf.fmj.utility.*;

/**
 * ULAW encoder Codec.
 * 
 * @author Ken Larson
 * 
 */
public class Encoder extends AbstractCodec {

    private static final Logger logger = LoggerSingleton.logger;

    protected Format[] outputFormats = new Format[] { new AudioFormat(AudioFormat.ULAW, -1.0, 8, 1, -1, AudioFormat.SIGNED, 8, -1.0, Format.byteArray) };

    private static final boolean TRACE = false;

    public Encoder() {
        super();
        this.inputFormats = new Format[] { new AudioFormat(AudioFormat.LINEAR, -1.0, 16, 1, -1, AudioFormat.SIGNED, 16, -1.0, Format.byteArray) };
    }

    @Override
    public void close() {
    }

    @Override
    public String getName() {
        return "ULAW Encoder";
    }

    @Override
    public Format[] getSupportedOutputFormats(Format input) {
        if (input == null) return outputFormats; else {
            if (!(input instanceof AudioFormat)) {
                logger.warning(this.getClass().getSimpleName() + ".getSupportedOutputFormats: input format does not match, returning format array of {null} for " + input);
                return new Format[] { null };
            }
            final AudioFormat inputCast = (AudioFormat) input;
            if (!inputCast.getEncoding().equals(AudioFormat.LINEAR) || (inputCast.getSampleSizeInBits() != 16 && inputCast.getSampleSizeInBits() != Format.NOT_SPECIFIED) || (inputCast.getChannels() != 1 && inputCast.getChannels() != Format.NOT_SPECIFIED) || (inputCast.getSigned() != AudioFormat.SIGNED && inputCast.getSigned() != Format.NOT_SPECIFIED) || (inputCast.getFrameSizeInBits() != 16 && inputCast.getFrameSizeInBits() != Format.NOT_SPECIFIED) || (inputCast.getDataType() != null && inputCast.getDataType() != Format.byteArray)) {
                logger.warning(this.getClass().getSimpleName() + ".getSupportedOutputFormats: input format does not match, returning format array of {null} for " + input);
                return new Format[] { null };
            }
            final AudioFormat result = new AudioFormat(AudioFormat.ULAW, inputCast.getSampleRate(), 8, 1, -1, AudioFormat.SIGNED, 8, inputCast.getFrameRate(), Format.byteArray);
            return new Format[] { result };
        }
    }

    @Override
    public void open() {
    }

    @Override
    public int process(Buffer inputBuffer, Buffer outputBuffer) {
        if (TRACE) dump("input ", inputBuffer);
        if (!checkInputBuffer(inputBuffer)) {
            return BUFFER_PROCESSED_FAILED;
        }
        if (isEOM(inputBuffer)) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }
        final AudioFormat inputAudioFormat = (AudioFormat) inputBuffer.getFormat();
        byte[] outputBufferData = (byte[]) outputBuffer.getData();
        final int requiredOutputBufferLength = inputBuffer.getLength() / 2;
        if (outputBufferData == null || outputBufferData.length < requiredOutputBufferLength) {
            outputBufferData = new byte[requiredOutputBufferLength];
            outputBuffer.setData(outputBufferData);
        }
        if (!inputAudioFormat.equals(inputFormat)) throw new RuntimeException("Incorrect input format");
        if (inputAudioFormat.getEndian() == -1) throw new RuntimeException("Unspecified endian-ness");
        final boolean bigEndian = inputAudioFormat.getEndian() == AudioFormat.BIG_ENDIAN;
        MuLawEncoderUtil.muLawEncode(bigEndian, (byte[]) inputBuffer.getData(), inputBuffer.getOffset(), inputBuffer.getLength(), outputBufferData);
        outputBuffer.setLength(requiredOutputBufferLength);
        outputBuffer.setOffset(0);
        outputBuffer.setFormat(outputFormat);
        final int result = BUFFER_PROCESSED_OK;
        if (TRACE) {
            dump("input ", inputBuffer);
            dump("output", outputBuffer);
            System.out.println("Result=" + LoggingStringUtils.plugInResultToStr(result));
        }
        return result;
    }

    @Override
    public Format setInputFormat(Format arg0) {
        return super.setInputFormat(arg0);
    }

    @Override
    public Format setOutputFormat(Format arg0) {
        return super.setOutputFormat(arg0);
    }
}
