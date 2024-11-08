package org.soda.dpws.addressing;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.jdom.Attribute;
import org.jdom.Element;
import org.soda.dpws.DPWSContext;
import org.soda.dpws.DPWSException;
import org.soda.dpws.exchange.AbstractMessage;
import org.soda.dpws.exchange.InMessage;
import org.soda.dpws.exchange.MessageExchange;
import org.soda.dpws.exchange.OutMessage;
import org.soda.dpws.fault.DPWSFault;
import org.soda.dpws.handler.AbstractHandler;
import org.soda.dpws.handler.Phase;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.registry.ServiceEndpoint;
import org.soda.dpws.registry.discovery.WSDConstants;
import org.soda.dpws.registry.discovery.handler.DiscoveryAddressingInHandler;
import org.soda.dpws.registry.discovery.handler.DuplicateInHandler;
import org.soda.dpws.registry.discovery.service.DiscoveryMessageType;
import org.soda.dpws.registry.discovery.service.DiscoveryService;
import org.soda.dpws.server.ServicePort;
import org.soda.dpws.transport.Channel;
import org.soda.dpws.transport.Transport;
import org.soda.dpws.transport.dead.DeadLetterTransport;
import org.soda.dpws.wsdl.OperationInfo;

/**
 *
 *
 */
public class AddressingInHandler extends AbstractHandler {

    /**
   *
   */
    public static final Object ADRESSING_HEADERS = "dpws-ws-adressing-headers";

    /**
   *
   */
    public static final Object ADRESSING_FACTORY = "dpws-ws-adressing-factory";

    private static final QName faultQName = new QName(WSAConstants.WSA_NAMESPACE_200408, "Fault");

    protected AddressingHeadersFactory200408 factory = new AddressingHeadersFactory200408();

    /**
   *
   */
    public AddressingInHandler() {
        super();
        before(DuplicateInHandler.class.getName());
        after(DiscoveryAddressingInHandler.class.getName());
        setPhase(Phase.PRE_DISPATCH);
    }

    public void invoke(DPWSContextImpl context) throws DPWSException {
        if (Boolean.TRUE.equals(context.getProperty(DPWSContext.CLIENT_MODE))) {
            invokeClient(context);
        } else {
            invokeServer(context);
        }
    }

    private DPWSFault makeFault(String msg, @SuppressWarnings("unused") QName code, String actionStr, QName subCode) {
        DPWSFault fault = new DPWSFault(msg, DPWSFault.SENDER);
        fault.setSubCode(subCode);
        Element detail = new Element("FaultDetail", WSAConstants.WSA_PREFIX, WSAConstants.WSA_NAMESPACE_200408);
        Element problem = new Element("ProblemHeaderQName", WSAConstants.WSA_PREFIX, WSAConstants.WSA_NAMESPACE_200408);
        detail.addContent(problem);
        if (actionStr == null) problem.addContent("wsa:Action"); else {
            Element action = new Element("Action", WSAConstants.WSA_PREFIX, WSAConstants.WSA_NAMESPACE_200408);
            problem.addContent(action);
            action.addContent(actionStr);
        }
        fault.setDetail(detail);
        return fault;
    }

    protected void invokeClient(DPWSContextImpl context) throws DPWSException {
        InMessage msg = context.getExchange().getInMessage();
        Element header = msg.getHeader();
        if (header == null || !factory.hasHeaders(header)) throw makeFault("A required header representing a Message Addressing Property is not present", DPWSFault.SENDER, null, new QName(WSAConstants.WSA_NAMESPACE_200408, "MessageInformationHeaderRequired"));
        AbstractAddressingHeaders headers = factory.createHeaders(header);
        msg.setProperty(ADRESSING_HEADERS, headers);
        msg.setProperty(ADRESSING_FACTORY, factory);
    }

    protected void invokeServer(DPWSContextImpl context) throws DPWSException {
        InMessage msg = context.getExchange().getInMessage();
        Element header = msg.getHeader();
        if (header == null || !factory.hasHeaders(header)) throw makeFault("A required header representing a Message Addressing Property is not present", DPWSFault.SENDER, null, new QName(WSAConstants.WSA_NAMESPACE_200408, "MessageInformationHeaderRequired"));
        try {
            AbstractAddressingHeaders headers = factory.createHeaders(header);
            msg.setProperty(ADRESSING_HEADERS, headers);
            msg.setProperty(ADRESSING_FACTORY, factory);
            EndpointReferenceElement replyTo = headers.replyTo;
            ServiceEndpoint service = getService(headers, context);
            if (context.getProperty(DiscoveryService.DISCOVERYSERVICE_KEY) != null) return;
            if (service != null) {
                context.setService(service);
            } else {
                service = context.getService();
            }
            OperationInfo aoi = null;
            if (service != null) aoi = getOperationByInAction(service, headers.action);
            if (aoi == null) {
                String serviceName = "null";
                if (service != null) {
                    serviceName = service.getId();
                }
                throw makeFault("The " + headers.action + " action cannot be processed at the receiver end [Service: " + serviceName + "]", DPWSFault.SENDER, headers.action, new QName(WSAConstants.WSA_NAMESPACE_200408, "ActionNotSupported"));
            }
            MessageExchange exchange = context.getExchange();
            exchange.setOperation(aoi);
            EndpointReferenceElement faultTo = headers.faultTo;
            OutMessage faultMsg = null;
            if (faultTo != null) faultMsg = processEPR(context, faultTo, aoi, headers, factory);
            exchange.setFaultMessage(faultMsg);
            OutMessage outMessage = null;
            if (replyTo != null) outMessage = processEPR(context, replyTo, aoi, headers, factory); else {
                sendEmptyResponse(context);
            }
            exchange.setOutMessage(outMessage);
        } catch (DPWSFault fault) {
            AbstractMessage faultMsg = context.getExchange().getFaultMessage();
            AbstractAddressingHeaders headers = (AbstractAddressingHeaders) faultMsg.getProperty(ADRESSING_HEADERS);
            if (headers == null) {
                headers = new AddressingHeaders(null, WSAConstants.WSA_200408_FAULT_ACTION);
                faultMsg.setProperty(ADRESSING_HEADERS, headers);
                faultMsg.setProperty(ADRESSING_FACTORY, factory);
            }
            throw fault;
        }
    }

