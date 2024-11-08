package org.ogce.schemas.gfac.wsdl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;
import java.util.Hashtable;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Logger;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.OperationType;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.UnknownExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import org.ogce.schemas.gfac.beans.utils.GFacSchemaException;
import org.ogce.schemas.gfac.documents.MethodType;
import org.ogce.schemas.gfac.documents.PortTypeType;
import org.ogce.schemas.gfac.documents.ServiceMapType;
import org.ogce.schemas.gfac.utils.GFacConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.builder.XmlInfosetBuilder;
import xsul.XmlConstants;
import com.ibm.wsdl.BindingInputImpl;
import com.ibm.wsdl.BindingOutputImpl;
import com.ibm.wsdl.InputImpl;
import com.ibm.wsdl.MessageImpl;
import com.ibm.wsdl.OperationImpl;
import com.ibm.wsdl.OutputImpl;
import com.ibm.wsdl.PartImpl;
import com.ibm.wsdl.PortImpl;
import com.ibm.wsdl.PortTypeImpl;
import com.ibm.wsdl.ServiceImpl;
import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;
import com.ibm.wsdl.extensions.soap.SOAPBindingImpl;
import com.ibm.wsdl.extensions.soap.SOAPBodyImpl;
import com.ibm.wsdl.extensions.soap.SOAPOperationImpl;

public class WSDLGenerator implements WSDLConstants {

    public static final String WSA_PREFIX = "wsa";

    public static final String CROSSCUT_PREFIX = "crosscutns";

    private static Logger logger = Logger.getLogger(GFacConstants.LOGGER_NAME);

    private static final XmlInfosetBuilder builder = XmlConstants.BUILDER;

