package dalsong.mp3info;

/**
 *  Create a MPEG Frame object that represent a Mpeg frame in a mp3 file !! Contains an exception that should be modified !! $Id: MPEGFrame.java,v 1.1 2008/07/17 09:40:10 chs Exp $
 *
 * @author     Rapha�l Slinckx (KiKiDonK)
 * @version    v0.03
 */
public class MPEGFrame {

    private byte[] mpegBytes;

    /**  The version of this MPEG frame (see the constants) */
    private int MPEGVersion;

    /**  Bitrate of this frame */
    private int bitrate;

    /**  Channel Mode of this Frame (see constants) */
    private int channelMode;

    /**  Emphasis mode string */
    private String emphasis;

    /**  Flag indicating if this frame has padding byte */
    private boolean hasPadding;

    /**  Flag indicating if this frame contains copyrighted material */
    private boolean isCopyrighted;

    /**  Flag indicating if this frame contains original material */
    private boolean isOriginal;

    /**  Flag indicating if this frame is protected */
    private boolean isProtected;

    /**  Flag indicating if this is a valid MPEG Frame */
    private boolean isValid;

    /**  Contains the mpeg layer of this frame (see constants) */
    private int layer;

    /**  Mode Extension of this frame */
    private String modeExtension;

    /**  Sampling rate of this frame in kbps */
    private int samplingRate;

    /**  Constant holding the Dual Channel Stereo Mode */
    public static final int CHANNEL_MODE_DUAL_CHANNEL = 2;

    /**  Constant holding the Joint Stereo Mode */
    public static final int CHANNEL_MODE_JOINT_STEREO = 1;

    /**  Constant holding the Mono Mode */
    public static final int CHANNEL_MODE_MONO = 3;

    /**  Constant holding the Stereo Mode */
    public static final int CHANNEL_MODE_STEREO = 0;

    /**  Constant holding the Layer 1 value Mpeg frame */
    public static final int LAYER_I = 3;

    /**  Constant holding the Layer 2 value Mpeg frame */
    public static final int LAYER_II = 2;

    /**  Constant holding the Layer 3 value Mpeg frame */
    public static final int LAYER_III = 1;

    /**  Constant holding the Reserved Layer value Mpeg frame */
    public static final int LAYER_RESERVED = 0;

    /**  Constant holding the mpeg frame version 1 */
    public static final int MPEG_VERSION_1 = 3;

    /**  Constant holding the mpeg frame version 2 */
    public static final int MPEG_VERSION_2 = 2;

    /**  Constant holding the mpeg frame version 2.5 */
    public static final int MPEG_VERSION_2_5 = 0;

    /**  Constant holding the reserved mpeg frame */
    public static final int MPEG_VERSION_RESERVED = 1;

    /**  Constant table holding the different Mpeg versions allowed */
    private static final int[] MPEGVersionTable = { MPEG_VERSION_2_5, MPEG_VERSION_RESERVED, MPEG_VERSION_2, MPEG_VERSION_1 };

    /**  Constant table holding the different Mpeg versions allowed in a string representation  */
    private static final String[] MPEGVersionTable_String = { "MPEG Version 2.5", "reserved", "MPEG Version 2 (ISO/IEC 13818-3)", "MPEG Version 1 (ISO/IEC 11172-3)" };

    /**  Constant 3ple table that holds the bitrate in kbps for the given layer, mode and value  */
    private static final int[][][] bitrateTable = { { { 0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, -1 }, { 0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, -1 }, { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, -1 } }, { { 0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, -1 }, { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1 }, { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, -1 } } };

    /**  Constant table holding the channel modes allowed in a string representation */
    private static final String[] channelModeTable_String = { "Stereo", "Joint stereo (Stereo)", "Dual channel (2 mono channels)", "Single channel (Mono)" };

    /**  Constant table holding the names of the emphasis modes in a string representation */
    private static final String[] emphasisTable = { "none", "50/15 ms", "reserved", "CCIT J.17" };

    /**  Constant table holding the Layer descriptions allowed */
    private static final int[] layerDescriptionTable = { LAYER_RESERVED, LAYER_III, LAYER_II, LAYER_I };

    /**  Constant table holding the Layer descriptions allowed in a string representation */
    private static final String[] layerDescriptionTable_String = { "reserved", "Layer III", "Layer II", "Layer I" };