    /**
   * @param factory
   * @param addr
   * @return
   */
    private boolean isNoneAddress(AddressingHeadersFactory200408 factory, String addr) {
        return factory.getNoneUri() != null && factory.getNoneUri().equals(addr);
    }

    /**
   * @param context
   * @param epr
   * @param aoi
   * @param inHeaders
   * @param factory
   * @return
   * @throws DPWSFault
   * @throws Exception
   */
    @SuppressWarnings("unchecked")
    protected OutMessage processEPR(DPWSContextImpl context, EndpointReferenceElement epr, OperationInfo aoi, AbstractAddressingHeaders inHeaders, AddressingHeadersFactory200408 factory) throws DPWSFault, DPWSException {
        String addr = epr.getAddress();
        OutMessage outMessage = null;
        String messId = inHeaders.messageID;
        boolean isFault = epr.getName().equals(WSAConstants.WSA_FAULT_TO);
        Transport t = null;
        if (addr == null) {
            throw new DPWSFault("Invalid ReplyTo address.", DPWSFault.SENDER);
        }
        if (addr.equals(factory.getAnonymousUri())) {
            outMessage = new OutMessage(Channel.BACKCHANNEL_URI);
            t = context.getExchange().getInMessage().getChannel().getTransport();
        } else if (isNoneAddress(factory, addr)) {
            t = new DeadLetterTransport();
            outMessage = new OutMessage(addr);
        } else {
            outMessage = new OutMessage(addr);
            if (!isFault) sendEmptyResponse(context);
            t = context.getDpws().getTransportManager().getTransportForUri(addr);
        }
        outMessage.setSoapVersion(context.getExchange().getInMessage().getSoapVersion());
        if (t == null) {
            throw new DPWSFault("URL was not recognized: " + addr, DPWSFault.SENDER);
        }
        outMessage.setChannel(t.createChannel());
        AddressingHeaders headers = new AddressingHeaders(null, null);
        if (!isFault) {
            headers.to = addr;
            headers.action = aoi.getOutAction();
            headers.relatesTo = messId;
        } else {
            headers.action = WSAConstants.WSA_200408_FAULT_ACTION;
            headers.relatesTo = messId;
            headers.relationshipType = faultQName;
        }
        Element refParam = epr.getReferenceParametersElement();
        if (refParam != null) {
            List<Element> refs = refParam.cloneContent();
            List<Element> params = new ArrayList<Element>();
            for (int i = 0; i < refs.size(); i++) {
                if (refs.get(i) != null) {
                    Element e = refs.get(i);
                    e.setAttribute(new Attribute(WSAConstants.WSA_IS_REF_PARAMETER, "true", epr.getNamespace()));
                    params.add(e);
                }
            }
            headers.referenceParameters = params;
        }
        outMessage.setProperty(ADRESSING_HEADERS, headers);
        outMessage.setProperty(ADRESSING_FACTORY, factory);
        return outMessage;
    }

    private void sendEmptyResponse(DPWSContextImpl context) throws DPWSException {
        context.getExchange().getInMessage().getChannel().sendEmptyResponse(context);
    }

    protected OperationInfo getOperationByInAction(ServiceEndpoint service, String actionName) {
        if (service == null) return null;
        OperationInfo aoi = service.getAddressingOperationInfoByInAction(actionName);
        if (aoi != null) return aoi;
        if (!actionName.equals("*")) return getOperationByInAction(service, "*");
        return null;
    }

    protected ServiceEndpoint getService(AbstractAddressingHeaders headers, DPWSContextImpl context) {
        String serviceName = headers.to;
        if (WSDConstants.WSD_URN.equals(serviceName) || (WSAConstants.WSA_200408_ANONYMOUS_URI.equals(serviceName) && (DiscoveryMessageType.PROBE_MATCH.action.equals(headers.action) || DiscoveryMessageType.RESOLVE_MATCH.action.equals(headers.action)))) {
            serviceName = context.getExchange().getInChannel().getUri();
        }
        if (serviceName != null) {
            int i = serviceName.lastIndexOf('/');
            if (i >= 0) serviceName = serviceName.substring(i + 1);
        }
        if (serviceName == null) {
            return null;
        }
        if (serviceName.startsWith("urn:uuid:")) serviceName = serviceName.substring(9);
        ServicePort servicePort = context.getDpws().getPortListener().getServicePort(serviceName);
        if (servicePort == null) return null;
        context.setProperty(DPWSContext.SERVICE_PORT, servicePort);
        return servicePort.getServiceEndpoint();
    }
}
