package com.hongbo.cobweb.nmr.runtime.spi.endpoints;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import com.hongbo.cobweb.nmr.runtime.spi.DefaultComponent;
import com.hongbo.cobweb.nmr.runtime.spi.EndpointComponentContext;
import com.hongbo.cobweb.nmr.runtime.spi.ServiceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleEndpoint extends AbstractEndpoint {

    protected static Logger logger = LoggerFactory.getLogger(SimpleEndpoint.class);

    private DeliveryChannel channel;

    private MessageExchangeFactory exchangeFactory;

    private ComponentContext context;

    public SimpleEndpoint() {
    }

    public SimpleEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    public SimpleEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component.getServiceUnit(), endpoint.getServiceName(), endpoint.getEndpointName());
    }

    public synchronized void activate() throws Exception {
        context = new EndpointComponentContext(this);
        channel = context.getDeliveryChannel();
        exchangeFactory = channel.createExchangeFactory();
    }

    public synchronized void deactivate() throws Exception {
    }

    public synchronized void start() throws Exception {
    }

    public synchronized void stop() throws Exception {
    }

    protected void send(MessageExchange me) throws MessagingException {
        channel.send(me);
    }

    protected void sendSync(MessageExchange me) throws MessagingException {
        if (!channel.sendSync(me)) {
            throw new MessagingException("SendSync failed");
        }
    }

    protected void done(MessageExchange me) throws MessagingException {
        logger.debug("SimpleEndpoint.done called: ");
        me.setStatus(ExchangeStatus.DONE);
        send(me);
    }

    protected void fail(MessageExchange me, Exception error) throws MessagingException {
        logger.warn("SimpleEndpoint.fail called: ", error);
        me.setError(error);
        send(me);
    }

    /**
     * @return the exchangeFactory
     */
    public MessageExchangeFactory getExchangeFactory() {
        return exchangeFactory;
    }

    /**
     * @return the channel
     */
    public DeliveryChannel getChannel() {
        return channel;
    }

    /**
     * @return the context
     */
    public ComponentContext getContext() {
        return context;
    }
}
