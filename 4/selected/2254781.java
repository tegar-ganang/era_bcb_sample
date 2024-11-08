package uk.org.toot.audio.meter;

import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.SimpleAudioProcess;

/**
 * A partial K-System meter process, for proper K-System compatibility 0dBr
 * should equal 83dBC, you should adjust your monitoring level to achieve this.
 * http://www.digido.com/portal/pmodule_id=11/pmdmode=fullscreen/pageadder_page_id=59
 * Deficiencies:
 * high peak is always infinite hold, last 10s unimplemented.
 * no pink noise source.
 */
public class MeterProcess extends SimpleAudioProcess {

    /**
     * @supplierCardinality 1
     * @link aggregation 
     */
    private MeterControls controls;

    private float bufferTime = -1;

    public MeterProcess(MeterControls controls) {
        this.controls = controls;
    }

    public int processAudio(AudioBuffer buffer) {
        int nc = buffer.getChannelCount();
        int ns = buffer.getSampleCount();
        float[] array;
        check(buffer);
        for (int c = 0; c < nc; c++) {
            array = buffer.getChannel(c);
            detectOvers(c, array, ns);
            detectPeak(c, array, ns);
            detectAverage(c, array, ns);
        }
        return AUDIO_OK;
    }

    private void check(AudioBuffer buffer) {
        float ms = buffer.getSampleCount() / buffer.getSampleRate() * 1000;
        if (bufferTime != ms) {
            bufferTime = ms;
            controls.setUpdateTime(ms);
        }
    }

    protected void detectOvers(int chan, float[] samples, int len) {
        int overs = 0;
        float sample;
        for (int i = 0; i < len; i++) {
            sample = samples[i];
            if (sample > 1) overs++; else if (sample < -1) overs++;
        }
        if (overs > 0) {
            controls.addOvers(chan, overs);
        }
    }

    protected void detectPeak(int chan, float[] samples, int len) {
        float peak = 0;
        float sample;
        for (int i = 0; i < len; i++) {
            sample = samples[i];
            if (sample > peak) {
                peak = sample;
            } else if (-sample > peak) {
                peak = -sample;
            }
        }
        controls.setPeak(chan, peak);
    }

    protected void detectAverage(int chan, float[] samples, int len) {
        float sumOfSquares = 0f;
        float sample;
        for (int i = 0; i < len; i++) {
            sample = samples[i];
            sumOfSquares += (sample * sample);
        }
        float rms = (float) (1.41 * Math.sqrt(sumOfSquares / len));
        controls.setAverage(chan, rms);
    }
}
