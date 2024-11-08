package net.sf.archimede;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.OnParentVersionAction;
import net.sf.archimede.model.DatabaseUtil;
import net.sf.archimede.model.collection.Collection;
import net.sf.archimede.model.collection.CollectionDao;
import net.sf.archimede.model.collection.CollectionImpl;
import net.sf.archimede.model.content.ContentDao;
import net.sf.archimede.model.folder.FolderDao;
import net.sf.archimede.model.group.GroupDao;
import net.sf.archimede.model.metadata.MetadataDao;
import net.sf.archimede.model.user.User;
import net.sf.archimede.model.user.UserDao;
import net.sf.archimede.model.user.UserImpl;
import net.sf.archimede.security.SystemPrincipal;
import net.sf.archimede.util.PasswordUtil;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.core.nodetype.ValueConstraint;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;

public class NodesTypes {

    public static String NAMESPACE = "http://sf.net/projects/archimede/ns/2.0";

    public static String PREFIX = "arc:";

    public static String CONFIG_NODE = PREFIX + "config";

    public static String CONFIG_INDEXER_PROPERTY = PREFIX + "indexToken";

    public static String INDEX_NODE = PREFIX + "index";

    public static String DUBLIN_CORE_NAMESPACE = "http://purl.org/dc/elements/1.1/";

    public static String DUBLIN_CORE_PREFIX = "dc:";

    public static String DUBLIN_CORE_METADATA_NAME = "dublinCoreMetadata";

    public static String DUBLIN_CORE_ELEMENT_NAME = "dublinCoreElement";

    public static String ETDMS_NAMESPACE = "http://www.ndltd.org/standards/metadata/etdms/1.0/";

    public static String ETDMS_PREFIX = "etdms:";

    public static String ETDMS_METADATA_NAME = "etdmsMetadata";

    public static String ETDMS_ELEMENT_NAME = "thesis";

    public static String ETDMS_DEGREE_ELEMENT_NAME = "degree";

    private static NamespaceResolver namespaceResolver;

    private static PropDef[] getCollectionPropertyDefs() throws InvalidConstraintException {
        PropDefImpl imageUrlProp = new PropDefImpl();
        imageUrlProp.setAutoCreated(false);
        imageUrlProp.setDeclaringNodeType(new QName(NAMESPACE, CollectionDao.NAME));
        imageUrlProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        imageUrlProp.setMandatory(false);
        imageUrlProp.setMultiple(false);
        imageUrlProp.setName(new QName(NAMESPACE, CollectionDao.IMAGEURL_PROPERTY_NAME));
        imageUrlProp.setOnParentVersion(OnParentVersionAction.VERSION);
        imageUrlProp.setProtected(false);
        imageUrlProp.setRequiredType(PropertyType.STRING);
        imageUrlProp.setValueConstraints(new ValueConstraint[0]);
        PropDefImpl iconProp = new PropDefImpl();
        iconProp.setAutoCreated(true);
        iconProp.setDeclaringNodeType(new QName(NAMESPACE, CollectionDao.NAME));
        iconProp.setDefaultValues(InternalValue.create(new String[] { "default" }));
        iconProp.setMandatory(false);
        iconProp.setMultiple(false);
        iconProp.setName(new QName(NAMESPACE, CollectionDao.ICON_PROPERTY_NAME));
        iconProp.setOnParentVersion(OnParentVersionAction.VERSION);
        iconProp.setProtected(false);
        iconProp.setRequiredType(PropertyType.STRING);
        ValueConstraint[] constraints = new ValueConstraint[CollectionDao.ICON_PROPERTIES_VALUES.size()];
        for (ListIterator it = CollectionDao.ICON_PROPERTIES_VALUES.listIterator(); it.hasNext(); ) {
            constraints[it.nextIndex()] = ValueConstraint.create(PropertyType.STRING, (String) it.next(), namespaceResolver);
        }
        iconProp.setValueConstraints(constraints);
        PropDefImpl descriptionProp = new PropDefImpl();
        descriptionProp.setAutoCreated(false);
        descriptionProp.setDeclaringNodeType(new QName(NAMESPACE, CollectionDao.NAME));
        descriptionProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        descriptionProp.setMandatory(false);
        descriptionProp.setMultiple(false);
        descriptionProp.setName(new QName(NAMESPACE, CollectionDao.DESCRIPTION_PROPERTY_NAME));
        descriptionProp.setOnParentVersion(OnParentVersionAction.COPY);
        descriptionProp.setProtected(false);
        descriptionProp.setRequiredType(PropertyType.STRING);
        descriptionProp.setValueConstraints(ValueConstraint.EMPTY_ARRAY);
        return new PropDef[] { imageUrlProp, iconProp, descriptionProp };
    }

