package games.strategy.engine.history;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameModifiedChannel;
import games.strategy.engine.message.IChannelMessenger;

/**
 * Has a subset of the historyWriters functionality.
 * Delegates should only have access to these functions.
 * The rest of the history writers functions should only
 * be used by the GameData
 */
public class DelegateHistoryWriter implements IDelegateHistoryWriter {

    private final IGameModifiedChannel m_channel;

    public DelegateHistoryWriter(final IChannelMessenger messenger) {
        m_channel = (IGameModifiedChannel) messenger.getChannelBroadcastor(IGame.GAME_MODIFICATION_CHANNEL);
    }

    public DelegateHistoryWriter(final IGameModifiedChannel channel) {
        m_channel = channel;
    }

    private IGameModifiedChannel getGameModifiedChannel() {
        return m_channel;
    }

    public void startEvent(final String eventName) {
        getGameModifiedChannel().startHistoryEvent(eventName);
    }

    public void addChildToEvent(final String child) {
        addChildToEvent(child, null);
    }

    public void addChildToEvent(final String child, final Object renderingData) {
        getGameModifiedChannel().addChildToEvent(child, renderingData);
    }

    /**
	 * Set the redering data for the current event.
	 */
    public void setRenderingData(final Object renderingData) {
        getGameModifiedChannel().setRenderingData(renderingData);
    }
}
