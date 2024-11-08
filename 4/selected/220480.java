package org.soda.dpws.handler;

import org.soda.dpws.exchange.OutMessage;
import org.soda.dpws.fault.DPWSFault;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.soap.handler.SoapBodyHandler;
import org.soda.dpws.transport.DefaultEndpoint;

/**
 * Reads in the message body using the service binding.
 * 
 */
public class DispatchServiceHandler extends AbstractHandler {

    /**
   * 
   */
    public DispatchServiceHandler() {
        super();
        setPhase(Phase.DISPATCH);
        after(SoapBodyHandler.class.getName());
    }

    public void invoke(DPWSContextImpl context) throws DPWSFault {
        Boolean b = (Boolean) context.getProperty(DefaultEndpoint.SERVICE_HANDLERS_REGISTERED);
        if ((b == null || b.equals(Boolean.FALSE)) && context.getService() != null) {
            context.getInPipeline().addHandlers(context.getService().getInHandlers());
        }
        if (context.getExchange().hasOutMessage()) {
            HandlerPipeline pipeline = new HandlerPipeline(context.getDpws().getOutPhases());
            if (context.getService() != null) pipeline.addHandlers(context.getService().getOutHandlers());
            pipeline.addHandlers(context.getDpws().getOutHandlers());
            OutMessage msg = context.getExchange().getOutMessage();
            pipeline.addHandlers(msg.getChannel().getTransport().getOutHandlers());
            context.setOutPipeline(pipeline);
        }
    }
}
