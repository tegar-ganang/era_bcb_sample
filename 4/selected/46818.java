package org.soda.dpws.handler;

import org.soda.dpws.DPWSContext;
import org.soda.dpws.DPWSException;
import org.soda.dpws.fault.DPWSFault;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.server.ServicePort;
import org.soda.dpws.service.Binding;
import org.soda.dpws.soap.handler.ReadHeadersHandler;
import org.soda.dpws.transport.Channel;

/**
 * Finds the appropriate binding to use when invoking a service. This is
 * delegated to the transport via the findBinding method.
 * 
 */
public class LocateBindingHandler extends AbstractHandler {

    /**
   * 
   */
    public LocateBindingHandler() {
        super();
        setPhase(Phase.DISPATCH);
        after(ReadHeadersHandler.class.getName());
    }

    public void invoke(DPWSContextImpl context) throws DPWSException {
        if (context.getBinding() != null) return;
        Channel c = context.getExchange().getInMessage().getChannel();
        ServicePort servicePort = (ServicePort) context.getProperty(DPWSContext.SERVICE_PORT);
        if (servicePort == null) {
            throw new DPWSFault("Could not service port.", DPWSFault.SENDER);
        }
        Binding binding = c.getTransport().findBinding(context, servicePort);
        if (binding == null) {
            throw new DPWSFault("Could not find an appropriate Transport Binding to invoke.", DPWSFault.SENDER);
        }
        context.setBinding(binding);
    }
}
