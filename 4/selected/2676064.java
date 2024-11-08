package org.gamio.conf;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 19 $ $Date: 2008-09-26 19:00:58 -0400 (Fri, 26 Sep 2008) $
 */
public final class ChannelManagerProps {

    private int channelByteBufferSize = 512;

    private int srvChannelWriteQueueCacheSize = 16;

    private int srvChannelCacheSize = 2048;

    private int srvChannelHouseInitCapacity = 4096;

    private int srvChannelHouseListCacheSize = 2048;

    private int cltChannelWriteQueueCacheSize = 1;

    private int cltChannelCacheSize = 256;

    private int cltChannelHouseInitCapacity = 512;

    private int cltChannelHouseListCacheSize = 256;

    public int getChannelByteBufferSize() {
        return channelByteBufferSize;
    }

    public void setChannelByteBufferSize(int channelByteBufferSize) {
        this.channelByteBufferSize = channelByteBufferSize;
    }

    public int getCltChannelWriteQueueCacheSize() {
        return cltChannelWriteQueueCacheSize;
    }

    public void setCltChannelWriteQueueCacheSize(int cltChannelWriteQueueCacheSize) {
        this.cltChannelWriteQueueCacheSize = cltChannelWriteQueueCacheSize;
    }

    public int getCltChannelCacheSize() {
        return cltChannelCacheSize;
    }

    public void setCltChannelCacheSize(int cltChannelCacheSize) {
        this.cltChannelCacheSize = cltChannelCacheSize;
    }

    public int getCltChannelHouseInitCapacity() {
        return cltChannelHouseInitCapacity;
    }

    public void setCltChannelHouseInitCapacity(int cltChannelHouseInitCapacity) {
        this.cltChannelHouseInitCapacity = cltChannelHouseInitCapacity;
    }

    public int getCltChannelHouseListCacheSize() {
        return cltChannelHouseListCacheSize;
    }

    public void setCltChannelHouseListCacheSize(int cltChannelHouseListCacheSize) {
        this.cltChannelHouseListCacheSize = cltChannelHouseListCacheSize;
    }

    public int getSrvChannelWriteQueueCacheSize() {
        return srvChannelWriteQueueCacheSize;
    }

    public void setSrvChannelWriteQueueCacheSize(int srvChannelWriteQueueCacheSize) {
        this.srvChannelWriteQueueCacheSize = srvChannelWriteQueueCacheSize;
    }

    public int getSrvChannelCacheSize() {
        return srvChannelCacheSize;
    }

    public void setSrvChannelCacheSize(int srvChannelCacheSize) {
        this.srvChannelCacheSize = srvChannelCacheSize;
    }

    public int getSrvChannelHouseInitCapacity() {
        return srvChannelHouseInitCapacity;
    }

    public void setSrvChannelHouseInitCapacity(int srvChannelHouseInitCapacity) {
        this.srvChannelHouseInitCapacity = srvChannelHouseInitCapacity;
    }

    public int getSrvChannelHouseListCacheSize() {
        return srvChannelHouseListCacheSize;
    }

    public void setSrvChannelHouseListCacheSize(int srvChannelHouseListCacheSize) {
        this.srvChannelHouseListCacheSize = srvChannelHouseListCacheSize;
    }
}