    private static NodeDef[] getCollectionNodeDefs() {
        NodeDefImpl collectionsNodeDef = new NodeDefImpl();
        collectionsNodeDef.setAllowsSameNameSiblings(false);
        collectionsNodeDef.setAutoCreated(false);
        collectionsNodeDef.setDeclaringNodeType(new QName(NAMESPACE, CollectionDao.NAME));
        collectionsNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, CollectionDao.NAME));
        collectionsNodeDef.setMandatory(false);
        collectionsNodeDef.setName(new QName("", "*"));
        collectionsNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        collectionsNodeDef.setProtected(false);
        collectionsNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, CollectionDao.NAME) });
        NodeDefImpl foldersNodeDef = new NodeDefImpl();
        foldersNodeDef.setAllowsSameNameSiblings(false);
        foldersNodeDef.setAutoCreated(true);
        foldersNodeDef.setDeclaringNodeType(new QName(NAMESPACE, CollectionDao.NAME));
        foldersNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, CollectionDao.FOLDERS_NODE_NAME));
        foldersNodeDef.setMandatory(true);
        foldersNodeDef.setName(new QName(NAMESPACE, CollectionDao.FOLDERS_NODE_NAME));
        foldersNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        foldersNodeDef.setProtected(false);
        foldersNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, CollectionDao.FOLDERS_NODE_NAME) });
        NodeDefImpl groupsNodeDef = new NodeDefImpl();
        groupsNodeDef.setAllowsSameNameSiblings(false);
        groupsNodeDef.setAutoCreated(true);
        groupsNodeDef.setDeclaringNodeType(new QName(NAMESPACE, CollectionDao.NAME));
        groupsNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, CollectionDao.GROUPS_NODE_NAME));
        groupsNodeDef.setMandatory(true);
        groupsNodeDef.setName(new QName(NAMESPACE, CollectionDao.GROUPS_NODE_NAME));
        groupsNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        groupsNodeDef.setProtected(false);
        groupsNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, CollectionDao.GROUPS_NODE_NAME) });
        return new NodeDef[] { collectionsNodeDef, foldersNodeDef, groupsNodeDef };
    }

    private static NodeTypeDef getCollectionNodeTypeDef() throws InvalidConstraintException {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(getCollectionNodeDefs());
        ntDef.setMixin(false);
        ntDef.setName(new QName(NAMESPACE, CollectionDao.NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(new QName(NAMESPACE, CollectionDao.FOLDERS_NODE_NAME));
        ntDef.setPropertyDefs(getCollectionPropertyDefs());
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable"), new QName("http://www.jcp.org/jcr/mix/1.0", "lockable"), new QName(NAMESPACE, ContentDao.NAME) });
        return ntDef;
    }

    private static PropDef[] getValueElementPropertyDefs() throws InvalidConstraintException {
        PropDefImpl valueProp = new PropDefImpl();
        valueProp.setAutoCreated(false);
        valueProp.setDeclaringNodeType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_NAME));
        valueProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        valueProp.setMandatory(true);
        valueProp.setMultiple(false);
        valueProp.setName(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_PROPERTY_NAME));
        valueProp.setOnParentVersion(OnParentVersionAction.VERSION);
        valueProp.setProtected(false);
        valueProp.setRequiredType(PropertyType.STRING);
        valueProp.setValueConstraints(new ValueConstraint[0]);
        return new PropDef[] { valueProp };
    }

    private static NodeTypeDef getValueElementNodeTypeDef() throws InvalidConstraintException {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(NodeDef.EMPTY_ARRAY);
        ntDef.setMixin(false);
        ntDef.setName(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_NAME));
        ntDef.setPropertyDefs(getValueElementPropertyDefs());
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable"), new QName("http://www.jcp.org/jcr/mix/1.0", "lockable") });
        return ntDef;
    }

    private static NodeDef[] getFoldersNodeDefs() {
        NodeDefImpl foldersNodeDef = new NodeDefImpl();
        foldersNodeDef.setAllowsSameNameSiblings(false);
        foldersNodeDef.setAutoCreated(false);
        foldersNodeDef.setDeclaringNodeType(new QName(NAMESPACE, CollectionDao.FOLDERS_NODE_NAME));
        foldersNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, FolderDao.NAME));
        foldersNodeDef.setMandatory(false);
        foldersNodeDef.setName(new QName("", "*"));
        foldersNodeDef.setOnParentVersion(OnParentVersionAction.COPY);
        foldersNodeDef.setProtected(false);
        foldersNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, FolderDao.NAME) });
        return new NodeDef[] { foldersNodeDef };
    }

    private static NodeTypeDef getFoldersNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(getFoldersNodeDefs());
        ntDef.setMixin(false);
        ntDef.setName(new QName(NAMESPACE, CollectionDao.FOLDERS_NODE_NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(null);
        ntDef.setPropertyDefs(PropDef.EMPTY_ARRAY);
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable") });
        return ntDef;
    }

    private static NodeDef[] getGroupsNodeDefs() {
        NodeDefImpl groupsNodeDef = new NodeDefImpl();
        groupsNodeDef.setAllowsSameNameSiblings(false);
        groupsNodeDef.setAutoCreated(false);
        groupsNodeDef.setDeclaringNodeType(new QName(NAMESPACE, CollectionDao.GROUPS_NODE_NAME));
        groupsNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, GroupDao.NAME));
        groupsNodeDef.setMandatory(true);
        groupsNodeDef.setName(new QName("", "*"));
        groupsNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        groupsNodeDef.setProtected(false);
        groupsNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, GroupDao.NAME) });
        return new NodeDef[] { groupsNodeDef };
    }

    private static NodeTypeDef getGroupsNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(getGroupsNodeDefs());
        ntDef.setMixin(false);
        ntDef.setName(new QName(NAMESPACE, CollectionDao.GROUPS_NODE_NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(null);
        ntDef.setPropertyDefs(PropDef.EMPTY_ARRAY);
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable") });
        return ntDef;
    }

    private static PropDef[] getGroupPropertyDefs() {
        PropDefImpl descriptionProp = new PropDefImpl();
        descriptionProp.setAutoCreated(false);
        descriptionProp.setDeclaringNodeType(new QName(NAMESPACE, GroupDao.NAME));
        descriptionProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        descriptionProp.setMandatory(false);
        descriptionProp.setMultiple(false);
        descriptionProp.setName(new QName(NAMESPACE, GroupDao.DESCRIPTION_PROPERTY_NAME));
        descriptionProp.setOnParentVersion(OnParentVersionAction.VERSION);
        descriptionProp.setProtected(false);
        descriptionProp.setRequiredType(PropertyType.STRING);
        descriptionProp.setValueConstraints(ValueConstraint.EMPTY_ARRAY);
        PropDefImpl usersRefsProp = new PropDefImpl();
        usersRefsProp.setAutoCreated(false);
        usersRefsProp.setDeclaringNodeType(new QName(NAMESPACE, GroupDao.NAME));
        usersRefsProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        usersRefsProp.setMandatory(false);
        usersRefsProp.setMultiple(true);
        usersRefsProp.setName(new QName(NAMESPACE, GroupDao.USERS_PROPERTY_NAME));
        usersRefsProp.setOnParentVersion(OnParentVersionAction.COPY);
        usersRefsProp.setProtected(false);
        usersRefsProp.setRequiredType(PropertyType.STRING);
        usersRefsProp.setValueConstraints(ValueConstraint.EMPTY_ARRAY);
        return new PropDef[] { descriptionProp, usersRefsProp };
    }

    private static NodeTypeDef getGroupNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(NodeDef.EMPTY_ARRAY);
        ntDef.setMixin(false);
        ntDef.setName(new QName(NAMESPACE, GroupDao.NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(new QName(NAMESPACE, GroupDao.DESCRIPTION_PROPERTY_NAME));
        ntDef.setPropertyDefs(getGroupPropertyDefs());
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable") });
        return ntDef;
    }

    private static PropDef[] getFolderPropertyDefs() {
        PropDefImpl datestampProp = new PropDefImpl();
        datestampProp.setAutoCreated(false);
        datestampProp.setDeclaringNodeType(new QName(NAMESPACE, FolderDao.NAME));
        datestampProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        datestampProp.setMandatory(true);
        datestampProp.setMultiple(false);
        datestampProp.setName(new QName(NAMESPACE, FolderDao.DATESTAMP_PROPERTY_NAME));
        datestampProp.setOnParentVersion(OnParentVersionAction.COPY);
        datestampProp.setProtected(false);
        datestampProp.setRequiredType(PropertyType.DATE);
        datestampProp.setValueConstraints(ValueConstraint.EMPTY_ARRAY);
        PropDefImpl indexStateProp = new PropDefImpl();
        indexStateProp.setAutoCreated(false);
        indexStateProp.setDeclaringNodeType(new QName(NAMESPACE, FolderDao.NAME));
        indexStateProp.setDefaultValues(InternalValue.create(new String[] { "ok" }));
        indexStateProp.setMandatory(true);
        indexStateProp.setMultiple(false);
        indexStateProp.setName(new QName(NAMESPACE, FolderDao.INDEX_STATE_PROPERTY_NAME));
        indexStateProp.setOnParentVersion(OnParentVersionAction.IGNORE);
        indexStateProp.setProtected(false);
        indexStateProp.setRequiredType(PropertyType.STRING);
        indexStateProp.setValueConstraints(ValueConstraint.EMPTY_ARRAY);
        return new PropDef[] { datestampProp, indexStateProp };
    }

    private static NodeDef[] getFolderNodeDefs() {
        NodeDefImpl filesNodeDef = new NodeDefImpl();
        filesNodeDef.setAllowsSameNameSiblings(false);
        filesNodeDef.setAutoCreated(false);
        filesNodeDef.setDeclaringNodeType(new QName(NAMESPACE, FolderDao.NAME));
        filesNodeDef.setDefaultPrimaryType(new QName("http://www.jcp.org/jcr/nt/1.0", "file"));
        filesNodeDef.setMandatory(false);
        filesNodeDef.setName(new QName("", "*"));
        filesNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        filesNodeDef.setProtected(false);
        filesNodeDef.setRequiredPrimaryTypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "file") });
        NodeDefImpl metadatasNodeDef = new NodeDefImpl();
        metadatasNodeDef.setAllowsSameNameSiblings(false);
        metadatasNodeDef.setAutoCreated(true);
        metadatasNodeDef.setDeclaringNodeType(new QName(NAMESPACE, FolderDao.NAME));
        metadatasNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, FolderDao.METADATAS_NODE_NAME));
        metadatasNodeDef.setMandatory(true);
        metadatasNodeDef.setName(new QName(NAMESPACE, FolderDao.METADATAS_NODE_NAME));
        metadatasNodeDef.setOnParentVersion(OnParentVersionAction.COPY);
        metadatasNodeDef.setProtected(false);
        metadatasNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, FolderDao.METADATAS_NODE_NAME) });
        return new NodeDef[] { filesNodeDef, metadatasNodeDef };
    }

    private static NodeTypeDef getFolderNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(getFolderNodeDefs());
        ntDef.setMixin(false);
        ntDef.setName(new QName(NAMESPACE, FolderDao.NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(null);
        ntDef.setPropertyDefs(getFolderPropertyDefs());
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable"), new QName("http://www.jcp.org/jcr/mix/1.0", "lockable"), new QName(NAMESPACE, ContentDao.NAME) });
        return ntDef;
    }

    private static NodeDef[] getMetadatasNodeDefs() {
        NodeDefImpl metadatasNodeDef = new NodeDefImpl();
        metadatasNodeDef.setAllowsSameNameSiblings(false);
        metadatasNodeDef.setAutoCreated(false);
        metadatasNodeDef.setDeclaringNodeType(new QName(NAMESPACE, FolderDao.METADATAS_NODE_NAME));
        metadatasNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.NAME));
        metadatasNodeDef.setMandatory(true);
        metadatasNodeDef.setName(new QName("", "*"));
        metadatasNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        metadatasNodeDef.setProtected(false);
        metadatasNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.NAME) });
        return new NodeDef[] { metadatasNodeDef };
    }

    private static NodeTypeDef getMetadatasNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(getMetadatasNodeDefs());
        ntDef.setMixin(false);
        ntDef.setName(new QName(NAMESPACE, FolderDao.METADATAS_NODE_NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(null);
        ntDef.setPropertyDefs(PropDef.EMPTY_ARRAY);
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable") });
        return ntDef;
    }

    private static PropDef[] getRestrictedContentPropertyDefs() throws InvalidConstraintException {
        PropDefImpl readGroupsRefsProp = new PropDefImpl();
        readGroupsRefsProp.setAutoCreated(false);
        readGroupsRefsProp.setDeclaringNodeType(new QName(NAMESPACE, ContentDao.NAME));
        readGroupsRefsProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        readGroupsRefsProp.setMandatory(false);
        readGroupsRefsProp.setMultiple(true);
        readGroupsRefsProp.setName(new QName(NAMESPACE, ContentDao.READ_GROUPS_PROPERTY));
        readGroupsRefsProp.setOnParentVersion(OnParentVersionAction.COPY);
        readGroupsRefsProp.setProtected(false);
        readGroupsRefsProp.setRequiredType(PropertyType.REFERENCE);
        ValueConstraint[] readGroupsRefsConstraints = new ValueConstraint[] { ValueConstraint.create(PropertyType.REFERENCE, PREFIX + GroupDao.NAME, namespaceResolver) };
        readGroupsRefsProp.setValueConstraints(readGroupsRefsConstraints);
        PropDefImpl readUsersRefsProp = new PropDefImpl();
        readUsersRefsProp.setAutoCreated(false);
        readUsersRefsProp.setDeclaringNodeType(new QName(NAMESPACE, ContentDao.NAME));
        readUsersRefsProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        readUsersRefsProp.setMandatory(false);
        readUsersRefsProp.setMultiple(true);
        readUsersRefsProp.setName(new QName(NAMESPACE, ContentDao.READ_USERS_PROPERTY));
        readUsersRefsProp.setOnParentVersion(OnParentVersionAction.COPY);
        readUsersRefsProp.setProtected(false);
        readUsersRefsProp.setRequiredType(PropertyType.STRING);
        readUsersRefsProp.setValueConstraints(ValueConstraint.EMPTY_ARRAY);
        PropDefImpl writeGroupsRefsProp = new PropDefImpl();
        writeGroupsRefsProp.setAutoCreated(false);
        writeGroupsRefsProp.setDeclaringNodeType(new QName(NAMESPACE, ContentDao.NAME));
        writeGroupsRefsProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        writeGroupsRefsProp.setMandatory(false);
        writeGroupsRefsProp.setMultiple(true);
        writeGroupsRefsProp.setName(new QName(NAMESPACE, ContentDao.WRITE_GROUPS_PROPERTY));
        writeGroupsRefsProp.setOnParentVersion(OnParentVersionAction.COPY);
        writeGroupsRefsProp.setProtected(false);
        writeGroupsRefsProp.setRequiredType(PropertyType.REFERENCE);
        ValueConstraint[] writeGroupsRefsConstraints = new ValueConstraint[] { ValueConstraint.create(PropertyType.REFERENCE, PREFIX + GroupDao.NAME, namespaceResolver) };
        writeGroupsRefsProp.setValueConstraints(writeGroupsRefsConstraints);
        PropDefImpl writeUsersRefsProp = new PropDefImpl();
        writeUsersRefsProp.setAutoCreated(false);
        writeUsersRefsProp.setDeclaringNodeType(new QName(NAMESPACE, ContentDao.NAME));
        writeUsersRefsProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        writeUsersRefsProp.setMandatory(false);
        writeUsersRefsProp.setMultiple(true);
        writeUsersRefsProp.setName(new QName(NAMESPACE, ContentDao.WRITE_USERS_PROPERTY));
        writeUsersRefsProp.setOnParentVersion(OnParentVersionAction.COPY);
        writeUsersRefsProp.setProtected(false);
        writeUsersRefsProp.setRequiredType(PropertyType.STRING);
        writeUsersRefsProp.setValueConstraints(ValueConstraint.EMPTY_ARRAY);
        PropDefImpl removeGroupsRefsProp = new PropDefImpl();
        removeGroupsRefsProp.setAutoCreated(false);
        removeGroupsRefsProp.setDeclaringNodeType(new QName(NAMESPACE, ContentDao.NAME));
        removeGroupsRefsProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        removeGroupsRefsProp.setMandatory(false);
        removeGroupsRefsProp.setMultiple(true);
        removeGroupsRefsProp.setName(new QName(NAMESPACE, ContentDao.REMOVE_GROUPS_PROPERTY));
        removeGroupsRefsProp.setOnParentVersion(OnParentVersionAction.COPY);
        removeGroupsRefsProp.setProtected(false);
        removeGroupsRefsProp.setRequiredType(PropertyType.REFERENCE);
        ValueConstraint[] removeGroupsRefsConstraints = new ValueConstraint[] { ValueConstraint.create(PropertyType.REFERENCE, PREFIX + GroupDao.NAME, namespaceResolver) };
        removeGroupsRefsProp.setValueConstraints(removeGroupsRefsConstraints);
        PropDefImpl removeUsersRefsProp = new PropDefImpl();
        removeUsersRefsProp.setAutoCreated(false);
        removeUsersRefsProp.setDeclaringNodeType(new QName(NAMESPACE, ContentDao.NAME));
        removeUsersRefsProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        removeUsersRefsProp.setMandatory(false);
        removeUsersRefsProp.setMultiple(true);
        removeUsersRefsProp.setName(new QName(NAMESPACE, ContentDao.REMOVE_USERS_PROPERTY));
        removeUsersRefsProp.setOnParentVersion(OnParentVersionAction.COPY);
        removeUsersRefsProp.setProtected(false);
        removeUsersRefsProp.setRequiredType(PropertyType.STRING);
        removeUsersRefsProp.setValueConstraints(ValueConstraint.EMPTY_ARRAY);
        PropDefImpl ownerGroupsRefsProp = new PropDefImpl();
        ownerGroupsRefsProp.setAutoCreated(false);
        ownerGroupsRefsProp.setDeclaringNodeType(new QName(NAMESPACE, ContentDao.NAME));
        ownerGroupsRefsProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        ownerGroupsRefsProp.setMandatory(false);
        ownerGroupsRefsProp.setMultiple(true);
        ownerGroupsRefsProp.setName(new QName(NAMESPACE, ContentDao.OWNER_GROUPS_PROPERTY));
        ownerGroupsRefsProp.setOnParentVersion(OnParentVersionAction.COPY);
        ownerGroupsRefsProp.setProtected(false);
        ownerGroupsRefsProp.setRequiredType(PropertyType.REFERENCE);
        ValueConstraint[] ownerGroupsRefsConstraints = new ValueConstraint[] { ValueConstraint.create(PropertyType.REFERENCE, PREFIX + GroupDao.NAME, namespaceResolver) };
        ownerGroupsRefsProp.setValueConstraints(ownerGroupsRefsConstraints);
        PropDefImpl ownerUsersRefsProp = new PropDefImpl();
        ownerUsersRefsProp.setAutoCreated(false);
        ownerUsersRefsProp.setDeclaringNodeType(new QName(NAMESPACE, ContentDao.NAME));
        ownerUsersRefsProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        ownerUsersRefsProp.setMandatory(false);
        ownerUsersRefsProp.setMultiple(true);
        ownerUsersRefsProp.setName(new QName(NAMESPACE, ContentDao.OWNER_USERS_PROPERTY));
        ownerUsersRefsProp.setOnParentVersion(OnParentVersionAction.COPY);
        ownerUsersRefsProp.setProtected(false);
        ownerUsersRefsProp.setRequiredType(PropertyType.STRING);
        ownerUsersRefsProp.setValueConstraints(ValueConstraint.EMPTY_ARRAY);
        PropDefImpl restrictChildrenProp = new PropDefImpl();
        restrictChildrenProp.setAutoCreated(true);
        restrictChildrenProp.setDeclaringNodeType(new QName(NAMESPACE, ContentDao.NAME));
        restrictChildrenProp.setDefaultValues(new InternalValue[] { InternalValue.create(false) });
        restrictChildrenProp.setMandatory(true);
        restrictChildrenProp.setMultiple(false);
        restrictChildrenProp.setName(new QName(NAMESPACE, ContentDao.RESTRICT_CHILDREN_PROPERTY));
        restrictChildrenProp.setOnParentVersion(OnParentVersionAction.COPY);
        restrictChildrenProp.setProtected(false);
        restrictChildrenProp.setRequiredType(PropertyType.BOOLEAN);
        restrictChildrenProp.setValueConstraints(ValueConstraint.EMPTY_ARRAY);
        return new PropDef[] { readGroupsRefsProp, readUsersRefsProp, writeGroupsRefsProp, writeUsersRefsProp, removeGroupsRefsProp, removeUsersRefsProp, ownerGroupsRefsProp, ownerUsersRefsProp, restrictChildrenProp };
    }

    private static NodeTypeDef getSecuredContentNodeTypeDef() throws InvalidConstraintException {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(NodeDef.EMPTY_ARRAY);
        ntDef.setMixin(true);
        ntDef.setName(new QName(NAMESPACE, ContentDao.NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(null);
        ntDef.setPropertyDefs(getRestrictedContentPropertyDefs());
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base") });
        return ntDef;
    }

    private static NodeDef[] getValueElementHolderNodeDefs() {
        NodeDefImpl dcTitleNodeDef = new NodeDefImpl();
        dcTitleNodeDef.setAllowsSameNameSiblings(false);
        dcTitleNodeDef.setAutoCreated(false);
        dcTitleNodeDef.setDeclaringNodeType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        dcTitleNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_NAME));
        dcTitleNodeDef.setMandatory(true);
        dcTitleNodeDef.setName(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_NAME));
        dcTitleNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        dcTitleNodeDef.setProtected(false);
        dcTitleNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_NAME) });
        return new NodeDef[] { dcTitleNodeDef };
    }

    private static NodeTypeDef getValueElementHolderNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(getValueElementHolderNodeDefs());
        ntDef.setMixin(false);
        ntDef.setName(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_NAME));
        ntDef.setPropertyDefs(PropDef.EMPTY_ARRAY);
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable"), new QName("http://www.jcp.org/jcr/mix/1.0", "lockable") });
        return ntDef;
    }

    private static NodeDef[] getDublinCoreNodeDefs() {
        NodeDefImpl titleNodeDef = new NodeDefImpl();
        titleNodeDef.setAllowsSameNameSiblings(true);
        titleNodeDef.setAutoCreated(false);
        titleNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        titleNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        titleNodeDef.setMandatory(false);
        titleNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "title"));
        titleNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        titleNodeDef.setProtected(false);
        titleNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl creatorNodeDef = new NodeDefImpl();
        creatorNodeDef.setAllowsSameNameSiblings(true);
        creatorNodeDef.setAutoCreated(false);
        creatorNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        creatorNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        creatorNodeDef.setMandatory(false);
        creatorNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "creator"));
        creatorNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        creatorNodeDef.setProtected(false);
        creatorNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl subjectNodeDef = new NodeDefImpl();
        subjectNodeDef.setAllowsSameNameSiblings(true);
        subjectNodeDef.setAutoCreated(false);
        subjectNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        subjectNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        subjectNodeDef.setMandatory(false);
        subjectNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "subject"));
        subjectNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        subjectNodeDef.setProtected(false);
        subjectNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl descriptionNodeDef = new NodeDefImpl();
        descriptionNodeDef.setAllowsSameNameSiblings(true);
        descriptionNodeDef.setAutoCreated(false);
        descriptionNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        descriptionNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        descriptionNodeDef.setMandatory(false);
        descriptionNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "description"));
        descriptionNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        descriptionNodeDef.setProtected(false);
        descriptionNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl publisherNodeDef = new NodeDefImpl();
        publisherNodeDef.setAllowsSameNameSiblings(true);
        publisherNodeDef.setAutoCreated(false);
        publisherNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        publisherNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        publisherNodeDef.setMandatory(false);
        publisherNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "publisher"));
        publisherNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        publisherNodeDef.setProtected(false);
        publisherNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl contributorNodeDef = new NodeDefImpl();
        contributorNodeDef.setAllowsSameNameSiblings(true);
        contributorNodeDef.setAutoCreated(false);
        contributorNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        contributorNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        contributorNodeDef.setMandatory(false);
        contributorNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "contributor"));
        contributorNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        contributorNodeDef.setProtected(false);
        contributorNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl dateNodeDef = new NodeDefImpl();
        dateNodeDef.setAllowsSameNameSiblings(true);
        dateNodeDef.setAutoCreated(false);
        dateNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        dateNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        dateNodeDef.setMandatory(false);
        dateNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "date"));
        dateNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        dateNodeDef.setProtected(false);
        dateNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl typeNodeDef = new NodeDefImpl();
        typeNodeDef.setAllowsSameNameSiblings(true);
        typeNodeDef.setAutoCreated(false);
        typeNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        typeNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        typeNodeDef.setMandatory(false);
        typeNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "type"));
        typeNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        typeNodeDef.setProtected(false);
        typeNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl formatNodeDef = new NodeDefImpl();
        formatNodeDef.setAllowsSameNameSiblings(true);
        formatNodeDef.setAutoCreated(false);
        formatNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        formatNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        formatNodeDef.setMandatory(false);
        formatNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "format"));
        formatNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        formatNodeDef.setProtected(false);
        formatNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl identifierNodeDef = new NodeDefImpl();
        identifierNodeDef.setAllowsSameNameSiblings(true);
        identifierNodeDef.setAutoCreated(false);
        identifierNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        identifierNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        identifierNodeDef.setMandatory(false);
        identifierNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "identifier"));
        identifierNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        identifierNodeDef.setProtected(false);
        identifierNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl sourceNodeDef = new NodeDefImpl();
        sourceNodeDef.setAllowsSameNameSiblings(true);
        sourceNodeDef.setAutoCreated(false);
        sourceNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        sourceNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        sourceNodeDef.setMandatory(false);
        sourceNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "source"));
        sourceNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        sourceNodeDef.setProtected(false);
        sourceNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl languageNodeDef = new NodeDefImpl();
        languageNodeDef.setAllowsSameNameSiblings(true);
        languageNodeDef.setAutoCreated(false);
        languageNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        languageNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        languageNodeDef.setMandatory(false);
        languageNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "language"));
        languageNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        languageNodeDef.setProtected(false);
        languageNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl relationNodeDef = new NodeDefImpl();
        relationNodeDef.setAllowsSameNameSiblings(true);
        relationNodeDef.setAutoCreated(false);
        relationNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        relationNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        relationNodeDef.setMandatory(false);
        relationNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "relation"));
        relationNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        relationNodeDef.setProtected(false);
        relationNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl coverageNodeDef = new NodeDefImpl();
        coverageNodeDef.setAllowsSameNameSiblings(true);
        coverageNodeDef.setAutoCreated(false);
        coverageNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        coverageNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        coverageNodeDef.setMandatory(false);
        coverageNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "coverage"));
        coverageNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        coverageNodeDef.setProtected(false);
        coverageNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl rightsNodeDef = new NodeDefImpl();
        rightsNodeDef.setAllowsSameNameSiblings(true);
        rightsNodeDef.setAutoCreated(false);
        rightsNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        rightsNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        rightsNodeDef.setMandatory(false);
        rightsNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, "rights"));
        rightsNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        rightsNodeDef.setProtected(false);
        rightsNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        return new NodeDef[] { titleNodeDef, creatorNodeDef, subjectNodeDef, descriptionNodeDef, publisherNodeDef, contributorNodeDef, dateNodeDef, typeNodeDef, formatNodeDef, identifierNodeDef, sourceNodeDef, languageNodeDef, relationNodeDef, coverageNodeDef, rightsNodeDef };
    }

    private static NodeTypeDef getDublinCoreElementNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(getDublinCoreNodeDefs());
        ntDef.setMixin(false);
        ntDef.setName(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        ntDef.setOrderableChildNodes(true);
        ntDef.setPrimaryItemName(null);
        ntDef.setPropertyDefs(PropDef.EMPTY_ARRAY);
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable"), new QName("http://www.jcp.org/jcr/mix/1.0", "lockable") });
        return ntDef;
    }

    private static NodeDef[] getDublinCoreMetadataNodeDefs() {
        NodeDefImpl dublinCoreRootElementNodeDef = new NodeDefImpl();
        dublinCoreRootElementNodeDef.setAllowsSameNameSiblings(false);
        dublinCoreRootElementNodeDef.setAutoCreated(true);
        dublinCoreRootElementNodeDef.setDeclaringNodeType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_METADATA_NAME));
        dublinCoreRootElementNodeDef.setDefaultPrimaryType(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        dublinCoreRootElementNodeDef.setMandatory(true);
        dublinCoreRootElementNodeDef.setName(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME));
        dublinCoreRootElementNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        dublinCoreRootElementNodeDef.setProtected(false);
        dublinCoreRootElementNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_ELEMENT_NAME) });
        return new NodeDef[] { dublinCoreRootElementNodeDef };
    }

    private static NodeTypeDef getDublinCoreMetadataNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(getDublinCoreMetadataNodeDefs());
        ntDef.setMixin(false);
        ntDef.setName(new QName(DUBLIN_CORE_NAMESPACE, DUBLIN_CORE_METADATA_NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(null);
        ntDef.setPropertyDefs(PropDef.EMPTY_ARRAY);
        ntDef.setSupertypes(new QName[] { new QName(NAMESPACE, MetadataDao.NAME) });
        return ntDef;
    }

    private static NodeDef[] getEtdmsDegreeNodeDefs() {
        NodeDefImpl nameNodeDef = new NodeDefImpl();
        nameNodeDef.setAllowsSameNameSiblings(true);
        nameNodeDef.setAutoCreated(false);
        nameNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_DEGREE_ELEMENT_NAME));
        nameNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        nameNodeDef.setMandatory(false);
        nameNodeDef.setName(new QName(ETDMS_NAMESPACE, "name"));
        nameNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        nameNodeDef.setProtected(false);
        nameNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl levelNodeDef = new NodeDefImpl();
        levelNodeDef.setAllowsSameNameSiblings(true);
        levelNodeDef.setAutoCreated(false);
        levelNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_DEGREE_ELEMENT_NAME));
        levelNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        levelNodeDef.setMandatory(false);
        levelNodeDef.setName(new QName(ETDMS_NAMESPACE, "level"));
        levelNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        levelNodeDef.setProtected(false);
        levelNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl disciplineNodeDef = new NodeDefImpl();
        disciplineNodeDef.setAllowsSameNameSiblings(true);
        disciplineNodeDef.setAutoCreated(false);
        disciplineNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_DEGREE_ELEMENT_NAME));
        disciplineNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        disciplineNodeDef.setMandatory(false);
        disciplineNodeDef.setName(new QName(ETDMS_NAMESPACE, "discipline"));
        disciplineNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        disciplineNodeDef.setProtected(false);
        disciplineNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl grantorNodeDef = new NodeDefImpl();
        grantorNodeDef.setAllowsSameNameSiblings(true);
        grantorNodeDef.setAutoCreated(false);
        grantorNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_DEGREE_ELEMENT_NAME));
        grantorNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        grantorNodeDef.setMandatory(false);
        grantorNodeDef.setName(new QName(ETDMS_NAMESPACE, "grantor"));
        grantorNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        grantorNodeDef.setProtected(false);
        grantorNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        return new NodeDef[] { nameNodeDef, levelNodeDef, disciplineNodeDef, grantorNodeDef };
    }

    private static NodeTypeDef getEtdmsDegreeElementNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(getEtdmsDegreeNodeDefs());
        ntDef.setMixin(false);
        ntDef.setName(new QName(ETDMS_NAMESPACE, ETDMS_DEGREE_ELEMENT_NAME));
        ntDef.setOrderableChildNodes(true);
        ntDef.setPrimaryItemName(null);
        ntDef.setPropertyDefs(PropDef.EMPTY_ARRAY);
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable"), new QName("http://www.jcp.org/jcr/mix/1.0", "lockable") });
        return ntDef;
    }

    private static NodeDef[] getEtdmsNodeDefs() {
        NodeDefImpl titleNodeDef = new NodeDefImpl();
        titleNodeDef.setAllowsSameNameSiblings(true);
        titleNodeDef.setAutoCreated(false);
        titleNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        titleNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        titleNodeDef.setMandatory(false);
        titleNodeDef.setName(new QName(ETDMS_NAMESPACE, "title"));
        titleNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        titleNodeDef.setProtected(false);
        titleNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl creatorNodeDef = new NodeDefImpl();
        creatorNodeDef.setAllowsSameNameSiblings(true);
        creatorNodeDef.setAutoCreated(false);
        creatorNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        creatorNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        creatorNodeDef.setMandatory(false);
        creatorNodeDef.setName(new QName(ETDMS_NAMESPACE, "creator"));
        creatorNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        creatorNodeDef.setProtected(false);
        creatorNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl subjectNodeDef = new NodeDefImpl();
        subjectNodeDef.setAllowsSameNameSiblings(true);
        subjectNodeDef.setAutoCreated(false);
        subjectNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        subjectNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        subjectNodeDef.setMandatory(false);
        subjectNodeDef.setName(new QName(ETDMS_NAMESPACE, "subject"));
        subjectNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        subjectNodeDef.setProtected(false);
        subjectNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl descriptionNodeDef = new NodeDefImpl();
        descriptionNodeDef.setAllowsSameNameSiblings(true);
        descriptionNodeDef.setAutoCreated(false);
        descriptionNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        descriptionNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        descriptionNodeDef.setMandatory(false);
        descriptionNodeDef.setName(new QName(ETDMS_NAMESPACE, "description"));
        descriptionNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        descriptionNodeDef.setProtected(false);
        descriptionNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl publisherNodeDef = new NodeDefImpl();
        publisherNodeDef.setAllowsSameNameSiblings(true);
        publisherNodeDef.setAutoCreated(false);
        publisherNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        publisherNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        publisherNodeDef.setMandatory(false);
        publisherNodeDef.setName(new QName(ETDMS_NAMESPACE, "publisher"));
        publisherNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        publisherNodeDef.setProtected(false);
        publisherNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl contributorNodeDef = new NodeDefImpl();
        contributorNodeDef.setAllowsSameNameSiblings(true);
        contributorNodeDef.setAutoCreated(false);
        contributorNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        contributorNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        contributorNodeDef.setMandatory(false);
        contributorNodeDef.setName(new QName(ETDMS_NAMESPACE, "contributor"));
        contributorNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        contributorNodeDef.setProtected(false);
        contributorNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl dateNodeDef = new NodeDefImpl();
        dateNodeDef.setAllowsSameNameSiblings(true);
        dateNodeDef.setAutoCreated(false);
        dateNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        dateNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        dateNodeDef.setMandatory(false);
        dateNodeDef.setName(new QName(ETDMS_NAMESPACE, "date"));
        dateNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        dateNodeDef.setProtected(false);
        dateNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl typeNodeDef = new NodeDefImpl();
        typeNodeDef.setAllowsSameNameSiblings(true);
        typeNodeDef.setAutoCreated(false);
        typeNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        typeNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        typeNodeDef.setMandatory(false);
        typeNodeDef.setName(new QName(ETDMS_NAMESPACE, "type"));
        typeNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        typeNodeDef.setProtected(false);
        typeNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl formatNodeDef = new NodeDefImpl();
        formatNodeDef.setAllowsSameNameSiblings(true);
        formatNodeDef.setAutoCreated(false);
        formatNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        formatNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        formatNodeDef.setMandatory(false);
        formatNodeDef.setName(new QName(ETDMS_NAMESPACE, "format"));
        formatNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        formatNodeDef.setProtected(false);
        formatNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl identifierNodeDef = new NodeDefImpl();
        identifierNodeDef.setAllowsSameNameSiblings(true);
        identifierNodeDef.setAutoCreated(false);
        identifierNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        identifierNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        identifierNodeDef.setMandatory(false);
        identifierNodeDef.setName(new QName(ETDMS_NAMESPACE, "identifier"));
        identifierNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        identifierNodeDef.setProtected(false);
        identifierNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl languageNodeDef = new NodeDefImpl();
        languageNodeDef.setAllowsSameNameSiblings(true);
        languageNodeDef.setAutoCreated(false);
        languageNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        languageNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        languageNodeDef.setMandatory(false);
        languageNodeDef.setName(new QName(ETDMS_NAMESPACE, "language"));
        languageNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        languageNodeDef.setProtected(false);
        languageNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl coverageNodeDef = new NodeDefImpl();
        coverageNodeDef.setAllowsSameNameSiblings(true);
        coverageNodeDef.setAutoCreated(false);
        coverageNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        coverageNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        coverageNodeDef.setMandatory(false);
        coverageNodeDef.setName(new QName(ETDMS_NAMESPACE, "coverage"));
        coverageNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        coverageNodeDef.setProtected(false);
        coverageNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl rightsNodeDef = new NodeDefImpl();
        rightsNodeDef.setAllowsSameNameSiblings(true);
        rightsNodeDef.setAutoCreated(false);
        rightsNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        rightsNodeDef.setDefaultPrimaryType(new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER));
        rightsNodeDef.setMandatory(false);
        rightsNodeDef.setName(new QName(ETDMS_NAMESPACE, "rights"));
        rightsNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        rightsNodeDef.setProtected(false);
        rightsNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(NAMESPACE, MetadataDao.VALUE_ELEMENT_HOLDER) });
        NodeDefImpl degreeNodeDef = new NodeDefImpl();
        degreeNodeDef.setAllowsSameNameSiblings(false);
        degreeNodeDef.setAutoCreated(false);
        degreeNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        degreeNodeDef.setDefaultPrimaryType(new QName(ETDMS_NAMESPACE, ETDMS_DEGREE_ELEMENT_NAME));
        degreeNodeDef.setMandatory(false);
        degreeNodeDef.setName(new QName(ETDMS_NAMESPACE, ETDMS_DEGREE_ELEMENT_NAME));
        degreeNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        degreeNodeDef.setProtected(false);
        degreeNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(ETDMS_NAMESPACE, ETDMS_DEGREE_ELEMENT_NAME) });
        return new NodeDef[] { titleNodeDef, creatorNodeDef, subjectNodeDef, descriptionNodeDef, publisherNodeDef, contributorNodeDef, dateNodeDef, typeNodeDef, formatNodeDef, identifierNodeDef, languageNodeDef, coverageNodeDef, rightsNodeDef, degreeNodeDef };
    }

    private static NodeTypeDef getEtdmsElementNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(getEtdmsNodeDefs());
        ntDef.setMixin(false);
        ntDef.setName(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        ntDef.setOrderableChildNodes(true);
        ntDef.setPrimaryItemName(null);
        ntDef.setPropertyDefs(PropDef.EMPTY_ARRAY);
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable"), new QName("http://www.jcp.org/jcr/mix/1.0", "lockable") });
        return ntDef;
    }

    private static NodeDef[] getEtdmsMetadataNodeDefs() {
        NodeDefImpl etdmsRootElementNodeDef = new NodeDefImpl();
        etdmsRootElementNodeDef.setAllowsSameNameSiblings(false);
        etdmsRootElementNodeDef.setAutoCreated(true);
        etdmsRootElementNodeDef.setDeclaringNodeType(new QName(ETDMS_NAMESPACE, ETDMS_METADATA_NAME));
        etdmsRootElementNodeDef.setDefaultPrimaryType(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        etdmsRootElementNodeDef.setMandatory(true);
        etdmsRootElementNodeDef.setName(new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME));
        etdmsRootElementNodeDef.setOnParentVersion(OnParentVersionAction.VERSION);
        etdmsRootElementNodeDef.setProtected(false);
        etdmsRootElementNodeDef.setRequiredPrimaryTypes(new QName[] { new QName(ETDMS_NAMESPACE, ETDMS_ELEMENT_NAME) });
        return new NodeDef[] { etdmsRootElementNodeDef };
    }

    private static NodeTypeDef getEtdmsMetadataNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(getEtdmsMetadataNodeDefs());
        ntDef.setMixin(false);
        ntDef.setName(new QName(ETDMS_NAMESPACE, ETDMS_METADATA_NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(null);
        ntDef.setPropertyDefs(PropDef.EMPTY_ARRAY);
        ntDef.setSupertypes(new QName[] { new QName(NAMESPACE, MetadataDao.NAME) });
        return ntDef;
    }

    private static NodeTypeDef getMetadataNodeTypeDef() throws InvalidConstraintException {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(NodeDef.EMPTY_ARRAY);
        ntDef.setMixin(false);
        ntDef.setName(new QName(NAMESPACE, MetadataDao.NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(null);
        ntDef.setPropertyDefs(PropDef.EMPTY_ARRAY);
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "versionable"), new QName("http://www.jcp.org/jcr/mix/1.0", "lockable") });
        return ntDef;
    }

    private static PropDef[] getUserPropertyDefs() {
        PropDefImpl passwordProp = new PropDefImpl();
        passwordProp.setAutoCreated(false);
        passwordProp.setDeclaringNodeType(new QName(NAMESPACE, UserDao.NAME));
        passwordProp.setDefaultValues(InternalValue.EMPTY_ARRAY);
        passwordProp.setMandatory(true);
        passwordProp.setMultiple(false);
        passwordProp.setName(new QName(NAMESPACE, UserDao.PASSWORD_PROPERTY_NAME));
        passwordProp.setOnParentVersion(OnParentVersionAction.VERSION);
        passwordProp.setProtected(false);
        passwordProp.setRequiredType(PropertyType.STRING);
        passwordProp.setValueConstraints(ValueConstraint.EMPTY_ARRAY);
        return new PropDef[] { passwordProp };
    }

    private static NodeTypeDef getUserNodeTypeDef() {
        NodeTypeDef ntDef = new NodeTypeDef();
        ntDef.setChildNodeDefs(NodeDef.EMPTY_ARRAY);
        ntDef.setMixin(false);
        ntDef.setName(new QName(NAMESPACE, UserDao.NAME));
        ntDef.setOrderableChildNodes(false);
        ntDef.setPrimaryItemName(new QName(NAMESPACE, UserDao.PASSWORD_PROPERTY_NAME));
        ntDef.setPropertyDefs(getUserPropertyDefs());
        ntDef.setSupertypes(new QName[] { new QName("http://www.jcp.org/jcr/nt/1.0", "base"), new QName("http://www.jcp.org/jcr/mix/1.0", "referenceable") });
        return ntDef;
    }

    public static void addNodeTypes(Session session) {
        try {
            namespaceResolver = ((SessionImpl) session).getNamespaceResolver();
            Workspace wsp = session.getWorkspace();
            NamespaceRegistry nsRegistry = wsp.getNamespaceRegistry();
            nsRegistry.registerNamespace(PREFIX.replaceAll(":", ""), NAMESPACE);
            nsRegistry.registerNamespace(DUBLIN_CORE_PREFIX.replaceAll(":", ""), DUBLIN_CORE_NAMESPACE);
            nsRegistry.registerNamespace(ETDMS_PREFIX.replaceAll(":", ""), ETDMS_NAMESPACE);
            NodeTypeManager ntMgr = wsp.getNodeTypeManager();
            NodeTypeRegistry ntReg = ((NodeTypeManagerImpl) ntMgr).getNodeTypeRegistry();
            ntReg.registerNodeType(getGroupNodeTypeDef());
            ntReg.registerNodeType(getSecuredContentNodeTypeDef());
            ntReg.registerNodeType(getUserNodeTypeDef());
            ntReg.registerNodeType(getValueElementNodeTypeDef());
            ntReg.registerNodeType(getValueElementHolderNodeTypeDef());
            ntReg.registerNodeType(getDublinCoreElementNodeTypeDef());
            ntReg.registerNodeType(getEtdmsDegreeElementNodeTypeDef());
            ntReg.registerNodeType(getEtdmsElementNodeTypeDef());
            ntReg.registerNodeType(getMetadataNodeTypeDef());
            ntReg.registerNodeType(getDublinCoreMetadataNodeTypeDef());
            ntReg.registerNodeType(getEtdmsMetadataNodeTypeDef());
            ntReg.registerNodeType(getMetadatasNodeTypeDef());
            ntReg.registerNodeType(getFolderNodeTypeDef());
            List nts = new ArrayList();
            nts.add(getCollectionNodeTypeDef());
            nts.add(getFoldersNodeTypeDef());
            nts.add(getGroupsNodeTypeDef());
            ntReg.registerNodeTypes(nts);
            Node configNode = session.getRootNode().addNode(CONFIG_NODE);
            configNode.setProperty(CONFIG_INDEXER_PROPERTY, PasswordUtil.randomString(12));
            Node indexNode = session.getRootNode().addNode(INDEX_NODE, "nt:folder");
            indexNode.addMixin("mix:lockable");
            Node rootNode = session.getRootNode();
            rootNode.addNode(UserDao.ROOT_USERS_NODE);
            session.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void populateRepository(Session session) {
        try {
            DatabaseUtil dbUtil = DatabaseUtil.getSingleton();
            dbUtil.beginTransaction(SystemPrincipal.getCredentials());
            User admin = new UserImpl();
            admin.setUsername("admin");
            admin.setPassword("admin");
            UserDao.createInstance().save(admin);
            User anonymous = new UserImpl();
            anonymous.setUsername("anonymous");
            anonymous.setPassword("anonymous");
            UserDao.createInstance().save(anonymous);
            User user = new UserImpl();
            user.setUsername("user");
            user.setPassword("user");
            UserDao.createInstance().save(user);
            CollectionDao collectionDao = CollectionDao.createInstance();
            Collection rootCollection = new CollectionImpl();
            rootCollection.setName(CollectionDao.ROOT_COLLECTION);
            rootCollection.setDescription("Access point");
            {
                List readUsers = new ArrayList();
                readUsers.add(anonymous);
                readUsers.add(admin);
                rootCollection.setReadUsers(readUsers);
            }
            {
                List writeUsers = new ArrayList();
                writeUsers.add(admin);
                rootCollection.setWriteUsers(writeUsers);
            }
            {
                List removeUsers = new ArrayList();
                removeUsers.add(admin);
                rootCollection.setRemoveUsers(removeUsers);
            }
            {
                List ownerUsers = new ArrayList();
                ownerUsers.add(admin);
                rootCollection.setOwnerUsers(ownerUsers);
            }
            collectionDao.save(rootCollection);
            dbUtil.commitTransaction();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
