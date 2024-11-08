package org.soda.dpws.transport;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.soda.dpws.exchange.InMessage;
import org.soda.dpws.exchange.MessageExchange;
import org.soda.dpws.fault.DPWSFault;
import org.soda.dpws.handler.DefaultFaultHandler;
import org.soda.dpws.handler.Handler;
import org.soda.dpws.handler.HandlerPipeline;
import org.soda.dpws.internal.DPWSContextImpl;

/**
 * A <code>ChannelEndpoint</code> which executes the in pipeline on the
 * service and starts a <code>MessageExchange</code>.
 * 
 */
public class DefaultEndpoint implements ChannelEndpoint {

    /**
   * 
   */
    public static final String SERVICE_HANDLERS_REGISTERED = "service.handlers.registered";

    private Log log = LogFactory.getLog(DefaultEndpoint.class);

    /**
   * 
   */
    public DefaultEndpoint() {
    }

    public void onReceive(DPWSContextImpl context, InMessage msg) {
        if (context.getExchange() == null) {
            MessageExchange exchange = new MessageExchange(context);
            exchange.setInMessage(msg);
            context.setCurrentMessage(msg);
        }
        HandlerPipeline pipeline = new HandlerPipeline(context.getDpws().getInPhases());
        pipeline.addHandlers(context.getDpws().getInHandlers());
        pipeline.addHandlers(msg.getChannel().getTransport().getInHandlers());
        if (context.getService() != null) {
            pipeline.addHandlers(context.getService().getInHandlers());
            context.setProperty(SERVICE_HANDLERS_REGISTERED, Boolean.TRUE);
        }
        context.setInPipeline(pipeline);
        if (context.getFaultHandler() == null) context.setFaultHandler(createFaultHandler());
        try {
            pipeline.invoke(context);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                log.error("onReceive", e);
            }
            context.setProperty(DefaultFaultHandler.EXCEPTION, e);
            try {
                context.getFaultHandler().invoke(context);
            } catch (Exception e1) {
                log.warn("Error invoking fault handler.", e1);
            }
        }
    }

    protected Handler createFaultHandler() {
        return new DefaultFaultHandler();
    }

    /**
   * @param message
   * @param context
   * @throws DPWSFault
   */
    public void finishReadingMessage(InMessage message, DPWSContextImpl context) throws DPWSFault {
        XMLStreamReader reader = message.getXMLStreamReader();
        try {
            int event = reader.getEventType();
            while (event != XMLStreamReader.END_DOCUMENT && reader.hasNext()) event = reader.next();
        } catch (XMLStreamException e) {
            log.warn("Couldn't parse to end of message.", e);
        }
    }
}