    public Hashtable generateWSDL(String serviceLocation, QName wsdlQName_, String security, ServiceMapType serviceMap, boolean abstractWSDL) throws GFacSchemaException {
        Hashtable table = new Hashtable();
        QName wsdlQName = null;
        String wsdlString = null;
        String serviceName = serviceMap.getService().getServiceName().getStringValue();
        String nameSpaceURI = serviceMap.getService().getServiceName().getTargetNamespace();
        QName serviceQName = new QName(nameSpaceURI, serviceName);
        if (wsdlQName_ != null && !abstractWSDL) {
            wsdlQName = wsdlQName_;
        } else if (wsdlQName_ == null && !abstractWSDL) {
            String date = (new Date()).toString();
            date = date.replaceAll(" ", "_");
            date = date.replaceAll(":", "_");
            Random rand = new Random();
            int rdint = rand.nextInt(1000000);
            wsdlQName = new QName(nameSpaceURI, serviceName + "_" + date + "_" + rdint);
        }
        PortTypeType[] portTypes = serviceMap.getPortTypeArray();
        MethodType[] methods = portTypes[0].getMethodArray();
        QName portTypeName = serviceQName;
        String portName = portTypeName.getLocalPart();
        try {
            WSDLFactory fac = WSDLFactory.newInstance();
            WSDLWriter wsWriter = fac.newWSDLWriter();
            Definition def = fac.newDefinition();
            String typens = nameSpaceURI + "/" + portName + "/" + "xsd";
            String globalTypens = nameSpaceURI + "/" + "xsd";
            if (abstractWSDL) {
                def.setQName(serviceQName);
                logger.info("Service QName set to = " + serviceQName);
            } else {
                def.setQName(wsdlQName);
                logger.info("WSDL QName set to = " + wsdlQName);
            }
            def.setTargetNamespace(nameSpaceURI);
            def.addNamespace(WSDLNS, nameSpaceURI);
            def.addNamespace(TYPENS, typens);
            def.addNamespace(GLOBAL_TYPENS, globalTypens);
            def.addNamespace(SOAP, SOAP_NAMESPACE);
            def.addNamespace(XSD, XSD_NAMESPACE);
            def.addNamespace(CROSSCUT_PREFIX, "http://lead.extreme.indiana.edu/namespaces/2006/lead-crosscut-parameters/");
            def.addNamespace(WSA_PREFIX, "http://www.w3.org/2005/08/addressing");
            if (GFacConstants.TRANSPORT_LEVEL.equals(security) || GFacConstants.MESSAGE_SIGNATURE.equals(security)) {
                def.addNamespace(WSA_PREFIX, WSA_NAMESPACE);
                def.addNamespace("wsp", WSP_NAMESPACE);
                def.addNamespace("wsu", WSU_NAMESPACE);
                def.addNamespace("wspe", WSPE_NAMESPACE);
                def.addNamespace("sp", SP_NAMESPACE);
                def.addNamespace("wss10", WSS10_NAMESPACE);
                def.addNamespace("sp", SP_NAMESPACE);
                def.addNamespace("wst", WST_NAMESPACE);
            }
            javax.xml.parsers.DocumentBuilderFactory domfactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = null;
            try {
                builder = domfactory.newDocumentBuilder();
            } catch (javax.xml.parsers.ParserConfigurationException e) {
                throw new GFacSchemaException("Parser configuration exception: " + e.getMessage());
            }
            DOMImplementation dImpl = builder.getDOMImplementation();
            String policyID = portName + "_Policy";
            String inputPolicyID = portName + "_operationPolicy";
            UnknownExtensibilityElement serviceLevelPolicRef = null;
            UnknownExtensibilityElement opLevelPolicRef = null;
            String namespace = GFacConstants.GFAC_NAMESPACE;
            Document doc = dImpl.createDocument(namespace, "factoryServices", null);
            String description = serviceMap.getService().getServiceDescription();
            if (description != null) {
                Element documentation = doc.createElementNS("http://schemas.xmlsoap.org/wsdl/", "wsdl:documentation");
                documentation.appendChild(doc.createTextNode(description));
                def.setDocumentationElement(documentation);
            }
            if (GFacConstants.TRANSPORT_LEVEL.equals(security)) {
                def.addExtensibilityElement(createTransportLevelPolicy(dImpl, policyID));
                serviceLevelPolicRef = createWSPolicyRef(dImpl, policyID);
            } else if (GFacConstants.MESSAGE_SIGNATURE.equals(security)) {
                def.addExtensibilityElement(WSPolicyGenerator.createServiceLevelPolicy(dImpl, policyID));
                def.addExtensibilityElement(WSPolicyGenerator.createOperationLevelPolicy(dImpl, "inputPolicy", inputPolicyID));
                serviceLevelPolicRef = createWSPolicyRef(dImpl, policyID);
                opLevelPolicRef = createWSPolicyRef(dImpl, inputPolicyID);
            }
            Types types = TypesGenerator.addTypes(def, dImpl, serviceMap, typens, globalTypens, methods);
            def.setTypes(types);
            PortTypeImpl portType = addPortTypes(def, dImpl, portTypes, serviceQName);
            Binding binding = addBinding(def, nameSpaceURI, portType, serviceLevelPolicRef, dImpl);
            Vector methodDesc = WSDLGenUtil.getMethodDescriptions(serviceMap);
            for (int i = 0; i < methods.length; ++i) {
                String methodName = methods[i].getMethodName();
                Vector outParams = WSDLGenUtil.getOutParams(serviceMap, methodName);
                OperationImpl operation = addOperation(def, dImpl, methodName, (String) methodDesc.get(i), typens, outParams);
                portType.addOperation(operation);
                if (!abstractWSDL) {
                    UnknownExtensibilityElement wsInPolicyRef = null;
                    UnknownExtensibilityElement wsOutPolicyRef = null;
                    BindingInputImpl bindingInput = addBindingInput(def, methodName, wsInPolicyRef);
                    BindingOutputImpl bindingOutput = addBindingOutput(def, methodName, outParams, wsOutPolicyRef);
                    BindingOperation bindingOperation = addBindingOperation(def, operation, dImpl);
                    bindingOperation.setBindingInput(bindingInput);
                    bindingOperation.setBindingOutput(bindingOutput);
                    binding.addBindingOperation(bindingOperation);
                    if (opLevelPolicRef != null) {
                        binding.addExtensibilityElement(opLevelPolicRef);
                    }
                }
            }
            def.addPortType(portType);
            if (!abstractWSDL) {
                def.addBinding(binding);
                ServiceImpl service = (ServiceImpl) def.createService();
                service.setQName(wsdlQName);
                PortImpl port = (PortImpl) def.createPort();
                port.setName(wsdlQName.getLocalPart() + WSDL_PORT_SUFFIX);
                port.setBinding(binding);
                service.addPort(port);
                SOAPAddressImpl soapAddress = new SOAPAddressImpl();
                soapAddress.setLocationURI(serviceLocation);
                port.addExtensibilityElement(soapAddress);
                def.addService(service);
            }
            if (!abstractWSDL) {
                table.put(WSDL_QNAME, wsdlQName);
            }
            table.put(SERVICE_QNAME, serviceQName);
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            wsWriter.writeWSDL(def, bs);
            wsdlString = bs.toString();
        } catch (WSDLException e) {
            throw new GFacSchemaException("Error generating WSDL: " + e.getMessage());
        }
        Reader reader = new StringReader(wsdlString);
        Writer writer = new StringWriter();
        try {
            RoundTrip.roundTrip(reader, writer, "  ");
        } catch (XmlPullParserException e) {
            throw new GFacSchemaException(e);
        } catch (IOException e) {
            throw new GFacSchemaException(e);
        }
        wsdlString = writer.toString();
        if (abstractWSDL) {
            table.put(AWSDL, wsdlString);
        } else {
            table.put(WSDL, wsdlString);
        }
        return table;
    }

