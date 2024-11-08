package com.corratech.opensuite.resolver;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;

/**
 * @org.apache.xbean.XBean element="endpoint"
 */
public class EREndpoint extends ProviderEndpoint implements ExchangeProcessor {

    public void validate() throws DeploymentException {
    }

    protected void processInOut(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            out.setContent(in.getContent());
            getChannel().send(exchange);
        }
    }

    public void activate() throws JBIException {
        ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
        ctx.activateEndpoint(service, endpoint);
    }
}
