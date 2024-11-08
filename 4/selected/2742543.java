package com.cookeroo.media;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * @author Thomas Quintana
 */
public class SampleRateConverter {

    /**
	 * Converts an <code>AudioInputStream</code> sample rate in to the target sample rate.
	 * (The desired sample rate must be supported or an <code>IllegalArgumentException</code>
	 * will be thrown)
	 * @param sampleRate The target sample rate.
	 * @param inputStream The source <code>AudioInputStream</code>.
	 * @throws IllegalArgumentException If the conversion is not supported.
	 * @return An <code>AudioInputStream</code> with the target sample rate.
	 */
    public static AudioInputStream convert(float sampleRate, AudioInputStream inputStream) throws IllegalArgumentException {
        AudioFormat format = new AudioFormat(inputStream.getFormat().getEncoding(), sampleRate, inputStream.getFormat().getSampleSizeInBits(), inputStream.getFormat().getChannels(), inputStream.getFormat().getFrameSize(), sampleRate, inputStream.getFormat().isBigEndian());
        AudioInputStream result = AudioSystem.getAudioInputStream(format, inputStream);
        return result;
    }
}
