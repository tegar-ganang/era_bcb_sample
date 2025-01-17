package biz.xsoftware.impl.nio.cm.routing;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import biz.xsoftware.api.nio.BufferHelper;
import biz.xsoftware.api.nio.ChannelServiceFactory;
import biz.xsoftware.api.nio.channels.Channel;
import biz.xsoftware.api.nio.channels.RegisterableChannel;
import biz.xsoftware.api.nio.handlers.DataListener;
import biz.xsoftware.api.nio.libs.BufferFactory;
import biz.xsoftware.api.nio.libs.ChannelsRunnable;

class ThdProxyDataHandler implements DataListener {

    private static final Logger log = Logger.getLogger(ThdProxyDataHandler.class.getName());

    private static final BufferHelper HELPER = ChannelServiceFactory.bufferHelper(null);

    private ThdTCPChannel channel;

    private DataListener handler;

    private SpecialRoutingExecutor svc;

    private BufferFactory bufFactory;

    public ThdProxyDataHandler(ThdTCPChannel channel, DataListener handler, SpecialRoutingExecutor svc2, BufferFactory bufFactory) {
        this.channel = channel;
        this.handler = handler;
        this.svc = svc2;
        this.bufFactory = bufFactory;
    }

    public void incomingData(Channel realChannel, ByteBuffer b) throws IOException {
        int length = b.remaining();
        final ByteBuffer newBuffer = bufFactory.createBuffer(channel, length);
        newBuffer.put(b);
        ChannelsRunnable r = new ChannelsRunnable() {

            public void run() {
                try {
                    HELPER.doneFillingBuffer(newBuffer);
                    handler.incomingData(channel, newBuffer);
                } catch (Exception e) {
                    log.log(Level.WARNING, channel + "Exception", e);
                }
            }

            public RegisterableChannel getChannel() {
                return channel;
            }
        };
        svc.execute(realChannel, r);
    }

    public void farEndClosed(Channel realChannel) {
        ChannelsRunnable r = new ChannelsRunnable() {

            public void run() {
                try {
                    handler.farEndClosed(channel);
                } catch (Exception e) {
                    log.log(Level.WARNING, channel + "Exception", e);
                }
            }

            public RegisterableChannel getChannel() {
                return channel;
            }
        };
        svc.execute(realChannel, r);
    }

    public void failure(Channel realChannel, final ByteBuffer data, final Exception ee) {
        ChannelsRunnable r = new ChannelsRunnable() {

            public void run() {
                try {
                    handler.failure(channel, data, ee);
                } catch (Exception e) {
                    log.log(Level.WARNING, channel + "Exception", e);
                }
            }

            public RegisterableChannel getChannel() {
                return channel;
            }
        };
        svc.execute(realChannel, r);
    }
}
