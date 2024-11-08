package de.fmui.cmis.fileshare;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.fmui.cmis.fileshare.jaxb.CmisACLCapabilityType;
import de.fmui.cmis.fileshare.jaxb.CmisAccessControlEntryType;
import de.fmui.cmis.fileshare.jaxb.CmisAccessControlListType;
import de.fmui.cmis.fileshare.jaxb.CmisAccessControlPrincipalType;
import de.fmui.cmis.fileshare.jaxb.CmisAllowableActionsType;
import de.fmui.cmis.fileshare.jaxb.CmisContentStreamType;
import de.fmui.cmis.fileshare.jaxb.CmisObjectInFolderContainerType;
import de.fmui.cmis.fileshare.jaxb.CmisObjectInFolderType;
import de.fmui.cmis.fileshare.jaxb.CmisObjectListType;
import de.fmui.cmis.fileshare.jaxb.CmisObjectParentsType;
import de.fmui.cmis.fileshare.jaxb.CmisObjectType;
import de.fmui.cmis.fileshare.jaxb.CmisPermissionDefinition;
import de.fmui.cmis.fileshare.jaxb.CmisPermissionMapping;
import de.fmui.cmis.fileshare.jaxb.CmisPropertiesType;
import de.fmui.cmis.fileshare.jaxb.CmisProperty;
import de.fmui.cmis.fileshare.jaxb.CmisPropertyBoolean;
import de.fmui.cmis.fileshare.jaxb.CmisPropertyDateTime;
import de.fmui.cmis.fileshare.jaxb.CmisPropertyDefinitionType;
import de.fmui.cmis.fileshare.jaxb.CmisPropertyId;
import de.fmui.cmis.fileshare.jaxb.CmisPropertyInteger;
import de.fmui.cmis.fileshare.jaxb.CmisPropertyString;
import de.fmui.cmis.fileshare.jaxb.CmisRepositoryCapabilitiesType;
import de.fmui.cmis.fileshare.jaxb.CmisRepositoryInfoType;
import de.fmui.cmis.fileshare.jaxb.CmisTypeDefinitionType;
import de.fmui.cmis.fileshare.jaxb.EnumACLPropagation;
import de.fmui.cmis.fileshare.jaxb.EnumAllowableActionsKey;
import de.fmui.cmis.fileshare.jaxb.EnumBaseObjectTypeIds;
import de.fmui.cmis.fileshare.jaxb.EnumBasicPermissions;
import de.fmui.cmis.fileshare.jaxb.EnumCapabilityACL;
import de.fmui.cmis.fileshare.jaxb.EnumCapabilityChanges;
import de.fmui.cmis.fileshare.jaxb.EnumCapabilityContentStreamUpdates;
import de.fmui.cmis.fileshare.jaxb.EnumCapabilityJoin;
import de.fmui.cmis.fileshare.jaxb.EnumCapabilityQuery;
import de.fmui.cmis.fileshare.jaxb.EnumCapabilityRendition;
import de.fmui.cmis.fileshare.jaxb.EnumIncludeRelationships;
import de.fmui.cmis.fileshare.jaxb.EnumPropertiesBase;
import de.fmui.cmis.fileshare.jaxb.EnumPropertiesDocument;
import de.fmui.cmis.fileshare.jaxb.EnumPropertiesFolder;
import de.fmui.cmis.fileshare.jaxb.EnumServiceException;
import de.fmui.cmis.fileshare.jaxb.EnumUpdatability;
import de.fmui.cmis.fileshare.jaxb.EnumVersioningState;
import de.fmui.cmis.fileshare.jaxb.ObjectFactory;
import de.fmui.cmis.fileshare.jaxb.DeleteTreeResponse.FailedToDelete;

/**
 * Encapsulates all file system operations.
 * 
 * @author Florian Müller
 */
public class FileShareRepository implements IFileShareRepository {

    private static final String ROOT_ID = "@root@";

    private static final String SHADOW_EXT = ".cmis.xml";

    private static final String SHADOW_FOLDER = "cmis.xml";

    private static final int BUFFER_SIZE = 64 * 1024;

    private static final QName CMIS_OBJECT = new QName("http://docs.oasis-open.org/ns/cmis/core/200908/", "object");

    private static final JAXBContext JAXB_CONTEXT;

