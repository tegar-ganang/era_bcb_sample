package com.corratech.integration;

import javax.annotation.Resource;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.apache.log4j.Logger;
import org.apache.servicemix.MessageExchangeListener;
import org.oasis_open.docs.wsn.b_2.Notify;
import com.corratech.bp.SaveEmailBP;
import com.corratech.bp.ServiceMixClientPoolFactory;
import com.opensuite.bind.services.eventmanagement.ProcessEventRequest;

/**
 * @author Aleksandr Kryzhak
 * 
 */
public class SaveEmailBPHandlerBean implements MessageExchangeListener {

    private static final int MAX_POOL_SIZE = 10;

    private static final Logger log = Logger.getLogger(SaveEmailBPHandlerBean.class);

    protected static GenericObjectPool serviceMixClientPool;

    public static GenericObjectPool getServiceMixClientPool() {
        Config config = new Config();
        config.maxActive = MAX_POOL_SIZE;
        if (serviceMixClientPool == null) {
            serviceMixClientPool = new GenericObjectPool(ServiceMixClientPoolFactory.getInstance(), config);
            serviceMixClientPool.setTestOnBorrow(true);
            serviceMixClientPool.setTestOnReturn(true);
            serviceMixClientPool.setTestWhileIdle(true);
        }
        return serviceMixClientPool;
    }

    public SaveEmailBPHandlerBean() {
        try {
            getJAXBContext();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Resource
    private DeliveryChannel channel;

    @Resource
    private ComponentContext context;

    private static JAXBContext jaxbContext;

    public void onMessageExchange(MessageExchange exchange) throws MessagingException {
        if (exchange == null) return;
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            return;
        } else if (exchange.getFault() != null) {
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
        } else {
            NormalizedMessage inMessage = exchange.getMessage("in");
            if (inMessage == null) {
                System.out.println("SaveEmailBPHandlerBean.onMessageExchange(): Exchange has no IN message");
                log.error("SaveEmailBPHandlerBean.onMessageExchange(): Exchange has no IN message");
            }
            try {
                if (inMessage.getContent() == null) throw new MessagingException("SaveEmailBPHandlerBean.onMessageExchange(): Message has no content");
                Notify notify = (Notify) getJAXBContext().createUnmarshaller().unmarshal(inMessage.getContent());
                Object msg = notify.getNotificationMessage().get(0).getMessage().getAny();
                if (msg instanceof ProcessEventRequest) {
                    SaveEmailBP bp = new SaveEmailBP((ProcessEventRequest) msg);
                    bp.run();
                }
            } catch (MessagingException e) {
                log.error(e);
                e.printStackTrace();
            } catch (JAXBException e) {
                log.error(e);
                e.printStackTrace();
            }
        }
    }

    /**
	 * @return the context
	 */
    public ComponentContext getContext() {
        return context;
    }

    /**
	 * @param context
	 *            the context to set
	 */
    public void setContext(ComponentContext context) {
        this.context = context;
    }

    /**
	 * @return the channel
	 */
    public DeliveryChannel getChannel() {
        return channel;
    }

    /**
	 * @param channel
	 *            the channel to set
	 */
    public void setChannel(DeliveryChannel channel) {
        this.channel = channel;
    }

    private static JAXBContext getJAXBContext() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance("org.oasis_open.docs.wsn.b_2:" + "com.opensuite.bind.services.eventmanagement");
        }
        return jaxbContext;
    }
}
