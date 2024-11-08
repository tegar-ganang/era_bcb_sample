package com.google.code.b0rx0r.advancedSamplerEngine.effect;

import com.google.code.b0rx0r.advancedSamplerEngine.Enqueueable;

public class AmplitudeEffect extends AbstractEffect {

    public AmplitudeEffect(Enqueueable wrapped) {
        super(wrapped);
    }

    @Override
    public float getAudioData(int channel, long offset) {
        float data = wrapped.getAudioData(channel, offset);
        if (data == NO_MORE_AUDIO_DATA) return NO_MORE_AUDIO_DATA;
        return data * modulation.getValue(offset);
    }

    @Override
    public int getChannelCount() {
        return wrapped.getChannelCount();
    }

    @Override
    public long getLength() {
        return wrapped.getLength();
    }

    @Override
    public void prepareData(long start, long length) {
        wrapped.prepareData(start, length);
    }
}
