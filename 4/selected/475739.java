package avisync.decoder;

public class MP3Frame {

    public static final int MPEG_FRAME_HEADER_SIZE = 4;

    public static final int MPEG_SYNC_MARKER = 0xFFF;

    public static final int MPEG_VERSION_2 = 0;

    public static final int MPEG_VERSION_1 = 1;

    public static final int MPEG_LAYER_III = 1;

    public static final int MPEG_LAYER_II = 2;

    public static final int MPEG_LAYER_I = 3;

    public static final int MPEG_MODE_STEREO = 0;

    public static final int MPEG_MODE_JOINT_STEREO = 1;

    public static final int MPEG_MODE_DUAL_CHANNEL = 2;

    public static final int MPEG_MODE_MONO = 3;

    public static final int MPEG_MIN_FRAME_SIZE = 24;

    public static final int MPEG_MAX_FRAME_SIZE = 1440;

    public static final int MPEG_MIN_BIT_RATE = 8;

    public static final int MPEG_MAX_BIT_RATE = 320;

    private static final int BIT_RATE[][] = { { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1 }, { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, -1 } };

    private static final int SAMPLE_RATE[][] = { { 22050, 24000, 16000, -1 }, { 44100, 48000, 32000, -1 } };

    private int header;

    public MP3Frame(int header) {
        this.header = header;
    }

    public int getMarker() {
        return (header >> 20) & 0xFFF;
    }

    public int getVersion() {
        return (header >> 19) & 0x01;
    }

    public int getLayer() {
        return (header >> 17) & 0x03;
    }

    public boolean getErrorProtection() {
        return ((header >> 16) & 0x01) == 0x00;
    }

    public int getBitRateIndex() {
        return (header >> 12) & 0x0F;
    }

    public int getSampleRateIndex() {
        return (header >> 10) & 0x03;
    }

    public int getPadding() {
        return (header >> 9) & 0x01;
    }

    public int getPrivate() {
        return (header >> 8) & 0x01;
    }

    public int getMode() {
        return (header >> 6) & 0x03;
    }

    public int getModeEx() {
        return (header >> 4) & 0x03;
    }

    public int getCopyright() {
        return (header >> 3) & 0x01;
    }

    public int getOriginal() {
        return (header >> 2) & 0x01;
    }

    public int getEmphasis() {
        return (header >> 0) & 0x03;
    }

    public int getBitRate() {
        return BIT_RATE[getVersion()][getBitRateIndex()];
    }

    public int getSampleRate() {
        return SAMPLE_RATE[getVersion()][getSampleRateIndex()];
    }

    public int getFrameLength() {
        return ((getVersion() + 1) * 72000 * getBitRate()) / getSampleRate() + getPadding();
    }

    public int getChannels() {
        if (getMode() != MPEG_MODE_MONO) return 2;
        return 1;
    }

    public boolean verify() {
        return getMarker() == MPEG_SYNC_MARKER && getLayer() == MPEG_LAYER_III && getBitRate() >= 0 && getSampleRate() >= 0;
    }

    public static int getSamplesPerFrame(int sampleRate) {
        return (sampleRate <= 24000 ? 576 : 1152);
    }

    public static int getBytesPerFrame(int bitRate, int sampleRate) {
        return (sampleRate >= 32000 ? 2 : 1) * 72000 * bitRate / sampleRate;
    }
}
