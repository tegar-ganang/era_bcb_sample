package org.gamio.channel;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.gamio.buffer.Buffer;
import org.gamio.channel.Channel.RCStatus;
import org.gamio.client.ClientChannelMsgListener;
import org.gamio.conf.ClientProps;
import org.gamio.system.Context;
import org.gamio.util.IteratorEx;
import org.gamio.util.LinkedListEx;
import org.gamio.util.SimplePool;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 19 $ $Date: 2008-09-26 19:00:58 -0400 (Fri, 26 Sep 2008) $
 */
public final class ClientChannelHouse extends ChannelHouse {

    private static final Lock lock = new ReentrantLock();

    private static int id = 0;

    private static int writeQueueCacheSize = 1;

    private ClientChannelCache clientChannelCache = null;

    private final ChannelMsgListener channelMsgListener = new ClientChannelMsgListener();

    private static int genId() {
        lock.lock();
        try {
            return ++id;
        } finally {
            lock.unlock();
        }
    }

    private interface PoolStrategy {

        public void release(ClientChannel clientChannel);

        public void destroy(ClientChannel clientChannel);
    }

    private final class ClientChannel extends Channel {

        private int timeout = 0;

        private boolean idle = true;

        private boolean sendErrHandlerEnabled = false;

        private String sessionId = null;

        private Integer originalChannelId = null;

        private PoolStrategy poolStrategy = null;

        private IteratorEx<ClientChannel> iterInPool = null;

        public boolean isIdle() {
            return idle;
        }

        public void setIdle(boolean idle) {
            this.idle = idle;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public Integer getOriginalChannelId() {
            return originalChannelId;
        }

        public void setOriginalChannelId(Integer originalChannelId) {
            this.originalChannelId = originalChannelId;
        }

        public void setPoolStrategy(PoolStrategy poolStrategy) {
            this.poolStrategy = poolStrategy;
        }

        public IteratorEx<ClientChannel> getIterInPool() {
            return iterInPool;
        }

        public void setIterInPool(IteratorEx<ClientChannel> iterInPool) {
            this.iterInPool = iterInPool;
        }

        @Override
        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public void activate() {
            ClientChannelHouse.this.activate(this);
        }

        public void reactivate() {
            ClientChannelHouse.this.reactivate(getId());
        }

        public void onHouseClose() {
            PoolStrategy poolStrategyTemp = poolStrategy;
            if (poolStrategyTemp != null) poolStrategyTemp.destroy(this);
        }

        @Override
        public void send(Buffer data) throws Exception {
            getLock().lock();
            try {
                disableSendErrHandler();
                getChannelState().send(this, data);
                enableSendErrHandler();
            } finally {
                getLock().unlock();
            }
        }

        @Override
        public void release() {
            poolStrategy.release(this);
        }

        public void clear() {
            iterInPool = null;
            timeout = 0;
            sessionId = null;
            originalChannelId = null;
            poolStrategy = null;
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
            return "Client";
        }

        protected ChannelMsgListener getChannelMsgListener() {
            return channelMsgListener;
        }

        private boolean isSendErrHandlerEnabled() {
            return sendErrHandlerEnabled;
        }

        private void enableSendErrHandler() {
            sendErrHandlerEnabled = true;
        }

        private void disableSendErrHandler() {
            sendErrHandlerEnabled = false;
        }
    }

    private final class ClientChannelCache extends SimplePool<ClientChannel> {

        public ClientChannelCache(int capacity) {
            super(capacity);
        }

        protected ClientChannel newInstance() {
            return new ClientChannel();
        }
    }

    private interface ClientChannelPoolState {

        public Channel getClientChannel(ClientChannelPoolImpl clientChannelPoolImpl);

        public void close(ClientChannelPoolImpl clientChannelPoolImpl);

        public void release(ClientChannelPoolImpl clientChannelPoolImpl, ClientChannel clientChannel);
    }

    private static final class ClientChannelPoolCreated implements ClientChannelPoolState {

        private static ClientChannelPoolCreated created = new ClientChannelPoolCreated();

        public static ClientChannelPoolState getInstance() {
            return created;
        }

        public void close(ClientChannelPoolImpl clientChannelPoolImpl) {
            clientChannelPoolImpl.changeState(ClientChannelPoolClosed.getInstance());
        }

        public Channel getClientChannel(ClientChannelPoolImpl clientChannelPoolImpl) {
            return clientChannelPoolImpl.getClientChannelNonSync();
        }

        public void release(ClientChannelPoolImpl clientChannelPoolImpl, ClientChannel clientChannel) {
            clientChannelPoolImpl.releaseNonSync(clientChannel);
        }
    }

    private static final class ClientChannelPoolClosed implements ClientChannelPoolState {

        private static ClientChannelPoolClosed closed = new ClientChannelPoolClosed();

        public static ClientChannelPoolState getInstance() {
            return closed;
        }

        public void close(ClientChannelPoolImpl clientChannelPoolImpl) {
        }

        public Channel getClientChannel(ClientChannelPoolImpl clientChannelPoolImpl) {
            return null;
        }

        public void release(ClientChannelPoolImpl clientChannelPoolImpl, ClientChannel clientChannel) {
        }
    }

    private final class ClientChannelPoolImpl implements ClientChannelPool, PoolStrategy {

        private ClientProps clientProps = null;

        private ClientChannelPoolListener clientChannelPoolListener = null;

        private LinkedListEx<ClientChannel> list = new LinkedListEx<ClientChannel>();

        private IteratorEx<ClientChannel> endIter = list.addBack(null);

        private int size = 0;