    private UnknownExtensibilityElement createWSPolicyRef(DOMImplementation dImpl, String id) {
        Document doc = dImpl.createDocument(WSP_NAMESPACE, "wsp:PolicyReference", null);
        Element policyRef = doc.getDocumentElement();
        policyRef.setAttribute("URI", "#" + id);
        UnknownExtensibilityElement elem = new UnknownExtensibilityElement();
        elem.setElement(policyRef);
        elem.setElementType(new QName(WSP_NAMESPACE, "PolicyReference"));
        return elem;
    }

    private UnknownExtensibilityElement createTransportLevelPolicy(DOMImplementation dImpl, String policyID) {
        Document doc = dImpl.createDocument(WSP_NAMESPACE, "wsp:Policy", null);
        Element policy = doc.getDocumentElement();
        policy.setAttribute("wsu:Id", policyID);
        Element exactlyOne = doc.createElement("wsp:ExactlyOne");
        Element all = doc.createElement("wsp:All");
        Element transportBinding = doc.createElement("sp:TransportBinding");
        transportBinding.setAttribute("xmlns:sp", SP_NAMESPACE);
        Element policy1 = doc.createElement("wsp:Policy");
        Element transportToken = doc.createElement("sp:TransportToken");
        Element policy2 = doc.createElement("wsp:Policy");
        Element httpsToken = doc.createElement("sp:HttpsToken");
        httpsToken.setAttribute("RequireClientCertificate", "true");
        policy2.appendChild(httpsToken);
        transportToken.appendChild(policy2);
        policy1.appendChild(transportToken);
        Element signedEndorsingSupportingTokens = doc.createElement("sp:SignedEndorsingSupportingTokens");
        policy2 = doc.createElement("wsp:Policy");
        Element x509V3Token = doc.createElement("sp:X509V3Token");
        policy2.appendChild(x509V3Token);
        signedEndorsingSupportingTokens.appendChild(policy2);
        policy1.appendChild(signedEndorsingSupportingTokens);
        transportBinding.appendChild(policy1);
        all.appendChild(transportBinding);
        exactlyOne.appendChild(all);
        policy.appendChild(exactlyOne);
        UnknownExtensibilityElement elem = new UnknownExtensibilityElement();
        elem.setElement(policy);
        elem.setElementType(new QName(WSP_NAMESPACE, "wsp:Policy"));
        return elem;
    }

    private PortTypeImpl addPortTypes(Definition def, DOMImplementation dImpl, PortTypeType[] portTypes, QName serviceQName) {
        PortTypeImpl portType = (PortTypeImpl) def.createPortType();
        portType.setQName(serviceQName);
        portType.setUndefined(false);
        Document doc = dImpl.createDocument(WSDL_NAMEPSPACE, WSDL_DOCUMENTATION, null);
        Element documentation = doc.getDocumentElement();
        documentation.appendChild(doc.createTextNode(portTypes[0].getPortDescription()));
        portType.setDocumentationElement(documentation);
        return portType;
    }

