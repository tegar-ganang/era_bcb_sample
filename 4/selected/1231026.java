package eu.more.core.internal.servicechain;

import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadFactory;
import javax.wsdl.Definition;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.SAXSource;
import org.soda.dpws.service.binding.BindingProvider;
import org.soda.dpws.service.binding.MessageBindingProvider;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mortbay.jetty.Server;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.soda.bindingprovider.JaxbBindingProvider;
import org.soda.dpws.DPWSContext;
import org.soda.dpws.DPWSException;
import org.soda.dpws.DeviceExplorer;
import org.soda.dpws.ServiceProxy;
import org.soda.dpws.addressing.EndpointReference;
import org.soda.dpws.addressing.UserEndpointReference;
import org.soda.dpws.cache.CachedDevice;
import org.soda.dpws.cache.CachedService;
import org.soda.dpws.internal.DPWS;
import org.soda.dpws.internal.DPWSContextImpl;
import org.soda.dpws.internal.DPWSFactory;
import org.soda.dpws.invocation.Call;
import org.soda.dpws.registry.Device;
import org.soda.dpws.registry.ServiceClass;
import org.soda.dpws.registry.ServiceEndpoint;
import org.soda.dpws.server.ServicePort;
import org.soda.dpws.service.MessageInfo;
import org.soda.dpws.service.MessagePartInfo;
import org.soda.dpws.service.binding.MessageBinding;
import org.soda.dpws.soap.Soap12Binding;
import org.soda.dpws.soap.SoapConstants;
import org.soda.dpws.transport.http.XFireHttpSession;
import org.soda.dpws.util.STAXUtils;
import org.soda.dpws.util.jdom.StaxBuilder;
import org.soda.dpws.util.parser.stax.InputFactory;
import org.soda.dpws.util.stax.FragmentStreamReader;
import org.soda.dpws.wsdl.OperationInfo;
import org.soda.dpws.wsdl.PortTypeInfo;
import org.soda.dpws.wsdl.SimpleSchemaType;
import org.soda.dpws.wsdl.WSDLInfo;
import org.xml.sax.InputSource;
import com.ibm.wsdl.OperationImpl;
import com.ibm.wsdl.PartImpl;
import com.ibm.wsdl.PortTypeImpl;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.impl.util.SchemaWriter;
import com.sun.xml.xsom.parser.XSOMParser;
import eu.more.core.internal.MOREService;
import eu.more.core.internal.ResourceThreadFactory;
import eu.more.core.internal.servicechain.jaxb.impl.ServiceChainObjectImpl;
import eu.more.core.internal.servicechain.jaxb.impl.ServiceChainObjectTypeImpl.DependentServicesTypeImpl.DependentServiceImpl;

public class ServiceChain extends MOREService implements ServiceChainInterface {

    private static final ThreadLocal<Integer> serviceChainPosition = new ThreadLocal<Integer>() {

        protected Integer initialValue() {
            return 0;
        }
    };

    private String TNS = "";

    private String serviceOperation = "";

    private String WSDL = null;

    private Element types = null;

    private WSDLInfo instance = null;

    ArrayList<serviceChainMember> serviceChainMemberArray;

    ServiceChainObjectImpl Service = null;

    private ServiceReference threadServiceReference = null;

    private Thread serviceThread = null;

    /**
	   * This functions invokes a remote services that is specified in the parameters. The messages also includes
	   * the original message that was sent to this DPWS device.
	   * 
	   * @param context DPWS status variable.
	   * @param provider BindingProvider that should be used to parse the output of the service.
	   * @param serviceInstance Local instance of the invoked service.
	   * @param service Name of the service.
	   * @param operation Name of the operation of the service
	   * @param params Object list with a list of the parameters which are used while service invocation.
	   *  
	   * @return Byte array which includes the output of the invoked service.
	   * 
	   * @throws DPWSException
	   * 
	   */
    private byte[] invokeServiceAndGsetXMLByteArrayFromInput(DPWSContextImpl context, BindingProvider provider, MOREService serviceInstance, String service, String operation, List<Object> params) throws Exception {
        ByteBuffer allocated = null;
        try {
            class invocationThread extends Thread {

                public boolean success;

                DPWSContextImpl context;

                BindingProvider provider;

                MOREService serviceInstance;

                String service;

                String operation;

                List<Object> params;

                PipedOutputStream outStream;

                public invocationThread(PipedOutputStream outStream, DPWSContextImpl context, BindingProvider provider, MOREService serviceInstance, String service, String operation, List<Object> params) {
                    success = true;
                    this.context = context;
                    this.provider = provider;
                    this.serviceInstance = serviceInstance;
                    this.service = service;
                    this.operation = operation;
                    this.params = params;
                    this.outStream = outStream;
                }

                public void run() {
                    try {
                        Object[] invokeReturnObject = serviceInstance.sendToOtherServiceOnNode(context, service, operation, params);
                        if ((invokeReturnObject != null) && (invokeReturnObject[0] != null)) {
                            final XMLStreamWriter writer = STAXUtils.createXMLStreamWriter(outStream, null, context);
                            provider.writeParameter(null, writer, context, invokeReturnObject[0]);
                            writer.close();
                        } else throw new Exception("Parsing not successful");
                    } catch (Exception e) {
                        System.out.println("Thread-Shit: " + e);
                        success = false;
                    }
                }
            }
            PipedInputStream stream = new PipedInputStream();
            invocationThread writeThread = new invocationThread(new PipedOutputStream(stream), context, provider, serviceInstance, service, operation, params);
            writeThread.start();
            byte[] buffer = new byte[4096];
            while (true) {
                if ((stream.available() > 0) && (allocated == null)) {
                    int laenge = stream.read(buffer);
                    allocated = (ByteBuffer.allocate(laenge)).put(buffer, 0, laenge);
                }
                if ((allocated != null) || (!writeThread.success)) break;
            }
        } catch (Exception e) {
        }
        if (allocated != null) return allocated.array(); else throw new Exception();
    }

