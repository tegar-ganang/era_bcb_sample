package edu.cmu.ece.agora.kernel.router;

import java.net.InetSocketAddress;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.TerminationNotice;

public class Link {

    private Channel channel;

    private Object context;

    public Link() {
        this(null);
    }

    public Link(Channel channel) {
        this.channel = channel;
    }

    @SuppressWarnings("unchecked")
    public <T> T getContext() {
        return (T) context;
    }

    public <T> void setContext(T context) {
        this.context = context;
    }

    public InetSocketAddress getRemoteAddress() {
        if (channel == null) {
            return new InetSocketAddress("127.0.0.1", 65535);
        } else {
            return (InetSocketAddress) this.channel.getRemoteAddress();
        }
    }

    Channel getChannel() {
        return channel;
    }

    public void close(String reason) {
        if (channel != null) {
            TerminationNotice tn = TerminationNotice.newBuilder().setReason(reason).build();
            channel.write(tn).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
