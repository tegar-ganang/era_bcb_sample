package com.google.code.b0rx0r.advancedSamplerEngine.effect;

import com.google.code.b0rx0r.advancedSamplerEngine.Enqueueable;

public class SliceEffect extends AbstractEffect {

    private int startOffset;

    private int length;

    public SliceEffect(Enqueueable wrapped, int startOffset, int length) {
        super(wrapped);
        this.startOffset = startOffset;
        this.length = length;
    }

    @Override
    public float getAudioData(int channel, long offset) {
        if (offset >= length) return Enqueueable.NO_MORE_AUDIO_DATA;
        return wrapped.getAudioData(channel, offset + startOffset);
    }

    @Override
    public int getChannelCount() {
        return wrapped.getChannelCount();
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public void prepareData(long start, long length) {
        wrapped.prepareData(start + startOffset, Math.min(length, this.length - start));
    }
}
