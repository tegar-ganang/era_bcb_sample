package net.sf.fmj.media;

import javax.media.*;
import javax.media.format.*;

/**
 * Creates audio formats which have any unspecified values set to defaults
 * 
 * @author Ken Larson
 * 
 */
public class AudioFormatCompleter {

    /** Fill in any needed, unspecified values, like endianness. */
    public static AudioFormat complete(AudioFormat f) {
        if (f.getSampleSizeInBits() > 8 && f.getEndian() == Format.NOT_SPECIFIED) {
            return new AudioFormat(f.getEncoding(), f.getSampleRate(), f.getSampleSizeInBits(), f.getChannels(), AudioFormat.BIG_ENDIAN, f.getSigned(), f.getFrameSizeInBits(), f.getFrameRate(), f.getDataType());
        }
        return f;
    }
}
