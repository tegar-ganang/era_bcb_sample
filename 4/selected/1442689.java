package se.vgregion.metaservice.vocabularyservice;

import com.apelon.dts.client.DTSException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.log4j.Logger;
import se.vgregion.metaservice.keywordservice.MedicalTaxonomyService;
import se.vgregion.metaservice.keywordservice.domain.Identification;
import se.vgregion.metaservice.keywordservice.domain.LastChangeResponseObject;
import se.vgregion.metaservice.keywordservice.domain.LookupResponseObject;
import se.vgregion.metaservice.keywordservice.domain.MedicalNode;
import se.vgregion.metaservice.keywordservice.domain.NodeListResponseObject;
import se.vgregion.metaservice.keywordservice.domain.NodePath;
import se.vgregion.metaservice.keywordservice.domain.NodeProperty;
import se.vgregion.metaservice.keywordservice.domain.Options;
import se.vgregion.metaservice.keywordservice.domain.ResponseObject;
import se.vgregion.metaservice.keywordservice.domain.ResponseObject.StatusCode;
import se.vgregion.metaservice.keywordservice.domain.SearchProfile;
import se.vgregion.metaservice.keywordservice.domain.XMLResponseObject;
import se.vgregion.metaservice.keywordservice.exception.InvalidPropertyTypeException;
import se.vgregion.metaservice.keywordservice.exception.KeywordsException;
import se.vgregion.metaservice.keywordservice.exception.NodeAlreadyExistsException;
import se.vgregion.metaservice.keywordservice.exception.NodeNotFoundException;
import se.vgregion.metaservice.keywordservice.exception.ParentNotFoundException;
import se.vgregion.metaservice.lemmatisation.generated.LemmatisedObject;
import se.vgregion.metaservice.lemmatisation.generated.LemmatisedResponse;

/**
 * Class for handling queries for a vocabulary
 */
public class VocabularyService {

    private static Logger log = Logger.getLogger(VocabularyService.class);

    private MedicalTaxonomyService medicalTaxonomyService;

    private String profileIdPropertyName = "profileId";

    private String userIdPropertyName = "userId";

    private String urlPropertyName = "url";

    private Set<String> allowedNamespaces = null;

    /** Caches namespaceName to namespaceId-name resolutions. */
    private Map<String, String> namespaceCache = new HashMap<String, String>();

    /** Map of searchprofiles indexed by profileId */
    private Map<String, SearchProfile> searchProfiles;

    /** REST client used to access the Lemmatisation service */
    private LemmatisationRestClient lemmatisationRestClient;

    public LastChangeResponseObject getLastChange(Identification identification, String requestId, String namespaceName) {
        String namespaceId = getNamespaceIdByName(namespaceName, requestId);
        LastChangeResponseObject response = new LastChangeResponseObject();
        response.setStatusCode(StatusCode.ok);
        response.setRequestId(requestId);
        if (namespaceId != null) {
            try {
                response.setLastChange(medicalTaxonomyService.getLastChange(namespaceId));
            } catch (KeywordsException ex) {
                response.setStatusCode(StatusCode.error_resolving_property);
                response.setErrorMessage("No lastChange property found");
                log.warn(MessageFormat.format("{0}:{1}: Error retrieving property lastChange", requestId, StatusCode.error_resolving_property.code()));
            }
        } else {
            response.setStatusCode(StatusCode.error_locating_namespace);
            response.setErrorMessage("Error locating namespaceId by namespace name. Note that namespace is case sensitive.");
            log.warn(MessageFormat.format("{0}:{1}: Error locating namespaceId by namespace name {2}", requestId, StatusCode.error_locating_namespace.code(), namespaceName));
        }
        return response;
    }

