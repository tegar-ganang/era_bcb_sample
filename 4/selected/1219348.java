package soapdust.server;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import soapdust.ComposedValue;
import soapdust.FaultResponseException;
import soapdust.MalformedResponseException;
import soapdust.SoapMessageBuilder;
import soapdust.SoapMessageParser;
import soapdust.wsdl.Operation;
import soapdust.wsdl.WebServiceDescription;
import soapdust.wsdl.WsdlParser;

public class Servlet extends HttpServlet {

    private WebServiceDescription serviceDescription;

    private boolean wsdlSet;

    private URL wsdlUrl;

    public Servlet setWsdl(String wsdlUrl) throws MalformedURLException, SAXException, IOException {
        try {
            this.wsdlUrl = new URL(wsdlUrl);
            serviceDescription = new WsdlParser(this.wsdlUrl).parse();
            wsdlSet = true;
            return this;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("unexpected exception while parsing wsdl: " + e, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/xml");
        OutputStream out = resp.getOutputStream();
        InputStream in = wsdlUrl.openStream();
        try {
            byte[] buffer = new byte[1024];
            for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } finally {
            in.close();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getHeader("SOAPAction").replace("\"", "");
        ComposedValue params;
        try {
            params = new SoapMessageParser().parse(req.getInputStream());
        } catch (MalformedResponseException e) {
            throw new RuntimeException(e);
        }
        OperationHandler operationHandler = selectOperationHandler(action, params);
        try {
            try {
                Operation operation = operationHandler.operation;
                SoapDustHandler handler = operationHandler.handler;
                String operationName = operation.name;
                ComposedValue result = handler.handle(operationName, params);
                Document soapResponse = new SoapMessageBuilder(serviceDescription).buildResponse(operationName, result == null ? new ComposedValue() : result);
                sendSoapResponse(resp, soapResponse);
            } catch (FaultResponseException e) {
                sendFault(resp, e);
                return;
            }
        } catch (TransformerException e2) {
            throw new RuntimeException("Unexpected exception while sending soap response to client: " + e2, e2);
        }
    }

    private void sendFault(HttpServletResponse resp, FaultResponseException e) throws IOException, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        resp.setStatus(500);
        Document soapResponse;
        soapResponse = newDocument();
        Element body = createSoapBody(soapResponse);
        Element fault = soapResponse.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "Fault");
        Element faultCode = soapResponse.createElement("faultcode");
        Element faultString = soapResponse.createElement("faultstring");
        body.appendChild(fault);
        fault.appendChild(faultCode);
        fault.appendChild(faultString);
        faultCode.appendChild(soapResponse.createTextNode(e.fault.getStringValue("faultcode")));
        faultString.appendChild(soapResponse.createTextNode(e.fault.getStringValue("faultstring")));
        sendSoapResponse(resp, soapResponse);
        return;
    }

    private void sendSoapResponse(HttpServletResponse resp, Document document) throws IOException, TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        OutputStream out = resp.getOutputStream();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(document), new StreamResult(out));
            out.flush();
        } finally {
            out.close();
        }
    }

    private Document newDocument() {
        try {
            Document document;
            DocumentBuilder documentBuilder;
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = documentBuilder.newDocument();
            return document;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Map<String, OperationHandler>> handlers = new HashMap<String, Map<String, OperationHandler>>();

    public Servlet register(String operationName, final SoapDustHandler handler) {
        if (!wsdlSet) throw new IllegalStateException("you must set wsdl before registering handlers");
        final Operation operation = serviceDescription.findOperation(operationName);
        if (operation.isDocumentWrapped()) {
            Map<String, OperationHandler> map = handlers.get(operation.soapAction);
            if (map == null) {
                map = new HashMap<String, Servlet.OperationHandler>();
                handlers.put(operation.soapAction, map);
            }
            map.put(operation.name, new OperationHandler(operation, handler));
        } else {
            handlers.put(operation.soapAction, new HashMap<String, Servlet.OperationHandler>() {

                {
                    put("", new OperationHandler(operation, handler));
                }
            });
        }
        return this;
    }

    private OperationHandler selectOperationHandler(String action, ComposedValue params) {
        Map<String, OperationHandler> map = handlers.get(action);
        if (map != null) {
            Iterator<Entry<String, OperationHandler>> iterator = map.entrySet().iterator();
            Entry<String, OperationHandler> next = iterator.next();
            if (!iterator.hasNext()) {
                return next.getValue();
            } else {
                return map.get(params.getChildrenKeys().iterator().next());
            }
        }
        return new OperationHandler(new Operation(null, action), new DefaultHandler());
    }

    private class OperationHandler {

        Operation operation;

        SoapDustHandler handler;

        OperationHandler(Operation operation, SoapDustHandler handler) {
            this.operation = operation;
            this.handler = handler;
        }
    }

    private Element createSoapBody(Document document) {
        Element envelope = document.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "Envelope");
        Element header = document.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "Header");
        Element body = document.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "Body");
        document.appendChild(envelope);
        envelope.appendChild(header);
        envelope.appendChild(body);
        return body;
    }
}

class DefaultHandler implements SoapDustHandler {

    @Override
    public ComposedValue handle(String action, ComposedValue params) throws FaultResponseException {
        throw new FaultResponseException(new ComposedValue().put("faultcode", "UnsupportedOperation").put("faultstring", "Unsupported operation: " + action));
    }
}
