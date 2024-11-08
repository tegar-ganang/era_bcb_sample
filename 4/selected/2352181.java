package games.strategy.engine.chat;

import games.strategy.net.INode;
import games.strategy.net.Messengers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class StatusManager {

    private final List<IStatusListener> m_listeners = new CopyOnWriteArrayList<IStatusListener>();

    private final Map<INode, String> m_status = new HashMap<INode, String>();

    private final Messengers m_messengers;

    private final Object m_mutex = new Object();

    private final IStatusChannel m_statusChannelSubscribor;

    public StatusManager(final Messengers messengers) {
        m_messengers = messengers;
        m_statusChannelSubscribor = new IStatusChannel() {

            public void statusChanged(final INode node, final String status) {
                synchronized (m_mutex) {
                    if (status == null) {
                        m_status.remove(node);
                    } else {
                        m_status.put(node, status);
                    }
                }
                notifyStatusChanged(node, status);
            }
        };
        if (messengers.getMessenger().isServer() && !messengers.getRemoteMessenger().hasLocalImplementor(IStatusController.STATUS_CONTROLLER)) {
            final StatusController controller = new StatusController(messengers);
            messengers.getRemoteMessenger().registerRemote(controller, IStatusController.STATUS_CONTROLLER);
        }
        m_messengers.getChannelMessenger().registerChannelSubscriber(m_statusChannelSubscribor, IStatusChannel.STATUS_CHANNEL);
        final IStatusController controller = (IStatusController) m_messengers.getRemoteMessenger().getRemote(IStatusController.STATUS_CONTROLLER);
        final Map<INode, String> values = controller.getAllStatus();
        synchronized (m_mutex) {
            m_status.putAll(values);
        }
    }

    public void shutDown() {
        m_messengers.getChannelMessenger().unregisterChannelSubscriber(m_statusChannelSubscribor, IStatusChannel.STATUS_CHANNEL);
    }

    /**
	 * Get the status for the given node.
	 */
    public String getStatus(final INode node) {
        synchronized (m_mutex) {
            return m_status.get(node);
        }
    }

    public void setStatus(final String status) {
        final IStatusController controller = (IStatusController) m_messengers.getRemoteMessenger().getRemote(IStatusController.STATUS_CONTROLLER);
        controller.setStatus(status);
    }

    public void addStatusListener(final IStatusListener listener) {
        m_listeners.add(listener);
    }

    public void removeStatusListener(final IStatusListener listener) {
        m_listeners.remove(listener);
    }

    private void notifyStatusChanged(final INode node, final String newStatus) {
        for (final IStatusListener listener : m_listeners) {
            listener.statusChanged(node, newStatus);
        }
    }
}