    /**
     * Look up a word in whitelist or blacklist
     * @param id the identification of the user that lookup the node
     * @param requestId the unique request id
     * @param word the word to lookup
     * @return
     */
    public LookupResponseObject lookupWord(Identification id, String requestId, String word, Options options) {
        SearchProfile profile = searchProfiles.get(id.getProfileId());
        if (profile == null) {
            log.warn(MessageFormat.format("{0}:{1}: Error locating profile by profileId {2}", requestId, StatusCode.error_locating_namespace.code(), id.getProfileId()));
            return new LookupResponseObject(requestId, StatusCode.error_locating_profile, "Error locating profile");
        }
        String namespace = profile.getWhiteList().getNamespace();
        String namespaceId = getNamespaceIdByName(namespace, requestId);
        List<MedicalNode> nodes = null;
        try {
            nodes = medicalTaxonomyService.findNodesWithParents(word, namespaceId, options.matchSynonyms());
        } catch (DTSException ex) {
            log.warn(MessageFormat.format("{0}:{1}: Could not get keywords from taxonomy, reason: {2} ", requestId, StatusCode.error_getting_keywords_from_taxonomy.code(), ex.getMessage()));
            return new LookupResponseObject(requestId, StatusCode.error_getting_keywords_from_taxonomy, "Could not get keywords from taxonomy");
        }
        LookupResponseObject response = new LookupResponseObject(requestId, LookupResponseObject.ListType.NONE);
        if (nodes.size() != 0) {
            MedicalNode node = nodes.get(0);
            if (hasNamespaceReadAccess(node.getNamespaceId(), id.getProfileId(), requestId)) {
                for (MedicalNode parent : node.getParents()) {
                    if (parent.getName().equals(profile.getBlackList().getName())) {
                        response.setListType(LookupResponseObject.ListType.BLACKLIST);
                    } else if (parent.getName().equals(profile.getWhiteList().getName())) {
                        response.setListType(LookupResponseObject.ListType.WHITELIST);
                    } else if (parent.getName().equals(profile.getReviewList().getName())) {
                        if (hasNamespaceWriteAccess(node.getNamespaceId(), id.getProfileId(), requestId)) {
                            node = addNodeProperties(node, id, options);
                            try {
                                medicalTaxonomyService.createNodeProperties(node, false);
                                medicalTaxonomyService.setLastChangeNow(node.getNamespaceId());
                            } catch (InvalidPropertyTypeException ex) {
                                response.setStatusCode(StatusCode.invalid_node_property);
                                response.setErrorMessage("Invalid node property");
                                log.warn(MessageFormat.format("{0}:{1}: Invalid node property, reason: {2} ", requestId, StatusCode.invalid_node_property.code(), ex.getMessage()));
                            } catch (KeywordsException ex) {
                                response.setStatusCode(StatusCode.error_storing_property);
                                response.setErrorMessage("Error adding properties to keyword");
                                log.warn(MessageFormat.format("{0}:{1}: Error adding properties to keyword, reason: {2} ", requestId, StatusCode.error_storing_property.code(), ex.getMessage()));
                            }
                        } else {
                            response.setStatusCode(StatusCode.insufficient_namespace_privileges);
                            response.setErrorMessage("The profile is invalid or does not have read privileges to namespace " + profile.getReviewList().getName());
                            log.warn(MessageFormat.format("{0}:{1}: The profileId {2} is invalid or does not have write privileges to namespace {3}", requestId, StatusCode.insufficient_namespace_privileges.code(), id.getProfileId(), profile.getReviewList().getName()));
                        }
                    }
                }
            } else {
                response.setStatusCode(StatusCode.insufficient_namespace_privileges);
                response.setErrorMessage("The profile is invalid or does not have read privileges to namespace " + profile.getReviewList().getName());
                log.warn(MessageFormat.format("{0}:{1}: The profileId {2} is invalid or does not have read privileges to namespace {3}", requestId, StatusCode.insufficient_namespace_privileges.code(), id.getProfileId(), profile.getReviewList().getName()));
            }
        } else {
            MedicalNode reviewNode = null;
            try {
                reviewNode = medicalTaxonomyService.findNodes(profile.getReviewList().getName(), namespaceId, false).get(0);
            } catch (DTSException ex) {
                response.setStatusCode(StatusCode.error_locating_node);
                response.setErrorMessage("Unable to locate the reviewList node");
                log.warn(MessageFormat.format("{0}:{1}: Unable to locate the reviewList node", requestId, StatusCode.error_locating_node.code()));
            }
            if (hasNamespaceWriteAccess(reviewNode.getNamespaceId(), id.getProfileId(), requestId)) {
                try {
                    MedicalNode node = new MedicalNode();
                    node.setName(word);
                    node.setNamespaceId(namespaceId);
                    node = addNodeProperties(node, id, options);
                    node.addParent(reviewNode);
                    medicalTaxonomyService.createNewConcept(node);
                    medicalTaxonomyService.setLastChangeNow(node.getNamespaceId());
                } catch (InvalidPropertyTypeException ex) {
                    response.setStatusCode(StatusCode.error_editing_taxonomy);
                    response.setErrorMessage("Invalid property types");
                    log.warn(MessageFormat.format("{0}:{1}: Invalid property types", requestId, StatusCode.error_editing_taxonomy.code()));
                } catch (NodeNotFoundException ex) {
                    response.setStatusCode(StatusCode.error_editing_taxonomy);
                    response.setErrorMessage("Error locating the reviewList node");
                    log.warn(MessageFormat.format("{0}:{1}: Error locating the reviewList node", requestId, StatusCode.error_editing_taxonomy.code()));
                } catch (NodeAlreadyExistsException ex) {
                    response.setStatusCode(StatusCode.error_editing_taxonomy);
                    response.setErrorMessage("Node already exist");
                    log.error(MessageFormat.format("{0}:{1}: Node {2} already exist", requestId, StatusCode.error_editing_taxonomy.code(), word));
                } catch (KeywordsException ex) {
                    response.setStatusCode(StatusCode.error_editing_taxonomy);
                    response.setErrorMessage("Error creating keyword");
                    log.warn(MessageFormat.format("{0}:{1}: Error creating keywords, reason: {2}", requestId, StatusCode.error_editing_taxonomy.code(), ex.getMessage()));
                }
            } else {
                response.setStatusCode(StatusCode.insufficient_namespace_privileges);
                response.setErrorMessage("The profile is invalid or does not have write privileges to " + profile.getReviewList().getName());
                log.warn(MessageFormat.format("{0}:{1}: The profileId {2} is invalid or does not have write privileges to namespace {3}", requestId, StatusCode.insufficient_namespace_privileges.code(), id.getProfileId(), profile.getReviewList().getName()));
                return response;
            }
        }
        return response;
    }

