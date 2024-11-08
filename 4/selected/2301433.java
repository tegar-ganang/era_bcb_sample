package com.googlecode.fascinator.redbox;

import com.googlecode.fascinator.api.PluginDescription;
import com.googlecode.fascinator.api.PluginException;
import com.googlecode.fascinator.api.PluginManager;
import com.googlecode.fascinator.api.indexer.Indexer;
import com.googlecode.fascinator.api.indexer.SearchRequest;
import com.googlecode.fascinator.api.storage.DigitalObject;
import com.googlecode.fascinator.api.storage.Payload;
import com.googlecode.fascinator.api.storage.Storage;
import com.googlecode.fascinator.api.storage.StorageException;
import com.googlecode.fascinator.api.transformer.Transformer;
import com.googlecode.fascinator.api.transformer.TransformerException;
import com.googlecode.fascinator.common.JsonObject;
import com.googlecode.fascinator.common.JsonSimple;
import com.googlecode.fascinator.common.JsonSimpleConfig;
import com.googlecode.fascinator.common.messaging.MessagingException;
import com.googlecode.fascinator.common.messaging.MessagingServices;
import com.googlecode.fascinator.common.solr.SolrDoc;
import com.googlecode.fascinator.common.solr.SolrResult;
import fedora.client.FedoraClient;
import fedora.server.management.FedoraAPIM;
import fedora.server.types.gen.Datastream;
import fedora.server.types.gen.DatastreamDef;
import fedora.server.types.gen.UserInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Transformer to notify VITAL of completed objects in ReDBox.
 * 
 * @author Greg Pendlebury
 */
public class VitalTransformer implements Transformer {

    /** Logging */
    private final Logger log = LoggerFactory.getLogger(VitalTransformer.class);

    /** Messaging */
    private MessagingServices messaging;

    private String emailQueue;

    private List<String> emailAddresses;

    private String emailSubject;

    private String emailTemplate;

    /** Fascinator plugins */
    private Storage storage;

    private Indexer indexer;

    /** Fedora */
    private String fedoraUrl;

    private String fedoraUsername;

    private String fedoraPassword;

    private String fedoraNamespace;

    private String fedoraMessageTemplate;

    private int fedoraTimeout;

    /** Valid instantiation */
    boolean valid = false;

    /** VITAL integration config */
    private Map<String, JsonSimple> pids;

    private String attachDs;

    private String attachStatusField;

    private Map<String, String> attachStatuses;

    private String attachLabelField;

    private Map<String, String> attachLabels;

    private String attachControlGroup;

    private boolean attachVersionable;

    private boolean attachRetainIds;

    private Map<String, List<String>> attachAltIds;

    private File foxmlTemplate;

    /** Temp directory */
    private File tmpDir;

    /** Wait conditions */
    private List<String> waitProperties;

    /**
     * Gets an identifier for this type of plugin. This should be a simple name
     * such as "file-system" for a storage plugin, for example.
     * 
     * @return the plugin type id
     */
    @Override
    public String getId() {
        return "vital";
    }

    /**
     * Gets a name for this plugin. This should be a descriptive name.
     * 
     * @return the plugin name
     */
    @Override
    public String getName() {
        return "VITAL Transformer";
    }

    /**
     * Gets a PluginDescription object relating to this plugin.
     * 
     * @return a PluginDescription
     */
    @Override
    public PluginDescription getPluginDetails() {
        return new PluginDescription(this);
    }

    /**
     * Initializes the plugin using the specified JSON String
     * 
     * @param jsonString JSON configuration string
     * @throws TransformerException if there was an error in initialization
     */
    @Override
    public void init(String jsonString) throws TransformerException {
        try {
            setConfig(new JsonSimpleConfig(jsonString));
        } catch (IOException e) {
            throw new TransformerException(e);
        }
    }

    /**
     * Initializes the plugin using the specified JSON configuration
     * 
     * @param jsonFile JSON configuration file
     * @throws TransformerException if there was an error in initialization
     */
    @Override
    public void init(File jsonFile) throws TransformerException {
        try {
            setConfig(new JsonSimpleConfig(jsonFile));
        } catch (IOException ioe) {
            throw new TransformerException(ioe);
        }
    }

