package avisync.model;

public class AVIAudioStreamFormat extends AVIStreamFormat {

    public static final int AVI_AUDIO_STREAM_FORMAT_CHUNK_ID = AVIStreamFormat.AVI_STREAM_FORMAT_CHUNK_ID;

    public static final int AVI_AUDIO_STREAM_FORMAT_CHUNK_LENGTH = 16;

    public static final int AVI_AUDIO_STREAM_FORMAT_EX_CHUNK_LENGTH = 18;

    public static final short AVI_AUDIO_STREAM_FORMAT_TAG_PCM = 0x0001;

    public static final short AVI_AUDIO_STREAM_FORMAT_TAG_MP3 = 0x0055;

    public static final short AVI_AUDIO_STREAM_FORMAT_TAG_AC3 = 0x2000;

    public static final short AVI_AUDIO_STREAM_FORMAT_EXTRA_SIZE_PCM = 0;

    public static final short AVI_AUDIO_STREAM_FORMAT_EXTRA_SIZE_MP3 = 12;

    public static final short AVI_AUDIO_STREAM_FORMAT_EXTRA_SIZE_AC3 = 0;

    private short formatTag;

    private short channels;

    private int samplesPerSecond;

    private int avgBytesPerSec;

    private short blockAlign;

    private short bitsPerSample;

    private short extraSize;

    public AVIAudioStreamFormat() {
    }

    public short getFormatTag() {
        return formatTag;
    }

    public short getChannels() {
        return channels;
    }

    public int getSamplesPerSecond() {
        return samplesPerSecond;
    }

    public int getAvgBytesPerSec() {
        return avgBytesPerSec;
    }

    public short getBlockAlign() {
        return blockAlign;
    }

    public short getBitsPerSample() {
        return bitsPerSample;
    }

    public short getExtraSize() {
        return extraSize;
    }

    public void setFormatTag(short formatTag) {
        this.formatTag = formatTag;
    }

    public void setChannels(short channels) {
        this.channels = channels;
    }

    public void setSamplesPerSecond(int samplesPerSecond) {
        this.samplesPerSecond = samplesPerSecond;
    }

    public void setAvgBytesPerSec(int avgBytesPerSec) {
        this.avgBytesPerSec = avgBytesPerSec;
    }

    public void setBlockAlign(short blockAlign) {
        this.blockAlign = blockAlign;
    }

    public void setBitsPerSample(short bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    public void setExtraSize(short extraSize) {
        this.extraSize = extraSize;
    }
}
