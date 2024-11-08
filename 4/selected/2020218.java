package org.lwjgl.opencl.api;

/**
 * Simple container for cl_image_format struct values.
 *
 * @author Spasi
 */
public final class CLImageFormat {

    /** The cl_image_format struct size in bytes. */
    public static final int STRUCT_SIZE = 2 * 4;

    private final int channelOrder;

    private final int channelType;

    public CLImageFormat(final int channelOrder, final int channelType) {
        this.channelOrder = channelOrder;
        this.channelType = channelType;
    }

    public int getChannelOrder() {
        return channelOrder;
    }

    public int getChannelType() {
        return channelType;
    }
}