    /**
	   * The function of this block is to take the parsed byte array and to change the namespace
	   * to the namespace of the successor service. The byte array is then transformed into a jaxb object 
	   * of the successor service. 
	   * 
	   * @param context DPWS status variable.
	   *  
	   * @return Byte array which includes the output of the invoked service.
	   * 
	   * @throws DPWSException
	   * 
	   */
    private Object getObjectFromAStringAndChangeNamespace(DPWSContext context, Object body, String input, int position, String oldNamespace, String newNamespace, int offset) throws Exception {
        serviceChainMember currentServiceChainMember = serviceChainMemberArray.get(position + offset);
        if ((position + offset) == 0) {
            Element changeNameSpace = removeNamespaces((new StaxBuilder()).build(new FragmentStreamReader((XMLStreamReader) body)).getRootElement());
            if (currentServiceChainMember.getInputBinding()) changeNameSpace = addNamespaces(changeNameSpace, Namespace.getNamespace(currentServiceChainMember.getServiceIdentifier()));
            input = (new org.jdom.output.XMLOutputter()).outputString(changeNameSpace);
        } else if (currentServiceChainMember.getInputBinding()) input = input.replace(oldNamespace, newNamespace);
        Object returnObject = null;
        ByteArrayInputStream objectToByteArray = new ByteArrayInputStream(input.getBytes());
        if (serviceChainMemberArray.get(position + offset).getInputBinding()) returnObject = currentServiceChainMember.getBindingProvider().readParameter((MessagePartInfo) null, STAXUtils.createXMLStreamReader(objectToByteArray, null, (DPWSContextImpl) context), (DPWSContextImpl) context); else returnObject = (XMLStreamReader) (new org.soda.dpws.util.parser.stax.InputFactory()).createXMLStreamReader(objectToByteArray);
        objectToByteArray.close();
        return returnObject;
    }

    private List addElementToList(Element element, List list) {
        list.add(element);
        for (Object itElement : element.getContent()) {
            if ((itElement != null) && (itElement instanceof Element)) addElementToList((Element) itElement, list);
        }
        return list;
    }

    private Element removeNamespaces(Element paraElement) {
        for (Element itElement : (List<Element>) addElementToList((Element) paraElement, new ArrayList<Element>())) {
            while (itElement.getAdditionalNamespaces().size() > 0) {
                itElement.removeNamespaceDeclaration((Namespace) itElement.getAdditionalNamespaces().get(0));
            }
        }
        return paraElement;
    }

    private Element substituteAdditionalNamespaces(Element paraElement, Namespace paraNamespace) throws Exception {
        for (Element itElement : (List<Element>) addElementToList((Element) paraElement, new ArrayList<Element>())) {
            if (itElement.getAdditionalNamespaces().size() < 1) continue;
            if (itElement.getAdditionalNamespaces().size() > 1) throw new Exception("Too many different namespaces");
            itElement.removeNamespaceDeclaration((Namespace) itElement.getAdditionalNamespaces().get(0));
            itElement.addNamespaceDeclaration(paraNamespace);
        }
        return paraElement;
    }

    private Element addNamespaces(Element paraElement, Namespace paraNamespace) {
        for (Element itElement : (List<Element>) addElementToList((Element) paraElement, new ArrayList<Element>())) itElement.setNamespace(paraNamespace);
        return paraElement;
    }

    public String getStringByteArray(byte[] bytearray) {
        return String.valueOf((Charset.forName("ISO-8859-1")).decode((ByteBuffer) ((ByteBuffer.allocate(bytearray.length)).put(bytearray).flip())));
    }

    private Object invokeLocal(DPWSContext context, Object body, Object localReturnObject, int sCPV) throws Exception {
        List invokeList = new LinkedList();
        serviceChainMember currentServiceChainMember = serviceChainMemberArray.get(sCPV);
        if (sCPV > 0) {
            if (localReturnObject instanceof Object[]) {
                for (int i = 0; i < ((Object[]) localReturnObject).length; i++) invokeList.add(getObjectFromAStringAndChangeNamespace(context, body, getStringByteArray((byte[]) ((Object[]) localReturnObject)[0]), sCPV - 1, serviceChainMemberArray.get(sCPV - 1).getServiceIdentifier(), serviceChainMemberArray.get(sCPV).getServiceIdentifier(), 1));
            } else {
                invokeList.add(getObjectFromAStringAndChangeNamespace(context, body, getStringByteArray((byte[]) localReturnObject), sCPV - 1, serviceChainMemberArray.get(sCPV - 1).getServiceIdentifier(), serviceChainMemberArray.get(sCPV).getServiceIdentifier(), 1));
            }
        } else if (body != null) invokeList.add(getObjectFromAStringAndChangeNamespace(context, body, "", 0, "", "", 0));
        return invokeServiceAndGsetXMLByteArrayFromInput((DPWSContextImpl) context, (BindingProvider) currentServiceChainMember.getBindingProvider(), (MOREService) this, currentServiceChainMember.getServiceIdentifier(), currentServiceChainMember.getServiceOperation(), (List<Object>) invokeList);
    }

