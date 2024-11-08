package entagged.audioformats.ape.util;

import entagged.audioformats.generic.Utils;

public class MonkeyHeader {

    byte[] b;

    public MonkeyHeader(byte[] b) {
        this.b = b;
    }

    public int getCompressionLevel() {
        return Utils.getNumber(b, 0, 1);
    }

    public int getFormatFlags() {
        return Utils.getNumber(b, 2, 3);
    }

    public long getBlocksPerFrame() {
        return Utils.getLongNumber(b, 4, 7);
    }

    public long getFinalFrameBlocks() {
        return Utils.getLongNumber(b, 8, 11);
    }

    public long getTotalFrames() {
        return Utils.getLongNumber(b, 12, 15);
    }

    public int getLength() {
        return (int) (getBlocksPerFrame() * (getTotalFrames() - 1.0) + getFinalFrameBlocks()) / getSamplingRate();
    }

    public float getPreciseLength() {
        return (float) ((double) (getBlocksPerFrame() * (getTotalFrames() - 1) + getFinalFrameBlocks()) / (double) getSamplingRate());
    }

    public int getBitsPerSample() {
        return Utils.getNumber(b, 16, 17);
    }

    public int getChannelNumber() {
        return Utils.getNumber(b, 18, 19);
    }

    public int getSamplingRate() {
        return Utils.getNumber(b, 20, 23);
    }
}
