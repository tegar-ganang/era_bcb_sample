package org.xactor.ws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jboss.logging.Logger;
import org.xactor.ws.addressing.WSAddrUtil;
import org.xactor.ws.addressing.element.EndpointReference;

/**
 * Base class for the WS-Coordination and WS-AtomicTransaction endpoints.
 * 
 * @author <a href="mailto:ivanneto@gmail.com">Ivan Neto</a>
 */
public abstract class ServletEndpoint extends HttpServlet implements Constants {

    /** The SOAP connection. */
    protected static SOAPConnection soapConnection = SOAPConnection.getInstance();

    /** The StAX XML input factory. */
    protected static XMLInputFactory xmlInputFactory = XMLInputFactorySingleton.getSingleton();

    /** Logging. */
    protected Logger log = Logger.getLogger(getClass());

    /** Is trace enbled? */
    protected boolean isTraceEnabled = log.isTraceEnabled();

    /** The charset encoding used in the SOAP messages. */
    private static String charsetEncoding = System.getProperty(SOAPMessage.CHARACTER_SET_ENCODING, "UTF-8");

    /**
    * The initial size of the <code>ByteArrayOutputStream</code> that will hold the request
    * messages.
    */
    private static final int INITIAL_BUFFER_SIZE = 4096;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            SOAPRequest request = null;
            SOAPResponse response = null;
            try {
                String reqMessage = getRequestMessage(req.getInputStream());
                String requestURI = req.getRequestURI();
                String targetEndpoint = req.getPathInfo().substring(1);
                if (isTraceEnabled) {
                    String prettySoapMessage = DOMWriter.printNode(DOMUtils.parse(reqMessage), true);
                    log.trace("Incoming SOAPMessage [" + requestURI + "]\n" + prettySoapMessage);
                }
                request = new SOAPRequest(reqMessage, requestURI, targetEndpoint);
                request.initAndverifyRequest();
                response = processSOAPRequest(request);
            } catch (Exception e) {
                response = processException(e, request);
            }
            if (response.isOneWayMessage()) {
                if (response.getTargetEndpoint() != null) {
                    soapConnection.callOneWay(response.getMessage(), response.getTargetEndpoint());
                } else if (response.isFaultMessage()) {
                    log.debug("Error processing one way request", response.getException());
                }
                res.setStatus(HttpServletResponse.SC_ACCEPTED);
            } else {
                if (response.isFaultMessage()) {
                    res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                } else {
                    res.setStatus(HttpServletResponse.SC_OK);
                }
                if (isTraceEnabled) {
                    String prettySoapMessage = DOMWriter.printNode(DOMUtils.parse(response.getMessage()), true);
                    log.trace("Outgoing SOAPMessage\n" + prettySoapMessage);
                }
                writeResponseMessage(res.getOutputStream(), response.getMessage());
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        showHttpGetNotSupportedPage(res);
    }

    protected abstract SOAPResponse processSOAPRequest(SOAPRequest request);

    protected abstract boolean isValidMessage(String path, QName elementName);

    protected abstract boolean isOneWayEndpoint(String path);

    protected abstract Object getSoapBodyElementJavaRepresentation(XMLStreamReader parser) throws XMLStreamException;

    protected abstract String getSOAPFaultAction();

    protected void throwMissingHeaderElementSOAPFaultRuntimeException(QName qname) {
        FaultInfo faultInfo = WSCoor.Faults.INVALID_PARAMETERS;
        String exceptionMessage = "Missing '" + qname + "' header element in message.";
        IllegalArgumentException e = new IllegalArgumentException(exceptionMessage);
        String exceptionName = IllegalArgumentException.class.getName();
        throw new SOAPFaultRuntimeException(faultInfo, e, exceptionName);
    }

