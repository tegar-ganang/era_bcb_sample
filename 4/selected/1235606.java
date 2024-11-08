package com.google.code.b0rx0r.advancedSamplerEngine.source;

import org.tritonus.share.sampled.FloatSampleBuffer;
import com.google.code.b0rx0r.advancedSamplerEngine.AbstractEnqueueable;
import com.google.code.b0rx0r.advancedSamplerEngine.ChannelOutputMap;
import com.google.code.b0rx0r.advancedSamplerEngine.Enqueueable;

public class MemorySample extends AbstractEnqueueable {

    private FloatSampleBuffer fsb;

    public MemorySample(FloatSampleBuffer fsb, ChannelOutputMap outputMap) {
        super(outputMap);
        this.fsb = fsb;
    }

    @Override
    public int getChannelCount() {
        return fsb.getChannelCount();
    }

    @Override
    public float getAudioData(int channel, long offset) {
        if (offset < fsb.getSampleCount()) return fsb.getChannel(channel)[(int) offset];
        return Enqueueable.NO_MORE_AUDIO_DATA;
    }

    @Override
    public long getLength() {
        return fsb.getSampleCount();
    }

    @Override
    public void prepareData(long start, long length) {
    }
}