    /**
     * Initialization of plugin
     * 
     * @param config The configuration to use
     * @throws TransformerException if fails to initialize
     */
    private void setConfig(JsonSimpleConfig config) throws TransformerException {
        fedoraUrl = config.getString(null, Strings.CONFIG_SERVER, "url");
        fedoraNamespace = config.getString(null, Strings.CONFIG_SERVER, "namespace");
        fedoraUsername = config.getString(null, Strings.CONFIG_SERVER, "username");
        fedoraPassword = config.getString(null, Strings.CONFIG_SERVER, "password");
        fedoraTimeout = config.getInteger(15, Strings.CONFIG_SERVER, "timeout");
        if (fedoraUrl == null || fedoraNamespace == null || fedoraUsername == null || fedoraPassword == null) {
            throw new TransformerException("Valid fedora configuration is missing!");
        }
        fedoraConnect(true);
        fedoraMessageTemplate = config.getString(Strings.DEFAULT_VITAL_MESSAGE, Strings.CONFIG_SERVER, "message");
        boolean success = false;
        String tempPath = config.getString(System.getProperty("java.io.tmpdir"), "tempDir");
        if (tempPath != null) {
            tmpDir = new File(tempPath);
            if (!tmpDir.exists()) {
                success = tmpDir.mkdirs();
            } else {
                success = tmpDir.isDirectory();
            }
            if (success) {
                File file = new File(tmpDir, "creation.test");
                try {
                    file.createNewFile();
                    file.delete();
                    success = !file.exists();
                } catch (IOException ex) {
                    success = false;
                }
            }
        }
        if (tmpDir == null || !success) {
            throw new TransformerException("Cannot find a valid (and writable) TEMP directory!");
        }
        pids = config.getJsonSimpleMap("dataStreams");
        if (pids == null || pids.isEmpty()) {
            throw new TransformerException("No datastreams configured to export!");
        }
        JsonSimple attachmentsConfig = new JsonSimple(config.getObject("attachments"));
        attachDs = attachmentsConfig.getString("ATTACHMENT%02d", "dsID");
        Pattern p = Pattern.compile("%\\d*d");
        Matcher m = p.matcher(attachDs);
        if (!m.find()) {
            throw new TransformerException("'*/attachments/dsId' must have a format placeholder for incrementing integer, eg. '%d' or '%02d'. The value provided ('" + attachDs + "') is invalid");
        }
        attachStatusField = attachmentsConfig.getString(null, "statusField");
        attachStatuses = getStringMap(attachmentsConfig, "status");
        attachLabelField = attachmentsConfig.getString(null, "labelField");
        attachLabels = getStringMap(attachmentsConfig, "label");
        attachControlGroup = attachmentsConfig.getString(null, "controlGroup");
        attachVersionable = attachmentsConfig.getBoolean(false, "versionable");
        attachRetainIds = attachmentsConfig.getBoolean(true, "retainIds");
        attachAltIds = new LinkedHashMap<String, List<String>>();
        JsonSimple json;
        try {
            json = new JsonSimple(config.toString());
        } catch (IOException ex) {
            throw new TransformerException("Error parsing attachment JSON", ex);
        }
        JsonObject objAltIds = json.getObject("attachments", "altIds");
        JsonSimple altIds = new JsonSimple(objAltIds);
        for (Object oKey : objAltIds.keySet()) {
            String key = (String) oKey;
            List<String> ids = altIds.getStringList(key);
            if (ids.isEmpty()) {
                log.warn("WARNING: '{}' has no altIds configured.", key);
            } else {
                attachAltIds.put(key, ids);
            }
        }
        if (!attachAltIds.containsKey(Strings.LITERAL_DEFAULT)) {
            attachAltIds.put(Strings.LITERAL_DEFAULT, new ArrayList<String>());
        }
        emailQueue = config.getString(null, Strings.CONFIG_FAILURE, "emailQueue");
        if (emailQueue != null) {
            emailAddresses = config.getStringList(Strings.CONFIG_FAILURE, "emailAddress");
            if (emailAddresses != null && !emailAddresses.isEmpty()) {
                emailSubject = config.getString(Strings.DEFAULT_EMAIL_SUBJECT, Strings.CONFIG_FAILURE, "emailSubject");
                emailTemplate = config.getString(Strings.DEFAULT_EMAIL_TEMPLATE, Strings.CONFIG_FAILURE, "emailTemplate");
            } else {
                log.error("No email address provided! Reverting to errors using log files");
                emailQueue = null;
            }
        } else {
            log.warn("No email queue provided. Errors will only be logged");
        }
        if (emailQueue != null) {
            try {
                messaging = MessagingServices.getInstance();
            } catch (MessagingException ex) {
                throw new TransformerException("Error starting Messaging Services", ex);
            }
        }
        File sysFile = null;
        try {
            sysFile = JsonSimpleConfig.getSystemFile();
        } catch (IOException ioe) {
            log.error("Failed to read configuration: {}", ioe.getMessage());
            throw new TransformerException("Failed to read configuration", ioe);
        }
        try {
            storage = PluginManager.getStorage(config.getString("file-system", "storage", "type"));
            storage.init(sysFile);
        } catch (PluginException pe) {
            log.error("Failed to initialise plugin: {}", pe.getMessage());
            throw new TransformerException("Failed to initialise storage", pe);
        }
        try {
            indexer = PluginManager.getIndexer(config.getString("solr", "indexer", "type"));
            indexer.init(sysFile);
        } catch (PluginException pe) {
            log.error("Failed to initialise plugin: {}", pe.getMessage());
            throw new TransformerException("Failed to initialise indexer", pe);
        }
        String templatePath = config.getString(null, "foxmlTemplate");
        if (templatePath != null) {
            foxmlTemplate = new File(templatePath);
            if (!foxmlTemplate.exists()) {
                foxmlTemplate = null;
                throw new TransformerException("The new object template provided does not exist: '" + templatePath + "'");
            }
        }
        waitProperties = new ArrayList<String>();
        Map<String, String> waitConditions = getStringMap(config, "waitConditions");
        if (waitConditions != null) {
            for (String type : waitConditions.keySet()) {
                String value = waitConditions.get(type);
                if (value == null) {
                    continue;
                }
                if (type.equals("property")) {
                    log.info("New wait condition: Property '{}'.", value);
                    waitProperties.add(value);
                }
            }
        }
        valid = true;
    }

