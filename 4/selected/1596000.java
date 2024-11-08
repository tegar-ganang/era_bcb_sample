package net.sourceforge.jaad.adts;

import java.io.DataInputStream;
import java.io.IOException;
import net.sourceforge.jaad.aac.ChannelConfiguration;
import net.sourceforge.jaad.aac.SampleFrequency;

class ADTSFrame {

    private boolean id, protectionAbsent, privateBit, copy, home;

    private int layer, profile, sampleFrequency, channelConfiguration;

    private boolean copyrightIDBit, copyrightIDStart;

    private int frameLength, adtsBufferFullness, rawDataBlockCount;

    private int[] rawDataBlockPosition;

    private int crcCheck;

    private byte[] info;

    ADTSFrame(DataInputStream in) throws IOException {
        readHeader(in);
        if (!protectionAbsent) crcCheck = in.readUnsignedShort();
        if (rawDataBlockCount == 0) {
        } else {
            int i;
            if (!protectionAbsent) {
                rawDataBlockPosition = new int[rawDataBlockCount];
                for (i = 0; i < rawDataBlockCount; i++) {
                    rawDataBlockPosition[i] = in.readUnsignedShort();
                }
                crcCheck = in.readUnsignedShort();
            }
            for (i = 0; i < rawDataBlockCount; i++) {
                if (!protectionAbsent) crcCheck = in.readUnsignedShort();
            }
        }
    }

    private void readHeader(DataInputStream in) throws IOException {
        int i = in.read();
        id = ((i >> 3) & 0x1) == 1;
        layer = (i >> 1) & 0x3;
        protectionAbsent = (i & 0x1) == 1;
        i = in.read();
        profile = ((i >> 6) & 0x3) + 1;
        sampleFrequency = (i >> 2) & 0xF;
        privateBit = ((i >> 1) & 0x1) == 1;
        i = (i << 8) | in.read();
        channelConfiguration = ((i >> 6) & 0x7);
        copy = ((i >> 5) & 0x1) == 1;
        home = ((i >> 4) & 0x1) == 1;
        copyrightIDBit = ((i >> 3) & 0x1) == 1;
        copyrightIDStart = ((i >> 2) & 0x1) == 1;
        i = (i << 16) | in.readUnsignedShort();
        frameLength = (i >> 5) & 0x1FFF;
        i = (i << 8) | in.read();
        adtsBufferFullness = (i >> 2) & 0x7FF;
        rawDataBlockCount = i & 0x3;
    }

    int getFrameLength() {
        return frameLength - (protectionAbsent ? 7 : 9);
    }

    byte[] createDecoderSpecificInfo() {
        if (info == null) {
            info = new byte[2];
            info[0] = (byte) (profile << 3);
            info[0] |= (sampleFrequency >> 1) & 0x7;
            info[1] = (byte) ((sampleFrequency & 0x1) << 7);
            info[1] |= (channelConfiguration << 3);
        }
        return info;
    }

    int getSampleFrequency() {
        return SampleFrequency.forInt(sampleFrequency).getFrequency();
    }

    int getChannelCount() {
        return ChannelConfiguration.forInt(channelConfiguration).getChannelCount();
    }
}
