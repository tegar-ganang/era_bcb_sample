package engine.server.senders.ServerClient;

import engine.QueueIF;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * The bocked loop thread object.
 * In each loop iteration bocked in q untill new data portion arrived,
 * then sent it.
 */
public class ServerClientSender extends Thread {

    private volatile Thread self = null;

    private QueueIF q;

    private Channel channel;

    private static Logger log = Logger.getLogger(ServerClientSender.class.getName());

    public ServerClientSender(QueueIF q) {
        this.q = q;
    }

    @Override
    public final void start() {
        if (self == null) {
            self = new Thread(this);
            self.start();
        }
    }

    public final void shutdown() {
        self = null;
    }

    @Override
    public void run() {
        Thread thisThread = Thread.currentThread();
        ChannelFuture lastWriteFuture = null;
        while (self == thisThread) {
            final ChannelBuffer buf = q.get();
            buf.readerIndex(0);
            try {
                lastWriteFuture = channel.write(buf);
                lastWriteFuture.addListener(new ChannelFutureListener() {

                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            fireDataSent(buf, channel.getId().toString());
                        } else {
                            fireDataTransmissionError(channel.getId().toString());
                        }
                    }
                });
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    log.error(ex.getMessage());
                }
            } catch (NullPointerException ex) {
                fireDataTransmissionError("No client");
            }
            yield();
        }
        if (lastWriteFuture != null) {
            lastWriteFuture.awaitUninterruptibly();
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    protected javax.swing.event.EventListenerList CustomEventsListenerList = new javax.swing.event.EventListenerList();

    public void addListener(ServerClientSenderListenerIF listener) {
        CustomEventsListenerList.add(ServerClientSenderListenerIF.class, listener);
    }

    public void removeListener(ServerClientSenderListenerIF listener) {
        CustomEventsListenerList.remove(ServerClientSenderListenerIF.class, listener);
    }

    public void fireDataSent(ChannelBuffer bufferedMessage, String ClientID) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerClientSenderListenerIF.class) {
                ((ServerClientSenderListenerIF) listeners[i + 1]).DataSent(bufferedMessage, ClientID);
            }
        }
    }

    public void fireDataTransmissionError(String ClientID) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerClientSenderListenerIF.class) {
                ((ServerClientSenderListenerIF) listeners[i + 1]).DataTransmissionError(ClientID);
            }
        }
    }
}
