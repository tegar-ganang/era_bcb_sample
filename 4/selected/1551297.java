package com.volantis.devrep.repository.accessors;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import com.volantis.mcs.accessors.xml.ZipArchive;
import com.volantis.devrep.repository.api.accessors.xml.EclipseEntityResolver;
import com.volantis.mcs.repository.RepositoryException;
import com.volantis.synergetics.io.IOUtils;
import com.volantis.xml.schema.JarFileEntityResolver;
import com.volantis.xml.schema.W3CSchemata;
import com.volantis.synergetics.cornerstone.utilities.xml.jaxp.TransformerMetaFactory;
import com.volantis.synergetics.log.LogDispatcher;
import com.volantis.devrep.localization.LocalizationFactory;
import com.volantis.synergetics.UndeclaredThrowableException;
import com.volantis.synergetics.localization.ExceptionLocalizer;
import com.volantis.devrep.device.api.xml.DeviceSchemas;
import com.volantis.devrep.repository.api.accessors.xml.DeviceRepositoryConstants;
import com.volantis.devrep.repository.api.devices.DeviceRepositorySchemaConstants;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.DefaultJDOMFactory;
import org.jdom.input.JDOMFactory;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.xml.sax.XMLReader;
import org.xml.sax.XMLFilter;

/**
 * This xml accessor has the ability to read the contents of the xml repository
 * (in this case, a zip file) and permit user actions on the contents of the
 * repository such as renaming, deleting and adding devices in the device
 * hierarchy.
 *
 * <p>
 * If any modifications are made then the {@link #saveRepositoryArchive} must
 * be called in order to save the modifications (changes are then immediately
 * updated in the repository).
 * <p>
 *
 * Note that this accessor permits the reading of repositories identified only
 * by a filename.
 */
public class EclipseDeviceRepository {

    /**
     * Volantis copyright mark.
     * */
    private static String mark = "(c) Volantis Systems Ltd 2004. ";

    /**
     * Used for logging
     */
    private static final LogDispatcher logger = LocalizationFactory.createLogger(EclipseDeviceRepository.class);

    /**
     * Used to obtain localized messages.
     */
    private static final ExceptionLocalizer exceptionLocalizer = LocalizationFactory.createExceptionLocalizer(EclipseDeviceRepository.class);

    /**
     * xsi constant definiton
     */
    private static final String XSI = "xsi";

    /**
     * Schema location definition.
     */
    private static final String SCHEMA_LOCATION = "schemaLocation";

    /**
     * The standard element name filter.
     */
    public static final DeletionFilter STANDARD_ELEMENT_FILTER = new DeletionFilter(new DeletionFilter.NodeIdentifier[] { new DeletionFilter.NodeIdentifier(DeviceRepositorySchemaConstants.STANDARD_ELEMENT_NAME, DeviceSchemas.DEVICE_CURRENT.getNamespaceURL()), new DeletionFilter.NodeIdentifier(DeviceRepositorySchemaConstants.STANDARD_ELEMENT_NAME, DeviceSchemas.IDENTIFICATION_CURRENT.getNamespaceURL()), new DeletionFilter.NodeIdentifier(DeviceRepositorySchemaConstants.STANDARD_ELEMENT_NAME, DeviceSchemas.TAC_IDENTIFICATION_CURRENT.getNamespaceURL()) });

    /**
     * Permit validation to be on or off during parsing of the xml files
     * contained in the repository.
     */
    private boolean validation;

    /**
     * Ignore whitespace whilst parsing the xml input within the repository.
     */
    private boolean ignoreWhitespace;

    /**
     * The archive which stores the device repository.
     */
    private ZipArchive repositoryArchive;

    /**
     * Store the definitions.xml JDOM document.
     */
    private Document xmlDefinitionsDocument;

    /**
     * Store the hierarchy.xml JDOM document.
     * */
    private Document xmlHierarchyDocument = null;

    /**
     * Store the identification.xml JDOM document.
     */
    private Document xmlIdentificationDocument = null;

    /**
     * Store the tac-identification.xml JDOM document.
     */
    private Document xmlTACIdentificationDocument = null;

    /**
     * The Properties object which contains both standard and custom properties.
     */
    private Properties properties;

    /**
     * The full pathname within the device repository of the standard
     * properties file.
     */
    private String standardPropertiesPath;

    /**
     * The full pathname within the device repository of the custom
     * properties file.
     */
    private String customPropertiesPath;

    /**
     * The version of the device repository being accessed.
     */
    private String version;

    /**
     * The revision of the device repository being accessed.
     */
    private String revision;

    /**
     * Used when creating the documents that this accessor manages
     */
    private JDOMFactory factory;

    /**
     * The xml filter for this accessor.
     */
    private XMLFilter xmlFilter;

    /**
     * Create the XMLDeviceRepositoryAccessor instance with the specified
     * filename.
     * @param repositoryFilename the file name for the device repository.
     * @param transformerMetaFactory the meta factory for creating XSL
     *      transforms, used to do automatic  s.
     * @param jdomFactory A {@link JDOMFactory} instance that will be used to
     * create the Documents that this accessor manages.
     * @throws IllegalArgumentException if the repositoryFilename or factory
     * arguments are null.
     */
    public EclipseDeviceRepository(String repositoryFilename, TransformerMetaFactory transformerMetaFactory, JDOMFactory jdomFactory, XMLFilter xmlFilter) throws RepositoryException {
        this(repositoryFilename, transformerMetaFactory, jdomFactory, true, true, xmlFilter);
    }

    /**
     * Create the XMLDeviceRepositoryAccessor instance with the specified
     * archive accessor.
     *
     * @param mdprAccessor the MDPRDeviceAccessor used to read the repository
     *                     file.
     * @param factory A {@link JDOMFactory} instance that will be used to
     * create the Documents that this accessor manages.
     * @throws IllegalArgumentException if the mdprAcessor or factory
     * arguments are null.
     */
    public EclipseDeviceRepository(MDPRArchiveAccessor mdprAccessor, JDOMFactory factory) throws RepositoryException {
        this(mdprAccessor, factory, true, true, null);
    }

    /**
     * Create the XMLDeviceRepositoryAccessor instance with the specified file
     * name and parsing criteria.
     *
     * @param repositoryFilename the file name for the device repository.
     * @param transformerMetaFactory the meta factory for creating XSL
     *      transforms, used to do automatic upgrades.
     * @param jdomFactory A {@link JDOMFactory} instance that will be used to
     * create the Documents that this accessor manages.
     * @param validation         true if validation of the contained xml files
     *                           is required, false otherwise.
     * @param ignoreWhitespace   ignore whitespace when parsing the xml file.
     * @throws IllegalArgumentException if the repositoryFilename or factory
     * arguments are null.
     */
    public EclipseDeviceRepository(String repositoryFilename, TransformerMetaFactory transformerMetaFactory, JDOMFactory jdomFactory, boolean validation, boolean ignoreWhitespace, XMLFilter xmlFilter) throws RepositoryException {
        this(new MDPRArchiveAccessor(repositoryFilename, transformerMetaFactory), jdomFactory, validation, ignoreWhitespace, xmlFilter);
    }

