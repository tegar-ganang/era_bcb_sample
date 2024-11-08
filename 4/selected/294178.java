package org.openremote.controller.service;

import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.ProtocolException;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.openremote.controller.Constants;
import org.openremote.controller.ControllerConfiguration;
import org.openremote.controller.OpenRemoteRuntime;
import org.openremote.controller.exception.ControllerDefinitionNotFoundException;
import org.openremote.controller.exception.InitializationException;
import org.openremote.controller.exception.XMLParsingException;
import org.openremote.controller.exception.ConfigurationException;
import org.openremote.controller.exception.ConnectionException;
import org.openremote.controller.model.XMLMapping;
import org.openremote.controller.model.sensor.Sensor;
import org.openremote.controller.model.xml.ObjectBuilder;
import org.openremote.controller.statuscache.StatusCache;
import org.openremote.controller.utils.Logger;
import org.openremote.controller.utils.PathUtil;
import org.springframework.security.providers.encoding.Md5PasswordEncoder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

/**
 * Deployer service centralizes access to the controller's runtime state information. It maintains
 * the controller object model (declared in the XML documents controller deploys), and also
 * acts as a mediator for some other key services in the controller. <p>
 *
 * Mainly the tasks relate to objects and services that maintain some state in-memory of
 * the controller, and where access to such objects or services needs to be shared across
 * multiple threads (rather than created per thread or invocation). Deployer manages the lifecycle
 * of such stateful objects and services to ensure proper state transitions when new controller
 * definitions (from the XML model) are loaded, for instance. <p>
 *
 * Main parts of this implementation relate to managing the XML to Java object mapping --
 * transferring the information from the XML document instance that describe controller
 * behavior into a runtime object model -- and managing the lifecycle of services through
 * restarts and reloading the controller descriptions. In addition, this deployer provides
 * access to the object model instances it generates (such as references to the sensor
 * implementations).
 *
 *
 * @see #softRestart
 * @see #getSensor
 *
 * @author <a href="mailto:juha@openremote.org">Juha Lindfors</a>
 */
public class Deployer {

    /**
   * Indicates a controller schema version which the deployer attempts to map to its object model.
   * <p>
   *
   * These enums act as keys (or part of keys) to locate XML mapping components and object model
   * builders associated with specific XML document schemas.
   */
    public enum ControllerSchemaVersion {

        /**
     * Version 2.0 : this is the schema for controller.xml file
     */
        VERSION_2_0, /**
     * Version 3.0 : this is the schema for openremote.xml file
     */
        VERSION_3_0
    }

    /**
   * TODO :
   *
   *     This is a temporary construct -- with introduction of top-level ModelBuilder (as aggregate
   *     of associated object builders) this should move to appropriate model builder implementation
   *     (see ORCJAVA-182, ORCJAVA-185)
   *
   *
   * Indicates XML segments in the controller schema which the XML mapping implementations
   * can register with.
   */
    public enum XMLSegment {

        /**
     * XML segment identifier used for identifying XML mappers for {@code<sensors>} section
     * in the XML document instance.
     */
        SENSORS("sensors"), /**
     * TODO
     */
        SLIDER("slider"), /**
     * TODO
     */
        CONFIG("config");

        /**
     * Actual element name in the XML document instance that is used to identify the beginning
     * section of where the XML mapping implementation applies.
     */
        private String elementName;

        /**
     * @param elementName   should be the actual element name in the XML document instance which
     *                      identifies the root node of where an XML mapping implementation
     *                      should apply.
     */
        private XMLSegment(String elementName) {
            this.elementName = elementName;
        }

        /**
     * Returns the XML element name that indicates the root XML element of this mapping component.
     *
     * @return  root XML element name of the mapping component.
     */
        public String getName() {
            return elementName;
        }
    }

    /**
   * Common log category for startup logging, with a specific sub-category for this deployer
   * implementation.
   */
    private static final Logger log = Logger.getLogger(Constants.DEPLOYER_LOG_CATEGORY);

    /**
   * Utility method to execute a given XPath expression against the given XML document
   * instance. This implementation is limited to XPath expressions that target XML document
   * elements only.
   *
   * @param doc     XML document instance
   * @param xPath   XPath expression to return a single XML document element
   *
   * @return  One XML document element or <tt>null</tt> if nothing was found
   *
   * @throws  XMLParsingException   if there were errors creating the XPath expression
   *                                or executing it
   */
    private static Element queryElementFromXML(Document doc, String xPath) throws XMLParsingException {
        if (doc == null) {
            throw new XMLParsingException("Cannot execute XPath expression ''{0}'' -- XML document instance was null.", xPath);
        }
        if (xPath == null || xPath.equals("")) {
            throw new XMLParsingException("Null or empty XPath expression for document {0}", doc);
        }
        try {
            XPath xpath = XPath.newInstance(xPath);
            xpath.addNamespace(Constants.OPENREMOTE_NAMESPACE, Constants.OPENREMOTE_WEBSITE);
            List elements = xpath.selectNodes(doc);
            if (!elements.isEmpty()) {
                if (elements.size() > 1) {
                    throw new XMLParsingException("Expression ''{0}'' matches more than one element : {1}", xPath, elements.size());
                }
                Object o = elements.get(0);
                if (o instanceof Element) {
                    return (Element) o;
                } else {
                    throw new XMLParsingException("XPath query is expected to only return Element types, got ''{0}''", o.getClass());
                }
            } else {
                return null;
            }
        } catch (JDOMException e) {
            throw new XMLParsingException("XPath evaluation ''{0}'' failed : {1}", e, xPath, e.getMessage());
        }
    }

    /**
   * Reference to status cache instance that does the actual lifecycle management of sensors
   * (and receives the event updates). This implementation delegates these tasks to it.
   */
    private StatusCache deviceStateCache;

    /**
   * User defined controller configuration variables.
   */
    private ControllerConfiguration controllerConfig;

    /**
   * Acts as a registry of object builders for this deployer. Object builders are responsible
   * for mapping the XML document model into Java object model for specific XML schema versions. <p>
   *
   * The key to this map contains the expected schema version and the XML segment root element
   * which the builder is able to map to Java objects. <p>
   *
   * Map of object builders is maintained through the lifecycle of the JVM -- they are not
   * reset at controller soft restarts.
   *
   * @see org.openremote.controller.model.xml.ObjectBuilder
   * @see org.openremote.controller.model.xml.SensorBuilder
   * @see ModelBuilder
   * @see BuilderKey
   * @see #softRestart
   */
    private Map<BuilderKey, ObjectBuilder> objectBuilders = new HashMap<BuilderKey, ObjectBuilder>(10);