    /**  Constant table holding the mode extensions for a given layer in a string representation  */
    private static final String[][] modeExtensionTable = { { "4-31", "8-31", "12-31", "16-31" }, { "off-off", "on-off", "off-on", "on-on" } };

    /**  Constant table holding the sampling rate in Hz for a given Mpeg version */
    private static final int[][] samplingRateTable = { { 44100, 48000, 32000, 0 }, { 22050, 24000, 16000, 0 }, { 11025, 12000, 8000, 0 } };

    private static final int[] SAMPLE_NUMBERS = { -1, 1152, 1152, 384 };

    /**
	 *  Creates a new MPEG frame with the given bytre array and decodes its contents
	 *
	 * @param  b  the array of bytes representing this mpeg frame
	 */
    public MPEGFrame(byte[] b) {
        this.mpegBytes = b;
        if (isMPEGFrame()) {
            MPEGVersion = MPEGVersion();
            layer = layerDescription();
            isProtected = isProtected();
            bitrate = bitrate();
            samplingRate = samplingRate();
            hasPadding = hasPadding();
            channelMode = channelMode();
            modeExtension = modeExtension();
            isCopyrighted = isCopyrighted();
            isOriginal = isOriginal();
            emphasis = emphasis();
            isValid = true;
        } else isValid = false;
        this.mpegBytes = null;
    }

    /**
	 *  Gets the bitrate attribute of the MPEGFrame object
	 *
	 * @return    The bitrate value
	 */
    public int getBitrate() {
        return bitrate;
    }

    /**
	 *  Gets the channelMode attribute of the MPEGFrame object
	 *
	 * @return    The channelMode value
	 */
    public int getChannelNumber() {
        switch(channelMode) {
            case CHANNEL_MODE_DUAL_CHANNEL:
                return 2;
            case CHANNEL_MODE_JOINT_STEREO:
                return 2;
            case CHANNEL_MODE_MONO:
                return 1;
            case CHANNEL_MODE_STEREO:
                return 2;
        }
        return 0;
    }

    public int getChannelMode() {
        return channelMode;
    }

    /**
	 *  Gets the layerVersion attribute of the MPEGFrame object
	 *
	 * @return    The layerVersion value
	 */
    public int getLayerVersion() {
        return layer;
    }

    /**
	 *  Gets the mPEGVersion attribute of the MPEGFrame object
	 *
	 * @return    The mPEGVersion value
	 */
    public int getMPEGVersion() {
        return MPEGVersion;
    }

    /**
	 *  Gets the paddingLength attribute of the MPEGFrame object
	 *
	 * @return    The paddingLength value
	 */
    public int getPaddingLength() {
        if (hasPadding && layer != LAYER_I) return 1;
        if (hasPadding && layer == LAYER_I) return 4;
        return 0;
    }

    /**
	 *  Gets the samplingRate attribute of the MPEGFrame object
	 *
	 * @return    The samplingRate value
	 */
    public int getSamplingRate() {
        return samplingRate;
    }

    /**
	 *  Verify if this frame is a valid one
	 *
	 * @return    The isValid value
	 */
    public boolean isValid() {
        return isValid;
    }

    public int getFrameLength() {
        if (layer == LAYER_I) {
            return (12 * (getBitrate() * 1000) / getSamplingRate() + getPaddingLength()) * 4;
        }
        return 144 * (getBitrate() * 1000) / getSamplingRate() + getPaddingLength();
    }

    public int getSampleNumber() {
        int sn = SAMPLE_NUMBERS[layer];
        return sn;
    }

    /**
	 *  The Mpeg version of this frame in a string representation
	 *
	 * @param  i  the int constant of the version
	 * @return    the string representation of the version
	 */
    public String MPEGVersionToString(int i) {
        return MPEGVersionTable_String[i];
    }

    /**
	 *  get a string representation of the channel mode of this frame
	 *
	 * @param  i  the constant holding the channel mode
	 * @return    the string representation of this mode
	 */
    public String channelModeToString(int i) {
        return channelModeTable_String[i];
    }

    /**
	 *  Get the string representation of the layer version given the constant representing it
	 *
	 * @param  i  the constant holding the layer information
	 * @return    the string representation of this layer version
	 */
    public String layerToString(int i) {
        return layerDescriptionTable_String[i];
    }

