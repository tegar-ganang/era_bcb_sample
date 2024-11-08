package org.tritonus.sampled.convert;

import java.util.HashSet;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.tritonus.share.sampled.convert.TFormatConversionProvider;

/**	"Smart" formatConversionProvider.
 *	This FormatConversionProvider tries to find combinations of other
 *	FormatConversionProviders so that the chain of these providers fulfill the request for a
 *	format conversion given to this provider.
 *
 * @author Matthias Pfisterer
 */
public class SmartFormatConversionProvider extends TFormatConversionProvider {

    /**	Stores the threads currently blocked.
	 *	To avoid recursion, this class stores which threads have already "passed"
	 *	methods of this class once. On entry of a method prone to recursion, it is
	 *	checked if the current thread is in the set. If so, this indicates a recursion
	 *	and the method will return immediately. If not, the current thread is entered
	 *	this data structure, so that further invocations can detect a recursion. On
	 *	exit of this method, it is removed from this data structure to indicate it is
	 *	"free".
	 */
    private Set<Thread> m_blockedThreads;

    public SmartFormatConversionProvider() {
        m_blockedThreads = new HashSet<Thread>();
    }

    public AudioFormat.Encoding[] getSourceEncodings() {
        return EMPTY_ENCODING_ARRAY;
    }

    public AudioFormat.Encoding[] getTargetEncodings() {
        return EMPTY_ENCODING_ARRAY;
    }

    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        return null;
    }

    public boolean isConversionSupported(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        return false;
    }

    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        return null;
    }

    public boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        if (isCurrentThreadBlocked()) {
            return false;
        }
        AudioFormat[] aIntermediateFormats = getIntermediateFormats(sourceFormat, targetFormat);
        return aIntermediateFormats != null;
    }

    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream audioInputStream) {
        return null;
    }

    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream) {
        return null;
    }

    private AudioFormat[] getIntermediateFormats(AudioFormat sourceFormat, AudioFormat targetFormat) {
        AudioFormat.Encoding sourceEncoding = sourceFormat.getEncoding();
        AudioFormat.Encoding targetEncoding = targetFormat.getEncoding();
        blockCurrentThread();
        boolean bDirectConversionPossible = AudioSystem.isConversionSupported(targetFormat, sourceFormat);
        unblockCurrentThread();
        if (bDirectConversionPossible) {
            return EMPTY_FORMAT_ARRAY;
        } else if (isPCM(sourceEncoding) && isPCM(targetEncoding)) {
            return null;
        } else if (!isPCM(sourceEncoding)) {
            AudioFormat intermediateFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), sourceFormat.getSampleSizeInBits(), sourceFormat.getChannels(), AudioSystem.NOT_SPECIFIED, sourceFormat.getSampleRate(), true);
            blockCurrentThread();
            AudioFormat[] aPreIntermediateFormats = getIntermediateFormats(sourceFormat, intermediateFormat);
            unblockCurrentThread();
            AudioFormat[] aPostIntermediateFormats = getIntermediateFormats(intermediateFormat, targetFormat);
            if (aPreIntermediateFormats != null && aPostIntermediateFormats != null) {
                AudioFormat[] aIntermediateFormats = new AudioFormat[aPreIntermediateFormats.length + 1 + aPostIntermediateFormats.length];
                System.arraycopy(aPreIntermediateFormats, 0, aIntermediateFormats, 0, aPreIntermediateFormats.length);
                aIntermediateFormats[aPreIntermediateFormats.length] = intermediateFormat;
                System.arraycopy(aPostIntermediateFormats, 0, aIntermediateFormats, aPreIntermediateFormats.length, aPostIntermediateFormats.length);
                return aIntermediateFormats;
            } else {
                return null;
            }
        } else if (!isPCM(targetEncoding)) {
            AudioFormat intermediateFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, targetFormat.getSampleRate(), targetFormat.getSampleSizeInBits(), targetFormat.getChannels(), AudioSystem.NOT_SPECIFIED, targetFormat.getSampleRate(), true);
            AudioFormat[] aPreIntermediateFormats = getIntermediateFormats(sourceFormat, intermediateFormat);
            blockCurrentThread();
            AudioFormat[] aPostIntermediateFormats = getIntermediateFormats(intermediateFormat, targetFormat);
            unblockCurrentThread();
            if (aPreIntermediateFormats != null && aPostIntermediateFormats != null) {
                AudioFormat[] aIntermediateFormats = new AudioFormat[aPreIntermediateFormats.length + 1 + aPostIntermediateFormats.length];
                System.arraycopy(aPreIntermediateFormats, 0, aIntermediateFormats, 0, aPreIntermediateFormats.length);
                aIntermediateFormats[aPreIntermediateFormats.length] = intermediateFormat;
                System.arraycopy(aPostIntermediateFormats, 0, aIntermediateFormats, aPreIntermediateFormats.length, aPostIntermediateFormats.length);
                return aIntermediateFormats;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static boolean isPCM(AudioFormat.Encoding encoding) {
        return encoding.equals(AudioFormat.Encoding.PCM_SIGNED) || encoding.equals(AudioFormat.Encoding.PCM_SIGNED);
    }

    protected static boolean isSignedPCM(AudioFormat.Encoding encoding) {
        return encoding.equals(AudioFormat.Encoding.PCM_SIGNED);
    }

    private boolean isCurrentThreadBlocked() {
        return m_blockedThreads.contains(Thread.currentThread());
    }

    private void blockCurrentThread() {
        m_blockedThreads.add(Thread.currentThread());
    }

    private void unblockCurrentThread() {
        m_blockedThreads.remove(Thread.currentThread());
    }
}