    /**
   * Model builders are sequences of actions to construct the controller's object model (a.k.a
   * strategy pattern). Different model builders may therefore act on differently
   * structured XML document instances. <p>
   *
   * Model builder implementations in general delegate sub-tasks to various object builders
   * that have been registered for the relevant XML schema. <p>
   *
   * This model builder's lifecycle is delimited by the controller's soft restart 
   * lifecycle (see {@link #softRestart()}. Each deploy lifecycle represents one object model
   * (and therefore one model builder instance) that matches a particular XML schema structure.
   *
   * @see ModelBuilder
   * @see #softRestart
   */
    private ModelBuilder modelBuilder = null;

    /**
   * This is a file watch service for the controller definition associated with the current
   * object model builder (i.e. the currently deployed controller XML schema). <p>
   *
   * Depending on the implementation (deletegated to current model builder instance) it may
   * detect changes from the file's timestamp, adding/deleting particular files, etc. and
   * control the deployer lifecycle accordingly, initiating soft restarts, shutdowns, and so on.
   */
    private ControllerDefinitionWatch controllerDefinitionWatch;

    /**
   * This is a generic state flag indicator for this deployer service that its operations are
   * in a 'paused' state -- these may occur during periods where the internal object model is
   * changed, such as during a soft restart. <p>
   *
   * Method implementations in this class may use this flag to check whether to service the
   * incoming call, block it until pause flag is cleared, or immediately return back to the
   * caller.
   */
    private boolean isPaused = false;

    /**
   * Human readable service name for this deployer service. Useful for some logging and
   * diagnostics.
   */
    private String name = "<undefined>";

    /**
   * Creates a new deployer service with a given device state cache implementation and user
   * configuration variables. <p>
   *
   * Creating a deployer instance will not make it 'active' -- no controller object model is
   * loaded (or attempted to be loaded) until a {@link #startController()} method is called.
   * The <tt>startController</tt> therefore acts as an initializer method for the controller
   * runtime.
   *
   * @see #startController()
   *
   * @param serviceName         human-readable name for this deployer service
   * @param deviceStateCache    device cache instance for this deployer
   * @param controllerConfig    user configuration of this deployer's controller
   */
    public Deployer(String serviceName, StatusCache deviceStateCache, ControllerConfiguration controllerConfig) {
        if (deviceStateCache == null || controllerConfig == null) {
            throw new IllegalArgumentException("Null parameters are not allowed.");
        }
        this.deviceStateCache = deviceStateCache;
        this.controllerConfig = controllerConfig;
        if (name != null) {
            this.name = serviceName;
        }
        this.controllerDefinitionWatch = new ControllerDefinitionWatch(this);
        log.debug("Deployer ''{0}'' initialized.", name);
    }

    /**
   * This method initializes the controller's runtime model, making it 'active'. The method should
   * be called once during the lifecycle of the controller JVM -- subsequent re-deployments of
   * controller's runtime should go via {@link #softRestart()} method. <p>
   *
   * If a controller definition is present, it is loaded and the object model created accordingly.
   * If no definition is found, the controller is left in an init state where adding the
   * required artifacts to the controller will trigger the deployment of controller definition.
   *
   * @see #softRestart()
   */
    public void startController() {
        try {
            startup();
        } catch (ControllerDefinitionNotFoundException e) {
            log.info("\n\n" + "********************************************************************************\n" + "\n" + " Controller definition was not found in this OpenRemote Controller instance.      \n" + "\n" + " If you are starting the controller for the first time, please use your web     \n" + " browser to connect to the controller home page and synchronize it with your    \n" + " online account. \n" + "\n" + "********************************************************************************\n\n" + "\n" + e.getMessage());
        } catch (Throwable t) {
            log.error("!!! CONTROLLER STARTUP FAILED : {0} !!!", t, t.getMessage());
        }
        controllerDefinitionWatch.start();
    }

    /**
   * Indicates the current state of the deployer. Deployer may be 'paused' during certain
   * lifecycle stages, such as reloading the controller's internal object model. During those
   * phases, other deployer operations may opt to block calling threads until deployer has
   * resumed, or return calls immediately without servicing them.
   * 
   * @return    true to indicate deployer is currently paused, false otherwise
   */
    public boolean isPaused() {
        return isPaused;
    }

    /**
   * TODO :
   *    This is subject to further refactoring -- the aggregating object should be the model
   *    builder, object builder should register directly with it. Model builder can delegate
   *    to deployer if necessary. Both object builders and model builders should be configurable
   *    via DI framework. See ORCJAVA-182, ORCJAVA-185
   *
   * 
   * Allows object builders to be registered with this deployer. Object builders can be
   * registered for multiple different XML schema versions and are used by model builders
   * to construct an object model. <p>
   *
   * A deployer is associated with one active model builder at a time (per controller model
   * deployment). Model builders may delegate their tasks to registered object builders as
   * necessary.
   *
   * @see ModelBuilder
   * @see org.openremote.controller.service.Deployer.ControllerSchemaVersion
   * @see org.openremote.controller.model.xml.ObjectBuilder
   * @see org.openremote.controller.model.xml.SensorBuilder
   *
   * @param builder   object builder instance to register
   */
    public void registerObjectBuilder(ObjectBuilder builder) {
        if (builder == null) {
            log.error("Attempted to register a <null> object builder. Registration ignored.");
            return;
        }
        try {
            ControllerSchemaVersion version = builder.getSchemaVersion();
            XMLSegment segment = builder.getRootSegment();
            BuilderKey key = new BuilderKey(version, segment);
            ObjectBuilder previous = objectBuilders.put(key, builder);
            if (previous != null) {
                log.warn("Double registration of an object builder ({0} was replaced with {1}). " + "This may indicate an error if unintended.", previous.toString(), builder.toString());
            }
        } catch (Throwable t) {
            log.error("Error registering object builder : {0}", t, t.getMessage());
        }
    }

