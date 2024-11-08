package org.gamio.client;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.gamio.channel.Channel;
import org.gamio.channel.ClientChannelPool;
import org.gamio.channel.ClientChannelPoolListener;
import org.gamio.conf.ClientProps;
import org.gamio.logging.Log;
import org.gamio.logging.Logger;
import org.gamio.mq.InternalMessage;
import org.gamio.mq.MqMsgListener;
import org.gamio.system.Context;
import org.gamio.util.GmQueue;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 20 $ $Date: 2008-10-01 19:37:36 -0400 (Wed, 01 Oct 2008) $
 */
public final class ClientMqMsgListener implements MqMsgListener {

    private static final Log log = Logger.getLogger(ClientMqMsgListener.class);

    private ClientChannelPool clientChannelPool = null;

    private OnMessage onMessageEvt = null;

    private GmQueue<InternalMessage> queue = null;

    private abstract class OnMessage implements Runnable, ClientChannelPoolListener {

        boolean run(Channel channel) {
            InternalMessage internalMessage = queue.pop();
            if (internalMessage != null) {
                channel.setSessionId(internalMessage.getSessionId());
                channel.setOriginalChannelId(internalMessage.getOriginalChannelId());
                channel.setPath(internalMessage.getPath());
                try {
                    channel.send(internalMessage.detachData());
                } catch (Exception e) {
                    channel.close();
                    log.error(e, "Failed to send");
                    return false;
                } finally {
                    internalMessage.close();
                }
            } else channel.release();
            return true;
        }

        void close() {
        }
    }

    private final class OnMessageNp extends OnMessage {

        public void run() {
            try {
                run(clientChannelPool.getClientChannel());
            } catch (Exception e) {
                log.error(e, "Failed to run");
            }
        }

        public void onClientChannelAvailable() {
        }

        public void onSendError() {
        }
    }

    private final class OnMessageP extends OnMessage {

        private int size = 0;

        private final Lock lock = new ReentrantLock();

        public void run() {
            Channel channel = null;
            do {
                lock.lock();
                try {
                    if ((channel = clientChannelPool.getClientChannel()) == null) {
                        size++;
                        return;
                    }
                } finally {
                    lock.unlock();
                }
            } while (!run(channel) && needRun());
        }

        public void onClientChannelAvailable() {
            if (needRun()) {
                try {
                    Context.getInstance().getWorkshop().run(this);
                } catch (Exception e) {
                    log.warn(e, "Use current thread to run");
                    run();
                }
            }
        }

        public void onSendError() {
            if (needRun()) {
                try {
                    Context.getInstance().getWorkshop().run(this);
                } catch (Exception e) {
                    log.warn(e, "Use current thread to run");
                    run();
                }
            }
        }

        @Override
        void close() {
            size = 0;
        }

        private boolean needRun() {
            lock.lock();
            try {
                if (size <= 0) return false;
                size--;
                return true;
            } finally {
                lock.unlock();
            }
        }
    }

    public ClientMqMsgListener(ClientProps clientProps) {
        onMessageEvt = clientProps.isPooling() ? new OnMessageP() : new OnMessageNp();
        clientChannelPool = Context.getInstance().getChannelManager().createClientChannelPool(clientProps, onMessageEvt);
        queue = new GmQueue<InternalMessage>(clientProps.getMqMsgQueueCacheSize());
    }

    public Runnable onMessage(InternalMessage internalMessage) {
        queue.push(internalMessage);
        return onMessageEvt;
    }

    public void close() {
        clientChannelPool.close();
        InternalMessage internalMessage = null;
        while ((internalMessage = queue.pop()) != null) internalMessage.close();
        onMessageEvt.close();
    }
}