    private OperationImpl addOperation(Definition def, DOMImplementation dImpl, String methodName, String methodDesc, String typens, Vector outputparameter) {
        OperationImpl operation = (OperationImpl) def.createOperation();
        operation.setUndefined(false);
        operation.setName(methodName);
        if (outputparameter.size() == 0) {
            operation.setStyle(OperationType.ONE_WAY);
        } else {
            operation.setStyle(OperationType.REQUEST_RESPONSE);
        }
        Document doc = dImpl.createDocument(WSDL_NAMEPSPACE, WSDL_DOCUMENTATION, null);
        Element documentation = doc.createElement(WSDL_DOCUMENTATION);
        documentation.appendChild(doc.createTextNode(methodDesc));
        operation.setDocumentationElement(documentation);
        MessageImpl inputMessage = (MessageImpl) def.createMessage();
        String inputMessageName = methodName + GFacConstants.SERVICE_REQ_MSG_SUFFIX + "_" + UUID.randomUUID();
        inputMessage.setQName(new QName(def.getTargetNamespace(), inputMessageName));
        inputMessage.setUndefined(false);
        PartImpl inPart = (PartImpl) def.createPart();
        inPart.setName(PART_NAME);
        String inputElementName = methodName + GFacConstants.SERVICE_IN_PARAMS_SUFFIX;
        inPart.setElementName(new QName(typens, inputElementName));
        inputMessage.addPart(inPart);
        def.addMessage(inputMessage);
        InputImpl ip = (InputImpl) def.createInput();
        ip.setName(inputMessageName);
        ip.setMessage(inputMessage);
        operation.setInput(ip);
        if (outputparameter.size() > 0) {
            MessageImpl outputMessage = (MessageImpl) def.createMessage();
            String outputMessageName = methodName + GFacConstants.SERVICE_RESP_MSG_SUFFIX + "_" + UUID.randomUUID();
            outputMessage.setQName(new QName(def.getTargetNamespace(), outputMessageName));
            outputMessage.setUndefined(false);
            PartImpl part = (PartImpl) def.createPart();
            part.setName(PART_NAME);
            String outputElementName = methodName + GFacConstants.SERVICE_OUT_PARAMS_SUFFIX;
            part.setElementName(new QName(typens, outputElementName));
            outputMessage.addPart(part);
            def.addMessage(outputMessage);
            OutputImpl op = (OutputImpl) def.createOutput();
            op.setName(outputMessageName);
            op.setMessage(outputMessage);
            operation.setOutput(op);
        }
        return operation;
    }

    private BindingInputImpl addBindingInput(Definition def, String methodName, UnknownExtensibilityElement wsPolicyRef) {
        BindingInputImpl bindingInput = (BindingInputImpl) def.createBindingInput();
        bindingInput.setName(methodName + GFacConstants.SERVICE_REQ_MSG_SUFFIX);
        if (wsPolicyRef != null) {
            logger.info("policy info is not null");
            bindingInput.addExtensibilityElement(wsPolicyRef);
        }
        SOAPBodyImpl inputExtension = new SOAPBodyImpl();
        inputExtension.setUse(LITERAL);
        bindingInput.addExtensibilityElement(inputExtension);
        return bindingInput;
    }

    private static BindingOutputImpl addBindingOutput(Definition def, String methodName, Vector outParams, UnknownExtensibilityElement wsPolicyRef) {
        BindingOutputImpl bindingOutput = null;
        if (outParams.size() > 0) {
            bindingOutput = (BindingOutputImpl) def.createBindingOutput();
            bindingOutput.setName(methodName + GFacConstants.SERVICE_RESP_MSG_SUFFIX);
            if (wsPolicyRef != null) {
                logger.info("policy info is not null");
                bindingOutput.addExtensibilityElement(wsPolicyRef);
            }
            SOAPBodyImpl outputExtension = new SOAPBodyImpl();
            outputExtension.setUse(LITERAL);
            bindingOutput.addExtensibilityElement(outputExtension);
        }
        return bindingOutput;
    }