    /**
   * Initiate a shutdown/startup sequence.  <p>
   *
   * Shutdown phase will undeploy the current runtime object model and manage service lifecycles
   * that are dependent on the object model. Resources will be stopped and freed. <p>
   *
   * The soft restart only impacts the runtime object model and associated component lifecycles
   * in the controller. The controller itself stays at an init level where a new object model can
   * be loaded into the system. The JVM process will not exit. <p>
   *
   * Startup phase loads back a runtime object model and start dependent services of the controller.
   * The object model is loaded from the controller definition file, path of which is indicated by
   * the {@link org.openremote.controller.ControllerConfiguration#getResourcePath()} method. <p>
   *
   * After the startup phase is done, a complete functional controller definition has been
   * loaded into the controller (unless fatal errors occured), it has been initialized and
   * started and it is ready to handle incoming requests. <p>
   *
   * <b>NOTE : </b> This method call should only be used after {@link #startController()}
   * has been invoked once. The <tt>startController</tt> method will initialize this deployer
   * instance's lifecycle, and perform first deployment of controller definition, if the
   * required artifacts are present in the controller. </p>
   *
   * Subsequent soft restarts of this controller/deployer should use this method instead.
   *
   * @see #startController()
   * @see org.openremote.controller.ControllerConfiguration#getResourcePath
   * @see #softShutdown
   * @see #startup()
   *
   * @throws ControllerDefinitionNotFoundException
   *            If there are no controller definitions to load from. This exception indicates that
   *            the {@link #startup} phase of the restart cannot complete. The controller/deployer
   *            is left in an init state where the previous controller object model has been
   *            undeployed, and a new one will be deployed once the required artifacts have been
   *            added to the controller.
   */
    public void softRestart() throws ControllerDefinitionNotFoundException {
        try {
            pause();
            softShutdown();
            startup();
        } finally {
            resume();
        }
    }

    /**
   * Deploys a controller configuration from a given ZIP archive. This can be used when the
   * controller configuration is already present on the local system. <p>
   *
   * The contents of the ZIP archive will be extracted on the file system location pointed
   * by the 'resource.path' property. <p>
   *
   * If the {@link ControllerConfiguration#RESOURCE_UPLOAD_ALLOWED} has been configured to
   * 'false', this method will throw an exception.
   *
   * @see org.openremote.controller.ControllerConfiguration#getResourcePath()
   * @see org.openremote.controller.ControllerConfiguration#isResourceUploadAllowed()
   *
   * @see #deployFromOnline(String, String)
   *
   * @param inputStream     Input stream to the zip file to deploy. Note that this method will
   *                        attempt to close the input stream on exit.
   *
   * @throws ConfigurationException   If new deployments through admin interface have been disabled,
   *                                  or if the target path to extract the new deployment archive
   *                                  cannot be resolved, or if the target path does not exist
   *
   * @throws IOException              If there was an unrecovable I/O error when extracting the
   *                                  deployment archive. Note that errors in extracting individual
   *                                  files from within the deployment archive may be logged as
   *                                  errors or warnings instead of raising an exception.
   */
    public void deployFromZip(InputStream inputStream) throws ConfigurationException, IOException {
        if (!controllerConfig.isResourceUploadAllowed()) {
            throw new ConfigurationException("Updating controller through web interface has been disabled. " + "You must update controller files manually instead.");
        }
        String resourcePath = controllerConfig.getResourcePath();
        if (resourcePath == null || resourcePath.equals("")) {
            throw new ConfigurationException("Configuration option 'resource.path' was not found or contains empty value.");
        }
        URI resourceDirURI = new File(controllerConfig.getResourcePath()).toURI();
        unzip(inputStream, resourceDirURI);
        copyLircdConf(resourceDirURI, controllerConfig);
    }