    private MedicalNode addNodeProperties(MedicalNode node, Identification identification, Options options) {
        node.addProperty(profileIdPropertyName, identification.getProfileId());
        node.addProperty(userIdPropertyName, identification.getUserId());
        if (options.getUrl() != null) {
            node.addProperty(urlPropertyName, options.getUrl());
        }
        return node;
    }

    /**
     * Return all the children of a node
     * @param requestId the unique request id
     * @param path the path to the node
     * @param options Options object to filter on property. Only nodes which matches
     * at least one of the filters will be rutned.
     * @return a NodeListResponeObject that contains a list of all the childrenNodes
     * check the statuscode in this object to see if the operation was succesfull
     */
    public NodeListResponseObject getVocabulary(String requestId, String path, Options options) {
        List<MedicalNode> childNodes = new ArrayList<MedicalNode>();
        List<MedicalNode> responseChildNodes = new ArrayList<MedicalNode>();
        NodeListResponseObject response = new NodeListResponseObject();
        response.setRequestId(requestId);
        response.setStatusCode(StatusCode.ok);
        try {
            MedicalNode node = getNodeByPath(path, requestId);
            childNodes = medicalTaxonomyService.getChildNodes(node);
            if (childNodes.size() > 0 & options != null && options.getFilterByProperties() != null && !options.getFilterByProperties().isEmpty()) {
                responseChildNodes = filterByProperty(childNodes, options);
                response.setNodeList(responseChildNodes);
            } else {
                response.setNodeList(childNodes);
            }
        } catch (DTSException ex) {
            response.setErrorMessage(ex.getMessage());
            response.setStatusCode(StatusCode.error_locating_node);
        }
        return response;
    }

    /**
     * Add a new node to appelon
     * @param id the identification of the user that adds the node
     * @param requestId the unique request id
     * @param node the node to add, this node should already contain
     * the parent-relations
     * @return a ResponeObject, check the statuscode in this object to see
     * if the operation was succesfull
     */
    public ResponseObject addVocabularyNode(Identification id, String requestId, MedicalNode node, Options options) {
        ResponseObject response = new ResponseObject();
        response.setRequestId(requestId);
        if (hasNamespaceWriteAccess(node.getNamespaceId(), id.getProfileId(), requestId)) {
            try {
                if (options != null && options.synonymize()) {
                    try {
                        this.enrichNodeWithSynonyms(requestId, node);
                    } catch (Exception ex) {
                        log.warn(MessageFormat.format("{0}:{1}: Error communicating with Lemmatisation service", requestId, StatusCode.unknown_error.code()));
                    }
                }
                medicalTaxonomyService.createNewConcept(node);
                response.setStatusCode(StatusCode.ok);
                log.info("Created new node " + node.getName());
            } catch (InvalidPropertyTypeException ex) {
                response.setErrorMessage("Could not create new keyword, invalid property name");
                response.setStatusCode(StatusCode.error_editing_taxonomy);
                log.warn(MessageFormat.format("{0}:{1}: Could not create new keyword node {2}, invalid property name", requestId, StatusCode.error_editing_taxonomy.code(), node.getName()));
            } catch (NodeNotFoundException ex) {
                response.setErrorMessage("Could not create new keyword, unable to locate the specified parent");
                response.setStatusCode(StatusCode.error_editing_taxonomy);
                log.warn(MessageFormat.format("{0}:{1}: Could not create new keyword node {2}, unable to locate the specified parent", requestId, StatusCode.error_editing_taxonomy.code(), node.getName()));
            } catch (NodeAlreadyExistsException ex) {
                response.setErrorMessage("Could not create new keyword, the keyword already exists");
                response.setStatusCode(StatusCode.error_editing_taxonomy);
                log.warn(MessageFormat.format("{0}:{1}: Could not create new keyword node {2}, the keyword already exist", requestId, StatusCode.error_editing_taxonomy.code(), node.getName()));
            } catch (KeywordsException ex) {
                response.setErrorMessage("Could not create new keyword, error editing taxonomy");
                response.setStatusCode(StatusCode.error_editing_taxonomy);
                log.warn(MessageFormat.format("{0}:{1}: Could not create new keyword node {2}, error editing the taxonomy", requestId, StatusCode.error_editing_taxonomy.code(), node.getName()));
            }
        } else {
            response.setErrorMessage("The profile is invalid or does not have read privileges to target namespace");
            response.setStatusCode(StatusCode.insufficient_namespace_privileges);
            log.warn(MessageFormat.format("{0}:{1}: The profileId {2} is invalid or does not have write privileges to namespace with id {3}", requestId, StatusCode.insufficient_namespace_privileges.code(), id.getProfileId(), node.getNamespaceId()));
        }
        return response;
    }

