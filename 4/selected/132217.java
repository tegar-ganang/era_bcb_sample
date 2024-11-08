package org.gamio.server;

import org.gamio.buffer.Buffer;
import org.gamio.channel.Channel;
import org.gamio.channel.ChannelClosedException;
import org.gamio.mq.InternalMessage;
import org.gamio.mq.MqMsgListener;
import org.gamio.system.Context;
import org.gamio.util.SimplePool;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 19 $ $Date: 2008-09-26 19:00:58 -0400 (Fri, 26 Sep 2008) $
 */
public final class ServerMqMsgListener implements MqMsgListener {

    private static OnMessagePool pool = null;

    private static final class OnMessage implements Runnable {

        private InternalMessage internalMessage = null;

        public void setInternalMessage(InternalMessage internalMessage) {
            this.internalMessage = internalMessage;
        }

        public void run() {
            if (internalMessage == null || internalMessage.getDataSize() < 1) {
                close();
                return;
            }
            Channel channel = Context.getInstance().getChannelManager().getServerChannel(internalMessage.getOriginalChannelId());
            if (channel != null) {
                channel.reactivate();
                Buffer data = internalMessage.detachData();
                try {
                    channel.write(data);
                } catch (ChannelClosedException e) {
                }
            }
            close();
        }

        private void close() {
            if (internalMessage != null) {
                internalMessage.close();
                internalMessage = null;
            }
            pool.release(this);
        }
    }

    private static final class OnMessagePool extends SimplePool<OnMessage> {

        public OnMessagePool(int cacheSize) {
            super(cacheSize);
        }

        protected OnMessage newInstance() {
            return new OnMessage();
        }
    }

    public static void initializeCache(int cacheSize) {
        pool = new OnMessagePool(cacheSize);
    }

    public void close() {
    }

    public Runnable onMessage(InternalMessage internalMessage) {
        OnMessage onMessage = pool.acquire();
        onMessage.setInternalMessage(internalMessage);
        return onMessage;
    }
}