    /**
     * Create the XMLDeviceRepositoryAccessor instance with the specified
     * archive accessor and parsing criteria.
     *
     * @param mdprAccessor the MDPRDeviceAccessor used to read the repository
     *                     file.
     * @param factory A {@link JDOMFactory} instance that will be used to
     * create the Documents that this accessor manages.
     * @param validation         true if validation of the contained xml files
     *                           is required, false otherwise.
     * @param ignoreWhitespace   ignore whitespace when parsing the xml file.
     * @param xmlFilter the XMLFilter used when writing out the repository
     * @throws IllegalArgumentException if the mdprAccessor or factory
     * arguments are null.
     */
    public EclipseDeviceRepository(MDPRArchiveAccessor mdprAccessor, JDOMFactory factory, boolean validation, boolean ignoreWhitespace, XMLFilter xmlFilter) throws RepositoryException {
        if (mdprAccessor == null) {
            throw new IllegalArgumentException("Archive accessor cannot be null.");
        }
        if (factory == null) {
            throw new IllegalArgumentException("factory cannot be null.");
        }
        repositoryArchive = mdprAccessor.getArchive();
        version = retrieveVersion(repositoryArchive);
        this.factory = factory;
        this.validation = validation;
        this.ignoreWhitespace = ignoreWhitespace;
        this.xmlFilter = xmlFilter;
        InputStream input = repositoryArchive.getInputFrom(DeviceRepositoryConstants.STANDARD_DEFINITIONS_XML);
        if (input != null) {
            xmlDefinitionsDocument = createNewDocument(new BufferedInputStream(input));
            input = repositoryArchive.getInputFrom(DeviceRepositoryConstants.CUSTOM_DEFINITIONS_XML);
            if (input != null) {
                Document customDefinitions = createNewDocument(new BufferedInputStream(input));
                mergeDefinitionDocuments(xmlDefinitionsDocument, customDefinitions);
            }
        } else {
            throw new RepositoryException(exceptionLocalizer.format("device-repository-file-missing", DeviceRepositoryConstants.DEFINITIONS_XML));
        }
        input = repositoryArchive.getInputFrom(DeviceRepositoryConstants.HIERARCHY_XML);
        if (input != null) {
            xmlHierarchyDocument = createNewDocument(new BufferedInputStream(input));
        } else {
            throw new RepositoryException(exceptionLocalizer.format("device-repository-file-missing", DeviceRepositoryConstants.HIERARCHY_XML));
        }
        input = repositoryArchive.getInputFrom(DeviceRepositoryConstants.IDENTIFICATION_XML);
        if (input != null) {
            xmlIdentificationDocument = createNewDocument(new BufferedInputStream(input));
        } else {
            throw new RepositoryException(exceptionLocalizer.format("device-repository-file-missing", DeviceRepositoryConstants.IDENTIFICATION_XML));
        }
        input = repositoryArchive.getInputFrom(DeviceRepositoryConstants.TAC_IDENTIFICATION_XML);
        if (input != null) {
            xmlTACIdentificationDocument = createNewDocument(new BufferedInputStream(input));
        } else {
        }
        revision = retrieveRevision(repositoryArchive, factory);
        try {
            properties = createMergedProperties(repositoryArchive);
        } catch (IOException ioe) {
            throw new RepositoryException(exceptionLocalizer.format("device-repository-file-missing", "policies.properties"), ioe);
        }
    }

    /**
     * Retrieve the version of a device repository from the ZipArchive
     * representation of that repository.
     * @param repositoryArchive the repository
     * @param factory the JDOMFactory for creating JDOM objects
     * @return the revision of the provided repository
     * @throws RepositoryException
     */
    private static String retrieveRevision(ZipArchive repositoryArchive, JDOMFactory factory) throws RepositoryException {
        InputStream input = repositoryArchive.getInputFrom(DeviceRepositoryConstants.REPOSITORY_XML);
        return retrieveRevision(input, factory);
    }

    /**
     * Retrieve the repository revision from an InputStream.
     * @param is an InputStream attached to the repository revision file.
     * @param factory the JDOMFactory for creating JDOM objects
     * @return the version as read from the is stream
     * @throws RepositoryException if there was a problem read the version
     */
    private static String retrieveRevision(InputStream is, JDOMFactory factory) throws RepositoryException {
        String revision = null;
        if (is != null) {
            Document repositoryDocument = createNewDocument(new BufferedInputStream(is), factory, true, true);
            revision = repositoryDocument.getRootElement().getAttributeValue(DeviceRepositorySchemaConstants.REVISION_ATTRIBUTE_NAME);
        } else {
            throw new RepositoryException(exceptionLocalizer.format("device-repository-file-missing", DeviceRepositoryConstants.REPOSITORY_XML));
        }
        return revision;
    }

