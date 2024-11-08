package sound.spi;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;

/**
 * A format conversion provider provides format conversion services from one or
 * more input formats to one or more output formats. Converters include codecs,
 * which encode and/or decode audio data, as well as transcoders, etc. Format
 * converters provide methods for determining what conversions are supported and
 * for obtaining an audio stream from which converted data can be read. The
 * source format represents the format of the incoming audio data, which will be
 * converted. The target format represents the format of the processed,
 * converted audio data. This is the format of the data that can be read from
 * the stream returned by one of the getAudioInputStream methods.
 * 
 * @author Marc Gimpel, Wimba S.A. (marc@wimba.com)
 * @author Florian Bomers
 * @version $Revision: 1.1 $
 */
public class FlacFormatConversionProvider extends FormatConversionProvider {

    /** debugging flag */
    private static final boolean DEBUG = false;

    /** to disable encoding */
    private static final boolean HAS_ENCODING = false;

    /**
	 * Obtains the set of source format encodings from which format conversion
	 * services are provided by this provider.
	 * 
	 * @return array of source format encodings. The array will always have a
	 *         length of at least 1.
	 */
    public AudioFormat.Encoding[] getSourceEncodings() {
        if (HAS_ENCODING) {
            AudioFormat.Encoding[] encodings = { FlacEncoding.FLAC, AudioFormat.Encoding.PCM_SIGNED };
            return encodings;
        } else {
            AudioFormat.Encoding[] encodings = { FlacEncoding.FLAC };
            return encodings;
        }
    }

    /**
	 * Obtains the set of target format encodings to which format conversion
	 * services are provided by this provider.
	 * 
	 * @return array of target format encodings. The array will always have a
	 *         length of at least 1.
	 */
    public AudioFormat.Encoding[] getTargetEncodings() {
        if (HAS_ENCODING) {
            AudioFormat.Encoding[] encodings = { FlacEncoding.FLAC, AudioFormat.Encoding.PCM_SIGNED };
            return encodings;
        } else {
            AudioFormat.Encoding[] encodings = { FlacEncoding.PCM_SIGNED };
            return encodings;
        }
    }

    private boolean isBitSizeOK(AudioFormat format, boolean notSpecifiedOK) {
        int bitSize = format.getSampleSizeInBits();
        return (notSpecifiedOK && (bitSize == AudioSystem.NOT_SPECIFIED)) || (bitSize == 8) || (bitSize == 16) || (bitSize == 24);
    }

    private boolean isChannelsOK(AudioFormat format, boolean notSpecifiedOK) {
        int channels = format.getChannels();
        return (notSpecifiedOK && (channels == AudioSystem.NOT_SPECIFIED)) || (channels == 1) || (channels == 2);
    }

