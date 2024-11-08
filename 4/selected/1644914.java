package se.slackers.locality.media.reader.mp3;

public class Mp3FrameHeader {

    private byte header[] = new byte[4];

    private long offset;

    private static final int sampleRateTable[][] = { { 0, 44100, 22050, 11025 }, { 0, 48000, 24000, 12000 }, { 0, 32000, 16000, 8000 }, { 0, 0, 0, 0 } };

    private static final int bitRateTable[][] = { { 0, 0, 0, 0, 0 }, { 32, 32, 32, 32, 8 }, { 64, 48, 40, 48, 16 }, { 96, 56, 48, 56, 24 }, { 128, 64, 56, 64, 32 }, { 160, 80, 64, 80, 40 }, { 192, 96, 80, 96, 48 }, { 224, 112, 96, 112, 56 }, { 256, 128, 112, 128, 64 }, { 288, 160, 128, 144, 80 }, { 320, 192, 160, 160, 96 }, { 352, 224, 192, 176, 112 }, { 384, 256, 224, 192, 128 }, { 416, 320, 256, 224, 144 }, { 448, 384, 320, 256, 160 }, { 0, 0, 0, 0, 0 } };

    public Mp3FrameHeader() {
    }

    public void setData(byte[] data) {
        for (int i = 0; i < 4; i++) {
            header[i] = data[i];
        }
    }

    public void setData(byte b1, byte b2, byte b3, byte b4) {
        header[0] = b1;
        header[1] = b2;
        header[2] = b3;
        header[3] = b4;
    }

    public byte[] getData() {
        return header;
    }

    public int getMPEGVersion() {
        return 4 - ((header[1] & 0x18) >> 3);
    }

    public int getLayerDescription() {
        return 4 - ((header[1] & 0x06) >> 1);
    }

    public boolean isCRCProtected() {
        return (header[1] & 0x01) == 1;
    }

    public int getBitRate() {
        int index = (header[2] & 0xf0) >> 4;
        int v = getMPEGVersion();
        int l = getLayerDescription();
        int index2 = Math.min((v - 1) * 3 + (l - 1), 4);
        return bitRateTable[index][index2] * 1000;
    }

    public boolean isPadded() {
        return (header[2] & 0x2) == 2;
    }

    public int getSampleRate() {
        int index = (header[2] & 0x0e) >> 2;
        if (index < 0 || index > 3) {
            return 0;
        }
        int version = getMPEGVersion();
        return sampleRateTable[index][version];
    }

    public int getChannelMode() {
        return ((header[3] & 0xC0) >> 6);
    }

    public int getModeExtension() {
        return ((header[3] & 0x30) >> 4);
    }

    public boolean isCopyrighted() {
        return (header[3] & 0x08) != 0;
    }

    public boolean isOriginal() {
        return (header[3] & 0x04) != 0;
    }

    public int getEmphasis() {
        return (header[3] & 0x03);
    }

    public int getFrameSize() {
        int bitrate = getBitRate();
        int samplerate = getSampleRate();
        int padding = isPadded() ? 1 : 0;
        return 144 * bitrate / (samplerate + padding);
    }

    public String toString() {
        return "Mp3Header[" + offset + "] {" + "\n MPEG Version: " + getMPEGVersion() + "\n Layer description: " + getLayerDescription() + "\n Is CRC protected: " + isCRCProtected() + "\n Bitrate: " + getBitRate() + "\n Samplerate: " + getSampleRate() + "\n Is Padded: " + isPadded() + "\n Channel mode: " + getChannelMode() + "\n Mode Extension: " + getModeExtension() + "\n Copyright: " + isCopyrighted() + "\n Is original: " + isOriginal() + "\n Emphasis: " + getEmphasis() + "\n Frame size: " + getFrameSize() + "\n}";
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}
