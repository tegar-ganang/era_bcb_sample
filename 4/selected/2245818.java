package org.gamio.channel;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.gamio.server.ServerChannelMsgListener;
import org.gamio.system.Context;
import org.gamio.util.Helper;
import org.gamio.util.SimplePool;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 23 $ $Date: 2008-10-05 21:00:52 -0400 (Sun, 05 Oct 2008) $
 */
public final class ServerChannelHouse extends ChannelHouse implements ServerChannelFactory {

    private static final Lock lock = new ReentrantLock();

    private static int id = 0;

    private static int writeQueueCacheSize = 16;

    private ServerChannelCache serverChannelCache = null;

    private final ChannelMsgListener channelMsgListener = new ServerChannelMsgListener();

    private static int genId() {
        lock.lock();
        try {
            return ++id;
        } finally {
            lock.unlock();
        }
    }

    private final class ServerChannel extends Channel {

        private String sessionId = null;

        public void activate() {
            ServerChannelHouse.this.activate(this);
        }

        public void reactivate() {
            ServerChannelHouse.this.reactivate(getId());
        }

        public String getSessionId() {
            if (sessionId == null) {
                StringBuilder strBldr = Helper.getRawStringBuilder();
                int start = strBldr.length();
                strBldr.append(Context.getInstance().getInstProps().getId()).append(getGateId());
                Helper.dumpInt32(strBldr, getId());
                sessionId = strBldr.substring(start);
                strBldr.delete(start, strBldr.length());
            }
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            throw new UnsupportedOperationException("Not supported.");
        }

        public Integer getOriginalChannelId() {
            return getId();
        }

        public void setOriginalChannelId(Integer originalChannelId) {
            throw new UnsupportedOperationException("Not supported.");
        }

        public void onHouseClose() {
            if (rawClose() != RCStatus.E_ALREADYCLOSED) {
                sessionId = null;
                serverChannelCache.release(this);
            }
        }

        public void close() {
            remove(getId());
            onHouseClose();
        }

        protected int getWriteQueueCacheSize() {
            return writeQueueCacheSize;
        }

        protected int generateId() {
            return genId();
        }

        protected String getTypeName() {
            return "Server";
        }

        protected ChannelMsgListener getChannelMsgListener() {
            return channelMsgListener;
        }
    }

    private final class ServerChannelCache extends SimplePool<ServerChannel> {

        public ServerChannelCache(int capacity) {
            super(capacity);
        }

        protected ServerChannel newInstance() {
            return new ServerChannel();
        }
    }

    public ServerChannelHouse() {
        this(0);
    }

    public ServerChannelHouse(int srvChannelCacheSize) {
        this(srvChannelCacheSize, 32);
    }

    public ServerChannelHouse(int srvChannelCacheSize, int initCapacity) {
        this(srvChannelCacheSize, initCapacity, 0);
    }

    public ServerChannelHouse(int srvChannelCacheSize, int initCapacity, int srvChannelHouseListCacheSize) {
        super(initCapacity, srvChannelHouseListCacheSize);
        serverChannelCache = new ServerChannelCache(srvChannelCacheSize);
    }

    public static void setWriteQueueCacheSize(int writeQueueCacheSize) {
        ServerChannelHouse.writeQueueCacheSize = writeQueueCacheSize;
    }

    public Channel createServerChannel() {
        return serverChannelCache.acquire();
    }
}
