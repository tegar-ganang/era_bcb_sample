package com.google.code.b0rx0r.advancedSamplerEngine.effect;

import com.google.code.b0rx0r.advancedSamplerEngine.Enqueueable;

public class LoopEffect extends AbstractEffect {

    long length;

    public LoopEffect(Enqueueable wrapped) {
        super(wrapped);
        this.length = wrapped.getLength();
    }

    @Override
    public float getAudioData(int channel, long offset) {
        if (length == UNKNOWN) {
            float data = wrapped.getAudioData(channel, offset);
            if (data == NO_MORE_AUDIO_DATA) {
                length = offset;
            } else {
                return data;
            }
        }
        return wrapped.getAudioData(channel, offset % length);
    }

    @Override
    public int getChannelCount() {
        return wrapped.getChannelCount();
    }

    @Override
    public long getLength() {
        return UNKNOWN;
    }

    @Override
    public void prepareData(long start, long length) {
        long assumedLength = this.length;
        if (assumedLength == UNKNOWN) assumedLength = start + length;
        wrapped.prepareData(start % assumedLength, length);
    }
}
