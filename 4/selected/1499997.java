package eu.planets_project.ifr.core.wdt.gui.faces;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.component.html.HtmlSelectOneMenu;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.io.IOUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.apache.myfaces.custom.tree2.TreeModel;
import org.apache.myfaces.custom.tree2.TreeModelBase;
import org.apache.myfaces.custom.tree2.TreeNode;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.SAXException;
import eu.planets_project.ifr.core.servreg.api.ServiceRegistry;
import eu.planets_project.ifr.core.servreg.api.ServiceRegistryFactory;
import eu.planets_project.ifr.core.storage.api.DataRegistryFactory;
import eu.planets_project.ifr.core.storage.api.DigitalObjectManager.DigitalObjectNotFoundException;
import eu.planets_project.ifr.core.storage.impl.jcr.DOJCRConstants;
import eu.planets_project.ifr.core.storage.impl.oai.OAIDigitalObjectManagerDCBase;
import eu.planets_project.ifr.core.storage.impl.oai.OAIDigitalObjectManagerKBBase;
import eu.planets_project.ifr.core.wdt.impl.data.DetailEntry;
import eu.planets_project.ifr.core.wdt.impl.data.DigitalObjectDirectoryLister;
import eu.planets_project.ifr.core.wdt.impl.data.DigitalObjectDirectoryLister.RegistryType;
import eu.planets_project.ifr.core.wdt.impl.data.DigitalObjectReference;
import eu.planets_project.ifr.core.wee.api.workflow.WorkflowResult;
import eu.planets_project.ifr.core.wee.api.wsinterface.WeeService;
import eu.planets_project.ifr.core.wee.api.wsinterface.WftRegistryService;
import eu.planets_project.services.PlanetsException;
import eu.planets_project.services.datatypes.Agent;
import eu.planets_project.services.datatypes.Content;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.DigitalObjectContent;
import eu.planets_project.services.datatypes.Event;
import eu.planets_project.services.datatypes.Metadata;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.ServiceDescription;

/**
 * @author Ross King class providing access to the workflow status
 */
public class WorkflowBackingBean {

    enum ButtonType {

        PREV, SET, NEXT
    }

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private DigitalObjectDirectoryLister dr = new DigitalObjectDirectoryLister();

    private URI location = null;

    private FileTreeNode[] currentItems;

    FileTreeNode tn = null;

    TreeModel tm;

    private boolean inputDataSelected = false;

    private boolean workflowLoaded = false;

    private boolean workflowStarted = false;

    private UploadedFile myUploadedFile;

    private String workflowName = "none";

    private String workflowStatus = "";

    private String currentTab = "manageTemplateTab";

    private String parameterName = "";

    private String parameterValue = "";

    private ArrayList<DigitalObjectReference> selectedObjects;

    private ArrayList<DetailEntry> detailEntries;

    private ArrayList<DigitalObject> digObjs;

    private ArrayList<ServiceBean> serviceBeans;

    private HashMap<String, String> serviceTypes;

    private HashMap<String, String> serviceNameMap;

    private HashMap<String, ServiceBean> serviceLookup;

    private String selectedTemplateQname = "none";

    private String xmlWorkflowConfig = "";

    private ArrayList<String> errorMessageString;

    private ServiceBean sbiq;

    private HtmlDataTable parameterTable;

    private String newValue = "";

    private WftRegistryService wftRegImp;

    private WeeService weeService;

    private UUID workflowUUID = null;

    private SAXBuilder builder;

    private ServiceRegistry registry;

    private ArrayList<SubmittedWorkflowBean> submittedWorkflows;

    private HashMap<UUID, SubmittedWorkflowBean> workflowLookup;

    private int dataScrollerIndex = 0;

    private boolean disable = true;

    private String display = "display:none";

    private static URI currentNode = null;

    public WorkflowBackingBean() {
        errorMessageString = new ArrayList<String>();
        try {
            Context ctx = new javax.naming.InitialContext();
            wftRegImp = (WftRegistryService) PortableRemoteObject.narrow(ctx.lookup("planets-project.eu/WftRegistryService/remote"), WftRegistryService.class);
            weeService = (WeeService) PortableRemoteObject.narrow(ctx.lookup("planets-project.eu/WeeService/remote"), WeeService.class);
        } catch (NamingException e) {
            errorMessageString.add("Unable to retrieve Workflow Services.");
        }
        registry = ServiceRegistryFactory.getServiceRegistry();
        builder = new SAXBuilder();
        selectedObjects = new ArrayList<DigitalObjectReference>();
        detailEntries = new ArrayList<DetailEntry>();
        digObjs = new ArrayList<DigitalObject>();
        serviceBeans = new ArrayList<ServiceBean>();
        serviceLookup = new HashMap<String, ServiceBean>();
        serviceNameMap = new HashMap<String, String>();
        submittedWorkflows = new ArrayList<SubmittedWorkflowBean>();
        workflowLookup = new HashMap<UUID, SubmittedWorkflowBean>();
        serviceTypes = new HashMap<String, String>();
        serviceTypes.put("Characterise", "eu.planets_project.services.characterise.Characterise");
        serviceTypes.put("Compare", "eu.planets_project.services.compare.Compare");
        serviceTypes.put("Identify", "eu.planets_project.services.identify.Identify");
        serviceTypes.put("Migrate", "eu.planets_project.services.migrate.Migrate");
        serviceTypes.put("Validate", "eu.planets_project.services.validate.Validate");
        serviceTypes.put("Modify", "eu.planets_project.services.modify.Modify");
        serviceTypes.put("CreateView", "eu.planets_project.services.view.CreateView");
        serviceTypes.put("ViewAction", "eu.planets_project.services.view.ViewAction");
        tn = new FileTreeNode(dr.getRootDigitalObject());
        tn.setType("folder");
        tn.setLeaf(false);
        tn.setExpanded(true);
        tm = new TreeModelBase(tn);
        logger.info("### WorkflowBackingBean: Constructor() before .list() uri: null ");
        this.getChildItems(tm, tn, dr.list(null), 1);
    }