    private String getRequestMessage(InputStream in) throws IOException, UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
        IOUtils.copyStream(baos, in);
        String reqMessage = new String(baos.toByteArray(), charsetEncoding);
        return reqMessage;
    }

    private void writeResponseMessage(ServletOutputStream out, String resMessage) throws IOException, UnsupportedEncodingException {
        out.write(resMessage.getBytes(charsetEncoding));
        out.close();
    }

    private void showHttpGetNotSupportedPage(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        res.setContentType("text/plain");
        Writer out = res.getWriter();
        out.write("HTTP GET not supported");
        out.flush();
        out.close();
    }

    private void handleException(Exception e) throws ServletException {
        log.error("Error processing Web service request", e);
        throw new ServletException(e);
    }

    private SOAPResponse processException(Exception e, SOAPRequest request) {
        SOAPResponse response = new SOAPResponse(request.isOneWayMessage());
        XMLFragmentMetadata headerMetadata = null;
        if (request.isOneWayMessage()) {
            SOAPHeaderMetadataInbound inboundHeaderMetadata = request.getHeaderMetadata();
            if (inboundHeaderMetadata != null) {
                EndpointReference wsaFaultTo = inboundHeaderMetadata.getWsaFaultTo();
                EndpointReference wsaReplyTo = inboundHeaderMetadata.getWsaReplyTo();
                EndpointReference targetEndpoint = wsaFaultTo != null ? wsaFaultTo : wsaReplyTo;
                if (targetEndpoint != null) {
                    headerMetadata = targetEndpoint.getHeaderMetadata();
                    String messageId = UUIDFactory.newMessageId();
                    String relatesTo = inboundHeaderMetadata.getWsaMessageID();
                    String targetAddress = targetEndpoint.getAddress().getValue();
                    String action = getSOAPFaultAction();
                    XMLFragmentMetadata wsAddrMetadata = WSAddrUtil.getHeaderMetadata(messageId, relatesTo, targetAddress, action);
                    headerMetadata = (headerMetadata == null ? wsAddrMetadata : headerMetadata.merge(wsAddrMetadata));
                    response.setTargetEndpoint(targetEndpoint);
                }
            }
        }
        SOAPFaultRuntimeException ex = null;
        if (e instanceof SOAPFaultRuntimeException) ex = (SOAPFaultRuntimeException) e; else ex = new SOAPFaultRuntimeException(e, e.getClass().getName());
        response.setMessage(ex.toString(headerMetadata));
        response.setFaultMessage(true);
        response.setException(e);
        return response;
    }

    /** This class is used to hold the SOAP request message and related information. */
    protected class SOAPRequest {

        /** The SOAP request message. */
        private String message;

        /** The servlet context path. */
        private String requestURI;

        /** The servlet path the SOAP request message is targeted to. */
        private String targetEndpoint;

        /** Whether or not the SOAP request message is one way. */
        private boolean isOneWayMessage;

        /** SOAP resquest message header metadata. */
        private SOAPHeaderMetadataInbound headerMetadata;

        /** The SOAP body element name. */
        private QName soapBodyElementName;

        /**
       * The Java representation of the SOAP body element, or null if there's no such
       * representation.
       */
        private Object soapBodyElement;

        public SOAPRequest(String reqMessage, String requestURI, String targetEndpoint) {
            this.message = reqMessage;
            this.requestURI = requestURI;
            this.targetEndpoint = targetEndpoint;
        }

        public void initAndverifyRequest() throws XMLStreamException {
            if (isValidMessage(targetEndpoint, null) == false) {
                FaultInfo faultInfo = WSCoor.Faults.INVALID_PARAMETERS;
                String exceptionMessage = "Invalid endpoint address: " + requestURI;
                IllegalArgumentException e = new IllegalArgumentException(exceptionMessage);
                String exceptionName = IllegalArgumentException.class.getName();
                throw new SOAPFaultRuntimeException(faultInfo, e, exceptionName);
            }
            this.isOneWayMessage = isOneWayEndpoint(targetEndpoint);
            XMLStreamReader parser = xmlInputFactory.createXMLStreamReader(new StringReader(message));
            try {
                processSoapHeader(parser);
                processSoapBody(parser);
            } finally {
                parser.close();
            }
        }

        private void processSoapHeader(XMLStreamReader parser) throws XMLStreamException {
            for (int ev = parser.next(); ev != XMLStreamConstants.END_DOCUMENT; ev = parser.next()) {
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    QName qname = parser.getName();
                    if (SOAP11.HEADER.equals(qname)) {
                        headerMetadata = new SOAPHeaderMetadataInbound(parser);
                        break;
                    }
                }
            }
        }

        private void processSoapBody(XMLStreamReader parser) throws XMLStreamException {
            boolean inBody = false;
            for (int ev = parser.next(); ev != XMLStreamConstants.END_DOCUMENT; ev = parser.next()) {
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    QName qname = parser.getName();
                    if (SOAP11.BODY.equals(qname)) {
                        inBody = true;
                    } else if (inBody) {
                        soapBodyElementName = parser.getName();
                        if (isValidMessage(targetEndpoint, soapBodyElementName) == false) {
                            FaultInfo faultInfo = WSCoor.Faults.INVALID_PARAMETERS;
                            String exceptionMessage = "Invalid '" + soapBodyElementName + "' message to endpoint: " + requestURI;
                            IllegalArgumentException e = new IllegalArgumentException(exceptionMessage);
                            String exceptionName = IllegalArgumentException.class.getName();
                            throw new SOAPFaultRuntimeException(faultInfo, e, exceptionName);
                        }
                        soapBodyElement = getSoapBodyElementJavaRepresentation(parser);
                        break;
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    QName qname = parser.getName();
                    if (SOAP11.BODY.equals(qname)) break;
                }
            }
        }

        public SOAPHeaderMetadataInbound getHeaderMetadata() {
            return headerMetadata;
        }

        public void setHeaderMetadata(SOAPHeaderMetadataInbound headerMetadata) {
            this.headerMetadata = headerMetadata;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getRequestURI() {
            return requestURI;
        }

        public void setRequestURI(String contextPath) {
            this.requestURI = contextPath;
        }

        public String getTargetEndpoint() {
            return targetEndpoint;
        }

        public void setTargetEndpoint(String path) {
            this.targetEndpoint = path;
        }

        public boolean isOneWayMessage() {
            return isOneWayMessage;
        }

        public void setOneWayMessage(boolean isOneWayMessage) {
            this.isOneWayMessage = isOneWayMessage;
        }

        public Object getSoapBodyElement() {
            return soapBodyElement;
        }

        public void setSoapBodyElement(Object soapBodyElement) {
            this.soapBodyElement = soapBodyElement;
        }

        public QName getSoapBodyElementName() {
            return soapBodyElementName;
        }

        public void setSoapBodyElementName(QName soapBodyElementName) {
            this.soapBodyElementName = soapBodyElementName;
        }
    }

    /** This class is used to hold the SOAP response and related information. */
    protected static class SOAPResponse {

        /** The SOAP response message. */
        private String message;

        /** Whether the response message is a fault or not. */
        private boolean isFaultMessage;

        /** Whether or not the response message is one way. */
        private boolean isOneWayMessage;

        /** The target address (used for one-way messages only, null for RPC messages). */
        private EndpointReference targetEndpoint;

        /**
       * If the response message is a SOAP fault, this field contains the exception that generated
       * the SOAP fault. If the response message isn't a SOAP fault or the fault wasn't generated by
       * an exception, this field contains null.
       */
        private Exception exception;

        public SOAPResponse() {
        }

        public SOAPResponse(boolean isOneWayMessage) {
            this.isOneWayMessage = isOneWayMessage;
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public boolean isFaultMessage() {
            return isFaultMessage;
        }

        public void setFaultMessage(boolean isFaultMessage) {
            this.isFaultMessage = isFaultMessage;
        }

        public boolean isOneWayMessage() {
            return isOneWayMessage;
        }

        public void setOneWayMessage(boolean isOneWayMessage) {
            this.isOneWayMessage = isOneWayMessage;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public EndpointReference getTargetEndpoint() {
            return targetEndpoint;
        }

        public void setTargetEndpoint(EndpointReference targetEndpoint) {
            this.targetEndpoint = targetEndpoint;
        }
    }

    /** This class represents the header of an incoming SOAP message. */
    protected static class SOAPHeaderMetadataInbound {

        /** The ID of the coordinated activity. */
        private String coordinatedActivityId;

        /** The ID of the participant. */
        private String participantId;

        /** The WS-Addressing wsa:MessageID header element. */
        private String wsaMessageID;

        /** The WS-Addressing wsa:RelatesTo header element. */
        private String wsaRelatesTo;

        /** The WS-Addressing wsa:To header element. */
        private String wsaTo;

        /** The WS-Addressing wsa:Action header element. */
        private String wsaAction;

        /** The WS-Addressing wsa:ReplyTo header element. */
        private EndpointReference wsaReplyTo;

        /** The WS-Addressing wsa:FaultTo header element. */
        private EndpointReference wsaFaultTo;

        public SOAPHeaderMetadataInbound(XMLStreamReader parser) throws XMLStreamException {
            for (int ev = parser.next(); ev != XMLStreamConstants.END_DOCUMENT; ev = parser.next()) {
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    QName qname = parser.getName();
                    if (WSCoor.Elements.COORDINATED_ACTIVITY_ID.equals(qname)) coordinatedActivityId = parser.getElementText();
                    if (WSCoor.Elements.PARTICIPANT_ID.equals(qname)) participantId = parser.getElementText(); else if (WSAddr.Elements.MESSAGE_ID.equals(qname)) wsaMessageID = parser.getElementText(); else if (WSAddr.Elements.RELATES_TO.equals(qname)) wsaRelatesTo = parser.getElementText(); else if (WSAddr.Elements.TO.equals(qname)) wsaTo = parser.getElementText(); else if (WSAddr.Elements.ACTION.equals(qname)) wsaAction = parser.getElementText(); else if (WSAddr.Elements.REPLY_TO.equals(qname)) wsaReplyTo = new EndpointReference(parser); else if (WSAddr.Elements.FAULT_TO.equals(qname)) wsaFaultTo = new EndpointReference(parser);
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    QName qname = parser.getName();
                    if (SOAP11.HEADER.equals(qname)) break;
                }
            }
        }

        public String getCoordinatedActivityId() {
            return coordinatedActivityId;
        }

        public String getParticipantId() {
            return participantId;
        }

        public String getWsaAction() {
            return wsaAction;
        }

        public EndpointReference getWsaFaultTo() {
            return wsaFaultTo;
        }

        public String getWsaMessageID() {
            return wsaMessageID;
        }

        public String getWsaRelatesTo() {
            return wsaRelatesTo;
        }

        public EndpointReference getWsaReplyTo() {
            return wsaReplyTo;
        }

        public String getWsaTo() {
            return wsaTo;
        }
    }
}
