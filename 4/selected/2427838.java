package org.traccar;

import java.util.Timer;
import java.util.TimerTask;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.traccar.model.DataManager;

/**
 * Base class for protocol decoders
 */
public abstract class GenericProtocolDecoder extends OneToOneDecoder {

    /**
     * Data manager
     */
    private DataManager dataManager;

    /**
     * Set data manager
     */
    public final void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Return data manager
     */
    public final DataManager getDataManager() {
        return dataManager;
    }

    /**
     * Reset connection delay
     */
    private Integer resetDelay;

    /**
     * Set reset connection delay
     */
    public final void setResetDelay(Integer resetDelay) {
        this.resetDelay = resetDelay;
    }

    /**
     * Default constructor
     */
    public GenericProtocolDecoder() {
    }

    /**
     * Initialize
     */
    public GenericProtocolDecoder(DataManager dataManager, Integer resetDelay) {
        setDataManager(dataManager);
        setResetDelay(resetDelay);
    }

    /**
     * Disconnect channel
     */
    private class DisconnectTask extends TimerTask {

        private Channel channel;

        public DisconnectTask(Channel channel) {
            this.channel = channel;
        }

        public void run() {
            channel.disconnect();
        }
    }

    /**
     * Handle connect event
     */
    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        super.handleUpstream(ctx, evt);
        if (evt instanceof ChannelStateEvent) {
            ChannelStateEvent event = (ChannelStateEvent) evt;
            if (event.getState() == ChannelState.CONNECTED && event.getValue() != null && resetDelay != 0) {
                new Timer().schedule(new GenericProtocolDecoder.DisconnectTask(evt.getChannel()), resetDelay);
            }
        }
    }
}
