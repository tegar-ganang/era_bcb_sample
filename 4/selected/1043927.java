package org.alfresco.extension.procurement.model;

import java.io.Serializable;
import java.util.Map;
import org.alfresco.extension.procurement.workflow.WorkflowExecutorImpl;
import org.alfresco.extension.procurement.workflow.WorkflowJob;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.filestore.FileContentReader;
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProcurementRequestType implements ProcurementModel {

    private static final Log log = LogFactory.getLog(ProcurementRequestType.class);

    private PolicyComponent policyComponent;

    private NodeService nodeService;

    private ContentService contentService;

    private WorkflowExecutorImpl workflowExecutor;

    private PersonService personService;

    private IdGenerator idGenerator;

    public ProcurementRequestType(ServiceRegistry serviceRegistry, WorkflowExecutorImpl workflowExecutor) {
        this.nodeService = serviceRegistry.getNodeService();
        this.contentService = serviceRegistry.getContentService();
        this.workflowExecutor = workflowExecutor;
        this.personService = serviceRegistry.getPersonService();
    }

    public void init() {
        log.debug("Registering type behaviour");
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, TYPE_PROCUREMENT_REQUEST, new JavaBehaviour(this, "onCreateNode"));
        policyComponent.bindAssociationBehaviour(NodeServicePolicies.OnCreateAssociationPolicy.QNAME, TYPE_PROCUREMENT_REQUEST, TYPE_PROCUREMENT_REQUEST_APPROVER, new JavaBehaviour(this, "onCreateAssociation"));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnUpdatePropertiesPolicy.QNAME, TYPE_PROCUREMENT_REQUEST, new JavaBehaviour(this, "onUpdateProperties"));
    }

    /**
	 * On create node behavior.
	 * Adds a processed ID property to the node.
	 * 
	 * @param childAssocRef
	 *            child association reference
	 */
    public void onCreateNode(ChildAssociationRef childAssocRef) {
        NodeRef node = childAssocRef.getChildRef();
        log.debug("onCreateNode event fired");
        addProcessProperties(childAssocRef, node);
    }

    /**
	 * On update node properties behavior. 
	 * There is no binary  content object uploaded for this model type. It is created on the fly instead, because
	 * the document is based on the provided metadata.
	 * The behavior ensures the (re-)creation of the content document. 
	 * TODO: Use a XSL rendition to create a formatted pdf.
	 */
    public void onUpdateProperties(NodeRef node, Map<QName, Serializable> before, Map<QName, Serializable> after) {
        log.debug("onUpdateNode event on node " + node.getId() + " fired. Creating property rendition.");
        createBinaryRequestDocument(node);
    }

    /**
	 * On create association properties behavior.
	 * Starts the approval workflow. Therefore an assignee, indicated a peer association to a cm:person, object is necessary.
	 *  
	 * @param nodeAssocRef
	 */
    public void onCreateAssociation(AssociationRef nodeAssocRef) {
        log.debug("onCreateAssociation on association " + nodeAssocRef.getId() + " fired");
        QName assocType = nodeAssocRef.getTypeQName();
        if (ProcurementModel.TYPE_PROCUREMENT_REQUEST_APPROVER.isMatch(assocType)) {
            log.debug("Approver association found, so let's start the workflow tasks.");
            NodeRef sourceNode = nodeAssocRef.getSourceRef();
            NodeRef assigneeNode = nodeAssocRef.getTargetRef();
            startAndAssignApprovalWorkflow(sourceNode, assigneeNode);
        }
    }

    /**
	 * Start the approval workflow. Therefore an approval assignee has to be set on the node.
	 * The workflow will not be started if there is no association to an approver (cm:person node).
	 * 
	 * @param node The node to approve.
	 */
    private void startAndAssignApprovalWorkflow(NodeRef sourceNode, NodeRef assignee) {
        String approver = getAssigneeUserName(assignee);
        if (doesPersonExist(approver)) {
            log.debug("Workflow will be approved by : " + approver);
            WorkflowJob job = new WorkflowJob(ProcurementModel.REVIEW_WORKFLOW_NAME, assignee, sourceNode);
            job.setWorkflowDescription("Please review");
            log.debug("Starting workflow job " + job.getWorkflow() + " assigned to " + assignee.toString());
            workflowExecutor.doRun(job);
        }
    }

    private boolean doesPersonExist(String approver) {
        return approver != null && approver.length() > 0 && personService.personExists(approver);
    }

    private void addProcessProperties(ChildAssociationRef childAssocRef, NodeRef node) {
        String id = idGenerator.generateProcessId(childAssocRef, node);
        nodeService.setProperty(node, ProcurementModel.TYPE_PR_NUMBER, id);
    }

    /**
	 * Retrieves the username that has been assigned as a approver to the current node.
	 * The assignment is done via a peer association.
	 * 
	 * @param node A node with peer associations to cm:person object.
	 * @return A username.
	 */
    private String getAssigneeUserName(NodeRef approver) {
        log.debug("Using approver node: " + approver.getStoreRef() + "/" + approver.getId());
        return (String) nodeService.getProperty(approver, ContentModel.PROP_USERNAME);
    }

    /**
	 * Creates a PDF order and stores it inside the order nodes content
	 * property.
	 * TODO ... use an XSL stylesheet to create a formated PDF.
	 * 
	 * @param request
	 */
    private void createBinaryRequestDocument(NodeRef request) {
        ContentTransformer transformer = contentService.getTransformer(MimetypeMap.MIMETYPE_TEXT_PLAIN, MimetypeMap.MIMETYPE_PDF);
        ProcurementRequest procurementRequest = new ProcurementRequest(request, this.nodeService);
        ContentReader reader = FileContentReader.getSafeContentReader(null, procurementRequest.createTextOutput());
        ContentWriter writer = contentService.getWriter(request, TYPE_CONTENT, true);
        writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
        writer.setEncoding("UTF-8");
        transformer.transform(reader, writer);
    }

    public PolicyComponent getPolicyComponent() {
        return policyComponent;
    }

    public void setPolicyComponent(PolicyComponent policyComponent) {
        this.policyComponent = policyComponent;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }
}