    /**
     * Retrieve the version of a device repository from the ZipArchive
     * representation of that repository.
     * @param repositoryArchive the repository
     * @return the version of the repository
     * @throws RepositoryException if there is a problem accessing the version
     * information in the repository.
     */
    private static String retrieveVersion(ZipArchive repositoryArchive) throws RepositoryException {
        InputStream is = null;
        String version = null;
        try {
            is = repositoryArchive.getInputFrom(DeviceRepositoryConstants.VERSION_FILENAME);
            if (is == null) {
                throw new RepositoryException(exceptionLocalizer.format("device-repository-file-missing", DeviceRepositoryConstants.VERSION_FILENAME));
            }
            version = retrieveVersion(is);
            return version;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("unexpected-ioexception", e);
                    if (version != null) {
                        throw new RepositoryException(exceptionLocalizer.format("unexpected-ioexception"), e);
                    }
                }
            }
        }
    }

    /**
     * Retrieve the version from an InputStream.
     * @param is an InputStream attached to the repository version file.
     * @return the version as read from the input stream
     * @throws RepositoryException if there was a problem read the version
     */
    private static String retrieveVersion(InputStream is) throws RepositoryException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            IOUtils.copy(is, buffer);
        } catch (IOException e) {
            throw new RepositoryException(exceptionLocalizer.format("device-repository-file-missing", DeviceRepositoryConstants.VERSION_FILENAME), e);
        }
        return buffer.toString().trim();
    }

    /**
     * Set the name of the device repository being accessed by this accessor -
     * i.e. rename the file.
     * @param filename the absolute path of the filename for the repository.
     * @throws IllegalArgumentException if the provided filename would
     * produce an unwriteable repository.
     */
    public void setRepositoryName(String filename) {
        repositoryArchive.setArchiveFilename(filename);
    }

    /**
     * Provide access to the device hierarchy document.
     * @return The JDOM Document that represents device hierearchy.
     */
    public Document getDeviceHierarchyDocument() {
        return xmlHierarchyDocument;
    }

    /**
     * Provide access to the device identification document.
     * @return The JDOM Document for the device identification markup.
     */
    public Document getDeviceIdentificationDocument() {
        return xmlIdentificationDocument;
    }

    /**
     * Provide access to the device TAC identification document.
     * @return The JDOM Document for the device TAC identification markup, or
     *         null if no TAC identification document exists.
     */
    public Document getDeviceTACIdentificationDocument() {
        return xmlTACIdentificationDocument;
    }

    /**
     * Provide access to the device policy definitions document.
     * @return The JDOM Document that represents device policy definitions.
     */
    public Document getDevicePolicyDefinitions() {
        return xmlDefinitionsDocument;
    }

    /**
     * Write a file to a directory in a zip archive.
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @param archive the archive to write to.
     * @param directory the directory in the archive to write to.
     * @param filename the base name of the file.
     * @param document the JDOM document containing file content.
     * @throws RepositoryException
     */
    private void writeFile(ZipArchive archive, String directory, String filename, Document document) throws RepositoryException {
        String path = getXMLFilePath(directory, filename);
        OutputStream output = archive.getOutputTo(path);
        try {
            writeDocument(document, output);
        } catch (IOException e) {
            throw new RepositoryException(exceptionLocalizer.format("device-repository-update-failure", path), e);
        }
    }

    /**
     * Moves the specified device so that it has a new parent in the device
     * hierarchy. Note the new parent should not be a child of the device
     * that is being moved.
     * @param device the name of the device to move. Cannot be null.
     * @param newParentDevice the name of the device that is to be the new
     * parent for the device to be moved
     * @throws RepositoryException if the device or newParentDevice does not
     * exist
     * @throws IllegalArgumentException if the device or newParentDevice
     * arguments are null.
     * @throws org.jdom.IllegalAddException if the new parent device is a
     * descendent of the device being moved.
     */
    public void moveDevice(String device, String newParentDevice) throws RepositoryException {
        if (device == null) {
            throw new IllegalArgumentException("device cannot be null");
        }
        if (newParentDevice == null) {
            throw new IllegalArgumentException("newParentDevice cannot be null");
        }
        Element deviceElement = getHierarchyDeviceElement(device);
        if (deviceElement == null) {
            throw new RepositoryException(exceptionLocalizer.format("device-definition-missing", device));
        }
        Element parentElement = getHierarchyDeviceElement(newParentDevice);
        if (parentElement == null) {
            throw new RepositoryException(exceptionLocalizer.format("device-definition-missing", newParentDevice));
        }
        Element oldParent = deviceElement.getParent();
        try {
            deviceElement.detach();
            parentElement.addContent(deviceElement);
        } finally {
            if (deviceElement.getParent() == null) {
                oldParent.addContent(deviceElement);
            }
        }
    }

    /**
     * Take a directory and filename and return a full filepath to an xml
     * file, such as path/to/my/device-file.xml
     * <p> If the directory path is not suffixed by a path separator then one
     * is added.
     * <p> If the file name is not suffixed by a ".xml" extension then it is
     * appended.
     * @param directory The directory file path
     * @param filename The file name
     * @return A full file path to an xml file.
     */
    private String getXMLFilePath(String directory, String filename) {
        StringBuffer result = new StringBuffer(directory);
        if (!(directory.endsWith("/") || directory.endsWith("\\"))) {
            result.append(File.separator);
        }
        result.append(filename);
        if (!(filename.endsWith(DeviceRepositoryConstants.XML_SUFFIX))) {
            result.append(DeviceRepositoryConstants.XML_SUFFIX);
        }
        return result.toString();
    }

    /**
     * Get the version of the device repository associated with this
     * accessor.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get the revision of the device repository associated with this
     * accessor.
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Retrieve the identification XML for a device in the form of a JDOM
     * Element.
     * @param deviceName The name of the device whose identification XML to
     * retrieve.
     * @return The Element representing the device identification for the
     * specified device or null if there was no device element
     */
    public Element retrieveDeviceIdentificationElement(String deviceName) {
        return retrieveDeviceElementFromDocument(deviceName, xmlIdentificationDocument);
    }

    /**
     * Retrieve the TAC identification XML for a device in the form of a JDOM
     * Element.
     * @param deviceName The name of the device whose identification XML to
     * retrieve.
     * @return The Element representing the device TAC identification for the
     * specified device or null if there was no device element
     */
    public Element retrieveTACDeviceElement(String deviceName) {
        return retrieveDeviceElementFromDocument(deviceName, xmlTACIdentificationDocument);
    }

    /**
     * Retrieve the Device Element XML for a device in the form of a JDOM
     * Element from the specified Document.
     * <p> Note that this method requires that the "device" elements are
     * children of the root element and that the device name is represented as
     * by an attribute on element.  The attribute name is defined as
     * {@link XMLAccessorConstants#DEVICE_NAME_ATTRIBUTE}
     *
     * @param deviceName The name of the device whose identification XML to
     * retrieve.
     * @param document The document from which to retrieve the Device element.
     * @return The Element representing the device xml for the specified device
     * or null if there was no device element
     */
    private Element retrieveDeviceElementFromDocument(String deviceName, Document document) {
        if (deviceName == null) {
            throw new IllegalArgumentException("deviceName cannot be null");
        }
        Element idElement = null;
        if (document != null) {
            List children = document.getRootElement().getChildren();
            Iterator iterator = children.iterator();
            while (iterator.hasNext() && idElement == null) {
                Element child = (Element) iterator.next();
                String childDeviceName = child.getAttributeValue("name");
                if (childDeviceName.equals(deviceName)) {
                    idElement = child;
                }
            }
        }
        return idElement;
    }

    /**
     * Get the prefix for customer created device names.
     * @return the prefix for a device name that designates that device as
     * a customer created device.
     */
    public static String getCustomDeviceNamePrefix() {
        return DeviceRepositoryConstants.CUSTOM_DEVICE_NAME_PREFIX;
    }

    /**
     * Get the prefix for customer created device names.
     * @return the prefix for a device name that designates that device as
     * a customer created device.
     */
    public static String getCustomPolicyNamePrefix() {
        return DeviceRepositoryConstants.CUSTOM_POLICY_NAME_PREFIX;
    }

    /**
     * Determine if a device is a standard device (i.e. defined by Volantis)
     * or not.
     * @param deviceName the name of the device to test
     * @return true if the named device is a standard device; otherwise false
     * (note that a return value of false does not indicate that the device
     * exists).
     */
    public static boolean isStandardDevice(String deviceName) {
        return !deviceName.startsWith(getCustomDeviceNamePrefix());
    }

    /**
     * Retrieve a Device as a JDOM Element. This method will merge custom
     * attributes with standard attributes in the returned Element.
     * @param deviceName The name of the device to retrieve.
     * @return The Element definition of the specified device.
     * @throws RepositoryException If the specified device could not be found
     * or if there was a problem accessing the repository.
     */
    public Element retrieveDeviceElement(String deviceName) throws RepositoryException {
        Document deviceDocument = null;
        Element hierarchyDeviceElement = getHierarchyDeviceElement(deviceName);
        if (hierarchyDeviceElement != null) {
            String src = getXMLFilePath(DeviceRepositoryConstants.STANDARD_DEVICE_DIRECTORY, deviceName);
            InputStream standardInput = repositoryArchive.getInputFrom(src);
            if (standardInput != null) {
                deviceDocument = createNewDocument(standardInput);
                src = getXMLFilePath(DeviceRepositoryConstants.CUSTOM_DEVICE_DIRECTORY, deviceName);
                InputStream customInput = repositoryArchive.getInputFrom(src);
                if (customInput != null) {
                    Document customDocument = createNewDocument(customInput);
                    mergeDeviceDocuments(deviceDocument, customDocument);
                }
            } else {
                throw new RepositoryException(exceptionLocalizer.format("device-definition-missing", deviceName));
            }
            Element device = deviceDocument.getRootElement();
            String deviceNameAttr = device.getAttributeValue(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE);
            if (deviceNameAttr == null || deviceNameAttr.length() == 0) {
                device.setAttribute(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE, deviceName);
            }
        }
        return deviceDocument.getRootElement();
    }

    /**
     * Merge the standard and custom documents for the definitions repository
     * files.
     *
     * The standard definitions document and the custom definitions document
     * both share the "types" element.  That is to say the information is
     * repeated in both documents.  As such we ignore any "types" elements
     * when merging the custom document contents into the standard document.
     *
     * @param standard The document containing the standard policy definitions
     * @param custom The document containing the custom policy definitions
     */
    private void mergeDefinitionDocuments(Document standard, Document custom) {
        Element sDefs = standard.getRootElement();
        Element cDefs = custom.getRootElement();
        List cChildren = cDefs.getChildren();
        while (cChildren.size() > 0) {
            Element child = (Element) cChildren.get(0);
            child.detach();
            if (!(DeviceRepositorySchemaConstants.POLICY_DEFINITION_TYPES_ELEMENT_NAME.equals(child.getName()))) {
                sDefs.addContent(child);
            }
        }
    }

    /**
     * Merge the standard and custom documents for a Device repository file.
     * The policy elements are removed from the custom document and added to
     * the standard.
     * @param standard The document containing the standard device policies
     * @param custom The document containing the custom device policies
     */
    private void mergeDeviceDocuments(Document standard, Document custom) {
        Element sDevice = standard.getRootElement();
        Element sPolicies = sDevice.getChild(DeviceRepositorySchemaConstants.POLICIES_ELEMENT_NAME, sDevice.getNamespace());
        Element cDevice = custom.getRootElement();
        Element cPolicies = cDevice.getChild(DeviceRepositorySchemaConstants.POLICIES_ELEMENT_NAME, cDevice.getNamespace());
        List cChildren = cPolicies.getChildren();
        while (cChildren.size() > 0) {
            Element cPolicy = (Element) cChildren.get(0);
            cPolicy.detach();
            sPolicies.addContent(cPolicy);
        }
    }

    /**
     * Remove the device from the device hierarchy. If the device to be removed
     * cannot be found, throw and exception. If the removal was successful then
     * return the removed jdom element.
     * <p>
     * This method will recurse through the children of the device removing
     * each from the hierarchy from the leaf nodes upwards.  The device
     * files, hierarchy references and identifiers will all be removed.
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @param deviceName the device name to remove from the hierarchy.
     * @throws RepositoryException if the element to be removed could not be
     *                             found.
     */
    public void removeDevice(String deviceName) throws RepositoryException {
        if (deviceName == null) {
            throw new IllegalArgumentException("deviceName cannot be null");
        }
        Element element = getHierarchyDeviceElement(deviceName);
        if (element != null) {
            removeDevice(element);
        } else {
            throw new RepositoryException(exceptionLocalizer.format("cannot-delete-device", deviceName));
        }
    }

    /**
     * Remove the device Element from the device hierarchy.
     * <p>
     * This method will recurse through the children of the device removing
     * each from the hierarchy from the leaf nodes upwards.  The device
     * files, hierarchy references and identifiers will all be removed.
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @param element the device Element to remove from the hierarchy.
     * @throws RepositoryException if the element to be removed could not be
     *                             found.
     */
    private void removeDevice(Element element) throws RepositoryException {
        List children = new ArrayList(element.getChildren());
        for (int i = 0; i < children.size(); i++) {
            Element child = (Element) children.get(i);
            removeDevice(child);
        }
        String deviceName = element.getAttributeValue(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE);
        ZipArchive archive = new ZipArchive(repositoryArchive);
        removeFile(archive, DeviceRepositoryConstants.STANDARD_DEVICE_DIRECTORY, true, deviceName);
        removeFile(archive, DeviceRepositoryConstants.CUSTOM_DEVICE_DIRECTORY, false, deviceName);
        element.detach();
        removeDeviceIdentifiers(deviceName);
        removeDeviceTACs(deviceName);
        repositoryArchive = archive;
    }

    /**
     * Remove the device element in the identifiers document that contains the
     * device indentifiers for the named device.
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @param deviceName The name of the device to remove from the identifiers
     */
    private void removeDeviceIdentifiers(String deviceName) {
        Element identifiers = retrieveDeviceIdentificationElement(deviceName);
        if (identifiers != null) {
            identifiers.detach();
        }
    }

    /**
     * Remove the device element in the tac-identification document that
     * contains the TAC information for the named device.
     *
     * <p>Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.</p>
     *
     * @param deviceName The name of the device to remove from the TAC
     *                   identification
     */
    private void removeDeviceTACs(String deviceName) {
        Element tacs = retrieveTACDeviceElement(deviceName);
        if (tacs != null) {
            tacs.detach();
        }
    }

    /**
     * Rename the device element in the identifiers document that contains the
     * device indentifiers for the named device.
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @param oldName The current name of the device.
     * @param newName The new name of the device.
     */
    private void renameDeviceIdentifiers(String oldName, String newName) {
        Element identifiers = retrieveDeviceIdentificationElement(oldName);
        if (identifiers != null) {
            identifiers.setAttribute(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE, newName);
        }
    }

    /**
     * Rename the device element in the tac-identification document that
     * contains the TAC information for the named device.
     *
     * <p>Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.</p>
     *
     * @param oldName The current name of the device.
     * @param newName The new name of the device.
     */
    private void renameDeviceTACs(String oldName, String newName) {
        Element tacs = retrieveTACDeviceElement(oldName);
        if (tacs != null) {
            tacs.setAttribute(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE, newName);
        }
    }

    /**
     * Remove a file from a directory in a zip archive.
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * <p>
     * This method does not guarantee the integrity of the archive.  Clients
     * are responsible for recovering errors should they arise during execution
     * of this method.
     *
     * @param archive the archive to remove the file from.
     * @param directory the directory in the archive to remove from.
     * @param mandatory if the file must exist.
     * @param filename the base name of the file.
     * @throws RepositoryException
     */
    private void removeFile(ZipArchive archive, String directory, boolean mandatory, String filename) throws RepositoryException {
        String path = getXMLFilePath(directory, filename);
        boolean deleted = archive.delete(path);
        if (mandatory && !deleted) {
            throw new RepositoryException(exceptionLocalizer.format("cannot-delete-file", path));
        }
    }

    /**
     * Add a new device file to the device repository archive
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @param deviceName The name of the device to add.
     */
    public void addDeviceElement(String deviceName) throws RepositoryException {
        if (deviceName == null || deviceName.trim().length() == 0) {
            throw new IllegalArgumentException("deviceName cannot be null or empty");
        }
        Map map = new HashMap();
        map.put(deviceName, createEmptyDeviceDocument().getRootElement());
        writeDeviceElements(map);
    }

    /**
     * Adds a device element to the identifiers document for the named
     * device. Note - no user agent or header patterns will be added to the
     * created device element.
     * @param deviceName The name of the device that requires an entry in
     * the identification document
     * @throws RepositoryException if the device already has identifiers
     * defined in the device repository.
     */
    public void addIdentifiersDeviceElement(String deviceName) throws RepositoryException {
        addDeviceElementToDocumentRoot(deviceName, xmlIdentificationDocument);
    }

    /**
     * Adds a device element to the TAC document for the named device. Note -
     * no user agent or header patterns will be added to the created device
     * element.
     * @param deviceName The name of the device that requires an entry in
     * the TAC document
     * @throws RepositoryException if the device already has TACs defined in
     * the device repository.
     */
    public void addTACDeviceElement(String deviceName) throws RepositoryException {
        addDeviceElementToDocumentRoot(deviceName, xmlTACIdentificationDocument);
    }

    /**
     * Adds a device element to the specified document for the named
     * device.
     * @param deviceName The name of the device that requires an entry in
     * the  document
     * @throws RepositoryException if the device already has an element defined
     * in the document.
     */
    private void addDeviceElementToDocumentRoot(String deviceName, Document document) throws RepositoryException {
        if (deviceName == null || deviceName.trim().length() == 0) {
            throw new IllegalArgumentException("deviceName cannot be null or empty");
        }
        Element existing = retrieveDeviceElementFromDocument(deviceName, document);
        if (existing != null) {
            throw new RepositoryException(exceptionLocalizer.format("device-already-exists", deviceName));
        }
        Element root = document.getRootElement();
        root.addContent(createDeviceElement(deviceName, root.getNamespace()));
    }

    /**
     * Rename the device to the new name. This will only succeed if the device
     * name exists and the new device name to use does not already exist in the
     * hierarchy.
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @param deviceName    the device name to be renamed.
     * @param newDeviceName the new device name.
     * @throws IllegalArgumentException if deviceName or newDeviceName is null.
     */
    public void renameDevice(String deviceName, String newDeviceName) throws RepositoryException {
        if (deviceName == null) {
            throw new IllegalArgumentException("Cannot rename a null device name.");
        }
        if (newDeviceName == null) {
            throw new IllegalArgumentException("Cannot rename a device name to null name.");
        }
        if (deviceExists(newDeviceName)) {
            throw new RepositoryException(exceptionLocalizer.format("device-already-exists-cannot-rename", deviceName));
        }
        Element element = getHierarchyDeviceElement(deviceName);
        if (element == null) {
            throw new RepositoryException(exceptionLocalizer.format("device-not-found-cannot-rename", deviceName));
        }
        ZipArchive archive = new ZipArchive(repositoryArchive);
        Element parent = element.getParent();
        element.detach();
        try {
            renameDeviceFile(archive, DeviceRepositoryConstants.STANDARD_DEVICE_DIRECTORY, true, deviceName, newDeviceName);
            renameDeviceFile(archive, DeviceRepositoryConstants.CUSTOM_DEVICE_DIRECTORY, false, deviceName, newDeviceName);
            renameDeviceIdentifiers(deviceName, newDeviceName);
            renameDeviceTACs(deviceName, newDeviceName);
            element.setAttribute(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE, newDeviceName);
            repositoryArchive = archive;
        } finally {
            parent.addContent(element);
        }
    }

    /**
     * Rename a device file in a directory in a zip archive.
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * <p>
     * This method does not guarantee the integrity of the archive.  Clients
     * are responsible for recovering errors should they arise during execution
     * of this method.
     *
     * @param archive the archive file to rename in.
     * @param deviceDirectory the directory in the archive to rename in.
     * @param mandatory if the device file must exist.
     * @param deviceName the base name of the device file.
     * @param newDeviceName the base name of the new device file.
     * @throws RepositoryException If the specified device could not be found
     * or if there was a problem renaming the device.
     */
    private void renameDeviceFile(ZipArchive archive, String deviceDirectory, boolean mandatory, String deviceName, String newDeviceName) throws RepositoryException {
        String devicePath = getXMLFilePath(deviceDirectory, deviceName);
        if (mandatory || archive.exists(devicePath)) {
            String newDevicePath = getXMLFilePath(deviceDirectory, newDeviceName);
            if (!archive.rename(devicePath, newDevicePath)) {
                throw new RepositoryException(exceptionLocalizer.format("device-rename-failed", new Object[] { devicePath, newDevicePath }));
            }
            Document deviceDoc = createNewDocument(archive.getInputFrom(newDevicePath));
            Element deviceElement = deviceDoc.getRootElement();
            deviceElement.setAttribute(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE, newDeviceName);
            try {
                writeDocument(deviceDoc, archive.getOutputTo(newDevicePath));
            } catch (IOException e) {
                throw new RepositoryException(exceptionLocalizer.format("device-update-failure", newDevicePath), e);
            }
        }
    }

    /**
     * Get the child device names for the specified device name.
     *
     * @param deviceName the device name to get the children device name(s).
     * @return the collection of child device names or null if no child names
     *         could be found.
     */
    public List getChildDeviceNames(String deviceName) {
        List list = null;
        Element element = getHierarchyDeviceElement(deviceName);
        if (element != null) {
            List children = element.getChildren();
            if (children != null && children.size() > 0) {
                list = new ArrayList();
                for (int i = 0; i < children.size(); i++) {
                    Element child = (Element) children.get(i);
                    if (DeviceRepositorySchemaConstants.DEVICE_ELEMENT_NAME.equals(child.getName())) {
                        list.add(child.getAttributeValue(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE));
                    }
                }
            }
        }
        return list;
    }

    /**
     * Return the root device's name.
     *
     * @return the root device's name, e.g. 'Master'.
     */
    public String getRootDeviceName() throws RepositoryException {
        String result = null;
        if (xmlHierarchyDocument != null) {
            Element root = xmlHierarchyDocument.getRootElement();
            List list = root.getChildren();
            if (list != null) {
                if (list.size() == 1) {
                    result = ((Element) list.get(0)).getAttributeValue(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE);
                } else if (list.size() > 1) {
                    throw new RepositoryException(exceptionLocalizer.format("device-hierarchy-too-many-roots", list));
                }
            }
        }
        return result;
    }

    /**
     * Return the fallback device name for the specified device. If none is
     * found return null.
     *
     * @param deviceName
     * @return the fallback device name for the specified device. If none is
     *         found return null.
     */
    public String getFallbackDeviceName(String deviceName) {
        String result = null;
        Element element = getHierarchyDeviceElement(deviceName);
        if ((element != null) && !element.isRootElement()) {
            result = element.getParent().getAttributeValue(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE);
        }
        return result;
    }

    /**
     * Return true if the device name exists in the repository, false
     * otherwise.
     *
     * @param deviceName the device name to use for the search.
     * @return true if the device name exists in the repository, false
     *         otherwise.
     */
    public boolean deviceExists(String deviceName) {
        return getHierarchyDeviceElement(deviceName) != null;
    }

    /**
     * Save the current device repository archive to the file system.
     * @throws RepositoryException if there was a problem writing the device
     * repository to the file system.
     */
    public void saveRepositoryArchive() throws RepositoryException {
        try {
            repositoryArchive.save();
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Write the device identifiers to the device repository archive.
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @throws RepositoryException if there was a problem writing the device
     * repository to the file system.
     */
    public void writeIdentifiers() throws RepositoryException {
        try {
            if (xmlIdentificationDocument != null) {
                ZipArchive archive = new ZipArchive(repositoryArchive);
                OutputStream out = archive.getOutputTo(DeviceRepositoryConstants.IDENTIFICATION_XML);
                writeDocument(xmlIdentificationDocument, out);
                repositoryArchive = archive;
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Write the device TAC identifiers to the device repository archive.
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @throws RepositoryException if there was a problem writing the device
     * repository to the file system.
     */
    public void writeTACs() throws RepositoryException {
        try {
            if (xmlTACIdentificationDocument != null) {
                ZipArchive archive = new ZipArchive(repositoryArchive);
                OutputStream out = archive.getOutputTo(DeviceRepositoryConstants.TAC_IDENTIFICATION_XML);
                writeDocument(xmlTACIdentificationDocument, out);
                repositoryArchive = archive;
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Write the device hierarchy to the device repository archive.
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @throws RepositoryException if there was a problem writing to the device
     * repository.
     */
    public void writeHierarchy() throws RepositoryException {
        try {
            if (xmlHierarchyDocument != null) {
                ZipArchive archive = new ZipArchive(repositoryArchive);
                OutputStream out = archive.getOutputTo(DeviceRepositoryConstants.HIERARCHY_XML);
                writeDocument(xmlHierarchyDocument, out);
                repositoryArchive = archive;
            } else {
                throw new RepositoryException(exceptionLocalizer.format("device-repository-invalid", DeviceRepositoryConstants.HIERARCHY_XML));
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Write the policy definitions to the device repository archive.  The
     * definitions must be split out into standard and custom files.
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @throws RepositoryException if there was a problem writing the
     * definitions file.
     */
    public void writeDefinitions() throws RepositoryException {
        Document standardDefDoc = createDocument(DeviceRepositorySchemaConstants.DEFINITIONS_ELEMENT_NAME, DeviceSchemas.POLICY_DEFINITIONS_CURRENT.getNamespaceURL(), DeviceSchemas.POLICY_DEFINITIONS_CURRENT.getLocationURL());
        Element standardDefRoot = standardDefDoc.getRootElement();
        Document customDefDoc = createDocument(DeviceRepositorySchemaConstants.DEFINITIONS_ELEMENT_NAME, DeviceSchemas.POLICY_DEFINITIONS_CURRENT.getNamespaceURL(), DeviceSchemas.POLICY_DEFINITIONS_CURRENT.getLocationURL());
        Element customDefRoot = customDefDoc.getRootElement();
        boolean hasCustomDefs = false;
        Element defRoot = xmlDefinitionsDocument.getRootElement();
        List children = defRoot.getChildren();
        Element element = null;
        Element clone = null;
        for (int i = 0; i < children.size(); i++) {
            element = (Element) children.get(i);
            if (DeviceRepositorySchemaConstants.CATEGORY_ELEMENT_NAME.equals(element.getName())) {
                clone = (Element) element.clone();
                String catName = clone.getAttributeValue(DeviceRepositorySchemaConstants.CATEGORY_NAME_ATTRIBUTE);
                if (catName != null && catName.equals(DeviceRepositorySchemaConstants.CUSTOM_CATEGORY_NAME)) {
                    customDefRoot.addContent(clone);
                    hasCustomDefs = true;
                } else {
                    standardDefRoot.addContent(clone);
                }
            } else if (DeviceRepositorySchemaConstants.POLICY_DEFINITION_TYPES_ELEMENT_NAME.equals(element.getName())) {
                clone = (Element) element.clone();
                standardDefRoot.addContent(clone);
                clone = (Element) element.clone();
                customDefRoot.addContent(clone);
            }
        }
        ZipArchive archive = new ZipArchive(repositoryArchive);
        removeFile(archive, DeviceRepositoryConstants.CUSTOM_DEFINITIONS_DIRECTORY, false, DeviceRepositorySchemaConstants.DEFINITIONS_DOCUMENT_NAME);
        if (hasCustomDefs) {
            writeFile(archive, DeviceRepositoryConstants.CUSTOM_DEFINITIONS_DIRECTORY, DeviceRepositorySchemaConstants.DEFINITIONS_DOCUMENT_NAME, customDefDoc);
        }
        removeFile(archive, DeviceRepositoryConstants.STANDARD_DEFINITIONS_DIRECTORY, false, DeviceRepositorySchemaConstants.DEFINITIONS_DOCUMENT_NAME);
        writeFile(archive, DeviceRepositoryConstants.STANDARD_DEFINITIONS_DIRECTORY, DeviceRepositorySchemaConstants.DEFINITIONS_DOCUMENT_NAME, standardDefDoc);
        repositoryArchive = archive;
    }

    /**
     * Write the Collection of device Elements to the device repository archive
     *
     * <p>
     * Note that, as per the class javadoc, changes made via this method are
     * only saved to disk when {@link #saveRepositoryArchive} is called.
     *
     * @param devices the Collection of device Elements to write to the device
     * repository archive
     * @throws RepositoryException if there was a problem writing the devices
     * to the repository.
     */
    public void writeDeviceElements(Map devices) throws RepositoryException {
        for (Iterator i = devices.keySet().iterator(); i.hasNext(); ) {
            String deviceName = (String) i.next();
            Element deviceElement = (Element) devices.get(deviceName);
            if (deviceElement != null) {
                Document standardDeviceDocument = createEmptyDeviceDocument();
                Document customDeviceDocument = null;
                Element policiesElement = deviceElement.getChild(DeviceRepositorySchemaConstants.POLICIES_ELEMENT_NAME, deviceElement.getNamespace());
                if (policiesElement != null) {
                    for (Iterator j = policiesElement.getChildren().iterator(); j.hasNext(); ) {
                        Element policyElement = (Element) j.next();
                        if (policyElement != null) {
                            Element clone = (Element) policyElement.clone();
                            String name = policyElement.getAttributeValue(DeviceRepositorySchemaConstants.POLICY_NAME_ATTRIBUTE);
                            Document target = standardDeviceDocument;
                            if (name != null && name.startsWith(DeviceRepositoryConstants.CUSTOM_POLICY_NAME_PREFIX)) {
                                if (customDeviceDocument == null) {
                                    customDeviceDocument = createEmptyDeviceDocument();
                                }
                                target = customDeviceDocument;
                            }
                            target.getRootElement().getChild(DeviceRepositorySchemaConstants.POLICIES_ELEMENT_NAME, deviceElement.getNamespace()).addContent(clone);
                        }
                    }
                }
                ZipArchive archive = new ZipArchive(repositoryArchive);
                removeFile(archive, DeviceRepositoryConstants.CUSTOM_DEVICE_DIRECTORY, false, deviceName);
                if (customDeviceDocument != null) {
                    writeFile(archive, DeviceRepositoryConstants.CUSTOM_DEVICE_DIRECTORY, deviceName, customDeviceDocument);
                }
                removeFile(archive, DeviceRepositoryConstants.STANDARD_DEVICE_DIRECTORY, false, deviceName);
                writeFile(archive, DeviceRepositoryConstants.STANDARD_DEVICE_DIRECTORY, deviceName, standardDeviceDocument);
                repositoryArchive = archive;
            }
        }
    }

    /**
     * creates an empty device document that does not contain any policies.
     * It does however contain a policies element.
     * @return A <code>Document</code> for a device
     */
    private Document createEmptyDeviceDocument() {
        Document document = createDocument(DeviceRepositorySchemaConstants.DEVICE_ELEMENT_NAME, DeviceSchemas.DEVICE_CURRENT.getNamespaceURL(), DeviceSchemas.DEVICE_CURRENT.getLocationURL());
        Element device = document.getRootElement();
        device.addContent(factory.element(DeviceRepositorySchemaConstants.POLICIES_ELEMENT_NAME, device.getNamespace()));
        return document;
    }

    /**
     * Writes out all standard properties, and all custom properties (if any)
     * as separate files within the device repository.
     * @throws RepositoryException if the properties could not be written
     */
    public void writeProperties() throws RepositoryException {
        OutputStream standardPropsOut = null;
        OutputStream customPropsOut = null;
        try {
            if (properties != null) {
                Properties customProps = new Properties();
                Properties standardProps = new Properties();
                Enumeration allPropertyNames = properties.propertyNames();
                while (allPropertyNames.hasMoreElements()) {
                    String name = (String) allPropertyNames.nextElement();
                    String value = properties.getProperty(name);
                    if (name.startsWith(DeviceRepositoryConstants.CUSTOM_POLICY_RESOURCE_PREFIX)) {
                        customProps.setProperty(name, value);
                    } else {
                        standardProps.setProperty(name, value);
                    }
                }
                ZipArchive archive = new ZipArchive(repositoryArchive);
                standardPropsOut = archive.getOutputTo(standardPropertiesPath);
                try {
                    standardProps.store(standardPropsOut, null);
                } catch (IOException e) {
                    throw new RepositoryException(e);
                }
                if (customProps.size() > 0) {
                    if (customPropertiesPath == null) {
                        customPropertiesPath = DeviceRepositoryConstants.CUSTOM_POLICIES_PROPERTIES_PREFIX + DeviceRepositoryConstants.POLICIES_PROPERTIES_SUFFIX;
                    }
                    customPropsOut = archive.getOutputTo(customPropertiesPath);
                    try {
                        customProps.store(customPropsOut, null);
                    } catch (IOException e) {
                        throw new RepositoryException(e);
                    }
                }
                repositoryArchive = archive;
            }
        } finally {
            if (standardPropsOut != null) {
                try {
                    standardPropsOut.close();
                } catch (IOException e) {
                    throw new RepositoryException(e);
                }
            }
            if (customPropsOut != null) {
                try {
                    customPropsOut.close();
                } catch (IOException e) {
                    throw new RepositoryException(e);
                }
            }
        }
    }

    /**
     * Get a device element from the device hierarchy document.
     *
     * @param deviceName the device name to get
     * @return the named device element if found, or null if not found.
     */
    public Element getHierarchyDeviceElement(String deviceName) {
        return getDeviceElement(xmlHierarchyDocument.getRootElement(), deviceName);
    }

    /**
     * Add the deviceElement as a child element of the fallback element.
     *
     * @param deviceName the name of the device that is to be added
     * @param fallback      The name of the fallback device.
     * @throws RepositoryException If the device element already exists, or is
     *                             invalid, or if the fallback is invalid (e.g.
     *                             does not exist).
     * @throws IllegalArgumentException if deviceElement is null.
     */
    public void addHierarchyDeviceElement(String deviceName, String fallback) throws RepositoryException {
        if (deviceName == null || deviceName.trim().length() == 0) {
            throw new IllegalArgumentException("deviceName cannot be null or empty.");
        }
        if (deviceExists(deviceName)) {
            throw new RepositoryException(exceptionLocalizer.format("device-already-exists", deviceName));
        }
        Element parent = null;
        if (fallback == null) {
            parent = xmlHierarchyDocument.getRootElement();
            List children = parent.getChildren();
            if ((children != null) && (children.size() > 0)) {
                throw new RepositoryException(exceptionLocalizer.format("device-hierarchy-only-one-root"));
            }
        } else {
            parent = getHierarchyDeviceElement(fallback);
        }
        if (parent != null) {
            parent.addContent(createDeviceElement(deviceName, xmlHierarchyDocument.getRootElement().getNamespace()));
        } else {
            throw new RepositoryException(exceptionLocalizer.format("device-fallback-invalid", new Object[] { fallback, deviceName }));
        }
    }

    /**
     * Creates a device element for the given device name and namespace
     * @param name the name of the device
     * @param namespace the namespace to use when creating the element
     * @return an element instance
     */
    private Element createDeviceElement(String name, Namespace namespace) {
        Element device = factory.element(DeviceRepositorySchemaConstants.DEVICE_ELEMENT_NAME, namespace);
        device.setAttribute(factory.attribute(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE, name));
        return device;
    }

    /**
     * Recursively get the element with the name specified by the deviceName
     * parameter. If no name matches, return null.
     *
     * @param parent     the parent element used in the recursive search.
     * @param deviceName the device name to search for.
     * @return the Element if a match occurs, null otherwise.
     * @throws IllegalArgumentException if parent is null.
     */
    private Element getDeviceElement(Element parent, String deviceName) {
        Element result = null;
        if (parent == null) {
            throw new IllegalArgumentException("Parent cannot be null.");
        }
        if (deviceName != null) {
            List namedChildren = parent.getChildren();
            if (namedChildren != null) {
                for (int i = 0; (result == null) && (i < namedChildren.size()); i++) {
                    Element child = (Element) namedChildren.get(i);
                    String attributeValue = child.getAttributeValue(DeviceRepositorySchemaConstants.DEVICE_NAME_ATTRIBUTE);
                    String name = child.getName();
                    if (DeviceRepositorySchemaConstants.DEVICE_ELEMENT_NAME.equals(name) && deviceName.equals(attributeValue)) {
                        result = child;
                    }
                }
                for (int i = 0; (result == null) && (i < namedChildren.size()); i++) {
                    result = getDeviceElement((Element) namedChildren.get(i), deviceName);
                }
            }
        }
        return result;
    }

    /**
     * Create an empty JDOM document with the appropriate XMLSchema attributes
     * set up.
     *
     * @param rootName the name of the root element.
     * @param namespaceURI the default namespace for the root element.
     * @param schemaURI the external URI for the schema file associated
     * with the document
     * @return the created document.
     */
    private Document createDocument(String rootName, String namespaceURI, String schemaURI) {
        Element root = factory.element(rootName, namespaceURI);
        Namespace xsi = Namespace.getNamespace(XSI, W3CSchemata.XSI_NAMESPACE);
        root.addNamespaceDeclaration(xsi);
        StringBuffer buffer = new StringBuffer(namespaceURI.length() + schemaURI.length() + 1);
        buffer.append(namespaceURI).append(' ').append(schemaURI);
        root.setAttribute(SCHEMA_LOCATION, buffer.toString(), xsi);
        return factory.document(root);
    }

    /**
     * Create a JDOM Document from an input stream.
     *
     * @param inputStream the input stream to use for building the
     * document
     * @return the created Document
     * @throws RepositoryException if the a document could not be built from
     *                             the input stream.
     */
    public Document createNewDocument(InputStream inputStream) throws RepositoryException {
        return createNewDocument(inputStream, factory, validation, ignoreWhitespace);
    }

    /**
     * Create a JDOM Document from an input stream.
     * @param inputStream the input stream to use for building the
     * document
     * @param factory the JDOMFactory used to create the Document and its
     * Elements.
     * @param validation indicate if the document to be created should be
     * parsed against its associated schema
     * @param ignoreWhitespace indicate is whitespace in the inputstream should
     * be ignored for the Document
     * @return the Document
     * @throws RepositoryException
     */
    private static Document createNewDocument(InputStream inputStream, JDOMFactory factory, boolean validation, boolean ignoreWhitespace) throws RepositoryException {
        Document result = null;
        SAXBuilder builder = new SAXBuilder() {

            protected XMLReader createParser() throws JDOMException {
                XMLReader parser = new com.volantis.xml.xerces.parsers.SAXParser();
                setFeaturesAndProperties(parser, true);
                return parser;
            }
        };
        builder.setFeature("http://xml.org/sax/features/namespaces", true);
        builder.setFactory(factory);
        builder.setXMLFilter(new DefaultNamespaceAdapterFilter(DeviceRepositoryConstants.DEFAULT_NAMESPACE_PREFIX));
        if (validation) {
            builder.setValidation(true);
            builder.setFeature("http://apache.org/xml/features/validation/" + "schema-full-checking", true);
            builder.setFeature("http://xml.org/sax/features/validation", true);
            builder.setFeature("http://apache.org/xml/features/validation/" + "schema", true);
            builder.setIgnoringElementContentWhitespace(ignoreWhitespace);
            JarFileEntityResolver resolver = new EclipseEntityResolver();
            resolver.addSystemIdMapping(DeviceSchemas.DEVICE_CURRENT);
            resolver.addSystemIdMapping(DeviceSchemas.CORE_CURRENT);
            resolver.addSystemIdMapping(DeviceSchemas.HEIRARCHY_CURRENT);
            resolver.addSystemIdMapping(DeviceSchemas.IDENTIFICATION_CURRENT);
            resolver.addSystemIdMapping(DeviceSchemas.TAC_IDENTIFICATION_CURRENT);
            resolver.addSystemIdMapping(DeviceSchemas.POLICY_DEFINITIONS_CURRENT);
            resolver.addSystemIdMapping(DeviceSchemas.REPOSITORY_CURRENT);
            builder.setEntityResolver(resolver);
        } else {
            builder.setValidation(false);
        }
        try {
            result = builder.build(inputStream);
        } catch (JDOMException e) {
            throw new RepositoryException(e);
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
        return result;
    }

    /**
     * Write a JDOM document to an output stream.
     *
     * @param document
     * @param out
     * @throws IOException
     */
    private void writeDocument(Document document, OutputStream out) throws IOException {
        XMLOutputter outputter = new XMLOutputter();
        Document currentDocument = document;
        if (xmlFilter != null) {
            SAXBuilder parser = new SAXBuilder();
            parser.setXMLFilter(xmlFilter);
            try {
                currentDocument = parser.build(new StringReader(outputter.outputString(document)));
            } catch (JDOMException e) {
                e.printStackTrace();
                throw new UndeclaredThrowableException(e, "Could not build the a filtered document.");
            }
        }
        outputter.setIndent("    ");
        outputter.setTextTrim(true);
        outputter.setNewlines(true);
        outputter.output(currentDocument, out);
        out.flush();
        out.close();
    }

    /**
     * Utility method to output the contents of this hierarchy to the console.
     */
    private void dumpHierarchy(StringBuffer buffer) {
        buffer.append("\nXML Device Repository Hierarchy\n");
        buffer.append("-------------------------------\n");
        String rootDeviceName = null;
        try {
            rootDeviceName = getRootDeviceName();
            dumpHierarchy(buffer, rootDeviceName, 0);
        } catch (RepositoryException e) {
            e.printStackTrace();
            if (logger.isDebugEnabled()) {
                logger.debug("Could not obtain root device name.", e);
            }
        }
    }

    /**
     * Recursively output the contents of this hierarchy to a buffer.
     */
    private void dumpHierarchy(StringBuffer buffer, String deviceName, int level) {
        for (int i = 0; i < level; i++) {
            buffer.append("    ");
        }
        buffer.append(deviceName).append("\n");
        List list = getChildDeviceNames(deviceName);
        ++level;
        if (list != null) {
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                String s = (String) iterator.next();
                dumpHierarchy(buffer, s, level);
            }
        }
    }

    /**
     * Return a string representation of this object.
     *
     * @return a string representation of this object.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        dumpHierarchy(buffer);
        return buffer.toString();
    }

    /**
     * Get the version of a given device repository. This method is made
     * available so that version information can be accessed without the
     * need to create a new XMLDeviceRepositoryAccessor - which reads in
     * the entire repository.
     * @param repositoryFile the full path name of the device repository
     * file whose version to query.
     * @return the version of the given repository.
     * @throws RepositoryException If there is a problem accessing the version
     * information.
     */
    public static String getRepositoryVersion(File repositoryFile) throws RepositoryException {
        return queryRepository(repositoryFile, DeviceRepositoryConstants.VERSION_FILENAME, new DeviceRepositoryQuery() {

            public String doQuery(InputStream zipEntryStream) throws RepositoryException {
                return retrieveVersion(zipEntryStream);
            }
        });
    }

    /**
     * Get the revision of a given device repository. This method is made
     * available so that version information can be accessed without the
     * need to create a new XMLDeviceRepositoryAccessor - which reads in
     * the entire repository.
     * @param repositoryFile the full path name of the device repository
     * file whose version to query.
     * @return the revision of the given repository.
     * @throws RepositoryException If there is a problem accessing the version
     * information.
     */
    public static String getRepositoryRevision(File repositoryFile) throws RepositoryException {
        return queryRepository(repositoryFile, DeviceRepositoryConstants.REPOSITORY_XML, new DeviceRepositoryQuery() {

            public String doQuery(InputStream zipEntryStream) throws RepositoryException {
                return retrieveRevision(zipEntryStream, new DefaultJDOMFactory());
            }
        });
    }

    /**
     * Generic method for querying a ZipEntry in the device repository and
     * returning the result as a String. This method uses a
     * DeviceRepositoryQuery as the executee for query (i.e. a command pattern).
     * @param repositoryFile the repository file
     * @param zipEntryName the name of the ZipEntry in the repository to query
     * @param query the DeviceRepositoryQuery
     * @return the result of the query
     * @throws RepositoryException if there is a problem running the query
     */
    private static String queryRepository(File repositoryFile, String zipEntryName, DeviceRepositoryQuery query) throws RepositoryException {
        if (!repositoryFile.canRead()) {
            throw new RepositoryException(exceptionLocalizer.format("file-cannot-be-read", repositoryFile));
        }
        InputStream is = null;
        String queryResult = null;
        try {
            ZipFile repositoryZip = new ZipFile(repositoryFile, ZipFile.OPEN_READ);
            ZipEntry versionEntry = repositoryZip.getEntry(zipEntryName);
            is = repositoryZip.getInputStream(versionEntry);
            queryResult = query.doQuery(is);
        } catch (IOException e) {
            logger.error("unexpected-ioexception", e);
            throw new RepositoryException(exceptionLocalizer.format("unexpected-ioexception"), e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("unexpected-ioexception", e);
                    if (queryResult != null) {
                        throw new RepositoryException(exceptionLocalizer.format("unexpected-ioexception"), e);
                    }
                }
            }
        }
        return queryResult;
    }

    /**
     * Get the standard policy resource prefix.
     * @return the resource prefix
     */
    public static String getStandardPolicyResourcePrefix() {
        return DeviceRepositoryConstants.STANDARD_POLICY_RESOURCE_PREFIX;
    }

    /**
     * Interface for use in a command pattern for querying individual
     * ZipEntries within a repository.
     */
    private interface DeviceRepositoryQuery {

        /**
         * Query a zip entry within a device repository.
         * @param zipEntryStream the InputStream associated with the ZipEntry
         * to be queried.
         * @return the result of the query.
         * @throws RepositoryException if there is a problem with the query.
         */
        public String doQuery(InputStream zipEntryStream) throws RepositoryException;
    }

    /**
     * Returns the pathname within the device repository of the properties file
     * with the given prefix, and populates the given properties object with
     * the properties found in this file.
     *
     * If properties for the current locale cannot be found then the default
     * non-localized properties file (i.e. English language) will be used. In
     * this latter case some debug will be logged.
     *
     * The rules for finding the locale specific properties file are to look
     * from most specific to most general.
     *
     * @param deviceRepository the device repository as a ZipArchive
     * @param prefix the prefix of the properties file of interest
     * @param properties the Properties object to populate
     * @return the path of the properties file within the device repository
     * @throws IOException if there is a problem reading the device repository
     */
    private String populateProperties(ZipArchive deviceRepository, String prefix, Properties properties) throws IOException {
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        String propertiesFile = null;
        if (language.length() != 0 && country.length() != 0 && variant.length() != 0) {
            String checkPath = getPropertiesPath(prefix, language, country, variant);
            if (deviceRepository.exists(checkPath)) {
                propertiesFile = checkPath;
            }
        }
        if (propertiesFile == null && language.length() != 0 && country.length() != 0) {
            String checkPath = getPropertiesPath(prefix, language, country, null);
            if (deviceRepository.exists(checkPath)) {
                propertiesFile = checkPath;
            }
        }
        if (propertiesFile == null && language.length() != 0) {
            String checkPath = getPropertiesPath(prefix, language, null, null);
            if (deviceRepository.exists(checkPath)) {
                propertiesFile = checkPath;
            }
        }
        if (propertiesFile == null) {
            String checkPath = getPropertiesPath(prefix, null, null, null);
            if (deviceRepository.exists(checkPath)) {
                propertiesFile = checkPath;
            }
        }
        if (propertiesFile != null) {
            properties.load(deviceRepository.getInputFrom(propertiesFile));
        } else if (logger.isDebugEnabled()) {
            logger.debug("Could not locate the properties file for the prefix " + prefix);
        }
        return propertiesFile;
    }

    /**
     * Gets the default name for the properties file using the given prefix and
     * the default locale.
     * @param prefix the file's prefix
     * @param language the locale's language can be null
     * @param country the locale's country can be null
     * @param variant the locale's variant can be null
     * @return the default pathname
     */
    private String getPropertiesPath(String prefix, String language, String country, String variant) {
        StringBuffer buffer = new StringBuffer(prefix);
        if (language != null && language.length() != 0) {
            buffer.append('_').append(language);
            if (country != null && country.length() != 0) {
                buffer.append('_').append(country);
                if (variant != null && variant.length() != 0) {
                    buffer.append('_').append(variant);
                }
            }
        }
        buffer.append(DeviceRepositoryConstants.POLICIES_PROPERTIES_SUFFIX);
        return buffer.toString();
    }

    /**
     * Creates a Properties object containing all standard and optional custom
     * properties.
     * @param deviceRepository the name of the device repository from which
     *                         to retrieve the properties
     * @return the Properties object
     * @throws IOException if the standard property file cannot be
     *                     accessed.
     */
    private Properties createMergedProperties(ZipArchive deviceRepository) throws IOException {
        Properties allProps = new Properties();
        Properties customProps = new Properties();
        standardPropertiesPath = populateProperties(deviceRepository, DeviceRepositoryConstants.STANDARD_POLICIES_PROPERTIES_PREFIX, allProps);
        if (standardPropertiesPath == null) {
            throw new IOException("Error reading standard properties file.");
        }
        customPropertiesPath = populateProperties(deviceRepository, DeviceRepositoryConstants.CUSTOM_POLICIES_PROPERTIES_PREFIX, customProps);
        if (customPropertiesPath != null) {
            Enumeration customEnum = customProps.propertyNames();
            while (customEnum.hasMoreElements()) {
                String propertyName = (String) customEnum.nextElement();
                String value = customProps.getProperty(propertyName);
                allProps.setProperty(propertyName, value);
            }
        }
        return allProps;
    }

    /**
     * Gets all standard and custom properties.
     * @return the properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Provide access to the device hierarchy document from the specified
     * device repository using XML document factory provided.
     * <p>
     * This method is static so that it can be called independently of any
     * context within this class and it does not rely on it at all.  It
     * obtains a device hierarchy document from the repository named.
     *
     * @param deviceRepository The repository from which the device hierarchy
     *                         document should extracted.
     * @param transformerMetaFactory the meta factory for creating XSL
     *      transforms, used to do automatic upgrades.
     * @param jdomFactory          A means of creating XML documents.
     *
     * @return The JDOM Document that represents the device hierarchy
     */
    public static Document getDeviceHierarchyDocument(String deviceRepository, TransformerMetaFactory transformerMetaFactory, JDOMFactory jdomFactory) throws RepositoryException {
        return getDocument(deviceRepository, transformerMetaFactory, jdomFactory, DeviceRepositoryConstants.HIERARCHY_XML);
    }

    /**
     * Provide access to the device identification document from the specified
     * device repository using XML document factory provided.
     * <p>
     * This method is static so that it can be called independently of any
     * context within this class and it does not rely on it at all.  It
     * obtains a device hierarchy document from the repository named.
     *
     * @param deviceRepository The repository from which the device
     *                         identification document should extracted.
     * @param transformerMetaFactory the meta factory for creating XSL
     *      transforms, used to do automatic upgrades.
     * @param jdomFactory          A means of creating XML documents.
     *
     * @return The JDOM Document that represents the device identification
     */
    public static Document getDeviceIdentificationDocument(String deviceRepository, TransformerMetaFactory transformerMetaFactory, JDOMFactory jdomFactory) throws RepositoryException {
        return getDocument(deviceRepository, transformerMetaFactory, jdomFactory, DeviceRepositoryConstants.IDENTIFICATION_XML);
    }

    /**
     * Provide access to the a repository document from the specified
     * device repository using XML document factory provided.
     * <p>
     * This method is static so that it can be called independently of any
     * context within this class and it does not rely on it at all.  It
     * obtains a device hierarchy document from the repository named.
     *
     * @param deviceRepository The repository from which the
     *                         document should extracted.
     * @param transformerMetaFactory the meta factory for creating XSL
     *      transforms, used to do automatic upgrades.
     * @param jdomFactory          A means of creating XML documents.
     * @param documentFile the name of the file containing the docuemnt
     * to get
     * @return The JDOM Document that represents the device identification
     */
    private static Document getDocument(String deviceRepository, TransformerMetaFactory transformerMetaFactory, JDOMFactory jdomFactory, String documentFile) throws RepositoryException {
        Document document = null;
        MDPRArchiveAccessor accessor = new MDPRArchiveAccessor(deviceRepository, transformerMetaFactory);
        InputStream input = accessor.getArchiveEntryInputStream(documentFile);
        if (input != null) {
            document = createNewDocument(new BufferedInputStream(input), jdomFactory, false, false);
        } else {
            throw new RepositoryException(exceptionLocalizer.format("device-repository-file-missing", documentFile));
        }
        return document;
    }

    /**
     * Retrieves the version of the device repository file that this accessor
     * is capable of working with.
     *
     * @return The required version of the device repository
     */
    protected static String getRequiredMDPRVersion() {
        return DeviceRepositoryConstants.VERSION;
    }
}
