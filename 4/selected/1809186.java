package games.strategy.engine.message;

import games.strategy.net.INode;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Implementation of IChannelMessenger built on top of an IMessenger
 * 
 * @author Sean Bridges
 */
public class ChannelMessenger implements IChannelMessenger {

    private final UnifiedMessenger m_unifiedMessenger;

    public ChannelMessenger(final UnifiedMessenger messenger) {
        m_unifiedMessenger = messenger;
    }

    UnifiedMessenger getUnifiedMessenger() {
        return m_unifiedMessenger;
    }

    public IChannelSubscribor getChannelBroadcastor(final RemoteName channelName) {
        final InvocationHandler ih = new UnifiedInvocationHandler(m_unifiedMessenger, channelName.getName(), true, channelName.getClazz());
        final IChannelSubscribor rVal = (IChannelSubscribor) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] { channelName.getClazz() }, ih);
        return rVal;
    }

    public void registerChannelSubscriber(final Object implementor, final RemoteName channelName) {
        if (!IChannelSubscribor.class.isAssignableFrom(channelName.getClazz())) throw new IllegalStateException(channelName.getClazz() + " is not a channel subscribor");
        m_unifiedMessenger.addImplementor(channelName, implementor, true);
    }

    public void unregisterChannelSubscriber(final Object implementor, final RemoteName channelName) {
        m_unifiedMessenger.removeImplementor(channelName.getName(), implementor);
    }

    public INode getLocalNode() {
        return m_unifiedMessenger.getLocalNode();
    }

    public boolean isServer() {
        return m_unifiedMessenger.isServer();
    }
}
