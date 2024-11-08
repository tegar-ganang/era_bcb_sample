package org.soda.dpws.transport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.soda.dpws.DPWSException;
import org.soda.dpws.addressing.RandomGUID;
import org.soda.dpws.handler.AbstractHandlerSupport;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.server.ServicePort;
import org.soda.dpws.service.Binding;

/**
 * 
 * 
 */
public abstract class AbstractTransport extends AbstractHandlerSupport implements Transport {

    private Map<String, Channel> channels = new HashMap<String, Channel>();

    /**
   * Disposes all the existing channels.
   */
    public void dispose() {
        for (Iterator<Channel> itr = channels.values().iterator(); itr.hasNext(); ) {
            Channel channel = itr.next();
            channel.close();
        }
    }

    public Channel createChannel() throws DPWSException {
        return createChannel(getUriPrefix() + new RandomGUID().toString());
    }

    public Channel createChannel(String uri) throws DPWSException {
        Channel c = channels.get(uri);
        if (c == null) {
            c = createNewChannel(uri);
            channels.put(c.getUri(), c);
            c.open();
        }
        return c;
    }

    public void close(Channel c) {
        channels.remove(c.getUri());
    }

    protected Map<String, Channel> getChannelMap() {
        return channels;
    }

    public String[] getSupportedBindings() {
        return new String[0];
    }

    protected abstract Channel createNewChannel(String uri);

    protected abstract String getUriPrefix();

    protected abstract String[] getKnownUriSchemes();

    public boolean isUriSupported(String uri) {
        String[] schemes = getKnownUriSchemes();
        for (int i = 0; i < schemes.length; i++) {
            if (uri.startsWith(schemes[i])) return true;
        }
        return false;
    }

    public Binding findBinding(DPWSContextImpl context, ServicePort service) {
        return null;
    }
}