    private Object invokeRemote(Object body, Object ReturnObject, int sCPV) throws Exception {
        Object localReturnObject = null;
        serviceChainMember currentServiceChainMember = serviceChainMemberArray.get(sCPV);
        Element invokeElement = null;
        java.util.ArrayList params = new java.util.ArrayList();
        if (sCPV == 0) {
            invokeElement = removeNamespaces((new StaxBuilder()).build(new FragmentStreamReader((XMLStreamReader) body)).getRootElement());
            if (currentServiceChainMember.getInputBinding()) invokeElement = addNamespaces(invokeElement, Namespace.getNamespace("tns", currentServiceChainMember.getServiceIdentifier()));
            invokeElement.addNamespaceDeclaration(Namespace.getNamespace(currentServiceChainMember.getServiceIdentifier()));
        } else {
            if (ReturnObject instanceof Object[]) {
                for (int i = 0; i < ((Object[]) localReturnObject).length; i++) {
                    ByteArrayInputStream bytesToElement = new ByteArrayInputStream((byte[]) ReturnObject);
                    Element localInvokeElement = removeNamespaces((((Document) new StaxBuilder().build((new InputFactory()).createXMLStreamReader(bytesToElement))).getRootElement()));
                    localInvokeElement = addNamespaces(localInvokeElement, Namespace.getNamespace("tns", currentServiceChainMember.getServiceIdentifier()));
                    localInvokeElement.addNamespaceDeclaration(Namespace.getNamespace(currentServiceChainMember.getServiceIdentifier()));
                    bytesToElement.close();
                    params.add(localInvokeElement);
                }
            } else {
                ByteArrayInputStream bytesToElement = new ByteArrayInputStream((byte[]) ReturnObject);
                invokeElement = removeNamespaces((((Document) new StaxBuilder().build((new InputFactory()).createXMLStreamReader(bytesToElement))).getRootElement()));
                invokeElement = addNamespaces(invokeElement, Namespace.getNamespace("tns", currentServiceChainMember.getServiceIdentifier()));
                invokeElement.addNamespaceDeclaration(Namespace.getNamespace(currentServiceChainMember.getServiceIdentifier()));
                bytesToElement.close();
            }
        }
        ArrayList<byte[]> input = new ArrayList<byte[]>();
        try {
            Call endpointCall = currentServiceChainMember.getCall();
            GetMeasureResponseCallback newResponseCallback = new GetMeasureResponseCallback(currentServiceChainMember.getOutAction());
            endpointCall.setResponseCallback(newResponseCallback);
            if (ReturnObject instanceof Object[]) {
                endpointCall.invoke(currentServiceChainMember.getInAction(), params);
            } else {
                endpointCall.invoke(currentServiceChainMember.getInAction(), invokeElement);
            }
            Element[] response = (Element[]) newResponseCallback.getResult();
            for (int i = 0; i < response.length; i++) {
                response[i] = removeNamespaces(response[i]);
                response[i] = addNamespaces(response[i], Namespace.getNamespace(currentServiceChainMember.getServiceIdentifier()));
                input.add((new org.jdom.output.XMLOutputter()).outputString(response[i]).getBytes());
            }
        } catch (Exception e) {
            currentServiceChainMember.resetCall();
            currentServiceChainMember.getCall();
        }
        return input.toArray();
    }

    private class GetMeasureResponseCallback implements org.soda.dpws.invocation.ResponseCallback {

        Element[] response = null;

        String responseAction;

        public GetMeasureResponseCallback(String responseAction) {
            this.responseAction = responseAction;
        }

        public void invoke(org.soda.dpws.DPWSContext context, String action, javax.xml.stream.XMLStreamReader body) throws DPWSException {
            try {
                if (!action.equals(responseAction)) throw new DPWSException("This callback is designed to receive messages with the '" + responseAction + "' action and not '" + action + "'");
                java.util.List params = (java.util.List) context.getCurrentMessageBody();
                response = new Element[params.size()];
                for (int i = 0; i < params.size(); i++) response[i] = (new org.soda.dpws.util.jdom.StaxBuilder().build((javax.xml.stream.XMLStreamReader) params.get(i))).getRootElement();
            } catch (Exception e) {
                e.printStackTrace();
                response = null;
            }
        }

        public Element[] getResult() {
            return response;
        }
    }

