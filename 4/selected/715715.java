package com.frinika.tootX.plugins;

import java.util.Observable;
import java.util.Observer;
import rasmus.interpreter.sampled.util.Freeverb;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioControls;
import uk.org.toot.audio.core.AudioProcess;
import com.frinika.global.FrinikaConfig;

public class ReverbProcess implements AudioProcess {

    private ReverbProcessVariables vars;

    Freeverb freeverb = null;

    double reverbBufferIn[] = null;

    double reverbBufferOut[] = null;

    public ReverbProcess(ReverbProcessVariables variables) {
        vars = variables;
        ((AudioControls) vars).addObserver(new Observer() {

            public void update(Observable arg0, Object arg1) {
                ReverbProcess.this.update();
            }
        });
    }

    public void update() {
        float wet = vars.getMix();
        float dry = 1.0f - wet;
        float level = vars.getLevel();
        freeverb.setwet(wet * level);
        freeverb.setdry(dry * level);
        freeverb.setroomsize(vars.getRoomSize());
        freeverb.setwidth(vars.getWidth());
        freeverb.setdamp(vars.getDamp());
    }

    public void open() {
        freeverb = new Freeverb(FrinikaConfig.sampleRate, 1.0);
        update();
    }

    float mix1 = -1;

    public int processAudio(AudioBuffer buffer) {
        if (((AudioControls) vars).isBypassed()) return AUDIO_OK;
        int n = buffer.getSampleCount();
        if (freeverb != null && (reverbBufferIn == null || reverbBufferIn.length != n)) {
            reverbBufferIn = new double[2 * n];
            reverbBufferOut = new double[2 * n];
        }
        int nCh = buffer.getChannelCount();
        float inL[] = buffer.getChannel(0);
        if (nCh == 1) {
            for (int i = 0; i < n; i++) {
                reverbBufferIn[2 * n] = inL[n];
                reverbBufferIn[2 * n + 1] = inL[n];
            }
        } else if (nCh == 2) {
            float inR[] = buffer.getChannel(1);
            for (int i = 0; i < n; i++) {
                reverbBufferIn[2 * i] = inL[i];
                reverbBufferIn[2 * i + 1] = inR[i];
            }
        }
        freeverb.processReplace(reverbBufferIn, reverbBufferOut, 0, 2 * n, 2);
        if (buffer.getChannelCount() == 1) buffer.addChannel(false);
        inL = buffer.getChannel(0);
        float inR[] = buffer.getChannel(1);
        for (int i = 0; i < n; i++) {
            inL[i] = (float) reverbBufferOut[2 * i];
            inR[i] = (float) reverbBufferOut[2 * i + 1];
        }
        return AUDIO_OK;
    }

    public void close() {
    }
}
