package org.skycastle.texture;

import org.skycastle.util.ParameterChecker;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public final class FieldImpl implements Field {

    private final Map<String, Channel> myChannels = new HashMap<String, Channel>(7);

    private int mySizeX = 0;

    private int mySizeY = 0;

    public FieldImpl(final int sizeX, final int sizeY) {
        resize(sizeX, sizeY);
    }

    public int getXSize() {
        return mySizeX;
    }

    public int getYSize() {
        return mySizeY;
    }

    public void resize(final int xSize, final int ySize) {
        ParameterChecker.checkPositiveNonZeroInteger(xSize, "xSize");
        ParameterChecker.checkPositiveNonZeroInteger(ySize, "ySize");
        if (mySizeX != xSize || mySizeY != ySize) {
            mySizeX = xSize;
            mySizeY = ySize;
            for (Channel channel : myChannels.values()) {
                channel.resize(xSize, ySize);
            }
        }
    }

    public Collection<Channel> getChannels() {
        return Collections.unmodifiableCollection(myChannels.values());
    }

    public Channel getChannel(String channelName) {
        return myChannels.get(channelName);
    }

    public FloatBuffer getDataBuffer(String channelName) {
        final Channel channel = myChannels.get(channelName);
        if (channel != null) {
            return channel.getDataBuffer();
        } else {
            return null;
        }
    }

    public void addChannel(Channel addedChannel) {
        ParameterChecker.checkNotNull(addedChannel, "addedChannel");
        ParameterChecker.checkNotAlreadyContained(addedChannel.getName(), myChannels, "myChannels");
        if (addedChannel.getDataBuffer().capacity() != mySizeX * mySizeY) {
            throw new IllegalArgumentException("The added channel '" + addedChannel + "' is not the same size as the field '" + this + "' .");
        }
        myChannels.put(addedChannel.getName(), addedChannel);
    }

    public void addChannel(final String newChannelName) {
        addChannel(newChannelName, 0);
    }

    public void addChannel(final String newChannelName, final float defaultValue) {
        ParameterChecker.checkNotAlreadyContained(newChannelName, myChannels, "myChannels");
        myChannels.put(newChannelName, new ChannelImpl(newChannelName, mySizeX, mySizeY, defaultValue));
    }

    public void removeChannel(Channel removedChannel) {
        ParameterChecker.checkNotNull(removedChannel, "removedChannel");
        ParameterChecker.checkContained(removedChannel, myChannels.values(), "myChannels");
        myChannels.remove(removedChannel.getName());
    }

    public String toString() {
        return "FieldImpl{" + "mySizeX=" + mySizeX + ", mySizeY=" + mySizeY + ", myChannels=" + myChannels + '}';
    }
}