    public void updateParameter() {
        errorMessageString.clear();
        if (newValue == null) {
            errorMessageString.add("New value is null - cannot update!");
            return;
        }
        if (newValue.equals("")) {
            errorMessageString.add("Invalid new value - cannot update!");
            return;
        }
        int dataRow = parameterTable.getRowIndex();
        String pn = sbiq.getServiceParameters().get(dataRow).getName();
        sbiq.getServiceParameters().remove(dataRow);
        sbiq.getServiceParameters().add(dataRow, new ServiceParameter(pn, newValue));
        workflowStatus = " (in memory)";
    }

    public void removeParameter() {
        int dataRow = parameterTable.getRowIndex();
        sbiq.getServiceParameters().remove(dataRow);
        workflowStatus = " (in memory)";
    }

    public void addParameter(ActionEvent event) {
        errorMessageString.clear();
        if (sbiq == null) {
            errorMessageString.add("No ServiceBean selected!");
            return;
        }
        if (parameterName.equals("")) {
            errorMessageString.add("Unable to create new parameter: name undefined!");
            return;
        }
        if (parameterValue.equals("")) {
            errorMessageString.add("Unable to create new parameter: value undefined!");
            return;
        }
        sbiq.addParameter(new ServiceParameter(parameterName, parameterValue));
        workflowStatus = " (in memory)";
    }

    public void buildXMLWorkflowDescription(ActionEvent event) {
        this.currentTab = "editWorkflowTab";
        errorMessageString.clear();
        String docString = buildXMLString();
        try {
            byte[] docBytes = docString.getBytes("UTF-8");
            ByteArrayInputStream input = new ByteArrayInputStream(docBytes);
            String fn = selectedTemplateQname.substring(selectedTemplateQname.lastIndexOf('.') + 1) + "_workflow.xml";
            download(input, fn);
            workflowName = fn;
            workflowStatus = " (from file)";
        } catch (IOException e) {
            e.printStackTrace();
            workflowStatus = " (in memory)";
            errorMessageString.add("Unable to write generated XML to the output stream!");
        }
    }

    public void clearWorkflow(ActionEvent event) {
        this.currentTab = "editWorkflowTab";
        selectedTemplateQname = "none";
        workflowStarted = false;
        workflowName = "none";
        workflowStatus = "";
        serviceBeans.clear();
        serviceLookup.clear();
    }

    public void executeWorkflow() {
        errorMessageString.clear();
        xmlWorkflowConfig = buildXMLString();
        if (xmlWorkflowConfig.equals("")) {
            errorMessageString.add("Unable to build workflow XML String - cannote execute!");
            return;
        }
        try {
            workflowUUID = weeService.submitWorkflow(digObjs, selectedTemplateQname, xmlWorkflowConfig);
            SubmittedWorkflowBean swb = new SubmittedWorkflowBean();
            swb.setStartTime(System.currentTimeMillis());
            swb.setStopTime(0l);
            swb.setUuid(workflowUUID);
            swb.setXmlConfigName(workflowName + workflowStatus);
            swb.setNumberObjects(digObjs.size());
            swb.setStatus("SUBMITTED");
            submittedWorkflows.add(swb);
            workflowLookup.put(workflowUUID, swb);
            workflowStarted = true;
            this.currentTab = "listWorkflowsTab";
        } catch (Exception e) {
            e.printStackTrace();
            errorMessageString.add("Error during workflow submission.");
        }
    }

    public String getCurrentProgress() {
        String progString = "None Active";
        if (workflowUUID != null) {
            SubmittedWorkflowBean swb = workflowLookup.get(workflowUUID);
            try {
                String status = weeService.getStatus(workflowUUID);
                swb.setStatus(status);
                progString = status;
                if (status.equals("COMPLETED") || status.equals("FAILED")) {
                    WorkflowResult wr = weeService.getResult(workflowUUID);
                    String rurl = wr.getReport().toString();
                    System.out.println("Got a report URL: " + rurl);
                    swb.setStopTime(System.currentTimeMillis());
                    swb.setReportURL(rurl);
                    swb.setReportExists(true);
                    workflowUUID = null;
                    workflowStarted = false;
                }
            } catch (Exception e) {
                System.out.println("Unidentified UUID: " + workflowUUID);
                errorMessageString.add("Unable to retrieve workflow status from execution service.");
            }
        }
        return progString;
    }

    public void serviceSelectionChanged(ValueChangeEvent event) {
        this.currentTab = "editWorkflowTab";
        errorMessageString.clear();
        HtmlSelectOneMenu sel = (HtmlSelectOneMenu) event.getComponent();
        String selServiceId = sel.getLabel();
        String selServiceEndpoint = (String) event.getNewValue();
        ServiceBean theServiceBean = serviceLookup.get(selServiceId);
        if (theServiceBean == null) {
            errorMessageString.add("Unable to lookup service bean with ID: " + selServiceId);
            return;
        }
        if (selServiceEndpoint.equals("None")) {
            theServiceBean.setServiceName("None");
            theServiceBean.setServiceEndpoint("");
            return;
        }
        String selServiceName = serviceNameMap.get(selServiceEndpoint);
        if (selServiceName == null) {
            errorMessageString.add("Unable to lookup service name for endpoint: " + selServiceEndpoint);
            return;
        }
        theServiceBean.clearParameters();
        URL sendsURL;
        try {
            sendsURL = new URL(selServiceEndpoint);
            List<ServiceDescription> regSer = registry.query(new ServiceDescription.Builder(null, null).endpoint(sendsURL).build());
            if (regSer.size() < 1) {
                errorMessageString.add("No service with endpoint: " + selServiceEndpoint + " found in service registry.");
            } else if (regSer.size() > 1) {
                errorMessageString.add("Service lookup with endpoint: " + selServiceEndpoint + " yielded more than one result!");
            } else {
                ServiceDescription sd = regSer.get(0);
                List<Parameter> pList = sd.getParameters();
                if (pList != null) {
                    Iterator<Parameter> it = pList.iterator();
                    while (it.hasNext()) {
                        Parameter par = it.next();
                        ServiceParameter spar = new ServiceParameter(par.getName(), par.getValue());
                        theServiceBean.addParameter(spar);
                    }
                } else {
                    errorMessageString.add("Service: " + selServiceName + " has no default parameters.");
                }
            }
        } catch (MalformedURLException e) {
            errorMessageString.add("Unable to lookup service with endpoint: " + selServiceEndpoint);
        }
        theServiceBean.setServiceName(selServiceName);
        theServiceBean.setServiceEndpoint(selServiceEndpoint);
        workflowStatus = " (in memory)";
    }

