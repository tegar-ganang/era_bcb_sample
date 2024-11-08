package org.tritonus.share.sampled.convert;

import java.util.Collection;
import java.util.Iterator;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import org.tritonus.share.sampled.AudioFormats;
import org.tritonus.share.ArraySet;
import org.tritonus.share.TDebug;

public abstract class TSimpleFormatConversionProvider extends TFormatConversionProvider {

    private Collection m_sourceEncodings;

    private Collection m_targetEncodings;

    private Collection m_sourceFormats;

    private Collection m_targetFormats;

    protected TSimpleFormatConversionProvider(Collection sourceFormats, Collection targetFormats) {
        m_sourceEncodings = new ArraySet();
        m_targetEncodings = new ArraySet();
        m_sourceFormats = sourceFormats;
        m_targetFormats = targetFormats;
        collectEncodings(m_sourceFormats, m_sourceEncodings);
        collectEncodings(m_targetFormats, m_targetEncodings);
    }

    /**	Disables this FormatConversionProvider.
		This may be useful when e.g. native libraries are not present.
		TODO: enable method, better implementation
	*/
    protected void disable() {
        if (TDebug.TraceAudioConverter) {
            TDebug.out("TSimpleFormatConversionProvider.disable(): disabling " + getClass().getName());
        }
        m_sourceEncodings = new ArraySet();
        m_targetEncodings = new ArraySet();
        m_sourceFormats = new ArraySet();
        m_targetFormats = new ArraySet();
    }

    private static void collectEncodings(Collection formats, Collection encodings) {
        Iterator iterator = formats.iterator();
        while (iterator.hasNext()) {
            AudioFormat format = (AudioFormat) iterator.next();
            encodings.add(format.getEncoding());
        }
    }

    public AudioFormat.Encoding[] getSourceEncodings() {
        return (AudioFormat.Encoding[]) m_sourceEncodings.toArray(EMPTY_ENCODING_ARRAY);
    }

    public AudioFormat.Encoding[] getTargetEncodings() {
        return (AudioFormat.Encoding[]) m_targetEncodings.toArray(EMPTY_ENCODING_ARRAY);
    }

    public boolean isSourceEncodingSupported(AudioFormat.Encoding sourceEncoding) {
        return m_sourceEncodings.contains(sourceEncoding);
    }

    public boolean isTargetEncodingSupported(AudioFormat.Encoding targetEncoding) {
        return m_targetEncodings.contains(targetEncoding);
    }

    /**
	 *	This implementation assumes that the converter can convert
	 *	from each of its source encodings to each of its target
	 *	encodings. If this is not the case, the converter has to
	 *	override this method.
	 */
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (isAllowedSourceFormat(sourceFormat)) {
            return getTargetEncodings();
        } else {
            return EMPTY_ENCODING_ARRAY;
        }
    }

    /**
	 *	This implementation assumes that the converter can convert
	 *	from each of its source formats to each of its target
	 *	formats. If this is not the case, the converter has to
	 *	override this method.
	 */
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (isConversionSupported(targetEncoding, sourceFormat)) {
            return (AudioFormat[]) m_targetFormats.toArray(EMPTY_FORMAT_ARRAY);
        } else {
            return EMPTY_FORMAT_ARRAY;
        }
    }

    protected boolean isAllowedSourceEncoding(AudioFormat.Encoding sourceEncoding) {
        return m_sourceEncodings.contains(sourceEncoding);
    }

    protected boolean isAllowedTargetEncoding(AudioFormat.Encoding targetEncoding) {
        return m_targetEncodings.contains(targetEncoding);
    }

    protected boolean isAllowedSourceFormat(AudioFormat sourceFormat) {
        Iterator iterator = m_sourceFormats.iterator();
        while (iterator.hasNext()) {
            AudioFormat format = (AudioFormat) iterator.next();
            if (AudioFormats.matches(format, sourceFormat)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isAllowedTargetFormat(AudioFormat targetFormat) {
        Iterator iterator = m_targetFormats.iterator();
        while (iterator.hasNext()) {
            AudioFormat format = (AudioFormat) iterator.next();
            if (AudioFormats.matches(format, targetFormat)) {
                return true;
            }
        }
        return false;
    }

    protected Collection getCollectionSourceEncodings() {
        return m_sourceEncodings;
    }

    protected Collection getCollectionTargetEncodings() {
        return m_targetEncodings;
    }

    protected Collection getCollectionSourceFormats() {
        return m_sourceFormats;
    }

    protected Collection getCollectionTargetFormats() {
        return m_targetFormats;
    }

    protected static boolean doMatch(int i1, int i2) {
        return i1 == AudioSystem.NOT_SPECIFIED || i2 == AudioSystem.NOT_SPECIFIED || i1 == i2;
    }

    protected static boolean doMatch(float f1, float f2) {
        return f1 == AudioSystem.NOT_SPECIFIED || f2 == AudioSystem.NOT_SPECIFIED || Math.abs(f1 - f2) < 1.0e-9;
    }

    protected AudioFormat replaceNotSpecified(AudioFormat sourceFormat, AudioFormat targetFormat) {
        boolean bSetSampleSize = false;
        boolean bSetChannels = false;
        boolean bSetSampleRate = false;
        boolean bSetFrameRate = false;
        if (targetFormat.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED && sourceFormat.getSampleSizeInBits() != AudioSystem.NOT_SPECIFIED) {
            bSetSampleSize = true;
        }
        if (targetFormat.getChannels() == AudioSystem.NOT_SPECIFIED && sourceFormat.getChannels() != AudioSystem.NOT_SPECIFIED) {
            bSetChannels = true;
        }
        if (targetFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED && sourceFormat.getSampleRate() != AudioSystem.NOT_SPECIFIED) {
            bSetSampleRate = true;
        }
        if (targetFormat.getFrameRate() == AudioSystem.NOT_SPECIFIED && sourceFormat.getFrameRate() != AudioSystem.NOT_SPECIFIED) {
            bSetFrameRate = true;
        }
        if (bSetSampleSize || bSetChannels || bSetSampleRate || bSetFrameRate || (targetFormat.getFrameSize() == AudioSystem.NOT_SPECIFIED && sourceFormat.getFrameSize() != AudioSystem.NOT_SPECIFIED)) {
            float sampleRate = bSetSampleRate ? sourceFormat.getSampleRate() : targetFormat.getSampleRate();
            float frameRate = bSetFrameRate ? sourceFormat.getFrameRate() : targetFormat.getFrameRate();
            int sampleSize = bSetSampleSize ? sourceFormat.getSampleSizeInBits() : targetFormat.getSampleSizeInBits();
            int channels = bSetChannels ? sourceFormat.getChannels() : targetFormat.getChannels();
            int frameSize = getFrameSize(targetFormat.getEncoding(), sampleRate, sampleSize, channels, frameRate, targetFormat.isBigEndian(), targetFormat.getFrameSize());
            targetFormat = new AudioFormat(targetFormat.getEncoding(), sampleRate, sampleSize, channels, frameSize, frameRate, targetFormat.isBigEndian());
        }
        return targetFormat;
    }

    protected int getFrameSize(AudioFormat.Encoding encoding, float sampleRate, int sampleSize, int channels, float frameRate, boolean bigEndian, int oldFrameSize) {
        if (sampleSize == AudioSystem.NOT_SPECIFIED || channels == AudioSystem.NOT_SPECIFIED) {
            return AudioSystem.NOT_SPECIFIED;
        }
        return sampleSize * channels / 8;
    }
}