    /**
     * Trivial wrapper on the JsonConfigHelper getMap() method to cast all map
     * entries to strings if appropriate and return.
     * 
     * @param json The json object to query.
     * @param path The path on which the map is found.
     * @return Map<String, String>: The object map cast to Strings
     */
    private Map<String, String> getStringMap(JsonSimple json, String... path) {
        Map<String, String> response = new LinkedHashMap<String, String>();
        JsonObject object = json.getObject((Object[]) path);
        if (object == null) {
            return null;
        }
        for (Object key : object.keySet()) {
            Object value = object.get(key);
            if (value instanceof String) {
                response.put((String) key, (String) value);
            }
        }
        return response;
    }

    /**
     * Establish a connection to Fedora's management API (API-M) to confirm
     * credentials, then return the instantiated fedora client used to connect.
     * 
     * @param firstConnection If this is the first connection (ie. from the
     *      Constructor), set this flag. Some logging will occur, and a basic
     *      API call will be triggered to test genuine connectivity with regards
     *      to the network and the credentials supplied.
     * @return FedoraClient The client used to connect to the API
     * @throws TransformerException if there was an error
     */
    private FedoraClient fedoraConnect() throws TransformerException {
        return fedoraConnect(false);
    }

    private FedoraClient fedoraConnect(boolean firstConnection) throws TransformerException {
        FedoraClient fedora = null;
        try {
            fedora = new FedoraClient(fedoraUrl, fedoraUsername, fedoraPassword);
            fedora.SOCKET_TIMEOUT_SECONDS = fedoraTimeout;
            if (firstConnection) {
                log.info("Connected to FEDORA : '{}'", fedoraUrl);
            }
            String version = fedora.getServerVersion();
            if (!version.startsWith(Strings.FEDORA_VERSION_TEST)) {
                throw new StorageException("Error; this plugin is designed to work with Fedora versions 2.x");
            }
            if (firstConnection) {
                log.info("FEDORA version: '{}'", version);
            }
            FedoraAPIM apim = fedora.getAPIM();
            if (firstConnection) {
                log.info("API-M access testing... {} second timeout", fedoraTimeout);
                UserInfo user = apim.describeUser(fedoraUsername);
                log.info("API-M access confirmed: User '{}', ID: '{}'", fedoraUsername, user.getId());
            }
        } catch (MalformedURLException ex) {
            throw new TransformerException("Server URL is Invalid (?) : ", ex);
        } catch (IOException ex) {
            throw new TransformerException("Error connecting to VITAL! : ", ex);
        } catch (Exception ex) {
            throw new TransformerException("Error accesing management API! : ", ex);
        }
        return fedora;
    }

    /**
     * Shuts down the plugin
     * 
     * @throws TransformerException if there was an error during shutdown
     */
    @Override
    public void shutdown() throws TransformerException {
        if (storage != null) {
            try {
                storage.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown storage: {}", pe.getMessage());
                throw new TransformerException("Failed to shutdown storage", pe);
            }
        }
        if (indexer != null) {
            try {
                indexer.shutdown();
            } catch (PluginException pe) {
                log.error("Failed to shutdown indexer: {}", pe.getMessage());
                throw new TransformerException("Failed to shutdown indexer", pe);
            }
        }
    }

    /**
     * Transform method
     * 
     * @param object DigitalObject to be transformed
     * @param jsonConfig String containing configuration for this item
     * @return DigitalObject The object after being transformed
     * @throws TransformerException
     */
    @Override
    public DigitalObject transform(DigitalObject in, String jsonConfig) throws TransformerException {
        if (!valid) {
            error("Instantiation did not complete.");
        }
        log.debug("Received OID '{}'", in.getId());
        return process(in, jsonConfig);
    }

