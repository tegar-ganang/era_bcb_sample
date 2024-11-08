package com.hongbo.cobweb.nmr.core;

import java.util.Map;
import java.util.Collections;
import com.hongbo.cobweb.nmr.api.Channel;
import com.hongbo.cobweb.nmr.api.Endpoint;
import com.hongbo.cobweb.nmr.api.Exchange;
import com.hongbo.cobweb.nmr.api.NMR;
import com.hongbo.cobweb.nmr.api.internal.InternalChannel;
import com.hongbo.cobweb.nmr.api.internal.InternalEndpoint;
import com.hongbo.cobweb.nmr.core.util.UuidGenerator;
import com.hongbo.cobweb.executors.Executor;

/**
 * A {@link Channel} to be used as a client.
 * Only sendSync should be used, else an exception will occur
 */
public class ClientChannel extends ChannelImpl {

    public ClientChannel(NMR nmr, Executor executor) {
        super(new ClientEndpoint(), executor, nmr);
        getEndpoint().setChannel(this);
    }

    protected static class ClientEndpoint implements InternalEndpoint {

        private InternalChannel channel;

        private String id = UuidGenerator.getUUID();

        public String getId() {
            return id;
        }

        public Map<String, ?> getMetaData() {
            return Collections.emptyMap();
        }

        public Endpoint getEndpoint() {
            return this;
        }

        public void setChannel(Channel channel) {
            this.channel = (InternalChannel) channel;
        }

        public InternalChannel getChannel() {
            return channel;
        }

        public void process(Exchange exchange) {
            throw new IllegalStateException();
        }
    }
}