    private BindingOperation addBindingOperation(Definition def, OperationImpl operation, DOMImplementation dImpl) {
        BindingOperation bindingOperation = def.createBindingOperation();
        bindingOperation.setName(operation.getName());
        SOAPOperation soapOperation = new SOAPOperationImpl();
        bindingOperation.addExtensibilityElement(soapOperation);
        bindingOperation.setOperation(operation);
        Document doc = dImpl.createDocument(WSP_NAMESPACE, "Misc", null);
        UnknownExtensibilityElement exEle = new UnknownExtensibilityElement();
        Element anonymousEle = doc.createElementNS("http://www.w3.org/2006/05/addressing/wsdl", "wsaw:Anonymous");
        anonymousEle.appendChild(doc.createTextNode("optional"));
        exEle.setElement(anonymousEle);
        exEle.setElementType(new QName("http://www.w3.org/2006/05/addressing/wsdl", "wsaw:Anonymous"));
        bindingOperation.addExtensibilityElement(exEle);
        return bindingOperation;
    }

    private Binding addBinding(Definition def, String nameSpaceURI, PortTypeImpl portType, UnknownExtensibilityElement wsPolicyRef, DOMImplementation dImpl) {
        String portName = portType.getQName().getLocalPart();
        Binding binding = def.createBinding();
        binding.setQName(new QName(nameSpaceURI, portName + WSDL_SOAP_BINDING_SUFFIX));
        binding.setUndefined(false);
        binding.setPortType(portType);
        SOAPBindingImpl soapBindingImpl = new SOAPBindingImpl();
        soapBindingImpl.setStyle(DOCUMENT);
        soapBindingImpl.setTransportURI(SOAP_HTTP_NAMESPACE);
        binding.addExtensibilityElement(soapBindingImpl);
        if (wsPolicyRef != null) {
            logger.info("policy info is not null");
            binding.addExtensibilityElement(wsPolicyRef);
        }
        Document doc = dImpl.createDocument(WSP_NAMESPACE, "Misc", null);
        UnknownExtensibilityElement exEle = new UnknownExtensibilityElement();
        exEle.setElement(doc.createElementNS("http://www.w3.org/2006/05/addressing/wsdl", "wsaw:UsingAddressing"));
        exEle.setElementType(new QName("http://www.w3.org/2006/05/addressing/wsdl", "wsaw:UsingAddressing"));
        binding.addExtensibilityElement(exEle);
        return binding;
    }

    public static void createMetadata(InParamObj inparam, Document doc, Element annotation) {
        if (inparam.getMetadata() != null) {
            unwrapMetadata(doc, annotation, (Element) inparam.getMetadata());
        }
    }

    public static void createMetadata(OutParamObj outparam, Document doc, Element annotation) {
        if (outparam.getMetadata() != null) {
            unwrapMetadata(doc, annotation, (Element) outparam.getMetadata());
        }
    }

    private static void unwrapMetadata(Document doc, Element annotation, Element base) {
        Element appInfo = doc.createElementNS(XSD_NAMESPACE, "appinfo");
        annotation.appendChild(appInfo);
        NodeList childs = base.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            if (childs.item(i) instanceof Element) {
                appInfo.appendChild(cloneElement(doc, (Element) childs.item(i)));
            }
        }
    }

    private static Element cloneElement(Document doc, Element base) {
        Element copy = doc.createElementNS(base.getNamespaceURI(), base.getTagName());
        NodeList nodes = base.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            switch(node.getNodeType()) {
                case Node.COMMENT_NODE:
                    Comment comment = (Comment) node;
                    copy.appendChild(doc.createComment(comment.getData()));
                    break;
                case Node.ELEMENT_NODE:
                    copy.appendChild(cloneElement(doc, (Element) node));
                    break;
                case Node.ATTRIBUTE_NODE:
                    Attr attr = (Attr) node;
                    Attr attrCopy = doc.createAttributeNS(attr.getNamespaceURI(), attr.getPrefix() + ":" + attr.getName());
                    attrCopy.setValue(attr.getValue());
                    copy.appendChild(attrCopy);
                    break;
                case Node.TEXT_NODE:
                    copy.appendChild(doc.createTextNode(((Text) node).getNodeValue()));
                    break;
                default:
                    break;
            }
        }
        return copy;
    }
}