    /**
     * Top level wrapping method for a processing an object.
     * 
     * This method first performs all the basic checks whether this Object is
     * technically ready to go to VITAL (no matter what the workflow says).
     * 
     * @param param Map of key/value pairs to add to the index
     */
    private DigitalObject process(DigitalObject in, String jsonConfig) throws TransformerException {
        String oid = in.getId();
        JsonSimple workflow = null;
        try {
            Payload workflowPayload = in.getPayload("workflow.metadata");
            workflow = new JsonSimple(workflowPayload.open());
            workflowPayload.close();
        } catch (StorageException ex) {
            error("Error accessing workflow data from Object!\nOID: '" + oid + "'", ex);
        } catch (IOException ex) {
            error("Error parsing workflow data from Object!\nOID: '" + oid + "'", ex);
        }
        String step = workflow.getString(null, "step");
        if (step == null || !step.equals("live")) {
            log.warn("Object is not live! '{}'", oid);
            return in;
        }
        String title = workflow.getString(null, Strings.NODE_FORMDATA, "title");
        if (title == null) {
            error("No title provided in Object form data!\nOID: '" + oid + "'");
        }
        Properties metadata = null;
        try {
            metadata = in.getMetadata();
        } catch (StorageException ex) {
            error("Error reading Object metadata!\nOID: '" + oid + "'", ex);
        }
        return processObject(in, workflow, metadata);
    }

    /**
     * Middle level wrapping method for processing objects. Now we are looking
     * at what actually needs to be done. Has the object already been put in
     * VITAL, or is it new.
     * 
     * @param object The Object in question
     * @param workflow The workflow data for the object
     * @param metadata The Object's metadata
     */
    private DigitalObject processObject(DigitalObject object, JsonSimple workflow, Properties metadata) throws TransformerException {
        String oid = object.getId();
        String title = workflow.getString(null, Strings.NODE_FORMDATA, "title");
        FedoraClient fedora = null;
        try {
            fedora = fedoraConnect();
        } catch (TransformerException ex) {
            error("Error connecting to VITAL", ex, oid, title);
        }
        String vitalPid = metadata.getProperty(Strings.PROP_VITAL_KEY);
        if (vitalPid != null) {
            log.debug("Existing VITAL object: '{}'", vitalPid);
            if (!datastreamExists(fedora, vitalPid, "DC")) {
                String message = " !!! WARNING !!! The expected VITAL object '" + vitalPid + "' was not found. A new object will be created instead!";
                error(message, null, oid, title);
                vitalPid = null;
            }
        }
        if (vitalPid == null) {
            try {
                vitalPid = createNewObject(fedora, object.getId());
                log.debug("New VITAL object created: '{}'", vitalPid);
                metadata.setProperty(Strings.PROP_VITAL_KEY, vitalPid);
                object.close();
            } catch (Exception ex) {
                error("Failed to create object in VITAL", ex, oid, title);
            }
        }
        if (!waitProperties.isEmpty()) {
            boolean process = false;
            for (String test : waitProperties) {
                String value = metadata.getProperty(test);
                if (value != null) {
                    log.info("Wait condition '{}' found.", test);
                    process = true;
                }
            }
            if (!process) {
                log.info("No wait conditions have been met, processing halted");
                return object;
            }
        }
        try {
            String isActive = metadata.getProperty(Strings.PROP_VITAL_ACTIVE);
            if (isActive == null) {
                log.info("Activating object in fedora: '{}'", oid);
                String cutTitle = title;
                if (cutTitle.length() > 250) {
                    cutTitle = cutTitle.substring(0, 250) + "...";
                }
                fedora.getAPIM().modifyObject(vitalPid, "A", cutTitle, null, "ReDBox activating object: '" + oid + "'");
                metadata.setProperty(Strings.PROP_VITAL_ACTIVE, "true");
                object.close();
            }
        } catch (Exception ex) {
            error("Failed to activate object in VITAL", ex, oid, title);
        }
        try {
            processDatastreams(fedora, object, vitalPid);
        } catch (Exception ex) {
            error("Failed to send object to VITAL", ex, oid, title);
        }
        return object;
    }

    /**
     * Create a new VITAL object and return the PID.
     * 
     * @param fedora An instantiated fedora client
     * @param oid The ID of the ReDBox object we will store here. For logging
     * @return String The new VITAL PID that was just created
     */
    private String createNewObject(FedoraClient fedora, String oid) throws Exception {
        InputStream in = null;
        byte[] template = null;
        try {
            if (foxmlTemplate != null) {
                in = new FileInputStream(foxmlTemplate);
                template = IOUtils.toByteArray(in);
            } else {
                in = getClass().getResourceAsStream("/foxml_template.xml");
                template = IOUtils.toByteArray(in);
            }
        } catch (IOException ex) {
            throw new Exception("Error accessing FOXML Template, please check system configuration!");
        } finally {
            if (in != null) {
                in.close();
            }
        }
        String vitalPid = fedora.getAPIM().ingest(template, Strings.FOXML_VERSION, "ReDBox creating new object: '" + oid + "'");
        log.info("New VITAL PID: '{}'", vitalPid);
        return vitalPid;
    }