    static {
        JAXBContext jc = null;
        try {
            jc = JAXBContext.newInstance(ObjectFactory.class, ObjectObjectFactory.class);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        JAXB_CONTEXT = jc;
    }

    @XmlRegistry
    static class ObjectObjectFactory {

        @XmlElementDecl(namespace = "http://docs.oasis-open.org/ns/cmis/core/200908/", name = "object")
        public JAXBElement<CmisObjectType> createObject(CmisObjectType value) {
            return new JAXBElement<CmisObjectType>(CMIS_OBJECT, CmisObjectType.class, value);
        }
    }

    private Log log = LogFactory.getLog("de.fmui.cmis.fileshare.FileShareRepository");

    /** Repository id */
    private String fRepositoryId;

    /** Root directory */
    private File fRoot;

    /** Types */
    private ITypes fTypes;

    /** user table */
    private Map<String, Boolean> fUserMap;

    /** query handler class name */
    private String fQueryHandler;

    private CmisRepositoryInfoType fRepositoryInfo;

    /**
	 * Constructor.
	 * 
	 * @param repId
	 *            CMIS repository id
	 * @param root
	 *            root folder
	 */
    public FileShareRepository(String repId, String root, ITypes types) {
        if ((repId == null) || (repId.trim().length() == 0)) {
            throw new IllegalArgumentException("Invalid repository id!");
        }
        fRepositoryId = repId;
        if ((root == null) || (root.trim().length() == 0)) {
            throw new IllegalArgumentException("Invalid root folder!");
        }
        fRoot = new File(root);
        if (!fRoot.isDirectory()) {
            throw new IllegalArgumentException("Root is not a directory!");
        }
        fTypes = types;
        fUserMap = new HashMap<String, Boolean>();
        fQueryHandler = null;
        fRepositoryInfo = new CmisRepositoryInfoType();
        fRepositoryInfo.setRepositoryId(fRepositoryId);
        fRepositoryInfo.setRepositoryName(fRepositoryId);
        fRepositoryInfo.setRepositoryDescription(fRepositoryId);
        fRepositoryInfo.setCmisVersionSupported(Constants.CMIS_VERSION);
        fRepositoryInfo.setProductName(Constants.PRODUCT);
        fRepositoryInfo.setProductVersion(Constants.PRODUCT_VERSION);
        fRepositoryInfo.setVendorName(Constants.VENDOR);
        fRepositoryInfo.setRootFolderId(ROOT_ID);
        fRepositoryInfo.setThinClientURI("");
        CmisRepositoryCapabilitiesType capabilities = new CmisRepositoryCapabilitiesType();
        capabilities.setCapabilityACL(EnumCapabilityACL.DISCOVER);
        capabilities.setCapabilityAllVersionsSearchable(false);
        capabilities.setCapabilityJoin(EnumCapabilityJoin.NONE);
        capabilities.setCapabilityMultifiling(false);
        capabilities.setCapabilityPWCSearchable(false);
        capabilities.setCapabilityPWCUpdatable(false);
        capabilities.setCapabilityQuery(EnumCapabilityQuery.NONE);
        capabilities.setCapabilityUnfiling(false);
        capabilities.setCapabilityVersionSpecificFiling(false);
        capabilities.setCapabilityChanges(EnumCapabilityChanges.NONE);
        capabilities.setCapabilityContentStreamUpdatability(EnumCapabilityContentStreamUpdates.ANYTIME);
        capabilities.setCapabilityGetDescendants(true);
        capabilities.setCapabilityGetFolderTree(true);
        capabilities.setCapabilityMultifiling(false);
        capabilities.setCapabilityRenditions(EnumCapabilityRendition.NONE);
        capabilities.setCapabilityVersionSpecificFiling(false);
        fRepositoryInfo.setCapabilities(capabilities);
        CmisACLCapabilityType aclCapability = new CmisACLCapabilityType();
        aclCapability.setPropagation(EnumACLPropagation.OBJECTONLY);
        List<CmisPermissionDefinition> permissions = aclCapability.getPermissions();
        permissions.add(createPermission(EnumBasicPermissions.CMIS_READ.value(), "Read"));
        permissions.add(createPermission(EnumBasicPermissions.CMIS_WRITE.value(), "Write"));
        permissions.add(createPermission(EnumBasicPermissions.CMIS_ALL.value(), "All"));
        List<CmisPermissionMapping> mappings = aclCapability.getMapping();
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_CREATE_DOCUMENT_FOLDER, EnumBasicPermissions.CMIS_READ.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_CREATE_FOLDER_FOLDER, EnumBasicPermissions.CMIS_READ.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_DELETE_CONTENT_DOCUMENT, EnumBasicPermissions.CMIS_WRITE.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_DELETE_OBJECT, EnumBasicPermissions.CMIS_ALL.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_DELETE_TREE_FOLDER, EnumBasicPermissions.CMIS_ALL.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_GET_ACL_OBJECT, EnumBasicPermissions.CMIS_READ.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_GET_ALL_VERSIONS_VERSION_SERIES, EnumBasicPermissions.CMIS_READ.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_GET_CHILDREN_FOLDER, EnumBasicPermissions.CMIS_READ.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_GET_DESCENDENTS_FOLDER, EnumBasicPermissions.CMIS_READ.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_GET_FOLDER_PARENT_OBJECT, EnumBasicPermissions.CMIS_READ.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_GET_PARENTS_FOLDER, EnumBasicPermissions.CMIS_READ.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_GET_PROPERTIES_OBJECT, EnumBasicPermissions.CMIS_READ.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_MOVE_OBJECT, EnumBasicPermissions.CMIS_WRITE.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_MOVE_SOURCE, EnumBasicPermissions.CMIS_READ.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_MOVE_TARGET, EnumBasicPermissions.CMIS_WRITE.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_SET_CONTENT_DOCUMENT, EnumBasicPermissions.CMIS_WRITE.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_UPDATE_PROPERTIES_OBJECT, EnumBasicPermissions.CMIS_WRITE.value()));
        mappings.add(createMapping(EnumAllowableActionsKey.CAN_VIEW_CONTENT_OBJECT, EnumBasicPermissions.CMIS_READ.value()));
        fRepositoryInfo.setAclCapability(aclCapability);
    }

    private CmisPermissionDefinition createPermission(String permission, String description) {
        CmisPermissionDefinition pd = new CmisPermissionDefinition();
        pd.setPermission(permission);
        pd.setDescription(description);
        return pd;
    }

    private CmisPermissionMapping createMapping(EnumAllowableActionsKey key, String permission) {
        CmisPermissionMapping pm = new CmisPermissionMapping();
        pm.setKey(key);
        pm.getPermission().add(permission);
        return pm;
    }

    public void addUser(String user, boolean readOnly) {
        if ((user == null) || (user.length() == 0)) {
            return;
        }
        fUserMap.put(user, readOnly);
    }

    public void setQueryHandler(String queryHandler) {
        fQueryHandler = queryHandler;
    }

    public String getRepositoryId(CallContext context) {
        return fRepositoryId;
    }

    public CmisRepositoryInfoType getRepositoryInfo(CallContext context) {
        return fRepositoryInfo;
    }

    public boolean isRootFolder(String id) {
        return ROOT_ID.equals(id);
    }

    public String createFile(CallContext context, String typeId, String parentId, CmisPropertiesType properties, CmisContentStreamType content, EnumVersioningState versioningState) throws CMISFileShareException {
        debug("createFile");
        checkUser(context, true);
        if (EnumVersioningState.NONE != versioningState) {
            throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Versioning not supported!", 0);
        }
        CmisTypeDefinitionType type = fTypes.getTypeDefinition(context, typeId);
        if (type == null) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Type '" + typeId + "' is unknown!", 0);
        }
        CmisPropertiesType props = compileProperties(typeId, context.getUser(), System.currentTimeMillis(), context.getUser(), properties);
        String name = Util.getFirstStringValue(properties, EnumPropertiesBase.CMIS_NAME.value());
        if (!isValidName(name)) {
            throw new CMISFileShareException(EnumServiceException.NAME_CONSTRAINT_VIOLATION, "Name is not valid.", 0);
        }
        File parent;
        try {
            parent = idToFile(parentId);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        if (!parent.isDirectory()) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Parent is not a folder.", 0);
        }
        File newFile = new File(parent, name);
        if (newFile.exists()) {
            throw new CMISFileShareException(EnumServiceException.NAME_CONSTRAINT_VIOLATION, "Document already exists.", 0);
        }
        try {
            newFile.createNewFile();
        } catch (IOException e) {
            throw new CMISFileShareException(EnumServiceException.STORAGE, "Could not create file: " + e.getMessage(), 0, e);
        }
        if ((content != null) && (content.getStream() != null)) {
            try {
                OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile), BUFFER_SIZE);
                InputStream in = new BufferedInputStream(content.getStream().getInputStream(), BUFFER_SIZE);
                byte[] buffer = new byte[BUFFER_SIZE];
                int b;
                while ((b = in.read(buffer)) > -1) {
                    out.write(buffer, 0, b);
                }
                out.flush();
                out.close();
                in.close();
            } catch (Exception e) {
                throw new CMISFileShareException(EnumServiceException.STORAGE, "Could not write content: " + e.getMessage(), 0, e);
            }
        }
        writePropertiesFile(newFile, props);
        try {
            return fileToId(newFile);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.RUNTIME, e.getMessage(), 0);
        }
    }

    public String createFileFromSource(CallContext context, String parentId, String sourceId, CmisPropertiesType properties, EnumVersioningState versioningState) throws CMISFileShareException {
        if (EnumVersioningState.NONE != versioningState) {
            throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Versioning not supported!", 0);
        }
        File parent;
        try {
            parent = idToFile(parentId);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        if (!parent.isDirectory()) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Parent is not a folder.", 0);
        }
        File source;
        try {
            source = idToFile(sourceId);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        if (!source.isFile()) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Source is not a document.", 0);
        }
        String name = source.getName();
        CmisPropertiesType sourceProperties = new CmisPropertiesType();
        readCustomProperties(source, sourceProperties, null);
        String typeId = Util.getFirstIdValue(sourceProperties, EnumPropertiesBase.CMIS_OBJECT_TYPE_ID.value());
        if (typeId == null) {
            typeId = ITypes.DOCUMENT_TYPE_ID;
        }
        CmisPropertiesType props = new CmisPropertiesType();
        for (CmisProperty prop : sourceProperties.getProperty()) {
            if ((prop.getPropertyDefinitionId().equals(EnumPropertiesBase.CMIS_OBJECT_TYPE_ID.value())) || (prop.getPropertyDefinitionId().equals(EnumPropertiesBase.CMIS_CREATED_BY.value())) || (prop.getPropertyDefinitionId().equals(EnumPropertiesBase.CMIS_CREATION_DATE.value())) || (prop.getPropertyDefinitionId().equals(EnumPropertiesBase.CMIS_LAST_MODIFIED_BY.value()))) {
                continue;
            }
            props.getProperty().add(prop);
        }
        if (properties != null) {
            String newName = Util.getFirstStringValue(properties, EnumPropertiesBase.CMIS_NAME.value());
            if (newName != null) {
                if (!isValidName(newName)) {
                    throw new CMISFileShareException(EnumServiceException.NAME_CONSTRAINT_VIOLATION, "Name is not valid.", 0);
                }
                name = newName;
            }
            Map<String, CmisPropertyDefinitionType> propDefs = fTypes.getPropertyDefinitions(typeId);
            if (propDefs == null) {
                throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Type '" + typeId + "' is unknown!", 0);
            }
            for (CmisProperty prop : properties.getProperty()) {
                CmisPropertyDefinitionType propType = propDefs.get(prop.getPropertyDefinitionId());
                if (propType == null) {
                    throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Property '" + prop.getPropertyDefinitionId() + "' is unknown!", 0);
                }
                if ((propType.getUpdatability() != EnumUpdatability.READWRITE)) {
                    throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Property '" + prop.getPropertyDefinitionId() + "' cannot be updated!", 0);
                }
                if (isEmptyProperty(prop)) {
                    throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Property '" + prop.getPropertyDefinitionId() + "' must not be empty!", 0);
                }
                replaceProperty(props, prop);
            }
        }
        addPropertyId(props, typeId, null, EnumPropertiesBase.CMIS_OBJECT_TYPE_ID.value(), typeId);
        addPropertyString(props, typeId, null, EnumPropertiesBase.CMIS_CREATED_BY.value(), context.getUser());
        addPropertyDateTime(props, typeId, null, EnumPropertiesBase.CMIS_CREATION_DATE.value(), System.currentTimeMillis());
        addPropertyString(props, typeId, null, EnumPropertiesBase.CMIS_LAST_MODIFIED_BY.value(), context.getUser());
        File newFile = new File(parent, name);
        if (newFile.exists()) {
            throw new CMISFileShareException(EnumServiceException.NAME_CONSTRAINT_VIOLATION, "Document already exists.", 0);
        }
        try {
            newFile.createNewFile();
        } catch (IOException e) {
            throw new CMISFileShareException(EnumServiceException.STORAGE, "Could not create file: " + e.getMessage(), 0, e);
        }
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(newFile));
            InputStream in = new BufferedInputStream(new FileInputStream(source));
            byte[] buffer = new byte[BUFFER_SIZE];
            int b;
            while ((b = in.read(buffer)) > -1) {
                out.write(buffer, 0, b);
            }
            out.flush();
            out.close();
            in.close();
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.STORAGE, "Could not roead or write content: " + e.getMessage(), 0, e);
        }
        writePropertiesFile(newFile, props);
        try {
            return fileToId(newFile);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.RUNTIME, e.getMessage(), 0);
        }
    }

    public String createFolder(CallContext context, String typeId, String parentId, CmisPropertiesType properties) throws CMISFileShareException {
        debug("createFolder");
        checkUser(context, true);
        CmisTypeDefinitionType type = fTypes.getTypeDefinition(context, typeId);
        if (type == null) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Type '" + typeId + "' is unknown!", 0);
        }
        CmisPropertiesType props = compileProperties(typeId, context.getUser(), System.currentTimeMillis(), context.getUser(), properties);
        String name = Util.getFirstStringValue(properties, EnumPropertiesBase.CMIS_NAME.value());
        if (!isValidName(name)) {
            throw new CMISFileShareException(EnumServiceException.NAME_CONSTRAINT_VIOLATION, "Name is not valid.", 0);
        }
        File parent;
        try {
            parent = idToFile(parentId);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        if (!parent.isDirectory()) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Parent is not a folder.", 0);
        }
        File newFolder = new File(parent, name);
        if (!newFolder.mkdir()) {
            throw new CMISFileShareException(EnumServiceException.STORAGE, "Could not create folder.", 0);
        }
        writePropertiesFile(newFolder, props);
        try {
            return fileToId(newFolder);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.RUNTIME, e.getMessage(), 0);
        }
    }

    public String move(CallContext context, String id, String newParentId) throws CMISFileShareException {
        debug("move");
        checkUser(context, true);
        File file;
        try {
            file = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        File parent;
        try {
            parent = idToFile(newParentId);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        File newFile = new File(parent, file.getName());
        if (newFile.exists()) {
            throw new CMISFileShareException(EnumServiceException.STORAGE, "Object already exists!", 0, null);
        }
        if (!file.renameTo(newFile)) {
            throw new CMISFileShareException(EnumServiceException.STORAGE, "Move failed!", 0, null);
        } else {
            if (newFile.isFile()) {
                File propFile = getPropertiesFile(file);
                if (propFile.exists()) {
                    File newPropFile = new File(parent, propFile.getName());
                    propFile.renameTo(newPropFile);
                }
            }
        }
        try {
            return fileToId(newFile);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.RUNTIME, e.getMessage(), 0);
        }
    }

    public void setContentStream(CallContext context, String id, boolean overwriteFlag, CmisContentStreamType content) throws CMISFileShareException {
        debug("setContentStream");
        checkUser(context, true);
        File file;
        try {
            file = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        if (!file.isFile()) {
            throw new CMISFileShareException(EnumServiceException.STREAM_NOT_SUPPORTED, "Not a file.", 0);
        }
        if (!overwriteFlag && file.length() > 0) {
            throw new CMISFileShareException(EnumServiceException.CONTENT_ALREADY_EXISTS, "Content already exists.", 0, null);
        }
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(file), BUFFER_SIZE);
            if ((content == null) || (content.getStream() == null)) {
                out.write(new byte[0]);
            } else {
                InputStream in = new BufferedInputStream(content.getStream().getInputStream(), BUFFER_SIZE);
                byte[] buffer = new byte[BUFFER_SIZE];
                int b;
                while ((b = in.read(buffer)) > -1) {
                    out.write(buffer, 0, b);
                }
                in.close();
            }
            out.close();
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.STORAGE, "Could not write content: " + e.getMessage(), 0, e);
        }
    }

    public FailedToDelete delete(CallContext context, String id, boolean deep, boolean continueOnFail) throws CMISFileShareException {
        debug("delete");
        checkUser(context, true);
        File file;
        try {
            file = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        FailedToDelete result = new FailedToDelete();
        try {
            if (file.isDirectory() && deep) {
                deleteFolder(file, continueOnFail, result);
            } else {
                if (!isFolderEmpty(file)) {
                    throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Folder is not empty!", 0);
                }
                getPropertiesFile(file).delete();
                if (!file.delete()) {
                    result.getObjectIds().add(fileToId(file));
                }
            }
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.RUNTIME, e.getMessage(), 0);
        }
        return result;
    }

    public String updateProperties(CallContext context, String id, CmisPropertiesType properties) throws CMISFileShareException {
        debug("updateProperties");
        checkUser(context, true);
        File file;
        try {
            file = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        String newName = Util.getFirstStringValue(properties, EnumPropertiesBase.CMIS_NAME.value());
        boolean isRename = (newName != null) && (!file.getName().equals(newName));
        if (isRename && !isValidName(newName)) {
            throw new CMISFileShareException(EnumServiceException.NAME_CONSTRAINT_VIOLATION, "Name is not valid.", 0);
        }
        CmisPropertiesType oldProperties = new CmisPropertiesType();
        readCustomProperties(file, oldProperties, null);
        String typeId = Util.getFirstIdValue(oldProperties, EnumPropertiesBase.CMIS_OBJECT_TYPE_ID.value());
        if (typeId == null) {
            typeId = (file.isDirectory() ? ITypes.FOLDER_TYPE_ID : ITypes.DOCUMENT_TYPE_ID);
        }
        String creator = Util.getFirstStringValue(oldProperties, EnumPropertiesBase.CMIS_CREATED_BY.value());
        if (creator == null) {
            creator = context.getUser();
        }
        long creationDate = Util.getFirstDateTimeValue(oldProperties, EnumPropertiesBase.CMIS_CREATION_DATE.value());
        if (creationDate < 0) {
            creationDate = file.lastModified();
        }
        CmisPropertiesType props = updateProperties(typeId, creator, creationDate, context.getUser(), oldProperties, properties);
        writePropertiesFile(file, props);
        File newFile = file;
        if (isRename) {
            File parent = file.getParentFile();
            File propFile = getPropertiesFile(file);
            newFile = new File(parent, newName);
            if (!file.renameTo(newFile)) {
                throw new CMISFileShareException(EnumServiceException.UPDATE_CONFLICT, "Could not rename object!", 0);
            } else {
                if (newFile.isFile()) {
                    if (propFile.exists()) {
                        File newPropFile = new File(parent, newName + SHADOW_EXT);
                        propFile.renameTo(newPropFile);
                    }
                }
            }
        }
        try {
            return fileToId(newFile);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.RUNTIME, e.getMessage(), 0);
        }
    }

    public CmisObjectType getObject(CallContext context, String id, String filter, boolean includeAllowableActions, boolean includeACL) throws CMISFileShareException {
        debug("getObject");
        boolean userReadOnly = checkUser(context, false);
        File file;
        try {
            file = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        Set<String> filterCollection = Util.splitFilter(filter);
        return compileObjectType(file, filterCollection, includeAllowableActions, includeACL, userReadOnly);
    }

    public String getPathSegment(String id) throws CMISFileShareException {
        File file;
        try {
            file = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        if (fRoot.equals(file)) {
            return "/";
        }
        return file.getName();
    }

    public CmisAllowableActionsType getAllowableActions(CallContext context, String id) throws CMISFileShareException {
        debug("getAllowableActions");
        boolean userReadOnly = checkUser(context, false);
        File file;
        try {
            file = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        return compileAllowableActions(file, userReadOnly);
    }

    public CmisAccessControlListType getACL(CallContext context, String id) throws CMISFileShareException {
        debug("getACL");
        checkUser(context, false);
        File file;
        try {
            file = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        return compileACL(file);
    }

    public CmisContentStreamType getContent(CallContext context, String id) throws CMISFileShareException {
        debug("getContent");
        checkUser(context, false);
        final File file;
        try {
            file = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        if (!file.isFile()) {
            throw new CMISFileShareException(EnumServiceException.STREAM_NOT_SUPPORTED, "Not a file.", 0);
        }
        final String mimeType = MIMETypes.getMIMEType(file);
        CmisContentStreamType result = new CmisContentStreamType();
        result.setFilename(file.getName());
        result.setLength(BigInteger.valueOf(file.length()));
        result.setMimeType(mimeType);
        result.setStream(new DataHandler(new DataSource() {

            public OutputStream getOutputStream() throws IOException {
                return null;
            }

            public String getName() {
                return file.getName();
            }

            public InputStream getInputStream() throws IOException {
                return new BufferedInputStream(new FileInputStream(file), 4 * 1024);
            }

            public String getContentType() {
                return mimeType;
            }
        }));
        return result;
    }

    public List<CmisObjectInFolderContainerType> getDescendants(CallContext context, String id, boolean foldersOnly, int depth, String filter, boolean includeAllowableActions, boolean includePathSegments, String orderBy, int max) throws CMISFileShareException {
        debug("getDescendants");
        boolean userReadOnly = checkUser(context, false);
        if (depth == 0) {
            throw new CMISFileShareException(EnumServiceException.INVALID_ARGUMENT, "Depth must not be 0!", 0, null);
        }
        File folder;
        try {
            folder = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        if (!folder.isDirectory()) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Not a folder.", 0);
        }
        Set<String> filterCollection = Util.splitFilter(filter);
        List<CmisObjectInFolderContainerType> result = new ArrayList<CmisObjectInFolderContainerType>();
        gatherDescendants(folder, result, foldersOnly, depth, filterCollection, includeAllowableActions, includePathSegments, max, userReadOnly);
        return result;
    }

    public CmisObjectParentsType getParent(CallContext context, String id, String filter, boolean includeAllowableActions, boolean includeRelativePathSegments) throws CMISFileShareException {
        debug("getParent");
        boolean userReadOnly = checkUser(context, false);
        File file;
        try {
            file = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        Set<String> filterCollection = Util.splitFilter(filter);
        if (fRoot.equals(file)) {
            return null;
        }
        File parent = file.getParentFile();
        CmisObjectType object = compileObjectType(parent, filterCollection, includeAllowableActions, false, userReadOnly);
        CmisObjectParentsType result = new CmisObjectParentsType();
        result.setObject(object);
        result.setRelativePathSegment(file.getName());
        return result;
    }

    public String getParentId(CallContext context, String id) throws CMISFileShareException {
        debug("getParentId");
        checkUser(context, false);
        File file;
        try {
            file = idToFile(id);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, e.getMessage(), 0, e);
        }
        if (fRoot.equals(file)) {
            return null;
        }
        if (fRoot.equals(file.getParent())) {
            return ROOT_ID;
        }
        try {
            return fileToId(file.getParentFile());
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.RUNTIME, e.getMessage(), 0);
        }
    }

    public CmisObjectType getObjectByPath(CallContext context, String folderPath, String filter, boolean includeAllowableActions, boolean includeACL) throws CMISFileShareException {
        debug("getObjectByPath");
        boolean userReadOnly = checkUser(context, false);
        Set<String> filterCollection = Util.splitFilter(filter);
        if ((folderPath == null) || (!folderPath.startsWith("/"))) {
            throw new CMISFileShareException(EnumServiceException.INVALID_ARGUMENT, "Invalid folder path.", 0);
        }
        File file = null;
        if (folderPath.length() == 1) {
            file = fRoot;
        } else {
            String path = folderPath.replace('/', File.separatorChar).substring(1);
            file = new File(fRoot, path);
        }
        if (!file.exists()) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Path doesn't exist.", 0);
        }
        return compileObjectType(file, filterCollection, includeAllowableActions, includeACL, userReadOnly);
    }

    public CmisObjectListType query(CallContext context, String statement, Boolean searchAllVersions, Boolean includeAllowableActions, EnumIncludeRelationships includeRelationships, String renditionFilter, BigInteger maxItems, BigInteger skipCount) throws CMISFileShareException {
        debug("query");
        if (fQueryHandler == null) {
            throw new CMISFileShareException(EnumServiceException.NOT_SUPPORTED, "Query not supported!", 0);
        }
        try {
            IQueryHandler queryHandlerInstance = (IQueryHandler) Class.forName(fQueryHandler).newInstance();
            return queryHandlerInstance.query(context, statement, searchAllVersions, includeAllowableActions, includeRelationships, renditionFilter, maxItems, skipCount);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.RUNTIME, e.getMessage(), 0, e);
        }
    }

    /**
	 * Gather the children of a folder.
	 */
    private void gatherDescendants(File folder, List<CmisObjectInFolderContainerType> list, boolean foldersOnly, int depth, Set<String> filter, boolean includeAllowableActions, boolean includePathSegments, int max, boolean userReadOnly) throws CMISFileShareException {
        for (File child : folder.listFiles()) {
            if (child.isHidden() || child.getName().equals(SHADOW_FOLDER) || child.getPath().endsWith(SHADOW_EXT)) {
                continue;
            }
            if (foldersOnly && !child.isDirectory()) {
                continue;
            }
            CmisObjectType obj = compileObjectType(child, filter, includeAllowableActions, false, userReadOnly);
            CmisObjectInFolderType objInFolder = new CmisObjectInFolderType();
            objInFolder.setObject(obj);
            if (includePathSegments) {
                objInFolder.setPathSegment(child.getName());
            }
            CmisObjectInFolderContainerType container = new CmisObjectInFolderContainerType();
            container.setObjectInFolder(objInFolder);
            list.add(container);
            if ((max > -1) && (list.size() == max)) {
                return;
            }
            if ((depth != 1) && child.isDirectory()) {
                gatherDescendants(child, container.getChildren(), foldersOnly, depth - 1, filter, includeAllowableActions, includePathSegments, max, userReadOnly);
            }
        }
    }

    /**
	 * Removes a folder and its content.
	 * 
	 * @throws
	 */
    private boolean deleteFolder(File folder, boolean continueOnFail, FailedToDelete ftd) throws Exception {
        boolean success = true;
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                if (!deleteFolder(file, continueOnFail, ftd)) {
                    if (!continueOnFail) {
                        return false;
                    }
                    success = false;
                }
            } else {
                if (!file.delete()) {
                    ftd.getObjectIds().add(fileToId(file));
                    if (!continueOnFail) {
                        return false;
                    }
                    success = false;
                }
            }
        }
        if (!folder.delete()) {
            ftd.getObjectIds().add(fileToId(folder));
            success = false;
        }
        return success;
    }

    /**
	 * Checks if the given name is valid for a file system.
	 * 
	 * @param name
	 *            the name to check
	 * 
	 * @return <code>true</code> if the name is valid, <code>false</code>
	 *         otherwise
	 */
    private boolean isValidName(String name) {
        if ((name == null) || (name.length() == 0) || (name.indexOf(File.separatorChar) != -1) || (name.indexOf(File.pathSeparatorChar) != -1)) {
            return false;
        }
        return true;
    }

    /**
	 * Checks if a folder is empty. A folder is considered as empty if no files
	 * or only the shadow file reside in the folder.
	 * 
	 * @param folder
	 *            the folder
	 * 
	 * @return <code>true</code> if the folder is empty.
	 */
    private boolean isFolderEmpty(File folder) {
        if (!folder.isDirectory()) {
            return true;
        }
        String[] fileNames = folder.list();
        if ((fileNames == null) || (fileNames.length == 0)) {
            return true;
        }
        if ((fileNames.length == 1) && (fileNames[0].equals(SHADOW_FOLDER))) {
            return true;
        }
        return false;
    }

    /**
	 * Compiles an object type object from a file or folder.�
	 */
    private CmisObjectType compileObjectType(File file, Set<String> filter, boolean includeAllowableActions, boolean includeACL, boolean userReadOnly) throws CMISFileShareException {
        CmisObjectType result = new CmisObjectType();
        result.setProperties(compileProperties(file, filter));
        if (includeAllowableActions) {
            result.setAllowableActions(compileAllowableActions(file, userReadOnly));
        }
        if (includeACL) {
            result.setAcl(compileACL(file));
            result.setExactACL(true);
        }
        return result;
    }

    /**
	 * Gathers all base properties of a file or folder.
	 * 
	 * @param file
	 *            the file or folder
	 * 
	 * @return
	 * @throws CMISFileShareException
	 */
    private CmisPropertiesType compileProperties(File file, Set<String> orgfilter) throws CMISFileShareException {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null!");
        }
        if (!file.exists()) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Object not found!", 0);
        }
        Set<String> filter = (orgfilter == null ? null : new HashSet<String>(orgfilter));
        String typeId = (file.isDirectory() ? ITypes.FOLDER_TYPE_ID : ITypes.DOCUMENT_TYPE_ID);
        try {
            CmisPropertiesType result = new CmisPropertiesType();
            addPropertyId(result, typeId, filter, EnumPropertiesBase.CMIS_OBJECT_ID.value(), fileToId(file));
            addPropertyString(result, typeId, filter, EnumPropertiesBase.CMIS_NAME.value(), file.getName());
            addPropertyString(result, typeId, filter, EnumPropertiesBase.CMIS_CREATED_BY.value(), "<unknown>");
            addPropertyString(result, typeId, filter, EnumPropertiesBase.CMIS_LAST_MODIFIED_BY.value(), "<unknown>");
            addPropertyDateTime(result, typeId, filter, EnumPropertiesBase.CMIS_CREATION_DATE.value(), file.lastModified());
            addPropertyDateTime(result, typeId, filter, EnumPropertiesBase.CMIS_LAST_MODIFICATION_DATE.value(), file.lastModified());
            if (file.isDirectory()) {
                addPropertyId(result, typeId, filter, EnumPropertiesBase.CMIS_BASE_TYPE_ID.value(), EnumBaseObjectTypeIds.CMIS_FOLDER.value());
                addPropertyId(result, typeId, filter, EnumPropertiesBase.CMIS_OBJECT_TYPE_ID.value(), ITypes.FOLDER_TYPE_ID);
                String path = getRepositoryPath(file);
                addPropertyString(result, typeId, filter, EnumPropertiesFolder.CMIS_PATH.value(), (path.length() == 0 ? "/" : path));
                if (!fRoot.equals(file)) {
                    addPropertyId(result, typeId, filter, EnumPropertiesFolder.CMIS_PARENT_ID.value(), (fRoot.equals(file.getParentFile()) ? ROOT_ID : fileToId(file.getParentFile())));
                }
            } else {
                addPropertyId(result, typeId, filter, EnumPropertiesBase.CMIS_BASE_TYPE_ID.value(), EnumBaseObjectTypeIds.CMIS_DOCUMENT.value());
                addPropertyId(result, typeId, filter, EnumPropertiesBase.CMIS_OBJECT_TYPE_ID.value(), ITypes.DOCUMENT_TYPE_ID);
                addPropertyBoolean(result, typeId, filter, EnumPropertiesDocument.CMIS_IS_IMMUTABLE.value(), false);
                addPropertyBoolean(result, typeId, filter, EnumPropertiesDocument.CMIS_IS_LATEST_VERSION.value(), true);
                addPropertyBoolean(result, typeId, filter, EnumPropertiesDocument.CMIS_IS_MAJOR_VERSION.value(), true);
                addPropertyBoolean(result, typeId, filter, EnumPropertiesDocument.CMIS_IS_LATEST_MAJOR_VERSION.value(), true);
                addPropertyString(result, typeId, filter, EnumPropertiesDocument.CMIS_VERSION_LABEL.value(), file.getName());
                addPropertyId(result, typeId, filter, EnumPropertiesDocument.CMIS_VERSION_SERIES_ID.value(), fileToId(file));
                addPropertyString(result, typeId, filter, EnumPropertiesDocument.CMIS_CHECKIN_COMMENT.value(), "");
                addPropertyInteger(result, typeId, filter, EnumPropertiesDocument.CMIS_CONTENT_STREAM_LENGTH.value(), file.length());
                addPropertyString(result, typeId, filter, EnumPropertiesDocument.CMIS_CONTENT_STREAM_MIME_TYPE.value(), MIMETypes.getMIMEType(file));
                addPropertyString(result, typeId, filter, EnumPropertiesDocument.CMIS_CONTENT_STREAM_FILE_NAME.value(), file.getName());
            }
            readCustomProperties(file, result, filter);
            if (filter != null) {
                if (!filter.isEmpty()) {
                    debug("Unknown filter properties: " + filter.toString(), null);
                }
            }
            return result;
        } catch (CMISFileShareException fse) {
            throw fse;
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.RUNTIME, e.getMessage(), 0, e);
        }
    }

    /**
	 * Reads and adds properties.
	 */
    private void readCustomProperties(File file, CmisPropertiesType properties, Set<String> filter) {
        File propFile = getPropertiesFile(file);
        if (!propFile.exists()) {
            return;
        }
        JAXBElement<CmisObjectType> obj = null;
        try {
            Unmarshaller u = JAXB_CONTEXT.createUnmarshaller();
            obj = (JAXBElement<CmisObjectType>) u.unmarshal(propFile);
        } catch (Exception e) {
            warn("Unvalid CMIS properties: " + propFile.getAbsolutePath(), e);
        }
        if ((obj == null) || (obj.getValue() == null) || (obj.getValue().getProperties() == null)) {
            return;
        }
        for (CmisProperty prop : obj.getValue().getProperties().getProperty()) {
            if (filter != null) {
                if (!filter.contains(prop.getPropertyDefinitionId())) {
                    continue;
                } else {
                    filter.remove(prop.getPropertyDefinitionId());
                }
            }
            if (EnumPropertiesBase.CMIS_OBJECT_ID.value().equals(prop.getPropertyDefinitionId())) {
                continue;
            }
            if (EnumPropertiesBase.CMIS_BASE_TYPE_ID.value().equals(prop.getPropertyDefinitionId())) {
                continue;
            }
            replaceProperty(properties, prop);
        }
    }

    /**
	 * Checks and compiles a property set that can be written to disc.
	 */
    private CmisPropertiesType compileProperties(String typeId, String creator, long creationDate, String modifier, CmisPropertiesType properties) throws CMISFileShareException {
        CmisPropertiesType result = new CmisPropertiesType();
        Set<String> addedProps = new HashSet<String>();
        if (properties == null) {
            throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "No properties!", 0);
        }
        Map<String, CmisPropertyDefinitionType> propDefs = fTypes.getPropertyDefinitions(typeId);
        if (propDefs == null) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Type '" + typeId + "' is unknown!", 0);
        }
        for (CmisProperty prop : properties.getProperty()) {
            CmisPropertyDefinitionType propType = propDefs.get(prop.getPropertyDefinitionId());
            if (propType == null) {
                throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Property '" + prop.getPropertyDefinitionId() + "' is unknown!", 0);
            }
            if (propType.getId().equals(EnumPropertiesBase.CMIS_OBJECT_TYPE_ID.value())) {
                continue;
            }
            if ((propType.getUpdatability() == EnumUpdatability.READONLY)) {
                throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Property '" + prop.getPropertyDefinitionId() + "' is readonly!", 0);
            }
            if (isEmptyProperty(prop)) {
                throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Property '" + prop.getPropertyDefinitionId() + "' must not be empty!", 0);
            }
            result.getProperty().add(prop);
            addedProps.add(prop.getPropertyDefinitionId());
        }
        for (CmisPropertyDefinitionType propDef : propDefs.values()) {
            if (!addedProps.contains(propDef.getId()) && (propDef.getUpdatability() != EnumUpdatability.READONLY)) {
                if (!addPropertyDefault(result, propDef) && propDef.isRequired()) {
                    throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Property '" + propDef.getId() + "' is required!", 0);
                }
            }
        }
        addPropertyId(result, typeId, null, EnumPropertiesBase.CMIS_OBJECT_TYPE_ID.value(), typeId);
        addPropertyString(result, typeId, null, EnumPropertiesBase.CMIS_CREATED_BY.value(), creator);
        addPropertyDateTime(result, typeId, null, EnumPropertiesBase.CMIS_CREATION_DATE.value(), creationDate);
        addPropertyString(result, typeId, null, EnumPropertiesBase.CMIS_LAST_MODIFIED_BY.value(), modifier);
        return result;
    }

    /**
	 * Checks and updates a property set that can be written to disc.
	 */
    private CmisPropertiesType updateProperties(String typeId, String creator, long creationDate, String modifier, CmisPropertiesType oldProperties, CmisPropertiesType properties) throws CMISFileShareException {
        CmisPropertiesType result = new CmisPropertiesType();
        if (properties == null) {
            throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "No properties!", 0);
        }
        Map<String, CmisPropertyDefinitionType> propDefs = fTypes.getPropertyDefinitions(typeId);
        if (propDefs == null) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Type '" + typeId + "' is unknown!", 0);
        }
        for (CmisProperty prop : oldProperties.getProperty()) {
            CmisPropertyDefinitionType propType = propDefs.get(prop.getPropertyDefinitionId());
            if (propType == null) {
                throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Property '" + prop.getPropertyDefinitionId() + "' is unknown!", 0);
            }
            if ((propType.getUpdatability() != EnumUpdatability.READWRITE)) {
                continue;
            }
            result.getProperty().add(prop);
        }
        for (CmisProperty prop : properties.getProperty()) {
            CmisPropertyDefinitionType propType = propDefs.get(prop.getPropertyDefinitionId());
            if (propType == null) {
                throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Property '" + prop.getPropertyDefinitionId() + "' is unknown!", 0);
            }
            if ((propType.getUpdatability() == EnumUpdatability.READONLY)) {
                throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Property '" + prop.getPropertyDefinitionId() + "' is readonly!", 0);
            }
            if ((propType.getUpdatability() == EnumUpdatability.ONCREATE)) {
                throw new CMISFileShareException(EnumServiceException.CONSTRAINT, "Property '" + prop.getPropertyDefinitionId() + "' can only be set on create!", 0);
            }
            if (isEmptyProperty(prop)) {
                deleteProperty(result, prop.getPropertyDefinitionId());
                addPropertyDefault(result, propType);
            } else {
                replaceProperty(result, prop);
            }
        }
        addPropertyId(result, typeId, null, EnumPropertiesBase.CMIS_OBJECT_TYPE_ID.value(), typeId);
        addPropertyString(result, typeId, null, EnumPropertiesBase.CMIS_CREATED_BY.value(), creator);
        addPropertyDateTime(result, typeId, null, EnumPropertiesBase.CMIS_CREATION_DATE.value(), creationDate);
        addPropertyString(result, typeId, null, EnumPropertiesBase.CMIS_LAST_MODIFIED_BY.value(), modifier);
        return result;
    }

    private void replaceProperty(CmisPropertiesType properties, CmisProperty newProperty) {
        int i = 0;
        for (CmisProperty prop : properties.getProperty()) {
            if (prop.getPropertyDefinitionId().equals(newProperty.getPropertyDefinitionId())) {
                properties.getProperty().set(i, newProperty);
                return;
            }
            i++;
        }
        properties.getProperty().add(newProperty);
    }

    private void deleteProperty(CmisPropertiesType properties, String propId) {
        int i = 0;
        for (CmisProperty prop : properties.getProperty()) {
            if (prop.getPropertyDefinitionId().equals(propId)) {
                properties.getProperty().remove(i);
                return;
            }
            i++;
        }
    }

    private boolean isEmptyProperty(CmisProperty prop) {
        if (prop == null) {
            return true;
        }
        try {
            Method m = prop.getClass().getMethod("getValue", new Class[0]);
            List list = (List) m.invoke(prop, new Object[0]);
            return list.isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    private void addPropertyId(CmisPropertiesType props, String typeId, Set<String> filter, String id, String value) {
        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null!");
        }
        CmisPropertyId prop = new CmisPropertyId();
        prop.setPropertyDefinitionId(id);
        prop.getValue().add(value);
        props.getProperty().add(prop);
    }

    private void addPropertyString(CmisPropertiesType props, String typeId, Set<String> filter, String id, String value) {
        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }
        CmisPropertyString prop = new CmisPropertyString();
        prop.setPropertyDefinitionId(id);
        prop.getValue().add(value);
        props.getProperty().add(prop);
    }

    private void addPropertyInteger(CmisPropertiesType props, String typeId, Set<String> filter, String id, long value) {
        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }
        CmisPropertyInteger prop = new CmisPropertyInteger();
        prop.setPropertyDefinitionId(id);
        prop.getValue().add(new BigInteger("" + value));
        props.getProperty().add(prop);
    }

    private void addPropertyBoolean(CmisPropertiesType props, String typeId, Set<String> filter, String id, boolean value) {
        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }
        CmisPropertyBoolean prop = new CmisPropertyBoolean();
        prop.setPropertyDefinitionId(id);
        prop.getValue().add(value);
        props.getProperty().add(prop);
    }

    private void addPropertyDateTime(CmisPropertiesType props, String typeId, Set<String> filter, String id, long value) {
        if (!checkAddProperty(props, typeId, filter, id)) {
            return;
        }
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.setTimeInMillis(value);
        CmisPropertyDateTime prop = new CmisPropertyDateTime();
        prop.setPropertyDefinitionId(id);
        try {
            prop.getValue().add(DatatypeFactory.newInstance().newXMLGregorianCalendar(cal));
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("Error in calender conversion: " + e.getMessage(), e);
        }
        props.getProperty().add(prop);
    }

    private boolean checkAddProperty(CmisPropertiesType props, String typeId, Set<String> filter, String id) {
        if (props == null) {
            throw new IllegalArgumentException("Props must not be null!");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null!");
        }
        Map<String, CmisPropertyDefinitionType> propDefMap = fTypes.getPropertyDefinitions(typeId);
        if (propDefMap == null) {
            throw new IllegalArgumentException("Unknown type: " + typeId);
        }
        if (!propDefMap.containsKey(id)) {
            throw new IllegalArgumentException("Unknown property: " + id);
        }
        String queryName = propDefMap.get(id).getQueryName();
        if ((queryName != null) && (filter != null)) {
            if (!filter.contains(queryName)) {
                return false;
            } else {
                filter.remove(queryName);
            }
        }
        return true;
    }

    /**
	 * Adds the default value of property if defined.
	 */
    private boolean addPropertyDefault(CmisPropertiesType props, CmisPropertyDefinitionType propDef) {
        if (props == null) {
            throw new IllegalArgumentException("Props must not be null!");
        }
        if (propDef == null) {
            return false;
        }
        try {
            Method m = propDef.getClass().getMethod("getDefaultValue", new Class[0]);
            CmisProperty defProp = (CmisProperty) m.invoke(propDef, new Object[0]);
            if (defProp != null) {
                props.getProperty().add(defProp);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
	 * Compiles the allowable actions for a file or folder.
	 */
    private CmisAllowableActionsType compileAllowableActions(File file, boolean userReadOnly) throws CMISFileShareException {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null!");
        }
        if (!file.exists()) {
            throw new CMISFileShareException(EnumServiceException.OBJECT_NOT_FOUND, "Object not found!", 0);
        }
        boolean isReadOnly = !file.canWrite();
        boolean isFolder = file.isDirectory();
        boolean isRoot = fRoot.equals(file);
        CmisAllowableActionsType result = new CmisAllowableActionsType();
        result.setCanGetObjectParents(!isRoot);
        result.setCanCreateRelationship(false);
        result.setCanGetProperties(true);
        result.setCanGetRenditions(false);
        result.setCanUpdateProperties(!userReadOnly && !isReadOnly);
        result.setCanMoveObject(!userReadOnly);
        result.setCanDeleteObject(!userReadOnly && !isReadOnly);
        result.setCanGetObjectRelationships(false);
        result.setCanApplyPolicy(false);
        result.setCanRemovePolicy(false);
        result.setCanGetACL(true);
        result.setCanApplyACL(false);
        if (isFolder) {
            result.setCanGetDescendants(true);
            result.setCanGetChildren(true);
            result.setCanGetFolderParent(!isRoot);
            result.setCanGetFolderTree(true);
            result.setCanCreateDocument(!userReadOnly);
            result.setCanCreateFolder(!userReadOnly);
            result.setCanCreatePolicy(false);
            result.setCanDeleteTree(!userReadOnly && !isReadOnly);
        } else {
            result.setCanGetContentStream(true);
            result.setCanSetContentStream(!userReadOnly && !isReadOnly);
            result.setCanDeleteContentStream(!userReadOnly && !isReadOnly);
            result.setCanAddObjectToFolder(false);
            result.setCanRemoveObjectFromFolder(false);
            result.setCanCheckOut(false);
            result.setCanCancelCheckOut(false);
            result.setCanCheckIn(false);
            result.setCanGetAllVersions(true);
        }
        return result;
    }

    /**
	 * Compiles the ACL for a file or folder.
	 */
    private CmisAccessControlListType compileACL(File file) {
        CmisAccessControlListType result = new CmisAccessControlListType();
        for (Map.Entry<String, Boolean> ue : fUserMap.entrySet()) {
            CmisAccessControlPrincipalType principal = new CmisAccessControlPrincipalType();
            principal.setPrincipalId(ue.getKey());
            CmisAccessControlEntryType entry = new CmisAccessControlEntryType();
            entry.setPrincipal(principal);
            entry.getPermission().add(EnumBasicPermissions.CMIS_READ.value());
            if (!ue.getValue().booleanValue() && file.canWrite()) {
                entry.getPermission().add(EnumBasicPermissions.CMIS_WRITE.value());
                entry.getPermission().add(EnumBasicPermissions.CMIS_ALL.value());
            }
            entry.setDirect(true);
            result.getPermission().add(entry);
        }
        return result;
    }

    /**
	 * Writes the properties for a document or folder.
	 */
    private void writePropertiesFile(File file, CmisPropertiesType properties) throws CMISFileShareException {
        File propFile = getPropertiesFile(file);
        if ((properties == null) || (properties.getProperty().size() == 0)) {
            propFile.delete();
            return;
        }
        CmisObjectType obj = new CmisObjectType();
        obj.setProperties(properties);
        try {
            JAXBElement<CmisObjectType> objElement = new JAXBElement<CmisObjectType>(CMIS_OBJECT, CmisObjectType.class, obj);
            Marshaller m = JAXB_CONTEXT.createMarshaller();
            m.setProperty("jaxb.formatted.output", true);
            m.marshal(objElement, propFile);
        } catch (Exception e) {
            throw new CMISFileShareException(EnumServiceException.STORAGE, "Couldn't store properties!", 0, e);
        }
    }

    private boolean checkUser(CallContext context, boolean writeRequired) throws CMISFileShareException {
        if (context == null) {
            throw new CMISFileShareException(EnumServiceException.PERMISSION_DENIED, "No user context.", 0);
        }
        Boolean readOnly = fUserMap.get(context.getUser());
        if (readOnly == null) {
            throw new CMISFileShareException(EnumServiceException.PERMISSION_DENIED, "Unknown user.", 0);
        }
        if (readOnly.booleanValue() && writeRequired) {
            throw new CMISFileShareException(EnumServiceException.PERMISSION_DENIED, "No write permission.", 0);
        }
        return readOnly.booleanValue();
    }

    private File getPropertiesFile(File file) {
        if (file.isDirectory()) {
            return new File(file, SHADOW_FOLDER);
        }
        return new File(file.getAbsolutePath() + SHADOW_EXT);
    }

    /**
	 * Converts an id to a File object. A simple and insecure implementation,
	 * but good enough for now.
	 */
    private File idToFile(String id) throws Exception {
        if ((id == null) || (id.length() == 0)) {
            throw new IllegalArgumentException("Id is not valid!");
        }
        if (id.equals(ROOT_ID)) {
            return fRoot;
        }
        return new File(fRoot, (new String(Base64.decodeBase64(id.getBytes("ISO-8859-1")), "UTF-8")).replace('/', File.separatorChar));
    }

    /**
	 * Creates a File object from an id. A simple and insecure implementation,
	 * but good enough for now.
	 */
    private String fileToId(File file) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("Fle is not valid!");
        }
        if (fRoot.equals(file)) {
            return ROOT_ID;
        }
        String path = getRepositoryPath(file);
        return new String(Base64.encodeBase64(path.getBytes("UTF-8")), "ISO-8859-1");
    }

    private String getRepositoryPath(File file) {
        return file.getAbsolutePath().substring(fRoot.getAbsolutePath().length()).replace(File.separatorChar, '/');
    }

    private void info(String msg) {
        info(msg, null);
    }

    private void info(String msg, Throwable t) {
        log.info("<" + fRepositoryId + "> " + msg, t);
    }

    private void warn(String msg) {
        warn(msg, null);
    }

    private void warn(String msg, Throwable t) {
        log.warn("<" + fRepositoryId + "> " + msg, t);
    }

    private void debug(String msg) {
        debug(msg, null);
    }

    private void debug(String msg, Throwable t) {
        log.debug("<" + fRepositoryId + "> " + msg, t);
    }
}
