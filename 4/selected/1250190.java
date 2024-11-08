package com.corratech.integration;

import javax.annotation.Resource;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import org.apache.log4j.Logger;
import org.apache.servicemix.MessageExchangeListener;
import org.oasis_open.docs.wsn.b_2.Notify;
import com.corratech.bp.CalendarsSyncBP;
import com.corratech.jbi.utils.JBIUtils;
import com.opensuite.bind.services.eventmanagement.ProcessEventRequest;

/**
 * @author Aleksandr Kryzhak
 * 
 */
public class CalendarsSyncBPHandlerBean implements MessageExchangeListener {

    private static final Logger log = Logger.getLogger(CalendarsSyncBPHandlerBean.class);

    @Resource
    private DeliveryChannel channel;

    @Resource
    private ComponentContext context;

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
                System.out.println("CalendarsSyncBPHandlerBean.onMessageExchange(): Exchange has no IN message");
                log.error("CalendarsSyncBPHandlerBean.onMessageExchange(): Exchange has no IN message");
            }
            try {
                if (inMessage.getContent() == null) throw new MessagingException("CalendarsSyncBPHandlerBean.onMessageExchange(): Message has no content");
                Notify notify = (Notify) JBIUtils.getContext().createUnmarshaller().unmarshal(inMessage.getContent());
                Object msg = notify.getNotificationMessage().get(0).getMessage().getAny();
                if (msg instanceof ProcessEventRequest) {
                    CalendarsSyncBP bp = new CalendarsSyncBP((ProcessEventRequest) msg);
                    bp.run();
                }
            } catch (Exception e) {
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
}