    public String processFile() {
        this.currentTab = "loadWorkflowTab";
        errorMessageString.clear();
        InputStream in;
        try {
            in = new BufferedInputStream(myUploadedFile.getInputStream());
        } catch (Exception x) {
            errorMessageString.add("\nError retrieving workflow file!");
            return null;
        }
        String retFromValidate = validateXML(in);
        if (!retFromValidate.equals("")) {
            errorMessageString.add("Input XML invalid against schema: " + retFromValidate);
            return null;
        }
        Document doc = null;
        try {
            in = new BufferedInputStream(myUploadedFile.getInputStream());
            doc = builder.build(in);
        } catch (JDOMException e) {
            errorMessageString.add("Error parsing workflow file - invalid XML?");
            return null;
        } catch (IOException e) {
            errorMessageString.add("Error retrieving workflow file while trying to build JDOM document!");
            return null;
        }
        if (doc != null) {
            serviceBeans.clear();
            serviceLookup.clear();
            String templateQName = "";
            Element rootElement = doc.getRootElement();
            try {
                templateQName = rootElement.getChild("template").getChild("class").getText();
            } catch (NullPointerException npe) {
                errorMessageString.add("Unable to find template 'class' Element - workflow XML is invalid?");
                return null;
            }
            if (!wftRegImp.getAllSupportedQNames().contains(templateQName)) {
                errorMessageString.add("Template " + templateQName + " must be registered before this workflow can be loaded.");
                return null;
            } else {
                selectedTemplateQname = templateQName;
                errorMessageString.add("Template " + templateQName + " is selected by loading this workflow.");
            }
            Element services = rootElement.getChild("services");
            Iterator<Element> sit = services.getChildren("service").iterator();
            while (sit.hasNext()) {
                Element sel = sit.next();
                Attribute sidAtt = sel.getAttribute("id");
                if (sidAtt != null) {
                    String sid = sidAtt.getValue();
                    ServiceBean sb = new ServiceBean(sid);
                    Element send = sel.getChild("endpoint");
                    if (send != null) {
                        String sendS = send.getValue();
                        sb.setServiceEndpoint(sendS);
                        try {
                            URL sendsURL = new URL(sendS);
                            List<ServiceDescription> regSer = registry.query(new ServiceDescription.Builder(null, null).endpoint(sendsURL).build());
                            if (regSer.size() < 1) {
                                errorMessageString.add("Unable to find service corresponding to endpoint: " + sendS);
                            } else if (regSer.size() > 1) {
                                errorMessageString.add("Multiple services corresponding to endpoint: " + sendS);
                            } else {
                                ServiceDescription sdesc = regSer.get(0);
                                sb.setServiceName(sdesc.getName());
                                String serType = sdesc.getType();
                                sb.setServiceType(serType.substring(serType.lastIndexOf('.') + 1));
                            }
                        } catch (Exception ex) {
                            errorMessageString.add("Unable lookup service endpoint: " + sendS);
                            ex.printStackTrace();
                        }
                    } else {
                        errorMessageString.add("Found a service element with no endpoint!");
                    }
                    Element params = sel.getChild("parameters");
                    if (params != null) {
                        Iterator<Element> pit = params.getChildren("param").iterator();
                        while (pit.hasNext()) {
                            Element pel = pit.next();
                            Element pname = pel.getChild("name");
                            if (pname != null) {
                                Element pvalue = pel.getChild("value");
                                if (pvalue != null) {
                                    ServiceParameter sp = new ServiceParameter(pname.getValue(), pvalue.getValue());
                                    sb.addParameter(sp);
                                }
                            }
                        }
                    }
                    serviceBeans.add(sb);
                    serviceLookup.put(sid, sb);
                } else {
                    errorMessageString.add("Found a service element with no id!");
                }
            }
        } else {
            errorMessageString.add("Null Document - workflow XML is invalid?");
            return null;
        }
        try {
            in = new BufferedInputStream(myUploadedFile.getInputStream());
            xmlWorkflowConfig = new String(inputStreamToBytes(in));
        } catch (IOException e) {
            errorMessageString.add("Unable to create XML String from input stream!");
            return null;
        }
        workflowName = myUploadedFile.getName();
        workflowLoaded = true;
        workflowStatus = " (from File)";
        return "OK";
    }

