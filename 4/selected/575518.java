package de.dgrid.bisgrid.services.management;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.xml.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.codehaus.xfire.addressing.AddressingInHandler;
import org.codehaus.xfire.handler.AbstractHandler;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.binding.PostInvocationHandler;
import org.codehaus.xfire.util.dom.DOMInHandler;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.oasisOpen.docs.wsrf.rl2.DestroyDocument;
import org.oasisOpen.docs.wsrf.rl2.DestroyResponseDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument;
import org.oasisOpen.docs.wsrf.rl2.SetTerminationTimeDocument.SetTerminationTime;
import org.w3.x2001.xmlSchema.SchemaDocument;
import org.w3.x2005.x08.addressing.EndpointReferenceType;
import org.xml.sax.InputSource;
import org.xmlsoap.schemas.wsdl.DefinitionsDocument;
import x0Policy.oasisNamesTcXacml1.RuleType;
import de.dgrid.bisgrid.common.BISGridConstants;
import de.dgrid.bisgrid.common.BISGridProperties;
import de.dgrid.bisgrid.common.bpel.BPELWorkflowEngine;
import de.dgrid.bisgrid.common.bpel.BPELWorkflowEngineManager;
import de.dgrid.bisgrid.common.bpel.adapter.Adapter;
import de.dgrid.bisgrid.common.bpel.adapter.AdapterFactory;
import de.dgrid.bisgrid.common.bpel.adapter.AdapterModule;
import de.dgrid.bisgrid.common.bpel.adapter.DeploymentAdapter;
import de.dgrid.bisgrid.common.bpel.adapter.DeploymentAdapterResult;
import de.dgrid.bisgrid.common.bpel.adapter.exceptions.BPELWorkflowEngineDeployFault;
import de.dgrid.bisgrid.common.bpel.adapter.exceptions.BPELWorkflowEngineUndeployFault;
import de.dgrid.bisgrid.common.exceptions.BPELWorkflowEngineNotSupportedException;
import de.dgrid.bisgrid.common.exceptions.PolicyDestructionFailedException;
import de.dgrid.bisgrid.common.exceptions.PolicyInstatiationFailedException;
import de.dgrid.bisgrid.common.performance.handler.WorkflowInvokatonPerfMonitoringInHandler;
import de.dgrid.bisgrid.common.performance.handler.WorkflowInvokatonPerfMonitoringOutHandler;
import de.dgrid.bisgrid.common.security.XACMLConfigurationHelper2;
import de.dgrid.bisgrid.common.unicore.handler.HandlerUtils;
import de.dgrid.bisgrid.services.management.deployment.ProcessDocument;
import de.dgrid.bisgrid.services.management.exceptions.DeployFault;
import de.dgrid.bisgrid.services.management.exceptions.IllegalUcServiceEPRException;
import de.dgrid.bisgrid.services.management.exceptions.PatternProcessingException;
import de.dgrid.bisgrid.services.management.exceptions.UndeployFault;
import de.dgrid.bisgrid.services.management.factories.WorkflowManagementServiceFactory;
import de.dgrid.bisgrid.services.management.messages.workflowManagementService.DeployReqDocument;
import de.dgrid.bisgrid.services.management.messages.workflowManagementService.DeployRespDocument;
import de.dgrid.bisgrid.services.management.messages.workflowManagementService.RedeployReqDocument;
import de.dgrid.bisgrid.services.management.messages.workflowManagementService.RedeployRespDocument;
import de.dgrid.bisgrid.services.management.messages.workflowManagementService.TerminationSelectionType;
import de.dgrid.bisgrid.services.management.messages.workflowManagementService.TerminationType;
import de.dgrid.bisgrid.services.management.messages.workflowManagementService.UndeployReqDocument;
import de.dgrid.bisgrid.services.management.messages.workflowManagementService.UndeployRespDocument;
import de.dgrid.bisgrid.services.management.patternprocessing.IPatternProcessingFacade;
import de.dgrid.bisgrid.services.management.patternprocessing.PatternProcessingFacade;
import de.dgrid.bisgrid.services.management.patternprocessing.UcServiceEndpointReference;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.DeploymentPackageDocument;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.DeploymentPackageNameDocument;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.DeploymentResultDocument;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.DeploymentWorkflowNameDocument;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.IsDeployedDocument;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.OtherFilesDocument;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.OtherType;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.UndeploymentResultDocument;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.WSDLType;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.WorkflowManagementServicePropertiesDocument;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.WsdlFilesDocument;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.XSDType;
import de.dgrid.bisgrid.services.management.properties.workflowManagementService.XsdFilesDocument;
import de.dgrid.bisgrid.services.workflow.IWorkflowService;
import de.dgrid.bisgrid.services.workflow.WorkflowServiceHome;
import de.dgrid.bisgrid.services.workflow.factory.IWorkflowFactoryService;
import de.dgrid.bisgrid.services.workflow.handler.fromclient.BISGridAddressingInHandler;
import de.dgrid.bisgrid.services.workflow.handler.fromclient.BISGridPostInvocationHandler;
import de.dgrid.bisgrid.services.workflow.properties.workflowService.CredentialDescriptionType;
import de.dgrid.bisgrid.services.workflow.properties.workflowService.ServiceDescriptionType;
import de.dgrid.bisgrid.services.workflow.wsdl.CombiningWSDLWriter;
import de.fzj.unicore.uas.UAS;
import de.fzj.unicore.uas.impl.UASWSResourceImpl;
import de.fzj.unicore.uas.security.DSigOutHandler;
import de.fzj.unicore.uas.security.IUASSecurityProperties;
import de.fzj.unicore.uas.security.ProxyCertInHandler;
import de.fzj.unicore.uas.security.SecurityManager;
import de.fzj.unicore.uas.security.UASDSigDecider;
import de.fzj.unicore.uas.security.UASSecurityProperties;
import de.fzj.unicore.uas.security.UASSelfCallChecker;
import de.fzj.unicore.uas.util.AddressingUtil;
import de.fzj.unicore.wsrflite.Home;
import de.fzj.unicore.wsrflite.Kernel;
import de.fzj.unicore.wsrflite.exceptions.ResourceUnknownException;
import de.fzj.unicore.wsrflite.persistence.Persist;
import de.fzj.unicore.wsrflite.utils.Utilities;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptor;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentDescriptorImpl;
import de.fzj.unicore.wsrflite.utils.deployment.DeploymentManager;
import de.fzj.unicore.wsrflite.xmlbeans.WSResource;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceNotDestroyedFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnavailableFault;
import de.fzj.unicore.wsrflite.xmlbeans.exceptions.ResourceUnknownFault;
import de.fzj.unicore.wsrflite.xmlbeans.impl.WSResourceImpl;
import de.fzj.unicore.wsrflite.xmlbeans.rp.ImmutableResourceProperty;
import eu.unicore.security.xfireutil.AuthInHandler;
import eu.unicore.security.xfireutil.DSigParseInHandler;
import eu.unicore.security.xfireutil.DSigSecurityInHandler;
import eu.unicore.security.xfireutil.ETDInHandler;

