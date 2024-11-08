package org.soda.dpws.service.binding;

import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Element;
import org.jdom.Namespace;
import org.soda.dpws.DPWSContext;
import org.soda.dpws.DPWSException;
import org.soda.dpws.exchange.AbstractMessage;
import org.soda.dpws.exchange.InMessage;
import org.soda.dpws.exchange.MessageExchange;
import org.soda.dpws.exchange.OutMessage;
import org.soda.dpws.fault.DPWSFault;
import org.soda.dpws.handler.AbstractHandler;
import org.soda.dpws.handler.DefaultFaultHandler;
import org.soda.dpws.handler.HandlerPipeline;
import org.soda.dpws.handler.Phase;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.registry.ServiceEndpoint;
import org.soda.dpws.service.MessageInfo;
import org.soda.dpws.service.MessagePartContainer;
import org.soda.dpws.service.MessagePartInfo;
import org.soda.dpws.util.stax.JDOMStreamReader;
import org.soda.dpws.util.stax.JDOMStreamWriter;
import org.soda.dpws.wsdl.OperationInfo;

/**
 * 
 * 
 */
public class ServiceInvocationHandler extends AbstractHandler {

    private Log log = LogFactory.getLog(ServiceInvocationHandler.class);

    /**
   * 
   */
    public ServiceInvocationHandler() {
        super();
        setPhase(Phase.SERVICE);
    }

    public void invoke(final DPWSContextImpl context) throws DPWSException {
        try {
            OperationInfo aoi = context.getExchange().getOperation();
            final ServiceEndpoint service = context.getService();
            Runnable runnable = new ServiceRunner() {

                public void run() {
                    try {
                        sendMessage(context, service);
                    } catch (Exception e) {
                        context.setProperty(DefaultFaultHandler.EXCEPTION, e);
                        DPWSFault fault;
                        if (e instanceof DPWSFault) fault = (DPWSFault) e; else fault = DPWSFault.createFault(e);
                        try {
                            if (context.getOutPipeline() == null) {
                                HandlerPipeline pipeline = new HandlerPipeline(context.getDpws().getOutPhases());
                                pipeline.addHandlers(context.getService().getOutHandlers());
                                pipeline.addHandlers(context.getDpws().getOutHandlers());
                                OutMessage msg = context.getExchange().getOutMessage();
                                if (msg != null) pipeline.addHandlers(msg.getChannel().getTransport().getOutHandlers());
                                context.setOutPipeline(pipeline);
                            }
                            context.getOutPipeline().handleFault(fault, context);
                            context.getFaultHandler().invoke(context);
                        } catch (Exception e1) {
                            log.warn("Error invoking fault handler.", e1);
                        }
                    }
                }
            };
            ServiceEndpoint service2 = context.getService();
            execute(runnable, service2, aoi);
        } catch (Exception e) {
            log.warn("Error invoking service.", e);
            throw new DPWSFault("Error invoking service" + (e.getMessage() != null ? ": " + e.getMessage() : "."), e, DPWSFault.SENDER);
        }
    }

    /**
   * Run the Runnable which executes our service.
   * 
   * @param runnable
   * @param service
   * @param operation
   */
    protected void execute(Runnable runnable, ServiceEndpoint service, OperationInfo aoi) {
        if (!aoi.isAsync()) {
            runnable.run();
        } else {
            Thread opthread = new Thread(runnable);
            opthread.start();
        }
    }

    protected void sendMessage(DPWSContextImpl context, org.soda.dpws.registry.ServiceEndpoint service) throws Exception {
        MessageExchange exchange = context.getExchange();
        OperationInfo oI = exchange.getOperation();
        InMessage inMessage = exchange.getInMessage();
        Object inMsgBody = inMessage.getBody();
        if (exchange.hasOutMessage()) {
            Object[] value = service.invoke(context, oI, inMsgBody);
            OutMessage outMsg = exchange.getOutMessage();
            writeHeaders(context);
            context.setCurrentMessage(outMsg);
            outMsg.setBody(value);
            outMsg.setSerializer(context.getBinding().getSerializer());
            try {
                context.getOutPipeline().invoke(context);
            } catch (Exception e) {
                log.error("SendMessage failed!", e);
                DPWSFault fault = DPWSFault.createFault(e);
                context.getOutPipeline().handleFault(fault, context);
                throw fault;
            }
        } else {
            service.invoke(context, oI, inMsgBody);
        }
    }

    /**
   * @param context
   * @throws DPWSFault
   * @throws XMLStreamException
   */
    public static void writeHeaders(DPWSContextImpl context) throws DPWSFault, XMLStreamException {
        MessageInfo msgInfo = AbstractBinding.getOutgoingMessageInfo(context);
        MessagePartContainer headers = context.getBinding().getHeaders(msgInfo);
        if (headers.size() == 0) return;
        Object[] body = (Object[]) context.getCurrentMessage().getBody();
        JDOMStreamWriter writer = new JDOMStreamWriter(context.getExchange().getOutMessage().getOrCreateHeader());
        for (Iterator<MessagePartInfo> itr = headers.getMessageParts().iterator(); itr.hasNext(); ) {
            MessagePartInfo part = itr.next();
            AbstractBinding.writeParameter(writer, context, body[part.getIndex()], part, part.getName().getNamespaceURI());
        }
    }

    /**
   * @param context
   * @param headerMsg
   * @param paramArray
   * @throws DPWSFault
   */
    public static void readHeaders(final DPWSContextImpl context, MessagePartContainer headerMsg, final Object[] paramArray) throws DPWSFault {
        final List<MessagePartInfo> headerInfos = headerMsg.getMessageParts();
        for (Iterator<MessagePartInfo> itr = headerInfos.iterator(); itr.hasNext(); ) {
            MessagePartInfo header = itr.next();
            BindingProvider bindingProvider = (BindingProvider) context.getProperty(DPWSContext.BINDING_PROVIDER);
            XMLStreamReader headerReader = getXMLStreamReader(context.getExchange().getInMessage(), header);
            if (headerReader == null) continue;
            Object headerVal = bindingProvider.readParameter(header, headerReader, context);
            if (paramArray[header.getIndex()] == null) {
                paramArray[header.getIndex()] = headerVal;
            }
        }
    }

    private static XMLStreamReader getXMLStreamReader(AbstractMessage msg, MessagePartInfo header) {
        if (msg.getHeader() == null) return null;
        QName name = header.getName();
        Element el = msg.getHeader().getChild(name.getLocalPart(), Namespace.getNamespace(name.getNamespaceURI()));
        if (el == null) return null;
        JDOMStreamReader reader = new JDOMStreamReader(el);
        try {
            reader.next();
        } catch (XMLStreamException e) {
        }
        return reader;
    }
}