    /**
     * Method responsible for arranging submissions to VITAL to store our
     * datastreams.
     * 
     * @param fedora An instantiated fedora client
     * @param object The Object to submit
     * @param vitalPid The VITAL PID to use
     * @throws Exception on any errors
     */
    private void processDatastreams(FedoraClient fedora, DigitalObject object, String vitalPid) throws Exception {
        int sent = 0;
        for (String ourPid : pids.keySet()) {
            String realPid = ourPid;
            if (ourPid.equals(".tfpackage")) {
                realPid = getPackagePid(object);
                if (realPid == null) {
                    String message = partialUploadErrorMessage(ourPid, sent, pids.size(), vitalPid);
                    throw new Exception(message + "\n\nPackage not found.");
                }
            }
            log.info("Processing PID to send to VITAL: '{}'", ourPid);
            JsonSimple thisPid = pids.get(ourPid);
            String dsId = thisPid.getString(realPid, "dsID");
            String label = thisPid.getString(dsId, "label");
            String status = thisPid.getString("A", "status");
            String controlGroup = thisPid.getString("X", "controlGroup");
            boolean versionable = thisPid.getBoolean(true, "versionable");
            boolean retainIds = thisPid.getBoolean(true, "retainIds");
            String[] altIds = {};
            if (retainIds && datastreamExists(fedora, vitalPid, dsId)) {
                altIds = getAltIds(fedora, vitalPid, dsId);
                for (String altId : altIds) {
                    log.debug("Retaining alt ID: '{}' => {}'", dsId, altId);
                }
            }
            Payload payload = null;
            String mimeType = null;
            try {
                payload = object.getPayload(realPid);
            } catch (StorageException ex) {
                String message = partialUploadErrorMessage(realPid, sent, pids.size(), vitalPid);
                throw new Exception(message + "\n\nError accessing payload '" + realPid + "' : ", ex);
            }
            mimeType = payload.getContentType();
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            try {
                sendToVital(fedora, object, realPid, vitalPid, dsId, altIds, label, mimeType, controlGroup, status, versionable);
            } catch (Exception ex) {
                String message = partialUploadErrorMessage(realPid, sent, pids.size(), vitalPid);
                throw new Exception(message, ex);
            }
            sent++;
        }
        try {
            processAttachments(fedora, object, vitalPid);
        } catch (Exception ex) {
            throw new Exception("Error processing attachments: ", ex);
        }
    }

    /**
     * Similar to sendToVital(), but this method is specifically looking for
     * attachments distributed throughout the system.
     * 
     * @param fedora An instantiated fedora client
     * @param object The Object to submit
     * @param vitalPid The VITAL PID to use
     * @throws Exception on any errors
     */
    private void processAttachments(FedoraClient fedora, DigitalObject object, String vitalPid) throws Exception {
        ByteArrayOutputStream out = null;
        ByteArrayInputStream in = null;
        SolrResult result;
        String oid = object.getId();
        SearchRequest req = new SearchRequest("attached_to:\"" + oid + "\"");
        req.setParam("rows", "1000");
        try {
            out = new ByteArrayOutputStream();
            indexer.search(req, out);
            in = new ByteArrayInputStream(out.toByteArray());
            result = new SolrResult(in);
        } catch (Exception ex) {
            throw new Exception("Error searching for attachments : ", ex);
        } finally {
            close(out);
            close(in);
        }
        if (result.getNumFound() == 0) {
            log.info("No attachments found for '{}'", oid);
            return;
        }
        Map<String, Map<String, String>> idMap = new HashMap<String, Map<String, String>>();
        List<String> usedIds = new ArrayList<String>();
        for (SolrDoc item : result.getResults()) {
            String aOid = item.getFirst("id");
            DigitalObject attachment = storage.getObject(aOid);
            Properties metadata = attachment.getMetadata();
            String vitalDsId = metadata.getProperty(Strings.PROP_VITAL_DSID);
            String vitalOrder = metadata.getProperty(Strings.PROP_VITAL_ORDER);
            Map<String, String> map = new HashMap<String, String>();
            if (vitalDsId != null) {
                map.put("hasId", "true");
                map.put(Strings.PROP_VITAL_DSID, vitalDsId);
                map.put(Strings.PROP_VITAL_ORDER, vitalOrder);
                usedIds.add(vitalDsId);
            } else {
                map.put("hasId", "false");
            }
            idMap.put(aOid, map);
        }
        int dsIdSuffix = 1;
        for (SolrDoc item : result.getResults()) {
            String aOid = item.getFirst("id");
            boolean hasId = Boolean.parseBoolean(idMap.get(aOid).get("hasId"));
            if (!hasId) {
                String newId = String.format(attachDs, dsIdSuffix);
                while (usedIds.contains(newId) || datastreamExists(fedora, vitalPid, newId)) {
                    dsIdSuffix++;
                    newId = String.format(attachDs, dsIdSuffix);
                }
                idMap.get(aOid).put(Strings.PROP_VITAL_DSID, newId);
                idMap.get(aOid).put(Strings.PROP_VITAL_ORDER, String.valueOf(dsIdSuffix));
                usedIds.add(newId);
                dsIdSuffix++;
            }
        }
        for (SolrDoc item : result.getResults()) {
            String aOid = item.getFirst("id");
            log.info("Processing Attachment: '{}'", aOid);
            DigitalObject attachment = storage.getObject(aOid);
            Payload wfPayload = attachment.getPayload("workflow.metadata");
            JsonSimple workflow = null;
            try {
                workflow = new JsonSimple(wfPayload.open());
            } catch (Exception ex) {
                throw ex;
            } finally {
                wfPayload.close();
            }
            String pid = workflow.getString(attachment.getSourceId(), Strings.NODE_FORMDATA, "filename");
            log.info(" === Attachment PID: '{}'", pid);
            Payload payload = attachment.getPayload(pid);
            String mimeType = payload.getContentType();
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            String dsId = idMap.get(aOid).get(Strings.PROP_VITAL_DSID);
            String vitalOrder = idMap.get(aOid).get(Strings.PROP_VITAL_ORDER);
            String label = dsId;
            String labelData = workflow.getString(null, Strings.NODE_FORMDATA, attachLabelField);
            if (attachLabels.containsKey(labelData)) {
                label = attachLabels.get(labelData);
            }
            String status = "A";
            String statusData = workflow.getString(null, Strings.NODE_FORMDATA, attachStatusField);
            if (attachStatuses.containsKey(statusData)) {
                status = attachStatuses.get(statusData);
            }
            String[] altIds = {};
            if (attachRetainIds && datastreamExists(fedora, vitalPid, dsId)) {
                altIds = getAltIds(fedora, vitalPid, dsId);
                for (String altId : altIds) {
                    log.debug("Retaining alt ID: '{}' => {}'", dsId, altId);
                }
            }
            altIds = resolveAltIds(altIds, mimeType, Integer.valueOf(vitalOrder));
            try {
                sendToVital(fedora, attachment, pid, vitalPid, dsId, altIds, label, mimeType, attachControlGroup, status, attachVersionable);
            } catch (Exception ex) {
                throw new Exception("Error uploading attachment '" + aOid + "' : ", ex);
            }
            boolean hasId = Boolean.parseBoolean(idMap.get(aOid).get("hasId"));
            if (!hasId) {
                Properties metadata = attachment.getMetadata();
                metadata.setProperty(Strings.PROP_VITAL_DSID, dsId);
                metadata.setProperty(Strings.PROP_VITAL_ORDER, vitalOrder);
                attachment.close();
            }
        }
    }