/**
 * 
 * @author sgudenkauf
 * @author gscherp
 * @author ahoeing
 * 
 */
public class WorkflowManagementService extends UASWSResourceImpl implements IWorkflowManagementService {

    /**
	 * Generated serial version UID
	 */
    private static final long serialVersionUID = -2189613452794515290L;

    /**
	 * id for the queue used in the internal messaging system
	 */
    public static final String messageQueueId = "WorkflowManagementService";

    /**
	 * Log4J logging service
	 */
    protected static Logger log = Logger.getLogger(WorkflowManagementService.class);

    /**
	 * Some config properties
	 */
    @Persist()
    private String deploymentDirectory;

    @Persist()
    private String workingDirectory;

    @Persist()
    private String workingDirectoryDeploymentPackage;

    @Persist()
    private String workingDirectoryBPELEngine;

    @Persist()
    private String CHECK_FOR_DUPLICATE_WORKFLOW_NAME;

    @Persist()
    private int deploymentStatus = 0;

    private final int DEPLOYMENT_UNICORE_START = 30;

    private final int DEPLOYMENT_BPEL_START = 20;

    private final int DEPLOYMENT_PREPARATION_START = 10;

    private final int DEPLOYMENT_UNDEPLOYED = 0;

    public void initialise(String serviceName, Map<java.lang.String, java.lang.Object> initobjs) throws Exception {
        super.initialise(serviceName, initobjs);
        BISGridProperties bisgridproperties = BISGridProperties.getInstance();
        this.deploymentDirectory = bisgridproperties.getDeploymentDirectory();
        this.workingDirectory = deploymentDirectory + File.separator + this.getUniqueID();
        this.workingDirectoryDeploymentPackage = workingDirectory + File.separator + BISGridConstants.deploymentPackageDirName;
        this.workingDirectoryBPELEngine = workingDirectory + File.separator + BISGridConstants.BPELEngineDeploymentPackageDirName;
        this.CHECK_FOR_DUPLICATE_WORKFLOW_NAME = bisgridproperties.getProperty(BISGridProperties.CHECK_FOR_DUPLICATE_WORKFLOW_NAME);
        DeploymentPackageNameDocument nameDoc = DeploymentPackageNameDocument.Factory.newInstance();
        nameDoc.setDeploymentPackageName("empty");
        properties.put(IWorkflowManagementService.deploymentPackageNameProperty, new ImmutableResourceProperty(nameDoc));
        this.deploymentStatus = this.DEPLOYMENT_UNDEPLOYED;
        if (initobjs.get(WorkflowManagementServiceFactory.creatorDN) != null) {
            String owner = (String) initobjs.get(WorkflowManagementServiceFactory.creatorDN);
            log.debug("setting owner of resource " + getUniqueID() + " to " + owner);
            this.setOwner(owner);
        } else {
            log.debug("setting unicorex as owner of resource " + getUniqueID() + " (" + getOwner() + ")");
            this.setOwner(getOwner());
        }
    }

    /***************************************************************************
	 * Public Methods
	 **************************************************************************/
    @Override
    public QName getResourcePropertyDocumentQName() {
        return WorkflowManagementServicePropertiesDocument.type.getDocumentElementName();
    }

    @Override
    public DestroyResponseDocument Destroy(DestroyDocument in) throws ResourceNotDestroyedFault, ResourceUnknownFault, ResourceUnavailableFault {
        if (this.properties.get(isDeployedProperty) != null && ((IsDeployedDocument) this.properties.get(isDeployedProperty).getXml()[0]).getIsDeployed()) {
            throw new ResourceNotDestroyedFault("A worklow is currently deployed. It has to be undeployed before the resource can be destroyed.", null);
        }
        return super.Destroy(in);
    }

    @Override
    public String getBpelProcessName() {
        XmlObject[] xml = ((ImmutableResourceProperty) properties.get(bpelFileProperty)).getXml();
        org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument process = (org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument) xml[0];
        return process.getProcess().getName();
    }

    @Override
    public DeployRespDocument deploy(DeployReqDocument request) throws DeployFault {
        log.debug("Deployment: Starting Deployment ...");
        if (this.properties.get(isDeployedProperty) != null && ((IsDeployedDocument) this.properties.get(isDeployedProperty).getXml()[0]).getIsDeployed()) {
            throw new DeployFault("There is currently a workflow deployed. Please call redeploy or undeploy/deploy instead.");
        }
        this.deleteResourceProperties();
        this.setDeploymentStatusProperty(false);
        this.deploymentStatus = this.DEPLOYMENT_UNDEPLOYED;
        String deploymentResult = null;
        EndpointReferenceType workflowServiceFactory = null;
        try {
            log.debug("Deployment: Preparation");
            this.deploymentStatus = this.DEPLOYMENT_PREPARATION_START;
            byte[] deploymentPackage = request.getDeployReq().getDeploymentPackage();
            String deploymentPackageName = request.getDeployReq().getDeploymentPackageName();
            if (deploymentPackage == null) {
                throw new DeployFault("Deployment failed because the deployment archive is missing.");
            }
            if (deploymentPackageName == null || deploymentPackageName == "") {
                throw new DeployFault("Deployment failed because the deployment archive's name is empty.");
            }
            this.deploymentPreparation(deploymentPackage, deploymentPackageName);
            de.dgrid.bisgrid.services.management.deployment.ProcessDocument deploymentDescriptor = (de.dgrid.bisgrid.services.management.deployment.ProcessDocument) this.properties.get(deploymentDescriptorProperty).getXml()[0];
            org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument bpelFile = (org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument) this.properties.get(bpelFileProperty).getXml()[0];
            org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument bpelFileProcessed = null;
            WsdlFilesDocument wsdlFiles = (WsdlFilesDocument) this.properties.get(wsdlFilesProperty).getXml()[0];
            WsdlFilesDocument wsdlFilesProcessed = null;
            XsdFilesDocument xsdFiles = (XsdFilesDocument) this.properties.get(xsdFilesProperty).getXml()[0];
            OtherFilesDocument otherFiles = (OtherFilesDocument) this.properties.get(otherFilesProperty).getXml()[0];
            this.deploymentPatternInjection(deploymentDescriptor, bpelFile, wsdlFiles);
            bpelFileProcessed = (org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument) this.properties.get(bpelFileProcessedProperty).getXml()[0];
            wsdlFilesProcessed = (WsdlFilesDocument) this.properties.get(wsdlFilesProcessedProperty).getXml()[0];
            log.debug("Deployment: BPEL engine deployment");
            this.deploymentStatus = this.DEPLOYMENT_BPEL_START;
            BPELWorkflowEngineManager manager = BPELWorkflowEngineManager.getInstance();
            BPELWorkflowEngine engine = manager.getEngineById(0);
            String workflowEndpointUrl = this.deploymentBpelEngine(engine, deploymentPackageName, deploymentDescriptor, bpelFileProcessed, wsdlFilesProcessed, xsdFiles, otherFiles);
            deploymentResult = ((DeploymentResultDocument) this.properties.get(deploymentResultProperty).getXml()[0]).getDeploymentResult();
            log.debug("Deployment: UNICORE6 deployment");
            this.deploymentStatus = this.DEPLOYMENT_UNICORE_START;
            this.deploymentUNICORE6(engine, deploymentPackageName, workflowEndpointUrl, deploymentDescriptor);
            workflowServiceFactory = (EndpointReferenceType) this.properties.get(workflowServiceFactoryProperty).getXml()[0];
        } catch (DeployFault e) {
            String rollback = new String();
            try {
                this.rollback();
            } catch (UndeployFault ex) {
                rollback = "Rollback reported the following errors: \n" + ex.getMessage();
            }
            log.error("Deployment failed : " + rollback, e);
            throw new DeployFault("Deployment failed : " + e + "\n\n" + rollback);
        }
        log.info("Deployment successfully finished");
        this.setDeploymentStatusProperty(true);
        DeployRespDocument response = DeployRespDocument.Factory.newInstance();
        response.addNewDeployResp();
        response.getDeployResp().setResult(deploymentResult);
        response.getDeployResp().setEndpointReference(workflowServiceFactory);
        return response;
    }

