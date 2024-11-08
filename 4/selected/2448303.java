package org.traccar;

import org.jboss.netty.channel.*;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.traccar.model.DataManager;

/**
  * Generic pipeline factory
  */
public abstract class GenericPipelineFactory implements ChannelPipelineFactory {

    private TrackerServer server;

    private DataManager dataManager;

    private Boolean loggerEnabled;

    /**
     * Open channel handler
     */
    protected class OpenChannelHandler extends SimpleChannelHandler {

        private TrackerServer server;

        public OpenChannelHandler(TrackerServer server) {
            this.server = server;
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
            server.getChannelGroup().add(e.getChannel());
        }
    }

    public GenericPipelineFactory(TrackerServer server, DataManager dataManager, Boolean loggerEnabled) {
        this.server = server;
        this.dataManager = dataManager;
        this.loggerEnabled = loggerEnabled;
    }

    protected DataManager getDataManager() {
        return dataManager;
    }

    protected abstract void addSpecificHandlers(ChannelPipeline pipeline);

    public ChannelPipeline getPipeline() {
        ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("openHandler", new OpenChannelHandler(server));
        if (loggerEnabled) {
            pipeline.addLast("logger", new LoggingHandler("logger"));
        }
        addSpecificHandlers(pipeline);
        pipeline.addLast("handler", new TrackerEventHandler(dataManager));
        return pipeline;
    }
}