    /**
     * Return all nodes that match a specified name, or has the specified word
     * as a synonym if that option is set to true
     * @param id the identification of the user searches for the node
     * @param name the name to look for
     * @param requestId the unique request id
     * @param options Specify if the function is to look for synonyms as well
     * @return
     */
    public NodeListResponseObject findNodesByName(Identification id, String namespaceName, String name, String requestId, Options options) {
        NodeListResponseObject response = new NodeListResponseObject();
        response.setStatusCode(StatusCode.ok);
        String nameSpaceId = getNamespaceIdByName(namespaceName, requestId);
        List<MedicalNode> list = null;
        List<MedicalNode> filteredNodes = new ArrayList<MedicalNode>();
        if (nameSpaceId == null) {
            response.setStatusCode(StatusCode.error_locating_namespace);
            response.setErrorMessage("Error locating namespaceId by namespace name. Note that namespace is case sensitive.");
            log.warn(MessageFormat.format("{0}:{1}: Error locating namespaceId by namespace {2}", requestId, StatusCode.error_locating_namespace.code(), namespaceName));
            return response;
        }
        if (hasNamespaceReadAccess(nameSpaceId, id.getProfileId(), requestId)) {
            try {
                list = medicalTaxonomyService.findNodes(name, nameSpaceId, options.matchSynonyms(), options.getWordsToReturn());
                response.setNodeList(list);
            } catch (DTSException ex) {
                response.setStatusCode(StatusCode.error_getting_keywords_from_taxonomy);
                response.setErrorMessage("Unable to retrieve nodes from taxonomy");
                log.warn(MessageFormat.format("{0}:{1}: Unable to retrieve nodes from taxonomy, reason: {2}", requestId, StatusCode.error_getting_keywords_from_taxonomy.code(), ex.getMessage()));
            }
        } else {
            response.setStatusCode(StatusCode.insufficient_namespace_privileges);
            response.setErrorMessage("The profile is invalid or does not have read privileges to target namespace");
            log.warn(MessageFormat.format("{0}:{1}: The profileId {2} is invalid or does not have read privileges to namespace {3}", requestId, StatusCode.insufficient_namespace_privileges.code(), id.getProfileId(), namespaceName));
        }
        return response;
    }

    public NodeListResponseObject findNodes(Identification id, String namespaceName, String requestId, Options options) {
        NodeListResponseObject response = new NodeListResponseObject();
        response.setStatusCode(StatusCode.ok);
        String nameSpaceId = getNamespaceIdByName(namespaceName, requestId);
        List<MedicalNode> list = null;
        if (nameSpaceId == null) {
            response.setStatusCode(StatusCode.error_locating_namespace);
            response.setErrorMessage("Error locating namespaceId by namespace name. Note that namespace is case sensitive.");
            log.warn(MessageFormat.format("{0}:{1}: Error locating namespaceId by namespace {2}", requestId, StatusCode.error_locating_namespace.code(), namespaceName));
            return response;
        }
        Map<String, List<String>> properties = options.getFilterByProperties();
        Set<String> propertySet = properties.keySet();
        String propertyKey = "";
        String propertyValue = "";
        for (Entry<String, List<String>> propertyEntry : properties.entrySet()) {
            propertyKey = propertyEntry.getKey();
            propertyValue = propertyEntry.getValue().get(0);
            break;
        }
        if (hasNamespaceReadAccess(nameSpaceId, id.getProfileId(), requestId)) {
            try {
                list = medicalTaxonomyService.findNodesByProperty(nameSpaceId, propertyKey, propertyValue, options.getWordsToReturn());
                response.setNodeList(list);
            } catch (KeywordsException ex) {
                response.setStatusCode(StatusCode.error_getting_keywords_from_taxonomy);
                response.setErrorMessage(ex.getMessage());
                log.warn(MessageFormat.format("{0}:{1}: Unable to retrieve nodes from taxonomy, reason: {2}", requestId, StatusCode.error_getting_keywords_from_taxonomy.code(), ex.getMessage()));
            }
        } else {
            response.setStatusCode(StatusCode.insufficient_namespace_privileges);
            response.setErrorMessage("The profile is invalid or does not have read privileges to target namespace");
            log.warn(MessageFormat.format("{0}:{1}: The profileId {2} is invalid or does not have read privileges to namespace {3}", requestId, StatusCode.insufficient_namespace_privileges.code(), id.getProfileId(), namespaceName));
        }
        return response;
    }