    /**
     * For the given digital object, find the Fascinator package inside.
     * 
     * @param object The object with a package
     * @return String The payload ID of the package, NULL if not found
     * @throws Exception if any errors occur
     */
    private String getPackagePid(DigitalObject object) throws Exception {
        for (String pid : object.getPayloadIdList()) {
            if (pid.endsWith(".tfpackage")) {
                return pid;
            }
        }
        return null;
    }

    /**
     * For the given mime type, ensure that the array of alternate identifiers
     * is correct. If identifiers are missing they will be added to the array.
     * 
     * @param oldArray The old array of identifiers
     * @param mimeType The mime type of the datastream
     * @param count The attachment count, to use in the format call
     * @return String[] An array containing all of the old IDs with any that
     *      were missing for the mime type
     */
    private String[] resolveAltIds(String[] oldArray, String mimeType, int count) {
        String key = null;
        for (String mimeTest : attachAltIds.keySet()) {
            if (mimeTest.equals(Strings.LITERAL_DEFAULT)) {
                continue;
            }
            if (mimeTest.endsWith("/")) {
                if (mimeType.startsWith(mimeTest)) {
                    key = mimeTest;
                }
            } else {
                if (mimeType.equals(mimeTest)) {
                    key = mimeTest;
                }
            }
        }
        if (key == null) {
            key = Strings.LITERAL_DEFAULT;
        }
        for (String newId : attachAltIds.get(key)) {
            String formatted = String.format(newId, count);
            oldArray = growArray(oldArray, formatted);
        }
        return oldArray;
    }

    /**
     * Check the array for the new element, and if not found, generate a new
     * array containing all of the old elements plus the new.
     * 
     * @param oldArray The old array of data
     * @param newElement The new element we want
     * @return String[] An array containing all of the old data
     */
    private String[] growArray(String[] oldArray, String newElement) {
        for (String element : oldArray) {
            if (element.equals(newElement)) {
                return oldArray;
            }
        }
        log.debug("Adding ID: '{}'", newElement);
        int length = oldArray.length + 1;
        String[] newArray = new String[length];
        System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
        newArray[length - 1] = newElement;
        return newArray;
    }

