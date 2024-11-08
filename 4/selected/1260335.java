package org.vous.facelib.filters;

import org.vous.facelib.bitmap.PixelUtils;

public class MaskOutFilter extends PointFilter {

    public enum Channel {

        R, G, B
    }

    private Channel mChannelOut;

    public MaskOutFilter(Channel channelOut) {
        mChannelOut = channelOut;
    }

    public Channel getChannel() {
        return mChannelOut;
    }

    public void setChannel(Channel channelOut) {
        mChannelOut = channelOut;
    }

    @Override
    protected int filter(int x, int y, int rgb) {
        int masked = 0;
        int a = PixelUtils.alpha(rgb);
        int r = PixelUtils.red(rgb);
        int g = PixelUtils.green(rgb);
        int b = PixelUtils.blue(rgb);
        if (mChannelOut == Channel.R) masked = PixelUtils.toARGB(a, 0, g, b); else if (mChannelOut == Channel.G) masked = PixelUtils.toARGB(a, r, 0, b); else masked = PixelUtils.toARGB(a, r, g, 0);
        return masked;
    }
}
