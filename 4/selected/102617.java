package com.frinika.tootX.audio;

import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;

/**
 * A simple AudioProcess that looks for the peak value.
 * It is reset when yoiu call getPeak()
 * @author pjl
 *
 */
public class AudioPeakMonitor implements AudioProcess {

    float monitVal = 0.0f;

    public float getPeak() {
        float t = monitVal;
        monitVal = 0.0f;
        return t;
    }

    public void open() {
    }

    public int processAudio(AudioBuffer buffer) {
        int n = buffer.getSampleCount();
        int nch = buffer.getChannelCount();
        for (int ch = 0; ch < nch; ch++) {
            float[] buff = buffer.getChannel(ch);
            for (int i = 0; i < n; i++) {
                if (Math.abs(buff[i]) > monitVal) monitVal = Math.abs(buff[i]);
            }
        }
        return AUDIO_OK;
    }

    public void close() {
    }
}