    /**
   * Deploys a controller configuration directly from user's account stored on the backend.
   * A HTTP connection is created to Beehive server and account information is downloaded to
   * this controller using the given user name and credentials.
   *
   * @see #deployFromZip(java.io.InputStream)
   *
   * @param username        user name to download account configuration through Beehive's REST
   *                        interface
   *
   * @param credentials     credentials to authenticate to use Beehive's REST interface
   *
   * @throws ConfigurationException   If the connection to backend cannot be created due to
   *                                  configuration errors, or deploying new configuration has
   *                                  been disabled, or there were other configuration errors
   *                                  which prevented the deployment archive from being extracted.
   *
   * @throws ConnectionException      If connection creation failed, or reading from the connection
   *                                  failed for any reason
   */
    public void deployFromOnline(String username, String credentials) throws ConfigurationException, ConnectionException {
        BeehiveConnection connection = new BeehiveConnection(this);
        InputStream stream = connection.downloadZip(username, credentials);
        try {
            deployFromZip(stream);
        } catch (IOException e) {
            throw new ConnectionException("Extracting controller configuration from Beehive account failed : {0}", e, e.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.warn("Could not close I/O stream to downloaded user configuration : {0}", e, e.getMessage());
                }
            }
        }
    }

    /**
   * TODO :
   *
   *   This is temporarily here, part of the refactoring of deprecating ComponentBuilder and
   *   migrating to a proper ObjectBuilder implementation. The existing component builders
   *   externalize their XML parsing which is currently serviced here. Over the long term,
   *   the object builders should be dependents of model builder which provides XML parsing
   *   services like this method.
   *
   *   See ORCJAVA-143, ORCJAVA-144, ORCJAVA-151, ORCJAVA-152, ORCJAVA-153, ORCJAVA-155,
   *       ORCJAVA-158, ORCJAVA-182, ORCJAVA-186, ORCJAVA-190
   *
   * @param id
   * @return
   * @throws InitializationException
   */
    public Element queryElementById(int id) throws InitializationException {
        if (modelBuilder == null) {
            throw new IllegalStateException("Runtime object model has not been initialized.");
        }
        Element element = queryElementFromXML(modelBuilder.getControllerDocument(), "//" + Constants.OPENREMOTE_NAMESPACE + ":*[@id='" + id + "']");
        if (element == null) {
            throw new XMLParsingException("No component found with id ''{0}''.", id);
        }
        return element;
    }

    /**
   * TODO :
   *
   *   This is temporarily here, part of refactoring to internalize Java-to-XML mapping to their
   *   corresponding services. This is only used by configuration API at the moment. Configuration
   *   is part of the deployment lifecycle, so configuration should be made a depedendant of
   *   Deployer service and in the process make the requirement of having this low level XML
   *   parsing API internalized.
   *
   *   See ORCJAVA-181, ORCJVA-182, ORCJAVA-183, ORCJAVA-186
   *
   *
   * @param xmls
   * @return
   * @throws InitializationException
   */
    public Element queryElementByName(XMLSegment xmls) throws InitializationException {
        if (modelBuilder == null) {
            throw new IllegalStateException("Runtime object model has not been initialized.");
        }
        Element element = queryElementFromXML(modelBuilder.getControllerDocument(), "//" + Constants.OPENREMOTE_NAMESPACE + ":" + xmls.getName());
        if (element == null) {
            throw new XMLParsingException("No XML elements found with name ''{0}''.", xmls.getName());
        }
        return element;
    }

    /**
   * TODO
   *
   *   This is temporarily here, part of refactoring to internalize Java-to-XML mapping to their
   *   corresponding services. This is used by deprecated ComponentBuilder implementations
   *   (see ORCJAVA-143) which rely on sensors for state input.
   *
   *   See ORCJAVA-143, ORCJAVA-144, ORCJAVA-147, ORCJAVA-151, ORCJVA-152, ORCJAVA-153
   *
   *
   * @param componentIncludeElement   JDOM element for sensor
   *
   * @throws InitializationException    if the sensor model cannot be built from the given XML
   *                                    element
   *
   * @return sensor
   */
    public Sensor getSensorFromComponentInclude(Element componentIncludeElement) throws InitializationException {
        if (componentIncludeElement == null) {
            throw new InitializationException("Implementation error, null reference on expected " + "<include type = \"sensor\" ref = \"nnn\"/> element.");
        }
        Attribute includeTypeAttr = componentIncludeElement.getAttribute(XMLMapping.XML_INCLUDE_ELEMENT_TYPE_ATTR);
        String typeAttributeValue = includeTypeAttr.getValue();
        if (!typeAttributeValue.equals(XMLMapping.XML_INCLUDE_ELEMENT_TYPE_SENSOR)) {
            throw new XMLParsingException("Expected to include 'sensor' type, got {0} instead.", typeAttributeValue);
        }
        Attribute includeRefAttr = componentIncludeElement.getAttribute(XMLMapping.XML_INCLUDE_ELEMENT_REF_ATTR);
        String refAttributeValue = includeRefAttr.getValue();
        try {
            int sensorID = Integer.parseInt(refAttributeValue);
            return getSensor(sensorID);
        } catch (NumberFormatException e) {
            throw new InitializationException("Currently only integer values are accepted as unique sensor ids. " + "Could not parse {0} to integer.", refAttributeValue);
        }
    }

    /**
   * Returns a registered sensor instance. Sensor instances are shared across threads.
   * Retrieving a sensor with the same ID will yield a same instance. <p>
   *
   * If the sensor with given ID is not found, a <tt>null</tt> is returned.
   *
   * @see org.openremote.controller.model.sensor.Sensor#getSensorID()
   *
   * @param id    sensor ID
   *
   * @return      sensor instance, or null if sensor with given ID was not found
   */
    protected Sensor getSensor(int id) {
        Sensor sensor = deviceStateCache.getSensor(id);
        if (sensor == null) {
            log.error("Attempted to access sensor with id ''{0}'' which did not exist in device " + "state cache.", id);
        }
        return sensor;
    }

    /**
   * Implements the sequence of shutting down the currently deployed controller
   * runtime (but will not exit the VM process). <p>
   *
   * After this method completes, the controller has no active runtime object model but is
   * at an init level where a new one can be loaded in.
   */
    private void softShutdown() {
        log.info("\n\n" + "--------------------------------------------------------------------\n\n" + "  UNDEPLOYING CURRENT CONTROLLER RUNTIME...\n\n" + "--------------------------------------------------------------------\n");
        deviceStateCache.shutdown();
        modelBuilder = null;
        log.info("Shutdown complete.");
    }

    /**
   * Manages the build-up of controller runtime with object model creation
   * from the XML document instance(s). Attempts to detect from configuration files
   * which version of object model and corresponding XML schema should be used to
   * build the runtime model. <p>
   *
   * Once this method returns, the controller runtime is 'ready' -- that is, the object
   * model has been created and also initialized, registered and started so it is fully
   * functional and able to receive requests.   <p>
   *
   * Note that partial failures (such as errors in the controller's object model definition) may
   * not prevent the startup from completing. Such errors may be logged instead, leaving the
   * controller with an object model that is only partial from the intended one.
   *
   * @throws ControllerDefinitionNotFoundException
   *              If the startup could not be completed because no controller definition
   *              was found.
   */
    private void startup() throws ControllerDefinitionNotFoundException {
        ControllerSchemaVersion version = detectVersion();
        log.info("\n\n" + "--------------------------------------------------------------------\n\n" + "  Deploying NEW CONTROLLER RUNTIME...\n\n" + "--------------------------------------------------------------------\n");
        switch(version) {
            case VERSION_3_0:
                modelBuilder = new Version30ModelBuilder();
                break;
            case VERSION_2_0:
                modelBuilder = new Version20ModelBuilder(this);
                break;
            default:
                throw new Error("Unrecognized schema version " + version);
        }
        modelBuilder.buildModel();
        log.info("Startup complete.");
    }

    /**
   * Sets this deployer in 'paused' state.
   *
   * @see #resume
   */
    private void pause() {
        isPaused = true;
        controllerDefinitionWatch.pause();
    }

    /**
   * Resumes this deployer from a previously 'paused' state.
   *
   * @see #pause
   */
    private void resume() {
        try {
            controllerDefinitionWatch.resume();
        } finally {
            isPaused = false;
        }
    }

    /**
   * A simplistic attempt at detecting which schema version we should use to build
   * the object model.
   *
   * TODO -- MODELER-256, ORCJAVA-189 : xml schema should include explicit version info
   *
   * @return  the detected schema version
   *
   * @throws  ControllerDefinitionNotFoundException
   *              if we can't find any controller definition files to load
   */
    private ControllerSchemaVersion detectVersion() throws ControllerDefinitionNotFoundException {
        if (Version30ModelBuilder.checkControllerDefinitionExists(controllerConfig)) {
            return ControllerSchemaVersion.VERSION_3_0;
        }
        if (Version20ModelBuilder.checkControllerDefinitionExists(controllerConfig)) {
            return ControllerSchemaVersion.VERSION_2_0;
        }
        throw new ControllerDefinitionNotFoundException("Could not find a controller definition to load at path ''{0}'' (for version 2.0)", Version20ModelBuilder.getControllerDefinitionFile(controllerConfig));
    }

    /**
   * Extracts an OpenRemote deployment archive into a given target file directory.
   *
   * @param inputStream     Input stream for reading the ZIP archive. Note that this method will
   *                        attempt to close the stream on exiting.
   *
   * @param targetDir       URI that points to the root directory where the extracted files should
   *                        be placed. Note that the URI must be an absolute file URI.
   *
   * @throws ConfigurationException   If target file URI cannot be resolved, or if the target
   *                                  file path does not exist
   *
   * @throws IOException              If there was an unrecovable I/O error reading or extracting
   *                                  the ZIP archive. Note that errors on individual files within
   *                                  the archive may not generate exceptions but be logged as
   *                                  errors or warnings instead.
   */
    private void unzip(InputStream inputStream, URI targetDir) throws ConfigurationException, IOException {
        if (targetDir == null || targetDir.getPath().equals("") || !targetDir.isAbsolute()) {
            throw new ConfigurationException("Target dir must be absolute file: protocol URI, got '" + targetDir + +'.');
        }
        File checkedTargetDir = new File(targetDir);
        if (!checkedTargetDir.exists()) {
            throw new ConfigurationException("The path ''{0}'' doesn't exist.", targetDir);
        }
        ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
        ZipEntry zipEntry = null;
        BufferedOutputStream fileOutputStream = null;
        try {
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                try {
                    URI extractFileURI = targetDir.resolve(new URI(null, null, zipEntry.getName(), null));
                    log.debug("Resolved URI to ''{0}''", extractFileURI);
                    File zippedFile = new File(extractFileURI);
                    log.debug("Attempting to extract ''{0}'' to ''{1}''.", zipEntry, zippedFile);
                    try {
                        fileOutputStream = new BufferedOutputStream(new FileOutputStream(zippedFile));
                        int b;
                        while ((b = zipInputStream.read()) != -1) {
                            fileOutputStream.write(b);
                        }
                    } catch (FileNotFoundException e) {
                        log.error("Could not extract ''{0}'' -- file ''{1}'' could not be created : {2}", e, zipEntry.getName(), zippedFile, e.getMessage());
                    } catch (IOException e) {
                        log.warn("Zip extraction of ''{0}'' to ''{1}'' failed : {2}", e, zipEntry, zippedFile, e.getMessage());
                    } finally {
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                                log.debug("Extraction of ''{0}'' to ''{1}'' completed.", zipEntry, zippedFile);
                            } catch (Throwable t) {
                                log.warn("Failed to close file ''{0}'' : {1}", t, zippedFile, t.getMessage());
                            }
                        }
                        if (zipInputStream != null) {
                            if (zipEntry != null) {
                                try {
                                    zipInputStream.closeEntry();
                                } catch (IOException e) {
                                    log.warn("Failed to close ZIP file entry ''{0}'' : {1}", e, zipEntry, e.getMessage());
                                }
                            }
                        }
                    }
                } catch (URISyntaxException e) {
                    log.warn("Cannot extract {0} from zip : {1}", e, zipEntry, e.getMessage());
                }
            }
        } finally {
            try {
                if (zipInputStream != null) {
                    zipInputStream.close();
                }
            } catch (IOException e) {
                log.warn("Failed to close zip file : {0}", e, e.getMessage());
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    log.warn("Failed to close file : {0}", e, e.getMessage());
                }
            }
        }
    }

    /**
   * TODO
   *
   * @param resourcePath
   * @param config
   */
    private void copyLircdConf(URI resourcePath, ControllerConfiguration config) {
        File lircdConfFile = new File(resourcePath.resolve(Constants.LIRCD_CONF).getPath());
        File lircdconfDir = new File(config.getLircdconfPath().replaceAll(Constants.LIRCD_CONF, ""));
        try {
            if (lircdconfDir.exists() && lircdConfFile.exists()) {
                if (config.isCopyLircdconf()) {
                    FileUtils.copyFileToDirectory(lircdConfFile, lircdconfDir);
                    log.info("copy lircd.conf to" + config.getLircdconfPath());
                }
            }
        } catch (IOException e) {
            log.error("Can't copy lircd.conf to " + config.getLircdconfPath(), e);
        }
    }

    /**
   * This service performs the automated file watching of the controller definition artifacts
   * (depending on the model builder implementation). <p>
   *
   * Per the rules defined in this implementation and in combination with those provided by
   * model builders via their
   * {@link org.openremote.controller.service.Deployer.ModelBuilder#hasControllerDefinitionChanged()}
   * method implementations, this service controls the deployer lifecycle through
   * {@link org.openremote.controller.service.Deployer#softRestart()} and
   * {@link Deployer#softShutdown()} methods.
   *
   * @see org.openremote.controller.service.Deployer#softRestart()
   * @see org.openremote.controller.service.Deployer#softShutdown()
   */
    private static class ControllerDefinitionWatch implements Runnable {

        /**
     * Deployer reference for this service to control the deployer lifecycle.
     */
        private Deployer deployer;

        /**
     * Indicates the watcher thread is running.
     */
        private volatile boolean running = true;

        /**
     * Indicates the watcher thread should temporarily pause and not trigger any actions
     * on the deployer.
     */
        private volatile boolean paused = false;

        /**
     * The actual thread reference.
     */
        private Thread watcherThread;

        /**
     * Creates a new controller file watcher for a given deployer. Use {@link #start()} to
     * make this service active (start the relevant thread(s)).
     *
     * @param deployer  reference to the deployer whose lifecycle this watcher service controls
     */
        private ControllerDefinitionWatch(Deployer deployer) {
            this.deployer = deployer;
        }

        /**
     * Starts the controller definition watcher thread.
     */
        public void start() {
            watcherThread = OpenRemoteRuntime.createThread("Controller Definition File Watcher for " + deployer.name, this);
            watcherThread.start();
            log.info("{0} started.", watcherThread.getName());
        }

        /**
     * Stops (and kills) the controller definition watcher thread.
     */
        public void stop() {
            running = false;
            watcherThread.interrupt();
        }

        /**
     * Temporarily pauses the controller definition watcher thread, preventing any state
     * modifications to the associated deployment service.
     *
     * @see #resume
     */
        public void pause() {
            paused = true;
        }

        /**
     * Resumes the controller definition watcher thread after it has been {@link #pause() paused}.
     *
     * @see #pause
     */
        public void resume() {
            paused = false;
        }

        /**
     * Runs the watcher thread using the following logic:  <p>
     *
     * - If paused, do nothing  <br>
     *
     * - If cannot detect controller definition files for any known schemas, keep waiting <br>
     *
     * - If detects a controller definition has been added but not deployed, run
     *   deployer.softRestart()  <br>
     *
     * - If has an existing controller object model deployed but the model builder reports
     *   a change in it (what constitutes a change depends on deployed model builder implementation),
     *   then run deployer.softRestart() <br>
     *
     * - If an existing controller model was deployed but the controller definition is removed
     *   (as reported by {@link org.openremote.controller.service.Deployer#detectVersion()})
     *   then undeploy the object model.
     */
        @Override
        public void run() {
            while (running) {
                if (paused) continue;
                try {
                    deployer.detectVersion();
                    if (deployer.modelBuilder == null || deployer.modelBuilder.hasControllerDefinitionChanged()) {
                        try {
                            deployer.softRestart();
                        } catch (ControllerDefinitionNotFoundException e) {
                            log.error("Soft restart cannot complete, controller definition not found : {0}", e.getMessage());
                        } catch (Throwable t) {
                            log.error("Controller soft restart failed : {0}", t, t.getMessage());
                        }
                    }
                } catch (ControllerDefinitionNotFoundException e) {
                    if (deployer.modelBuilder != null) {
                        deployer.softShutdown();
                    } else {
                        log.trace("Did not locate controller definitions for any known schema...");
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    running = false;
                    Thread.currentThread().interrupt();
                }
            }
            log.info("{0} has been stopped.", watcherThread.getName());
        }
    }

    /**
   * Controller's object model builder for the current 2.0 version of the implementation.
   */
    private static class Version20ModelBuilder implements ModelBuilder {

        /**
     * Utility method to return a Java I/O File instance representing the artifact with
     * controller runtime object model (version 2.0) definition.
     *
     * @param   config    controller's user configuration
     *
     * @return  file representing an object model definition for a controller
     */
        private static File getControllerDefinitionFile(ControllerConfiguration config) {
            try {
                URI uri = new URI(config.getResourcePath());
                return new File(uri.resolve(Constants.CONTROLLER_XML));
            } catch (Throwable t) {
                String xmlPath = PathUtil.addSlashSuffix(config.getResourcePath()) + Constants.CONTROLLER_XML;
                return new File(xmlPath);
            }
        }

        /**
     * Utility method to isolate the privileged code block for file read access check (exists)
     *
     * @param   config    controller's user configuration
     *
     * @return  true if file exists; false if file does not exists or was denied by
     *          security manager
     */
        private static boolean checkControllerDefinitionExists(ControllerConfiguration config) {
            final File file = getControllerDefinitionFile(config);
            try {
                return AccessController.doPrivilegedWithCombiner(new PrivilegedAction<Boolean>() {

                    @Override
                    public Boolean run() {
                        return file.exists();
                    }
                });
            } catch (SecurityException e) {
                log.error("Security manager prevented read access to file ''{0}'' : {1}", e, file.getAbsoluteFile(), e.getMessage());
                return false;
            }
        }

        /**
     * Contains the schema version for this implementation. This is used by some generic
     * implementations in this class which may be overriden by subclasses that implement
     * minor (but incompatible) changes to schema as it evolves. Therefore in some cases
     * it may be possible to extend and override this implementation for a next minor schema
     * version rather than implement the entire model builder from scratch.
     */
        protected ControllerSchemaVersion version;

        /**
     * Reference to the deployer service this model builder instance is associated with.
     */
        private Deployer deployer;

        /**
     * Indicates whether the controller.xml for this schema implementation has been found.
     *
     * @see #hasControllerDefinitionChanged()
     */
        private boolean controllerDefinitionIsPresent;

        /**
     * Last known timestamp of controller.xml file.
     */
        private long lastTimeStamp = 0L;

        /**
     * Initialize this model builder instance to schema version 2.0
     * ({@link ControllerSchemaVersion#VERSION_2_0}).
     *
     * @param deployer  reference to the deployer this model builder is associated with
     */
        protected Version20ModelBuilder(Deployer deployer) {
            this.deployer = deployer;
            this.version = ControllerSchemaVersion.VERSION_2_0;
            controllerDefinitionIsPresent = checkControllerDefinitionExists(deployer.controllerConfig);
            if (controllerDefinitionIsPresent) {
                lastTimeStamp = getControllerXMLTimeStamp();
            }
        }

        /**
     * TODO :
     * 
     *   Get a document for a user referenced controller.xml with xsd validation.
     *
     *   This implementation is here temporarily to support existing client interface access
     *   to deployer service -- it may however be removed later since it should not be necessary
     *   to expose the XML document instance to outside services beyond what is provided by the
     *   Deployer API.
     *
     *   See related issue references in Deployer.queryElementByName() and in
     *   Deployer.queryElementByID() methods.
     * 
     *
     * @return a built document for controller.xml.
     */
        @Override
        public Document getControllerDocument() throws InitializationException {
            SAXBuilder builder = new SAXBuilder();
            String xsdPath = Constants.CONTROLLER_XSD_PATH;
            File controllerXMLFile = getControllerDefinitionFile(deployer.controllerConfig);
            if (!checkControllerDefinitionExists(deployer.controllerConfig)) {
                throw new ControllerDefinitionNotFoundException("Controller.xml not found -- make sure it's in " + controllerXMLFile.getAbsoluteFile());
            }
            try {
                URL xsdResource = Version20ModelBuilder.class.getResource(xsdPath);
                if (xsdResource == null) {
                    log.error("Cannot find XSD schema ''{0}''. Disabling validation...", xsdPath);
                } else {
                    xsdPath = xsdResource.getPath();
                    builder.setProperty(Constants.SCHEMA_LANGUAGE, Constants.XML_SCHEMA);
                    builder.setProperty(Constants.SCHEMA_SOURCE, new File(xsdPath));
                    builder.setValidation(true);
                }
                return builder.build(controllerXMLFile);
            } catch (Throwable t) {
                throw new XMLParsingException("Unable to parse controller definition from " + "''{0}'' (accessing schema from ''{1}'') : {2}", t, controllerXMLFile.getAbsoluteFile(), xsdPath, t.getMessage());
            }
        }

        /**
     * TODO:
     * 
     *    - Sequence of actions to build object model based on the current 2.0 schema.
     *      Right now just has sensors.
     */
        @Override
        public void buildModel() {
            buildSensorModel();
        }

        /**
     * Attempts to determine whether the controller.xml 'last modified' timestamp has changed,
     * or if the file has been removed altogether, or if the file was not present earlier but
     * has been added since last check. <p>
     *
     * All the above cases yield an indication that the controller's model definition has changed
     * which can in turn result in reloading the model by the deployer (see
     * {@link org.openremote.controller.service.Deployer.ControllerDefinitionWatch} for more
     * details).
     * 
     * @return  true if controller.xml has been changed, removed or added since last check,
     *          false otherwise
     */
        @Override
        public boolean hasControllerDefinitionChanged() {
            if (controllerDefinitionIsPresent) {
                if (!checkControllerDefinitionExists(deployer.controllerConfig)) {
                    controllerDefinitionIsPresent = false;
                    return true;
                }
                long lastModified = getControllerXMLTimeStamp();
                if (lastModified > lastTimeStamp) {
                    lastTimeStamp = lastModified;
                    return true;
                }
            } else {
                if (checkControllerDefinitionExists(deployer.controllerConfig)) {
                    controllerDefinitionIsPresent = true;
                    return true;
                }
            }
            return false;
        }

        /**
     * Build concrete sensor Java instances from the XML declaration. <p>
     *
     * NOTE: this implementation will register and start the sensors at build time automatically.
     */
        protected void buildSensorModel() {
            Set<Sensor> sensors = buildSensorObjectModelFromXML();
            for (Sensor sensor : sensors) {
                deployer.deviceStateCache.registerSensor(sensor);
                sensor.start();
            }
        }

        /**
     * Parse sensor definitions from controller.xml and create the corresponding Java objects. <p>
     *
     * This method is somewhat generic in that it delegates to object builder instances that
     * register with {@link XMLSegment#SENSORS} identifier. Therefore if the top-level
     * {@code <sensors>} element is present in other schema definitions, this implementation
     * may be reused by registering an alternative object builder with the same <tt>SENSORS</tt>
     * identifier. The schema version part of the key in {@link Deployer#objectBuilders} is
     * determined by subclassing and overriding this implementations {@link #version} field.
     *
     * TODO :
     *   See ORCJAVA-182 and the impact this might have by removal of XMLSegment and having
     *   dependant object builders register with model builder directly.
     *
     * 
     * @see org.openremote.controller.model.xml.ObjectBuilder
     * @see #version
     * @see Deployer#registerObjectBuilder
     *
     * @return  list of sensor instances that were succesfully built from the controller.xml
     *          declaration
     */
        protected Set<Sensor> buildSensorObjectModelFromXML() {
            try {
                Document doc = getControllerDocument();
                String xPathExpression = "//" + Constants.OPENREMOTE_NAMESPACE + ":sensors";
                Element sensorsElement = queryElementFromXML(doc, xPathExpression);
                if (sensorsElement == null) {
                    log.info("No sensors found.");
                    return new HashSet<Sensor>(0);
                }
                Iterator<Element> sensorElementIterator = getSensorElements(sensorsElement).iterator();
                Set<Sensor> sensorModels = new HashSet<Sensor>();
                while (sensorElementIterator.hasNext()) {
                    try {
                        Element sensorElement = sensorElementIterator.next();
                        BuilderKey key = new BuilderKey(version, XMLSegment.SENSORS);
                        ObjectBuilder ob = deployer.objectBuilders.get(key);
                        if (ob == null) {
                            throw new XMLParsingException("No object builder found for <" + XMLSegment.SENSORS.getName() + "> XML segment " + "in schema " + version);
                        }
                        Sensor sensor = (Sensor) ob.build(sensorElement);
                        log.debug("Created object model for sensor ''{0}'' (ID = ''{1}'').", sensor.getName(), sensor.getSensorID());
                        sensorModels.add(sensor);
                    } catch (Throwable t) {
                        log.error("Creating sensor failed : " + t.getMessage(), t);
                    }
                }
                return sensorModels;
            } catch (Throwable t) {
                log.error("No sensors found - {0}", t, t.getMessage());
                return new HashSet<Sensor>(0);
            }
        }

        /**
     * Returns the timestamp of controller.xml file of this controller object model.
     *
     * @return  last modified timestamp, or zero if the timestamp cannot be accessed
     */
        private long getControllerXMLTimeStamp() {
            final File controllerXML = getControllerDefinitionFile(deployer.controllerConfig);
            try {
                return AccessController.doPrivilegedWithCombiner(new PrivilegedAction<Long>() {

                    @Override
                    public Long run() {
                        return controllerXML.lastModified();
                    }
                });
            } catch (SecurityException e) {
                log.error("Security manager prevented access to timestamp of file ''{0}'' ({1}). " + "Automatic detection of controller.xml file modifications are disabled.", e, controllerXML, e.getMessage());
                return 0L;
            }
        }

        /**
     * Isolated this one method call to suppress warnings (JDOM API does not use generics).
     *
     * @param sensorsElement  the {@code <sensors>} element of controller.xml file
     *
     * @return  the child elements of {@code <sensors>}
     */
        @SuppressWarnings("unchecked")
        private List<Element> getSensorElements(Element sensorsElement) {
            return sensorsElement.getChildren();
        }
    }

    /**
   * Model builders are sequences of actions which construct the controller's object model.
   * Therefore it implements a strategy pattern. Different model builders may act on differently
   * structured XML document instances. <p>
   *
   * The implementation of a model builder is expected not only to create the Java object instances
   * representing the object model, but also initialize, register and start all the created
   * resources as necessary. On returning from the {@link #buildModel()} method, the controller's
   * object model is expected to be running and fully functional.
   */
    private static interface ModelBuilder {

        /**
     * Responsible for constructing the controller's object model. Implementation details
     * vary depending on the schema and source of defining artifacts.
     */
        void buildModel();

        /**
     * Model builder (schema) specific implementation to determine whether the controller
     * definition artifacts have changed in such a way that should result in redeploying the
     * object model.
     *
     * @see org.openremote.controller.service.Deployer.ControllerDefinitionWatch
     *
     * @return  true if the object model should be reloaded, false otherwise
     */
        boolean hasControllerDefinitionChanged();

        /**
     * TODO :
     *
     *   This signature is temporary and will go away, see comments in Version20ModelBuilder,
     *   Deployer.queryElementByID() and Deployer.queryElementByName() methods.
     *
     * @return
     *
     * @throws XMLParsingException 
     */
        Document getControllerDocument() throws InitializationException;
    }

    /**
   * TODO :
   *
   *   placeholder for the next major schema version that is currently in planning stages.
   */
    private static class Version30ModelBuilder implements ModelBuilder {

        @Override
        public Document getControllerDocument() {
            return null;
        }

        @Override
        public void buildModel() {
        }

        @Override
        public boolean hasControllerDefinitionChanged() {
            return false;
        }

        /**
     * Utility method to return a Java I/O File instance representing the artifact with
     * controller runtime object model definition.
     *
     * @param   config    controller's user configuration
     *
     * @return  file representing an object model definition for a controller
     */
        private static File getControllerDefinitionFile(ControllerConfiguration config) {
            String uri = new File(config.getResourcePath()).toURI().resolve("openremote.xml").getPath();
            return new File(uri);
        }

        /**
     * Utility method to isolate the privileged code block for file read access check (exists)
     *
     * @param   config    controller's user configuration
     *
     * @return  true if file exists; false if file does not exists or was denied by
     *          security manager
     */
        private static boolean checkControllerDefinitionExists(ControllerConfiguration config) {
            final File file = getControllerDefinitionFile(config);
            try {
                return AccessController.doPrivilegedWithCombiner(new PrivilegedAction<Boolean>() {

                    @Override
                    public Boolean run() {
                        return file.exists();
                    }
                });
            } catch (SecurityException e) {
                log.error("Security manager prevented read access to file ''{0}'' : {1}", e, file.getAbsoluteFile(), e.getMessage());
                return false;
            }
        }
    }

    /**
   * Abstracts the connectivity from controller to back-end in this nested class. <p>
   *
   * Currently only the deployer is making connections to backend. This will be later
   * expanded with local device discovery data import, log analytics, data collection
   * history, remote access and other operations. At that point this class visibility may
   * be expanded and made more generic.
   */
    private static class BeehiveConnection {

        /**
     * Part of the Beehive URL to retrieve user's controller configuration from their
     * account. <p>
     *
     * The complete URL should be :
     * [Beehive REST Base URL]/BEEHIVE_REST_USER_DIR/[username]/BEEHIVE_REST_OPENREMOTE_ZIP
     */
        private static final String BEEHIVE_REST_USER_DIR = "user";

        /**
     * Part of the Beehive URL to retrieve user's controller configuration from their
     * account. <p>
     *
     * The complete URL should be :
     * [Beehive REST Base URL]/BEEHIVE_REST_USER_DIR/[username]/BEEHIVE_REST_OPENREMOTE_ZIP
     */
        private static final String BEEHIVE_REST_OPENREMOTE_ZIP = "openremote.zip";

        /**
     * Reference to the deployer that uses this connection.
     */
        private Deployer deployer;

        /**
     * Constructs a new connection object for a given deployer.
     *
     * @param deployer    reference to the deployer instance that owns this connection
     */
        private BeehiveConnection(Deployer deployer) {
            this.deployer = deployer;
        }

        /**
     * Downloads user's configuration from Beehive using the user's account name and credentials.
     *
     * @param username      user's account name -- part of the HTTP GET URL used to retrieve the
     *                      account configuration
     * @param credentials   user's credentials to access their account
     *
     * @return  I/O stream to read the incoming ZIP file from user's account. Note that it is up
     *          to the caller to close the stream when appropriate. The incoming stream has basic
     *          buffering for read operations enabled.
     *
     * @throws ConfigurationException   if the connection to backend cannot be created due to
     *                                  configuration errors in the controller
     *
     * @throws ConnectionException      if the connection creation fails for any reason
     */
        private InputStream downloadZip(String username, String credentials) throws ConfigurationException, ConnectionException {
            String beehiveBase = deployer.controllerConfig.getBeehiveRESTRootUrl();
            String httpURI = BEEHIVE_REST_USER_DIR + "/" + username + "/" + BEEHIVE_REST_OPENREMOTE_ZIP;
            try {
                URL beehiveBaseURL = new URL(beehiveBase);
                URI beehiveUserURI = beehiveBaseURL.toURI().resolve(httpURI);
                URLConnection connection = beehiveUserURI.toURL().openConnection();
                if (!(connection instanceof HttpURLConnection)) {
                    throw new ConfigurationException("The ''{0}'' property ''{1}'' must be a URL with http:// schema.", ControllerConfiguration.BEEHIVE_REST_ROOT_URL, beehiveBase);
                }
                HttpURLConnection http = (HttpURLConnection) connection;
                http.setDoInput(true);
                http.setRequestMethod("GET");
                http.addRequestProperty(Constants.HTTP_AUTHORIZATION_HEADER, Constants.HTTP_BASIC_AUTHORIZATION + encode(username, credentials));
                http.connect();
                int response = http.getResponseCode();
                switch(response) {
                    case HttpURLConnection.HTTP_OK:
                        return new BufferedInputStream(http.getInputStream());
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        throw new ConnectionException("Authentication failed, please check your username and password.");
                    default:
                        throw new ConnectionException("Connection to ''{0}'' failed, HTTP error code {1} - {2}", beehiveUserURI, response, http.getResponseMessage());
                }
            } catch (MalformedURLException e) {
                throw new ConfigurationException("Configuration property ''{0}'' with value ''{1}'' is not a valid URL : {2}", e, ControllerConfiguration.BEEHIVE_REST_ROOT_URL, beehiveBase, e.getMessage());
            } catch (URISyntaxException e) {
                throw new ConfigurationException("Invalid URI : {0}", e, e.getMessage());
            } catch (ProtocolException e) {
                throw new ConnectionException("Failed to create HTTP request : {0}", e, e.getMessage());
            } catch (IOException e) {
                throw new ConnectionException("Downloading account configuration failed : {0}", e, e.getMessage());
            }
        }

        private String encode(String username, String password) {
            Md5PasswordEncoder encoder = new Md5PasswordEncoder();
            String encodedPwd = encoder.encodePassword(new String(password), username);
            if (username == null || encodedPwd == null) {
                return null;
            }
            return new String(Base64.encodeBase64((username + ":" + encodedPwd).getBytes()));
        }
    }

    /**
   * Key implementation for object builders. Constructs a key based on the controller schema
   * version and xml segment identifier provided by a concrete object builder implementation.
   *
   *
   * TODO: this will be re-defined for ORCJAVA-182 and ORCJAVA-185
   * 
   * @see org.openremote.controller.model.xml.ObjectBuilder#getSchemaVersion()
   * @see org.openremote.controller.model.xml.ObjectBuilder#getRootSegment()
   */
    private static class BuilderKey {

        private ControllerSchemaVersion version;

        private XMLSegment segment;

        private BuilderKey(ControllerSchemaVersion version, XMLSegment rootSegment) {
            this.version = version;
            this.segment = rootSegment;
        }

        @Override
        public int hashCode() {
            return version.hashCode() + segment.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o == this) {
                return true;
            }
            if (!(o.getClass().equals(this.getClass()))) {
                return false;
            }
            BuilderKey other = (BuilderKey) o;
            return (this.version.equals(other.version)) && (this.segment.equals(other.segment));
        }
    }
}