    /**
	   * Method that is invoked via a DPWS service.
	   * 
	   * @param context DPWS status variable.
	   * @param body Values that had been transported in the invoking message.
	   *  
	   * @return Object that will be transfered back to the invoker of the service.
	   * 
	   * @throws DPWSException
	   * 
	   */
    public Object invoke(DPWSContext context, Object body) throws DPWSException {
        Object localReturnObject = null;
        try {
            int serviceChainPositionValue = 0;
            while ((serviceChainPositionValue = serviceChainPosition.get().intValue()) < serviceChainMemberArray.size()) {
                if (serviceChainMemberArray.get(serviceChainPositionValue).getLocal()) {
                    localReturnObject = invokeLocal(context, body, localReturnObject, serviceChainPositionValue);
                } else {
                    localReturnObject = invokeRemote(body, localReturnObject, serviceChainPositionValue);
                }
                serviceChainPosition.set(serviceChainPosition.get() + 1);
            }
            if (localReturnObject instanceof Object[]) {
                ArrayList<Element> localReturnObjectArrayList = new ArrayList<Element>();
                for (int i = 0; i < ((Object[]) localReturnObject).length; i++) {
                    ByteArrayInputStream bytesToElement = new ByteArrayInputStream((byte[]) ((Object[]) localReturnObject)[i]);
                    Element changeNameSpace = removeNamespaces(((Document) new StaxBuilder().build((new InputFactory()).createXMLStreamReader(bytesToElement))).getRootElement());
                    changeNameSpace.addNamespaceDeclaration(Namespace.getNamespace(TNS));
                    localReturnObjectArrayList.add(addNamespaces(changeNameSpace, Namespace.getNamespace("tns", TNS)));
                    bytesToElement.close();
                }
                Element[] localReturnObjectArray = new Element[localReturnObjectArrayList.size()];
                localReturnObject = localReturnObjectArrayList.toArray(localReturnObjectArray);
                if (localReturnObjectArrayList.size() == 1) {
                    localReturnObject = (Element) localReturnObjectArrayList.get(0);
                }
            } else {
                ByteArrayInputStream bytesToElement = new ByteArrayInputStream((byte[]) localReturnObject);
                Element changeNameSpace = removeNamespaces(((Document) new StaxBuilder().build((new InputFactory()).createXMLStreamReader(bytesToElement))).getRootElement());
                changeNameSpace.addNamespaceDeclaration(Namespace.getNamespace(TNS));
                localReturnObject = addNamespaces(changeNameSpace, Namespace.getNamespace("tns", TNS));
                bytesToElement.close();
            }
        } catch (Exception e) {
            System.out.println("Das war wohl nichts: " + e);
            e.printStackTrace();
            localReturnObject = null;
        }
        serviceChainPosition.set(0);
        return localReturnObject;
    }

    /**
	   * Method that is invoked via a DPWS service.
	   * 
	   * @param context DPWS status variable.
	   *  
	   * @return Object that will be transfered back to the invoker of the service.
	   * 
	   * @throws DPWSException
	   * 
	   */
    public Object invoke(DPWSContext context) throws DPWSException {
        return invoke(context, null);
    }