    /**
     * Return all nodes that are filtered out after the FilterByProperty is applied
     * to the full list of nodes
     * @param list - full list of medical nodes
     * @param options - Options object to filter on property. Only nodes which match
     * at least one of the filters will be returned.
     * @return a list of Medical Nodes
     */
    private List<MedicalNode> filterByProperty(List<MedicalNode> list, Options options) {
        List<MedicalNode> filteredNodes = new ArrayList<MedicalNode>();
        for (MedicalNode node : list) {
            outer: for (NodeProperty prop : node.getProperties()) {
                for (Entry<String, List<String>> filterEntry : options.getFilterByProperties().entrySet()) {
                    if (filterEntry.getKey().equals(prop.getName())) {
                        for (String filter : filterEntry.getValue()) {
                            if (filter.equals(prop.getValue())) {
                                log.debug("Node matches filter. Adding node " + node.getName() + " to the list of nodes to return");
                                filteredNodes.add(node);
                                break outer;
                            }
                        }
                    }
                }
            }
        }
        return filteredNodes;
    }

    /**
     * Move a node in a vocabulary, that is: change the parent of the node
     * @param id the identification of the user that moves the node
     * @param requestId the unique request id
     * @param nodeId the id of the node to move
     * @param destNodeId The path to the new parent
     * @return a ResponseObject, check the statuscode in this object to see
     * if the operation was succesfull
     */
    public ResponseObject moveVocabularyNode(Identification id, String requestId, MedicalNode node, String destNodePath, Options options) {
        ResponseObject response = new ResponseObject(requestId);
        response.setStatusCode(StatusCode.ok);
        MedicalNode destNode = null;
        try {
            destNode = getNodeByPath(destNodePath, requestId);
            if (hasNamespaceWriteAccess(node.getNamespaceId(), id.getProfileId(), requestId) && hasNamespaceWriteAccess(destNode.getNamespaceId(), id.getProfileId(), requestId)) {
                if (options != null && options.synonymize()) {
                    try {
                        this.enrichNodeWithSynonyms(requestId, node);
                    } catch (Exception ex) {
                        log.warn(MessageFormat.format("{0}:{1}: Error communicating with Lemmatisation service", requestId, StatusCode.unknown_error.code()));
                    }
                }
                medicalTaxonomyService.moveNode(node, destNode);
                medicalTaxonomyService.setLastChangeNow(destNode.getNamespaceId());
            } else {
                response.setErrorMessage("The profile is invalid or does not have read or write privileges to target namespace");
                response.setStatusCode(StatusCode.insufficient_namespace_privileges);
                log.warn(MessageFormat.format("{0}:{1}: The profileId {2} is invalid or does not have read privileges to namespace with id {3}", requestId, StatusCode.insufficient_namespace_privileges.code(), id.getProfileId(), node.getNamespaceId()));
            }
        } catch (DTSException ex) {
            response.setStatusCode(StatusCode.error_editing_taxonomy);
            response.setErrorMessage(requestId);
        } catch (KeywordsException ex) {
            response.setStatusCode(StatusCode.error_editing_taxonomy);
            response.setErrorMessage("Error editing taxonomy: Node could not be moved");
            log.error(MessageFormat.format("{0}:{1}: Node ({2}) could not be moved to parent {{3}}", requestId, StatusCode.error_editing_taxonomy.code(), node.getInternalId(), destNode.getInternalId()), ex);
        } catch (NodeNotFoundException ex) {
            response.setStatusCode(StatusCode.error_editing_taxonomy);
            response.setErrorMessage("Error editing taxonomy: Node could not be found");
            log.error(MessageFormat.format("{0}:{1}:{2}", requestId, StatusCode.error_editing_taxonomy.code(), ex.getMessage()), ex);
        } catch (Exception ex) {
            response.setStatusCode(StatusCode.error_editing_taxonomy);
            response.setErrorMessage("Error editing taxonomy: Node could not be found");
            log.error(MessageFormat.format("{0}:{1}:{2}", requestId, StatusCode.error_editing_taxonomy.code(), ex.getMessage()), ex);
        }
        return response;
    }

    /**
     * Update the content of a node (not implemented yet)
     * @param id the identification of the user that updates the node
     * @param requestId the unique request id
     * @param node the updated node, the id of the node must not be changed.
     * @return ResponseObject with status information.
     */
    public ResponseObject updateVocabularyNode(Identification id, String requestId, MedicalNode node, Options options) {
        ResponseObject response = new ResponseObject(requestId);
        if (hasNamespaceWriteAccess(node.getNamespaceId(), id.getProfileId(), requestId)) {
            try {
                if (options != null && options.synonymize()) {
                    try {
                        this.enrichNodeWithSynonyms(requestId, node);
                    } catch (Exception ex) {
                        log.warn(MessageFormat.format("{0}:{1}: Error communicating with Lemmatisation service", requestId, StatusCode.unknown_error.code()));
                    }
                }
                medicalTaxonomyService.updateConcept(node);
                response.setStatusCode(StatusCode.ok);
            } catch (NodeNotFoundException ex) {
                response.setErrorMessage("Error locating the node");
                response.setStatusCode(StatusCode.error_editing_taxonomy);
                log.warn(MessageFormat.format("{0}:{1}: Error locating the node {2}", requestId, StatusCode.error_locating_namespace.code(), node.getName()));
            } catch (KeywordsException ex) {
                response.setErrorMessage("Error updating the node");
                response.setStatusCode(StatusCode.error_editing_taxonomy);
                log.warn(MessageFormat.format("{0}:{1}: Error updating the node, erason: {2}", requestId, StatusCode.error_locating_namespace.code(), ex.getMessage()));
            } catch (InvalidPropertyTypeException ex) {
                response.setErrorMessage("One of the properties was of an undefined type");
                response.setStatusCode(StatusCode.error_editing_taxonomy);
                log.warn(MessageFormat.format("{0}:{1}: One of the properies {2} was of an indefined type", requestId, StatusCode.error_locating_namespace.code(), node.getProperties().toString()));
            } catch (ParentNotFoundException ex) {
                response.setErrorMessage("Error locating the specified parent");
                response.setStatusCode(StatusCode.error_editing_taxonomy);
                log.warn(MessageFormat.format("{0}:{1}: Error locating the specified parent", requestId, StatusCode.error_locating_namespace.code()));
            }
        } else {
            response.setErrorMessage("The profile is invalid or does not have write privileges to target namespace");
            response.setStatusCode(StatusCode.insufficient_namespace_privileges);
            log.warn(MessageFormat.format("{0}:{1}: The profileId {2} is invalid or does not have write privileges to namespace with id {3}", requestId, StatusCode.insufficient_namespace_privileges.code(), id.getProfileId(), node.getNamespaceId()));
        }
        return response;
    }

