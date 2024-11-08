package com.polarrose.spring.bayeux.jetty;

import com.polarrose.spring.bayeux.BayeuxChannel;
import com.polarrose.spring.bayeux.BayeuxClient;
import com.polarrose.spring.bayeux.BayeuxServer;
import dojox.cometd.Bayeux;
import dojox.cometd.Channel;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Stefan Arentz
 */
public class JettyBayeuxServer implements InitializingBean, BayeuxServer {

    /**
     * The configured name of the BayeuxServlet.
     */
    private String servletName;

    public void setServletName(String servletName) {
        this.servletName = servletName;
    }

    /**
     *
     */
    private Bayeux bayeux;

    public Bayeux getBayeux() {
        return bayeux;
    }

    /**
     *
     * @param channel
     * @param message
     */
    public void publish(BayeuxChannel channel, Object message) {
        channel.publish(message);
    }

    /**
     *
     * @param channel
     * @param message
     * @param from
     */
    public void publish(BayeuxChannel channel, Object message, BayeuxClient from) {
        channel.publish(message, from);
    }

    /**
     *
     * @param name
     * @return
     */
    public BayeuxChannel getChannel(String name) {
        BayeuxChannel bayeuxChannel = null;
        Channel channel = bayeux.getChannel(name, false);
        if (channel != null) {
            bayeuxChannel = new BayeuxChannel(this, channel);
        }
        return bayeuxChannel;
    }

    /**
     *
     * @param name
     * @param create
     * @return
     */
    public BayeuxChannel getChannel(String name, boolean create) {
        return null;
    }

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        if (servletName == null) {
            bayeux = JettyBayeuxServlet.getDefaultBayeux();
        } else {
            bayeux = JettyBayeuxServlet.getBayeuxByServletName(servletName);
        }
    }
}