    public String processTemplate() {
        this.currentTab = "manageTemplateTab";
        errorMessageString.clear();
        InputStream in;
        byte[] classBytes;
        String templateName = myUploadedFile.getName();
        String className = "";
        int i = templateName.indexOf(".java");
        if (i > -1) {
            className = templateName.substring(0, i);
        } else {
            errorMessageString.add("Templates must be Java source files!");
            return null;
        }
        try {
            in = new BufferedInputStream(myUploadedFile.getInputStream());
        } catch (Exception x) {
            errorMessageString.add("Error retrieving template file input stream!");
            return null;
        }
        try {
            classBytes = inputStreamToBytes(in);
        } catch (IOException e1) {
            errorMessageString.add("Unable to extract template byte array from input stream!");
            return null;
        }
        String packageName = extractPackage(classBytes);
        String qName = packageName + "." + className;
        if (wftRegImp.getAllSupportedQNames().contains(qName)) {
            errorMessageString.add("Template " + qName + " is already registered!");
            return null;
        }
        try {
            wftRegImp.registerWorkflowTemplate(qName, classBytes);
        } catch (PlanetsException e) {
            e.printStackTrace();
            errorMessageString.add("Unable to register template!");
            return null;
        }
        return "OK";
    }

    /**
	 * Controller that selects all of the current items.
	 */
    public String selectAll() {
        this.currentTab = "selectObjectsTab";
        if (currentItems != null) {
            for (FileTreeNode dob : currentItems) {
                if (dob.isSelectable()) dob.setSelected(true);
            }
            return "success";
        } else {
            return null;
        }
    }

    /**
	 * Controller that de-selects the current items.
	 */
    public String selectNone() {
        this.currentTab = "selectObjectsTab";
        if (currentItems != null) {
            for (FileTreeNode dob : currentItems) {
                if (dob.isSelectable()) dob.setSelected(false);
            }
            return "success";
        } else {
            return null;
        }
    }