    /**
	 *  Creates a string representation of this mpeg frame
	 *
	 * @return    the string representing this frame
	 */
    public String toString() {
        String output = "\n----MPEGFrame--------------------\n";
        output += "MPEG Version: " + MPEGVersionToString(MPEGVersion) + "\tLayer: " + layerToString(layer) + "\n";
        output += "Bitrate: " + bitrate + "\tSamp.Freq.: " + samplingRate + "\tChan.Mode: " + channelModeToString(channelMode) + "\n";
        output += "Mode Extension: " + modeExtension + "\tEmphasis: " + emphasis + "\n";
        output += "Padding? " + hasPadding + "\tProtected? " + isProtected + "\tCopyright? " + isCopyrighted + "\tOriginal? " + isOriginal + "\n";
        output += "--------------------------------";
        return output;
    }

    /**
	 *  Gets the copyrighted attribute of the MPEGFrame object
	 *
	 * @return    The copyrighted value
	 */
    private boolean isCopyrighted() {
        return ((mpegBytes[3] & 0x08) == 0x08);
    }

    /**
	 *  Gets the mPEGFrame attribute of the MPEGFrame object
	 *
	 * @return    The mPEGFrame value
	 */
    private boolean isMPEGFrame() {
        return ((mpegBytes[0] & 0xFF) == 0xFF) && ((mpegBytes[1] & 0xE0) == 0xE0);
    }

    /**
	 *  Gets the original attribute of the MPEGFrame object
	 *
	 * @return    The original value
	 */
    private boolean isOriginal() {
        return (mpegBytes[3] & 0x04) == 0x04;
    }

    /**
	 *  Gets the protected attribute of the MPEGFrame object
	 *
	 * @return    The protected value
	 */
    private boolean isProtected() {
        return (mpegBytes[1] & 0x01) == 0x00;
    }

    /**
	 *  get the Mpeg version of this frame as an int value (see constants)
	 *
	 * @return    the int value describing the Mpeg version
	 */
    private int MPEGVersion() {
        int index = ((mpegBytes[1] & 0x18) >>> 3);
        return MPEGVersionTable[index];
    }

    /**
	 *  get the bitrate of this frame
	 *
	 * @return    the bitrate in kbps
	 */
    private int bitrate() {
        int index3 = ((mpegBytes[2] & 0xF0) >>> 4);
        int index1 = (MPEGVersion == MPEG_VERSION_1) ? 0 : 1;
        int index2;
        if (layer == LAYER_I) index2 = 0; else if (layer == LAYER_II) index2 = 1; else index2 = 2;
        return bitrateTable[index1][index2][index3];
    }

    /**
	 *  get the Mpeg channel mode of this frame as a constant (see constants)
	 *
	 * @return    the constant holding the channel mode
	 */
    private int channelMode() {
        int index = ((mpegBytes[3] & 0xC0) >>> 6);
        return index;
    }

    /**
	 *  Get the emphasis mode of this frame in a string representation
	 *
	 * @return    the emphasis mode
	 */
    private String emphasis() {
        int index = (mpegBytes[3] & 0x03);
        return emphasisTable[index];
    }

    /**
	 *  Check wether this frame uses padding bytes
	 *
	 * @return    a boolean indicating if this frame uses padding
	 */
    private boolean hasPadding() {
        return (mpegBytes[2] & 0x02) == 0x02;
    }

    /**
	 *  Get the layer version of this frame as a constant int value (see constants)
	 *
	 * @return    the layer version constant
	 */
    private int layerDescription() {
        int index = ((mpegBytes[1] & 0x06) >>> 1);
        return layerDescriptionTable[index];
    }

    /**
	 *  Gets the string representation of the mode extension of this frame
	 *
	 * @return    mode extension of this frame
	 */
    private String modeExtension() {
        int index2 = ((mpegBytes[3] & 0x30) >>> 4);
        int index1 = (layer == LAYER_III) ? 1 : 0;
        return modeExtensionTable[index1][index2];
    }

    /**
	 *  get the sampling rate in Hz of this frame
	 *
	 * @return    the sampling rate in Hz of this frame
	 */
    private int samplingRate() {
        int index2 = ((mpegBytes[2] & 0x0c) >>> 2);
        int index1;
        if (MPEGVersion == MPEG_VERSION_1) index1 = 0; else if (MPEGVersion == MPEG_VERSION_2) index1 = 1; else index1 = 2;
        return samplingRateTable[index1][index2];
    }
}
