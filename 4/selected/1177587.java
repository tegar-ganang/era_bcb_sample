package com.google.code.b0rx0r.advancedSamplerEngine.effect;

import com.google.code.b0rx0r.advancedSamplerEngine.Enqueueable;

public class PlaybackSpeedEffect extends AbstractEffect {

    private InterpolationStrategy interpolation = new NearestNeighbourInterpolationStrategy();

    /** TODO: currently we always use modulation.getValue(0) to keep prepareData and getLength consistent with getAudioData. */
    public PlaybackSpeedEffect(Enqueueable wrapped) {
        super(wrapped);
    }

    @Override
    public float getAudioData(int channel, long offset) {
        float factor = modulation.getValue(0);
        return interpolation.getAudioData(0, offset * factor, wrapped);
    }

    @Override
    public long getLength() {
        if (wrapped.getLength() == Enqueueable.UNKNOWN) return Enqueueable.UNKNOWN;
        return (long) (wrapped.getLength() * modulation.getValue(0));
    }

    @Override
    public void prepareData(long start, long length) {
        float factor = modulation.getValue(0);
        wrapped.prepareData((long) (start / factor), (long) (length / factor));
    }

    @Override
    public int getChannelCount() {
        return wrapped.getChannelCount();
    }
}