    /**
	 * Obtains the set of target format encodings supported by the format
	 * converter given a particular source format. If no target format encodings
	 * are supported for this source format, an array of length 0 is returned.
	 * 
	 * @param sourceFormat format of the incoming data.
	 * @return array of supported target format encodings.
	 */
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        boolean bitSizeOK = isBitSizeOK(sourceFormat, true);
        boolean channelsOK = isChannelsOK(sourceFormat, true);
        if (HAS_ENCODING && bitSizeOK && channelsOK && !sourceFormat.isBigEndian() && sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            if (DEBUG) {
                System.out.println("FLAC converter: can encode to PCM: " + sourceFormat);
            }
            AudioFormat.Encoding[] encodings = { FlacEncoding.FLAC };
            return encodings;
        } else if (bitSizeOK && channelsOK && sourceFormat.getEncoding().equals(FlacEncoding.FLAC)) {
            if (DEBUG) {
                System.out.println("FLAC converter: can decode to FLAC: " + sourceFormat);
            }
            AudioFormat.Encoding[] encodings = { AudioFormat.Encoding.PCM_SIGNED };
            return encodings;
        } else {
            if (DEBUG) {
                System.out.println("FLAC converter: cannot de/encode: " + sourceFormat);
            }
            AudioFormat.Encoding[] encodings = {};
            return encodings;
        }
    }

    /**
	 * Obtains the set of target formats with the encoding specified supported
	 * by the format converter. If no target formats with the specified encoding
	 * are supported for this source format, an array of length 0 is returned.
	 * 
	 * @param targetEncoding desired encoding of the outgoing data.
	 * @param sourceFormat format of the incoming data.
	 * @return array of supported target formats.
	 */
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        return getTargetFormats(targetEncoding, sourceFormat, true);
    }

    /**
	 * Like getTargetFormats(AudioFormat.Encoding, AudioFormat), but with
	 * additional choice if AudioSystem.NOT_SPECIFIED is supported in
	 * sourceFormat's bitSize or channels.
	 * 
	 * @param notSpecifiedOK if true, bitSize and channels can have value
	 *            AudioSystem.NOT_SPECIFIED
	 * @return array of supported target formats.
	 */
    private AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat, boolean notSpecifiedOK) {
        boolean bitSizeOK = isBitSizeOK(sourceFormat, notSpecifiedOK);
        boolean channelsOK = isChannelsOK(sourceFormat, notSpecifiedOK);
        if (HAS_ENCODING && bitSizeOK && channelsOK && !sourceFormat.isBigEndian() && sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetEncoding.equals(FlacEncoding.FLAC)) {
            if (DEBUG) {
                System.out.println("FLAC converter: can encode: " + sourceFormat + " to " + targetEncoding);
            }
            AudioFormat[] formats = { new AudioFormat(FlacEncoding.FLAC, sourceFormat.getSampleRate(), -1, sourceFormat.getChannels(), -1, -1, false) };
            return formats;
        } else if (bitSizeOK && channelsOK && sourceFormat.getEncoding().equals(FlacEncoding.FLAC) && targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            if (DEBUG) {
                System.out.println("FLAC converter: can decode: " + sourceFormat + " to " + targetEncoding);
            }
            AudioFormat[] formats = { new AudioFormat(sourceFormat.getSampleRate(), sourceFormat.getSampleSizeInBits(), sourceFormat.getChannels(), true, false) };
            return formats;
        } else {
            if (DEBUG) {
                System.out.println("FLAC converter: cannot de/encode: " + sourceFormat + " to " + targetEncoding);
            }
            AudioFormat[] formats = {};
            return formats;
        }
    }

    /**
	 * Obtains an audio input stream with the specified encoding from the given
	 * audio input stream.
	 * 
	 * @param targetEncoding - desired encoding of the stream after processing.
	 * @param sourceStream - stream from which data to be processed should be
	 *            read.
	 * @return stream from which processed data with the specified target
	 *         encoding may be read.
	 */
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        AudioFormat[] formats = getTargetFormats(targetEncoding, sourceStream.getFormat(), false);
        if (formats != null && formats.length > 0) {
            return getAudioInputStream(formats[0], sourceStream);
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }

    /**
	 * Obtains an audio input stream with the specified format from the given
	 * audio input stream.
	 * 
	 * @param targetFormat - desired data format of the stream after processing.
	 * @param sourceStream - stream from which data to be processed should be
	 *            read.
	 * @return stream from which processed data with the specified format may be
	 *         read.
	 */
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat[] formats = getTargetFormats(targetFormat.getEncoding(), sourceFormat, false);
        if (formats != null && formats.length > 0) {
            if (sourceFormat.equals(targetFormat)) {
                return sourceStream;
            } else if (sourceFormat.getChannels() == targetFormat.getChannels() && sourceFormat.getSampleSizeInBits() == targetFormat.getSampleSizeInBits() && !targetFormat.isBigEndian() && sourceFormat.getEncoding().equals(FlacEncoding.FLAC) && targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                return new Flac2PcmAudioInputStream(sourceStream, targetFormat, -1);
            } else if (sourceFormat.getChannels() == targetFormat.getChannels() && sourceFormat.getSampleSizeInBits() == targetFormat.getSampleSizeInBits() && sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding().equals(FlacEncoding.FLAC)) {
                throw new IllegalArgumentException("FLAC encoder not yet implemented");
            } else {
                throw new IllegalArgumentException("unable to convert " + sourceFormat.toString() + " to " + targetFormat.toString());
            }
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }
}
