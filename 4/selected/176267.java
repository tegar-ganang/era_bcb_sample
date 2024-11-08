package org.tritonus.share.sampled;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

public class AudioFormats {

    private static boolean doMatch(int i1, int i2) {
        return i1 == AudioSystem.NOT_SPECIFIED || i2 == AudioSystem.NOT_SPECIFIED || i1 == i2;
    }

    private static boolean doMatch(float f1, float f2) {
        return f1 == AudioSystem.NOT_SPECIFIED || f2 == AudioSystem.NOT_SPECIFIED || Math.abs(f1 - f2) < 1.0e-9;
    }

    public static boolean matches(AudioFormat format1, AudioFormat format2) {
        return Encodings.equals(format1.getEncoding(), format2.getEncoding()) && (format2.getSampleSizeInBits() <= 8 || format1.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED || format2.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED || format1.isBigEndian() == format2.isBigEndian()) && doMatch(format1.getChannels(), format2.getChannels()) && doMatch(format1.getSampleSizeInBits(), format2.getSampleSizeInBits()) && doMatch(format1.getFrameSize(), format2.getFrameSize()) && doMatch(format1.getSampleRate(), format2.getSampleRate()) && doMatch(format1.getFrameRate(), format2.getFrameRate());
    }

    /**
	 * Tests for exact equality of 2 AudioFormats.
	 * This is the behaviour of AudioFormat.matches in JavaSound 1.0.
	 * <p>
	 * This is a proposition to be used as AudioFormat.equals.
	 * It can therefore be considered as a temporary workaround.
	 */
    public static boolean equals(AudioFormat format1, AudioFormat format2) {
        return Encodings.equals(format1.getEncoding(), format2.getEncoding()) && format1.getChannels() == format2.getChannels() && format1.getSampleSizeInBits() == format2.getSampleSizeInBits() && format1.getFrameSize() == format2.getFrameSize() && (Math.abs(format1.getSampleRate() - format2.getSampleRate()) < 1.0e-9) && (Math.abs(format1.getFrameRate() - format2.getFrameRate()) < 1.0e-9);
    }
}