    /**
	 * Controller that adds the currently selected items to the workflow.
	 */
    public String addToWorkflow() {
        this.currentTab = "selectObjectsTab";
        errorMessageString.clear();
        if (currentItems != null) {
            for (FileTreeNode dob : currentItems) {
                if (dob.isSelectable() && dob.isSelected()) {
                    DigitalObjectReference dor = new DigitalObjectReference(dob.getUri());
                    URI dobURI = dob.getUri();
                    DigitalObject o = null;
                    try {
                        if (dobURI.toString().contains(OAIDigitalObjectManagerDCBase.REGISTRY_NAME) || dobURI.toString().contains(OAIDigitalObjectManagerKBBase.REGISTRY_NAME) || dobURI.toString().contains(DOJCRConstants.REGISTRY_NAME)) {
                            logger.info("addToWorkflow() special dobURI: " + dobURI);
                            o = dr.getDataManager(dobURI).retrieve(dobURI);
                        } else {
                            logger.info("addToWorkflow() dobURI: " + dobURI);
                            o = dr.getDataManager(dobURI).retrieve(dobURI);
                            InputStream streamContent = o.getContent().getInputStream();
                            byte[] byteContent = null;
                            try {
                                byteContent = IOUtils.toByteArray(streamContent);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            DigitalObjectContent content = Content.byValue(byteContent);
                            o = (new DigitalObject.Builder(o)).content(content).title(dor.getLeafname()).permanentUri(dobURI).build();
                        }
                        logger.info("adding file name: " + dor.getLeafname());
                    } catch (DigitalObjectNotFoundException e) {
                        errorMessageString.add("\nUnable to retrieve selected digital object!");
                        e.printStackTrace();
                    }
                    if (o != null) {
                        selectedObjects.add(dor);
                        digObjs.add(o);
                    } else {
                        errorMessageString.add("\nRetrieved digital object is null for URI: " + dobURI);
                    }
                }
            }
            if (selectedObjects.size() > 0) inputDataSelected = true;
            selectNone();
            return "back";
        } else {
            return null;
        }
    }

    /**
	 * This method fills the digital object details in the list.
	 * 
	 * @param o
	 *        This is a digital object selected from the objects tree
	 */
    public void fillDetails(DigitalObject o) {
        detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_TITLE, o.getTitle()));
        if (o.getPermanentUri() != null) {
            detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_PERMANENT_URI, o.getPermanentUri().toString()));
        }
        if (o.getFormat() != null) {
            detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_FORMAT_URI, o.getFormat().toString()));
        }
        Iterator<Metadata> iterMetadata = o.getMetadata().iterator();
        while (iterMetadata.hasNext()) {
            Metadata metadata = iterMetadata.next();
            if (metadata != null) fillMetadata(metadata);
        }
        Iterator<Event> iterEvents = o.getEvents().iterator();
        while (iterEvents.hasNext()) {
            Event event = iterEvents.next();
            if (event != null) fillEvent(event);
        }
    }

    /**
	 * This method fills the digital object event properties in the details list.
	 * 
	 * @param event
	 *        This is a digital object event
	 */
    public void fillProperties(Event event) {
        Iterator<eu.planets_project.services.datatypes.Property> iterProperties = event.getProperties().iterator();
        while (iterProperties.hasNext()) {
            eu.planets_project.services.datatypes.Property property = iterProperties.next();
            if (property.getUri() != null) {
                detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_PROPERTY_URI, property.getUri().toString()));
            }
            detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_PROPERTY_NAME, property.getName()));
            detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_PROPERTY_VALUE, property.getValue()));
            detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_PROPERTY_DESCRIPTION, property.getDescription()));
            detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_PROPERTY_UNIT, property.getUnit()));
            detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_PROPERTY_TYPE, property.getType()));
        }
    }

    /**
	 * This method fills the digital object metadata in the details list.
	 * 
	 * @param metadata
	 *        This is a digital object metadata
	 */
    public void fillMetadata(Metadata metadata) {
        if (metadata.getType() != null) {
            detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_METADATA_TYPE, metadata.getType().toString()));
        }
        detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_METADATA_CONTENT, metadata.getContent()));
        detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_METADATA_NAME, metadata.getName()));
    }

    /**
	 * This method fills the digital object event agent in the details list.
	 * 
	 * @param agent
	 *        This is a digital object event agent
	 */
    public void fillAgent(Agent agent) {
        detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_AGENT_ID, agent.getId()));
        detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_AGENT_NAME, agent.getName()));
        if (agent.getType() != null) {
            detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_AGENT_TYPE, agent.getType()));
        }
    }

    /**
	 * This method fills the digital object events in the details list.
	 * 
	 * @param event
	 *        This is a digital object event 
	 */
    public void fillEvent(Event event) {
        detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_SUMMARY, event.getSummary()));
        detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_DATETIME, event.getDatetime()));
        detailEntries.add(new DetailEntry(DOJCRConstants.PREMIS_EVENT_DURATION, Double.toString(event.getDuration())));
        Agent agent = event.getAgent();
        if (agent != null) fillAgent(agent);
        if (event.getProperties() != null) fillProperties(event);
    }

    /**
	 * This method represents the details of particular digital object.
	 */
    public String showDetails() {
        this.currentTab = "selectObjectsTab";
        errorMessageString.clear();
        if (currentItems != null) {
            for (FileTreeNode dob : currentItems) {
                if (dob.isSelectable() && dob.isSelected()) {
                    URI dobURI = dob.getUri();
                    DigitalObject o = null;
                    if (dobURI.toString().contains(OAIDigitalObjectManagerDCBase.REGISTRY_NAME) || dobURI.toString().contains(OAIDigitalObjectManagerKBBase.REGISTRY_NAME) || dobURI.toString().contains(DOJCRConstants.REGISTRY_NAME)) {
                        logger.info("showDetails() special dobURI: " + dobURI);
                        try {
                            o = dr.getDataManager(dobURI).retrieve(dobURI);
                        } catch (Exception e) {
                            logger.info("showDetails() after OAI retrieve error: " + e.getMessage());
                        }
                    }
                    if (o != null) {
                        fillDetails(o);
                    } else {
                        errorMessageString.add("\nRetrieved digital object is null for URI: " + dobURI);
                    }
                }
            }
            selectNone();
            return "back";
        } else {
            return null;
        }
    }

    public String getNext() {
        logger.info("getNext()");
        calculateNodes(ButtonType.NEXT);
        return null;
    }

    /**
	 * Controller that selects the needed page.
	 */
    public String scrollEvent() {
        logger.info("scrollEvent() dataScrollerIndex: " + dataScrollerIndex);
        calculateNodes(ButtonType.SET);
        return "success";
    }

    public String getPrev() {
        logger.info("getPrev()");
        calculateNodes(ButtonType.PREV);
        return null;
    }

    public void calculateNodes(ButtonType bt) {
        logger.info("calculateNodes()");
        List<FileTreeNode> cchilds = (List<FileTreeNode>) tn.getChildren();
        if (cchilds != null) {
            logger.info("calculateNodes() cchilds.size: " + cchilds.size());
            for (FileTreeNode tfn : cchilds) {
                logger.info("calculateNodes() tfn.getUri(): " + tfn.getUri());
                if (dr.isOaiRegistry(tfn.getUri()) && currentNode != null && currentNode.equals(tfn.getUri())) {
                    switch(bt) {
                        case PREV:
                            dr.decreaseDorIndex();
                            if (dataScrollerIndex > 0) {
                                dataScrollerIndex--;
                            }
                            break;
                        case SET:
                            dr.changeDorIndex(dataScrollerIndex);
                            break;
                        case NEXT:
                            dr.increaseDorIndex();
                            dataScrollerIndex++;
                            break;
                        default:
                            break;
                    }
                    logger.info("calculateNodes() dataScrollerIndex: " + dataScrollerIndex);
                    setLocation(tfn.getUri());
                    tfn.setExpanded(true);
                    logger.info("### WorkflowBackingBean: calculateNodes() before .list() getLocation: " + getLocation());
                    this.getChildItems(tm, tfn, dr.list(getLocation()), 1);
                    this.currentTab = "selectObjectsTab";
                    break;
                }
            }
        }
    }

    /**
	 * Controller that removed all selected items from the workflow.
	 */
    public String clearObjects() {
        this.currentTab = "selectObjectsTab";
        selectedObjects.clear();
        digObjs.clear();
        inputDataSelected = false;
        return "back";
    }

    /**
	 * Controller that removes all details entries from the detailEntries list.
	 */
    public String clearDetails() {
        this.currentTab = "selectObjectsTab";
        detailEntries.clear();
        return "back";
    }

    public void selectTemplate(ActionEvent event) {
        this.currentTab = "editWorkflowTab";
        errorMessageString.clear();
        String tempQName = (String) event.getComponent().getAttributes().get("value");
        if (!selectedTemplateQname.equals(tempQName)) {
            selectedTemplateQname = tempQName;
            byte[] templateBytes;
            try {
                templateBytes = wftRegImp.getWFTemplate(selectedTemplateQname);
            } catch (PlanetsException e) {
                errorMessageString.add("Unable to retrieve template source for QName: " + selectedTemplateQname);
                return;
            }
            populateServiceBeans(templateBytes);
        }
    }

    public String getParameterName() {
        return this.parameterName;
    }

    public String getParameterValue() {
        return this.parameterValue;
    }

    public List<ServiceBean> getServiceBeans() {
        return serviceBeans;
    }

    public ArrayList<String> getErrorMessageString() {
        return errorMessageString;
    }

    public boolean getButtonRendered() {
        return !workflowStarted;
    }

    /**
	 * @return inputDataSelected
	 */
    public boolean getInputDataSelected() {
        return inputDataSelected;
    }

    public List<DigitalObjectReference> getObjects() {
        return selectedObjects;
    }

    /**
	 * This method returns a list of details for selected digital object.
	 * 
	 * @return detailEntries
	 *         This is a list of digital object detail entries
	 */
    public List<DetailEntry> getDetails() {
        return detailEntries;
    }

    /**
	 * @return workflowLoaded
	 */
    public boolean getWorkflowLoaded() {
        return workflowLoaded;
    }

    /**
	 * @return workflowStarted
	 */
    public boolean getWorkflowStarted() {
        return workflowStarted;
    }

    public UploadedFile getMyUploadedFile() {
        return myUploadedFile;
    }

    public ArrayList<String> getTemplates() {
        return wftRegImp.getAllSupportedQNames();
    }

    public ArrayList<SubmittedWorkflowBean> getSubmittedWorkflows() {
        return submittedWorkflows;
    }

    public String getWorkflowName() {
        return this.workflowName;
    }

    public String getWorkflowStatus() {
        return this.workflowStatus;
    }

    public String getCurrentTab() {
        return this.currentTab;
    }

    public String getSelectedTemplateQname() {
        return selectedTemplateQname;
    }

    public HtmlDataTable getParameterTable() {
        return parameterTable;
    }

    public String getNewValue() {
        return newValue;
    }

    public boolean getCannotExecute() {
        if ((!selectedTemplateQname.equals("none")) && workflowLoaded && inputDataSelected) {
            return false;
        } else {
            return true;
        }
    }

    public boolean getDisableWorkflowButtons() {
        if (serviceBeans.size() > 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
	 * This method hides the buttons panel if needed.
	 * @return display type
	 */
    public String getDisplay() {
        display = "display:none";
        if (!disable) {
            display = "display:inline";
        }
        return display;
    }

    /**
	 * Sends back a list of the DOs under the current URI
	 * 
	 * @return
	 */
    public FileTreeNode[] getList() {
        return this.currentItems;
    }

    /**
	 * @return the location
	 */
    public URI getLocation() {
        return location;
    }

    /**
	 * Check if the current location has a parent:
	 * 
	 * @return
	 */
    public boolean getParentExists() {
        return dr.canAccessURI(this.getParentUri());
    }

    /**
	 * Return the string used to denote the parent URI:
	 * 
	 * @return
	 */
    public String getParentName() {
        return "..";
    }

    /**
	 * Return the parent URI:
	 * 
	 * @return
	 */
    public URI getParentUri() {
        if (this.location == null) return this.location;
        return this.location.resolve("..").normalize();
    }

    /**
	 * Backing for the Tomahawk Tree2 I'm using for displaying the filer tree.
	 * 
	 * @return A TreeModel holding the directory structure.
	 */
    public TreeModel getFilerTree() {
        return tm;
    }

    /**
	 * Add the childs...
	 * 
	 * @param tm
	 * @param parent
	 * @param dobs
	 * @param depth
	 */
    private void getChildItems(TreeModel tm, TreeNode parent, DigitalObjectReference[] dobs, int depth) {
        if (dobs == null) return;
        if (dobs.length == 0) return;
        for (DigitalObjectReference dob : dobs) {
            if (dob.isDirectory()) {
                FileTreeNode cnode = new FileTreeNode(dob);
                List<FileTreeNode> cchilds = (List<FileTreeNode>) parent.getChildren();
                if (!cchilds.contains(cnode)) cchilds.add(cnode);
                if (dob.isDirectory() && depth > 0) {
                    logger.info("### WorkflowBackingBean: getChildItems() before .list() dob.getUri: " + dob.getUri());
                    RegistryType rt = dr.getRegistryType(dob.getUri());
                    switch(rt) {
                        case OAIDC:
                            logger.info("### WorkflowBackingBean: getChildItems() case OAIDC");
                            try {
                                dr.refreshChilds(DataRegistryFactory.createDataRegistryIdFromName(OAIDigitalObjectManagerDCBase.REGISTRY_NAME));
                            } catch (Exception e) {
                                logger.info("WorkflowBackingBean getChildItems() error: " + e.getMessage());
                            }
                            break;
                        case OAIKB:
                            logger.info("### WorkflowBackingBean: getChildItems() case OAIKB");
                            try {
                                dr.refreshChilds(DataRegistryFactory.createDataRegistryIdFromName(OAIDigitalObjectManagerKBBase.REGISTRY_NAME));
                            } catch (Exception e) {
                                logger.info("WorkflowBackingBean getChildItems() error: " + e.getMessage());
                            }
                            break;
                        default:
                            break;
                    }
                    this.getChildItems(tm, cnode, dr.list(dob.getUri()), depth - 1);
                }
            }
        }
    }

    public String getEditedSBId() {
        this.currentTab = "editWorkflowTab";
        errorMessageString.clear();
        String selectedRecordId = getParamValue("id");
        if (selectedRecordId == null) return sbiq.getServiceId();
        if (selectedRecordId.equals("")) return sbiq.getServiceId();
        ServiceBean sb = serviceLookup.get(selectedRecordId);
        if (sb != null) {
            sbiq = sb;
            return selectedRecordId;
        }
        errorMessageString.add("Unable to identify a service Id!");
        return "";
    }

    public List<ServiceParameter> getServiceParametersToEdit() {
        List<ServiceParameter> sps = new ArrayList<ServiceParameter>();
        if (sbiq != null) {
            sps = sbiq.getServiceParameters();
        }
        return sps;
    }

    public void setParameterTable(HtmlDataTable parameterTable) {
        this.parameterTable = parameterTable;
    }

    public void setNewValue(String value) {
        this.newValue = value;
    }

    public void setParameterName(String name) {
        this.parameterName = name;
    }

    public void setParameterValue(String value) {
        this.parameterValue = value;
    }

    public void setMyUploadedFile(UploadedFile myUploadedFile) {
        this.myUploadedFile = myUploadedFile;
    }

    public void setDir(FileTreeNode tfn) {
        if (tfn != null) {
            logger.info("setDir() uri: " + tfn.getUri());
            dr.resetDorIndex();
            dataScrollerIndex = 0;
            if (tfn.getUri() != null) {
                RegistryType rt = dr.getRegistryType(tfn.getUri());
                switch(rt) {
                    case OAIDC:
                        disable = false;
                        currentNode = tfn.getUri();
                        break;
                    case OAIKB:
                        disable = false;
                        currentNode = tfn.getUri();
                        break;
                    default:
                        disable = true;
                        currentNode = null;
                        break;
                }
                logger.info("WorkflowBackingBean setDir() set disable for buttons: " + disable);
            }
        }
        setLocation(tfn.getUri());
        tfn.setExpanded(true);
        logger.info("### WorkflowBackingBean: setDir() before .list() getLocation(): " + getLocation());
        this.getChildItems(tm, tfn, dr.list(getLocation()), 1);
        this.currentTab = "selectObjectsTab";
    }

    /**
	 * @param location
	 *            the location to set
	 */
    public void setLocation(URI location) {
        logger.fine("Setting location: " + location);
        if (location != null) this.location = location.normalize();
        logger.info("### WorkflowBackingBean: setLocation() before .list() this.location: " + this.location);
        DigitalObjectReference[] dobs = dr.list(this.location);
        int fileCount = 0;
        for (DigitalObjectReference dob : dobs) {
            if (!dob.isDirectory()) fileCount++;
        }
        this.currentItems = new FileTreeNode[dobs.length];
        int i = 0;
        for (DigitalObjectReference dob : dobs) {
            if (dob.isDirectory()) {
                this.currentItems[i] = new FileTreeNode(dob);
                i++;
            }
        }
        for (DigitalObjectReference dob : dobs) {
            if (!dob.isDirectory()) {
                this.currentItems[i] = new FileTreeNode(dob);
                i++;
            }
        }
    }

    private byte[] inputStreamToBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
        return out.toByteArray();
    }

    private String extractPackage(byte[] buffer) {
        String packageName = "";
        String classString = "";
        try {
            classString = new String(buffer, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            errorMessageString.add("Unable to extract UTF-8 String from byte array!");
            e.printStackTrace();
        }
        int i = classString.indexOf("package ");
        if (i > -1) {
            int j = classString.indexOf(";", i);
            if (j > i + 7) {
                packageName = classString.substring(i + 8, j);
            }
        }
        i = classString.indexOf("implements WorkflowTemplate");
        if (i < 0) {
            errorMessageString.add("WARNING: Template does not appear to implement the WorkflowTemplate interface! Invalid?");
        }
        return packageName;
    }

    private String validateXML(InputStream documentFile) {
        String errorString = "";
        InputStream bis = getClass().getClassLoader().getResourceAsStream("planets_wdt.xsd");
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(new StreamSource(bis));
            Validator validator = schema.newValidator();
            XMLOutputter outputter = new XMLOutputter();
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(documentFile);
            validator.validate(new StreamSource(new StringReader(outputter.outputString(doc.getRootElement()))));
        } catch (SAXException e) {
            errorString += e.getMessage();
        } catch (JDOMException e) {
            errorString += e.getMessage();
        } catch (IOException e) {
            errorString += e.getMessage();
        }
        return errorString;
    }

    private String getParamValue(String s) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String value = facesContext.getExternalContext().getRequestParameterMap().get(s);
        return value;
    }

    private void populateServiceBeans(byte[] template) {
        String temp = new String(template);
        serviceBeans.clear();
        serviceLookup.clear();
        Iterator<String> iter = serviceTypes.keySet().iterator();
        while (iter.hasNext()) {
            String serviceType = iter.next();
            Pattern pattern = Pattern.compile(serviceType + "\\s(\\S*);");
            Matcher matcher = pattern.matcher(temp);
            while (matcher.find()) {
                String sid = matcher.group(1);
                ServiceBean sb = new ServiceBean(sid);
                sb.setServiceType(serviceType);
                serviceBeans.add(sb);
                serviceLookup.put(sid, sb);
            }
        }
    }

    private String buildXMLString() {
        String docString = "";
        Element rootElement = new Element("workflowConf");
        rootElement.addNamespaceDeclaration(Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
        rootElement.setAttribute("noNamespaceSchemaLocation", "planets_wdt.xsd", Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
        Document workflowDoc = new Document(rootElement);
        Element templateEl = new Element("template");
        Element servicesEl = new Element("services");
        rootElement.addContent(templateEl);
        rootElement.addContent(servicesEl);
        templateEl.addContent(new Element("class").addContent(selectedTemplateQname));
        if (serviceBeans != null) {
            Iterator<ServiceBean> it1 = serviceBeans.iterator();
            while (it1.hasNext()) {
                ServiceBean sb = it1.next();
                Element serviceEl = new Element("service");
                servicesEl.addContent(serviceEl);
                serviceEl.setAttribute(new Attribute("id", sb.getServiceId()));
                serviceEl.addContent(new Element("endpoint").addContent(sb.getServiceEndpoint()));
                List<ServiceParameter> sbServiceParamters = sb.getServiceParameters();
                if (sbServiceParamters != null) {
                    Iterator<ServiceParameter> it2 = sbServiceParamters.iterator();
                    Element paramEl = null;
                    if (it2.hasNext()) {
                        paramEl = new Element("parameters");
                        serviceEl.addContent(paramEl);
                    }
                    while (it2.hasNext()) {
                        ServiceParameter sp = it2.next();
                        Element parEl = new Element("param");
                        paramEl.addContent(parEl);
                        parEl.addContent(new Element("name").addContent(sp.name));
                        parEl.addContent(new Element("value").addContent(sp.value));
                    }
                }
            }
        }
        try {
            StringWriter sw = new StringWriter();
            XMLOutputter outputter = new XMLOutputter();
            outputter.setFormat(Format.getPrettyFormat());
            outputter.output(workflowDoc, sw);
            docString = sw.toString();
        } catch (IOException e) {
            e.printStackTrace();
            errorMessageString.add("Unable to write generated XML to a String!");
        }
        return docString;
    }

    public String download(InputStream is, String theFilename) {
        final FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse res = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        try {
            res.setHeader("Pragma", "no-cache");
            res.setDateHeader("Expires", 0);
            res.setContentType("text/xml");
            res.setHeader("Content-disposition", "attachment; filename=" + theFilename);
            fastChannelCopy(Channels.newChannel(is), Channels.newChannel(res.getOutputStream()));
        } catch (final IOException e) {
        }
        facesContext.responseComplete();
        return null;
    }

    private void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        while (src.read(buffer) != -1) {
            buffer.flip();
            dest.write(buffer);
            buffer.compact();
        }
        buffer.flip();
        while (buffer.hasRemaining()) {
            dest.write(buffer);
        }
    }

    public int getDataScrollerIndex() {
        return dataScrollerIndex;
    }

    public void setDataScrollerIndex(int dataScrollerIndex) {
        this.dataScrollerIndex = dataScrollerIndex;
    }

    public boolean isDisable() {
        return disable;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }

    public class ServiceBean {

        private String serviceId;

        private String serviceType;

        private String serviceName;

        private String serviceEndpoint;

        private ArrayList<ServiceParameter> serviceParameters;

        private ArrayList<SelectItem> serviceNames;

        public ServiceBean() {
            this.serviceName = "None";
            this.serviceParameters = new ArrayList<ServiceParameter>();
        }

        public ServiceBean(String id) {
            this.serviceId = id;
            this.serviceName = "None";
            this.serviceParameters = new ArrayList<ServiceParameter>();
        }

        public String getServiceId() {
            return serviceId;
        }

        public String getServiceType() {
            return serviceType;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getServiceEndpoint() {
            return serviceEndpoint;
        }

        public List<ServiceParameter> getServiceParameters() {
            return serviceParameters;
        }

        public void setServiceId(String id) {
            this.serviceId = id;
        }

        public void setServiceType(String type) {
            this.serviceType = type;
            serviceNames = new ArrayList<SelectItem>();
            serviceNames.add(new SelectItem("None", "Select an Endpoint..."));
            serviceNames.add(new SelectItem("None", "None"));
            if (serviceType != null) {
                String serviceClass = serviceTypes.get(serviceType);
                List<ServiceDescription> services = registry.query(new ServiceDescription.Builder(null, serviceClass).build());
                Iterator<ServiceDescription> it = services.iterator();
                while (it.hasNext()) {
                    ServiceDescription sd = it.next();
                    serviceNames.add(new SelectItem(sd.getEndpoint().toString(), sd.getName()));
                    serviceNameMap.put(sd.getEndpoint().toString(), sd.getName());
                }
            }
        }

        public void setServiceName(String name) {
            this.serviceName = name;
        }

        public void setServiceEndpoint(String endpoint) {
            this.serviceEndpoint = endpoint;
        }

        public void addParameter(ServiceParameter par) {
            this.serviceParameters.add(par);
        }

        public void removeParameter(ServiceParameter par) {
            this.serviceParameters.remove(par);
        }

        public List<SelectItem> getEndpointOptions() {
            if (serviceNames == null) {
                serviceNames = new ArrayList<SelectItem>();
                serviceNames.add(new SelectItem("None", "Select an Endpoint..."));
                serviceNames.add(new SelectItem("None", "None"));
            }
            return serviceNames;
        }

        public void clearParameters() {
            this.serviceParameters.clear();
        }
    }

    public class ServiceParameter {

        private String name;

        private String value;

        public ServiceParameter() {
        }

        public ServiceParameter(String n, String v) {
            this.name = n;
            this.value = v;
        }

        public String getName() {
            return this.name;
        }

        public String getValue() {
            return this.value;
        }

        public void setName(String n) {
            this.name = n;
        }

        public void setValue(String v) {
            this.value = v;
        }
    }

    public class SubmittedWorkflowBean {

        private UUID uuid;

        private String xmlConfigName;

        private String status;

        private String reportURL;

        private long startTime;

        private long stopTime;

        private int numberObjects;

        private SimpleDateFormat formatter;

        private Boolean reportExists;

        public SubmittedWorkflowBean() {
            formatter = new SimpleDateFormat("MMM yyyy HH:mm:ss");
            this.reportExists = new Boolean(false);
        }

        public String getUuid() {
            return uuid.toString();
        }

        public String getXmlConfigName() {
            return xmlConfigName;
        }

        public String getStatus() {
            return status;
        }

        public String getExecutionTime() {
            if (stopTime > startTime) {
                return new Long(stopTime - startTime).toString();
            } else {
                return "";
            }
        }

        public String getNumberObjects() {
            return new Integer(numberObjects).toString();
        }

        public String getStartTime() {
            if (startTime > 0) {
                String s = formatter.format(new Date(this.startTime));
                return s;
            } else {
                return "";
            }
        }

        public String getStopTime() {
            if (stopTime > 0) {
                String s = formatter.format(new Date(this.stopTime));
                return s;
            } else {
                return "";
            }
        }

        public String getReportURL() {
            return this.reportURL;
        }

        public Boolean getReportExists() {
            return this.reportExists;
        }

        public void setUuid(UUID id) {
            this.uuid = id;
        }

        public void setXmlConfigName(String name) {
            this.xmlConfigName = name;
        }

        public void setStatus(String stat) {
            this.status = stat;
        }

        public void setStartTime(long time) {
            this.startTime = time;
        }

        public void setStopTime(long time) {
            this.stopTime = time;
        }

        public void setNumberObjects(int num) {
            this.numberObjects = num;
        }

        public void setReportURL(String rurl) {
            this.reportURL = rurl;
        }

        public void setReportExists(boolean exists) {
            this.reportExists = new Boolean(exists);
        }
    }
}
