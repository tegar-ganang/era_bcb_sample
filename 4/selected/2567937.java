package com.polarrose.spring.bayeux;

import com.polarrose.spring.bayeux.util.BayeuxUtil;
import com.polarrose.spring.bayeux.util.JsonUtil;
import dojox.cometd.Channel;
import dojox.cometd.Client;
import dojox.cometd.DataFilter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stefan Arentz
 */
public class BayeuxChannel implements InitializingBean {

    public BayeuxChannel() {
    }

    public BayeuxChannel(BayeuxServer bayeuxServer, Channel channel) {
        this.bayeuxServer = bayeuxServer;
        this.channel = channel;
        this.name = channel.getId();
    }

    /**
     *
     */
    private BayeuxServer bayeuxServer;

    @Required
    public void setBayeuxServer(BayeuxServer bayeuxServer) {
        this.bayeuxServer = bayeuxServer;
    }

    /**
     *
     */
    private String name;

    @Required
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     *
     */
    private Channel channel;

    Channel getChannel() {
        return channel;
    }

    /**
     *
     */
    private List<BayeuxFilter> filters = new ArrayList<BayeuxFilter>();

    public void setFilters(List<BayeuxFilter> filters) {
        this.filters = filters;
    }

    public void publish(Object object) {
        channel.publish(null, JsonUtil.transmogrify(object), null);
    }

    public void publish(Object object, BayeuxClient from) {
        channel.publish(from.getClient(), JsonUtil.transmogrify(object), null);
    }

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        channel = bayeuxServer.getBayeux().getChannel(name, true);
        for (BayeuxFilter filter : filters) {
            channel.addDataFilter(new DataFilterAdapter(filter));
        }
    }

    /**
     * 
     * @param client
     */
    public void subscribe(BayeuxClient client) {
        channel.subscribe(client.getClient());
    }

    public boolean deliver(String clientId, Object message) {
        Client client = bayeuxServer.getBayeux().getClient(clientId);
        if (client != null) {
            client.deliver(null, this.getName(), JsonUtil.transmogrify(message), null);
        }
        return (client != null);
    }

    public void deliver(BayeuxClient client, Object message) {
        client.deliver(null, this, JsonUtil.transmogrify(message));
    }

    class DataFilterAdapter implements DataFilter {

        private final Class<?> filterDataClass;

        private final BayeuxFilter bayeuxFilter;

        DataFilterAdapter(BayeuxFilter bayeuxFilter) {
            this.filterDataClass = BayeuxUtil.getBayeuxFilterDataClass(bayeuxFilter);
            this.bayeuxFilter = bayeuxFilter;
        }

        public Object filter(Client from, Channel to, Object data) throws IllegalStateException {
            if (filterDataClass != null) {
                return JsonUtil.transmogrify(bayeuxFilter.filter(bayeuxServer, new BayeuxClient(bayeuxServer, from), JsonUtil.transmogrify(filterDataClass, data, null)));
            } else {
                return bayeuxFilter.filter(bayeuxServer, new BayeuxClient(bayeuxServer, from), data);
            }
        }
    }

    public String toString() {
        return "BayeuxChannelImpl[name=" + getName() + "]";
    }
}