        private Lock lock = new ReentrantLock();

        private ClientChannelPoolState state = ClientChannelPoolCreated.getInstance();

        public ClientChannelPoolImpl(ClientProps clientProps, ClientChannelPoolListener clientChannelPoolListener) {
            this.clientProps = clientProps;
            this.clientChannelPoolListener = clientChannelPoolListener;
        }

        public Channel getClientChannel() {
            lock.lock();
            try {
                return state.getClientChannel(this);
            } finally {
                lock.unlock();
            }
        }

        public void close() {
            lock.lock();
            try {
                state.close(this);
            } finally {
                lock.unlock();
            }
            closeNonSync();
        }

        public void release(ClientChannel clientChannel) {
            lock.lock();
            try {
                state.release(this, clientChannel);
            } finally {
                lock.unlock();
            }
            clientChannelPoolListener.onClientChannelAvailable();
        }

        public void destroy(ClientChannel clientChannel) {
            RCStatus rcs = clientChannel.rawClose();
            if (rcs != RCStatus.E_ALREADYCLOSED) {
                if (clientChannel.isIdle()) size--;
                clientChannel.getIterInPool().remove();
                clientChannel.clear();
                clientChannelCache.release(clientChannel);
                if (clientChannel.isSendErrHandlerEnabled() && rcs == RCStatus.E_SENDERROR) clientChannelPoolListener.onSendError();
            }
        }

        public void changeState(ClientChannelPoolState state) {
            this.state = state;
        }

        public Channel getClientChannelNonSync() {
            ClientChannel channel = null;
            IteratorEx<ClientChannel> iter = null;
            if (list.size() <= clientProps.getMinPoolSize() || (!endIter.hasNext() && list.size() <= clientProps.getMaxPoolSize())) {
                iter = list.addFront(null);
                channel = clientChannelCache.acquire();
                channel.setGateProps(clientProps);
                channel.setPoolStrategy(this);
                channel.setIdle(false);
                channel.setTimeout(clientProps.getTimeout());
                channel.setIterInPool(iter);
                iter.set(channel);
            } else if (endIter.hasNext()) {
                iter = list.getLast();
                list.moveToFront(iter);
                channel = iter.get();
                if (channel.isIdle()) {
                    channel.setIdle(false);
                    size--;
                }
                channel.setTimeout(clientProps.getTimeout());
            }
            return channel;
        }

        public void closeNonSync() {
            list.moveToFront(endIter);
            while (endIter.hasNext()) endIter.next().get().close();
            clientProps = null;
            clientChannelPoolListener = null;
        }

        public void releaseNonSync(ClientChannel clientChannel) {
            clientChannel.clearName();
            clientChannel.setSessionId(null);
            clientChannel.setOriginalChannelId(null);
            ChannelManager channelManager = Context.getInstance().getChannelManager();
            list.moveAfter(endIter, clientChannel.getIterInPool());
            if (size >= clientProps.getMinPoolSize()) {
                clientChannel.setTimeout(clientProps.getKeepAliveTime());
                clientChannel.reactivate();
            } else if (clientChannel.getTimeout() > 0) {
                size++;
                clientChannel.setIdle(true);
                free(clientChannel.getId());
            }
            channelManager.onReadRequired(clientChannel);
        }
    }

    private final class ClientChannelFactory implements ClientChannelPool, PoolStrategy {

        private ClientProps clientProps = null;

        private ClientChannelPoolListener clientChannelPoolListener = null;

        public ClientChannelFactory(ClientProps clientProps, ClientChannelPoolListener clientChannelPoolListener) {
            this.clientProps = clientProps;
            this.clientChannelPoolListener = clientChannelPoolListener;
        }

        public Channel getClientChannel() {
            ClientChannel channel = clientChannelCache.acquire();
            channel.setGateProps(clientProps);
            channel.setTimeout(clientProps.getTimeout());
            channel.setPoolStrategy(this);
            return channel;
        }

        public void destroy(ClientChannel clientChannel) {
            RCStatus rcs = clientChannel.rawClose();
            if (rcs != RCStatus.E_ALREADYCLOSED) {
                clientChannel.clear();
                clientChannelCache.release(clientChannel);
                if (clientChannel.isSendErrHandlerEnabled() && rcs == RCStatus.E_SENDERROR) clientChannelPoolListener.onSendError();
            }
        }

        public void release(ClientChannel clientChannel) {
            clientChannel.close();
        }

        public void close() {
            clientProps = null;
            clientChannelPoolListener = null;
        }
    }

    public ClientChannelHouse() {
        this(0);
    }

    public ClientChannelHouse(int cltChannelCacheSize) {
        this(cltChannelCacheSize, 32);
    }

    public ClientChannelHouse(int cltChannelCacheSize, int initCapacity) {
        this(cltChannelCacheSize, initCapacity, 0);
    }

    public ClientChannelHouse(int cltChannelCacheSize, int initCapacity, int cltChannelHouseListCacheSize) {
        super(initCapacity, cltChannelHouseListCacheSize);
        clientChannelCache = new ClientChannelCache(cltChannelCacheSize);
    }

    public static void setWriteQueueCacheSize(int writeQueueCacheSize) {
        ClientChannelHouse.writeQueueCacheSize = writeQueueCacheSize;
    }

    public ClientChannelPool createClientChannelPool(ClientProps clientProps, ClientChannelPoolListener clientChannelPoolListener) {
        return clientProps.isPooling() ? new ClientChannelPoolImpl(clientProps, clientChannelPoolListener) : new ClientChannelFactory(clientProps, clientChannelPoolListener);
    }
}