    /**
	   * Method that created a DPWS service from a service chain XML description
	   * 
	   * @param context Bundle context of this bundle
	   * @param para_device DPWS device where the service chain will be add to.
	   * @param XMLlocation Name and location of the XML file which comprises the service chain description  
	   *  
	   * @return If the service chain is created a valid ServiceEndpoint will be return.
	   * 
	   * @throws Exception
	   * 
	   */
    public ServiceEndpoint CreateServiceChain(BundleContext context, Device para_device, String XMLlocation) throws Exception {
        serviceChainMemberArray = new ArrayList<serviceChainMember>();
        device = para_device;
        bundleContext = context;
        try {
            SAXParserFactory parserFactory = new org.apache.xerces.jaxp.SAXParserFactoryImpl();
            parserFactory.setNamespaceAware(true);
            Unmarshaller unmarshaller = JAXBContext.newInstance("eu.more.core.internal.servicechain.jaxb", ServiceChain.class.getClassLoader()).createUnmarshaller();
            Service = (ServiceChainObjectImpl) unmarshaller.unmarshal(new SAXSource((parserFactory.newSAXParser()).getXMLReader(), new InputSource(new FileInputStream(XMLlocation))));
        } catch (Exception ioe) {
            ioe.printStackTrace();
            throw new Exception("Cannot create Service Chain because of configuration file trouble");
        }
        if (Service == null) throw new Exception("Cannot create Service Chain because of configuration file trouble");
        TNS = Service.getNameSpace();
        WSDL = Service.getWSDL();
        serviceOperation = Service.getOperation();
        ServiceUID = Service.getUID();
        SERVICE_ID = Service.getServiceChainIdentifier();
        Iterator serIt = Service.getDependentServices().getDependentService().iterator();
        while (serIt.hasNext()) {
            DependentServiceImpl dependentServiceOb = (DependentServiceImpl) serIt.next();
            String ServiceIdentifier = dependentServiceOb.getServiceIdentifier();
            String ServiceOperation = dependentServiceOb.getServiceOperation();
            serviceChainMember serviceChainMemberObject = new serviceChainMember(ServiceIdentifier, ServiceOperation, dependentServiceOb.getNameSpace(), dependentServiceOb.isBinding());
            serviceChainMemberObject.setUID(dependentServiceOb.getUID());
            serviceChainMemberObject.setLocal(dependentServiceOb.isLocal());
            Element typesElement = null;
            if (!serviceChainMemberObject.getLocal()) {
                try {
                    HttpClient httpClient = new HttpClient();
                    httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
                    HttpMethod httpMethod = new GetMethod(serviceChainMemberObject.getCall().getEndpointReference().getAddress() + "/getwsdl");
                    httpMethod.setFollowRedirects(true);
                    httpClient.executeMethod(httpMethod);
                    String httpResponse = httpMethod.getResponseBodyAsString().replaceAll("[\\s\\t\\r\\n]+", " ").replaceAll(">[\\s\\t\\r\\n]*<", "><").trim();
                    serviceChainMemberObject.setWSDL(httpResponse.getBytes());
                    Definition newDefinition = (new com.ibm.wsdl.factory.WSDLFactoryImpl()).newWSDLReader().readWSDL((String) null, new InputSource(new ByteArrayInputStream(serviceChainMemberObject.getWSDL())));
                    httpResponse = httpResponse.replaceAll("[\\s\\t\\r\\n]+", " ").replaceAll(">[\\s\\t\\r\\n]*<", "><").trim();
                    ByteArrayInputStream bytesToElement = new ByteArrayInputStream((byte[]) httpResponse.getBytes());
                    typesElement = (((Document) new StaxBuilder().build((new InputFactory()).createXMLStreamReader(bytesToElement))).getRootElement());
                    typesElement = typesElement.getChild("types", Namespace.getNamespace("wsdl", "http://schemas.xmlsoap.org/wsdl/"));
                    typesElement = typesElement.getChild("schema", Namespace.getNamespace("xsd", "http://www.w3.org/2001/XMLSchema"));
                    List<Namespace> neededNamespaces = new LinkedList<Namespace>();
                    String input = (new org.jdom.output.XMLOutputter()).outputString(typesElement);
                    for (Namespace neededNamespaceCandidate : (List<Namespace>) typesElement.getAdditionalNamespaces()) {
                        if (input.contains(("\"" + neededNamespaceCandidate.getPrefix() + ":")) && (!neededNamespaces.contains(Namespace.getNamespace(neededNamespaceCandidate.getPrefix(), neededNamespaceCandidate.getURI())))) neededNamespaces.add(Namespace.getNamespace(neededNamespaceCandidate.getPrefix(), neededNamespaceCandidate.getURI()));
                    }
                    typesElement = removeNamespaces(typesElement);
                    for (Namespace neededNamespaceCandidate : (List<Namespace>) neededNamespaces) typesElement.addNamespaceDeclaration(neededNamespaceCandidate);
                    try {
                        substituteAdditionalNamespaces(typesElement, Namespace.getNamespace("tns", TNS));
                    } catch (Exception e) {
                        throw new Exception("Cannot create Service Chain because service dependencies are not met");
                    }
                    while (typesElement.getAttributes().size() > 0) typesElement.removeAttribute((Attribute) typesElement.getAttributes().get(0));
                    for (PortTypeImpl pti : (Collection<PortTypeImpl>) newDefinition.getPortTypes().values()) for (OperationImpl oi : (List<OperationImpl>) pti.getOperations()) if (oi.getName().equals(serviceChainMemberObject.getServiceOperation())) {
                        serviceChainMemberObject.setInElements(oi.getInput().getMessage().getParts().size());
                        serviceChainMemberObject.setOutElements(oi.getOutput().getMessage().getParts().size());
                        serviceChainMemberObject.setInputParameterAmount(oi.getInput().getMessage().getParts().size());
                        serviceChainMemberObject.setOutputParameterAmount(oi.getOutput().getMessage().getParts().size());
                        serviceChainMemberObject.setInAction(pti.getQName().getNamespaceURI() + "/" + pti.getQName().getLocalPart() + "/" + oi.getName() + "Request");
                        serviceChainMemberObject.setOutAction(pti.getQName().getNamespaceURI() + "/" + pti.getQName().getLocalPart() + "/" + oi.getName() + "Response");
                        for (int i = 0; i < serviceChainMemberObject.getInputParameterAmount(); i++) {
                            serviceChainMemberObject.setInElement(((PartImpl) ((oi.getInput().getMessage().getParts().values().toArray()[i]))).getElementName().getLocalPart(), i);
                        }
                        for (int i = 0; i < serviceChainMemberObject.getOutputParameterAmount(); i++) {
                            serviceChainMemberObject.setOutElement(((PartImpl) ((oi.getOutput().getMessage().getParts().values().toArray()[i]))).getElementName().getLocalPart(), i);
                        }
                    }
                } catch (Exception e) {
                    throw new Exception("Cannot create Service Chain because service dependencies are not met");
                }
            } else {
                ServiceEndpoint serviceEndpoint = getService(dependentServiceOb.getServiceIdentifier());
                if (serviceEndpoint == null) {
                    throw new Exception("Cannot create Service Chain because service dependencies are not met");
                }
                serviceChainMemberObject.setWSDL((byte[]) serviceEndpoint.getProperty("WSDL"));
                String wsdlstring = getStringByteArray(serviceChainMemberObject.getWSDL());
                wsdlstring = wsdlstring.replaceAll("[\\s\\t\\r\\n]+", " ").replaceAll(">[\\s\\t\\r\\n]*<", "><").trim();
                ByteArrayInputStream bytesToElement = new ByteArrayInputStream((byte[]) wsdlstring.getBytes());
                typesElement = (((Document) new StaxBuilder().build((new InputFactory()).createXMLStreamReader(bytesToElement))).getRootElement());
                typesElement = typesElement.getChild("types", Namespace.getNamespace("wsdl", "http://schemas.xmlsoap.org/wsdl/"));
                typesElement = typesElement.getChild("schema", Namespace.getNamespace("xsd", "http://www.w3.org/2001/XMLSchema"));
                List<Namespace> neededNamespaces = new LinkedList<Namespace>();
                String input = (new org.jdom.output.XMLOutputter()).outputString(typesElement);
                for (Namespace neededNamespaceCandidate : (List<Namespace>) typesElement.getAdditionalNamespaces()) {
                    if (input.contains(("\"" + neededNamespaceCandidate.getPrefix() + ":")) && (!neededNamespaces.contains(Namespace.getNamespace(neededNamespaceCandidate.getPrefix(), neededNamespaceCandidate.getURI())))) neededNamespaces.add(Namespace.getNamespace(neededNamespaceCandidate.getPrefix(), neededNamespaceCandidate.getURI()));
                }
                typesElement = removeNamespaces(typesElement);
                for (Namespace neededNamespaceCandidate : (List<Namespace>) neededNamespaces) typesElement.addNamespaceDeclaration(neededNamespaceCandidate);
                try {
                    substituteAdditionalNamespaces(typesElement, Namespace.getNamespace("tns", TNS));
                } catch (Exception e) {
                    throw new Exception("Cannot create Service Chain because service dependencies are not met");
                }
                while (typesElement.getAttributes().size() > 0) typesElement.removeAttribute((Attribute) typesElement.getAttributes().get(0));
                Iterator webserIt = serviceEndpoint.getServiceClass().getWebServices().iterator();
                while (webserIt.hasNext()) {
                    WSDLInfo localWSDLInfo = ((WSDLInfo) (webserIt.next()));
                    if (localWSDLInfo.getBindingProvider() != null) serviceChainMemberObject.setBindingProvider(localWSDLInfo.getBindingProvider()); else serviceChainMemberObject.setBindingProvider(new MessageBindingProvider());
                    Iterator portIt = localWSDLInfo.getPortTypeInfos().iterator();
                    while (portIt.hasNext()) {
                        PortTypeInfo localPo = (PortTypeInfo) portIt.next();
                        OperationInfo localOp = localPo.getAddressingOperation(ServiceOperation);
                        if (localOp != null) {
                            serviceChainMemberObject.setInAction(localOp.getInAction());
                            serviceChainMemberObject.setOutAction(localOp.getOutAction());
                            serviceChainMemberObject.setInputParameterAmount(localOp.getInputMessage().getMessageParts().size());
                            serviceChainMemberObject.setOutputParameterAmount(localOp.getOutputMessage().getMessageParts().size());
                            serviceChainMemberObject.setInElements(localOp.getInputMessage().getMessageParts().size());
                            serviceChainMemberObject.setOutElements(localOp.getOutputMessage().getMessageParts().size());
                            for (int i = 0; i < serviceChainMemberObject.getInputParameterAmount(); i++) {
                                serviceChainMemberObject.setInElement(((MessagePartInfo) localOp.getInputMessage().getMessageParts().get(i)).getSchemaType().getSchemaType().getLocalPart(), i);
                            }
                            for (int i = 0; i < serviceChainMemberObject.getOutputParameterAmount(); i++) {
                                serviceChainMemberObject.setOutElement(((MessagePartInfo) localOp.getOutputMessage().getMessageParts().get(i)).getSchemaType().getSchemaType().getLocalPart(), i);
                            }
                        }
                    }
                }
            }
            if (types == null) {
                types = new Element("schema");
                types.setNamespace(Namespace.getNamespace("xsd", "http://www.w3.org/2001/XMLSchema"));
                types.addNamespaceDeclaration(Namespace.getNamespace("tns", TNS));
            }
            for (Object child : typesElement.cloneContent()) {
                if (child instanceof Element) {
                    Element childElement = (Element) child;
                    boolean exists = false;
                    for (Object childTypes : types.cloneContent()) {
                        if (childTypes instanceof Element) if (childElement.getAttributeValue("name").equals(((Element) childTypes).getAttributeValue("name"))) exists = true;
                    }
                    if (!exists) types.addContent(childElement);
                }
            }
            String input = (new org.jdom.output.XMLOutputter()).outputString(types);
            serviceChainMemberArray.add(serviceChainMemberObject);
        }
        if (serviceChainMemberArray.size() != Service.getDependentServices().getDependentService().size()) {
            serviceChainMemberArray.clear();
            throw new Exception("Cannot create Service Chain because not all dependent service loaded");
        }
        if (Service.getWSDL().equals("")) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            stringBuffer.append("<wsdl:definitions name=\"ServiceChaining\"\n");
            stringBuffer.append("targetNamespace=\"http://www.ist-more.org/ServiceChaining\"\n");
            stringBuffer.append("xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap12/\"\n");
            stringBuffer.append("xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\"\n");
            stringBuffer.append("xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n");
            stringBuffer.append("xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n");
            stringBuffer.append("xmlns:tns=\"http://www.ist-more.org/ServiceChaining\"\n");
            stringBuffer.append("xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\">\n");
            stringBuffer.append("<wsdl:types>\n");
            stringBuffer.append((new org.jdom.output.XMLOutputter()).outputString(types) + "\n");
            stringBuffer.append("</wsdl:types>\n");
            if (!Service.isActive()) {
                stringBuffer.append("<wsdl:message name=\"OpReqMsg\">\n");
                for (int i = 0; i < serviceChainMemberArray.get(0).getInputParameterAmount(); i++) {
                    stringBuffer.append("<wsdl:part name=\"paramater" + i + "\" element=\"tns:" + serviceChainMemberArray.get(0).getInElement(i) + "\" />\n");
                }
                stringBuffer.append("</wsdl:message>\n");
            }
            stringBuffer.append("<wsdl:message name=\"OpReqRespMsg\">\n");
            for (int i = 0; i < serviceChainMemberArray.get(serviceChainMemberArray.size() - 1).getOutputParameterAmount(); i++) {
                stringBuffer.append("<wsdl:part name=\"paramater" + i + "\" element=\"tns:" + serviceChainMemberArray.get(serviceChainMemberArray.size() - 1).getOutElement(i) + "\" />\n");
            }
            stringBuffer.append("</wsdl:message>\n");
            stringBuffer.append("<wsdl:portType name=\"" + Service.getServiceChainIdentifier() + "\" wse:EventSource=\"true\">\n");
            stringBuffer.append("<wsdl:operation name=\"" + Service.getOperation() + "\">");
            if (!Service.isActive()) {
                stringBuffer.append("<wsdl:input message=\"tns:OpReqMsg\" wsa:Action=\"" + Service.getNameSpace() + "/" + Service.getServiceChainIdentifier() + "/" + Service.getOperation() + "Request\">\n");
                stringBuffer.append("</wsdl:input>\n");
            }
            stringBuffer.append("<wsdl:output message=\"tns:OpReqRespMsg\" wsa:Action=\"" + Service.getNameSpace() + "/" + Service.getServiceChainIdentifier() + "/" + Service.getOperation() + ((Service.isActive()) ? "" : "Response") + "\">\n");
            stringBuffer.append("</wsdl:output>\n");
            stringBuffer.append("</wsdl:operation>\n");
            stringBuffer.append("</wsdl:portType>\n");
            stringBuffer.append("<wsdl:binding name=\"" + Service.getServiceChainIdentifier() + "Bind\" type=\"tns:" + Service.getServiceChainIdentifier() + "\">\n");
            stringBuffer.append("<soap:binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\" />\n");
            stringBuffer.append("<wsdl:operation name=\"" + Service.getOperation() + "\">\n");
            stringBuffer.append("<soap:operation soapAction=\"" + Service.getNameSpace() + "/" + Service.getServiceChainIdentifier() + "/" + Service.getOperation() + "Request\" />\n");
            if (!Service.isActive()) {
                stringBuffer.append("<wsdl:input>\n");
                stringBuffer.append("<soap:body use=\"literal\" />\n");
                stringBuffer.append("</wsdl:input>\n");
            }
            stringBuffer.append("<wsdl:output>\n");
            stringBuffer.append("<soap:body use=\"literal\" />\n");
            stringBuffer.append("</wsdl:output>\n");
            stringBuffer.append("</wsdl:operation>\n");
            stringBuffer.append("</wsdl:binding>\n");
            stringBuffer.append("<wsdl:service name=\"" + Service.getServiceChainIdentifier() + "\">\n");
            stringBuffer.append("<wsdl:port name=\"" + Service.getServiceChainIdentifier() + "\" binding=\"tns:" + Service.getServiceChainIdentifier() + "Bind\">\n");
            stringBuffer.append("<soap:address location=\"" + Service.getNameSpace() + "/" + Service.getServiceChainIdentifier() + "\"/>\n");
            stringBuffer.append("</wsdl:port>\n");
            stringBuffer.append("</wsdl:service>\n");
            stringBuffer.append("</wsdl:definitions>\n");
            WSDL = stringBuffer.toString();
            Service.setWSDL(WSDL);
        }
        serviceEndpoint = null;
        ServicePort representativeServicePort = null;
        ServiceClass ServiceClassInst = null;
        try {
            ServiceClassInst = new ServiceClass();
            wsdlInfo = buildWSDLInfo(new WSDLInfo("Service.wsdl"));
            instance = wsdlInfo;
            ServiceClassInst.addWebService(wsdlInfo);
            device.getDeviceModel().addServiceClass(ServiceClassInst, SERVICE_ID);
            serviceEndpoint = ServiceClassInst.createService(this, SERVICE_ID, true);
            wsdlInfo.removeLocation("Service.wsdl");
            if (WSDL != null) serviceEndpoint.setProperty("WSDL", WSDL.getBytes());
            if (ServiceUID == null) device.addHostedService(serviceEndpoint); else device.addHostedService(serviceEndpoint, ServiceUID);
            Collection<ServicePort> physicalBindings = serviceEndpoint.getPhysicalBindings();
            Iterator<ServicePort> itAddr = physicalBindings.iterator();
            while (itAddr.hasNext()) {
                representativeServicePort = itAddr.next();
                this.wsdlInfo.addLocation((representativeServicePort).getFullAddress() + "/getwsdl");
            }
            if (Service.isActive()) {
                serviceThread = startThread(representativeServicePort);
            }
        } catch (Exception e) {
            if (ServiceClassInst != null) device.getDeviceModel().removeServiceClass(SERVICE_ID);
            if (serviceEndpoint != null) {
                device.removeHostedService(serviceEndpoint);
                serviceEndpoint = null;
            }
            throw new Exception("Cannot create Service Chain because of configuration file trouble", e);
        }
        return serviceEndpoint;
    }

    /**
	   * Method that will start a thread if the service chain is active.
	   * 
	   * @param ServicePort Service implementation
	   *  
	   * @return Thread that can be started.

	   * 
	   */
    private Thread startThread(final ServicePort ServiceInst) {
        ServiceTracker threadTracker = new ServiceTracker(bundleContext, ThreadFactory.class.getName(), null);
        threadTracker.open();
        Thread updateThread = null;
        if ((threadServiceReference = threadTracker.getServiceReference()) == null) {
            return null;
        }
        if (updateThread == null) {
            updateThread = ((ResourceThreadFactory) bundleContext.getService(threadServiceReference)).newThread(new Runnable() {

                public void run() {
                    Thread.currentThread().setContextClassLoader(thisClassLoader);
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            if (ServiceInst != null) {
                                ServicePort servicePort = ServiceInst;
                                final String action = TNS + "/" + SERVICE_ID + "/" + serviceOperation;
                                ServiceEndpoint remoteServiceEndpoint = servicePort.getServiceEndpoint();
                                remoteServiceEndpoint.getPhysicalBindings();
                                DPWS dpws = DPWSFactory.newInstance().getDPWS();
                                DPWSContextImpl dpwsContext = new DPWSContextImpl(dpws, new UserEndpointReference(servicePort.getFullAddress()));
                                dpwsContext.setDpws(dpws);
                                dpwsContext.setProperty(DPWSContext.CLIENT_MODE, Boolean.TRUE);
                                dpwsContext.setService(remoteServiceEndpoint);
                                List<EndpointReference> endpoints = remoteServiceEndpoint.getSubcribers(action);
                                final Object returnobject = invoke(dpwsContext);
                                if ((endpoints != null) && (returnobject != null)) for (EndpointReference endpoint : endpoints) {
                                    final EndpointReference epr = endpoint;
                                    new Thread(new Runnable() {

                                        public void run() {
                                            try {
                                                (new Call(new UserEndpointReference(epr), instance)).send(action, returnobject);
                                            } catch (DPWSException e) {
                                            }
                                        }
                                    }).start();
                                }
                            }
                            Thread.currentThread().sleep(Service.getActiveInterval());
                        } catch (Exception e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            });
            updateThread.start();
        }
        return updateThread;
    }

    /**
	   * Method that will stop a thread if the service chain is active.
	   * 

	   * 
	   */
    public void stopThread() {
        if (serviceThread != null) serviceThread.interrupt();
        if (threadServiceReference != null) bundleContext.ungetService(threadServiceReference);
    }

    /**
	   * Method that creates and fills all structures of the DPWS stack with the values of the 
	   * service chain
	   * 
	   * @param wsdlInfo WSDLInfo object for this service.
	   *  
	   * @return filled WSDLInfo
	   * 
	   * @throws Exception
	   * 
	   */
    private WSDLInfo buildWSDLInfo(WSDLInfo wsdlInfo) throws DPWSException {
        Class serviceClass, handlerClass;
        MessageInfo message = null;
        MessagePartInfo msgPart;
        SimpleSchemaType xmlType;
        OperationInfo opInfo;
        Class[] params;
        try {
            serviceClass = Class.forName("eu.more.core.internal.servicechain.ServiceChainInterface");
        } catch (ClassNotFoundException e) {
            throw new DPWSException("Could not load service chain interface.", e);
        }
        PortTypeInfo typeInfo0 = new PortTypeInfo(new QName(TNS, SERVICE_ID), serviceClass, null, (Service.isActive()) ? true : false);
        params = new Class[1 + serviceChainMemberArray.get(0).getInputParameterAmount()];
        params[0] = DPWSContext.class;
        for (int i = 0; i < (serviceChainMemberArray.get(0).getInputParameterAmount()); i++) {
            params[i + 1] = Object.class;
        }
        try {
            opInfo = typeInfo0.addOperation(serviceOperation, serviceClass.getMethod("invoke", params), TNS + "/" + SERVICE_ID + "/" + serviceOperation + "Request", TNS + "/" + SERVICE_ID + "/" + serviceOperation + ((Service.isActive()) ? "" : "Response"), (Service.isActive()) ? true : false);
        } catch (Exception e) {
            throw new DPWSException("Could not create Service Port.", e);
        }
        if (serviceChainMemberArray.get(0).getInAction() != null) {
            message = opInfo.createMessage(new QName(TNS, "OpReqMsg"));
            if (serviceChainMemberArray.get(0).getInputParameterAmount() > 0) {
                for (int i = 0; i < serviceChainMemberArray.get(0).getInputParameterAmount(); i++) {
                    msgPart = message.addMessagePart(new QName(TNS, "paramater" + i), javax.xml.stream.XMLStreamReader.class);
                }
            }
        }
        opInfo.setInputMessage(message);
        message = null;
        if (serviceChainMemberArray.get(serviceChainMemberArray.size() - 1).getOutAction() != null) {
            message = opInfo.createMessage(new QName(TNS, "OpReqRespMsg"));
            if (serviceChainMemberArray.get(serviceChainMemberArray.size() - 1).getOutputParameterAmount() > 0) {
                for (int i = 0; i < serviceChainMemberArray.get(serviceChainMemberArray.size() - 1).getOutputParameterAmount(); i++) {
                    msgPart = message.addMessagePart(new QName(TNS, "paramater" + i), javax.xml.stream.XMLStreamReader.class);
                }
            }
        }
        opInfo.setOutputMessage(message);
        wsdlInfo.addPortType(typeInfo0);
        Soap12Binding binding;
        binding = new Soap12Binding(typeInfo0, "http://www.w3.org/2003/05/soap/bindings/HTTP/");
        binding.setStyle(SoapConstants.STYLE_DOCUMENT);
        binding.setSerializer(new MessageBinding());
        wsdlInfo.addBinding(binding);
        return wsdlInfo;
    }
}
