package org.atricore.idbus.kernel.main.mediation.camel;

import org.apache.camel.*;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.Map;

/**
 *
 * @author <a href="mailto:gbrigand@josso.org">Gianluca Brigandi</a>
 * @version $Id: AbstractCamelEndpoint.java 1359 2009-07-19 16:57:57Z sgonzalez $
 */
public abstract class AbstractCamelEndpoint<E extends Exchange> extends DefaultEndpoint<E> {

    private static final Log logger = LogFactory.getLog(AbstractCamelEndpoint.class);

    protected String endpointRef;

    protected String channelRef;

    protected String action;

    protected boolean isResponse;

    protected AbstractCamelEndpoint(String endpointURI, Component component, Map parameters) throws Exception {
        super(endpointURI, component);
        CamelContext ctx = component.getCamelContext();
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isSingleton() {
        return false;
    }

    public String getChannelRef() {
        return channelRef;
    }

    public void setChannelRef(String channelRef) {
        this.channelRef = channelRef;
    }

    public String getEndpointRef() {
        return endpointRef;
    }

    public void setEndpointRef(String endpointRef) {
        this.endpointRef = endpointRef;
    }

    public boolean isResponse() {
        return isResponse;
    }

    public void setResponse(boolean response) {
        isResponse = response;
    }

    public String getAction() {
        if (action == null) {
            return this.getClass().getSimpleName().replace("IdentityMediationEndpoint", "");
        }
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
