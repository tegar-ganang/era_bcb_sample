package com.corratech.opensuite;

import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;

/**
 * @org.apache.xbean.XBean element="endpoint"
 */
public class MyEndpoint extends ProviderEndpoint {

    public void validate() throws DeploymentException {
    }

    protected void processInOut(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            out.setContent(in.getContent());
            getChannel().send(exchange);
        }
    }
}