    /**
     * Take care of the actual transmission to VITAL. This method will select
     * the appropriate transmission method based on:
     * 
     * 1) If VITAL has already seen the datastream before
     * 2) If the data is XML or not
     * 
     * @param fedora The fedora client to use in transmission
     * @param ourObject The DigitalObject in storage
     * @param ourPid The payload in the object to send
     * @param vitalPid The object in fedora we are targeting
     * @param dsId The datastream ID in fedora to create or overwrite
     * @param label The label to use
     * @param mimeType The mime type of the content we are sending
     * @param controlGroup The control group value to use if the object is new
     * @param status The status to use in fedora if the object is new
     * @throws Exception if any errors occur
     */
    private void sendToVital(FedoraClient fedora, DigitalObject ourObject, String ourPid, String vitalPid, String dsId, String[] altIds, String label, String mimeType, String controlGroup, String status, boolean versionable) throws Exception {
        File tempFile = null;
        String tempURI = null;
        try {
            if (datastreamExists(fedora, vitalPid, dsId)) {
                log.info("Updating existing datastream: '{}'", dsId);
                log.debug("LABEL: '" + label + "', STATUS: '" + status + "', GROUP: '" + controlGroup + "'");
                if (mimeType.equals("text/xml")) {
                    byte[] data = getBytes(ourObject, ourPid);
                    fedora.getAPIM().modifyDatastreamByValue(vitalPid, dsId, altIds, label, mimeType, null, data, null, null, fedoraLogEntry(ourObject, ourPid), true);
                } else {
                    try {
                        tempFile = getTempFile(ourObject, ourPid);
                    } catch (Exception ex) {
                        throw new Exception("Error caching file to disk '" + ourObject.getId() + "' : ", ex);
                    }
                    tempURI = fedora.uploadFile(tempFile);
                    fedora.getAPIM().modifyDatastreamByReference(vitalPid, dsId, altIds, label, mimeType, null, tempURI, null, null, fedoraLogEntry(ourObject, ourPid), true);
                }
            } else {
                log.info("Creating new datastream: '{}'", dsId);
                log.debug("LABEL: '" + label + "', STATUS: '" + status + "', GROUP: '" + controlGroup + "'");
                try {
                    tempFile = getTempFile(ourObject, ourPid);
                } catch (Exception ex) {
                    throw new Exception("Error caching file to disk '" + ourObject.getId() + "' : ", ex);
                }
                tempURI = fedora.uploadFile(tempFile);
                fedora.getAPIM().addDatastream(vitalPid, dsId, altIds, label, versionable, mimeType, null, tempURI, controlGroup, status, null, null, fedoraLogEntry(ourObject, ourPid));
            }
        } catch (Exception ex) {
            throw new Exception("Error submitting datastream '" + ourObject.getId() + "' : ", ex);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * Trivial wrapper to close Closeable objects with an awareness that they
     * may not have been instantiated, or may have already been closed.
     * 
     * Typically this would be a Stream, either in or out.
     * 
     * @param toClose The object to close
     */
    private void close(Closeable toClose) {
        if (toClose != null) {
            try {
                toClose.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Test for the existence of a given datastream in VITAL.
     * 
     * @param fedora An instantiated fedora client
     * @param vitalPid The VITAL PID to use
     * @param dsPid The datastream ID on the object
     * @returns boolean True is found, False if not found or there are errors
     */
    private boolean datastreamExists(FedoraClient fedora, String vitalPid, String dsPid) {
        try {
            DatastreamDef[] streams = fedora.getAPIA().listDatastreams(vitalPid, null);
            for (DatastreamDef stream : streams) {
                if (stream.getID().equals(dsPid)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            log.error("API Query error: ", ex);
        }
        return false;
    }

    /**
     * Find and return any alternate identifiers already in use in fedora for
     * the given datastream.
     * 
     * @param fedora An instantiated fedora client
     * @param vitalPid The VITAL PID to use
     * @param dsPid The datastream ID on the object
     * @returns String[] An array or String identifiers, will be empty if
     *      datastream does not exist.
     */
    private String[] getAltIds(FedoraClient fedora, String vitalPid, String dsPid) {
        Datastream ds = getDatastream(fedora, vitalPid, dsPid);
        if (ds != null) {
            return ds.getAltIDs();
        }
        return new String[] {};
    }

    /**
     * Get the indicated datastream from VITAL. This method pre-supposes that
     * the datastream does in fact exist. Call datastreamExists() first to
     * confirm.
     * 
     * @param fedora An instantiated fedora client
     * @param vitalPid The VITAL PID to use
     * @param dsPid The datastream ID on the object
     * @returns Datastream The datastream requested, null if not found
     */
    private Datastream getDatastream(FedoraClient fedora, String vitalPid, String dsPid) {
        try {
            return fedora.getAPIM().getDatastream(vitalPid, dsPid, null);
        } catch (Exception ex) {
            log.error("API Query error: ", ex);
            return null;
        }
    }

    /**
     * Build a Log entry to use in Fedora. Replace all the template placeholders
     * 
     * @param object The Object being submitted
     * @param pid The PID in our system
     */
    private String fedoraLogEntry(DigitalObject object, String pid) {
        String message = fedoraMessageTemplate.replace("[[PID]]", pid);
        return message.replace("[[OID]]", object.getId());
    }

    /**
     * Build an error message detailing an interrupted upload. Some (or none) of
     * the intended list of payloads did not transfer to VITAL correctly.
     * 
     * @param pid The PID in our system for which the failure occurred.
     * @param count The number of successful PIDs sent before the failure.
     * @param total The total number of PIDs that were intended to be sent.
     * @param vitalPid The PID for the entire object in VITAL.
     */
    private String partialUploadErrorMessage(String pid, int count, int total, String vitalPid) {
        String message = "Error submitting payload '" + pid + "' to VITAL. ";
        message += count + " of " + total + " payloads where successfully";
        message += " sent to VITAL before this error occurred.";
        message += " The VITAL PID is '" + vitalPid + "'.";
        return message;
    }

    /**
     * Stream the data out of storage to our temp directory.
     * 
     * @param object Our digital object.
     * @param pid The payload ID to retrieve.
     * @return File The file creating in the temp directory
     * @throws Exception on any errors
     */
    private File getTempFile(DigitalObject object, String pid) throws Exception {
        File directory = new File(tmpDir, object.getId());
        File target = new File(directory, pid);
        if (!target.exists()) {
            target.getParentFile().mkdirs();
            target.createNewFile();
        }
        Payload payload = object.getPayload(pid);
        InputStream in = payload.open();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(target);
            IOUtils.copyLarge(in, out);
        } catch (Exception ex) {
            close(out);
            target.delete();
            payload.close();
            throw ex;
        }
        close(out);
        payload.close();
        return target;
    }

    /**
     * Retrieve the payload from storage and return as a byte array.
     * 
     * @param object Our digital object.
     * @param pid The payload ID to retrieve.
     * @return byte[] The byte array containing payload data
     * @throws Exception on any errors
     */
    private byte[] getBytes(DigitalObject object, String pid) throws Exception {
        Payload payload = object.getPayload(pid);
        InputStream in = payload.open();
        byte[] result = null;
        try {
            result = IOUtils.toByteArray(in);
        } catch (Exception ex) {
            throw ex;
        } finally {
            payload.close();
        }
        return result;
    }

    /**
     * Error handling methods. Will at least log the errors, but also try to
     * send emails if configured to do so, and the data provided indicates it is
     * warranted.
     * 
     * If an OID and Title are provided it indicates an Object we are confident
     * should have been sent to VITAL, so emails will be sent out (if
     * configured).
     * 
     * @param message Our own error message
     * @param ex Any exception that has been thrown (OPTIONAL)
     * @param oid The OID of our Object (OPTIONAL)
     * @param title The title of our Object (OPTIONAL)
     */
    private void error(String message) throws TransformerException {
        error(message, null, null, null);
    }

    private void error(String message, Exception ex) throws TransformerException {
        error(message, ex, null, null);
    }

    private void error(String message, Exception ex, String oid, String title) throws TransformerException {
        if (emailQueue != null) {
            if (oid != null && title != null) {
                JsonSimple messageJson = new JsonSimple();
                JSONArray to = messageJson.writeArray("to");
                for (String email : emailAddresses) {
                    to.add(email);
                }
                JsonObject json = messageJson.getJsonObject();
                json.put("subject", emailSubject);
                json.put("oid", oid);
                String body = emailTemplate.replace("[[OID]]", oid);
                body = body.replace("[[TITLE]]", title);
                body = body.replace("[[MESSAGE]]", message);
                if (ex != null) {
                    StringWriter sw = new StringWriter();
                    ex.printStackTrace(new PrintWriter(sw));
                    body = body.replace("[[ERROR]]", sw.toString());
                } else {
                    body = body.replace("[[ERROR]]", "{No error stacktrace provide}");
                }
                json.put("body", body);
                log.debug("Error, sending email:\n{}", messageJson.toString(true));
                try {
                    messaging.queueMessage(emailQueue, messageJson.toString());
                } catch (MessagingException mex) {
                    log.error("Cannot access message system to send email!!", mex);
                }
            }
        }
        if (ex != null) {
            log.error("Error: {}", message, ex);
            log.error("STACK TRACE:\n", ex);
        } else {
            log.error("Error: {}", message);
        }
        throw new TransformerException(message);
    }

    /****
     * Avoid use of duplicate String literals
     * 
     */
    private static class Strings extends com.googlecode.fascinator.common.Strings {

        public static String CONFIG_FAILURE = "failure";

        public static String CONFIG_SERVER = "server";

        public static String FEDORA_VERSION_TEST = "2.";

        public static String FOXML_VERSION = "foxml1.0";

        public static String DEFAULT_EMAIL_SUBJECT = "VITAL Transformer error";

        public static String DEFAULT_EMAIL_TEMPLATE = "VITAL Transformer error: [[MESSAGE]]\n\n====\n\n[[ERROR]]";

        public static String DEFAULT_VITAL_MESSAGE = "Datastream update from ReDBox '[[OID]]' => '[[PID]]'";

        public static String LITERAL_DEFAULT = "default";

        public static String NODE_FORMDATA = "formData";

        public static String PROP_VITAL_ACTIVE = "vitalActive";

        public static String PROP_VITAL_DSID = "vitalDsId";

        public static String PROP_VITAL_ORDER = "vitalOrder";

        public static String PROP_VITAL_KEY = "vitalPid";
    }
}