    /**
     * Retrieves the XML representation of an Apelon namespaceId.
     * Only preconfiguered namespaces can be selected.
     * The namespaceId configuration is available by the classpath
     * resource <code>keywordservice-svc.properties</code>.
     *
     * @param id User identification
     * @param requestId unique request identifier
     * @param namespaceId The namespaceId to export
     */
    public XMLResponseObject getNamespaceXml(Identification id, String requestId, String namespace) {
        XMLResponseObject response = new XMLResponseObject();
        response.setRequestId(requestId);
        Long now = new Date().getTime();
        if (allowedNamespaces.contains(namespace)) {
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            List<MedicalNode> nodeList = getVocabulary(requestId, namespace, null).getNodeList();
            try {
                writer = factory.createXMLStreamWriter(out, "utf-8");
                writer.writeStartDocument();
                writer.writeComment("XML generated: " + new java.util.Date().toString());
                writer.writeStartElement("namespace");
                if (nodeList != null) {
                    for (MedicalNode node : nodeList) {
                        traverseChildNodes(node, requestId, writer, namespace);
                    }
                }
                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
                response.setXml(out.toString());
                writer.close();
                out.close();
                response.setStatusCode(XMLResponseObject.StatusCode.ok);
            } catch (IOException ex) {
                response.setErrorMessage("Error exporting namespace to XML");
                response.setStatusCode(XMLResponseObject.StatusCode.error_processing_content);
                log.warn(MessageFormat.format("{0}:{1}: Error exporting namespace {2} to xml, reason: {3}", requestId, StatusCode.error_locating_namespace.code(), namespace, ex.getMessage()));
            } catch (XMLStreamException ex) {
                response.setStatusCode(XMLResponseObject.StatusCode.error_processing_content);
                response.setErrorMessage("Error exporting namespace to XML: " + ex.getMessage());
                log.warn(MessageFormat.format("{0}:{1}: Error exporting namespace {2} to xml, reason: {3}", requestId, StatusCode.error_locating_namespace.code(), namespace, ex.getMessage()));
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        } else {
            response.setStatusCode(XMLResponseObject.StatusCode.insufficient_namespace_privileges);
            response.setErrorMessage("Namespace is not in the list of namespaces authorized for xml export");
            log.warn(MessageFormat.format("{0}:{1}: Error exporting xml, namespace {2} is not in the list of namespaces authorized for xml export", requestId, StatusCode.error_locating_namespace.code(), namespace));
        }
        response.setTime(now);
        return response;
    }

    /**
     * Helper routine to traverse all child nodes recursively from a parent node
     *
     * @param node The node to recurse from
     * @param requestId The request identifier
     * @param writer Writer used to produce xml for the node
     * @param path The path to the node using '/' as a path separator
     * @throws XMLStreamException
     */
    private void traverseChildNodes(MedicalNode node, String requestId, XMLStreamWriter writer, String path) throws XMLStreamException {
        writer.writeStartElement("node");
        writer.writeStartElement("name");
        writer.writeCharacters(node.getName());
        writer.writeEndElement();
        writer.writeStartElement("internalId");
        writer.writeCharacters(node.getInternalId());
        writer.writeEndElement();
        writer.writeStartElement("namespaceId");
        writer.writeCharacters(node.getNamespaceId());
        writer.writeEndElement();
        writer.writeStartElement("sourceId");
        writer.writeCharacters(node.getSourceId());
        writer.writeEndElement();
        writer.writeStartElement("path");
        writer.writeCharacters(path);
        writer.writeEndElement();
        writer.writeStartElement("properties");
        for (NodeProperty prop : node.getProperties()) {
            writer.writeStartElement(prop.getName());
            writer.writeCharacters(prop.getValue());
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeStartElement("synonyms");
        for (String synonym : node.getSynonyms()) {
            writer.writeStartElement("synonym");
            writer.writeCharacters(synonym);
            writer.writeEndElement();
        }
        writer.writeEndElement();
        writer.writeStartElement("hasChildren");
        writer.writeCharacters(node.getHasChildren() ? "true" : "false");
        writer.writeEndElement();
        String childpath = path + "/" + node.getName();
        List<MedicalNode> nodeList = getVocabulary(requestId, path, null).getNodeList();
        if (nodeList != null) {
            writer.writeStartElement("children");
            for (MedicalNode child : nodeList) {
                writer.writeStartElement("child");
                writer.writeCharacters(child.getName());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
        if (nodeList != null) {
            for (MedicalNode n : nodeList) {
                traverseChildNodes(n, requestId, writer, childpath);
            }
        }
    }

    /**
     * Check if a used has read access to the given namespaceId. This routine
     * makes use of the namespaceId cache and updates it where neccessary.
     *
     * @param namespaceName The id of the namespaceId used in the request
     * @param profileId The id of the profile used in the request
     * @param requestId The request identifier
     * @return True if the profile has read access to the namespaceId
     */
    private boolean hasNamespaceReadAccess(String namespaceId, String profileId, String requestId) {
        String namespace = getNamespaceById(namespaceId, requestId);
        if (namespace != null) {
            SearchProfile profile = searchProfiles.get(profileId);
            if (profile != null) {
                if (profile.getSearchableNamespaces().contains(namespace)) {
                    return true;
                } else {
                    log.warn(MessageFormat.format("{0}:{1}: Submitted profileId {2} does not have read privileges to namespace {3}", requestId, StatusCode.insufficient_namespace_privileges.code(), profileId, namespace));
                }
            } else {
                log.warn(MessageFormat.format("{0}:{1}: Submitted profileId {2} does not match any predefined search profile", requestId, StatusCode.insufficient_namespace_privileges.code(), profileId));
            }
        } else {
            log.warn(MessageFormat.format("{0}:{1}: Error locating namespace for namespaceId {2}. Note that namespace is case sensitive", requestId, StatusCode.invalid_parameter.code(), namespaceId));
        }
        return false;
    }

    /**
     * Check if a used has write access to the given namespaceId. This routine
     * makes use of the namespaceId cache and updates it where neccessary.
     *
     * @param namespaceName The id of the namespaceId used in the request
     * @param profileId The id of the profile used in the request
     * @param requestId The request identifier
     * @return True if the profile has write access to the namespaceId
     */
    private boolean hasNamespaceWriteAccess(String namespaceId, String profileId, String requestId) {
        String namespace = getNamespaceById(namespaceId, requestId);
        if (namespace != null) {
            SearchProfile profile = searchProfiles.get(profileId);
            if (profile != null) {
                if (profile.getWriteableNamespaces().contains(namespace)) {
                    return true;
                } else {
                    log.warn(MessageFormat.format("{0}:{1}: Submitted profileId {2} does not have read privileges to namespace {3}", requestId, StatusCode.insufficient_namespace_privileges.code(), profileId, namespace));
                }
            } else {
                log.warn(MessageFormat.format("{0}:{1}: Submitted profileId {2} does not match any predefined search profile", requestId, StatusCode.insufficient_namespace_privileges.code(), profileId));
            }
        } else {
            log.warn(MessageFormat.format("{0}:{1}: Error locating namespace for namespaceId {2}. Note that namespace is case sensitive.", requestId, StatusCode.invalid_parameter.code(), namespaceId));
        }
        return false;
    }

    /**
     * A utility routine to get the namespaceId name from a namespaceName.
     * This routine initially checks the namespaceCache. If no match is
     * found it retrieves the namespaceId name and updates the namespaceCache.
     *
     * @param namespaceName The id of the namespaceId to lookup. Must be capable
     * of being converted to an integer.
     * @param requestId The request identifier
     * @return The namespaceId or null if an error occured
     */
    private String getNamespaceById(String namespaceId, String requestId) {
        String namespace = namespaceCache.get(namespaceId);
        if (namespace != null) {
            return namespace;
        }
        try {
            namespace = medicalTaxonomyService.findNamespaceById(Integer.parseInt(namespaceId));
            namespaceCache.put(namespaceId, namespace);
            return namespace;
        } catch (NumberFormatException ex) {
            log.warn(MessageFormat.format("{0}:{1}:Unable to locate namespace name. NamespaceId {2} cannot be converted to an integer.", requestId, StatusCode.invalid_parameter.code(), namespaceId));
        } catch (Exception ex) {
            log.warn(MessageFormat.format("{0}:{1}:Error retrieving namespace", requestId, StatusCode.error_locating_namespace.code()), ex);
        }
        return null;
    }

    /**
     * A utility routine to get the namespace id from a namespace name.
     * This routine initially checks the namespaceCache. If no match is
     * found it retrieves the namespace id from the taxonomy service and updates the namespaceCache.
     *
     * @param namespaceName The name of the namespace to lookup.
     * @param requestId The request identifier
     * @return The namespace id or null if an error occured
     */
    private String getNamespaceIdByName(String namespaceName, String requestId) {
        String namespaceId = namespaceCache.get(namespaceName);
        if (namespaceId != null) {
            return namespaceId;
        }
        try {
            namespaceId = medicalTaxonomyService.findNamespaceIdByName(namespaceName);
            namespaceCache.put(namespaceName, namespaceId);
            return namespaceId;
        } catch (Exception ex) {
            log.warn(MessageFormat.format("{0}:{1}:Error retrieving namespace {2}", requestId, StatusCode.error_locating_namespace.code(), namespaceName), ex);
        }
        return null;
    }

    /**
     * Helper routine to retrieve a node by it's path.
     * @see #getNodeByPath(se.vgregion.metaservice.keywordservice.domain.NodePath, java.lang.String)
     */
    private MedicalNode getNodeByPath(String path, String requestId) throws DTSException {
        NodePath nodePath = new NodePath();
        nodePath.setPath(path);
        return getNodeByPath(nodePath, requestId);
    }

    /**
     * Helper routine to retrieve a node by it's path. A path is
     * constructed accordion to: <code>namespace/path/to/node</code>.
     *
     * @param nodePath The path to the node to locate
     * @param requestId Request identifier
     * @return The medical node at the given path
     * @throws DTSException If the medical node could not be located
     */
    private MedicalNode getNodeByPath(NodePath nodePath, String requestId) throws DTSException {
        if (nodePath.getNamespace() == null) {
            throw new DTSException("Path parameter can not be empty");
        }
        String namespaceId = getNamespaceIdByName(nodePath.getNamespace(), requestId);
        LinkedList<String> pathList = new LinkedList(Arrays.asList(nodePath.getRelativePath().split("/")));
        MedicalNode node = null;
        while (!pathList.isEmpty()) {
            try {
                node = medicalTaxonomyService.getChildNode(namespaceId, node, pathList.getFirst());
                pathList.removeFirst();
            } catch (Exception ex) {
                log.warn(MessageFormat.format("{0}:{1}: Error retrieving node {2} when traversing the path {3}", requestId, StatusCode.error_getting_keywords_from_taxonomy.code(), pathList.getFirst(), pathList.getFirst(), nodePath.getPath()));
                throw new DTSException("Error retrieving node from taxonomy, possible incorrect path parameter");
            }
            if (node == null) {
                log.error(MessageFormat.format("{0}:{1}: Invalid path {2}", requestId, StatusCode.invalid_parameter.code(), nodePath.getPath()));
                throw new DTSException("Invalid path");
            }
        }
        return node;
    }

    public void setSearchProfiles(List<SearchProfile> searchProfiles) {
        this.searchProfiles = new HashMap<String, SearchProfile>();
        for (SearchProfile profile : searchProfiles) {
            this.searchProfiles.put(profile.getProfileId(), profile);
        }
    }

    public void setMedicalTaxonomyService(MedicalTaxonomyService medicalTaxonomyService) {
        this.medicalTaxonomyService = medicalTaxonomyService;
    }

    public void setNamepacesExposedToXmlApi(String allowedNamespaces) {
        Set<String> set = new HashSet<String>();
        String[] arr = allowedNamespaces.split(",");
        if (arr != null) {
            for (String str : arr) {
                set.add(str.trim());
            }
        }
        this.allowedNamespaces = set;
    }

    public void setRestClient(LemmatisationRestClient restClient) {
        this.lemmatisationRestClient = restClient;
    }

    /**
     * Performs a request to the LemmatisationService to find the
     * lemma and all paradigms of a word. All found paradigms
     * will be added to the node as synonyms, unless the word is
     * already a synonym.
     *
     * @param requestId Request identifier
     * @param node The node to enrich with synonyms
     * @throws Exception If the communcation with the LemmatisationService failed
     */
    private void enrichNodeWithSynonyms(String requestId, MedicalNode node) throws Exception {
        LemmatisedResponse lemmas = lemmatisationRestClient.getLemmatisedResponse(node.getName());
        if (lemmas.getStatusCode() == se.vgregion.metaservice.lemmatisation.generated.StatusCode.OK) {
            if (lemmas.getList().size() == 1) {
                for (LemmatisedObject lemmaObj : lemmas.getList()) {
                    int numSynonyms = 0;
                    for (String paradigm : lemmaObj.getParadigms()) {
                        if (!node.getSynonyms().contains(paradigm)) {
                            node.addSynonym(paradigm);
                            numSynonyms++;
                        }
                    }
                    log.info("Added " + numSynonyms + " synonyms to node " + node.getName());
                }
            } else if (lemmas.getList().size() > 1) {
                log.debug("Skipping synomizing node " + node.getName() + ", more than one lemma matches");
            }
        } else {
            log.warn(MessageFormat.format("{0}{1}Unable to add synonyms to node, reason: {2}", requestId, StatusCode.unknown_error, lemmas.getErrorMessage()));
        }
    }
}