    @Override
    public UndeployRespDocument undeploy(UndeployReqDocument request) throws UndeployFault {
        Vector<Exception> exceptions = new Vector<Exception>();
        if (this.properties.get(isDeployedProperty) != null && !((IsDeployedDocument) this.properties.get(isDeployedProperty).getXml()[0]).getIsDeployed()) {
            throw new UndeployFault("There is no workflow deployed.");
        }
        String deploymentPackageName = ((DeploymentPackageNameDocument) this.properties.get(deploymentPackageNameProperty).getXml()[0]).getDeploymentPackageName();
        org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument bpelFile = (org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument) this.properties.get(bpelFileProperty).getXml()[0];
        String bpelEngineWorkflowName = ((DeploymentWorkflowNameDocument) this.properties.get(deploymentWorkflowNameProperty).getXml()[0]).getDeploymentWorkflowName();
        boolean skipOtherActions = false;
        if (this.deploymentStatus >= this.DEPLOYMENT_UNICORE_START) {
            try {
                this.undeploymentDestroyFactory(deploymentPackageName);
            } catch (UndeployFault e) {
                exceptions.add(e);
            }
            try {
                skipOtherActions = this.undeploymentTermination(deploymentPackageName, request.getUndeployReq().getTermination());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        if (!skipOtherActions) {
            if (this.deploymentStatus >= this.DEPLOYMENT_UNICORE_START) {
                try {
                    this.undeploymentRemoveWorkflowService();
                } catch (UndeployFault e) {
                    exceptions.add(e);
                }
                try {
                    XACMLConfigurationHelper2.undeploySecuirityPolicy(deploymentPackageName);
                } catch (PolicyDestructionFailedException e) {
                }
            }
            if (this.deploymentStatus >= this.DEPLOYMENT_BPEL_START) {
                try {
                    this.undeploymentBpelEngine(deploymentPackageName, bpelFile, bpelEngineWorkflowName);
                } catch (UndeployFault e) {
                    exceptions.add(e);
                }
            }
            if (this.deploymentStatus >= this.DEPLOYMENT_PREPARATION_START) {
                try {
                    this.undeploymentCleanup();
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
        }
        if (this.deploymentStatus >= this.DEPLOYMENT_PREPARATION_START) {
            this.deleteResourceProperties();
        }
        this.setDeploymentStatusProperty(false);
        if (exceptions.size() > 0) {
            String exceptionsString = new String();
            for (Exception e : exceptions) {
                exceptionsString += "*" + e + "\n";
            }
            log.error("Undeployment ended with errors \n" + exceptionsString);
            throw new UndeployFault("Undeployment ended with errors" + exceptionsString);
        }
        log.info("Undeployment successfully finished");
        UndeployRespDocument response = UndeployRespDocument.Factory.newInstance();
        response.addNewUndeployResp();
        return response;
    }

    @Override
    public RedeployRespDocument redeploy(RedeployReqDocument req) throws UndeployFault, DeployFault {
        UndeployReqDocument undeployRequest = UndeployReqDocument.Factory.newInstance();
        undeployRequest.addNewUndeployReq();
        undeployRequest.getUndeployReq().addNewTermination();
        undeployRequest.getUndeployReq().getTermination().setType(TerminationSelectionType.INSTANT);
        this.undeploy(undeployRequest);
        DeployReqDocument deployRequest = DeployReqDocument.Factory.newInstance();
        deployRequest.addNewDeployReq();
        deployRequest.getDeployReq().setDeploymentPackageName(req.getRedeployReq().getDeploymentPackageName());
        deployRequest.getDeployReq().setDeploymentPackage(req.getRedeployReq().getDeploymentPackage());
        DeployRespDocument deployResponse = this.deploy(deployRequest);
        RedeployRespDocument response = RedeployRespDocument.Factory.newInstance();
        response.addNewRedeployResp();
        response.getRedeployResp().setResult(deployResponse.getDeployResp().getResult());
        response.getRedeployResp().setEndpointReference(deployResponse.getDeployResp().getEndpointReference());
        return response;
    }

    /*************
	 * Deployment
	 ************/
    private void deploymentPreparation(byte[] deploymentPackage, String deploymentPackageName) throws DeployFault {
        log.info("Deployment: Storing deployment package");
        if (this.deploymentDirectory == null || this.deploymentDirectory.equals("") || !(new File(deploymentDirectory).exists())) {
            log.error("Invalid deployment directory:\"" + deploymentDirectory + "\" Please check the property \"" + BISGridProperties.BISGRID_DEPLOYMENT_DIRECTORY + "\" in the BIS-Grid properties file and/or create the directory if missing.");
            throw new DeployFault("Internal error (invalid deployment directory). Please contact the system administrator.");
        }
        log.info("Deployment: Using \"" + deploymentDirectory + "\" as deployment directory.");
        log.debug("Deployment: Creating directory structure in deployment directory.");
        if (!new File(workingDirectoryDeploymentPackage).mkdirs() || !new File(workingDirectoryBPELEngine).mkdirs()) {
            throw new DeployFault("Could not prepare working directory. Please contact the system administrator.");
        }
        log.debug("Deployment: Saving deployment archive to deployment directory.");
        String deplomentPackageFullpath = this.workingDirectoryDeploymentPackage + File.separator + this.filterIllegalCharacters(deploymentPackageName) + ".zip";
        File deploymentPackageFile = new File(deplomentPackageFullpath);
        FileChannel outChannel = null;
        try {
            outChannel = new FileOutputStream(deploymentPackageFile).getChannel();
            outChannel.write(ByteBuffer.wrap(deploymentPackage));
        } catch (FileNotFoundException e) {
            throw new DeployFault("Deployment failed due to a FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {
            throw new DeployFault("Deployment failed due to an IOException: " + e.getMessage());
        } finally {
            try {
                if (outChannel != null) outChannel.close();
            } catch (IOException e) {
            }
        }
        log.debug("Deployment: Open workflow deployment package and filling resource properties.");
        DeploymentPackageDocument deploymentPackagePropertyInstance = DeploymentPackageDocument.Factory.newInstance();
        deploymentPackagePropertyInstance.setDeploymentPackage(deploymentPackage);
        this.properties.put(deploymentPackageProperty, new ImmutableResourceProperty(deploymentPackagePropertyInstance));
        DeploymentPackageNameDocument deploymentPackageNamePropertyInstance = DeploymentPackageNameDocument.Factory.newInstance();
        deploymentPackageNamePropertyInstance.setDeploymentPackageName(deploymentPackageName);
        this.properties.put(deploymentPackageNameProperty, new ImmutableResourceProperty(deploymentPackageNamePropertyInstance));
        InputStream entryInStream = null;
        FileOutputStream entryOutStream = null;
        try {
            ZipFile deploymentPackageZip = new ZipFile(deploymentPackageFile);
            Enumeration<? extends ZipEntry> entries = deploymentPackageZip.entries();
            WsdlFilesDocument wsdlFiles = WsdlFilesDocument.Factory.newInstance();
            wsdlFiles.addNewWsdlFiles();
            XsdFilesDocument xsdFiles = XsdFilesDocument.Factory.newInstance();
            xsdFiles.addNewXsdFiles();
            OtherFilesDocument otherFiles = OtherFilesDocument.Factory.newInstance();
            otherFiles.addNewOtherFiles();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (!entry.isDirectory()) {
                    boolean bpelFileFound = false;
                    if (entry.getName().equals(BISGridConstants.deploymentDescriptorFileName)) {
                        try {
                            de.dgrid.bisgrid.services.management.deployment.ProcessDocument deploymentDescriptor = de.dgrid.bisgrid.services.management.deployment.ProcessDocument.Factory.parse(deploymentPackageZip.getInputStream(entry));
                            de.dgrid.bisgrid.services.management.deployment.ProcessType.References.Other other = deploymentDescriptor.getProcess().getReferences().addNewOther();
                            other.setType("file");
                            other.setLocation("bisgrid/bpel-ws_addressing-transformation.xslt");
                            other.setTypeURI("http://www.w3.org/1999/XSL/Transform");
                            this.properties.put(deploymentDescriptorProperty, new ImmutableResourceProperty(deploymentDescriptor));
                        } catch (XmlException e) {
                            log.error("Could not parse deployment descriptor", e);
                            throw new DeployFault("Could not parse deployment descriptor: " + e.getMessage());
                        }
                    } else if (entry.getName().endsWith(BISGridConstants.bpelFileNameSuffix)) {
                        try {
                            if (!bpelFileFound) {
                                org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument bpelFile = org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument.Factory.parse(deploymentPackageZip.getInputStream(entry));
                                this.properties.put(bpelFileProperty, new ImmutableResourceProperty(bpelFile));
                            } else {
                                throw new DeployFault("Found more than one .bpel file in the deployment archive. Only one file is currently allowed.");
                            }
                        } catch (XmlException e) {
                            log.error("Could not parse WS-BPEL file", e);
                            throw new DeployFault("Could not parse WS-BPEL file: " + e.getMessage());
                        }
                    } else if (entry.getName().endsWith(BISGridConstants.wsdlFileNameSuffix)) {
                        try {
                            DefinitionsDocument wsdlDefinition = DefinitionsDocument.Factory.parse(deploymentPackageZip.getInputStream(entry));
                            WSDLType wsdlEntry = wsdlFiles.getWsdlFiles().addNewWsdl();
                            wsdlEntry.setDefinitions(wsdlDefinition.getDefinitions());
                            wsdlEntry.setName(entry.getName());
                        } catch (XmlException e) {
                            log.error("Could not parse WSDL file", e);
                            throw new DeployFault("Could not parse WSDL file " + entry.getName() + ": " + e.getMessage());
                        }
                    } else if (entry.getName().endsWith(BISGridConstants.xsdFileNameSuffix)) {
                        try {
                            SchemaDocument xsd = SchemaDocument.Factory.parse(deploymentPackageZip.getInputStream(entry));
                            XSDType xsdEntry = xsdFiles.getXsdFiles().addNewXsd();
                            xsdEntry.setSchema(xsd.getSchema());
                            xsdEntry.setName(entry.getName());
                        } catch (XmlException e) {
                            log.error("Could not parse XML schema file", e);
                            throw new DeployFault("Could not parse XML schema file " + entry.getName() + ": " + e.getMessage());
                        }
                    } else {
                        if (!entry.getName().equals("META-INF/MANIFEST.MF")) {
                            byte[] bytes = this.getByteArray(entry, deploymentPackageZip.getInputStream(entry));
                            OtherType otherFile = otherFiles.getOtherFiles().addNewOther();
                            otherFile.setName(entry.getName());
                            otherFile.setBytes(bytes);
                        }
                    }
                }
            }
            XSDType wsAddressingSchema = xsdFiles.getXsdFiles().addNewXsd();
            wsAddressingSchema.setName("bisgrid/ws-addr.xsd");
            wsAddressingSchema.setSchema(SchemaDocument.Factory.parse(ClassLoader.getSystemResourceAsStream("ws-addr.xsd")).getSchema());
            XSDType bisgridMappingSchema = xsdFiles.getXsdFiles().addNewXsd();
            bisgridMappingSchema.setName("bisgrid/bisgrid-mapping-information.xsd");
            bisgridMappingSchema.setSchema(SchemaDocument.Factory.parse(ClassLoader.getSystemResourceAsStream("bisgrid-mapping-information.xsd")).getSchema());
            XSDType bpelServicerefSchema = xsdFiles.getXsdFiles().addNewXsd();
            bpelServicerefSchema.setName("bisgrid/ws-bpel_serviceref.xsd");
            bpelServicerefSchema.setSchema(SchemaDocument.Factory.parse(ClassLoader.getSystemResourceAsStream("ws-bpel_serviceref.xsd")).getSchema());
            OtherType otherFile = otherFiles.getOtherFiles().addNewOther();
            otherFile.setName("bisgrid/bpel-ws_addressing-transformation.xslt");
            otherFile.setBytes(this.getByteArray(ClassLoader.getSystemResourceAsStream("bpel-ws_addressing-transformation.xslt")));
            this.properties.put(wsdlFilesProperty, new ImmutableResourceProperty(wsdlFiles));
            this.properties.put(xsdFilesProperty, new ImmutableResourceProperty(xsdFiles));
            this.properties.put(otherFilesProperty, new ImmutableResourceProperty(otherFiles));
        } catch (ZipException e) {
            log.error("Deployment failed due to a ZipException when processing the deployment archive: ", e);
            throw new DeployFault("Deployment failed due to a ZipException when processing the deployment archive: " + e.getMessage());
        } catch (IOException e) {
            log.error("Deployment failed due to an IOException when processing the deployment archive: ", e);
            throw new DeployFault("Deployment failed due to an IOException when processing the deployment archive: " + e.getMessage());
        } catch (XmlException e) {
            log.error("Deployment failed due to an XmlException when processing the deployment archive: ", e);
            throw new DeployFault("Deployment failed due to an XmlException when processing the deployment archive: " + e.getMessage());
        } finally {
            try {
                if (entryInStream != null) entryInStream.close();
            } catch (Exception e) {
            }
            try {
                if (entryOutStream != null) entryOutStream.close();
            } catch (Exception e) {
            }
        }
    }

    private void deploymentPatternInjection(de.dgrid.bisgrid.services.management.deployment.ProcessDocument deploymentDescriptor, org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument bpelFile, WsdlFilesDocument wsdlFiles) throws DeployFault {
        log.info("Deployment: Pattern injection to WS-BPEL file and WSDL file(s)");
        List<DefinitionsDocument> wsdls = new Vector<DefinitionsDocument>();
        List<String> wsdlsNames = new Vector<String>();
        for (WSDLType wsdl : wsdlFiles.getWsdlFiles().getWsdlArray()) {
            DefinitionsDocument wsdlObj = null;
            wsdlObj = DefinitionsDocument.Factory.newInstance();
            wsdlObj.setDefinitions(wsdl.getDefinitions());
            wsdls.add(wsdlObj);
            wsdlsNames.add(wsdl.getName());
        }
        IPatternProcessingFacade processingFacade = PatternProcessingFacade.getInstance();
        try {
            log.debug("Deployment: Grid-BPEL mapping");
            org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument bpelFileProcessed = processingFacade.insertGridBpelMapping(bpelFile, deploymentDescriptor, wsdls);
            this.properties.put(bpelFileProcessedProperty, new ImmutableResourceProperty(bpelFileProcessed));
            log.debug("Deployment: Modify WSDLs");
            List<DefinitionsDocument> modifiedWsdls = processingFacade.insertStartMessagePartsInWsdls(bpelFile, wsdls);
            WsdlFilesDocument modifiedWsdlFiles = WsdlFilesDocument.Factory.newInstance();
            modifiedWsdlFiles.addNewWsdlFiles();
            for (int index = 0; index < modifiedWsdls.size(); index++) {
                WSDLType wsdlEntry = modifiedWsdlFiles.getWsdlFiles().addNewWsdl();
                wsdlEntry.set(modifiedWsdls.get(index));
                wsdlEntry.setName(wsdlsNames.get(index));
            }
            this.properties.put(wsdlFilesProcessedProperty, new ImmutableResourceProperty(modifiedWsdlFiles));
        } catch (PatternProcessingException e) {
            log.error("Deployment failed due to a pattern processing exception: " + e);
            throw new DeployFault("Deployment failed due to a pattern processing exception: " + e);
        }
    }

    /**
	 * 
	 * @param engine
	 * @param processName
	 * @param deploymentDescriptor
	 * @param bpelFile
	 * @param wsdlFiles
	 * @param xsdFiles
	 * @param otherFiles
	 * @return the endpoint of the service of the deployed workflow
	 * @throws DeployFault
	 */
    private String deploymentBpelEngine(BPELWorkflowEngine engine, String processName, de.dgrid.bisgrid.services.management.deployment.ProcessDocument deploymentDescriptor, org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument bpelFile, WsdlFilesDocument wsdlFiles, XsdFilesDocument xsdFiles, OtherFilesDocument otherFiles) throws DeployFault {
        log.info("Deployment: Deploy workflow with WS-BPEL engine adapter");
        if (Boolean.parseBoolean(this.CHECK_FOR_DUPLICATE_WORKFLOW_NAME)) {
            log.debug("Deployment: Testing if there is already a deployed WS-BPEL workflow with the same name");
            String wsbpelProcessName = bpelFile.getProcess().getName();
            boolean processAlreadyDeployed = ((WorkflowManagementServiceHome) home).isBPELNameAlreadyDeployed(wsbpelProcessName, getUniqueID());
            if (processAlreadyDeployed) {
                throw new DeployFault("A WS-BPEL process with the same name is already deployed.");
            }
        }
        Adapter engineAdapter = null;
        DeploymentAdapter deploymentAdapter = null;
        try {
            engineAdapter = AdapterFactory.getAdapterInstance(engine.getEngineType());
            deploymentAdapter = engineAdapter.getDeploymentAdapter();
        } catch (BPELWorkflowEngineNotSupportedException e) {
            log.error(e);
            throw new DeployFault("Could not create deployment adapter: " + e.getMessage());
        }
        org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument bpelFileEngineSpecificProcessed = bpelFile;
        for (AdapterModule m : engineAdapter.getAdapterModules()) {
            m.injectAdapterSpecificCode(bpelFileEngineSpecificProcessed);
        }
        this.properties.put(bpelFileEngineSpecificProcessedProperty, new ImmutableResourceProperty(bpelFileEngineSpecificProcessed));
        try {
            DeploymentAdapterResult deploymentResult = deploymentAdapter.deploy(this.workingDirectoryBPELEngine, processName, bpelFileEngineSpecificProcessed.getProcess().getName(), bpelFileEngineSpecificProcessed, deploymentDescriptor, wsdlFiles, xsdFiles, otherFiles);
            DeploymentResultDocument deploymentResultDocument = DeploymentResultDocument.Factory.newInstance();
            deploymentResultDocument.setDeploymentResult(deploymentResult.getResult());
            this.properties.put(deploymentResultProperty, new ImmutableResourceProperty(deploymentResultDocument));
            DeploymentWorkflowNameDocument deploymentWorkflowNameDocument = DeploymentWorkflowNameDocument.Factory.newInstance();
            deploymentWorkflowNameDocument.setDeploymentWorkflowName(deploymentResult.getProcessName());
            this.properties.put(deploymentWorkflowNameProperty, new ImmutableResourceProperty(deploymentWorkflowNameDocument));
        } catch (BPELWorkflowEngineDeployFault e) {
            log.error(e);
            throw new DeployFault("Could not deploy process in BPEL engine: " + e.getMessage());
        }
        return engineAdapter.getWorkflowEndpointUrl(deploymentDescriptor, bpelFile, wsdlFiles, xsdFiles, otherFiles, engine);
    }

    private void deploymentUNICORE6(BPELWorkflowEngine engine, String processName, String workflowUrl, de.dgrid.bisgrid.services.management.deployment.ProcessDocument deploymentDescriptor) throws DeployFault {
        log.info("Deployment: Create a corresponding WorkflowServiceFactory for the WS-BPEL workflow");
        log.info("Deployment: Deploy Workflow Service for WS-BPEL Service " + workflowUrl);
        String serviceName = BISGridConstants.createWorkflowServiceName(processName);
        try {
            boolean usePersistance = Boolean.parseBoolean(BISGridProperties.getInstance().getProperty(BISGridProperties.WORKFLOW_USE_PERSISTANCE));
            DeploymentDescriptor workflowDD = new DeploymentDescriptorImpl(IWorkflowService.class.getName(), WorkflowServiceHome.class.getName(), usePersistance);
            Service s = DeploymentManager.getInstance().deployService(true, serviceName, workflowDD);
            HandlerUtils.addHandlerFromProperties2Service(s, HandlerUtils.Pipelines.IN, BISGridProperties.WORKFLOW_IN_HANDLER, log);
            HandlerUtils.addHandlerFromProperties2Service(s, HandlerUtils.Pipelines.OUT, BISGridProperties.WORKFLOW_OUT_HANDLER, log);
            List<?> inHandlers = (List<?>) s.getInHandlers();
            for (int i = 0; i < inHandlers.size(); i++) {
                AbstractHandler h = (AbstractHandler) inHandlers.get(i);
                log.debug("Deployment: Handler-Class " + h.getClass());
                if (h instanceof PostInvocationHandler) {
                    log.debug("Removing uneccessary handler for BIS-Grid engine " + h.getClass());
                    inHandlers.remove(h);
                }
                if (h instanceof AddressingInHandler) {
                    log.debug("Removing uneccessary handler for BIS-Grid engine " + h.getClass());
                    inHandlers.remove(h);
                }
            }
            s.addInHandler(new BISGridPostInvocationHandler());
            s.addInHandler(new BISGridAddressingInHandler());
            s.addInHandler(new DOMInHandler());
            s.addInHandler(new ProxyCertInHandler());
            log.debug("Configuring default security handler chain.");
            s.addInHandler(new DSigParseInHandler(new UASDSigDecider(true)));
            s.addInHandler(createAuthInHandler());
            s.addInHandler(new DSigSecurityInHandler(null));
            s.addInHandler(new ETDInHandler(new UASSelfCallChecker(), true));
            DSigOutHandler o1 = new DSigOutHandler();
            o1.doInit(UAS.getSecurityProperties());
            s.addOutHandler(o1);
            boolean loggingEnabled = Boolean.parseBoolean(BISGridProperties.getInstance().getProperty(BISGridProperties.PERFORMANCE_LOGGING));
            if (loggingEnabled) {
                s.addInHandler(new WorkflowInvokatonPerfMonitoringInHandler());
                s.addOutHandler(new WorkflowInvokatonPerfMonitoringOutHandler());
            }
            CombiningWSDLWriter writer = new CombiningWSDLWriter(s.getWSDLWriter(), workflowUrl);
            s.setWSDLWriter(writer);
            UAS.setProperty(UASSecurityProperties.UAS_CHECKACCESS + "." + serviceName, "true");
        } catch (Exception e) {
            log.error("Could not deploy Workflow Service in UNICORE container", e);
            throw new DeployFault("Could not deploy Workflow Service in UNICORE container: " + e.getMessage());
        }
        log.info("Deployment: Deploying WorkflowFactoryService");
        Map<String, Object> imap = new HashMap<String, Object>();
        imap.put(WSResourceImpl.INIT_UNIQUE_ID, BISGridConstants.createWorkflowFactoryInstanceName(processName));
        imap.put(IWorkflowFactoryService.WORKFLOW_NAME_PROPERTY, BISGridConstants.createWorkflowServiceName(processName));
        imap.put(IWorkflowFactoryService.WORKFLOW_SERVICE_NAME_PROPERTY, serviceName);
        imap.put(IWorkflowFactoryService.WORKFLOW_URL_PROPERTY, workflowUrl);
        if (deploymentDescriptor.getProcess().getServiceDescriptions() != null || deploymentDescriptor.getProcess().getServiceDescriptions().getServiceDescriptionArray() != null) {
            ServiceDescriptionType[] serviceDescriptionArray = deploymentDescriptor.getProcess().getServiceDescriptions().getServiceDescriptionArray();
            log.debug("Deployment : Management Service found " + serviceDescriptionArray.length + " service descriptions");
            imap.put(IWorkflowFactoryService.SERVICE_DESCRIPTIONS_PROPERTY, serviceDescriptionArray);
        }
        if (deploymentDescriptor.getProcess().getCredentialDescriptions() != null || deploymentDescriptor.getProcess().getCredentialDescriptions().getCredentialDescriptionArray() != null) {
            CredentialDescriptionType[] credentialDescriptionArray = deploymentDescriptor.getProcess().getCredentialDescriptions().getCredentialDescriptionArray();
            log.info("Deployment: Management Service found " + credentialDescriptionArray.length + " credential descriptions");
            imap.put(IWorkflowFactoryService.CREDENTIAL_DESCRIPTIONS_PROPERTY, credentialDescriptionArray);
        }
        if (deploymentDescriptor.getProcess().getAccessRules() != null || deploymentDescriptor.getProcess().getAccessRules().getRuleArray() != null) {
            RuleType[] accessRulesArray = deploymentDescriptor.getProcess().getAccessRules().getRuleArray();
            log.debug("Deployment: Management Service found security rules " + accessRulesArray.length);
            imap.put(IWorkflowFactoryService.ACCESS_RULES_POLICY, accessRulesArray);
        }
        Home factoryHome = Kernel.getKernel().getServiceHome(IWorkflowFactoryService.SERVICE_NAME);
        String factoryName = BISGridConstants.createWorkflowFactoryInstanceName(processName);
        try {
            if (factoryHome.getWSRFServiceInstance(factoryName) != null) {
                factoryHome.destroyWSRFServiceInstance(factoryName);
                log.info("Deployment: Factory Service " + factoryName + "Factory already exists, deleting the old one");
            }
        } catch (ResourceUnknownException e) {
        } catch (Exception e) {
            throw new DeployFault("Could not destroy Workflow Factory Service instance : " + e.getMessage());
        }
        try {
            String id = factoryHome.createWSRFServiceInstance(imap);
            EndpointReferenceType factory = AddressingUtil.newEPR();
            factory.addNewAddress().setStringValue(Utilities.makeAddress(IWorkflowFactoryService.SERVICE_NAME, id));
            this.properties.put(workflowServiceFactoryProperty, new ImmutableResourceProperty(factory));
            WSResource factoryInstance = (WSResource) factoryHome.getWSRFServiceInstance(id);
            SetTerminationTimeDocument unlimitedTerminationTimeDocument = SetTerminationTimeDocument.Factory.newInstance();
            SetTerminationTime addNewSetTerminationTime = unlimitedTerminationTimeDocument.addNewSetTerminationTime();
            addNewSetTerminationTime.setNilRequestedTerminationTime();
            factoryInstance.SetTerminationTime(unlimitedTerminationTimeDocument);
            log.info("termination time of new instance");
            log.info("-------------");
            log.info(factoryInstance.getTerminationTime());
            log.info("-------------");
            log.debug("Deployment: Added Factrory for service \"" + processName + "\" at EPR: " + factory);
        } catch (Exception e) {
            log.error("Deployment: Error while creating factory service", e);
            throw new DeployFault("Could not create Factory Service: " + e.getMessage());
        }
        try {
            log.info("Deployment: Setting policy");
            RuleType[] accessRules = deploymentDescriptor.getProcess().getAccessRules().getRuleArray();
            if (accessRules != null) {
                log.info("found " + accessRules.length + " access rules at deployment descriptor");
                XACMLConfigurationHelper2.deploySecurityPolicy(processName, accessRules);
            }
        } catch (PolicyInstatiationFailedException e) {
            throw new DeployFault("Cannot deploy workflow because cannot reconfig the security configuration of UNICORE 6 " + e.getMessage(), e);
        }
    }

    /***************
	 * Undeployment
	 **************/
    private void undeploymentDestroyFactory(String processName) throws UndeployFault {
        log.info("Undeployment : Destroying the Workflow Service Factory instance");
        String factoryName = BISGridConstants.createWorkflowFactoryInstanceName(processName);
        try {
            Home factoryHome = Kernel.getKernel().getServiceHome(IWorkflowFactoryService.SERVICE_NAME);
            factoryHome.destroyWSRFServiceInstance(factoryName);
        } catch (ResourceUnknownException e) {
            log.error("Deployment: Could not destroy Workflow Factory Service instance", e);
            throw new UndeployFault("Could not destroy Workflow Factory Service instance" + e);
        } catch (Exception e) {
            log.error("Deployment: Could not destroy Workflow Factory Service instance", e);
            throw new UndeployFault("Could not destroy Workflow Factory Service instance : " + e);
        }
    }

    private boolean undeploymentTermination(String processName, TerminationType termination) throws UndeployFault {
        log.info("Undeployment : Executing termination step");
        String workflowServiceName = BISGridConstants.createWorkflowServiceName(processName);
        int terminationType = termination.getType().intValue();
        WorkflowServiceHome workflowServiceHome = (WorkflowServiceHome) Kernel.getKernel().getServiceHome(workflowServiceName);
        boolean skipOtherActions = false;
        if (terminationType == TerminationSelectionType.INT_BYDATE) {
            log.debug("Using termination type " + TerminationSelectionType.BYDATE);
            if (!workflowServiceHome.setTerminationTimesForRunningInstances(termination.getBydatevalue())) {
                throw new UndeployFault("Could not set resource lifetimes for running instances.");
            }
            skipOtherActions = true;
        } else if (terminationType == TerminationSelectionType.INT_BYPERIOD) {
            log.debug("Using termination type " + TerminationSelectionType.BYPERIOD);
            Calendar cal = Calendar.getInstance();
            long terminationTime = cal.getTimeInMillis() + termination.getByperiodvalue() * 1000;
            cal.setTimeInMillis(terminationTime);
            if (!workflowServiceHome.setTerminationTimesForRunningInstances(cal)) {
                throw new UndeployFault("Could not set resource lifetimes for running instances.");
            }
            skipOtherActions = true;
        } else if (terminationType == TerminationSelectionType.INT_INSTANT) {
            log.debug("Using termination type " + TerminationSelectionType.INSTANT);
            if (!workflowServiceHome.destroyRunningInstances()) {
                throw new UndeployFault("Could not destroy running instances.");
            }
        } else {
            log.debug("Using termination type " + TerminationSelectionType.NORMAL);
            skipOtherActions = true;
        }
        return skipOtherActions;
    }

    private void undeploymentRemoveWorkflowService() throws UndeployFault {
        log.info("Undeployment : Removing the Workflow Service");
    }

    private void undeploymentBpelEngine(String processName, org.oasisOpen.docs.wsbpel.x20.process.executable.ProcessDocument bpelProcess, String bpelEngineWorkflowName) throws UndeployFault {
        log.info("Undeployment : Undeploying workflow in WS-BPEL engine");
        BPELWorkflowEngineManager manager = BPELWorkflowEngineManager.getInstance();
        BPELWorkflowEngine engine = manager.getEngineById(0);
        DeploymentAdapter deploymentAdapter = null;
        try {
            deploymentAdapter = AdapterFactory.getAdapterInstance(engine.getEngineType()).getDeploymentAdapter();
        } catch (BPELWorkflowEngineNotSupportedException e) {
            log.error("Could not create deployment adapter", e);
            throw new UndeployFault("Could not create deployment adapter: " + e.getMessage());
        }
        try {
            DeploymentAdapterResult undeploymentResult = deploymentAdapter.undeploy(this.workingDirectoryBPELEngine, processName, bpelProcess.getProcess().getName(), bpelEngineWorkflowName);
            UndeploymentResultDocument undeploymentResultDocument = UndeploymentResultDocument.Factory.newInstance();
            undeploymentResultDocument.setUndeploymentResult(undeploymentResult.getResult());
            this.properties.put(undeploymentResultProperty, new ImmutableResourceProperty(undeploymentResultDocument));
        } catch (BPELWorkflowEngineUndeployFault e) {
            log.error("Could not undeploy process from BPEL engine", e);
            throw new UndeployFault("Could not undeploy process from BPEL engine: " + e.getMessage());
        }
    }

    private void undeploymentCleanup() throws UndeployFault {
        log.info("Undeployment: Cleanup");
        File file = new File(this.deploymentDirectory + File.separator + this.getUniqueID());
        if (file.exists()) {
            if (!this.deleteDirectory(file)) {
                throw new UndeployFault("Could not delete the deployment directory for the workflow. Please contact the system administrator if you want to be sure that all your data is deleted.");
            }
        }
    }

    /**
	 * This method deletes all resource properties
	 */
    private void deleteResourceProperties() {
        this.properties.remove(deploymentPackageProperty);
        this.properties.remove(deploymentPackageNameProperty);
        this.properties.remove(deploymentDescriptorProperty);
        this.properties.remove(bpelFileProperty);
        this.properties.remove(bpelFileProcessedProperty);
        this.properties.remove(bpelFileEngineSpecificProcessedProperty);
        this.properties.remove(wsdlFilesProperty);
        this.properties.remove(wsdlFilesProcessedProperty);
        this.properties.remove(xsdFilesProperty);
        this.properties.remove(otherFilesProperty);
        this.properties.remove(deploymentResultProperty);
        this.properties.remove(deploymentWorkflowNameProperty);
        this.properties.remove(undeploymentResultProperty);
        this.properties.remove(workflowServiceFactoryProperty);
        DeploymentPackageNameDocument nameDoc = DeploymentPackageNameDocument.Factory.newInstance();
        nameDoc.setDeploymentPackageName("empty");
        properties.put(IWorkflowManagementService.deploymentPackageNameProperty, new ImmutableResourceProperty(nameDoc));
    }

    /**
	 * Sets the deployment status property to true or false true = a workflow is
	 * deployed succesfully false = no workflow is deployed
	 */
    private void setDeploymentStatusProperty(boolean value) {
        IsDeployedDocument isDeployed = IsDeployedDocument.Factory.newInstance();
        isDeployed.setIsDeployed(value);
        this.properties.remove(isDeployedProperty);
        this.properties.put(isDeployedProperty, new ImmutableResourceProperty(isDeployed));
    }

    /**
	 * This method recursively removes a directory
	 */
    private boolean deleteDirectory(File path) {
        File[] files = path.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                deleteDirectory(files[i]);
            } else {
                files[i].delete();
            }
        }
        return (path.delete());
    }

    /**
	 * This method filters illegal characters as "\" and "/"
	 */
    private String filterIllegalCharacters(String string) {
        return string.replace(File.separatorChar, '_');
    }

    /**
	 * This method trys to execute a rollback if a deployment error occurs.
	 */
    private void rollback() throws UndeployFault {
        if (!(Boolean.getBoolean(BISGridProperties.getInstance().getProperty(BISGridProperties.BISGRID_MANAGMENT_DEBUG_ROLEBACK)))) return;
        this.setDeploymentStatusProperty(true);
        UndeployReqDocument undeployReqDocument = UndeployReqDocument.Factory.newInstance();
        undeployReqDocument.addNewUndeployReq();
        undeployReqDocument.getUndeployReq().addNewTermination();
        undeployReqDocument.getUndeployReq().getTermination().setType(TerminationSelectionType.INSTANT);
        this.undeploy(undeployReqDocument);
    }

    /**
	 * Returns an InputStream of a ZipEntry as byte array
	 */
    private byte[] getByteArray(ZipEntry file, InputStream is) throws IOException {
        long length = file.getSize();
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        return bytes;
    }

    /**
	 * Returns an InputStream as byte array
	 */
    private byte[] getByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        byte[] bytes = bos.toByteArray();
        return bytes;
    }

    /**
	 * @deprecated
	 */
    @SuppressWarnings("unused")
    private UcServiceEndpointReference retrieveEprFromDeploymentDescriptor(ProcessDocument deploymentDescr) throws IllegalUcServiceEPRException {
        try {
            String servname = "";
            String servaddr = "";
            String servportname = "";
            String servnsprefix = "";
            String servnsuri = "";
            Document doc = new SAXBuilder().build(new InputSource(new StringReader(deploymentDescr.xmlText())));
            Element root = doc.getRootElement();
            Filter filter = new ElementFilter();
            Iterator<?> it = root.getDescendants(filter);
            while (it.hasNext()) {
                Element elem = (Element) it.next();
                if (elem.getName().equals("EndpointReference")) {
                    servname = elem.getChild("ServiceName", Namespace.getNamespace("wsa", "http://schemas.xmlsoap.org/ws/2003/03/addressing")).getTextNormalize();
                    servaddr = elem.getChild("Address", Namespace.getNamespace("wsa", "http://schemas.xmlsoap.org/ws/2003/03/addressing")).getTextNormalize();
                    servportname = elem.getChild("ServiceName", Namespace.getNamespace("wsa", "http://schemas.xmlsoap.org/ws/2003/03/addressing")).getAttributeValue("PortName").trim();
                    if (servname.contains(":")) {
                        String temp[] = servname.split(":", 2);
                        servname = temp[1];
                        servnsprefix = temp[0];
                        servnsuri = elem.getNamespace(temp[0]).getURI();
                    }
                }
            }
            return UcServiceEndpointReference.getUcServiceInformation(servname, servaddr, servportname, Namespace.getNamespace(servnsprefix, servnsuri));
        } catch (JDOMException ex) {
            throw new IllegalUcServiceEPRException("The deployment descriptor is not well-formed (JDOMException): " + ex.getMessage());
        } catch (Exception ex) {
            throw new IllegalUcServiceEPRException("The deployment descriptor could not be parsed (Exception): " + ex.getMessage());
        }
    }

    private AuthInHandler createAuthInHandler() {
        String verifyConsignorP = UAS.getProperty(IUASSecurityProperties.UAS_CHECK_CONSIGNOR_SIGNATURE);
        boolean verifyConsignor = Boolean.parseBoolean(verifyConsignorP);
        X509Certificate gatewayCert = null;
        if (verifyConsignor) {
            gatewayCert = SecurityManager.getGatewayCert();
            if (gatewayCert == null) {
                log.error("Can't retrieve gateway certificate, required" + "for gateway assertion verification.");
            }
        } else {
            log.info("IMPORTANT! Gateway assertions verification is turned OFF." + " This is OK only if your UNICORE/X installation is protected " + "by a firewall, and the only way of accessing it is through the gateway." + " If this is not true then you SHOULD turn on the gateway assertion " + "verification to prevent unauthorized access.");
        }
        if (!verifyConsignor) gatewayCert = null;
        return new AuthInHandler(true, true, false, gatewayCert);
    }

    public String getDeploymentPackageName() {
        return ((DeploymentPackageNameDocument) properties.get(deploymentPackageNameProperty).getXml()[0]).getDeploymentPackageName();
    }
}
