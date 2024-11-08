package com.frinika.audio.analysis;

import com.frinika.sequencer.model.audio.DoubleDataSource;

public class SInDoubleSource implements DoubleDataSource {

    long pos;

    private double fact;

    public SInDoubleSource(double freq, double fs) {
        this.fact = 2 * Math.PI * freq / fs;
    }

    public int getChannels() {
        return 1;
    }

    public void readNextDouble(double[] buffer, int offSet, int nFrame) {
        for (int i = 0, j = offSet; i < nFrame; j++, i++) {
            buffer[j] = Math.sin(fact * pos++);
        }
    }

    public void seekFrame(long pos) {
        this.pos = pos;
    }

    public boolean endOfFile() {
        return false;
    }

    public long getCurrentFrame() {
        return 0;
    }

    public long getLengthInFrames() {
        return 0;
    }
}
